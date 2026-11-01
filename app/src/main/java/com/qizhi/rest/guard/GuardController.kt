package com.qizhi.rest.guard

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.qizhi.rest.data.Phase
import com.qizhi.rest.data.RestReason

object GuardController {

    const val ACTION_START_FOCUS = "com.qizhi.rest.action.START_FOCUS"
    const val ACTION_START_REST = "com.qizhi.rest.action.START_REST"
    const val ACTION_START_SLEEP = "com.qizhi.rest.action.START_SLEEP"
    const val ACTION_STOP = "com.qizhi.rest.action.STOP"
    const val ACTION_STOP_FOCUS = "com.qizhi.rest.action.STOP_FOCUS"
    const val ACTION_WAKE = "com.qizhi.rest.action.WAKE"
    const val ACTION_PHASE_END = "com.qizhi.rest.action.PHASE_END"
    const val ACTION_SCREEN_OFF = "com.qizhi.rest.action.SCREEN_OFF"
    const val ACTION_RESUME = "com.qizhi.rest.action.RESUME"
    const val ACTION_REFRESH = "com.qizhi.rest.action.REFRESH"
    const val ACTION_CANCEL_EMERGENCY = "com.qizhi.rest.action.CANCEL_EMERGENCY"

    const val EXTRA_MINUTES = "minutes"
    const val EXTRA_ENDS_AT = "ends_at"
    const val EXTRA_STARTED_AT = "started_at"
    const val EXTRA_PHASE = "phase"
    const val EXTRA_PACKAGE = "package"
    const val EXTRA_REASON = "reason"

    fun startFocus(context: Context, minutes: Int) {
        send(
            context,
            Intent(context, GuardService::class.java)
                .setAction(ACTION_START_FOCUS)
                .putExtra(EXTRA_MINUTES, minutes),
        )
    }

    fun startRest(context: Context, minutes: Int, reason: RestReason = RestReason.MANUAL) {
        send(
            context,
            Intent(context, GuardService::class.java)
                .setAction(ACTION_START_REST)
                .putExtra(EXTRA_MINUTES, minutes)
                .putExtra(EXTRA_REASON, reason.name),
        )
    }

    fun startSleep(context: Context, endsAt: Long) {
        send(
            context,
            Intent(context, GuardService::class.java)
                .setAction(ACTION_START_SLEEP)
                .putExtra(EXTRA_ENDS_AT, endsAt),
        )
    }

    fun stop(context: Context) {
        send(context, Intent(context, GuardService::class.java).setAction(ACTION_STOP))
    }

    fun stopFocus(context: Context) {
        send(context, Intent(context, GuardService::class.java).setAction(ACTION_STOP_FOCUS))
    }

    fun wake(context: Context) {
        send(context, Intent(context, GuardService::class.java).setAction(ACTION_WAKE))
    }

    fun phaseEnd(context: Context, phase: Phase, endsAt: Long) {
        send(
            context,
            Intent(context, GuardService::class.java)
                .setAction(ACTION_PHASE_END)
                .putExtra(EXTRA_PHASE, phase.name)
                .putExtra(EXTRA_ENDS_AT, endsAt),
        )
    }

    fun screenOff(context: Context) {
        send(context, Intent(context, GuardService::class.java).setAction(ACTION_SCREEN_OFF))
    }

    fun resume(context: Context) {
        send(context, Intent(context, GuardService::class.java).setAction(ACTION_RESUME))
    }

    fun refresh(context: Context) {
        send(context, Intent(context, GuardService::class.java).setAction(ACTION_REFRESH))
    }

    fun cancelEmergency(context: Context) {
        send(
            context,
            Intent(context, GuardService::class.java).setAction(ACTION_CANCEL_EMERGENCY),
        )
    }

    fun resumeIfNeeded(context: Context) {
        val app = context.applicationContext as? com.qizhi.rest.QizhiApp ?: return
        val session = app.container.session.session ?: return
        if (session.endsAt > System.currentTimeMillis()) {
            resume(context)
        } else {
            phaseEnd(context, session.phase, session.endsAt)
        }
    }

    private fun send(context: Context, intent: Intent) {
        runCatching {
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
