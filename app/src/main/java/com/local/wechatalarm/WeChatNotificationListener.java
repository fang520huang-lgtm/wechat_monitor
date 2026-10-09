package com.local.wechatalarm;

import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Log;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class WeChatNotificationListener extends NotificationListenerService {
    private static final String TAG = "WeChatAlarmListener";
    private static final int MAX_SEEN = 300;
    private static final long REBIND_DELAY_MS = 700L;
    private static final long REBIND_COOLDOWN_MS = 5_000L;
    private static final AtomicBoolean REBIND_PENDING = new AtomicBoolean(false);
    private static volatile boolean connected;
    private static volatile long lastRebindRequestElapsed;
    private static volatile WeChatNotificationListener activeInstance;

    private final Set<String> seen = new LinkedHashSet<>();
    private volatile boolean ready;
    private long connectedAtWallTime;

    static void requestReconnect(Context context) {
        if (!AppPrefs.isMonitoringEnabled(context)
                || !ListenerAccess.isGranted(context)) {
            return;
        }
        long now = SystemClock.elapsedRealtime();
        if (REBIND_PENDING.get()
                || now - lastRebindRequestElapsed < REBIND_COOLDOWN_MS) {
            return;
        }
        lastRebindRequestElapsed = now;
        REBIND_PENDING.set(true);

        ComponentName component = new ComponentName(
                context.getApplicationContext(), WeChatNotificationListener.class);
        Log.w(TAG, "Forcing notification listener unbind/rebind cycle.");
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                requestUnbind(component);
            } else if (activeInstance != null) {
                activeInstance.requestUnbind();
            }
        } catch (RuntimeException exc) {
            Log.w(TAG, "Unable to request listener unbind", exc);
        }
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            try {
                if (!AppPrefs.isMonitoringEnabled(context)) {
                    return;
                }
                requestRebind(component);
                Log.i(TAG, "Notification listener rebind requested.");
            } catch (RuntimeException exc) {
                Log.e(TAG, "Unable to request listener rebind", exc);
            } finally {
                REBIND_PENDING.set(false);
            }
        }, REBIND_DELAY_MS);
    }

    static void pause(Context context) {
        REBIND_PENDING.set(false);
        connected = false;
        WeChatNotificationListener instance = activeInstance;
        if (instance == null) {
            return;
        }
        instance.ready = false;
        try {
            instance.requestUnbind();
            Log.i(TAG, "Notification listener paused by user.");
        } catch (RuntimeException exc) {
            Log.w(TAG, "Unable to pause notification listener", exc);
        }
    }

    static void resume(Context context) {
        if (!AppPrefs.isMonitoringEnabled(context)
                || !ListenerAccess.isGranted(context)
                || connected) {
            return;
        }
        ComponentName component = new ComponentName(
                context.getApplicationContext(), WeChatNotificationListener.class);
        try {
            requestRebind(component);
            Log.i(TAG, "Notification listener resume requested.");
        } catch (RuntimeException exc) {
            Log.w(TAG, "Unable to resume notification listener", exc);
        }
    }

    static boolean isConnected() {
        return connected;
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        activeInstance = this;
        if (!AppPrefs.isMonitoringEnabled(this)) {
            ready = false;
            connected = false;
            ListenerMonitorService.stopRunning(this);
            pause(this);
            return;
        }
        connected = true;
        REBIND_PENDING.set(false);
        ListenerMonitorService.ensureRunning(this);
        MonitorNotification.removeLegacy(this);
        MonitorNotification.refresh(this);
        connectedAtWallTime = System.currentTimeMillis();
        synchronized (seen) {
            seen.clear();
            try {
                StatusBarNotification[] active = getActiveNotifications();
                if (active != null) {
                    for (StatusBarNotification sbn : active) {
                        if (MatchRule.AppSource.fromPackageName(
                                sbn.getPackageName()) != null) {
                            seen.add(fingerprint(sbn));
                        }
                    }
                }
            } catch (RuntimeException exc) {
                Log.w(TAG, "Unable to seed active notifications during reconnect", exc);
            }
        }
        ready = true;
        Log.i(TAG, "Listener connected; active supported notifications seeded.");
    }

    @Override
    public void onListenerDisconnected() {
        ready = false;
        connected = false;
        super.onListenerDisconnected();
        if (AppPrefs.isMonitoringEnabled(this)) {
            Log.w(TAG, "Listener disconnected; forcing a clean reconnect.");
            requestReconnect(this);
        } else {
            Log.i(TAG, "Listener disconnected while monitoring is paused.");
        }
    }

    @Override
    public void onDestroy() {
        ready = false;
        connected = false;
        if (activeInstance == this) {
            activeInstance = null;
        }
        super.onDestroy();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Log.i(TAG, "onNotificationPosted package="
                + (sbn == null ? "null" : sbn.getPackageName())
                + ", ready=" + ready);
        if (!AppPrefs.isMonitoringEnabled(this)) {
            return;
        }
        if (sbn != null && ListenerMonitorService.handleProbe(this, sbn)) {
            return;
        }
        if (!ready || sbn == null) {
            return;
        }
        MatchRule.AppSource source =
                MatchRule.AppSource.fromPackageName(sbn.getPackageName());
        if (source == null) {
            return;
        }

        Notification notification = sbn.getNotification();
        if (notification == null || notification.extras == null) {
            return;
        }

        String title = extractTitle(notification).trim();
        String text = extractText(notification).trim();
        Log.i(TAG, "Parsed " + source.displayName + " notification title=" + title
                + ", text=" + text
                + ", ticker=" + charSequenceToString(notification.tickerText));
        if (title.isEmpty() && text.isEmpty()) {
            Log.w(TAG, source.displayName + " notification has no usable title or text.");
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

        List<MatchRule> rules = AppPrefs.getRules(this);
        MatchRule matchedRule = null;
        for (MatchRule rule : rules) {
            if (rule.matches(source, title, text)) {
                matchedRule = rule;
                break;
            }
        }
        if (matchedRule == null) {
            Log.d(TAG, "Ignored " + source.displayName
                    + " notification; no rule matched.");
            return;
        }

        Log.i(TAG, "Matched rule: " + matchedRule.summary()
                + ", user=" + sbn.getUser());
        AlarmController.start(this,
                source.displayName + "消息：" + (title.isEmpty() ? "新消息" : title),
                TextUtils.isEmpty(text) ? "收到一条新消息" : text);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        if (sbn != null && MatchRule.AppSource.fromPackageName(
                sbn.getPackageName()) != null) {
            Log.i(TAG, "Supported notification removed: " + sbn.getKey());
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
