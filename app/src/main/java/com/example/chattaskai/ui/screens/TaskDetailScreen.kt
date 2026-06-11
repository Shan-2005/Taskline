package com.example.chattaskai.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.chattaskai.ui.TaskViewModel
import com.example.chattaskai.ui.components.*
import com.example.chattaskai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(taskId: Long, viewModel: TaskViewModel, onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val pendingTasks by viewModel.pendingTasks.collectAsState()
    val completedTasks by viewModel.completedTasks.collectAsState()
    val reviewTasks by viewModel.reviewTasks.collectAsState()
    val task = remember(taskId, pendingTasks, completedTasks) {
        pendingTasks.find { it.id == taskId }
            ?: reviewTasks.find { it.id == taskId }
            ?: completedTasks.find { it.id == taskId }
    }

    var isEditing by remember { mutableStateOf(false) }
    var editedTitle by remember { mutableStateOf(task?.title ?: "") }
    var editedDate by remember { mutableStateOf(task?.deadlineDate ?: "") }
    var editedTime by remember { mutableStateOf(task?.deadlineTime ?: "") }
    var editedCategory by remember { mutableStateOf(task?.category ?: "General") }

    LaunchedEffect(task) {
        if (task == null) {
            onBack()
        }
    }

    if (task == null) {
        // Show a placeholder or loading while the LaunchedEffect navigates back
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Black,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Text(
                        text = "Task Details",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            color = Color.White
                        )
                    )
                    Row {
                        if (isEditing) {
                            IconButton(onClick = {
                                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                                val timestamp = try {
                                    sdf.parse("$editedDate $editedTime")?.time ?: task.deadlineTimestamp
                                } catch (e: Exception) {
                                    task.deadlineTimestamp
                                }
                                viewModel.upsertTask(context, task.copy(
                                    title = editedTitle,
                                    deadlineDate = editedDate,
                                    deadlineTime = editedTime,
                                    deadlineTimestamp = timestamp,
                                    category = editedCategory
                                ))
                                isEditing = false
                            }) {
                                Icon(Icons.Default.Check, contentDescription = "Save", tint = LocalLiquidColors.current.cyan)
                            }
                        } else {
                            IconButton(onClick = { isEditing = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White)
                            }
                        }
                        IconButton(onClick = { 
                            viewModel.deleteTask(context, taskId)
                            onBack()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = UrgentPriority)
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LiquidBackground()
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                Spacer(modifier = Modifier.height(20.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassMorphism()
                        .padding(20.dp)
                ) {
                    Column {
                        Text("Task Title", style = MaterialTheme.typography.labelLarge, color = LocalLiquidColors.current.purple)
                        Spacer(modifier = Modifier.height(8.dp))
                        if (isEditing) {
                            OutlinedTextField(
                                value = editedTitle,
                                onValueChange = { editedTitle = it },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                        } else {
                            Text(task.title, style = MaterialTheme.typography.titleLarge, color = Color.White)
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(1f)) {
                                val colors = LocalLiquidColors.current
                                Text("Date", style = MaterialTheme.typography.labelLarge, color = colors.cyan)
                                if (isEditing) {
                                    OutlinedTextField(value = editedDate, onValueChange = { editedDate = it })
                                } else {
                                    Text(task.deadlineDate, style = MaterialTheme.typography.bodyLarge, color = Color.White)
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Time", style = MaterialTheme.typography.labelLarge, color = LocalLiquidColors.current.cyan)
                                if (isEditing) {
                                    OutlinedTextField(value = editedTime, onValueChange = { editedTime = it })
                                } else {
                                    Text(task.deadlineTime, style = MaterialTheme.typography.bodyLarge, color = Color.White)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Text("Priority", style = MaterialTheme.typography.labelLarge, color = LocalLiquidColors.current.pink)
                        Spacer(modifier = Modifier.height(8.dp))
                        PriorityBadge(task.priority)

                        Spacer(modifier = Modifier.height(24.dp))
                        Text("Category", style = MaterialTheme.typography.labelLarge, color = LocalLiquidColors.current.purple)
                        Spacer(modifier = Modifier.height(8.dp))
                        if (isEditing) {
                            val categories = listOf("General", "Work", "Personal", "Shopping", "Health")
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                categories.forEach { cat ->
                                    val isSelected = editedCategory == cat
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(if (isSelected) LocalLiquidColors.current.cyan else Color.White.copy(alpha = 0.15f))
                                            .clickable { editedCategory = cat }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(cat, color = if (isSelected) Color.Black else Color.White, fontSize = 14.sp)
                                    }
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(LocalLiquidColors.current.purple.copy(alpha = 0.2f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(task.category, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                // Subtasks Checklist Section
                val subTasks by viewModel.getSubTasksForTask(task.id).collectAsState(initial = emptyList())
                var newSubTaskTitle by remember { mutableStateOf("") }

                Spacer(modifier = Modifier.height(24.dp))
                Text("Subtasks Checklist", style = MaterialTheme.typography.headlineSmall, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassMorphism()
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        subTasks.forEach { sub ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Checkbox(
                                        checked = sub.isCompleted,
                                        onCheckedChange = { viewModel.toggleSubTaskCompletion(sub) },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = LocalLiquidColors.current.cyan,
                                            uncheckedColor = Color.White.copy(alpha = 0.5f)
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        sub.title,
                                        color = if (sub.isCompleted) Color.White.copy(alpha = 0.4f) else Color.White,
                                        style = if (sub.isCompleted) MaterialTheme.typography.bodyLarge.copy(
                                            textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                                        ) else MaterialTheme.typography.bodyLarge
                                    )
                                }
                                IconButton(onClick = { viewModel.deleteSubTask(sub.id) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Subtask",
                                        tint = Color.White.copy(alpha = 0.4f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // Add subtask input row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newSubTaskTitle,
                                onValueChange = { newSubTaskTitle = it },
                                placeholder = { Text("New subtask...", color = Color.White.copy(alpha = 0.5f)) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = LocalLiquidColors.current.cyan,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (newSubTaskTitle.isNotBlank()) {
                                        viewModel.addSubTask(newSubTaskTitle.trim(), task.id)
                                        newSubTaskTitle = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = LocalLiquidColors.current.cyan)
                            ) {
                                Text("Add", color = Color.Black)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text("Original Message", style = MaterialTheme.typography.headlineSmall, color = Color.White.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassMorphism()
                        .padding(16.dp)
                ) {
                    Text(
                        task.originalMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                if (task.status == "needs_review") {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                viewModel.approveReviewTask(context, task)
                                onBack()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Add to Tasks", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = {
                                viewModel.dismissReviewTask(context, task)
                                onBack()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Dismiss", fontWeight = FontWeight.Bold)
                        }
                    }
                } else if (task.status != "completed") {
                    Button(
                        onClick = { viewModel.completeTask(context, task); onBack() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .clip(RoundedCornerShape(30.dp))
                            .liquidGradient(listOf(LocalLiquidColors.current.cyan, LocalLiquidColors.current.purple, LocalLiquidColors.current.pink)),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        elevation = ButtonDefaults.buttonElevation(0.dp)
                    ) {
                        Text("Mark as Completed", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }
            }
        }
    }
}
}
