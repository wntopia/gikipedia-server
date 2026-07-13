package io.github.wntopia.gikipedia.server.global.storage

import io.github.wntopia.gikipedia.server.global.config.R2Environment
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.util.StringUtils
import org.springframework.web.multipart.MultipartFile
import software.amazon.awssdk.core.exception.SdkException
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import team.themoment.sdk.exception.ExpectedException
import java.util.UUID

@Component
class R2Uploader(
    private val s3Client: S3Client,
    private val environment: R2Environment,
) {
    fun upload(
        file: MultipartFile,
        keyPrefix: String,
    ): String {
        val key = "$keyPrefix/${UUID.randomUUID()}${extensionOf(file.originalFilename)}"
        val request =
            PutObjectRequest
                .builder()
                .bucket(environment.bucket)
                .key(key)
                .contentType(file.contentType)
                .build()

        try {
            s3Client.putObject(request, RequestBody.fromInputStream(file.inputStream, file.size))
        } catch (exception: SdkException) {
            throw ExpectedException("이미지 업로드에 실패했습니다.", HttpStatus.BAD_GATEWAY)
        }

        return "${publicUrlBase()}/$key"
    }

    private fun publicUrlBase(): String {
        if (!StringUtils.hasText(environment.publicUrl)) {
            throw IllegalStateException("r2.public-url must be configured")
        }
        return environment.publicUrl!!.trimEnd('/')
    }

    private fun extensionOf(originalFilename: String?): String =
        originalFilename
            ?.substringAfterLast('.', "")
            ?.takeIf { it.isNotBlank() }
            ?.let { ".$it" }
            ?: ""
}
