package com.studymate.data.remote

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal data class MobileSessionToken(
    val accessToken: String,
    val expiresAtEpochMillis: Long,
)

/** Stores the anonymous mobile bearer token encrypted with a key held by Android Keystore. */
internal interface SessionTokenStore {
    fun get(): MobileSessionToken?
    fun save(session: MobileSessionToken)
    fun clear()
}

internal class AndroidKeystoreSessionTokenStore(context: Context) : SessionTokenStore {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    @Synchronized
    override fun get(): MobileSessionToken? {
        val iv = preferences.getString(KEY_IV, null) ?: return null
        val ciphertext = preferences.getString(KEY_CIPHERTEXT, null) ?: return null
        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(TAG_LENGTH_BITS, Base64.decode(iv, Base64.NO_WRAP)))
            val plaintext = String(Base64.decode(ciphertext, Base64.NO_WRAP).let(cipher::doFinal), StandardCharsets.UTF_8)
            val separator = plaintext.indexOf('\n')
            require(separator > 0)
            MobileSessionToken(
                accessToken = plaintext.substring(separator + 1),
                expiresAtEpochMillis = plaintext.substring(0, separator).toLong(),
            ).takeIf { it.accessToken.isNotBlank() }
        }.getOrElse {
            clear()
            null
        }
    }

    @Synchronized
    override fun save(session: MobileSessionToken) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val plaintext = "${session.expiresAtEpochMillis}\n${session.accessToken}"
            .toByteArray(StandardCharsets.UTF_8)
        val ciphertext = cipher.doFinal(plaintext)
        check(
            preferences.edit()
                .putString(KEY_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
                .putString(KEY_CIPHERTEXT, Base64.encodeToString(ciphertext, Base64.NO_WRAP))
                .commit(),
        ) { "Could not persist mobile session token" }
    }

    @Synchronized
    override fun clear() {
        preferences.edit().remove(KEY_IV).remove(KEY_CIPHERTEXT).commit()
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

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
        const val PREFERENCES = "mobile_session_secure"
        const val KEY_IV = "iv"
        const val KEY_CIPHERTEXT = "ciphertext"
        const val KEY_ALIAS = "studymate_mobile_session_aes"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val TAG_LENGTH_BITS = 128
    }
}
