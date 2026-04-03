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
}
