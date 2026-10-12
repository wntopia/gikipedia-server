package io.github.wntopia.gikipedia.server.domain.user.entity

enum class ContributionType {
    /** 문서를 새로 만들었다. */
    CREATE,

    /** 기존 문서를 수정해 새 리비전을 만들었다. */
    EDIT,
}
