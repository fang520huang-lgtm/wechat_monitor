package com.local.wechatalarm;

import android.app.Notification;
import android.content.Context;
import android.graphics.drawable.Icon;

final class NotificationIconCompat {
    private static final String MIUI_APP_ICON_KEY = "miui.appIcon";

    private NotificationIconCompat() {}

    static void applyCardIcon(Context context, Notification notification) {
        notification.extras.putParcelable(
                MIUI_APP_ICON_KEY,
                Icon.createWithResource(context, R.drawable.ic_notification_card_bell_large));
    }
}
