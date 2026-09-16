package io.github.wntopia.gikipedia.server.global.storage

import io.github.wntopia.gikipedia.server.global.config.SeaweedEnvironment
import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.StringUtils
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.multipart.MultipartFile
import team.themoment.sdk.exception.ExpectedException
import java.util.UUID

@Component
class SeaweedUploader(
    private val filerClient: RestClient,
    private val environment: SeaweedEnvironment,
) {
    fun upload(
        file: MultipartFile,
        keyPrefix: String,
    ): String {
        val key = "$keyPrefix/${UUID.randomUUID()}${extensionOf(file.originalFilename)}"

        try {
            file.inputStream.use { inputStream ->
                filerClient
                    .put()
                    .uri("/$key")
                    .contentType(
                        MediaType.parseMediaType(file.contentType ?: MediaType.APPLICATION_OCTET_STREAM_VALUE),
                    ).header(HttpHeaders.CONTENT_LENGTH, file.size.toString())
                    .body(InputStreamResource(inputStream))
                    .retrieve()
                    .toBodilessEntity()
            }
        } catch (exception: RestClientException) {
            throw ExpectedException("이미지 업로드에 실패했습니다.", HttpStatus.BAD_GATEWAY)
        }

        return "${publicUrlBase()}/$key"
    }

    private fun publicUrlBase(): String {
        if (!StringUtils.hasText(environment.publicUrl)) {
            throw IllegalStateException("seaweedfs.public-url must be configured")
        }
        return environment.publicUrl!!.trimEnd('/')
    }

    private fun extensionOf(originalFilename: String?): String =
        originalFilename
            ?.substringAfterLast('/', originalFilename)
            ?.substringAfterLast('.', "")
            ?.takeIf { it.isNotBlank() && it.all { char -> char.isLetterOrDigit() } }
            ?.let { ".$it" }
            ?: ""
}
