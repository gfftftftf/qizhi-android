package com.qizhi.rest.guard

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.qizhi.rest.MainActivity
import com.qizhi.rest.R
import com.qizhi.rest.core.TimeUtils
import com.qizhi.rest.data.GuardSession
import com.qizhi.rest.data.Phase
import com.qizhi.rest.privilege.PermissionTools

object GuardNotifications {

    const val CHANNEL_GUARD = "guard"
    const val CHANNEL_REMINDER = "reminder"
    const val CHANNEL_COMPLETE = "complete"
    const val CHANNEL_ALERT = "alert"

    const val ID_GUARD = 1001
    const val ID_COMPLETE = 1002
    const val ID_WIND_DOWN = 1003
    const val ID_BLOCKED = 1004
    const val ID_MORNING = 1005

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannels(
                listOf(
                    NotificationChannel(
                        CHANNEL_GUARD,
                        "守护进行中",
                        NotificationManager.IMPORTANCE_LOW,
                    ).apply {
                        description = "专注、休息与睡眠守护的常驻通知"
                        setShowBadge(false)
                        enableVibration(false)
                        setSound(null, null)
                        lockscreenVisibility = Notification.VISIBILITY_PRIVATE
                    },
                    NotificationChannel(
                        CHANNEL_REMINDER,
                        "节律提醒",
                        NotificationManager.IMPORTANCE_DEFAULT,
                    ).apply {
                        description = "睡前准备、起床与休息提醒"
                        setShowBadge(true)
                    },
                    NotificationChannel(
                        CHANNEL_COMPLETE,
                        "完成与总结",
                        NotificationManager.IMPORTANCE_DEFAULT,
                    ).apply {
                        description = "专注结束与每日睡眠总结"
                        setShowBadge(false)
                    },
                    NotificationChannel(
                        CHANNEL_ALERT,
                        "管控提醒",
                        NotificationManager.IMPORTANCE_LOW,
                    ).apply {
                        description = "应用达到每日限额时的安静提醒"
                        setShowBadge(false)
                        setSound(null, null)
                    },
                ),
            )
        }
    }

    fun starting(context: Context): Notification =
        NotificationCompat.Builder(context, CHANNEL_GUARD)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("栖止正在启动")
            .setContentText("正在恢复守护状态…")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()

    fun guardNotification(context: Context, session: GuardSession): Notification {
        val phaseTitle = when (session.phase) {
            Phase.FOCUS -> "专注中"
            Phase.REST -> "休息中"
            Phase.SLEEP -> "睡眠守护中"
            Phase.IDLE -> "栖止"
        }
        val title = "$phaseTitle · ${TimeUtils.formatCompactDuration(session.durationMinutes)}"
        val text = when (session.phase) {
            Phase.FOCUS -> "把注意力放回一件事，剩下的交给时间。"
            Phase.REST -> "离开屏幕，慢慢呼吸。休息不是奖励，是节律。"
            Phase.SLEEP -> "灯光调暗，世界安静下来。明早见。"
            Phase.IDLE -> "让休息成为秩序。"
        }
        val builder = NotificationCompat.Builder(context, CHANNEL_GUARD)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(mainPendingIntent(context))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setWhen(session.endsAt)
            .setShowWhen(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        when (session.phase) {
            Phase.FOCUS -> {
                builder.addAction(
                    0,
                    "结束专注",
                    servicePendingIntent(context, GuardController.ACTION_STOP_FOCUS, 21),
                )
                builder.addAction(
                    0,
                    "立即休息",
                    PendingIntent.getService(
                        context,
                        23,
                        Intent(context, GuardService::class.java)
                            .setAction(GuardController.ACTION_START_REST)
                            .putExtra(GuardController.EXTRA_MINUTES, 10)
                            .putExtra(GuardController.EXTRA_REASON, "MANUAL"),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    ),
                )
            }

            Phase.REST -> {
                builder.addAction(
                    0,
                    "提前结束",
                    exitPendingIntent(context, 24),
                )
            }

            Phase.SLEEP -> {
                builder.addAction(
                    0,
                    "去睡觉",
                    servicePendingIntent(context, GuardController.ACTION_SCREEN_OFF, 25),
                )
                builder.addAction(
                    0,
                    "提前起床",
                    exitPendingIntent(context, 26),
                )
            }

            Phase.IDLE -> Unit
        }
        return builder.build()
    }

    fun completion(context: Context, title: String, text: String, id: Int = ID_COMPLETE) {
        val notification = NotificationCompat.Builder(context, CHANNEL_COMPLETE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(mainPendingIntent(context))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        notify(context, id, notification)
    }

    fun windDown(context: Context, minutes: Int) {
        val sleepNow = PendingIntent.getBroadcast(
            context,
            31,
            Intent(context, AlarmReceiver::class.java)
                .setAction(AlarmReceiver.ACTION_SLEEP_NOW),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDER)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("还有 $minutes 分钟入睡")
            .setContentText("把灯调暗，放下屏幕，给今天一个缓慢的句号。")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("把灯调暗，放下屏幕，给今天一个缓慢的句号。栖止会在入睡时间进入睡眠守护。"),
            )
            .setContentIntent(mainPendingIntent(context))
            .addAction(0, "提前进入睡前模式", sleepNow)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        notify(context, ID_WIND_DOWN, notification)
    }

    fun blocked(context: Context, packageLabel: String, message: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ALERT)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("$packageLabel 暂时不可用")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(mainPendingIntent(context))
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        notify(context, ID_BLOCKED, notification)
    }

    fun morning(context: Context, sleepMinutes: Int) {
        val text = if (sleepMinutes > 0) {
            "昨夜守护了 ${TimeUtils.formatCompactDuration(sleepMinutes)}。慢慢睁眼，先喝一杯水。"
        } else {
            "早安。新的一天，从一次深呼吸开始。"
        }
        completion(context, "早安", text, ID_MORNING)
    }

    private fun exitPendingIntent(context: Context, requestCode: Int): PendingIntent =
        PendingIntent.getActivity(
            context,
            requestCode,
            Intent(context, ExitConfirmActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun servicePendingIntent(
        context: Context,
        action: String,
        requestCode: Int,
    ): PendingIntent = PendingIntent.getService(
        context,
        requestCode,
        Intent(context, GuardService::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun mainPendingIntent(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context,
            10,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    fun notify(context: Context, id: Int, notification: Notification) {
        if (!PermissionTools.hasNotifications(context)) return
        runCatching {
            NotificationManagerCompat.from(context).notify(id, notification)
        }
    }

    fun cancel(context: Context, id: Int) {
        runCatching { NotificationManagerCompat.from(context).cancel(id) }
    }
}
