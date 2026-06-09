package com.example.chattaskai.service

import org.junit.Assert.assertNull
import org.junit.Test

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
}
