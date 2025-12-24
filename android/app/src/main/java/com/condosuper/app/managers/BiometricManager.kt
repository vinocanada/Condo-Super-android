package com.condosuper.app.managers

import android.content.Context
import android.content.SharedPreferences
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class BiometricManager private constructor(private val context: Context) {
    companion object {
        @Volatile
        private var INSTANCE: BiometricManager? = null
        
        fun getInstance(context: Context): BiometricManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BiometricManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val prefs: SharedPreferences = 
        context.getSharedPreferences("biometric_prefs", Context.MODE_PRIVATE)
    
    private val _isBiometricEnabled = MutableStateFlow(prefs.getBoolean("biometricEnabled", false))
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()
    
    private val _biometricType = MutableStateFlow<BiometricType>(checkBiometricType())
    val biometricType: StateFlow<BiometricType> = _biometricType.asStateFlow()

    enum class BiometricType(val displayName: String, val iconName: String) {
        NONE("Biometric", "lock"),
        FINGERPRINT("Fingerprint", "fingerprint"),
        FACE("Face", "face")
    }

    private fun checkBiometricType(): BiometricType {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                // Check which type is available
                when {
                    biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS -> {
                        // Try to determine type (Android doesn't provide direct API, so we'll default to fingerprint)
                        BiometricType.FINGERPRINT
                    }
                    else -> BiometricType.NONE
                }
            }
            else -> BiometricType.NONE
        }
    }

    val isBiometricAvailable: Boolean
        get() = BiometricManager.from(context)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS

    suspend fun authenticate(activity: FragmentActivity, reason: String = "Log in to your account"): Boolean {
        return suspendCancellableCoroutine { continuation ->
            val executor = ContextCompat.getMainExecutor(context)
            val biometricPrompt = BiometricPrompt(
                activity,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        android.util.Log.d("BiometricManager", "Biometric authentication successful")
                        continuation.resume(true)
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        android.util.Log.e("BiometricManager", "Biometric authentication failed: $errString")
                        continuation.resume(false)
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        android.util.Log.e("BiometricManager", "Biometric authentication failed")
                        continuation.resume(false)
                    }
                }
            )

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric Authentication")
                .setSubtitle(reason)
                .setNegativeButtonText("Cancel")
                .build()

            biometricPrompt.authenticate(promptInfo)
        }
    }

    fun enableBiometric() {
        _isBiometricEnabled.value = true
        prefs.edit().putBoolean("biometricEnabled", true).apply()
    }

    fun disableBiometric() {
        _isBiometricEnabled.value = false
        prefs.edit().putBoolean("biometricEnabled", false).apply()
        clearCredentials()
    }

    fun storeCredentials(companyId: String, identifier: String, pin: String) {
        val credentials = "$companyId|$identifier|$pin"
        val encryptedPrefs = context.getSharedPreferences("biometric_creds", Context.MODE_PRIVATE)
        encryptedPrefs.edit().putString("credentials", credentials).apply()
        android.util.Log.d("BiometricManager", "Credentials stored for biometric login")
    }

    fun retrieveCredentials(): Triple<String, String, String>? {
        val encryptedPrefs = context.getSharedPreferences("biometric_creds", Context.MODE_PRIVATE)
        val credentials = encryptedPrefs.getString("credentials", null) ?: return null
        val parts = credentials.split("|")
        return if (parts.size == 3) {
            Triple(parts[0], parts[1], parts[2])
        } else {
            null
        }
    }

    fun clearCredentials() {
        val encryptedPrefs = context.getSharedPreferences("biometric_creds", Context.MODE_PRIVATE)
        encryptedPrefs.edit().clear().apply()
        android.util.Log.d("BiometricManager", "Biometric credentials cleared")
    }
}


