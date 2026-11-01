package com.qizhi.rest.guard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.qizhi.rest.QizhiApp
import com.qizhi.rest.core.TimeUtils
import com.qizhi.rest.data.Phase
import java.time.ZonedDateTime

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as? QizhiApp ?: return
        val settings = app.container.settings.current
        val scheduler = Scheduler(context)
        when (intent.action) {
            ACTION_BEDTIME -> {
                if (settings.sleepGuardEnabled) {
                    val now = ZonedDateTime.now()
                    val end = TimeUtils.sleepEndFor(now, settings.wakeMinutes)
                    GuardController.startSleep(context, end.toInstant().toEpochMilli())
                }
                scheduler.scheduleAll(settings)
            }

            ACTION_WIND_DOWN -> {
                if (settings.sleepGuardEnabled && settings.windDownMinutes > 0) {
                    GuardNotifications.windDown(context, settings.windDownMinutes)
                }
                scheduler.scheduleAll(settings)
            }

            ACTION_WAKE -> {
                GuardController.wake(context)
                scheduler.scheduleAll(settings)
            }

            ACTION_SLEEP_NOW -> {
                val now = ZonedDateTime.now()
                val end = TimeUtils.sleepEndFor(now, settings.wakeMinutes)
                GuardController.startSleep(context, end.toInstant().toEpochMilli())
                GuardNotifications.cancel(context, GuardNotifications.ID_WIND_DOWN)
                scheduler.scheduleAll(settings)
            }

            ACTION_PHASE_END -> {
                val phase = runCatching {
                    Phase.valueOf(intent.getStringExtra(GuardController.EXTRA_PHASE) ?: "")
                }.getOrNull() ?: return
                val endsAt = intent.getLongExtra(GuardController.EXTRA_ENDS_AT, 0L)
                if (endsAt > 0) GuardController.phaseEnd(context, phase, endsAt)
            }
        }
    }

    companion object {
        const val ACTION_BEDTIME = "com.qizhi.rest.alarm.BEDTIME"
        const val ACTION_WAKE = "com.qizhi.rest.alarm.WAKE"
        const val ACTION_WIND_DOWN = "com.qizhi.rest.alarm.WIND_DOWN"
        const val ACTION_PHASE_END = "com.qizhi.rest.alarm.PHASE_END"
        const val ACTION_SLEEP_NOW = "com.qizhi.rest.alarm.SLEEP_NOW"
    }
}
