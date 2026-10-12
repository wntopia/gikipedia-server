package io.github.wntopia.gikipedia.server.global.security.session

/**
 * 편집을 한 사용자. [label]은 히스토리에 표시용으로 남기는 "학번 이름" 문자열이고, [userId]는 학년이 바뀌어도
 * 달라지지 않는 DataGSM 계정 id라 기여 집계의 기준이 된다.
 */
data class EditorIdentity(
    val userId: Long?,
    val label: String,
    /** 화면에 "○○ 편집 중"으로 보여줄 이름(학번 제외). 알 수 없으면 null. */
    val name: String? = null,
)
