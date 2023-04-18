package com.example.plugins

import com.example.dto.GitHubUser
import com.example.dto.GoogleUser
import com.example.oauth2.github.githubAuth
import com.example.oauth2.google.googleAuth
import io.ktor.server.sessions.*
import io.ktor.server.response.*
import io.ktor.server.auth.*
import io.ktor.client.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import kotlinx.serialization.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.server.html.*
import io.ktor.server.request.*
import kotlinx.html.*
import io.ktor.client.engine.java.*

val client = HttpClient(Java) {
    engine {
        threadsCount = 8
        pipelining = true
        protocolVersion = java.net.http.HttpClient.Version.HTTP_2
    }
}

fun Application.configureSecurity() {
    install(Sessions) {
        cookie<UserSession>("user_session")
        cookie<AuthType>("auth_type")
    }
    googleAuth()
    githubAuth()
    routing {
        authenticate("oauth-google") {
            get("/login/google") {
                call.respondRedirect("/google/callback")
            }
            get("/google/callback") {
                val principal: OAuthAccessTokenResponse.OAuth2? = call.authentication.principal()
                call.sessions.set("user_session",UserSession(principal!!.state!!, principal.accessToken))
                call.sessions.set("auth_type", AuthType("google"))
                call.respondRedirect("/dashboard")
            }
        }
        authenticate("oauth-github") {
            get("/login/github") {
                call.respondRedirect("/github/callback")
            }
            get("/github/callback") {
                val principal: OAuthAccessTokenResponse.OAuth2? = call.authentication.principal()
                call.sessions.set("user_session",UserSession(principal!!.state!!, principal.accessToken))
                call.sessions.set("auth_type", AuthType("github"))
                call.respondRedirect("/dashboard")
            }
        }
        get("/dashboard") {
            val session: UserSession? = call.sessions.get("user_session") as UserSession?
            val authType: AuthType? = call.sessions.get("auth_type") as AuthType?
            if (session!=null){
               if (authType?.type=="github"){
                   var userInfo: GitHubUser? = null
                   var userString = ""
                   try {
                       println(session.token)
                       userString = client.get("https://api.github.com/user") {
                           headers {
                               append(HttpHeaders.Authorization, "Bearer ${session.token}")
                               append("X-GitHub-Api-Version","2022-11-28")
                           }
                        }.bodyAsText()
                           val json = kotlinx.serialization.json.Json {
                               ignoreUnknownKeys = true
                           }
                           userInfo = json.decodeFromString(userString)
                   } catch (e: NoTransformationFoundException) {
                       println(e.localizedMessage)
                   } finally {
                       call.respondHtml(HttpStatusCode.OK) {
                           head {
                               title {
                                   +"GitHub User"
                               }
                           }
                           body {
                               h1 {
                                   +"Hello, ${userInfo?.name}"
                               }
                               img {
                                   src="${userInfo?.avatarUrl}"
                               }
                           }
                       }                   }
               }else{
                   var userString = ""
                   var userInfo: GoogleUser? = null
                   try {
                       userString = client.get("https://www.googleapis.com/oauth2/v2/userinfo?access_token=${session.token}").bodyAsText()
                       val json = kotlinx.serialization.json.Json {
                           ignoreUnknownKeys = true
                       }
                       userInfo = json.decodeFromString(userString)
                   } catch (e: NoTransformationFoundException) {
                       println(e.localizedMessage)
                   } finally {
                       call.respondHtml(HttpStatusCode.OK) {
                           head {
                               title {
                                   +"Google User"
                               }
                           }
                           body {
                               h1 {
                                   +"Hello, ${userInfo?.name}"
                               }
                               img {
                                   src="${userInfo?.picture}"
                               }
                           }
                       }
                   }
               }
            }else{
                val redirectUrl = URLBuilder("http://0.0.0.0:8080/login/${authType?.type}").run {
                    parameters.append("redirectUrl", call.request.uri)
                    build()
                }
                call.respondRedirect(redirectUrl)
            }
        }
    }
}
data class UserSession(val state: String, val token: String)
data class AuthType(val type: String)
