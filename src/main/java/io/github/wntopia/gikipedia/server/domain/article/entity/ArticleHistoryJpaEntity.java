package io.github.wntopia.gikipedia.server.domain.article.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@Table(name = "article_histories")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArticleHistoryJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false, updatable = false)
  private Long id;

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

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Builder
  private ArticleHistoryJpaEntity(
      ArticleJpaEntity article, Integer revision, String editor, String diff) {
    this.article = article;
    this.revision = revision;
    this.editor = editor;
    this.diff = diff;
  }
}
