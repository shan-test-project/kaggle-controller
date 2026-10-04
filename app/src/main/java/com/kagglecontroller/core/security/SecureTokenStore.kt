package com.kagglecontroller.core.security

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
 * Stores the Kaggle token encrypted with a non-exportable AES-256-GCM key held in the Android
 * Keystore. Only ciphertext touches disk, and the file is excluded from cloud backup.
 * (We avoid the deprecated androidx.security-crypto library on purpose.)
 */
class SecureTokenStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("kc_secure", Context.MODE_PRIVATE)

    @Volatile private var cached: String? = null
    @Volatile private var loaded = false

    fun save(token: String) {
        val cipher = Cipher.getInstance(TRANSFORM)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(token.trim().toByteArray(Charsets.UTF_8))
        val payload = Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" +
            Base64.encodeToString(encrypted, Base64.NO_WRAP)
        prefs.edit().putString(KEY_TOKEN, payload).apply()
        cached = token.trim()
        loaded = true
    }

    fun load(): String? {
        if (loaded) return cached
        val payload = prefs.getString(KEY_TOKEN, null)
        cached = if (payload == null) null else try {
            val (iv, data) = payload.split(":", limit = 2)
            val cipher = Cipher.getInstance(TRANSFORM)
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)))
            String(cipher.doFinal(Base64.decode(data, Base64.NO_WRAP)), Charsets.UTF_8)
        } catch (e: Exception) {
            // Key invalidated (e.g. lock-screen change) or corrupted: treat as signed out.
            clear()
            null
        }
        loaded = true
        return cached
    }

    fun clear() {
        prefs.edit().remove(KEY_TOKEN).apply()
        cached = null
        loaded = true
    }

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    private companion object {
        const val ALIAS = "kaggle_controller_token_key"
        const val KEY_TOKEN = "token"
        const val TRANSFORM = "AES/GCM/NoPadding"
    }
}
