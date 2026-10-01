package com.local.wechatalarm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.util.Set;

final class MonitorNotification {
    static final String CHANNEL_ID = "wechat_monitor_service";
    static final int NOTIFICATION_ID = 7040;
    private static final int[] LEGACY_NOTIFICATION_IDS = {7000, 7010, 7020, 7030};
    private static final String OLD_REMINDER_CHANNEL_ID = "wechat_monitor_reminder";

    private MonitorNotification() {}

    static void ensureChannel(Context context) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "微信监听运行状态",
                NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("保持微信消息闹钟自动监听");
        channel.setSound(null, null);
        channel.enableVibration(false);
        manager.createNotificationChannel(channel);
        manager.deleteNotificationChannel(OLD_REMINDER_CHANNEL_ID);
    }

    static Notification build(Context context) {
        ensureChannel(context);
        Intent openIntent = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openPendingIntent = PendingIntent.getActivity(
                context, 20, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Set<String> targets = AppPrefs.getTargets(context);
        String targetSummary;
        if (targets.isEmpty()) {
            targetSummary = "尚未添加监听对象";
        } else if (targets.size() == 1) {
            targetSummary = "监听对象：" + targets.iterator().next();
        } else {
            targetSummary = "监听多个对象";
        }

        Notification notification = new Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_bell_large)
                .setContentTitle("微信消息监听中")
                .setContentText(targetSummary)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(openPendingIntent)
                .build();
        NotificationIconCompat.applyCardIcon(context, notification);
        return notification;
    }

    static void removeLegacy(Context context) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        for (int notificationId : LEGACY_NOTIFICATION_IDS) {
            manager.cancel(notificationId);
        }
    }

    static void refresh(Context context) {
        context.getSystemService(NotificationManager.class)
                .notify(NOTIFICATION_ID, build(context));
    }
}
