package com.example.chattaskai.service

// Parsed Output Model used by Local and (previously) AI parsers
data class ParsedTask(
    val task: String,
    val date: String,
    val time: String,
    val priority: String,
    val category: String = "General",
    val is_task: Boolean
)
