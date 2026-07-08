package io.github.wntopia.gikipedia.server.global.entity;

import java.time.Instant;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;

@Getter
public abstract class BaseMongoEntity {

  @Id private String id;

  @CreatedDate private Instant createdAt;

  @LastModifiedDate private Instant updatedAt;
}
