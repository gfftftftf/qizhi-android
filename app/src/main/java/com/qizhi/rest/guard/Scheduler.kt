package com.qizhi.rest.guard

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.qizhi.rest.core.TimeUtils
import com.qizhi.rest.data.Phase
import com.qizhi.rest.data.Settings
import java.time.ZonedDateTime

class Scheduler(context: Context) {

    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(AlarmManager::class.java)

    fun scheduleAll(settings: Settings) {
        cancelDaily()
        if (!settings.sleepGuardEnabled) return
        val now = ZonedDateTime.now()
        val bedtime = TimeUtils.nextSleepStart(now, settings.bedtimeMinutes, settings.wakeMinutes)
        val wake = TimeUtils.nextWake(now, settings.bedtimeMinutes, settings.wakeMinutes)
        scheduleDaily(
            requestCode = REQUEST_BEDTIME,
            triggerAt = bedtime.toInstant().toEpochMilli(),
            action = AlarmReceiver.ACTION_BEDTIME,
        )
        scheduleDaily(
            requestCode = REQUEST_WAKE,
            triggerAt = wake.toInstant().toEpochMilli(),
            action = AlarmReceiver.ACTION_WAKE,
        )
        if (settings.windDownMinutes > 0) {
            val windDown = bedtime.minusMinutes(settings.windDownMinutes.toLong())
            if (windDown.isAfter(now)) {
                scheduleDaily(
                    requestCode = REQUEST_WIND_DOWN,
                    triggerAt = windDown.toInstant().toEpochMilli(),
                    action = AlarmReceiver.ACTION_WIND_DOWN,
                )
            }
        }
    }

    fun schedulePhaseEnd(phase: Phase, endsAt: Long) {
        val intent = Intent(appContext, AlarmReceiver::class.java)
            .setAction(AlarmReceiver.ACTION_PHASE_END)
            .putExtra(GuardController.EXTRA_PHASE, phase.name)
            .putExtra(GuardController.EXTRA_ENDS_AT, endsAt)
        val pi = PendingIntent.getBroadcast(
            appContext,
            REQUEST_PHASE_END,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        setAlarm(endsAt, pi)
    }

    fun cancelPhaseEnd() {
        val intent = Intent(appContext, AlarmReceiver::class.java)
            .setAction(AlarmReceiver.ACTION_PHASE_END)
        val pi = PendingIntent.getBroadcast(
            appContext,
            REQUEST_PHASE_END,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager?.cancel(pi)
    }

    fun cancelDaily() {
        alarmManager?.cancel(dailyIntent(REQUEST_BEDTIME, AlarmReceiver.ACTION_BEDTIME))
        alarmManager?.cancel(dailyIntent(REQUEST_WAKE, AlarmReceiver.ACTION_WAKE))
        alarmManager?.cancel(dailyIntent(REQUEST_WIND_DOWN, AlarmReceiver.ACTION_WIND_DOWN))
    }

    private fun scheduleDaily(requestCode: Int, triggerAt: Long, action: String) {
        if (triggerAt <= System.currentTimeMillis()) return
        setAlarm(triggerAt, dailyIntent(requestCode, action))
    }

    private fun dailyIntent(requestCode: Int, action: String): PendingIntent =
        PendingIntent.getBroadcast(
            appContext,
            requestCode,
            Intent(appContext, AlarmReceiver::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun setAlarm(triggerAt: Long, pendingIntent: PendingIntent) {
        val am = alarmManager ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        }.onFailure {
            runCatching {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        }
    }

    private companion object {
        const val REQUEST_BEDTIME = 101
        const val REQUEST_WAKE = 102
        const val REQUEST_WIND_DOWN = 103
        const val REQUEST_PHASE_END = 104
    }
}
