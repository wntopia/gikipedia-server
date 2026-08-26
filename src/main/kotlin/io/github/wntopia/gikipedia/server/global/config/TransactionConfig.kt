package io.github.wntopia.gikipedia.server.global.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

/**
 * R2 업로드처럼 DB와 무관한 외부 I/O를 트랜잭션 경계 밖으로 빼내면서, 그 뒤의 DB 쓰기만 명시적으로
 * 트랜잭션으로 묶고 싶은 서비스(`UpdateArticleServiceImpl`, `UpdateArticleImageServiceImpl` 등)를 위한
 * [TransactionTemplate]. `@Transactional`로 메서드 전체를 감싸면 커넥션 획득 시점이 Hibernate의 지연
 * 획득 설정에 암묵적으로 의존하게 되는데, 이 방식은 어떤 코드가 트랜잭션 안에 있는지 호출부에서 명시적으로
 * 드러난다.
 */
@Configuration
class TransactionConfig {
    @Bean
    fun transactionTemplate(transactionManager: PlatformTransactionManager): TransactionTemplate =
        TransactionTemplate(transactionManager)
}
