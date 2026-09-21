/*
 * Nuvexa Drive
 *
 * SPDX-FileCopyrightText: 2026 Nuvexa contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nuvexa.localserver

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.owncloud.android.R

class LocalDriveServerService : Service() {
    private var server: LocalDriveHttpServer? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopServer()
            stopSelf()
            return START_NOT_STICKY
        }

        startAsForeground()
        if (server == null) {
            runCatching {
                LocalDriveHttpServer(applicationContext).also {
                    server = it
                    it.start()
                }
            }.onFailure {
                LocalDriveServerRuntime.lastError = it.message ?: "Falha ao iniciar servidor local."
                LocalDriveServerRuntime.running = false
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        stopServer()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startAsForeground() {
        val openIntent = Intent(this, LocalDriveActivity::class.java)
        val openPendingIntent = PendingIntent.getActivity(
            this,
            10,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, LocalDriveServerService::class.java).setAction(ACTION_STOP)
        val stopPendingIntent = PendingIntent.getService(
            this,
            11,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.nuvexa_local_server_notification_title))
            .setContentText(getString(R.string.nuvexa_local_server_notification_text))
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .addAction(
                Notification.Action.Builder(
                    null,
                    getString(R.string.nuvexa_local_server_stop),
                    stopPendingIntent
                ).build()
            )
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun stopServer() {
        server?.stop()
        server = null
        LocalDriveServerRuntime.running = false
        LocalDriveServerRuntime.port = null
    }

    private fun createNotificationChannel() {
        val manager = ContextCompat.getSystemService(this, NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.nuvexa_local_server_channel),
            NotificationManager.IMPORTANCE_LOW
        )
        channel.description = getString(R.string.nuvexa_local_server_channel_description)
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val ACTION_STOP = "com.nuvexa.drive.action.STOP_LOCAL_SERVER"
        private const val CHANNEL_ID = "nuvexa_local_server"
        private const val NOTIFICATION_ID = 28071

        fun start(context: android.content.Context) {
            val intent = Intent(context, LocalDriveServerService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: android.content.Context) {
            val intent = Intent(context, LocalDriveServerService::class.java).setAction(ACTION_STOP)
            context.startService(intent)
        }
    }
}
