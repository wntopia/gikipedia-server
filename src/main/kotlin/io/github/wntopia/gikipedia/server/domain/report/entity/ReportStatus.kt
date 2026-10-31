package io.github.wntopia.gikipedia.server.domain.report.entity

enum class ReportStatus {
    /** 접수되어 아직 처리되지 않았다. */
    OPEN,

    /** 관리자가 처리를 마쳤다. */
    RESOLVED,
}
