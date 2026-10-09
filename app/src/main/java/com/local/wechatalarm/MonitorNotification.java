package com.local.wechatalarm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import java.util.List;

final class MonitorNotification {
    static final String CHANNEL_ID = "wechat_monitor_service";
    static final int NOTIFICATION_ID = 7040;
    static final String EXTRA_PROBE_TOKEN = "listener_probe_token";
    private static final int[] LEGACY_NOTIFICATION_IDS = {7000, 7010, 7020, 7030};
    private static final String OLD_REMINDER_CHANNEL_ID = "wechat_monitor_reminder";

    private MonitorNotification() {}

    static void ensureChannel(Context context) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "消息通知监听运行状态",
                NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("保持消息通知闹钟自动监听微信、企业微信与 QQ");
        channel.setSound(null, null);
        channel.enableVibration(false);
        manager.createNotificationChannel(channel);
        manager.deleteNotificationChannel(OLD_REMINDER_CHANNEL_ID);
    }

    static Notification build(Context context) {
        return build(context, 0L);
    }

    static Notification build(Context context, long probeToken) {
        ensureChannel(context);
        Intent openIntent = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openPendingIntent = PendingIntent.getActivity(
                context, 20, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        List<MatchRule> rules = AppPrefs.getRules(context);
        String ruleSummary;
        if (rules.isEmpty()) {
            ruleSummary = "尚未添加匹配规则";
        } else if (rules.size() == 1) {
            ruleSummary = rules.get(0).summary();
        } else {
            ruleSummary = "已设置 " + rules.size() + " 条匹配规则";
        }

        Bundle extras = new Bundle();
        if (probeToken != 0L) {
            extras.putLong(EXTRA_PROBE_TOKEN, probeToken);
        }

        boolean granted = ListenerAccess.isGranted(context);
        boolean connected = WeChatNotificationListener.isConnected();
        String statusTitle;
        if (!granted) {
            statusTitle = "消息通知监听未授权";
        } else if (connected) {
            statusTitle = "消息通知监听中";
        } else {
            statusTitle = "正在恢复消息监听";
        }

        Notification notification = new Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_bell_large)
                .setContentTitle(statusTitle)
                .setContentText(ruleSummary)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(openPendingIntent)
                .addExtras(extras)
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

    static void cancel(Context context) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        manager.cancel(NOTIFICATION_ID);
        removeLegacy(context);
    }

    static void refresh(Context context) {
        if (!AppPrefs.isMonitoringEnabled(context)) {
            cancel(context);
            return;
        }
        context.getSystemService(NotificationManager.class)
                .notify(NOTIFICATION_ID, build(context));
    }

    static void refreshWithProbe(Context context, long probeToken) {
        if (!AppPrefs.isMonitoringEnabled(context)) {
            cancel(context);
            return;
        }
        context.getSystemService(NotificationManager.class)
                .notify(NOTIFICATION_ID, build(context, probeToken));
    }
}
