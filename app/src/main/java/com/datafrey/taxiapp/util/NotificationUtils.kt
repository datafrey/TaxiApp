package com.datafrey.taxiapp.util

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import com.datafrey.taxiapp.ApplicationConstants
import com.datafrey.taxiapp.R
import com.datafrey.taxiapp.activities.DriverMapsActivity

fun NotificationManager.sendCustomerAppearedNotification(applicationContext: Context) {
    val contentIntent = Intent(applicationContext, DriverMapsActivity::class.java)
    val contentPendingIntent = PendingIntent.getActivity(
        applicationContext,
        ApplicationConstants.CUSTOMER_APPEARED_NOTIFICATION_ID,
        contentIntent,
        PendingIntent.FLAG_UPDATE_CURRENT
    )

    val builder = NotificationCompat.Builder(
        applicationContext,
        applicationContext.getString(R.string.driver_notification_channel_id)
    )
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle(applicationContext.getString(R.string.customer_appeared_notification_title))
        .setContentText(applicationContext.getString(R.string.customer_appeared_notification_text))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
        .setContentIntent(contentPendingIntent)
        .setAutoCancel(true)

    notify(ApplicationConstants.CUSTOMER_APPEARED_NOTIFICATION_ID, builder.build())
}

fun NotificationManager.cancelNotifications() {
    cancelAll()
}