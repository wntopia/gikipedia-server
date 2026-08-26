package io.github.wntopia.gikipedia.server.domain.article.repository

import io.github.wntopia.gikipedia.server.domain.article.entity.ArticleJpaEntity
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ArticleRepository : JpaRepository<ArticleJpaEntity, Long> {
    /**
     * 일일 정합성 검사 배치가 청크 단위로 훑을 article id 목록.
     *
     * 전체 id를 한 번에 올리면 문서 수에 비례해 힙을 먹으므로, 마지막으로 처리한 id 이후부터 [pageable]
     * 크기만큼만 끊어 가져오는 keyset 페이징을 쓴다. OFFSET 페이징과 달리 뒤쪽 청크로 갈수록 느려지지 않고,
     * 스캔 도중 새 article이 끼어들어도 앞선 청크를 다시 읽지 않는다.
     */
    @Query("SELECT a.id FROM ArticleJpaEntity a WHERE a.id > :afterId ORDER BY a.id")
    fun findIdsAfter(
        @Param("afterId") afterId: Long,
        pageable: Pageable,
    ): List<Long>

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
