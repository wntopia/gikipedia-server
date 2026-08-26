package io.github.wntopia.gikipedia.server.domain.article.repository

import io.github.wntopia.gikipedia.server.domain.article.entity.ArticleJpaEntity
import io.github.wntopia.gikipedia.server.global.config.JpaAuditingConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * findByIdForUpdate()의 `SELECT ... FOR UPDATE` 행 잠금이 같은 article에 대한 동시 트랜잭션을 실제로
 * 직렬화하는지 검증하는 통합 테스트.
 *
 * `@DataJpaTest`는 기본적으로 테스트 메서드를 트랜잭션으로 감싸고 종료 시 롤백한다. 이 테스트는 서로 다른
 * 스레드가 실제로 커밋하는 별도 트랜잭션이어야 하므로, 클래스 레벨에서 그 기본 동작을
 * `Propagation.NOT_SUPPORTED`로 꺼두고 [TransactionTemplate]으로 각 작업을 직접 트랜잭션에 담아 실행한다.
 */
@DataJpaTest
@Import(JpaAuditingConfig::class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ArticleRepositoryLockingIntegrationTest {
    @Autowired
    private lateinit var articleRepository: ArticleRepository

    @Autowired
    private lateinit var transactionManager: PlatformTransactionManager

    private val transactionTemplate by lazy { TransactionTemplate(transactionManager) }

    @Test
    fun `findByIdForUpdate는 같은 row에 대한 동시 트랜잭션을 직렬화한다`() {
        val articleId =
            requireNotNull(
                transactionTemplate.execute {
                    articleRepository.save(ArticleJpaEntity(title = "제목", content = "내용")).id
                },
            )

        val events = Collections.synchronizedList(mutableListOf<String>())
        val firstThreadLocked = CountDownLatch(1)

        val first =
            thread {
                transactionTemplate.execute {
                    articleRepository.findByIdForUpdate(articleId)
                    events.add("first-locked")
                    firstThreadLocked.countDown()
                    // 잠금을 쥔 채로 잠시 대기 — 그사이 두 번째 스레드가 findByIdForUpdate에서
                    // 블록되는지 확인하기 위한 시간을 벌어준다.
                    Thread.sleep(300)
                    events.add("first-commit")
                }
            }

        firstThreadLocked.await(2, TimeUnit.SECONDS)
        val second =
            thread {
                events.add("second-before-lock")
                transactionTemplate.execute {
                    articleRepository.findByIdForUpdate(articleId)
                    events.add("second-locked")
                }
            }

        first.join(5_000)
        second.join(5_000)

        // 두 번째 스레드의 잠금 획득은 반드시 첫 번째 스레드의 커밋 이후에 일어나야 한다 —
        // 그래야 두 트랜잭션이 article_histories의 "다음 리비전 번호"를 같은 값으로 계산해
        // 경합하는 일이 없다.
        assertThat(events).containsSubsequence("first-locked", "first-commit", "second-locked")
    }
}
