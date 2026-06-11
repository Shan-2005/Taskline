package com.example.chattaskai.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.LocalIndication
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chattaskai.ui.theme.*
import com.example.chattaskai.util.PermissionChecker
import com.example.chattaskai.reminder.ReminderManager
import com.example.chattaskai.data.database.TaskEntity
import com.example.chattaskai.ui.components.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.chattaskai.ui.TaskViewModel

import android.provider.Settings
import android.content.Context
import android.content.Intent
import android.net.Uri
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: TaskViewModel,
    onTaskClick: (Long) -> Unit,
    onSettingsClick: () -> Unit
) {
    val pendingTasks by viewModel.pendingTasks.collectAsState()
    val completedTasks by viewModel.completedTasks.collectAsState()
    val reviewTasks by viewModel.reviewTasks.collectAsState()
    val dailyQuote by viewModel.dailyQuote.collectAsState()
    val availableApkUpdate by viewModel.availableApkUpdate.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    LaunchedEffect(Unit) {
        viewModel.checkForApkUpdate(context)
    }
    
    // Permission Check
    var isServiceEnabled by remember { mutableStateOf(isNotificationServiceEnabled(context)) }
    var hasPostPermission by remember { mutableStateOf(PermissionChecker.hasNotificationPermission(context)) }
    var hasAlarmPermission by remember { mutableStateOf(PermissionChecker.hasExactAlarmPermission(context)) }

    // Re-check when screen is resumed
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            isServiceEnabled = isNotificationServiceEnabled(context)
            hasPostPermission = PermissionChecker.hasNotificationPermission(context)
            hasAlarmPermission = PermissionChecker.hasExactAlarmPermission(context)
        }
    }

    var selectedCategory by remember { mutableStateOf("All") }
    var activeTab by remember { mutableStateOf("tasks") } // Bottom navigation state

    val displayedPendingTasks = remember(pendingTasks, selectedDate, selectedCategory) {
        val filteredByDate = if (selectedDate != null) {
            val dateStr = selectedDate!!.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            pendingTasks.filter { it.deadlineDate == dateStr }
        } else {
            pendingTasks
        }

        if (selectedCategory != "All") {
            filteredByDate.filter { it.category.equals(selectedCategory, ignoreCase = true) }
        } else {
            filteredByDate
        }
    }

    var showAddTaskDialog by remember { mutableStateOf(false) }
    
    val liquidColors = LocalLiquidColors.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val isTablet = screenWidth >= 600

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Black,
        bottomBar = {
            NavigationBar(
                containerColor = Color.Black.copy(alpha = 0.8f),
                modifier = Modifier.glassMorphism()
            ) {
                NavigationBarItem(
                    selected = activeTab == "tasks",
                    onClick = { activeTab = "tasks" },
                    icon = { Icon(Icons.Default.List, contentDescription = "Tasks") },
                    label = { Text("Tasks") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = liquidColors.cyan,
                        unselectedIconColor = Color.White.copy(alpha = 0.4f),
                        selectedTextColor = liquidColors.cyan,
                        unselectedTextColor = Color.White.copy(alpha = 0.4f),
                        indicatorColor = Color.Transparent
                    )
                )
                NavigationBarItem(
                    selected = activeTab == "chat",
                    onClick = { activeTab = "chat" },
                    icon = { Icon(Icons.Default.Send, contentDescription = "AI Assistant") },
                    label = { Text("AI Assistant") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = liquidColors.cyan,
                        unselectedIconColor = Color.White.copy(alpha = 0.4f),
                        selectedTextColor = liquidColors.cyan,
                        unselectedTextColor = Color.White.copy(alpha = 0.4f),
                        indicatorColor = Color.Transparent
                    )
                )
            }
        },
        floatingActionButton = {
            if (activeTab == "tasks") {
                FloatingActionButton(
                    onClick = { showAddTaskDialog = true },
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape),
                    containerColor = liquidColors.purple,
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Task", modifier = Modifier.size(32.dp))
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LiquidBackground()
            
            val xp by viewModel.xp.collectAsState()
            val level by viewModel.level.collectAsState()
            val streak by viewModel.streak.collectAsState()
            val levelUpAlert by viewModel.levelUpAlert.collectAsState()

            if (levelUpAlert != null) {
                AlertDialog(
                    onDismissRequest = { viewModel.dismissLevelUpAlert() },
                    title = { Text("🎉 Level Up!", color = Color.White, fontWeight = FontWeight.Bold) },
                    text = { Text(levelUpAlert!!, color = Color.White.copy(alpha = 0.85f)) },
                    confirmButton = {
                        TextButton(onClick = { viewModel.dismissLevelUpAlert() }) {
                            Text("Awesome!", color = liquidColors.cyan)
                        }
                    },
                    containerColor = Color.DarkGray,
                    tonalElevation = 8.dp
                )
            }

            if (activeTab == "chat") {
                AssistantChatScreen(
                    viewModel = viewModel,
                    modifier = Modifier.padding(padding)
                )
            } else {
                if (isTablet) {
                    // Two-column split layout for tablet/landscape
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // Left Column: Header, Quote, Calendar, Stats
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = 16.dp)
                                .padding(bottom = 80.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            DashboardHeader(liquidColors = liquidColors, onSettingsClick = onSettingsClick)
                            
                            QuoteCard(dailyQuote = dailyQuote, liquidColors = liquidColors)
                            
                            MonthlyCalendar(
                                tasks = pendingTasks,
                                selectedDate = selectedDate,
                                onDateSelected = { selectedDate = it }
                            )
                            
                            StatsRow(pendingTasks = pendingTasks, completedTasks = completedTasks, liquidColors = liquidColors)

                            ProductivityStatsCard(xp = xp, level = level, streak = streak, liquidColors = liquidColors)
                        }
                        
                        // Right Column: Notifications, Reviews, and Tasks list
                        LazyColumn(
                            modifier = Modifier
                                .weight(1.2f)
                                .fillMaxHeight(),
                            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            setupRequiredSection(context, isServiceEnabled, hasPostPermission, hasAlarmPermission)
                            
                            apkUpdateSection(availableApkUpdate, context, viewModel)
                            
                            needsReviewSection(reviewTasks, viewModel, context, onTaskClick)
                            
                            item {
                                PendingTasksHeader(selectedDate = selectedDate, liquidColors = liquidColors, onClearFilter = { selectedDate = null })
                                CategoryFilterRow(selectedCategory = selectedCategory, onCategorySelected = { selectedCategory = it }, liquidColors = liquidColors)
                            }
                            
                            pendingTasksSection(displayedPendingTasks, selectedDate, viewModel, context, onTaskClick)
                        }
                    }
                } else {
                    // Original single-column phone layout
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .statusBarsPadding()
                                    .padding(vertical = 16.dp)
                            ) {
                                DashboardHeader(liquidColors = liquidColors, onSettingsClick = onSettingsClick)
                                Spacer(modifier = Modifier.height(24.dp))
                                QuoteCard(dailyQuote = dailyQuote, liquidColors = liquidColors)
                            }
                        }
                        
                        setupRequiredSection(context, isServiceEnabled, hasPostPermission, hasAlarmPermission)
                        
                        apkUpdateSection(availableApkUpdate, context, viewModel)
                        
                        needsReviewSection(reviewTasks, viewModel, context, onTaskClick)
                        
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            MonthlyCalendar(
                                tasks = pendingTasks,
                                selectedDate = selectedDate,
                                onDateSelected = { selectedDate = it }
                            )
                        }
                        
                        item {
                            StatsRow(pendingTasks = pendingTasks, completedTasks = completedTasks, liquidColors = liquidColors)
                        }

                        item {
                            ProductivityStatsCard(xp = xp, level = level, streak = streak, liquidColors = liquidColors)
                        }
                        
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            PendingTasksHeader(selectedDate = selectedDate, liquidColors = liquidColors, onClearFilter = { selectedDate = null })
                            CategoryFilterRow(selectedCategory = selectedCategory, onCategorySelected = { selectedCategory = it }, liquidColors = liquidColors)
                        }
                        
                        pendingTasksSection(displayedPendingTasks, selectedDate, viewModel, context, onTaskClick)
                    }
                }
            }
            
            if (showAddTaskDialog) {
                ManualAddTaskDialog(
                    onDismiss = { showAddTaskDialog = false },
                    onTaskAdded = { task ->
                        viewModel.upsertTask(context, task)
                        showAddTaskDialog = false
                    }
                )
            }
        } // End Main Content Box
    }
}

@Composable
private fun DashboardHeader(
    liquidColors: com.example.chattaskai.ui.theme.LiquidColors,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Taskline",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontFamily = FontLoader.lobster(),
                    color = Color.White,
                    fontSize = 42.sp
                )
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Liquid Glass Edition",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = FontLoader.ndot(),
                        color = Color.White.copy(alpha = 0.7f),
                        letterSpacing = 1.sp
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("✦", color = liquidColors.purple, fontSize = 14.sp)
            }
        }
        
        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier
                .size(44.dp)
                .background(Color.White.copy(alpha = 0.05f), CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Theme",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun QuoteCard(
    dailyQuote: com.example.chattaskai.ui.Quote,
    liquidColors: com.example.chattaskai.ui.theme.LiquidColors
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassMorphism(alpha = 0.15f, baseColor = Color.Black)
            .padding(24.dp)
    ) {
        // Subtle Glow
        Box(
            modifier = Modifier
                .size(120.dp)
                .align(Alignment.CenterEnd)
                .offset(x = 40.dp, y = (-20).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(liquidColors.purple.copy(alpha = 0.3f), Color.Transparent)
                    )
                )
        )
        
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                "“",
                color = liquidColors.purple,
                fontSize = 48.sp,
                fontFamily = FontLoader.lobster(),
                lineHeight = 24.sp
            )
            Text(
                dailyQuote.text,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    fontWeight = FontWeight.Normal,
                    color = Color.White.copy(alpha = 0.9f),
                    lineHeight = 30.sp
                )
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.width(20.dp).height(1.dp).background(liquidColors.purple.copy(alpha = 0.5f)))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    dailyQuote.author.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontLoader.ndot(),
                        color = liquidColors.purple,
                        letterSpacing = 2.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun StatsRow(
    pendingTasks: List<TaskEntity>,
    completedTasks: List<TaskEntity>,
    liquidColors: com.example.chattaskai.ui.theme.LiquidColors
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val pendingCount = pendingTasks.size.toString().padStart(2, '0')
        val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        val dueTodayCount = pendingTasks.count { it.deadlineDate == todayStr }.toString().padStart(2, '0')
        val doneCount = completedTasks.size.toString().padStart(2, '0')

        val stats = listOf(
            Triple(pendingCount, "PENDING", liquidColors.purple),
            Triple(dueTodayCount, "DUE\nTODAY", liquidColors.purple),
            Triple(doneCount, "DONE", Color.White.copy(alpha = 0.3f))
        )
        
        stats.forEach { (count, label, color) ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .glassMorphism(alpha = 0.05f)
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        count,
                        color = color,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = FontLoader.ndot(),
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        label,
                        color = Color.White.copy(alpha = 0.4f),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontLoader.ndot(),
                            lineHeight = 14.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun PendingTasksHeader(
    selectedDate: LocalDate?,
    liquidColors: com.example.chattaskai.ui.theme.LiquidColors,
    onClearFilter: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (selectedDate != null) {
                val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy")
                "Tasks for ${selectedDate.format(formatter)}"
            } else {
                "Pending Tasks"
            },
            style = MaterialTheme.typography.headlineLarge.copy(
                fontFamily = FontLoader.lobster(),
                color = Color.White,
                fontSize = 28.sp
            ),
            modifier = Modifier.padding(bottom = 4.dp)
        )
        if (selectedDate != null) {
            TextButton(onClick = onClearFilter) {
                Text("Clear Filter", color = liquidColors.cyan)
            }
        }
    }
}

private fun LazyListScope.setupRequiredSection(
    context: android.content.Context,
    isServiceEnabled: Boolean,
    hasPostPermission: Boolean,
    hasAlarmPermission: Boolean
) {
    if (!isServiceEnabled) {
        item {
            SetupRequiredCard(
                title = "Action Required",
                description = "Enable Notification Access to start fetching tasks automatically from WhatsApp.",
                buttonText = "Enable Access",
                icon = Icons.Default.Settings,
                onActionClick = { context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")) }
            )
        }
    } else if (!hasPostPermission) {
        item {
            SetupRequiredCard(
                title = "Notifications Restricted",
                description = "Please allow notification permission to receive your task reminders.",
                buttonText = "Allow Notifications",
                icon = Icons.Default.CheckCircle,
                onActionClick = { PermissionChecker.openNotificationSettings(context) }
            )
        }
    } else if (!hasAlarmPermission) {
        item {
            SetupRequiredCard(
                title = "Exact Alarms Blocked",
                description = "The app needs permission to trigger precise alarms for your deadlines.",
                buttonText = "Enable Exact Alarms",
                icon = Icons.Default.DateRange,
                onActionClick = { PermissionChecker.openExactAlarmSettings(context) }
            )
        }
    }
}

private fun LazyListScope.apkUpdateSection(
    availableApkUpdate: com.example.chattaskai.service.ApkUpdateInfo?,
    context: android.content.Context,
    viewModel: TaskViewModel
) {
    if (availableApkUpdate != null) {
        item {
            ApkUpdateCard(
                latestVersionName = availableApkUpdate.latestVersionName ?: "",
                latestVersionCode = availableApkUpdate.latestVersionCode ?: 0L,
                releaseNotes = availableApkUpdate.releaseNotes.orEmpty(),
                onUpdateClick = {
                    val downloadUri = Uri.parse(availableApkUpdate.downloadUrl ?: return@ApkUpdateCard)
                    val browserIntent = Intent(Intent.ACTION_VIEW, downloadUri)
                    context.startActivity(browserIntent)
                },
                onDismiss = { viewModel.dismissApkUpdateCard() }
            )
        }
    }
}

private fun LazyListScope.needsReviewSection(
    reviewTasks: List<TaskEntity>,
    viewModel: TaskViewModel,
    context: android.content.Context,
    onTaskClick: (Long) -> Unit
) {
    if (reviewTasks.isNotEmpty()) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Needs Review",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontFamily = FontLoader.lobster(),
                    color = Color.White,
                    fontSize = 28.sp
                ),
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        items(reviewTasks, key = { it.id }) { task ->
            ReviewTaskCard(
                task = task,
                onApprove = { viewModel.approveReviewTask(context, task) },
                onDismiss = { viewModel.dismissReviewTask(context, task) },
                onClick = { onTaskClick(task.id) }
            )
        }
    }
}

private fun LazyListScope.pendingTasksSection(
    displayedPendingTasks: List<TaskEntity>,
    selectedDate: java.time.LocalDate?,
    viewModel: TaskViewModel,
    context: android.content.Context,
    onTaskClick: (Long) -> Unit
) {
    if (displayedPendingTasks.isEmpty()) {
        item {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.White.copy(alpha = 0.2f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        if (selectedDate != null) "No tasks for this date" else "All caught up!",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.4f))
                    )
                }
            }
        }
    } else {
        items(displayedPendingTasks, key = { it.id }) { task ->
            TaskCard(
                task = task,
                viewModel = viewModel,
                onComplete = { viewModel.completeTask(context, task) },
                onClick = { onTaskClick(task.id) }
            )
        }
    }
}

@Composable
fun PaletteIcon() = Icons.Default.Settings

@Composable
fun SetupRequiredCard(
    title: String,
    description: String,
    buttonText: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onActionClick: () -> Unit
) {
    val colors = LocalLiquidColors.current
    Surface(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        color = Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = colors.cyan, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Taskline",
                    style = MaterialTheme.typography.displayLarge,
                    color = Color.White
                )
                Text(
                    text = "Taskline Edition",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.cyan.copy(alpha = 0.6f)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onActionClick,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.cyan.copy(alpha = 0.2f), contentColor = colors.cyan)
            ) {
                Text(buttonText, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ApkUpdateCard(
    latestVersionName: String,
    latestVersionCode: Long,
    releaseNotes: String,
    onUpdateClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalLiquidColors.current
    val trimmedNotes = releaseNotes.trim().ifBlank { "A newer build is available for download." }

    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        color = Color.White.copy(alpha = 0.07f),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.purple.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Update available: $latestVersionName (vc$latestVersionCode)",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Text(
                text = trimmedNotes,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onUpdateClick,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.purple)
                ) {
                    Text("Download APK")
                }
                OutlinedButton(onClick = onDismiss) {
                    Text("Later")
                }
            }
        }
    }
}

fun isNotificationServiceEnabled(context: Context): Boolean {
    val pkgName = context.packageName
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    if (!flat.isNullOrEmpty()) {
        val names = flat.split(":".toRegex()).toTypedArray()
        for (i in names.indices) {
            val cn = android.content.ComponentName.unflattenFromString(names[i])
            if (cn != null) {
                if (pkgName == cn.packageName) {
                    return true
                }
            }
        }
    }
    return false
}

@Composable
fun TaskCard(
    task: TaskEntity,
    viewModel: TaskViewModel,
    onComplete: () -> Unit,
    onClick: () -> Unit
) {
    val liquidColors = LocalLiquidColors.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    
    val padding = if (screenWidth < 360) 12.dp else if (screenWidth < 600) 18.dp else 24.dp
    val titleSize = if (screenWidth < 360) 16.sp else if (screenWidth < 600) 19.sp else 22.sp
    val checkButtonSize = if (screenWidth < 360) 44.dp else if (screenWidth < 600) 48.dp else 56.dp
    val checkIconSize = if (screenWidth < 360) 24.dp else if (screenWidth < 600) 28.dp else 32.dp
    val dateTextSize = if (screenWidth < 360) 10.sp else if (screenWidth < 600) 11.sp else 12.sp
    val spacerHeight = if (screenWidth < 360) 8.dp else 12.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassMorphism(alpha = 0.04f, cornerRadius = 32.dp)
            .clickable(onClick = onClick)
            .padding(padding)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = titleSize
                    )
                )
                Spacer(modifier = Modifier.height(spacerHeight))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.DateRange, 
                        contentDescription = null, 
                        tint = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${task.deadlineDate} @ ${task.deadlineTime}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontLoader.ndot(),
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = dateTextSize
                        )
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    PriorityBadge(task.priority)
                }

                val subTasks by viewModel.getSubTasksForTask(task.id).collectAsState(initial = emptyList())
                if (subTasks.isNotEmpty()) {
                    val completedCount = subTasks.count { it.isCompleted }
                    Spacer(modifier = Modifier.height(spacerHeight))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Checklist: $completedCount/${subTasks.size}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = liquidColors.cyan.copy(alpha = 0.9f),
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        LinearProgressIndicator(
                            progress = completedCount.toFloat() / subTasks.size,
                            modifier = Modifier
                                .width(80.dp)
                                .height(4.dp)
                                .clip(CircleShape),
                            color = liquidColors.cyan,
                            trackColor = Color.White.copy(alpha = 0.15f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(spacerHeight))
                // Source and Sender Information
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "From: ${task.sender} via ${task.sourceApp}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = liquidColors.cyan.copy(alpha = 0.8f)
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))

            // Large Circular Check Button with press scaling effect
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val scale by animateFloatAsState(
                targetValue = if (isPressed) 0.85f else 1.0f,
                label = "scale"
            )

            Box(
                modifier = Modifier
                    .size(checkButtonSize)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(liquidColors.purple)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = LocalIndication.current,
                        onClick = onComplete
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle, 
                    contentDescription = "Complete",
                    tint = Color.White,
                    modifier = Modifier.size(checkIconSize)
                )
            }
        }
    }
}

@Composable
fun ReviewTaskCard(
    task: TaskEntity,
    onApprove: () -> Unit,
    onDismiss: () -> Unit,
    onClick: () -> Unit
) {
    val liquidColors = LocalLiquidColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassMorphism(alpha = 0.06f, cornerRadius = 28.dp)
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 20.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Needs your confirmation before Taskline adds reminders.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.55f)
                    )
                }
            }

            Text(
                text = task.originalMessage,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onApprove,
                    colors = ButtonDefaults.buttonColors(containerColor = liquidColors.purple)
                ) {
                    Text("Add to Tasks")
                }
                OutlinedButton(onClick = onDismiss) {
                    Text("Dismiss")
                }
            }
        }
    }
}

@Composable
fun ManualAddTaskDialog(onDismiss: () -> Unit, onTaskAdded: (TaskEntity) -> Unit) {
    var title by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())) }
    var time by remember { mutableStateOf("09:00") }
    var priority by remember { mutableStateOf("medium") }
    val colors = LocalLiquidColors.current

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .background(Color.Black, RoundedCornerShape(24.dp))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "New Task",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Details", color = colors.purple, style = MaterialTheme.typography.labelLarge)
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("What's on your mind?", color = Color.White.copy(alpha = 0.5f)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = colors.purple,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        )
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1.2f)) {
                        Text("Date", color = colors.cyan, style = MaterialTheme.typography.labelLarge)
                        OutlinedTextField(
                            value = date,
                            onValueChange = { date = it },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = colors.cyan,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                            )
                        )
                    }
                    Column(modifier = Modifier.weight(0.8f)) {
                        Text("Time", color = colors.cyan, style = MaterialTheme.typography.labelLarge)
                        OutlinedTextField(
                            value = time,
                            onValueChange = { time = it },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = colors.cyan,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                            )
                        )
                    }
                }

                Text("Priority", color = colors.pink, style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("low", "medium", "high").forEach { p ->
                        val isSelected = priority == p
                        val color = when(p) {
                            "high" -> UrgentPriority
                            "medium" -> MediumPriority
                            else -> LowPriority
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) color.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.05f))
                                .border(1.dp, if (isSelected) color else Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                .clickable { priority = p }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(p.uppercase(), fontWeight = FontWeight.Bold, color = if (isSelected) color else Color.White.copy(alpha = 0.4f))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                            val timestamp = try {
                                sdf.parse("$date $time")?.time ?: System.currentTimeMillis()
                            } catch (e: Exception) {
                                System.currentTimeMillis()
                            }
                            onTaskAdded(TaskEntity(
                                title = title,
                                originalMessage = "Manual Entry",
                                sender = "You",
                                deadlineDate = date,
                                deadlineTime = time,
                                deadlineTimestamp = timestamp,
                                priority = priority,
                                category = "General",
                                reminderMinutesBefore = 30,
                                sourceApp = "Manual"
                            ))
                            onDismiss()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .liquidGradient(listOf(colors.cyan, colors.purple)),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    enabled = title.isNotBlank()
                ) {
                    Text("Add Task", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantChatScreen(viewModel: TaskViewModel, modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val messages by viewModel.chatMessages.collectAsState()
    val isLoading by viewModel.isChatLoading.collectAsState()
    val liquidColors = LocalLiquidColors.current
    var inputText by remember { mutableStateOf("") }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    // Auto-scroll to the bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassMorphism()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(if (isLoading) liquidColors.purple else liquidColors.cyan)
                        )
                        Column {
                            Text(
                                text = "Taskline Assistant",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            Text(
                                text = if (isLoading) "Typing..." else "Online",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }

                    // Interactive AI Status pill button
                    val aiMode by viewModel.aiMode.collectAsState()
                    val aiStatus by viewModel.aiStatus.collectAsState()
                    var menuExpanded by remember { mutableStateOf(false) }

                    val statusDotColor = when (aiStatus) {
                        "Active" -> Color(0xFF4CAF50) // Green
                        "Quota Exceeded" -> Color(0xFFFF9800) // Orange/Red
                        else -> Color(0xFF9E9E9E) // Gray
                    }

                    val statusText = when (aiStatus) {
                        "Active" -> "Active"
                        "Quota Exceeded" -> "Quota Exceeded"
                        else -> "Offline"
                    }

                    Box {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.White.copy(alpha = 0.1f))
                                .clickable { menuExpanded = true }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(statusDotColor)
                            )
                            Text(
                                text = statusText,
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Select AI Mode",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            modifier = Modifier.background(Color.DarkGray)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Gemini AI (Cloud)", color = Color.White) },
                                onClick = {
                                    viewModel.setAiMode("Cloud")
                                    menuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Local Offline Parser", color = Color.White) },
                                onClick = {
                                    viewModel.setAiMode("Offline")
                                    menuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Message List
            androidx.compose.foundation.lazy.LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages) { msg ->
                    val isAi = msg.sender != "User"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isAi) Arrangement.Start else Arrangement.End
                    ) {
                        Box(
                            modifier = Modifier
                                .widthIn(max = 280.dp)
                                .clip(
                                    RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = if (isAi) 4.dp else 16.dp,
                                        bottomEnd = if (isAi) 16.dp else 4.dp
                                    )
                                )
                                .background(
                                    if (isAi) {
                                        Color.White.copy(alpha = 0.12f)
                                    } else {
                                        liquidColors.purple.copy(alpha = 0.85f)
                                    }
                                )
                                .border(
                                    1.dp,
                                    if (isAi) Color.White.copy(alpha = 0.08f) else Color.Transparent,
                                    RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = if (isAi) 4.dp else 16.dp,
                                        bottomEnd = if (isAi) 16.dp else 4.dp
                                    )
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Column {
                                Text(
                                    text = msg.sender,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isAi) liquidColors.cyan else liquidColors.pink,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = msg.text,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // Input Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Ask Assistant to do tasks...", color = Color.White.copy(alpha = 0.4f)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = liquidColors.cyan,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                    )
                )

                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            val textToSend = inputText.trim()
                            inputText = ""
                            viewModel.sendChatMessage(context, textToSend)
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(liquidColors.purple),
                    enabled = !isLoading
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send Message",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductivityStatsCard(
    xp: Int,
    level: Int,
    streak: Int,
    liquidColors: com.example.chattaskai.ui.theme.LiquidColors
) {
    val progress = (xp % 100) / 100f
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassMorphism(alpha = 0.12f, baseColor = Color.Black)
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Productivity Level",
                        color = Color.White.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        "Level $level",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(liquidColors.pink.copy(alpha = 0.15f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        "🔥 $streak Day Streak",
                        color = liquidColors.pink,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
            
            // Progress Bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = liquidColors.cyan,
                    trackColor = Color.White.copy(alpha = 0.1f)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "${xp % 100} XP",
                        color = Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "100 XP to Level Up",
                        color = Color.White.copy(alpha = 0.4f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryFilterRow(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    liquidColors: com.example.chattaskai.ui.theme.LiquidColors
) {
    val categories = listOf("All", "Work", "Personal", "Shopping", "Health")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { cat ->
            val isSelected = selectedCategory == cat
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) liquidColors.cyan else Color.White.copy(alpha = 0.08f))
                    .clickable { onCategorySelected(cat) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = cat,
                    color = if (isSelected) Color.Black else Color.White,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
