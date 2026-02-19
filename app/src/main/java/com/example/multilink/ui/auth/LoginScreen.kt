package com.example.multilink.ui.auth

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.multilink.R
import com.example.multilink.ui.theme.MultiLinkTheme
import com.example.multilink.ui.viewmodel.MultiLinkViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit
) {
    val viewModel: MultiLinkViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    val coroutineScope = rememberCoroutineScope()

    // ⭐ WEB CLIENT ID
    val webClientId = "1088420870250-3vlqpo3dfaeej8e9tg3edb8kao8j3dg8.apps.googleusercontent.com"

    // ⭐ NEW: Credential Manager Init
    val credentialManager = CredentialManager.create(context)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "By continuing, you agree to our Terms.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(scrollState), // Keeps it scrollable
            horizontalAlignment = Alignment.CenterHorizontally,
            // ⭐ FIX: Align content in the center vertically
            verticalArrangement = Arrangement.Center
        ) {
            // Adjust spacer based on orientation to keep it centered but not cramped
            if (isPortrait) {
                Spacer(modifier = Modifier.height(60.dp))
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }

            Text(
                text = "MultiLink",
                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Discover connections & track real-time.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(40.dp))
            LoveFromBadge(modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = {
                    coroutineScope.launch {
                        try {
                            // 1. Configure Options
                            val googleIdOption = GetGoogleIdOption.Builder()
                                .setFilterByAuthorizedAccounts(false)
                                .setServerClientId(webClientId)
                                .setAutoSelectEnabled(false)
                                .build()

                            val request = GetCredentialRequest.Builder()
                                .addCredentialOption(googleIdOption)
                                .build()

                            // 2. Show Bottom Sheet
                            val result = credentialManager.getCredential(
                                request = request,
                                context = context
                            )

                            // 3. Handle Result
                            val credential = result.credential
                            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                            val idToken = googleIdTokenCredential.idToken

                            // 4. Send to ViewModel (This matches the new String parameter)
                            viewModel.signInWithGoogle(idToken) { errorMsg ->
                                if (errorMsg == null) {
                                    onLoginSuccess()
                                } else {
                                    Toast.makeText(context, "Login Failed: $errorMsg", Toast.LENGTH_LONG).show()
                                }
                            }

                        } catch (e: Exception) {
                            // Log.e("Auth", "Sign in cancelled or failed", e)
                            if (e !is androidx.credentials.exceptions.GetCredentialCancellationException) {
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                } else {
                    Text("Continue with Google", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
            }

            // Extra spacer at bottom for scrolling comfort
            Spacer(modifier = Modifier.height(50.dp))
        }
    }
}

@Composable
fun LoveFromBadge(modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier.border(2.dp, MaterialTheme.colorScheme.primary.copy(0.6f), RoundedCornerShape(12.dp)).padding(12.dp)) {
        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerHigh), modifier = Modifier.fillMaxWidth().height(140.dp)) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Image(painterResource(R.drawable.logo_rajasthan2), "Logo", Modifier.size(150.dp), contentScale = ContentScale.Fit, colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface))
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("LOVE FROM RAJASTHAN", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp), color = MaterialTheme.colorScheme.primary)
    }
}

@Preview
@Composable
fun PreviewLogin(){
    MultiLinkTheme{
        LoginScreen(onLoginSuccess = {})
    }
}