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
    private var auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val trackDao = AppDatabase.getDatabase(application).trackDao()
    
    init {
        auth = FirebaseAuth.getInstance()
    }
    
    private val _isLoggedIn = MutableStateFlow(sharedPrefs.getBoolean("is_logged_in", false))
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
    
    private val _userName = MutableStateFlow(sharedPrefs.getString("user_name", ""))
    val userName: StateFlow<String?> = _userName

    private val _userEmail = MutableStateFlow(sharedPrefs.getString("user_email", ""))
    val userEmail: StateFlow<String?> = _userEmail

    private val _userPhotoUrl = MutableStateFlow(sharedPrefs.getString("user_photo", ""))
    val userPhotoUrl: StateFlow<String?> = _userPhotoUrl

    // 0: Wifi only, 1: All Networks, 2: Mobile Data Only
    private val _downloadNetworkType = MutableStateFlow(sharedPrefs.getInt("download_network", 0))
    val downloadNetworkType: StateFlow<Int> = _downloadNetworkType

    private val _verificationId = MutableStateFlow<String?>(null)
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    val isGuestUser: Boolean
        get() = _userEmail.value == "guest@melodify.com" || _userName.value == "Guest User"

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
        // Requirement: Gmail (or any authenticated) login required for background playback
        return _isLoggedIn.value && !isGuestUser
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
        saveUserToPrefs(name, email, photoUrl)
        _isLoggedIn.value = true
        _userName.value = name
        _userEmail.value = email
        _userPhotoUrl.value = photoUrl
        if (!email.isNullOrBlank() && email != "guest@melodify.com") {
            com.ansh.ytmusicpreimum.data.TrackRepository.currentSyncUserId = email.replace(".", "_")
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
                auth.signOut() 
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
            
            _isLoggedIn.value = false
            _userTier.value = 2 // Keep PRO even on logout
            _subMonths.value = 0
            _userName.value = ""
            _userEmail.value = ""
            _userPhotoUrl.value = ""
        }
    }

    fun signUpWithEmail(email: String, pass: String, name: String) {
        _isLoading.value = true
        auth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
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
        auth.signInWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
            _isLoading.value = false
            if (task.isSuccessful) {
                val user = auth.currentUser
                onLoginSuccess(user?.displayName ?: "User", user?.email, user?.photoUrl?.toString())
            } else {
                Toast.makeText(getApplication(), "Login failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun sendOtp(phoneNumber: String, activity: Activity, onOtpSent: () -> Unit) {
        Toast.makeText(getApplication(), "Phone OTP is currently disabled.", Toast.LENGTH_SHORT).show()
    }

    fun verifyOtp(otp: String) {
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
