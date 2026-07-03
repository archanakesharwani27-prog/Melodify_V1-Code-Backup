package com.ansh.ytmusicpreimum.ui

import android.app.Application
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ansh.ytmusicpreimum.data.AppDatabase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val sharedPrefs = application.getSharedPreferences("auth", Context.MODE_PRIVATE)
    private var auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firebaseAnalytics = FirebaseAnalytics.getInstance(application)
    private val trackDao = AppDatabase.getDatabase(application).trackDao()
    
    init {
        auth = FirebaseAuth.getInstance()
    }
    
    private val _isLoggedIn = MutableStateFlow(sharedPrefs.getBoolean("is_logged_in", false))
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    private val _dailyDownloadsUsed = MutableStateFlow(sharedPrefs.getInt("daily_downloads_used", 0))
    val dailyDownloadsUsed: StateFlow<Int> = _dailyDownloadsUsed

    private val _lastLimitReset = MutableStateFlow(sharedPrefs.getLong("last_limit_reset", 0L))
    
    private val _userName = MutableStateFlow(sharedPrefs.getString("user_name", ""))
    val userName: StateFlow<String?> = _userName

    private val _userEmail = MutableStateFlow(sharedPrefs.getString("user_email", ""))
    val userEmail: StateFlow<String?> = _userEmail

    private val _userPhotoUrl = MutableStateFlow(sharedPrefs.getString("user_photo", ""))
    val userPhotoUrl: StateFlow<String?> = _userPhotoUrl

    // 0: Wifi only, 1: All Networks
    private val _downloadNetworkType = MutableStateFlow(sharedPrefs.getInt("download_network", 0))
    val downloadNetworkType: StateFlow<Int> = _downloadNetworkType

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    val isGuestUser: Boolean
        get() = _userEmail.value == "guest@melodify.com" || _userName.value == "Guest User"

    init {
        checkAndResetLimits()
    }

    private fun checkAndResetLimits() {
        val now = System.currentTimeMillis()
        val lastReset = _lastLimitReset.value
        val calendar = java.util.Calendar.getInstance()
        val lastResetCal = java.util.Calendar.getInstance().apply { timeInMillis = lastReset }
        
        if (calendar.get(java.util.Calendar.DAY_OF_YEAR) != lastResetCal.get(java.util.Calendar.DAY_OF_YEAR) ||
            calendar.get(java.util.Calendar.YEAR) != lastResetCal.get(java.util.Calendar.YEAR)) {
            _dailyDownloadsUsed.value = 0
            sharedPrefs.edit().apply {
                putInt("daily_downloads_used", 0)
                putLong("last_limit_reset", now)
                apply()
            }
            _lastLimitReset.value = now
        }
    }

    fun canDownload(): Boolean = true
    fun useDownload(): Boolean = true
    fun canUse8D(): Boolean = true
    fun canCacheLyrics(): Boolean = true
    fun userTier(): Int = 2

    fun shouldShowNameIntro(): Boolean {
        return _userName.value?.isNotEmpty() == true
    }

    fun onLoginSuccess(name: String?, email: String?, photoUrl: String?) {
        saveUserToPrefs(name, email, photoUrl)
        _isLoggedIn.value = true
        _userName.value = name
        _userEmail.value = email
        _userPhotoUrl.value = photoUrl

        // LOG ANALYTICS EVENT
        try {
            val bundle = Bundle().apply {
                putString(FirebaseAnalytics.Param.METHOD, if (email == "guest@melodify.com") "guest" else "account")
                putString("user_name", name)
                putString("user_email", email)
            }
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.LOGIN, bundle)
            firebaseAnalytics.setUserId(email?.replace(".", "_"))
        } catch (e: Exception) {
            android.util.Log.e("Analytics", "Failed to log login event", e)
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
                context?.let {
                    val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN).build()
                    com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(it, gso).signOut()
                }
            } catch(e: Exception) {}
            
            trackDao.clearTracks()
            trackDao.clearPlaylists()
            trackDao.clearPlaylistCrossRefs()
            trackDao.clearHistory()
            
            sharedPrefs.edit().clear().apply()
            
            _isLoggedIn.value = false
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
                val bundle = Bundle().apply {
                    putString(FirebaseAnalytics.Param.METHOD, "email")
                }
                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SIGN_UP, bundle)
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
