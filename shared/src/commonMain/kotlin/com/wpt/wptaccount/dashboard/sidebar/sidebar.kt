package com.wpt.wptaccount.dashboard.sidebar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape

@Composable
fun Sidebar(modifier: Modifier = Modifier){
    Box(
        modifier = modifier
            .background(Color(0xFFF1F5F9))
            .border(
                width = 2.dp,
                color = Color.Gray,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(3.dp),
    ){
        Column(
            modifier = Modifier.padding(3.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.padding(5.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Company Name")
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SideButton("Dashboard")
                SideButton("Voucher")
            }
        }
    }
}

@Composable
private fun SideButton(
    text: String,
){
    Button(
        onClick = {println("$text Button Click")},
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
    ){
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge
        )
    }
}
