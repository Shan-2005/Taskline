package com.example.chattaskai.service

import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale

class GeminiTaskParserTest {

    @Test
    fun parse_withEmptyApiKey_returnsNull() {
        val parser = GeminiTaskParser("")
        val result = parser.parse("Please call Raj tomorrow at 10:30 am", strict = true)
        assertNull(result)
    }

    @Test
    fun parse_withBlankApiKey_returnsNull() {
        val parser = GeminiTaskParser("   ")
        val result = parser.parse("Please call Raj tomorrow at 10:30 am", strict = true)
        assertNull(result)
    }

    @Test
    fun testLiveGeminiParsing() {
        val props = java.util.Properties()
        var propsFile = java.io.File("gradle.properties")
        if (!propsFile.exists()) {
            propsFile = java.io.File("../gradle.properties")
        }
        if (propsFile.exists()) {
            propsFile.inputStream().use { props.load(it) }
        } else {
            println("Properties file not found at ${propsFile.absolutePath}")
        }
        val b64Key = props.getProperty("GEMINI_API_KEY_B64") ?: ""
        println("B64 Key from properties: '$b64Key'")
        if (b64Key.isNotBlank()) {
            val apiKey = String(java.util.Base64.getDecoder().decode(b64Key), Charsets.UTF_8)
            println("Decoded Key: '$apiKey'")
            val parser = GeminiTaskParser(apiKey)
            val result = parser.parse("Hey John, could you please finish the architectural diagrams and upload the draft report by tomorrow at 3:30 PM?", strict = true)
            println("Parser Result: $result")
            if (result != null) {
                println("Parsed Task: ${result.task}")
                println("Parsed Date: ${result.date}")
                println("Parsed Time: ${result.time}")
                println("Parsed Priority: ${result.priority}")
                println("Parsed Category: ${result.category}")
                println("Parsed IsTask: ${result.is_task}")
            }
        }
    }
}
