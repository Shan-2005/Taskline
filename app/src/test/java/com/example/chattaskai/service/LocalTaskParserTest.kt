package com.example.chattaskai.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class LocalTaskParserTest {

    private val parser = LocalTaskParser()

    @Test
    fun strictMode_parsesActionAndTimeTask() {
        val parsed = parser.parse("Please call Raj tomorrow at 10:30 am", strict = true)

        assertNotNull(parsed)
        assertEquals(true, parsed?.is_task)
        assertEquals("10:30", parsed?.time)
    }

    @Test
    fun strictMode_rejectsCasualConversation() {
        val parsed = parser.parse("Hey, how are you doing today?", strict = true)

        assertNull(parsed)
    }

    @Test
    fun lenientMode_acceptsActionWithoutExplicitRequest() {
        val parsed = parser.parse("Submit report", strict = false)

        assertNotNull(parsed)
        assertEquals(true, parsed?.is_task)
    }

    @Test
    fun strictMode_parsesMultiLineNotificationText() {
        val input = "Project Update\nPlease send the revised report by Friday at 3:30 pm"

        val parsed = parser.parse(input, strict = true)

        assertNotNull(parsed)
        assertEquals(true, parsed?.is_task)
        assertEquals("15:30", parsed?.time)
    }

    @Test
    fun strictMode_parsesShorthandWorkNotification() {
        val input = "U have work at now 14:24 hr today"

        val parsed = parser.parse(input, strict = true)

        assertNotNull(parsed)
        assertEquals(true, parsed?.is_task)
        assertEquals("14:24", parsed?.time)
    }
}
