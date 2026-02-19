package com.example.multilink.ui.viewmodel

data class SessionData(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val fromLocation: String,
    val toLocation: String,
    val durationVal: String,
    val durationUnit: String,
    val maxPeople: String,
    val status: String = "Live"
)

data class MultiLinkUiState(
    val sessions: List<SessionData> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)