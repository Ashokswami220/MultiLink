package com.example.multilink.ui.auth

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.multilink.repo.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@Composable
fun InfoInputScreen(onInfoSubmitted: () -> Unit) {
    val auth = FirebaseAuth.getInstance()
    val repo = remember { AuthRepository() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Pre-fill
    var name by remember { mutableStateOf(auth.currentUser?.displayName ?: "") }
    var phone by remember { mutableStateOf(auth.currentUser?.phoneNumber ?: "") }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .imePadding(), // Adjust for keyboard
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Complete Profile",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            text = "We need a few details to continue.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Full Name") },
            leadingIcon = { Icon(Icons.Default.Person, null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = phone,
            onValueChange = { if (it.all { c -> c.isDigit() || c == '+' }) phone = it },
            label = { Text("Mobile Number") },
            leadingIcon = { Icon(Icons.Default.Phone, null) },
            placeholder = { Text("+91 9876543210") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (name.isNotBlank() && phone.length >= 10) {
                    isLoading = true
                    scope.launch {
                        try {
                            repo.saveUserProfile(name, phone)
                            isLoading = false
                            onInfoSubmitted() // Triggers the navigation
                        } catch (e: Exception) {
                            isLoading = false
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                } else {
                    Toast.makeText(
                        context, "Please enter a valid name and phone number (10+ digits)",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Save & Continue")
            }
        }
    }
}