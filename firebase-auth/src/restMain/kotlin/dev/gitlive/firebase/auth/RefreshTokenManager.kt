package dev.gitlive.firebase.auth

import dev.gitlive.firebase.FirebaseApp
import dev.gitlive.firebase.FirebaseService
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val TOKEN_URL = "https://securetoken.googleapis.com/v1/token"

class RefreshTokenManager(
    private val app: FirebaseApp,
) : FirebaseService {

    private val mutex = Mutex()

    suspend fun awaitRefreshEnd() {
        return mutex.withLock { }
    }

    private val context = Job()

    suspend fun refresh(client: HttpClient): FirebaseAuthTokens? {
        return mutex.withLock {

            withContext(context) {

                runCatching {
                    val tokens = app.loadTokens()
                        ?: return@runCatching null

                    val resp = client.post(TOKEN_URL) {
                        parameter(KEY_PARAM_NAME, app.options.apiKey)
                        setBody(RefreshTokensRequest(refreshToken = tokens.refreshToken))
                    }.body<RefreshTokensResponse>()

                    val newTokens = FirebaseAuthTokens(
                        idToken = resp.idToken,
                        refreshToken = resp.refreshToken
                    )

                    idTokenFlow.tryEmit(newTokens.idToken)

                    app.saveTokens(newTokens)
                    newTokens
                }.getOrNull()
            }
        }
    }

    override fun release() {
        context.cancel()
        if (mutex.isLocked){
            mutex.unlock()
        }
    }
}

@Serializable
private class RefreshTokensResponse(
    @SerialName("expires_in")
    val expiresIn : String,

    @SerialName("token_type")
    val tokenType : String,

    @SerialName("refresh_token")
    val refreshToken : String,

    @SerialName("id_token")
    val idToken : String,

    @SerialName("user_id")
    val userId : String,

    @SerialName("project_id")
    val projectId : String,
)


@Serializable
private class RefreshTokensRequest(
    @SerialName("refresh_token")
    val refreshToken : String,

    @SerialName("grant_type")
    val grantType : String = "refresh_token",
)