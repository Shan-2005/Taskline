package com.example.chattaskai.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
// Using standard icons only
import androidx.compose.material.icons.filled.Settings
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: TaskViewModel,
    onTaskClick: (Long) -> Unit,
    onSettingsClick: () -> Unit
) {
    val pendingTasks by viewModel.pendingTasks.collectAsState()
    val completedTasks by viewModel.completedTasks.collectAsState()
    val dailyQuote by viewModel.dailyQuote.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    
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

    var showAddTaskDialog by remember { mutableStateOf(false) }
    
    val liquidColors = LocalLiquidColors.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Black,
        floatingActionButton = {
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
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LiquidBackground()
            
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header & Quote Item
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(vertical = 16.dp)
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
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Quote Card
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
                } // End Header Item

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

                // Stats Row Item
                item {
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

                // Pending Title Item
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Pending Tasks",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontFamily = FontLoader.lobster(),
                            color = Color.White,
                            fontSize = 32.sp
                        ),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                if (pendingTasks.isEmpty()) {
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
                                    "All caught up!", 
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.4f))
                                )
                            }
                        }
                    }
                } else {
                    items(pendingTasks, key = { it.id }) { task ->
                        TaskCard(
                            task = task,
                            onComplete = { viewModel.completeTask(context, task) },
                            onClick = { onTaskClick(task.id) }
                        )
                    }
                }
            } // End LazyColumn

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
    onComplete: () -> Unit,
    onClick: () -> Unit
) {
    val liquidColors = LocalLiquidColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassMorphism(alpha = 0.04f, cornerRadius = 32.dp)
            .clickable(onClick = onClick)
            .padding(24.dp)
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
                        fontSize = 22.sp
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
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
                            color = Color.White.copy(alpha = 0.4f)
                        )
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    PriorityBadge(task.priority)
                }
                Spacer(modifier = Modifier.height(12.dp))
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
            
            // Large Circular Check Button
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(liquidColors.purple, CircleShape)
                    .clickable { onComplete() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle, 
                    contentDescription = "Complete",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
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
