package com.local.wechatalarm;

import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;

final class ListenerAccess {
    private ListenerAccess() {}

    static boolean isGranted(Context context) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        ComponentName component = new ComponentName(context, WeChatNotificationListener.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            return manager.isNotificationListenerAccessGranted(component);
        }
        String enabled = Settings.Secure.getString(
                context.getContentResolver(), "enabled_notification_listeners");
        return enabled != null && enabled.contains(context.getPackageName());
    }
}
