package com.condosuper.app.managers

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.security.SecureRandom

class EncryptionManager private constructor(context: Context) {
    companion object {
        @Volatile
        private var INSTANCE: EncryptionManager? = null
        
        fun getInstance(context: Context): EncryptionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: EncryptionManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val masterKey: MasterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        "encryption_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private fun getCompanyKey(companyId: String): SecretKey {
        val keyString = encryptedPrefs.getString("key_$companyId", null)
        return if (keyString != null) {
            val keyBytes = Base64.decode(keyString, Base64.DEFAULT)
            SecretKeySpec(keyBytes, "AES")
        } else {
            // Generate new key for company
            val keyGenerator = KeyGenerator.getInstance("AES")
            keyGenerator.init(256)
            val newKey = keyGenerator.generateKey()
            val keyString = Base64.encodeToString(newKey.encoded, Base64.DEFAULT)
            encryptedPrefs.edit().putString("key_$companyId", keyString).apply()
            newKey
        }
    }

    fun encrypt(plainText: String, companyId: String): String? {
        if (plainText.isEmpty()) return ""
        
        return try {
            val key = getCompanyKey(companyId)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key)
            
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val iv = cipher.iv
            
            // Combine IV + encrypted data
            val combined = ByteArray(iv.size + encryptedBytes.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)
            
            Base64.encodeToString(combined, Base64.DEFAULT)
        } catch (e: Exception) {
            android.util.Log.e("EncryptionManager", "Encryption error", e)
            null
        }
    }

    fun decrypt(encryptedText: String, companyId: String): String? {
        if (encryptedText.isEmpty()) return ""
        
        return try {
            val combined = Base64.decode(encryptedText, Base64.DEFAULT)
            
            // Extract IV (first 12 bytes for GCM)
            val iv = ByteArray(12)
            System.arraycopy(combined, 0, iv, 0, 12)
            
            // Extract encrypted data
            val encryptedBytes = ByteArray(combined.size - 12)
            System.arraycopy(combined, 12, encryptedBytes, 0, encryptedBytes.size)
            
            val key = getCompanyKey(companyId)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)
            
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            // Decryption failed, might be unencrypted legacy message
            android.util.Log.w("EncryptionManager", "Decryption failed, returning original", e)
            encryptedText
        }
    }

    fun encryptURL(url: String?, companyId: String): String? {
        return url?.let { encrypt(it, companyId) }
    }

    fun decryptURL(encryptedURL: String?, companyId: String): String? {
        return encryptedURL?.let { decrypt(it, companyId) }
    }
}


