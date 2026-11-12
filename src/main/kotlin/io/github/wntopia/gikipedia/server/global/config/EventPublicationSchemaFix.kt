package io.github.wntopia.gikipedia.server.global.config

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import javax.sql.DataSource

/**
 * Spring Modulith의 `event_publication.serialized_event`는 길이 지정이 없어 `ddl-auto`가 VARCHAR(255)로 만든다.
 * `ArticleUpdatedEvent`는 본문을 통째로 싣기 때문에 JSON이 255자를 넘으면(본문 약 48자) 저장 트랜잭션이
 * "Data too long"으로 롤백된다. 라이브러리 엔티티라 매핑을 고칠 수 없고 `ddl-auto: update`는 기존 컬럼을
 * 넓히지 않으므로, 기동할 때마다 직접 넓힌다(이미 LONGTEXT면 변화 없음).
 */
@Component
class EventPublicationSchemaFix(
    private val dataSource: DataSource,
) : ApplicationRunner {
    override fun run(args: ApplicationArguments) {
        dataSource.connection.use { connection ->
            connection.createStatement().use {
                it.execute("ALTER TABLE event_publication MODIFY serialized_event LONGTEXT NOT NULL")
            }
        }
    }
}
