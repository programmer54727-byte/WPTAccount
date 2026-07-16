package com.wpt.wptaccount.signupform

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wpt.wptaccount.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUp(
    onBackClick: () -> Unit = {},
    onSignUpSuccess: () -> Unit = {},
    onLoginClick: () -> Unit = {}
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var isOtpSent by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Account", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF8F9FA)
                )
            )
        },
        containerColor = Color(0xFFF8F9FA)
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val isWide = maxWidth > 600.dp
            val contentModifier = if (isWide) Modifier.width(450.dp) else Modifier.fillMaxWidth()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(modifier = contentModifier) {
                    Column {
                        Text(
                            text = if (!isOtpSent) "Welcome to WPT" else "Verify OTP",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1C1E)
                        )
                        Text(
                            text = if (!isOtpSent) 
                                "Fill in your details to get started" 
                            else 
                                "An 8-digit OTP has been sent to $email",
                            fontSize = 16.sp,
                            color = Color(0xFF49454F),
                            modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
                        )

                        if (!isOtpSent) {
                            // Name Field
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Full Name") },
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Email Field
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("Email Address") },
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Password Field
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Password") },
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = null
                                        )
                                    }
                                },
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                            )
                        } else {
                            // OTP Field
                            OutlinedTextField(
                                value = otp,
                                onValueChange = { if (it.length <= 8) otp = it },
                                label = { Text("8-Digit OTP") },
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        if (errorMessage != null) {
                            Text(
                                text = errorMessage!!,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    isLoading = true
                                    errorMessage = null
                                    try {
                                        if (!isOtpSent) {
                                            if (name.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
                                                supabase.auth.signUpWith(Email) {
                                                    this.email = email
                                                    this.password = password
                                                    data = buildJsonObject {
                                                        put("full_name", name)
                                                    }
                                                }
                                                isOtpSent = true
                                            } else {
                                                errorMessage = "Please fill in all fields"
                                            }
                                        } else {
                                            if (otp.length == 8) {
                                                supabase.auth.verifyEmailOtp(
                                                    type = OtpType.Email.SIGNUP,
                                                    email = email,
                                                    token = otp
                                                )
                                                onSignUpSuccess()
                                            } else {
                                                errorMessage = "OTP must be 8 digits"
                                            }
                                        }
                                    } catch (e: Exception) {
                                        errorMessage = e.message ?: "Action failed"
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            },
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005AC1))
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    if (!isOtpSent) "Register" else "Verify OTP",
                                    fontSize = 18.sp, 
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        if (isOtpSent) {
                            TextButton(
                                onClick = { isOtpSent = false; otp = "" },
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Text("Change Details", color = Color(0xFF005AC1))
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        if (!isOtpSent) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Already have an account?", color = Color(0xFF49454F))
                                TextButton(onClick = onLoginClick) {
                                    Text("Log In", color = Color(0xFF005AC1), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
