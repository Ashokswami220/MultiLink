package com.example.multilink.ui.main

import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.Composable

@Composable
fun Test() {
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val screenWidth = with(density) { windowInfo.containerSize.width.toDp() }
    val promoPageWidth = screenWidth * 0.9f
}
