package com.ansh.ytmusicpreimum

import com.ansh.ytmusicpreimum.ui.components.formatTime
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTimeUnitTest {

    @Test
    fun testFormatTime_zeroMs() {
        assertEquals("0:00", formatTime(0L))
    }

    @Test
    fun testFormatTime_exactSeconds() {
        assertEquals("0:45", formatTime(45000L))
        assertEquals("0:05", formatTime(5000L))
    }

    @Test
    fun testFormatTime_minutesAndSeconds() {
        assertEquals("3:25", formatTime(205000L))
        assertEquals("10:00", formatTime(600000L))
    }

    @Test
    fun testFormatTime_longerTracks() {
        assertEquals("65:30", formatTime(3930000L))
    }
}
