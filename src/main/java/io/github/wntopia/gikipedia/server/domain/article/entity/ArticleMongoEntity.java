package io.github.wntopia.gikipedia.server.domain.article.entity;

import io.github.wntopia.gikipedia.server.global.entity.BaseMongoEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "articles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArticleMongoEntity extends BaseMongoEntity {

  @Field("document_id")
  private Long documentId;

  @Field("title")
  private String title;

  @Field("content")
  private String content;

  @Builder
  private ArticleMongoEntity(Long documentId, String title, String content) {
    this.documentId = documentId;
    this.title = title;
    this.content = content;
  }
}
