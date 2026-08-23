package io.github.wntopia.gikipedia.server.domain.auth.service.impl

import io.github.wntopia.gikipedia.server.domain.auth.dto.internal.DataGsmSession
import io.github.wntopia.gikipedia.server.domain.auth.dto.internal.DataGsmToken
import io.github.wntopia.gikipedia.server.domain.auth.dto.request.DataGsmOAuthCallbackReqDto
import io.github.wntopia.gikipedia.server.domain.auth.dto.response.DataGsmClubSummaryResDto
import io.github.wntopia.gikipedia.server.domain.auth.dto.response.DataGsmStudentInfoResDto
import io.github.wntopia.gikipedia.server.domain.auth.dto.response.DataGsmUserInfoResDto
import io.github.wntopia.gikipedia.server.domain.auth.service.SigninService
import io.github.wntopia.gikipedia.server.global.config.DataGsmOAuthEnvironment
import io.github.wntopia.gikipedia.server.global.security.session.OAuthSessionAttributes
import jakarta.servlet.http.HttpSession
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
import org.springframework.web.util.UriComponentsBuilder
import team.themoment.datagsm.sdk.oauth.DataGsmOAuthClient
import team.themoment.datagsm.sdk.oauth.exception.DataGsmException
import team.themoment.datagsm.sdk.oauth.model.AccountObjectType
import team.themoment.datagsm.sdk.oauth.model.ClubInfo
import team.themoment.datagsm.sdk.oauth.model.Student
import team.themoment.datagsm.sdk.oauth.model.TokenResponse
import team.themoment.datagsm.sdk.oauth.model.UserInfo
import team.themoment.sdk.exception.ExpectedException
import java.net.URI
import java.time.Instant

@Service
class SigninServiceImpl(
    private val environment: DataGsmOAuthEnvironment,
    private val client: DataGsmOAuthClient,
) : SigninService {
    override fun execute(
        reqDto: DataGsmOAuthCallbackReqDto,
        session: HttpSession,
    ): ResponseEntity<Void> {
        if (reqDto.error != null) {
            return redirect(failureRedirect(reqDto.errorDescription ?: reqDto.error))
        }

        return try {
            val userInfo = authenticate(reqDto.code, reqDto.state, session)
            redirect(successRedirect(userInfo))
        } catch (exception: ExpectedException) {
            redirect(failureRedirect(exception.message))
        }
    }

    private fun authenticate(
        code: String?,
        state: String?,
        session: HttpSession,
    ): DataGsmUserInfoResDto {
        val savedState = session.getAttribute(OAuthSessionAttributes.DATAGSM_STATE) as String?
        val codeVerifier = session.getAttribute(OAuthSessionAttributes.DATAGSM_CODE_VERIFIER) as String?

        session.removeAttribute(OAuthSessionAttributes.DATAGSM_STATE)
        session.removeAttribute(OAuthSessionAttributes.DATAGSM_CODE_VERIFIER)

        if (!StringUtils.hasText(code)) {
            throw ExpectedException("인가 코드가 없습니다.", HttpStatus.BAD_REQUEST)
        }
        if (!StringUtils.hasText(savedState) || savedState != state) {
            throw ExpectedException("OAuth state가 올바르지 않습니다.", HttpStatus.BAD_REQUEST)
        }
        if (!StringUtils.hasText(codeVerifier)) {
            throw ExpectedException("PKCE code verifier가 없습니다.", HttpStatus.BAD_REQUEST)
        }

        val token: DataGsmToken
        val userInfo: DataGsmUserInfoResDto
        try {
            val sdkToken = client.exchangeCodeForToken(code, environment.redirectUri(), codeVerifier)
            val sdkUserInfo = client.getUserInfo(sdkToken.accessToken)
            token = toDataGsmToken(sdkToken)
            userInfo = toDataGsmUserInfoResDto(sdkUserInfo)
        } catch (exception: DataGsmException) {
            throw ExpectedException(
                "DataGSM OAuth 처리에 실패했습니다. ${exception.message}",
                HttpStatus.BAD_GATEWAY,
            )
        }

        val dataGsmSession = DataGsmSession(userInfo, token, Instant.now())

        session.setAttribute(OAuthSessionAttributes.DATAGSM_USER_INFO, userInfo)
        session.setAttribute(OAuthSessionAttributes.DATAGSM_TOKEN, token)
        session.setAttribute(OAuthSessionAttributes.DATAGSM_SESSION, dataGsmSession)

        return userInfo
    }

    private fun redirect(uri: URI): ResponseEntity<Void> =
        ResponseEntity.status(HttpStatus.FOUND).header(HttpHeaders.LOCATION, uri.toString()).build()

    private fun successRedirect(userInfo: DataGsmUserInfoResDto): URI =
        UriComponentsBuilder
            .fromUriString(environment.successRedirectUri().orEmpty())
            .queryParam("authenticated", "true")
            .queryParam("student", userInfo.isStudent == true)
            .build()
            .encode()
            .toUri()

    private fun failureRedirect(reason: String?): URI =
        UriComponentsBuilder
            .fromUriString(environment.failureRedirectUri().orEmpty())
            .queryParam("reason", reason)
            .build()
            .encode()
            .toUri()

    private fun toDataGsmToken(token: TokenResponse): DataGsmToken =
        DataGsmToken(
            token.accessToken,
            token.tokenType,
            token.expiresIn,
            token.refreshToken,
            token.scope,
        )

    private fun toDataGsmUserInfoResDto(userInfo: UserInfo): DataGsmUserInfoResDto =
        DataGsmUserInfoResDto(
            userInfo.id,
            userInfo.email,
            enumName(userInfo.role),
            userInfo.objectType == AccountObjectType.STUDENT,
            toDataGsmStudentInfoResDto(userInfo.student),
        )

    private fun toDataGsmStudentInfoResDto(student: Student?): DataGsmStudentInfoResDto? {
        if (student == null) {
            return null
        }

        return DataGsmStudentInfoResDto(
            student.id,
            student.name,
            enumName(student.sex),
            student.email,
            student.grade,
            student.classNum,
            student.number,
            student.studentNumber,
            enumName(student.major),
            student.specialty,
            enumName(student.role),
            student.dormitoryFloor,
            student.dormitoryRoom,
            student.getIsLeaveSchool(),
            toDataGsmClubSummaryResDto(student.majorClub),
            toDataGsmClubSummaryResDto(student.autonomousClub),
            student.githubId,
            student.githubUrl,
        )
    }

    private fun toDataGsmClubSummaryResDto(clubInfo: ClubInfo?): DataGsmClubSummaryResDto? {
        if (clubInfo == null) {
            return null
        }

        return DataGsmClubSummaryResDto(
            clubInfo.id,
            clubInfo.name,
            enumName(clubInfo.type),
            enumName(clubInfo.status),
            clubInfo.foundedYear,
            clubInfo.abolishedYear,
        )
    }

    private fun enumName(value: Enum<*>?): String? = value?.name
}
