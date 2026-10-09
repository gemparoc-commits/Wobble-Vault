package com.wobble.vault.data

import com.wobble.vault.BuildConfig
import com.wobble.vault.data.api.WobbleApi
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class CsrfCookieJar(private val store: TokenStore) : CookieJar {

    @Volatile
    private var csrfCookie: Cookie? = null

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val cookie = cookies.lastOrNull { it.name == "XSRF-TOKEN" && it.value.isNotBlank() } ?: return
        csrfCookie = cookie
        store.csrfToken = cookie.value
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val cookie = csrfCookie ?: return emptyList()
        return if (cookie.matches(url)) listOf(cookie) else emptyList()
    }
}

class AuthInterceptor(private val store: TokenStore) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val original = chain.request()
        val builder = original.newBuilder()
        store.bearer?.let { builder.header("Authorization", "Bearer $it") }
        val csrfExempt = original.method == "POST" && original.url.encodedPath.endsWith("/api/auth/login")
        if (original.method != "GET" && !csrfExempt) {
            store.csrfToken?.takeIf { it.isNotBlank() }?.let {
                builder.header("X-XSRF-TOKEN", it)
                builder.header("Cookie", "XSRF-TOKEN=$it")
            }
        }
        return chain.proceed(builder.build())
    }
}

object Network {

    fun createApi(store: TokenStore): WobbleApi {
        val client = OkHttpClient.Builder()
            .cookieJar(CsrfCookieJar(store))
            .addInterceptor(AuthInterceptor(store))
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(
                        HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.BODY
                            redactHeader("Authorization")
                            redactHeader("X-XSRF-TOKEN")
                        }
                    )
                }
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL + "/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WobbleApi::class.java)
    }
}
