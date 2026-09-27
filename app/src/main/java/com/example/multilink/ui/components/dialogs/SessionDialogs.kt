package com.example.multilink.ui.components.dialogs

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

@Composable
fun TooFarDialog(
    distanceMeters: Int,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Too Far Away") },
        text = {
            Text(
                "You must be within 200 meters of the destination to check in. You are currently $distanceMeters meters away."
            )
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Got it") } }
    )
}

@Composable
fun ArrivedToggleDialog(
    isArriving: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isArriving) "Mark as Arrived?" else "Undo Arrival?") },
        text = {
            Text(
                if (isArriving) "This will automatically pause your location tracking and notify everyone that you have reached the destination."
                else "This will resume your active tracking and notify everyone."
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isArriving) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                )
            ) { Text(if (isArriving) "Reached" else "Undo") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

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