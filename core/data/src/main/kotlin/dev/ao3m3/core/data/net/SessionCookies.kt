package dev.ao3m3.core.data.net

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.Cookie
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the AO3 session cookie. The value is encrypted with a non-exportable Android Keystore key,
 * so a backup or root-level file read yields ciphertext rather than a live session.
 *
 * Only name/value pairs are persisted; attributes are re-created on load for archiveofourown.org,
 * which is the only host this app ever talks to.
 */
@Singleton
class SessionCookies @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val cookies = LinkedHashMap<String, String>()
    private var loaded = false

    @Synchronized
    fun load(): List<Cookie> {
        ensureLoaded()
        return cookies.mapNotNull { (name, value) -> cookie(name, value) }
    }

    @Synchronized
    fun save(incoming: List<Cookie>) {
        ensureLoaded()
        var changed = false
        incoming.forEach { cookie ->
            val expired = cookie.expiresAt < System.currentTimeMillis()
            if (expired) {
                if (cookies.remove(cookie.name) != null) changed = true
            } else if (cookies[cookie.name] != cookie.value) {
                cookies[cookie.name] = cookie.value
                changed = true
            }
        }
        if (changed) persist()
    }

    @Synchronized
    fun clear() {
        cookies.clear()
        loaded = true
        prefs.edit().remove(KEY_COOKIES).apply()
    }

    @Synchronized
    fun hasSession(): Boolean {
        ensureLoaded()
        return cookies.containsKey(SESSION_COOKIE)
    }

    private fun ensureLoaded() {
        if (loaded) return
        loaded = true
        val encoded = prefs.getString(KEY_COOKIES, null) ?: return
        val plain = decrypt(encoded) ?: run {
            prefs.edit().remove(KEY_COOKIES).apply()
            return
        }
        plain.lineSequence().forEach { line ->
            val separator = line.indexOf('=')
            if (separator > 0) {
                cookies[line.substring(0, separator)] = line.substring(separator + 1)
            }
        }
    }

    private fun persist() {
        val plain = cookies.entries.joinToString("\n") { "${it.key}=${it.value}" }
        val encrypted = encrypt(plain)
        if (encrypted == null) {
            prefs.edit().remove(KEY_COOKIES).apply()
        } else {
            prefs.edit().putString(KEY_COOKIES, encrypted).apply()
        }
    }

    private fun cookie(name: String, value: String): Cookie? = runCatching {
        Cookie.Builder()
            .name(name)
            .value(value)
            .domain("archiveofourown.org")
            .path("/")
            .secure()
            .httpOnly()
            .expiresAt(System.currentTimeMillis() + YEAR_MS)
            .build()
    }.getOrNull()

    private fun encrypt(plain: String): String? = runCatching {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val iv = cipher.iv
        val bytes = cipher.doFinal(plain.toByteArray())
        Base64.encodeToString(iv + bytes, Base64.NO_WRAP)
    }.getOrNull()

    private fun decrypt(encoded: String): String? = runCatching {
        val raw = Base64.decode(encoded, Base64.NO_WRAP)
        val iv = raw.copyOfRange(0, IV_BYTES)
        val body = raw.copyOfRange(IV_BYTES, raw.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
        String(cipher.doFinal(body))
    }.getOrNull()

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val PREFS = "ao3_session"
        const val KEY_COOKIES = "cookies"
        const val KEY_ALIAS = "ao3_session_key"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
        const val YEAR_MS = 365L * 24 * 60 * 60 * 1000
        const val SESSION_COOKIE = "_otwarchive_session"
    }
}
