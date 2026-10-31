package ph.smarc

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/** Privacy-safe reminders: the text only ever says how many accounts need attention. No names, no amounts, also on the lock screen. */
object Notify {
    private const val CH = "smarc_reminders"
    fun post(ctx: Context, n: Int) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH, "Payment reminders", NotificationManager.IMPORTANCE_DEFAULT).apply { lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE })
        val open = PendingIntent.getActivity(ctx, 0, Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val text = "$n account${if (n == 1) "" else "s"} need attention"
        val pub = NotificationCompat.Builder(ctx, CH).setSmallIcon(R.drawable.ic_notify).setContentTitle("SmarC").setContentText("Reminders available").build()
        NotificationManagerCompat.from(ctx).notify(1001, NotificationCompat.Builder(ctx, CH).setSmallIcon(R.drawable.ic_notify).setContentTitle("SmarC").setContentText(text)
            .setContentIntent(open).setAutoCancel(true).setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setPublicVersion(pub).build())
    }
    fun schedule(ctx: Context, on: Boolean) {
        val wm = WorkManager.getInstance(ctx)
        if (on) wm.enqueueUniquePeriodicWork("smarc_daily", ExistingPeriodicWorkPolicy.UPDATE, PeriodicWorkRequestBuilder<DailyCheckWorker>(12, TimeUnit.HOURS).build()) else wm.cancelUniqueWork("smarc_daily")
    }
}

class DailyCheckWorker(ctx: Context, p: WorkerParameters) : CoroutineWorker(ctx, p) {
    override suspend fun doWork(): Result {
        val app = applicationContext; val sp = app.getSharedPreferences("smarc_prefs", Context.MODE_PRIVATE)
        if (!sp.getBoolean("push", false) || BizRegistry(app).all().isEmpty()) return Result.success()
        val id = BizRegistry(app).active()
        val db = try { Dbs.open(app, id) } catch (e: Exception) { return Result.retry() }
        try {
            val dao = db.dao(); Cur.apply(BizStore.load(sp, id))
            val cfg = AlertCfg(sp.getInt("soon", 7), sp.getInt("lead", 1), sp.getInt("limit", 90))
            val n = Alerts.build(dao.dumpLedger(), dao.dumpCustomers().filter { it.status != "ARCHIVED" }, dao.dumpFollow(), System.currentTimeMillis(), Cur.zone, cfg, sp.getLong("last_backup", 0L)).count { it.urgent }
            if (n > 0) Notify.post(app, n)
        } finally { db.close() }
        return Result.success()
    }
}
