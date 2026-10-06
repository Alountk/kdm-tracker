package io.kdmtracker.app.sync

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Storage for the sync hub's bearer token.
 *
 * Kept as an interface so the transport can be tested against a fake and the production binding can
 * be swapped without touching [HttpSyncApi]. [read] returns `null` when no token is stored.
 */
interface TokenStore {
    fun read(): String?

    fun write(token: String?)
}

/**
 * [TokenStore] backed by a hardware-protected key in the Android Keystore.
 *
 * Rationale for the choice (Ruling AA): the token is a shared secret, so at rest it must not be
 * readable by other apps or by a rooted-device attacker who only slurps app data. The key lives in
 * the Keystore (not extractable), the token is encrypted with AES-GCM and only the ciphertext plus
 * IV are written to a private `SharedPreferences` file. That needs no third-party dependency —
 * `EncryptedSharedPreferences` would pull in `androidx.security:security-crypto`, which is not in
 * the version catalog, and `DataStore` alone stores plaintext. A deprecated/rotated key simply
 * makes [read] return `null`, which is safe: the next sync is unauthenticated and fails non-fatally.
 */
class AndroidKeystoreTokenStore(
    context: Context,
) : TokenStore {
    private val preferences =
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun read(): String? {
        val stored = preferences.getString(KEY_TOKEN, null) ?: return null
        val parts = stored.split(SEPARATOR, limit = 2)
        if (parts.size != 2) return null
        return runCatching {
            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val cipherText = Base64.decode(parts[1], Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(TAG_BITS, iv))
            String(cipher.doFinal(cipherText), Charsets.UTF_8)
        }.getOrNull()
    }

    override fun write(token: String?) {
        if (token == null) {
            preferences.edit().remove(KEY_TOKEN).apply()
            return
        }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val cipherText = cipher.doFinal(token.toByteArray(Charsets.UTF_8))
        val encoded =
            Base64.encodeToString(cipher.iv, Base64.NO_WRAP) +
                SEPARATOR +
                Base64.encodeToString(cipherText, Base64.NO_WRAP)
        preferences.edit().putString(KEY_TOKEN, encoded).apply()
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec
                .Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "kdm.sync.bearer"
        const val PREFERENCES_NAME = "kdm.sync.tokens"
        const val KEY_TOKEN = "bearer_token"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val TAG_BITS = 128
        const val SEPARATOR = ":"
    }
}
