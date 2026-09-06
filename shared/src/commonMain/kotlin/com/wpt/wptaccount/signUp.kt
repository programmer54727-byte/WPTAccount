package com.wpt.wptaccount

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.OtpType
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.jetbrains.compose.resources.painterResource
import wptaccount.shared.generated.resources.Res
import wptaccount.shared.generated.resources.applogo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUp(
    onBackClick: () -> Unit,
    onSignUpSuccess: () -> Unit
) {
    var fullName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    // var otpCode by rememberSaveable { mutableStateOf("") }
    // var isVerifying by rememberSaveable { mutableStateOf(false) }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current

    fun performSignUp() {
        if (email.isBlank() || password.isBlank() || fullName.isBlank()) {
            errorMessage = "Please fill all fields"
            return
        }
        
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                supabase.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                    data = buildJsonObject {
                        put("full_name", fullName)
                    }
                }
                // isVerifying = true
                onSignUpSuccess()
            } catch (e: Exception) {
                println("Signup error: ${e.message}")
                errorMessage = if (e.message?.contains("user_already_exists", ignoreCase = true) == true) {
                    "An account with this email already exists."
                } else {
                    "Signup failed. Please try again."
                }
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Account") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Image(
                painter = painterResource(Res.drawable.applogo),
                contentDescription = "Logo",
                modifier = Modifier.size(80.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Join WPT Account",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            /*
            // OTP verification UI commented out for now
            if (isVerifying) {
                Text(
                    text = "We've sent a 6-digit verification code to $email",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                OutlinedTextField(
                    value = otpCode,
                    onValueChange = { if (it.length <= 6) otpCode = it },
                    label = { Text("Verification Code") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
                )

                errorMessage?.let {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        if (otpCode.length < 6) {
                            errorMessage = "Please enter the 6-digit code"
                            return@Button
                        }

                        scope.launch {
                            isLoading = true
                            errorMessage = null
                            try {
                                supabase.auth.verifyEmailOtp(
                                    type = OtpType.Email.SIGNUP,
                                    email = email,
                                    token = otpCode
                                )
                                onSignUpSuccess()
                            } catch (e: Exception) {
                                errorMessage = e.message ?: "Verification failed"
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Verify & Continue")
                    }
                }

                TextButton(
                    onClick = { isVerifying = false },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("Change Email")
                }

            } else {
            */
                Spacer(modifier = Modifier.height(32.dp))
                
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name") },
                    modifier = Modifier.widthIn(max = 490.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    modifier = Modifier.widthIn(max = 490.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    modifier = Modifier.widthIn(max = 490.dp),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { 
                            focusManager.clearFocus()
                            performSignUp()
                        }
                    )
                )
                
                errorMessage?.let {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = { performSignUp() },
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Sign Up")
                    }
                }
            // }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
