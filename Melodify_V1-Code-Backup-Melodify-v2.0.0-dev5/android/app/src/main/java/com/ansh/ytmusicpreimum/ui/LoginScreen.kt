package com.ansh.ytmusicpreimum.ui

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ansh.ytmusicpreimum.ui.theme.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

@Composable
fun LoginScreen(loginViewModel: LoginViewModel = viewModel(), onLoginSuccess: (String?, String?, String?) -> Unit) {
    val context = LocalContext.current
    val activity = context as Activity
    val webClientId = "854004439119-vtfd5l5rfet0rviabr9uqe3d4rbgucj1.apps.googleusercontent.com"
    
    var showEmailDialog by remember { mutableStateOf(false) }
    var showPhoneDialog by remember { mutableStateOf(false) }
    var showOtpDialog by remember { mutableStateOf(false) }

    // Google Sign-In options
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .requestIdToken(webClientId)
            .build()
    }
    
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                val displayName = account.displayName ?: account.givenName ?: "Google User"
                val email = account.email ?: "user@melodify.com"
                val photoUrl = account.photoUrl?.toString()

                // Authenticate with Firebase for Gmail cloud sync
                account.idToken?.let { token ->
                    try {
                        val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(token, null)
                        com.google.firebase.auth.FirebaseAuth.getInstance().signInWithCredential(credential)
                            .addOnSuccessListener { authResult ->
                                authResult.user?.uid?.let { uid ->
                                    com.ansh.ytmusicpreimum.data.TrackRepository.currentSyncUserId = uid
                                }
                            }
                    } catch (e: Exception) {
                        android.util.Log.e("LoginScreen", "Firebase credential sign-in error", e)
                    }
                }
                com.ansh.ytmusicpreimum.data.TrackRepository.currentSyncUserId = email.replace(".", "_")

                Toast.makeText(context, "Welcome, $displayName!", Toast.LENGTH_SHORT).show()
                onLoginSuccess(displayName, email, photoUrl)
            } else {
                Toast.makeText(context, "Sign-in cancelled", Toast.LENGTH_SHORT).show()
            }
        } catch (e: ApiException) {
            android.util.Log.e("LoginScreen", "Google sign in failed code: ${e.statusCode}", e)
            val lastAccount = GoogleSignIn.getLastSignedInAccount(context)
            if (lastAccount != null) {
                val displayName = lastAccount.displayName ?: lastAccount.givenName ?: "Google User"
                val email = lastAccount.email ?: "user@melodify.com"
                val photoUrl = lastAccount.photoUrl?.toString()
                Toast.makeText(context, "Welcome, $displayName!", Toast.LENGTH_SHORT).show()
                onLoginSuccess(displayName, email, photoUrl)
            } else if (e.statusCode != 12501) { // 12501 is user cancelled
                Toast.makeText(context, "Sign-in failed (${e.statusCode}). You can also Continue as Guest.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(CyberDarkBg, Color.Black)
                )
            )
    ) {
        // Decorative background elements
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = (-100).dp, y = (-50).dp)
                .background(NeonRed.copy(alpha = 0.15f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 50.dp, y = 50.dp)
                .background(AccentPurple.copy(alpha = 0.15f), CircleShape)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(28.dp)),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                border = androidx.compose.foundation.BorderStroke(2.dp, Brush.linearGradient(listOf(NeonRed, AccentPurple)))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_media_play),
                        contentDescription = null,
                        modifier = Modifier.size(50.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Melodify",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp
                ),
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Elevate your music experience",
                style = MaterialTheme.typography.bodyMedium,
                color = GrayText,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            LoginButton(
                text = "Continue with Google",
                icon = null,
                containerColor = Color.White,
                contentColor = Color.Black,
                onClick = { 
                    launcher.launch(googleSignInClient.signInIntent)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            LoginButton(
                text = "Continue with Email",
                icon = Icons.Default.Email,
                containerColor = CyberSurface,
                contentColor = Color.White,
                onClick = { showEmailDialog = true }
            )

            Spacer(modifier = Modifier.height(16.dp))

            LoginButton(
                text = "Continue with Phone (Coming Soon)",
                icon = Icons.Default.Phone,
                containerColor = CyberSurface.copy(alpha = 0.5f),
                contentColor = Color.Gray,
                onClick = { 
                    Toast.makeText(context, "Phone login is currently locked.", Toast.LENGTH_SHORT).show()
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(onClick = { 
                onLoginSuccess("Guest User", "guest@melodify.com", null)
            }) {
                Text(
                    "Continue as Guest",
                    color = NeonRed,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }

    if (showEmailDialog) {
        EmailLoginDialog(
            onDismiss = { showEmailDialog = false },
            onLogin = { email, pass -> 
                loginViewModel.signInWithEmail(email, pass)
                showEmailDialog = false
            },
            onSignUp = { email, pass, name -> 
            loginViewModel.                                     signUpWithEmail(email, pass, name)
                showEmailDialog = false
            }
        )
    }

    if (showPhoneDialog) {
        PhoneLoginDialog(
            onDismiss = { showPhoneDialog = false },
            onSendOtp = { phone -> 
                loginViewModel.sendOtp(phone, activity) {
                    showPhoneDialog = false
                    showOtpDialog = true
                }
            }
        )
    }

    if (showOtpDialog) {
        OtpDialog(
            onDismiss = { showOtpDialog = false },
            onVerify = { otp ->
                loginViewModel.verifyOtp(otp)
                showOtpDialog = false
            }
        )
    }
}

@Composable
fun EmailLoginDialog(onDismiss: () -> Unit, onLogin: (String, String) -> Unit, onSignUp: (String, String, String) -> Unit) {
    var isSignUp by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberSurface,
        title = { Text(if (isSignUp) "Create Account" else "Welcome Back", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (isSignUp) {
                    AuthTextField(value = name, onValueChange = { name = it }, label = "Full Name")
                }
                AuthTextField(value = email, onValueChange = { email = it }, label = "Email Address")
                AuthTextField(
                    value = password, 
                    onValueChange = { password = it }, 
                    label = "Password",
                    isPassword = true
                )
                Text(
                    text = if (isSignUp) "Already have an account? Login" else "Don't have an account? Sign Up",
                    color = NeonRed,
                    modifier = Modifier.clickable { isSignUp = !isSignUp }.padding(vertical = 4.dp),
                    fontSize = 14.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    if (isSignUp) onSignUp(email, password, name) else onLogin(email, password)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isSignUp) "Sign Up" else "Login")
            }
        }
    )
}

@Composable
fun AuthTextField(
    value: String, 
    onValueChange: (String) -> Unit, 
    label: String, 
    isPassword: Boolean = false
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = GrayText) },
        modifier = Modifier.fillMaxWidth(),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Email),
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White.copy(alpha = 0.05f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedIndicatorColor = NeonRed,
            unfocusedIndicatorColor = GrayText.copy(alpha = 0.5f)
        )
    )
}

@Composable
fun PhoneLoginDialog(onDismiss: () -> Unit, onSendOtp: (String) -> Unit) {
    var phone by remember { mutableStateOf("") }
    var countryCode by remember { mutableStateOf("+91") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberSurface,
        title = { Text("Phone Login", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Enter your mobile number", color = GrayText, fontSize = 14.sp)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Country Code Picker (Simplified)
                    Surface(
                        modifier = Modifier.height(56.dp).width(60.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.1f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(countryCode, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    TextField(
                        value = phone,
                        onValueChange = { if (it.length <= 10) phone = it.filter { char -> char.isDigit() } },
                        placeholder = { Text("00000 00000", color = GrayText) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White.copy(alpha = 0.1f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.1f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (phone.length == 10) {
                        onSendOtp("$countryCode$phone")
                    }
                },
                enabled = phone.length == 10
            ) {
                Text("Send OTP")
            }
        }
    )
}

@Composable
fun OtpDialog(onDismiss: () -> Unit, onVerify: (String) -> Unit) {
    var otp by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberSurface,
        title = { Text("Verify OTP", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Enter the 6-digit code sent to your phone", color = GrayText, fontSize = 14.sp)
                TextField(
                    value = otp,
                    onValueChange = { if (it.length <= 6) otp = it.filter { char -> char.isDigit() } },
                    placeholder = { Text("000000", color = GrayText) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White.copy(alpha = 0.1f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.1f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedIndicatorColor = NeonRed
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (otp.length == 6) onVerify(otp) },
                enabled = otp.length == 6,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Verify & Login")
            }
        }
    )
}

@Composable
fun LoginButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
            }
            Text(
                text,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
            )
        }
    }
}
