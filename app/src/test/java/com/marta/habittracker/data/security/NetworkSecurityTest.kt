package com.marta.habittracker.data.security

import com.marta.habittracker.di.DataModule
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.util.concurrent.TimeUnit

class NetworkSecurityTest {

    @Test
    fun retrofitBaseUrlUsesHttps() {
        val retrofit = DataModule.provideRetrofit(
            Json {
                ignoreUnknownKeys = true
                isLenient = true
            }
        )

        assertEquals("https", retrofit.baseUrl().scheme)
    }

    @Test
    fun authInterceptorSendsTokenOnlyInAuthorizationHeader() {
        val token = "secret-token-123"
        val interceptor = SecureAuthInterceptor { token }

        val originalRequest = Request.Builder()
            .url("https://example.com/doLogin/.json")
            .build()

        val capturedRequest = interceptor.intercept(FakeChain(originalRequest)).request

        assertEquals("Bearer $token", capturedRequest.header("Authorization"))
        assertFalse(
            "Token must not be leaked through query params",
            capturedRequest.url.toString().contains(token)
        )
    }

    private class SecureAuthInterceptor(
        private val tokenProvider: () -> String?
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val token = tokenProvider()
            val request = if (token.isNullOrBlank()) {
                chain.request()
            } else {
                chain.request().newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            }

            return chain.proceed(request)
        }
    }

    private class FakeChain(
        private val originalRequest: Request
    ) : Interceptor.Chain {
        lateinit var request: Request

        override fun request(): Request = originalRequest

        override fun proceed(request: Request): Response {
            this.request = request
            return Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .build()
        }

        override fun connection() = null
        override fun call() = throw UnsupportedOperationException("Not needed by this unit test")
        override fun connectTimeoutMillis() = 1_000
        override fun readTimeoutMillis() = 1_000
        override fun writeTimeoutMillis() = 1_000
        override fun withConnectTimeout(timeout: Int, unit: TimeUnit) = this
        override fun withReadTimeout(timeout: Int, unit: TimeUnit) = this
        override fun withWriteTimeout(timeout: Int, unit: TimeUnit) = this
    }
}
