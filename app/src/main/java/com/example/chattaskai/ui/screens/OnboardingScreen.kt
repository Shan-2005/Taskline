package com.example.chattaskai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chattaskai.data.profile.ProfileStore
import com.example.chattaskai.ui.components.LiquidBackground
import com.example.chattaskai.ui.theme.LocalLiquidColors
import com.example.chattaskai.ui.theme.FontLoader

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val colors = LocalLiquidColors.current
    val store = remember { ProfileStore(context) }

    var profile by remember { mutableStateOf(store.loadProfile()) }
    var displayName by remember { mutableStateOf(profile.displayName) }
    var email by remember { mutableStateOf(profile.email) }
    var organization by remember { mutableStateOf(profile.organization) }
    var permissionGranted by remember { mutableStateOf(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true
    ) }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionGranted = isGranted
    }
    
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !permissionGranted) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LiquidBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Welcome to Taskline",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontFamily = FontLoader.lobster(),
                    color = Color.White,
                    fontSize = 38.sp
                )
            )
            Text(
                text = "Set up your profile to get started.",
                color = Color.White.copy(alpha = 0.75f),
                style = MaterialTheme.typography.bodyLarge
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OnboardingPoint(Icons.Default.Person, "Complete your profile", "Add your name, email, and organization.")
                    OnboardingPoint(Icons.Default.Email, "Configure in Settings", "Set up WhatsApp contacts, Gmail accounts, and keywords to track.")
                    OnboardingPoint(Icons.Default.CheckCircle, "Review & add tasks", "Check the review queue for weak matches before they become tasks.")

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f)),
                                shape = RoundedCornerShape(20.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, LocalLiquidColors.current.cyan.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        "📲 Notification Access",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "We need access to capture task mentions from WhatsApp and Gmail.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                    if (!permissionGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        Button(
                                            onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = LocalLiquidColors.current.cyan)
                                        ) {
                                            Text("Enable Notifications", color = Color.Black, fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        Text(
                                            "✓ Notifications enabled",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = LocalLiquidColors.current.cyan,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                }
            }

            // Profile fields
            androidx.compose.material3.OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Display Name", color = Color.White.copy(alpha = 0.6f)) },
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White.copy(alpha = 0.02f),
                    focusedContainerColor = Color.White.copy(alpha = 0.05f),
                    unfocusedTextColor = Color.White,
                    focusedTextColor = Color.White,
                    cursorColor = colors.cyan,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedBorderColor = colors.cyan
                )
            )

            androidx.compose.material3.OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email", color = Color.White.copy(alpha = 0.6f)) },
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White.copy(alpha = 0.02f),
                    focusedContainerColor = Color.White.copy(alpha = 0.05f),
                    unfocusedTextColor = Color.White,
                    focusedTextColor = Color.White,
                    cursorColor = colors.cyan,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedBorderColor = colors.cyan
                )
            )

            androidx.compose.material3.OutlinedTextField(
                value = organization,
                onValueChange = { organization = it },
                label = { Text("Organization (Optional)", color = Color.White.copy(alpha = 0.6f)) },
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White.copy(alpha = 0.02f),
                    focusedContainerColor = Color.White.copy(alpha = 0.05f),
                    unfocusedTextColor = Color.White,
                    focusedTextColor = Color.White,
                    cursorColor = colors.cyan,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedBorderColor = colors.cyan
                )
            )

            // Finish button
            Button(
                onClick = {
                    val updated = profile.copy(
                        displayName = displayName,
                        email = email,
                        organization = organization,
                        onboardingComplete = true
                    )
                    store.saveProfile(updated)
                    onFinished()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.cyan)
            ) {
                Text("Continue to Dashboard", color = Color.Black, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun OnboardingPoint(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    val colors = LocalLiquidColors.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.Icon(icon, contentDescription = null, tint = colors.cyan, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.size(8.dp))
            Text(title, color = Color.White, fontWeight = FontWeight.Bold)
        }
        Text(description, color = Color.White.copy(alpha = 0.65f), style = MaterialTheme.typography.bodySmall)
    }
}
