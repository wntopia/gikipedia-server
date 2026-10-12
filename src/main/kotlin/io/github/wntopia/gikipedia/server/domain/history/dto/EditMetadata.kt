package io.github.wntopia.gikipedia.server.domain.history.dto

/**
 * 편집자가 수정에 덧붙이는 정보. 둘 다 선택이라 보내지 않은 요청은 [NONE]과 같다.
 *
 * [summary]는 앞뒤 공백을 제거하고 비면 null, [MAX_SUMMARY_LENGTH]자로 자른다(공동편집 메시지는 요청 검증을 거치지 않으므로).
 */
data class EditMetadata(
    val summary: String? = null,
    val minorEdit: Boolean = false,
) {
    fun normalized(): EditMetadata = copy(summary = summary?.trim()?.take(MAX_SUMMARY_LENGTH)?.ifEmpty { null })

    companion object {
        const val MAX_SUMMARY_LENGTH = 500
        val NONE = EditMetadata()
    }
}
