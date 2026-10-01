package com.local.wechatalarm;

import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Log;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

public class WeChatNotificationListener extends NotificationListenerService {
    private static final String TAG = "WeChatAlarmListener";
    private static final String WECHAT_PACKAGE = "com.tencent.mm";
    private static final int MAX_SEEN = 300;

    private final Set<String> seen = new LinkedHashSet<>();
    private volatile boolean ready;
    private long connectedAtWallTime;

    static void requestReconnect(Context context) {
        requestRebind(new ComponentName(context, WeChatNotificationListener.class));
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        try {
            MonitorNotification.removeLegacy(this);
            Notification monitorNotification = MonitorNotification.build(this);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                        MonitorNotification.NOTIFICATION_ID,
                        monitorNotification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
            } else {
                startForeground(MonitorNotification.NOTIFICATION_ID, monitorNotification);
            }
            Log.i(TAG, "Notification listener promoted to foreground automatically.");
        } catch (RuntimeException exc) {
            Log.e(TAG, "Unable to promote notification listener to foreground", exc);
        }
        connectedAtWallTime = System.currentTimeMillis();
        synchronized (seen) {
            seen.clear();
            StatusBarNotification[] active = getActiveNotifications();
            if (active != null) {
                for (StatusBarNotification sbn : active) {
                    if (WECHAT_PACKAGE.equals(sbn.getPackageName())) {
                        seen.add(fingerprint(sbn));
                    }
                }
            }
        }
        ready = true;
        Log.i(TAG, "Listener connected; active WeChat notifications seeded.");
    }

    @Override
    public void onListenerDisconnected() {
        ready = false;
        Log.w(TAG, "Listener disconnected; requesting rebind.");
        super.onListenerDisconnected();
        requestReconnect(this);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Log.i(TAG, "onNotificationPosted package="
                + (sbn == null ? "null" : sbn.getPackageName())
                + ", ready=" + ready);
        if (!ready || sbn == null || !WECHAT_PACKAGE.equals(sbn.getPackageName())) {
            return;
        }

        Notification notification = sbn.getNotification();
        if (notification == null || notification.extras == null) {
            return;
        }

        Bundle extras = notification.extras;
        String title = extractTitle(notification).trim();
        String text = extractText(notification).trim();
        Log.i(TAG, "Parsed WeChat notification title=" + title
                + ", text=" + text
                + ", ticker=" + charSequenceToString(notification.tickerText));
        if (title.isEmpty()) {
            Log.w(TAG, "WeChat notification has no usable sender title.");
            return;
        }

        String fingerprint = fingerprint(sbn);
        synchronized (seen) {
            if (seen.contains(fingerprint)) {
                return;
            }
            seen.add(fingerprint);
            trimSeen();
        }

        // Ignore stale notifications replayed immediately after the listener is enabled.
        if (sbn.getPostTime() < connectedAtWallTime - 2000L) {
            return;
        }

        Set<String> targets = AppPrefs.getTargets(this);
        if (!targets.contains(title)) {
            Log.d(TAG, "Ignored WeChat sender: " + title);
            return;
        }

        Log.i(TAG, "Matched sender: " + title + ", user=" + sbn.getUser());
        AlarmController.start(this, "微信消息：" + title,
                TextUtils.isEmpty(text) ? "收到一条新消息" : text);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        if (sbn != null && WECHAT_PACKAGE.equals(sbn.getPackageName())) {
            Log.i(TAG, "WeChat notification removed: " + sbn.getKey());
        }
    }

    private String fingerprint(StatusBarNotification sbn) {
        Notification notification = sbn.getNotification();
        String title = notification == null ? "" : extractTitle(notification);
        String text = notification == null ? "" : extractText(notification);
        return sbn.getKey() + "|" + sbn.getPostTime() + "|" + title + "|" + text;
    }

    private String extractTitle(Notification notification) {
        Bundle extras = notification.extras;
        CharSequence title = extras.getCharSequence(Notification.EXTRA_TITLE);
        if (TextUtils.isEmpty(title)) {
            title = extras.getCharSequence(Notification.EXTRA_TITLE_BIG);
        }
        if (TextUtils.isEmpty(title)) {
            title = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE);
        }
        String result = charSequenceToString(title).trim();
        if (!result.isEmpty()) {
            return result;
        }

        String ticker = charSequenceToString(notification.tickerText).trim();
        int separator = firstSeparator(ticker);
        return separator > 0 ? ticker.substring(0, separator).trim() : "";
    }

    private String extractText(Notification notification) {
        Bundle extras = notification.extras;
        CharSequence text = extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
        if (TextUtils.isEmpty(text)) {
            text = extras.getCharSequence(Notification.EXTRA_TEXT);
        }
        if (TextUtils.isEmpty(text)) {
            CharSequence[] lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
            if (lines != null && lines.length > 0) {
                text = lines[lines.length - 1];
            }
        }
        String result = charSequenceToString(text).trim();
        if (!result.isEmpty()) {
            return result;
        }
        String ticker = charSequenceToString(notification.tickerText).trim();
        int separator = firstSeparator(ticker);
        return separator >= 0 && separator + 1 < ticker.length()
                ? ticker.substring(separator + 1).trim()
                : ticker;
    }

    private int firstSeparator(String value) {
        int ascii = value.indexOf(':');
        int chinese = value.indexOf('：');
        if (ascii < 0) {
            return chinese;
        }
        if (chinese < 0) {
            return ascii;
        }
        return Math.min(ascii, chinese);
    }

    private String charSequenceToString(CharSequence value) {
        return value == null ? "" : value.toString();
    }

    private void trimSeen() {
        while (seen.size() > MAX_SEEN) {
            Iterator<String> iterator = seen.iterator();
            if (iterator.hasNext()) {
                iterator.next();
                iterator.remove();
            } else {
                break;
            }
        }
    }
}
