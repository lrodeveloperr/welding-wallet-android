package com.goodusestudios.weldinggaswallet.wallet

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.goodusestudios.weldinggaswallet.MainActivity
import com.goodusestudios.weldinggaswallet.R

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED || intent.action == Intent.ACTION_TIME_CHANGED || intent.action == Intent.ACTION_TIMEZONE_CHANGED) {
            WalletStore(context).rescheduleReminders()
            return
        }
        val id = intent.getStringExtra(EXTRA_ID) ?: return
        val gas = intent.getStringExtra(EXTRA_GAS).orEmpty().ifBlank { "cylinder" }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel(CHANNEL, "Cylinder reminders", NotificationManager.IMPORTANCE_DEFAULT))
        }
        val open = PendingIntent.getActivity(
            context,
            id.hashCode(),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Check $gas")
            .setContentText("Open Welding Gas Wallet to review this cylinder.")
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(id.hashCode(), notification) }
    }

    companion object {
        private const val CHANNEL = "cylinder-reminders"
        private const val EXTRA_ID = "cylinder-id"
        private const val EXTRA_GAS = "cylinder-gas"

        fun schedule(context: Context, cylinder: Cylinder, at: Long?): Boolean {
            val intent = Intent(context, ReminderReceiver::class.java).putExtra(EXTRA_ID, cylinder.id).putExtra(EXTRA_GAS, cylinder.gas)
            val pending = PendingIntent.getBroadcast(context, cylinder.id.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val alarm = context.getSystemService(AlarmManager::class.java)
            if (at == null) { alarm.cancel(pending); return true }
            if (at <= System.currentTimeMillis()) return false
            return runCatching { alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending) }.isSuccess
        }
    }
}
