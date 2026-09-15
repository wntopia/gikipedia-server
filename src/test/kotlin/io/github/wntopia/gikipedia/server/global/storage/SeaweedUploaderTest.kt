package io.github.wntopia.gikipedia.server.global.storage

import io.github.wntopia.gikipedia.server.global.config.SeaweedEnvironment
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.core.io.InputStreamResource
import org.springframework.http.ResponseEntity
import org.springframework.mock.web.MockMultipartFile
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import team.themoment.sdk.exception.ExpectedException

/** publicUrlBase 검증과 extensionOf의 path traversal 방어를 실제 네트워크 없이 검증한다. */
class SeaweedUploaderTest {
    private val filerClient = mock<RestClient>()
    private val requestBodyUriSpec = mock<RestClient.RequestBodyUriSpec>()
    private val requestBodySpec = mock<RestClient.RequestBodySpec>()
    private val responseSpec = mock<RestClient.ResponseSpec>()

    @BeforeEach
    fun setUp() {
        whenever(filerClient.put()).thenReturn(requestBodyUriSpec)
        whenever(requestBodyUriSpec.uri(any<String>())).thenReturn(requestBodySpec)
        whenever(requestBodySpec.contentType(any())).thenReturn(requestBodySpec)
        whenever(requestBodySpec.header(any(), any())).thenReturn(requestBodySpec)
        whenever(requestBodySpec.body(any<InputStreamResource>())).thenReturn(requestBodySpec)
        whenever(requestBodySpec.retrieve()).thenReturn(responseSpec)
        whenever(responseSpec.toBodilessEntity()).thenReturn(ResponseEntity.ok().build())
    }

    @Test
    @DisplayName("정상 파일명은 확장자를 그대로 살려 key를 만든다")
    fun buildsKeyWithNormalExtension() {
        val environment = SeaweedEnvironment(filerUrl = "http://filer", publicUrl = "http://public")
        val uploader = SeaweedUploader(filerClient, environment)
        val file = MockMultipartFile("image", "photo.png", "image/png", "data".toByteArray())

        val url = uploader.upload(file, "articles")

        assertThat(url).startsWith("http://public/articles/")
        assertThat(url).endsWith(".png")
    }

    @Test
    @DisplayName("path traversal이 섞인 파일명이면 확장자를 비운다")
    fun stripsExtensionWhenFilenameContainsPathTraversal() {
        val environment = SeaweedEnvironment(filerUrl = "http://filer", publicUrl = "http://public")
        val uploader = SeaweedUploader(filerClient, environment)
        val file =
            MockMultipartFile("image", "weird.png/../../../etc/passwd", "text/plain", "malicious".toByteArray())

        val url = uploader.upload(file, "articles")

        assertThat(url).doesNotContain("..")
        assertThat(url).doesNotContain("/etc/")
        assertThat(url).doesNotContain("passwd")
    }

    @Test
    @DisplayName("확장자에 영숫자가 아닌 문자가 섞이면 확장자를 비운다")
    fun stripsExtensionWhenNotAlphanumeric() {
        val environment = SeaweedEnvironment(filerUrl = "http://filer", publicUrl = "http://public")
        val uploader = SeaweedUploader(filerClient, environment)
        val file = MockMultipartFile("image", "file.{evil}", "text/plain", "data".toByteArray())

        val url = uploader.upload(file, "articles")

        assertThat(url).doesNotContain("{")
        assertThat(url).doesNotContain("}")
    }

    @Test
    @DisplayName("originalFilename이 없으면 확장자 없이 key를 만든다")
    fun handlesNullOriginalFilename() {
        val environment = SeaweedEnvironment(filerUrl = "http://filer", publicUrl = "http://public")
        val uploader = SeaweedUploader(filerClient, environment)
        val file = MockMultipartFile("image", null, "text/plain", "data".toByteArray())

        val url = uploader.upload(file, "articles")

        assertThat(url).startsWith("http://public/articles/")
    }

    @Test
    @DisplayName("publicUrl이 설정되지 않으면 IllegalStateException을 던진다")
    fun throwsWhenPublicUrlMissing() {
        val environment = SeaweedEnvironment(filerUrl = "http://filer", publicUrl = null)
        val uploader = SeaweedUploader(filerClient, environment)
        val file = MockMultipartFile("image", "photo.png", "image/png", "data".toByteArray())

        assertThatThrownBy { uploader.upload(file, "articles") }
            .isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    @DisplayName("업로드 중 RestClientException이 발생하면 ExpectedException(502)으로 변환한다")
    fun wrapsRestClientExceptionAsExpectedException() {
        whenever(responseSpec.toBodilessEntity())
            .thenThrow(RestClientException("connection refused"))
        val environment = SeaweedEnvironment(filerUrl = "http://filer", publicUrl = "http://public")
        val uploader = SeaweedUploader(filerClient, environment)
        val file = MockMultipartFile("image", "photo.png", "image/png", "data".toByteArray())

        assertThatThrownBy { uploader.upload(file, "articles") }
            .isInstanceOf(ExpectedException::class.java)
    }

    @Test
    @DisplayName("publicUrl 끝의 슬래시는 제거하고 key와 합친다")
    fun trimsTrailingSlashFromPublicUrl() {
        val environment = SeaweedEnvironment(filerUrl = "http://filer", publicUrl = "http://public/")
        val uploader = SeaweedUploader(filerClient, environment)
        val file = MockMultipartFile("image", "photo.png", "image/png", "data".toByteArray())

        val url = uploader.upload(file, "articles")

        assertThat(url).doesNotContain("//articles")
    }
}
