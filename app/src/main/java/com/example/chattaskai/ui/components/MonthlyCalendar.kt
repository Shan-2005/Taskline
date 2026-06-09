package com.example.chattaskai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chattaskai.data.database.TaskEntity
import com.example.chattaskai.ui.theme.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun MonthlyCalendar(
    tasks: List<TaskEntity>,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalLiquidColors.current
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }

    // Group tasks by date for fast lookup
    val tasksByDate = remember(tasks) {
        tasks.groupBy { it.deadlineDate }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .glassMorphism(alpha = 0.05f)
            .padding(16.dp)
    ) {
        Column {
            // Month Selector Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowLeft,
                        contentDescription = "Previous Month",
                        tint = Color.White
                    )
                }

                Text(
                    text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontLoader.ndot(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )

                IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = "Next Month",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Days of the Week Header
            Row(modifier = Modifier.fillMaxWidth()) {
                val daysOfWeek = listOf("S", "M", "T", "W", "T", "F", "S")
                daysOfWeek.forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold
                        ),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Days Grid
            val firstDayOfMonth = currentMonth.atDay(1)
            val dayOfWeekOfFirst = firstDayOfMonth.dayOfWeek.value % 7 // 0 = Sunday, 1 = Monday...
            val daysInMonth = currentMonth.lengthOfMonth()
            
            val totalCells = 42
            val previousMonth = currentMonth.minusMonths(1)
            val daysInPrevMonth = previousMonth.lengthOfMonth()

            val rows = totalCells / 7
            
            for (row in 0 until rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (col in 0 until 7) {
                        val cellIndex = row * 7 + col
                        
                        val date = when {
                            cellIndex < dayOfWeekOfFirst -> {
                                val prevDayNum = daysInPrevMonth - dayOfWeekOfFirst + cellIndex + 1
                                previousMonth.atDay(prevDayNum)
                            }
                            cellIndex < dayOfWeekOfFirst + daysInMonth -> {
                                val dayNum = cellIndex - dayOfWeekOfFirst + 1
                                currentMonth.atDay(dayNum)
                            }
                            else -> {
                                val nextDayNum = cellIndex - dayOfWeekOfFirst - daysInMonth + 1
                                currentMonth.plusMonths(1).atDay(nextDayNum)
                            }
                        }

                        val isCurrentMonth = date.month == currentMonth.month && date.year == currentMonth.year
                        val isSelected = selectedDate != null && date.isEqual(selectedDate)
                        
                        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                        val dateStr = date.format(formatter)
                        val dayTasks = tasksByDate[dateStr] ?: emptyList()
                        val hasTasks = dayTasks.isNotEmpty()

                        val isToday = date.isEqual(LocalDate.now())

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    when {
                                        isSelected -> colors.purple.copy(alpha = 0.25f)
                                        isToday -> Color.White.copy(alpha = 0.08f)
                                        else -> Color.Transparent
                                    }
                                )
                                .border(
                                    width = 1.dp,
                                    color = when {
                                        isSelected -> colors.purple
                                        isToday -> colors.cyan.copy(alpha = 0.5f)
                                        else -> Color.Transparent
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    if (isSelected) {
                                        onDateSelected(null)
                                    } else {
                                        onDateSelected(date)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = date.dayOfMonth.toString(),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = when {
                                            isSelected -> Color.White
                                            isCurrentMonth -> Color.White.copy(alpha = 0.9f)
                                            else -> Color.White.copy(alpha = 0.25f)
                                        },
                                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                                
                                if (hasTasks) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (dayTasks.any { it.priority.lowercase() == "high" }) {
                                                    UrgentPriority
                                                } else if (dayTasks.any { it.priority.lowercase() == "medium" }) {
                                                    MediumPriority
                                                } else {
                                                    colors.cyan
                                                }
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
