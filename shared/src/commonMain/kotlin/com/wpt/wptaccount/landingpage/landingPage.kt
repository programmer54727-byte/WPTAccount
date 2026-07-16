package com.wpt.wptaccount.landingpage

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import wptaccount.shared.generated.resources.*

@Composable
fun PreLogin(
    modifier: Modifier = Modifier,
    onSignUpClick: () -> Unit = {},
    onLoginClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = Color(0xFFF8F9FA))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Surface(
                modifier = Modifier.size(120.dp),
                shape = RoundedCornerShape(28.dp),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(Res.drawable.applogo),
                        contentDescription = "App Logo",
                        modifier = Modifier.size(80.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "WPT Accounts",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1C1E),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "One account for your global workspace. Secure, fast, and accessible everywhere.",
                fontSize = 17.sp,
                color = Color(0xFF49454F),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(0.9f),
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.weight(1.5f))

            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                val isWide = maxWidth > 600.dp
                val contentModifier = if (isWide) Modifier.width(400.dp) else Modifier.fillMaxWidth()

                Column(
                    modifier = contentModifier,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = onSignUpClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005AC1))
                    ) {
                        Text("Get Started", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = onLoginClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF005AC1))
                    ) {
                        Text("Log In", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF005AC1))
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}
