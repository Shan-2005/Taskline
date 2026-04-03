package com.example.chattaskai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import com.example.chattaskai.ui.TaskViewModel
import com.example.chattaskai.data.profile.ProfileStore
import com.example.chattaskai.data.profile.TrackingRules
import com.example.chattaskai.data.profile.UserProfile
import com.example.chattaskai.ui.components.ProfileSourcesForm
import com.example.chattaskai.ui.theme.LocalLiquidColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: TaskViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalLiquidColors.current
    val profileStore = remember { ProfileStore(context) }
    
    val themeHue by viewModel.themeHue.collectAsState()
    val morningHour by viewModel.morningReminderHour.collectAsState()
    val snoozeMin by viewModel.snoozeMinutes.collectAsState()
    val strictFilter by viewModel.strictFiltering.collectAsState()
    var profile by remember { mutableStateOf(profileStore.loadProfile()) }
    var trackingRules by remember { mutableStateOf(profileStore.loadTrackingRules()) }

    LaunchedEffect(Unit) {
        viewModel.loadSettings(context)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = { Text("Taskline Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Account & Sources Section
            SettingsGroup(title = "Account & Sources", icon = Icons.Default.Person) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Set up the local account that will later sync to mobile and desktop.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.55f)
                    )

                    ProfileSourcesForm(
                        profile = profile,
                        rules = trackingRules,
                        onProfileChange = { profile = it },
                        onRulesChange = { trackingRules = it },
                        onSave = {
                            profileStore.saveProfile(profile.copy(onboardingComplete = true))
                            profileStore.saveTrackingRules(trackingRules)
                        },
                        saveLabel = "Save Profile & Sources"
                    )
                }
            }

            // Appearance Section
            SettingsGroup(title = "Premium Gradients", icon = Icons.Default.Face) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "Select your preferred aesthetic experience.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        listOf(
                            Triple(0, "Purple Void", com.example.chattaskai.ui.theme.PurpleVoid),
                            Triple(1, "Midnight Blue", com.example.chattaskai.ui.theme.MidnightBlue),
                            Triple(2, "Deep Forest", com.example.chattaskai.ui.theme.DeepForest)
                        ).forEach { (id, name, palette) ->
                            PremiumThemeCard(
                                name = name,
                                palette = palette,
                                isSelected = themeHue.toInt() == id,
                                onClick = { viewModel.setThemeHue(context, id.toFloat()) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Notifications Section
            SettingsGroup(title = "Notifications", icon = Icons.Default.Notifications) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Morning Hour
                    Column {
                        Text(
                            "Morning Sync Alert: ${morningHour}:00", 
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                        Slider(
                            value = morningHour.toFloat(),
                            onValueChange = { viewModel.setMorningHour(context, it.toInt()) },
                            valueRange = 6f..12f,
                            steps = 6,
                            colors = SliderDefaults.colors(thumbColor = colors.purple, activeTrackColor = colors.purple)
                        )
                        Text(
                            "When to show the daily summary for tasks without specific times.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.4f)
                        )
                    }

                    // Snooze Duration
                    Column {
                        Text(
                            "Default Snooze: ${snoozeMin} minutes", 
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                        Slider(
                            value = snoozeMin.toFloat(),
                            onValueChange = { viewModel.setSnoozeMinutes(context, it.toInt()) },
                            valueRange = 5f..30f,
                            steps = 5,
                            colors = SliderDefaults.colors(thumbColor = colors.pink, activeTrackColor = colors.pink)
                        )
                    }
                }
            }

            // Engine Section
            SettingsGroup(title = "Intelligence Engine", icon = Icons.Default.Build) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Strict Intent Filtering", style = MaterialTheme.typography.bodyLarge, color = Color.White)
                        Text(
                            "When enabled, Taskline ignores casual chat and only extracts clear actionable tasks.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                    Switch(
                        checked = strictFilter,
                        onCheckedChange = { viewModel.setStrictFiltering(context, it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.cyan,
                            checkedTrackColor = colors.cyan.copy(alpha = 0.3f)
                        )
                    )
                }
            }

            // About Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Taskline", 
                        style = MaterialTheme.typography.displayMedium, 
                        color = Color.White
                    )
                    Text(
                        "Liquid Glass Edition v1.2", 
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.cyan.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Designed by Shan Neeraj", 
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

@Composable
fun PremiumThemeCard(
    name: String,
    palette: com.example.chattaskai.ui.theme.LiquidColors,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) palette.cyan else Color.White.copy(alpha = 0.1f)
    val borderWidth = if (isSelected) 2.dp else 1.dp
    
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(borderWidth, borderColor, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Gradient Preview
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(
                    Brush.sweepGradient(
                        listOf(palette.cyan, palette.purple, palette.pink, palette.cyan)
                    )
                )
        )
        
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun SettingsGroup(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    val colors = LocalLiquidColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = colors.cyan, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                title, 
                style = MaterialTheme.typography.headlineSmall, 
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        content()
    }
}
