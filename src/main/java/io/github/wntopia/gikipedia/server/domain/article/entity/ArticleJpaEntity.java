package io.github.wntopia.gikipedia.server.domain.article.entity;

import io.github.wntopia.gikipedia.server.global.entity.BaseJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "articles")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArticleJpaEntity extends BaseJpaEntity {

  @Column(name = "title", nullable = false, length = 255)
  private String title;

  @Column(name = "content", nullable = false, columnDefinition = "TEXT")
  private String content;

  @Builder
  private ArticleJpaEntity(String title, String content) {
    this.title = title;
    this.content = content;
  }
}
