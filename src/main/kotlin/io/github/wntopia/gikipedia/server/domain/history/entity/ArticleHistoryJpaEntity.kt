package io.github.wntopia.gikipedia.server.domain.history.entity

import io.github.wntopia.gikipedia.server.domain.article.entity.ArticleJpaEntity
import io.github.wntopia.gikipedia.server.global.entity.BaseJpaEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

/**
 * 위키 문서의 수정 내역(이벤트)을 append-only로 저장하는 엔티티.
 *
 * 이벤트 소싱의 이벤트 스트림에 해당한다. 문서의 현재 상태는 [ArticleJpaEntity]가 스냅샷으로 유지하고, 이 테이블에는 수정이 일어날 때마다 이전 리비전 대비
 * 변경분(diff)을 한 건씩 쌓기만 한다. 특정 시점의 문서 내용을 복원하려면 revision 1부터 대상 리비전까지의 diff를 순서대로 적용한다.
 */
@Entity
@Table(name = "article_histories")
class ArticleHistoryJpaEntity(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false, updatable = false)
    val article: ArticleJpaEntity,
    /** 문서별 순차 리비전 번호(1부터 시작). diff 순차 적용 및 버전 지칭에 사용된다. */
    @Column(name = "revision", nullable = false, updatable = false) val revision: Int,
    /** 수정자 식별자. "학번 + 이름" 형식의 문자열(예: "2412 홍길동"). */
    @Column(name = "editor", nullable = false, updatable = false, length = 255) val editor: String,
    /** 이전 리비전 대비 변경분. unified diff(패치) 문자열. */
    @Column(name = "diff", nullable = false, updatable = false, columnDefinition = "TEXT")
    val diff: String,
) : BaseJpaEntity()
