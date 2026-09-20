package com.wpt.wptaccount

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

object WptColors {
    val PrimaryAccent = Color(0xFF7C4DFF)
    val NavigationBg = Color(0xFFF5F3F8)
    val AppSurface = Color(0xFFF8F9FA)
    val CardBackground = Color.White
    val PrimaryText = Color(0xFF1D1B20)
    val SecondaryText = Color(0xFF49454F)
    val Divider = Color(0xFFE0E0E0)
    val Error = Color(0xFFD32F2F)
}

@Composable
fun FormSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = WptColors.CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max)) {
            // Mandatory left vertical accent strip in primary purple
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(5.dp)
                    .background(WptColors.PrimaryAccent)
            )
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = WptColors.PrimaryAccent
                )
                content()
            }
        }
    }
}
