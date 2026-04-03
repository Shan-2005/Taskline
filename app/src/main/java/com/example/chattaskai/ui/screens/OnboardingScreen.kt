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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chattaskai.data.profile.ProfileStore
import com.example.chattaskai.ui.components.ProfileSourcesForm
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
    var rules by remember { mutableStateOf(store.loadTrackingRules()) }

    Box(modifier = Modifier.fillMaxSize()) {
        LiquidBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
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
                text = "Set up your profile, choose what to track, and start turning messages into tasks.",
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
                    OnboardingPoint(Icons.Default.Person, "Create your profile", "Add your name and email so Taskline can identify your account later.")
                    OnboardingPoint(Icons.Default.Email, "Choose sources", "Pick which apps can create tasks: WhatsApp, Gmail, or Outlook.")
                    OnboardingPoint(Icons.Default.CheckCircle, "Link rules", "Add specific Gmail accounts, WhatsApp groups, contacts, or keywords to track.")
                    OnboardingPoint(Icons.Default.CheckCircle, "Review before adding", "Weak matches go to a review queue instead of silently entering your task list.")
                }
            }

            ProfileSourcesForm(
                profile = profile,
                rules = rules,
                onProfileChange = { profile = it },
                onRulesChange = { rules = it },
                onSave = {
                    store.saveProfile(profile.copy(onboardingComplete = true))
                    store.saveTrackingRules(rules)
                    store.setOnboardingComplete(true)
                    onFinished()
                },
                saveLabel = "Finish Setup",
                modifier = Modifier.fillMaxWidth()
            )
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
