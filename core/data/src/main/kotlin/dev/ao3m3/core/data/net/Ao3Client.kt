package dev.ao3m3.core.data.net

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The only way the app talks to AO3.
 *
 * Politeness is structural, not advisory: every request passes through one mutex with a minimum
 * gap between requests, there is no parallel fan-out anywhere in the app, and transient failures
 * back off with a hard retry cap. A maintenance response surfaces as [Ao3Exception.Maintenance] so
 * callers fall back to cache instead of hammering the archive.
 */
@Singleton
class Ao3Client @Inject constructor(
    private val session: SessionCookies,
) {
    private val cookieJar = object : CookieJar {
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) = session.save(cookies)
        override fun loadForRequest(url: HttpUrl): List<Cookie> = session.load()
    }

    private val client = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(45, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val gate = Mutex()
    private var lastRequestAt = 0L

    suspend fun getHtml(
        path: List<String>,
        query: List<Pair<String, String>> = emptyList(),
        allowAdult: Boolean = true,
    ): Document {
        val params = if (allowAdult) query + ("view_adult" to "true") else query
        val url = baseUrlBuilder(path).apply {
            params.forEach { (key, value) -> addQueryParameter(key, value) }
        }.build()
        return Ksoup.parse(execute(Request.Builder().url(url).get()))
    }

    suspend fun getText(
        path: List<String>,
        query: List<Pair<String, String>> = emptyList(),
    ): String {
        val url = baseUrlBuilder(path).apply {
            query.forEach { (key, value) -> addQueryParameter(key, value) }
        }.build()
        return execute(Request.Builder().url(url).get())
    }

    /** Posts an AO3 form field-for-field, the way its own HTML form would. */
    suspend fun postForm(action: String, fields: Map<String, String>): String {
        val body = fields.entries.joinToString("&") { (key, value) ->
            "${key.urlEncode()}=${value.urlEncode()}"
        }
        val url = if (action.startsWith("http")) action.toHttpUrl() else {
            "${Ao3Urls.BASE}/".toHttpUrl().resolve(action)
                ?: throw Ao3Exception.Parse("bad form action $action")
        }
        return execute(
            Request.Builder().url(url).post(body.toRequestBody(FORM_MEDIA_TYPE)),
        )
    }

    fun hasSession(): Boolean = session.hasSession()

    fun clearSession() = session.clear()

    /** Imports the cookie AO3 set inside the login WebView. */
    fun importWebViewCookies(rawCookieHeader: String) {
        rawCookieHeader.split(';').forEach { part ->
            val trimmed = part.trim()
            val separator = trimmed.indexOf('=')
            if (separator <= 0) return@forEach
            val name = trimmed.substring(0, separator)
            val value = trimmed.substring(separator + 1)
            if (name != SESSION_COOKIE || value.isEmpty()) return@forEach
            session.save(
                listOf(
                    Cookie.Builder()
                        .name(name)
                        .value(value)
                        .domain("archiveofourown.org")
                        .path("/")
                        .secure()
                        .httpOnly()
                        .expiresAt(System.currentTimeMillis() + THIRTY_DAYS_MS)
                        .build(),
                ),
            )
        }
    }

    private suspend fun execute(builder: Request.Builder): String =
        withContext(Dispatchers.IO) {
            var attempt = 0
            while (true) {
                gate.withLock {
                    val elapsed = System.currentTimeMillis() - lastRequestAt
                    if (elapsed < MIN_INTERVAL_MS) delay(MIN_INTERVAL_MS - elapsed)
                    lastRequestAt = System.currentTimeMillis()
                }

                val request = builder
                    .header("User-Agent", Ao3Urls.USER_AGENT)
                    .header(
                        "Accept",
                        "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
                    )
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .build()

                try {
                    client.newCall(request).execute().use { response ->
                        val body = response.body?.string().orEmpty()
                        when {
                            isMaintenance(response.code, body) ->
                                throw Ao3Exception.Maintenance()

                            response.code == 401 || response.code == 403 ->
                                throw Ao3Exception.Unauthorized()

                            response.code == 404 -> throw Ao3Exception.NotFound()

                            response.code == 429 -> {
                                val retryAfter =
                                    response.header("Retry-After")?.toLongOrNull() ?: 30
                                if (attempt++ >= MAX_RETRIES) {
                                    throw Ao3Exception.RateLimited(retryAfter)
                                }
                                delay(retryAfter.coerceIn(1, 60) * 1_000)
                            }

                            response.code >= 500 -> {
                                if (attempt++ >= MAX_RETRIES) {
                                    throw Ao3Exception.Server(response.code)
                                }
                                delay(backoffMillis(attempt))
                            }

                            !response.isSuccessful -> throw Ao3Exception.Http(response.code)

                            else -> return@withContext body
                        }
                    }
                } catch (io: IOException) {
                    if (attempt++ >= MAX_RETRIES) throw Ao3Exception.Network(io)
                    delay(backoffMillis(attempt))
                }
            }
            @Suppress("UNREACHABLE_CODE")
            ""
        }

    private fun isMaintenance(code: Int, body: String): Boolean {
        if (code == 418) return true
        if (code !in 400..599) return false
        return body.contains("temporarily denied", ignoreCase = true) ||
            body.contains("down for maintenance", ignoreCase = true)
    }

    private fun backoffMillis(attempt: Int): Long = (1L shl attempt.coerceAtMost(4)) * 1_000

    private fun String.urlEncode(): String = java.net.URLEncoder.encode(this, "UTF-8")

    private companion object {
        const val MIN_INTERVAL_MS = 2_000L
        const val MAX_RETRIES = 2
        const val SESSION_COOKIE = "_otwarchive_session"
        const val THIRTY_DAYS_MS = 30L * 24 * 60 * 60 * 1000
        val FORM_MEDIA_TYPE = "application/x-www-form-urlencoded".toMediaType()
    }
}

/** Builds an AO3 URL for the given path segments, encoding tag slashes correctly. */
fun baseUrlBuilder(segments: List<String>): HttpUrl.Builder =
    "${Ao3Urls.BASE}/".toHttpUrl().newBuilder().apply {
        segments.forEach { addPathSegment(it) }
    }
