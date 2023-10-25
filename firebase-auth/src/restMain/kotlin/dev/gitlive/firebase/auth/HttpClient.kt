package dev.gitlive.firebase.auth

import dev.gitlive.firebase.FirebaseApp
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal const val KEY_PARAM_NAME = "key"

internal val idTokenFlow = MutableStateFlow<String?>(null)

fun HttpClientConfig<*>.defaultConfig(
    app: FirebaseApp,
    refreshTokenManager: RefreshTokenManager,
    json: Json
){
    anonymousConfig(app, json)

    Auth {
        bearer {

            sendWithoutRequest {
                "firestore" in it.url.host
            }
            loadTokens {
                val tokens = app.loadTokens() ?: return@loadTokens null
                BearerTokens(accessToken = tokens.idToken, refreshToken = tokens.refreshToken)
            }

            refreshTokens {

                val newTokens = refreshTokenManager.refresh(client)
                    ?: return@refreshTokens null


                BearerTokens(
                    accessToken = newTokens.idToken,
                    refreshToken = newTokens.refreshToken
                )
            }
        }
    }
}

fun HttpClientConfig<*>.anonymousConfig(
    app: FirebaseApp,
    json: Json
){
    install(ContentNegotiation) {
        json(json)
    }

    expectSuccess = true

    install(DefaultRequest) {
        contentType(ContentType.Application.Json)
        accept(ContentType.Application.Json)
    }

    Logging {
        this.level = LogLevel.ALL
        logger = object : Logger {
            override fun log(message: String) {
                app.platform.log(message)
            }
        }
    }
}




val DefaultFirebaseJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
    isLenient = true
}

//TODO @InternalApi
fun DefaultFirebaseHttpClient(
    app : FirebaseApp,
    refreshTokenManager : RefreshTokenManager,
    config : HttpClientConfig<*>.() -> Unit = {},
    json : Json = DefaultFirebaseJson
) = HttpClient {
    defaultConfig(app, refreshTokenManager,json)
    config(this)
}


//TODO @InternalApi
fun DefaultFirebaseHttpClient(
    engine: HttpClientEngine,
    app : FirebaseApp,
    refreshTokenManager : RefreshTokenManager,
    config : HttpClientConfig<*>.() -> Unit = {},
    json : Json = DefaultFirebaseJson
) = HttpClient(engine) {

    defaultConfig(app, refreshTokenManager, json)
    config(this)
}

fun AnonymousHttpClient(
    app : FirebaseApp,
    config : HttpClientConfig<*>.() -> Unit = {},
    json : Json = DefaultFirebaseJson
) = HttpClient {
    anonymousConfig(app,json)
    config(this)
}



@Serializable
private class FirebaseErrorResponse(
    val error: FirebaseError
)

@Serializable
private class FirebaseError(
    val code : Int,
    val message : String
)