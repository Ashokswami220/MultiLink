package com.example.multilink.ui.components.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

@Composable
fun DeleteSessionDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Session?") },
        text = { Text("This will permanently remove the session for everyone. Are you sure?") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) { Text("Delete") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun PauseSessionDialog(
    isPaused: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isPaused) "Resume Session?" else "Pause Session?") },
        text = {
            Text(
                if (isPaused) "Everyone will see live updates again." else "Live tracking will stop for everyone until you resume."
            )
        },
        confirmButton = {
            Button(onClick = onConfirm) { Text(if (isPaused) "Resume" else "Pause") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun SessionInfoDialog(
    title: String,
    start: String,
    end: String,
    activeUsers: Int,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Session Info") },
        text = {
            Column {
                Text("Title: $title", style = MaterialTheme.typography.bodyLarge)
                Text("Start: $start", style = MaterialTheme.typography.bodyMedium)
                Text("End: $end", style = MaterialTheme.typography.bodyMedium)
                Text("Active Users: $activeUsers", style = MaterialTheme.typography.bodyMedium)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}