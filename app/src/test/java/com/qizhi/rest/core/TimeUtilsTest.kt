package com.qizhi.rest.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class TimeUtilsTest {

    private val zone: ZoneId = ZoneId.of("Asia/Shanghai")
    private val jan1 = LocalDate.of(2026, 1, 1)

    private fun at(day: LocalDate, hour: Int, minute: Int): ZonedDateTime =
        day.atTime(hour, minute).atZone(zone)

    @Test
    fun overnightSleepWindow() {
        val bed = 23 * 60 + 30
        val wake = 7 * 60
        assertTrue(TimeUtils.isInSleepWindow(at(jan1, 23, 45), bed, wake))
        assertTrue(TimeUtils.isInSleepWindow(at(jan1, 6, 59), bed, wake))
        assertFalse(TimeUtils.isInSleepWindow(at(jan1, 7, 0), bed, wake))
        assertFalse(TimeUtils.isInSleepWindow(at(jan1, 12, 0), bed, wake))
        assertFalse(TimeUtils.isInSleepWindow(at(jan1, 23, 29), bed, wake))
    }

    @Test
    fun daytimeSleepWindow() {
        val bed = 13 * 60
        val wake = 14 * 60
        assertTrue(TimeUtils.isInSleepWindow(at(jan1, 13, 30), bed, wake))
        assertFalse(TimeUtils.isInSleepWindow(at(jan1, 12, 59), bed, wake))
        assertFalse(TimeUtils.isInSleepWindow(at(jan1, 14, 0), bed, wake))
    }

    @Test
    fun equalTimesAreInvalid() {
        assertFalse(TimeUtils.isInSleepWindow(at(jan1, 12, 0), 600, 600))
    }

    @Test
    fun nextOccurrenceRollsForward() {
        val now = at(jan1, 10, 0)
        assertEquals(at(jan1, 11, 0), TimeUtils.nextOccurrence(now, 11 * 60))
        assertEquals(at(jan1.plusDays(1), 9, 0), TimeUtils.nextOccurrence(now, 9 * 60))
    }

    @Test
    fun currentSleepWindowAtNight() {
        val (start, end) = TimeUtils.currentOrNextSleepWindow(
            at(jan1, 23, 45),
            23 * 60 + 30,
            7 * 60,
        )
        assertEquals(at(jan1, 23, 30), start)
        assertEquals(at(jan1.plusDays(1), 7, 0), end)
    }

    @Test
    fun currentSleepWindowInMorning() {
        val (start, end) = TimeUtils.currentOrNextSleepWindow(
            at(jan1, 6, 0),
            23 * 60 + 30,
            7 * 60,
        )
        assertEquals(at(jan1.minusDays(1), 23, 30), start)
        assertEquals(at(jan1, 7, 0), end)
    }

    @Test
    fun nextSleepWindowDuringDay() {
        val (start, end) = TimeUtils.currentOrNextSleepWindow(
            at(jan1, 12, 0),
            23 * 60 + 30,
            7 * 60,
        )
        assertEquals(at(jan1, 23, 30), start)
        assertEquals(at(jan1.plusDays(1), 7, 0), end)
    }

    @Test
    fun sleepEndWrapsMidnight() {
        assertEquals(
            at(jan1.plusDays(1), 7, 0),
            TimeUtils.sleepEndFor(at(jan1, 23, 30), 7 * 60),
        )
    }

    @Test
    fun formats() {
        assertEquals("23:05", TimeUtils.formatClock(23 * 60 + 5))
        assertEquals("00:00", TimeUtils.formatClock(1440))
        assertEquals("7 小时 30 分", TimeUtils.formatCompactDuration(450))
        assertEquals("02:05", TimeUtils.formatCountdown(125_000))
        assertEquals("1:00:00", TimeUtils.formatCountdown(3_600_000))
    }

    @Test
    fun dayKeyUsesZone() {
        val epoch = at(jan1, 0, 30).toInstant().toEpochMilli()
        assertEquals("2026-01-01", TimeUtils.dayKey(epoch, zone))
        val utc = TimeUtils.dayKey(epoch, ZoneId.of("UTC"))
        assertEquals("2025-12-31", utc)
    }
}
