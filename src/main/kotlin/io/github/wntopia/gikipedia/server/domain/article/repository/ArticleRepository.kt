package io.github.wntopia.gikipedia.server.domain.article.repository

import io.github.wntopia.gikipedia.server.domain.article.entity.ArticleJpaEntity
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ArticleRepository : JpaRepository<ArticleJpaEntity, Long> {
    /** 일일 정합성 검사 배치가 훑을 전체 article id 목록. */
    @Query("SELECT a.id FROM ArticleJpaEntity a")
    fun findAllIds(): List<Long>

    /**
     * 같은 article에 대한 동시 수정 요청을 직렬화하기 위한 행 잠금(`SELECT ... FOR UPDATE`) 조회.
     *
     * 두 사용자가 동시에 같은 article을 수정하면 각자 article_histories의 "다음 리비전 번호"를 동시에
     * 같은 값으로 계산해 유니크 제약(uk_history_article_revision) 위반으로 한쪽이 실패할 수 있다. 이 잠금을
     * 걸어두면 뒤에 온 트랜잭션은 앞선 트랜잭션이 커밋해 잠금을 풀 때까지 여기서 대기하므로, 리비전을 읽는
     * 시점엔 항상 최신 상태를 보게 되어 그 경합이 사라진다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM ArticleJpaEntity a WHERE a.id = :id")
    fun findByIdForUpdate(
        @Param("id") id: Long,
    ): ArticleJpaEntity?
}
