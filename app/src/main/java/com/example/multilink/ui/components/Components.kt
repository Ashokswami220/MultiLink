package com.example.multilink.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.net.toUri
import com.example.multilink.repo.RealtimeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun PauseWarningDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = { Text("Pause Session?") },
        text = {
            Text(
                "Pausing will prevent users from entering and stop all background tracking. You can resume anytime."
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Yes, Pause")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

class SessionActionHandler(
    private val context: Context,
    private val repository: RealtimeRepository,
    private val scope: CoroutineScope
) {

    fun onCall(phoneNumber: String?) {
        if (!phoneNumber.isNullOrEmpty()) {
            val intent = Intent(Intent.ACTION_DIAL, "tel:$phoneNumber".toUri())
            context.startActivity(intent)
        } else {
            Toast.makeText(context, "No phone number available", Toast.LENGTH_SHORT)
                .show()
        }
    }

    fun onRemoveUser(
        sessionId: String, userId: String, userName: String, onSuccess: () -> Unit = {}
    ) {
        scope.launch {
            repository.removeUser(sessionId, userId)
            Toast.makeText(context, "Removed $userName", Toast.LENGTH_SHORT)
                .show()
            onSuccess()
        }
    }

    fun onShowInfo(isSelf: Boolean) {
        val msg = if (isSelf) "This is your card" else "User Info"
        Toast.makeText(context, msg, Toast.LENGTH_SHORT)
            .show()
    }
}