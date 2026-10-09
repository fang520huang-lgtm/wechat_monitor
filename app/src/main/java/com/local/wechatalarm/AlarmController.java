package com.local.wechatalarm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.Log;

final class AlarmController {
    private static final String CHANNEL_ID = "wechat_alarm_playback";
    private static final int NOTIFICATION_ID = 7001;
    private static final String TAG = "WeChatAlarmPlayback";

    private static Context appContext;
    private static MediaPlayer player;
    private static Vibrator vibrator;
    private static PowerManager.WakeLock wakeLock;
    private static AudioManager audioManager;

    private AlarmController() {}

    static synchronized void start(Context context, String title, String text) {
        appContext = context.getApplicationContext();
        ensureChannel(appContext);
        stopPlaybackOnly();
        postAlarmNotification(appContext, title, text);

        if (AppPrefs.isSoundEnabled(appContext)) {
            audioManager = appContext.getSystemService(AudioManager.class);
            if (audioManager != null) {
                int focusResult = audioManager.requestAudioFocus(
                        null, AudioManager.STREAM_ALARM,
                        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
                Log.i(TAG, "Audio focus result=" + focusResult);
            }
            playRingtone(appContext);
        } else {
            Log.i(TAG, "Sound is disabled in app settings.");
        }
        acquireWakeLock(appContext);
        if (AppPrefs.isVibrationEnabled(appContext)) {
            startVibration(appContext);
        } else {
            Log.i(TAG, "Vibration is disabled in app settings.");
        }

    }

    static synchronized void stop(Context context) {
        if (appContext == null) {
            appContext = context.getApplicationContext();
        }
        stopInternal();
    }

    static void ensureChannel(Context context) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "消息通知闹钟",
                NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("微信、企业微信或 QQ 通知符合规则时显示的持续提醒");
        channel.setSound(null, null);
        channel.enableVibration(false);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        manager.createNotificationChannel(channel);
    }

    private static void postAlarmNotification(Context context, String title, String text) {
        Intent openIntent = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openPendingIntent = PendingIntent.getActivity(
                context, 1, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent stopIntent = new Intent(context, StopAlarmReceiver.class)
                .setAction(StopAlarmReceiver.ACTION_STOP);
        PendingIntent stopPendingIntent = PendingIntent.getBroadcast(
                context, 2, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        boolean soundEnabled = AppPrefs.isSoundEnabled(context);
        boolean vibrationEnabled = AppPrefs.isVibrationEnabled(context);
        String runningDescription;
        if (soundEnabled && vibrationEnabled) {
            runningDescription = "铃声和震动将一直持续";
        } else if (soundEnabled) {
            runningDescription = "铃声将一直持续（震动已关闭）";
        } else {
            runningDescription = "震动将一直持续（铃声已关闭）";
        }
        Notification notification = new Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_bell_large)
                .setContentTitle(title + "（点此停止）")
                .setContentText(text + "｜点击通知即可停止")
                .setStyle(new Notification.BigTextStyle().bigText(
                        text + "\n\n" + runningDescription
                                + "，点击本通知或下方按钮即可停止。"))
                .setCategory(Notification.CATEGORY_ALARM)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setAutoCancel(false)
                .setOnlyAlertOnce(true)
                .setContentIntent(stopPendingIntent)
                .addAction(new Notification.Action.Builder(
                        R.drawable.ic_notification_bell_large, "停止闹钟", stopPendingIntent).build())
                .addAction(new Notification.Action.Builder(
                        R.drawable.ic_notification_bell_large, "打开应用", openPendingIntent).build())
                .build();
        NotificationIconCompat.applyCardIcon(context, notification);

        context.getSystemService(NotificationManager.class)
                .notify(NOTIFICATION_ID, notification);
    }

    private static void playRingtone(Context context) {
        Uri selectedUri = AppPrefs.getRingtoneUri(context);
        Uri bundledUri = Uri.parse("android.resource://" + context.getPackageName()
                + "/" + R.raw.alarm_tune);

        try {
            startPlayer(context, selectedUri == null ? bundledUri : selectedUri);
            Log.i(TAG, "Alarm ringtone started; selected="
                    + (selectedUri != null) + ", isPlaying=" + player.isPlaying());
        } catch (Exception exc) {
            Log.e(TAG, "Unable to play selected alarm ringtone; trying bundled fallback", exc);
            releasePlayer();
            if (selectedUri != null) {
                try {
                    startPlayer(context, bundledUri);
                    Log.i(TAG, "Bundled fallback ringtone started; isPlaying=" + player.isPlaying());
                } catch (Exception fallbackExc) {
                    Log.e(TAG, "Unable to play bundled fallback ringtone", fallbackExc);
                    releasePlayer();
                }
            }
        }
    }

    private static void startPlayer(Context context, Uri uri) throws Exception {
        player = new MediaPlayer();
        player.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build());
        player.setDataSource(context, uri);
        player.setLooping(true);
        player.prepare();
        preferBuiltInSpeaker(context, player);
        player.start();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            MediaPlayer startedPlayer = player;
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (player != startedPlayer) {
                    return;
                }
                AudioDeviceInfo routedDevice = startedPlayer.getRoutedDevice();
                Log.i(TAG, "Alarm routed device="
                        + (routedDevice == null ? "unknown"
                        : routedDevice.getProductName() + "/type="
                        + routedDevice.getType()));
            }, 300L);
        }
    }

    private static void preferBuiltInSpeaker(Context context, MediaPlayer mediaPlayer) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            Log.w(TAG, "Per-player speaker routing requires Android 9 or newer.");
            return;
        }
        AudioManager manager = context.getSystemService(AudioManager.class);
        if (manager == null) {
            return;
        }

        AudioDeviceInfo standardSpeaker = null;
        AudioDeviceInfo safeSpeaker = null;
        for (AudioDeviceInfo device : manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) {
            if (device.getType() == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER) {
                standardSpeaker = device;
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                    && device.getType() == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER_SAFE) {
                safeSpeaker = device;
            }
        }

        AudioDeviceInfo preferred = safeSpeaker != null ? safeSpeaker : standardSpeaker;
        if (preferred == null) {
            Log.w(TAG, "No built-in speaker output was reported by the system.");
            return;
        }
        boolean accepted = mediaPlayer.setPreferredDevice(preferred);
        Log.i(TAG, "Built-in speaker preference accepted=" + accepted
                + ", type=" + preferred.getType());
    }

    private static void startVibration(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vibratorManager = context.getSystemService(VibratorManager.class);
            vibrator = vibratorManager == null ? null : vibratorManager.getDefaultVibrator();
        } else {
            vibrator = context.getSystemService(Vibrator.class);
        }

        if (vibrator != null && vibrator.hasVibrator()) {
            long[] pattern = {0, 700, 350, 700, 900};
            AudioAttributes alarmAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0), alarmAttributes);
            Log.i(TAG, "Repeating vibration started with USAGE_ALARM.");
        }
    }

    private static void acquireWakeLock(Context context) {
        PowerManager powerManager = context.getSystemService(PowerManager.class);
        wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK, "WeChatAlarm:Playback");
        wakeLock.acquire();
    }

    private static synchronized void stopInternal() {
        stopPlaybackOnly();
        if (appContext != null) {
            appContext.getSystemService(NotificationManager.class).cancel(NOTIFICATION_ID);
        }
    }

    private static void stopPlaybackOnly() {
        releasePlayer();
        if (vibrator != null) {
            vibrator.cancel();
            vibrator = null;
        }
        if (audioManager != null) {
            audioManager.abandonAudioFocus(null);
            audioManager = null;
        }
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        wakeLock = null;
    }

    private static void releasePlayer() {
        if (player != null) {
            try {
                player.stop();
            } catch (Exception ignored) {
            }
            player.release();
            player = null;
        }
    }
}
