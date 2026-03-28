package com.example.multilink.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun NoInternetBanner(
    isVisible: Boolean,
    errorTrigger: Int = 0
) {
    val offsetX = remember { Animatable(0f) }

    LaunchedEffect(errorTrigger) {
        if (errorTrigger > 0) {
            offsetX.animateTo(20f, tween(40, easing = LinearEasing))
            offsetX.animateTo(-20f, tween(40, easing = LinearEasing))
            offsetX.animateTo(20f, tween(40, easing = LinearEasing))
            offsetX.animateTo(-10f, tween(40, easing = LinearEasing))
            offsetX.animateTo(0f, tween(40, easing = LinearEasing))
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        Surface(
            color = Color(0xFFD32F2F),
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(
                        horizontal = 16.dp, vertical = 12.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

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