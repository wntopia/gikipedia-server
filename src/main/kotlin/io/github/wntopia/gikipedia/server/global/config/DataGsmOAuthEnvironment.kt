package io.github.wntopia.gikipedia.server.global.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "spring.security.oauth2.client")
data class DataGsmOAuthEnvironment(
    val registration: Map<String, Registration>?,
    val provider: Map<String, Provider>?,
) {
    fun authorizationBaseUrl(): String? = removeSuffix(datagsmProvider().authorizationUri, AUTHORIZATION_PATH)

    fun resourceBaseUrl(): String? = removeSuffix(datagsmProvider().userInfoUri, USER_INFO_PATH)

    fun clientId(): String? = datagsmRegistration().clientId

    fun clientSecret(): String? = datagsmRegistration().clientSecret

    fun redirectUri(): String? = datagsmRegistration().redirectUri

    fun scope(): String? = datagsmRegistration().scope

    fun successRedirectUri(): String? = datagsmRegistration().successRedirectUri

    fun failureRedirectUri(): String? = datagsmRegistration().failureRedirectUri

    private fun datagsmRegistration(): Registration =
        registration?.get(DATAGSM)
            ?: throw IllegalStateException(
                "spring.security.oauth2.client.registration.datagsm must be configured",
            )

    private fun datagsmProvider(): Provider =
        provider?.get(DATAGSM)
            ?: throw IllegalStateException(
                "spring.security.oauth2.client.provider.datagsm must be configured",
            )

    private fun removeSuffix(
        value: String?,
        suffix: String,
    ): String? =
        if (value == null || !value.endsWith(suffix)) {
            value
        } else {
            value.substring(0, value.length - suffix.length)
        }

    data class Registration(
        val clientId: String?,
        val clientSecret: String?,
        val redirectUri: String?,
        val scope: String?,
        val successRedirectUri: String?,
        val failureRedirectUri: String?,
    )

    data class Provider(
        val authorizationUri: String?,
        val tokenUri: String?,
        val userInfoUri: String?,
    )

    companion object {
        private const val DATAGSM = "datagsm"
        private const val AUTHORIZATION_PATH = "/v1/oauth/authorize"
        private const val USER_INFO_PATH = "/userinfo"
    }
}
