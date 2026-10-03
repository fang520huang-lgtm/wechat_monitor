package com.local.wechatalarm;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.service.notification.StatusBarNotification;
import android.util.Log;

public final class ListenerMonitorService extends Service {
    private static final String TAG = "WeChatAlarmMonitor";
    private static final long INITIAL_CHECK_DELAY_MS = 3_000L;
    private static final long CHECK_INTERVAL_MS = 60_000L;
    private static final long PROBE_TIMEOUT_MS = 2_500L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable healthCheck = this::runHealthCheck;
    private Runnable probeResultCheck;

    static void ensureRunning(Context context) {
        Context appContext = context.getApplicationContext();
        if (!AppPrefs.isMonitoringEnabled(appContext)) {
            stopRunning(appContext);
            return;
        }
        Intent intent = new Intent(appContext, ListenerMonitorService.class);
        try {
            appContext.startForegroundService(intent);
        } catch (RuntimeException exc) {
            Log.e(TAG, "Unable to start listener monitor service", exc);
        }
    }

    static void stopRunning(Context context) {
        Context appContext = context.getApplicationContext();
        appContext.stopService(new Intent(appContext, ListenerMonitorService.class));
        MonitorNotification.cancel(appContext);
    }

    static boolean handleProbe(Context context, StatusBarNotification sbn) {
        if (!context.getPackageName().equals(sbn.getPackageName())
                || sbn.getId() != MonitorNotification.NOTIFICATION_ID) {
            return false;
        }
        if (sbn.getNotification() == null || sbn.getNotification().extras == null) {
            return false;
        }
        long token = sbn.getNotification().extras.getLong(
                MonitorNotification.EXTRA_PROBE_TOKEN, 0L);
        if (token == 0L) {
            return false;
        }
        AppPrefs.acknowledgeListenerProbe(context, token);
        Log.i(TAG, "Listener probe acknowledged.");
        return true;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        MonitorNotification.removeLegacy(this);
        if (!AppPrefs.isMonitoringEnabled(this)) {
            MonitorNotification.cancel(this);
            stopSelf();
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                    MonitorNotification.NOTIFICATION_ID,
                    MonitorNotification.build(this),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(
                    MonitorNotification.NOTIFICATION_ID,
                    MonitorNotification.build(this));
        }
        scheduleNextCheck(INITIAL_CHECK_DELAY_MS);
        Log.i(TAG, "Listener monitor service started.");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!AppPrefs.isMonitoringEnabled(this)) {
            stopForeground(STOP_FOREGROUND_REMOVE);
            MonitorNotification.cancel(this);
            stopSelf();
            return START_NOT_STICKY;
        }
        scheduleNextCheck(INITIAL_CHECK_DELAY_MS);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        stopForeground(STOP_FOREGROUND_REMOVE);
        MonitorNotification.cancel(this);
        super.onDestroy();
        Log.i(TAG, "Listener monitor service stopped.");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void runHealthCheck() {
        if (!AppPrefs.isMonitoringEnabled(this)) {
            stopSelf();
            return;
        }
        if (!ListenerAccess.isGranted(this)) {
            Log.w(TAG, "Health check skipped because notification access is not granted.");
            scheduleNextCheck(CHECK_INTERVAL_MS);
            return;
        }

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager == null || !manager.areNotificationsEnabled()) {
            Log.w(TAG, "Health check skipped because app notifications are disabled.");
            scheduleNextCheck(CHECK_INTERVAL_MS);
            return;
        }

        long token = SystemClock.elapsedRealtimeNanos();
        MonitorNotification.refreshWithProbe(this, token);
        probeResultCheck = () -> {
            boolean acknowledged = AppPrefs.getListenerProbeAck(this) == token;
            if (acknowledged) {
                Log.i(TAG, "Listener health check passed.");
            } else {
                Log.w(TAG, "Listener probe timed out; forcing a clean reconnect.");
                WeChatNotificationListener.requestReconnect(this);
            }
            probeResultCheck = null;
            scheduleNextCheck(CHECK_INTERVAL_MS);
        };
        handler.postDelayed(probeResultCheck, PROBE_TIMEOUT_MS);
    }

    private void scheduleNextCheck(long delayMs) {
        handler.removeCallbacks(healthCheck);
        handler.postDelayed(healthCheck, delayMs);
    }
}
