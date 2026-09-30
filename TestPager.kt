package com.example.multilink
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.pager.PageSize
import androidx.compose.runtime.Composable

@Composable
fun Test() {
    val pagerState = rememberPagerState(pageCount = { 3 })
    HorizontalPager(state = pagerState, pageSize = PageSize.Fixed(androidx.compose.ui.unit.dp(280f))) { }
}
