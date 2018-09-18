package com.devforfun.app.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;

import com.devforfun.app.R;
import com.devforfun.app.NotificationsActivity;
import com.devforfun.app.R;
import com.devforfun.app.check.FillChecklistActivity;
import com.devforfun.app.cooling.FillCoolingTemperatureItemsActivity;
import com.devforfun.app.delivery.FillDeliveryTemperatureActivity;
import com.devforfun.app.diary.DiaryDetailsActivity;
import com.devforfun.app.fridge.FillFridgeTemperatureActivity;
import com.devforfun.app.reheating.FillReheatingTemperatureItemsActivity;
import com.devforfun.app.stock.FillStockListActivity;
import com.devforfun.app.wastage.WastageActivity;

import java.util.Calendar;

public class PushBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        NotificationManager notificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);

        Notification notification = null;
        Intent targetIntent = new Intent(context, NotificationsActivity.class);

        String notificationMessage = "";
        String notificationTitle = "";
        int notificationType = intent.getIntExtra("notificationType", 0);

        if(notificationType != 0) {
            switch (notificationType) {
            }

            targetIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            targetIntent.putExtra("pushMessage", true);

            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, targetIntent, 0);
            if (android.os.Build.VERSION.SDK_INT < 26) {
                notification = new Notification.Builder(context)
                        .setVibrate(new long[]{500})
                        .setLights(255, 500, 500)
                        .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                        .setContentText(notificationMessage)
                        .setContentTitle(notificationTitle)
                        .setContentIntent(pendingIntent)
                        .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher_round))
                        .setSmallIcon(R.drawable.ic_notification_icon)
                        .build();

                notification.flags |= Notification.FLAG_AUTO_CANCEL;
                //adding LED lights to notification
                notification.defaults |= Notification.DEFAULT_LIGHTS;
            } else {
                notification = new Notification.Builder(context, NotificationChannel.DEFAULT_CHANNEL_ID)
                        .setContentText(notificationMessage)
                        .setContentTitle(intent.getStringExtra("listName"))
                        .setContentIntent(pendingIntent)
                        .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher_round))
                        .setSmallIcon(R.drawable.ic_notification_icon)
                        .build();
            }

//            Set current millisecond as notification ID to show all notifications received
            notificationManager.notify((int) Calendar.getInstance().getTimeInMillis(), notification);
        }
    }
}
