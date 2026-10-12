package com.ansh.ytmusicpreimum.ui

import android.app.Activity
import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ansh.ytmusicpreimum.data.AppDatabase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val sharedPrefs = application.getSharedPreferences("auth", Context.MODE_PRIVATE)
    private var auth: FirebaseAuth? = null
    private val trackDao = AppDatabase.getDatabase(application).trackDao()
    
    init {
        try {
            auth = FirebaseAuth.getInstance()
        } catch (t: Throwable) {
            android.util.Log.w("LoginViewModel", "FirebaseAuth init skipped or failed", t)
        }
    }
    
    private val _isLoggedIn = MutableStateFlow(sharedPrefs.getBoolean("is_logged_in", true))
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    // All users are PRO by default now
    private val _userTier = MutableStateFlow(2)
    val userTier: StateFlow<Int> = _userTier

    // Freemium Limits
    private val _freeDownloadsLeft = MutableStateFlow(sharedPrefs.getInt("free_downloads", 5))
    val freeDownloadsLeft: StateFlow<Int> = _freeDownloadsLeft

    private val _dailyDownloadsUsed = MutableStateFlow(sharedPrefs.getInt("daily_downloads_used", 0))
    val dailyDownloadsUsed: StateFlow<Int> = _dailyDownloadsUsed

    private val _lyricsCount = MutableStateFlow(sharedPrefs.getInt("lyrics_count", 0))
    val lyricsCount: StateFlow<Int> = _lyricsCount

    private val _eightDCount = MutableStateFlow(sharedPrefs.getInt("eight_d_count", 0))
    private val _lastLimitReset = MutableStateFlow(sharedPrefs.getLong("last_limit_reset", 0L))

    private val _subMonths = MutableStateFlow(sharedPrefs.getInt("sub_months", 0))
    val subMonths: StateFlow<Int> = _subMonths

    private val _subDate = MutableStateFlow(sharedPrefs.getLong("sub_date", 0L))
    
    private val _userName = MutableStateFlow<String?>(
        sharedPrefs.getString("user_name", null)?.takeIf { it.isNotBlank() } ?: "Guest User"
    )
    val userName: StateFlow<String?> = _userName

    private val _userEmail = MutableStateFlow<String?>(
        sharedPrefs.getString("user_email", null)?.takeIf { it.isNotBlank() } ?: "guest@melodify.com"
    )
    val userEmail: StateFlow<String?> = _userEmail

    private val _userPhotoUrl = MutableStateFlow<String?>(sharedPrefs.getString("user_photo", ""))
    val userPhotoUrl: StateFlow<String?> = _userPhotoUrl

    // 0: Wifi only, 1: All Networks, 2: Mobile Data Only
    private val _downloadNetworkType = MutableStateFlow(sharedPrefs.getInt("download_network", 0))
    val downloadNetworkType: StateFlow<Int> = _downloadNetworkType

    private val _verificationId = MutableStateFlow<String?>(null)
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    val isGuestUser: Boolean
        get() = _userEmail.value == "guest@melodify.com" || _userName.value == "Guest User" || _userEmail.value.isNullOrBlank()

    init {
        checkAndResetLimits()
        val email = _userEmail.value
        if (!email.isNullOrBlank() && email != "guest@melodify.com") {
            com.ansh.ytmusicpreimum.data.TrackRepository.currentSyncUserId = email.replace(".", "_")
        }
    }

    private fun checkAndResetLimits() {
        val now = System.currentTimeMillis()
        val lastReset = _lastLimitReset.value
        val calendar = java.util.Calendar.getInstance()
        val lastResetCal = java.util.Calendar.getInstance().apply { timeInMillis = lastReset }
        
        if (calendar.get(java.util.Calendar.DAY_OF_YEAR) != lastResetCal.get(java.util.Calendar.DAY_OF_YEAR) ||
            calendar.get(java.util.Calendar.YEAR) != lastResetCal.get(java.util.Calendar.YEAR)) {
            _eightDCount.value = 0
            _dailyDownloadsUsed.value = 0
            sharedPrefs.edit().apply {
                putInt("eight_d_count", 0)
                putInt("daily_downloads_used", 0)
                putLong("last_limit_reset", now)
                apply()
            }
            _lastLimitReset.value = now
        }

        if (calendar.get(java.util.Calendar.MONTH) != lastResetCal.get(java.util.Calendar.MONTH) ||
            calendar.get(java.util.Calendar.YEAR) != lastResetCal.get(java.util.Calendar.YEAR)) {
            _freeDownloadsLeft.value = 5 
            _lyricsCount.value = 0
            sharedPrefs.edit().apply {
                putInt("free_downloads", 5)
                putInt("lyrics_count", 0)
                apply()
            }
        }
    }

    fun canDownload(): Boolean {
        return true
    }

    fun useDownload(): Boolean {
        return true
    }

    fun isBackgroundPlaybackAllowed(): Boolean {
        return true
    }

    fun canUse8D(): Boolean {
        return true
    }

    fun canCacheLyrics(): Boolean {
        return true
    }

    fun upgradeTier(tier: Int, months: Int) {
        // No-op: Everyone is PRO
    }

    fun shouldShowNameIntro(): Boolean {
        if (_userTier.value < 2) return false
        val now = System.currentTimeMillis()
        val subStart = _subDate.value
        val daysElapsed = (now - subStart) / (1000 * 60 * 60 * 24)
        
        val allowedDays = when(_subMonths.value) {
            1 -> 15
            6 -> 180
            12 -> 365
            else -> 0
        }
        return daysElapsed < allowedDays
    }

    fun onLoginSuccess(name: String?, email: String?, photoUrl: String?) {
        val finalName = name ?: "Guest User"
        val finalEmail = email ?: "guest@melodify.com"
        saveUserToPrefs(finalName, finalEmail, photoUrl)
        _isLoggedIn.value = true
        _userName.value = finalName
        _userEmail.value = finalEmail
        _userPhotoUrl.value = photoUrl
        if (finalEmail.isNotBlank() && finalEmail != "guest@melodify.com") {
            com.ansh.ytmusicpreimum.data.TrackRepository.currentSyncUserId = finalEmail.replace(".", "_")
        }
    }

    private fun saveUserToPrefs(name: String?, email: String?, photoUrl: String?) {
        sharedPrefs.edit().apply {
            putBoolean("is_logged_in", true)
            putString("user_name", name)
            putString("user_email", email)
            putString("user_photo", photoUrl)
            apply()
        }
    }

    fun logout(context: Context? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            try { 
                auth?.signOut() 
                com.ansh.ytmusicpreimum.data.TrackRepository.currentSyncUserId = null
                context?.let {
                    val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN).build()
                    com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(it, gso).signOut()
                }
            } catch(e: Exception) {}
            
            // Clear local database
            trackDao.clearTracks()
            trackDao.clearPlaylists()
            trackDao.clearPlaylistCrossRefs()
            trackDao.clearHistory()
            
            sharedPrefs.edit().clear().apply()
            
            _isLoggedIn.value = true
            _userTier.value = 2 // Keep PRO even on logout
            _subMonths.value = 0
            _userName.value = "Guest User"
            _userEmail.value = "guest@melodify.com"
            _userPhotoUrl.value = ""
        }
    }

    fun signUpWithEmail(email: String, pass: String, name: String) {
        _isLoading.value = true
        val firebaseAuth = auth
        if (firebaseAuth == null) {
            _isLoading.value = false
            Toast.makeText(getApplication(), "Authentication service unavailable", Toast.LENGTH_SHORT).show()
            return
        }
        firebaseAuth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
            _isLoading.value = false
            if (task.isSuccessful) {
                onLoginSuccess(name, email, null)
            } else {
                Toast.makeText(getApplication(), "Signup failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun signInWithEmail(email: String, pass: String) {
        _isLoading.value = true
        val firebaseAuth = auth
        if (firebaseAuth == null) {
            _isLoading.value = false
            Toast.makeText(getApplication(), "Authentication service unavailable", Toast.LENGTH_SHORT).show()
            return
        }
        firebaseAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
            _isLoading.value = false
            if (task.isSuccessful) {
                val user = firebaseAuth.currentUser
                onLoginSuccess(user?.displayName ?: "User", user?.email, user?.photoUrl?.toString())
            } else {
                Toast.makeText(getApplication(), "Login failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    fun sendOtp(phoneNumber: String, activity: Activity, onOtpSent: () -> Unit, onError: (String) -> Unit = {}) {
        android.util.Log.i("LoginViewModel", "sendOtp invoked for $phoneNumber with activity: $activity")
        _isLoading.value = true
        val firebaseAuth = auth
        if (firebaseAuth == null) {
            _isLoading.value = false
            val msg = "Authentication service unavailable"
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                Toast.makeText(getApplication(), msg, Toast.LENGTH_SHORT).show()
                onError(msg)
            }
            return
        }

        val options = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    android.util.Log.i("LoginViewModel", "onVerificationCompleted instant sign-in")
                    _isLoading.value = false
                    firebaseAuth.signInWithCredential(credential).addOnCompleteListener { task ->
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            if (task.isSuccessful) {
                                val user = firebaseAuth.currentUser
                                val phone = user?.phoneNumber ?: phoneNumber
                                onLoginSuccess(phone, phone, null)
                            } else {
                                val msg = "Instant verification failed: ${task.exception?.message}"
                                Toast.makeText(getApplication(), msg, Toast.LENGTH_LONG).show()
                                onError(msg)
                            }
                        }
                    }
                }

                override fun onVerificationFailed(e: com.google.firebase.FirebaseException) {
                    android.util.Log.e("LoginViewModel", "onVerificationFailed: ${e.message}", e)
                    _isLoading.value = false
                    val errorMsg = when {
                        e.message?.contains("BILLING_NOT_ENABLED") == true ->
                            "Real SMS requires Firebase Blaze plan. Please add this number under 'Phone numbers for testing' in Firebase Console (Free)."
                        e.message?.contains("17006") == true || e.message?.contains("SMS unable to be sent until this region enabled") == true ->
                            "SMS Region Blocked: Please enable India (+91) in Firebase SMS Region Policy or use a Test Phone Number."
                        e.message?.contains("17010") == true || e.message?.contains("Too many requests") == true ->
                            "Too many requests. Please try again later or use a Test Number."
                        e.message?.contains("Play Integrity") == true || e.message?.contains("app identifier") == true ->
                            "Device verification failed. Ensure SHA-1/SHA-256 is registered in Firebase Console."
                        else -> e.localizedMessage ?: (e.message ?: "OTP verification failed")
                    }
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        Toast.makeText(getApplication(), errorMsg, Toast.LENGTH_LONG).show()
                        onError(errorMsg)
                    }
                }

                override fun onCodeSent(verId: String, token: PhoneAuthProvider.ForceResendingToken) {
                    android.util.Log.i("LoginViewModel", "onCodeSent: verificationId=$verId")
                    _isLoading.value = false
                    verificationId = verId
                    resendToken = token
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        Toast.makeText(getApplication(), "OTP sent to $phoneNumber", Toast.LENGTH_SHORT).show()
                        onOtpSent()
                    }
                }
            })
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyOtp(otp: String, onError: (String) -> Unit = {}) {
        android.util.Log.i("LoginViewModel", "verifyOtp invoked with code: $otp")
        val verId = verificationId
        if (verId == null) {
            val msg = "Verification session expired. Please resend OTP."
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                Toast.makeText(getApplication(), msg, Toast.LENGTH_SHORT).show()
                onError(msg)
            }
            return
        }
        _isLoading.value = true
        val firebaseAuth = auth
        if (firebaseAuth == null) {
            _isLoading.value = false
            val msg = "Authentication service unavailable"
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                Toast.makeText(getApplication(), msg, Toast.LENGTH_SHORT).show()
                onError(msg)
            }
            return
        }

        val credential = PhoneAuthProvider.getCredential(verId, otp)
        firebaseAuth.signInWithCredential(credential).addOnCompleteListener { task ->
            _isLoading.value = false
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                if (task.isSuccessful) {
                    val user = firebaseAuth.currentUser
                    val phone = user?.phoneNumber ?: "Phone User"
                    onLoginSuccess(phone, phone, null)
                } else {
                    val msg = task.exception?.message ?: "Invalid OTP entered"
                    Toast.makeText(getApplication(), "Verification failed: $msg", Toast.LENGTH_LONG).show()
                    onError(msg)
                }
            }
        }
    }

    fun updateProfile(name: String, photoUrl: String?) {
        sharedPrefs.edit().apply {
            putString("user_name", name)
            putString("user_photo", photoUrl)
            apply()
        }
        _userName.value = name
        _userPhotoUrl.value = photoUrl
    }

    fun updateDownloadNetwork(type: Int) {
        _downloadNetworkType.value = type
        sharedPrefs.edit().putInt("download_network", type).apply()
    }
}
