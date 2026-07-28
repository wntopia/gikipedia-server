package io.github.wntopia.gikipedia.server.global.diff

import com.github.difflib.DiffUtils
import com.github.difflib.UnifiedDiffUtils
import com.github.difflib.patch.PatchFailedException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import team.themoment.sdk.exception.ExpectedException

/**
 * 문서 내용의 forward diff(이전 리비전 → 현재 리비전) 생성과 적용을 담당한다.
 *
 * 저장 포맷은 unified diff 문자열이며, 재구성 시 스냅샷 내용에 diff를 순차 적용해 특정 리비전 문서를 복원한다. 라인 단위로 동작하므로 위키 문서(문단 단위 편집)에 적합하다.
 *
 * 라인 분리 규칙: content를 `\n` 기준으로 나눠 List<String>으로 다루고, 재조립 시 `\n`으로 join한다. 생성과 적용이 동일한 규칙을 쓰므로 `apply(before, generate(before, after)) == after`가
 * 보장된다.
 */
@Component
class ArticleDiff {
    /** before → after 변경분을 unified diff 문자열로 만든다. 변경이 없으면 빈 문자열을 반환한다. */
    fun generate(
        before: String,
        after: String,
    ): String {
        val beforeLines = before.toLines()
        val afterLines = after.toLines()
        val patch = DiffUtils.diff(beforeLines, afterLines)
        if (patch.deltas.isEmpty()) return ""
        val unified = UnifiedDiffUtils.generateUnifiedDiff(FILE_NAME, FILE_NAME, beforeLines, patch, CONTEXT_SIZE)
        return unified.joinToString("\n")
    }

    /** base 내용에 unified diff를 forward 적용해 다음 리비전 내용을 복원한다. 빈 diff는 변경 없음으로 간주해 base를 그대로 반환한다. */
    fun apply(
        base: String,
        unifiedDiff: String,
    ): String {
        if (unifiedDiff.isEmpty()) return base
        val baseLines = base.toLines()
        val patch = UnifiedDiffUtils.parseUnifiedDiff(unifiedDiff.toLines())
        return try {
            patch.applyTo(baseLines).joinToString("\n")
        } catch (_: PatchFailedException) {
            throw ExpectedException("문서 재구성에 실패했습니다. 히스토리 데이터가 손상되었을 수 있습니다.", HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    private fun String.toLines(): List<String> = split("\n")

    companion object {
        private const val FILE_NAME = "article"
        private const val CONTEXT_SIZE = 3
    }
}
