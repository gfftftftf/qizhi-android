package com.qizhi.rest.core

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

object TimeUtils {

    private val clockFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.CHINA)

    fun now(zone: ZoneId = ZoneId.systemDefault()): ZonedDateTime = ZonedDateTime.now(zone)

    fun atMinuteOfDay(day: LocalDate, minuteOfDay: Int, zone: ZoneId): ZonedDateTime {
        val normalized = ((minuteOfDay % 1440) + 1440) % 1440
        return day.atTime(normalized / 60, normalized % 60).atZone(zone)
    }

    fun nextOccurrence(
        now: ZonedDateTime,
        minuteOfDay: Int,
        inclusive: Boolean = false,
    ): ZonedDateTime {
        val today = atMinuteOfDay(now.toLocalDate(), minuteOfDay, now.zone)
        val isFuture = if (inclusive) !today.isBefore(now) else today.isAfter(now)
        return if (isFuture) today else today.plusDays(1)
    }

    /**
     * 是否处于睡眠窗口。支持跨午夜。入睡与起床时间相同时视为无效配置。
     */
    fun isInSleepWindow(now: ZonedDateTime, bedtimeMinutes: Int, wakeMinutes: Int): Boolean {
        if (bedtimeMinutes == wakeMinutes) return false
        val minute = now.hour * 60 + now.minute
        return if (bedtimeMinutes < wakeMinutes) {
            minute >= bedtimeMinutes && minute < wakeMinutes
        } else {
            minute >= bedtimeMinutes || minute < wakeMinutes
        }
    }

    fun sleepEndFor(start: ZonedDateTime, wakeMinutes: Int): ZonedDateTime {
        val end = atMinuteOfDay(start.toLocalDate(), wakeMinutes, start.zone)
        return if (end.isAfter(start)) end else end.plusDays(1)
    }

    /**
     * 返回当前或下一段睡眠窗口 [start, end)。
     */
    fun currentOrNextSleepWindow(
        now: ZonedDateTime,
        bedtimeMinutes: Int,
        wakeMinutes: Int,
    ): Pair<ZonedDateTime, ZonedDateTime> {
        if (bedtimeMinutes == wakeMinutes) {
            val start = nextOccurrence(now, bedtimeMinutes)
            return start to start.plusHours(8)
        }
        if (isInSleepWindow(now, bedtimeMinutes, wakeMinutes)) {
            val minute = now.hour * 60 + now.minute
            val start = if (bedtimeMinutes < wakeMinutes) {
                atMinuteOfDay(now.toLocalDate(), bedtimeMinutes, now.zone)
            } else if (minute >= bedtimeMinutes) {
                atMinuteOfDay(now.toLocalDate(), bedtimeMinutes, now.zone)
            } else {
                atMinuteOfDay(now.toLocalDate().minusDays(1), bedtimeMinutes, now.zone)
            }
            return start to sleepEndFor(start, wakeMinutes)
        }
        val start = nextOccurrence(now, bedtimeMinutes)
        return start to sleepEndFor(start, wakeMinutes)
    }

    fun nextSleepStart(
        now: ZonedDateTime,
        bedtimeMinutes: Int,
        wakeMinutes: Int,
    ): ZonedDateTime = currentOrNextSleepWindow(now, bedtimeMinutes, wakeMinutes).first

    fun nextWake(
        now: ZonedDateTime,
        bedtimeMinutes: Int,
        wakeMinutes: Int,
    ): ZonedDateTime = currentOrNextSleepWindow(now, bedtimeMinutes, wakeMinutes).second

    fun minutesUntil(now: ZonedDateTime, target: ZonedDateTime): Long =
        java.time.Duration.between(now, target).toMinutes().coerceAtLeast(0L)

    fun formatClock(minutes: Int): String {
        val normalized = ((minutes % 1440) + 1440) % 1440
        return String.format(Locale.CHINA, "%02d:%02d", normalized / 60, normalized % 60)
    }

    fun formatClock(time: LocalTime): String = time.format(clockFormatter)

    fun formatDate(date: LocalDate): String =
        "${date.monthValue} 月 ${date.dayOfMonth} 日"

    fun formatWeekday(date: LocalDate): String = when (date.dayOfWeek.value) {
        1 -> "周一"
        2 -> "周二"
        3 -> "周三"
        4 -> "周四"
        5 -> "周五"
        6 -> "周六"
        else -> "周日"
    }

    fun formatCountdown(millis: Long): String {
        val total = (millis.coerceAtLeast(0L) + 999L) / 1000L
        val hours = total / 3600
        val minutes = (total % 3600) / 60
        val seconds = total % 60
        return if (hours > 0) {
            String.format(Locale.CHINA, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.CHINA, "%02d:%02d", minutes, seconds)
        }
    }

    fun formatCompactDuration(minutes: Int): String {
        if (minutes <= 0) return "0 分钟"
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h > 0 && m > 0 -> "${h} 小时 ${m} 分"
            h > 0 -> "${h} 小时"
            else -> "${m} 分钟"
        }
    }

    fun formatUsage(minutes: Long): String = when {
        minutes <= 0 -> "未使用"
        minutes < 60 -> "${minutes} 分钟"
        else -> "${minutes / 60} 小时 ${minutes % 60} 分"
    }

    fun formatRelativeMinutes(minutes: Long): String = when {
        minutes <= 0 -> "现在"
        minutes < 60 -> "还有 ${minutes} 分钟"
        else -> "还有 ${minutes / 60} 小时 ${minutes % 60} 分"
    }

    fun greeting(now: ZonedDateTime = now()): String = when (now.hour) {
        in 5..8 -> "早安，慢慢醒来"
        in 9..11 -> "上午好，留一点空白"
        in 12..13 -> "午间好，记得小憩"
        in 14..17 -> "下午好，别绷得太紧"
        in 18..22 -> "晚上好，准备收拢一天"
        else -> "夜深了，该休息了"
    }

    fun dayKey(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate().toString()

    fun todayKey(zone: ZoneId = ZoneId.systemDefault()): String = LocalDate.now(zone).toString()

    fun lastNDates(n: Int, zone: ZoneId = ZoneId.systemDefault()): List<LocalDate> {
        val today = LocalDate.now(zone)
        return (n - 1 downTo 0).map { today.minusDays(it.toLong()) }
    }

    fun safeMinutesBetween(start: Long, end: Long): Int =
        ((end - start).coerceAtLeast(0L) / 60_000L).toInt()

    fun clockDifferenceMinutes(a: Int, b: Int): Int {
        val raw = abs(a - b)
        return minOf(raw, 1440 - raw)
    }
}
