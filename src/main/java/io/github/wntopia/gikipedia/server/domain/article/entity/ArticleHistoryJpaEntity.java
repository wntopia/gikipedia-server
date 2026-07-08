package io.github.wntopia.gikipedia.server.domain.article.entity;

import io.github.wntopia.gikipedia.server.global.entity.BaseJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 위키 문서의 수정 내역(이벤트)을 append-only로 저장하는 엔티티.
 *
 * <p>이벤트 소싱의 이벤트 스트림에 해당한다. 문서의 현재 상태는 {@link ArticleJpaEntity}가 스냅샷으로
 * 유지하고, 이 테이블에는 수정이 일어날 때마다 이전 리비전 대비 변경분(diff)을 한 건씩 쌓기만 한다.
 * 특정 시점의 문서 내용을 복원하려면 revision 1부터 대상 리비전까지의 diff를 순서대로 적용한다.
 */
@Entity
@Getter
@Table(name = "article_histories")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArticleHistoryJpaEntity extends BaseJpaEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "article_id", nullable = false, updatable = false)
  private ArticleJpaEntity article;

  /** 문서별 순차 리비전 번호(1부터 시작). diff 순차 적용 및 버전 지칭에 사용된다. */
  @Column(name = "revision", nullable = false, updatable = false)
  private Integer revision;

  /** 수정자 식별자. "학번 + 이름" 형식의 문자열(예: "2412 홍길동"). */
  @Column(name = "editor", nullable = false, updatable = false, length = 255)
  private String editor;

  /** 이전 리비전 대비 변경분. unified diff(패치) 문자열. */
  @Column(name = "diff", nullable = false, updatable = false, columnDefinition = "TEXT")
  private String diff;

  @Builder
  private ArticleHistoryJpaEntity(
      ArticleJpaEntity article, Integer revision, String editor, String diff) {
    this.article = article;
    this.revision = revision;
    this.editor = editor;
    this.diff = diff;
  }
}
