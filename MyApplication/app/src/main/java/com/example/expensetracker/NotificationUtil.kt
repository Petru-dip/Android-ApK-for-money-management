package com.example.expensetracker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificationUtil {
    private const val CHANNEL_ID = "brimtech_payments"

    private fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "BrimTech Payments",
                NotificationManager.IMPORTANCE_HIGH
            )
            nm.createNotificationChannel(channel)
        }
    }

    fun notifyPaymentDetected(ctx: Context, tx: TransactionEntity) {
        ensureChannel(ctx)
        val title = "Plată detectată: %.2f %s".format(kotlin.math.abs(tx.amount), tx.currency)
        val text  = (tx.merchant ?: "Necunoscut") + " • " + tx.bookingDate

        val notif = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_more)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(tx.description ?: text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(ctx).notify(title.hashCode(), notif)
    }
}
