package com.example.multilink.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NoInternetBanner(
    isVisible: Boolean
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        Surface(
            color = Color(0xFFD32F2F), // PhonePe Red
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    // This pushes the content below the system status bar
                    // while keeping the red background behind the status bar icons.
                    .statusBarsPadding()
                    .padding(
                        horizontal = 16.dp, vertical = 12.dp
                    ), // Increased vertical padding for height
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Icon (Changed to WifiOff or CloudOff for better context)
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp) // Slightly larger icon
                )

                Spacer(modifier = Modifier.width(16.dp))

                // 2. Informative Text (Two lines)
                Column {
                    Text(
                        text = "No Internet Connection",
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Live tracking & updates are paused.",
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }
    }
}