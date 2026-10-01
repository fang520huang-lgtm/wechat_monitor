package com.local.wechatalarm;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

final class AppPrefs {
    static final String PREFS = "wechat_alarm_prefs";
    static final String KEY_TARGETS = "target_names";
    static final String DEFAULT_TARGET = "导师";
    static final String DEFAULT_RINGTONE_NAME = "微信消息闹钟默认铃声";

    private static final String KEY_RINGTONE_URI = "ringtone_uri";
    private static final String KEY_RINGTONE_NAME = "ringtone_name";
    private static final String KEY_VIBRATION_ENABLED = "vibration_enabled";

    private AppPrefs() {}

    static String getRawTargets(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_TARGETS, DEFAULT_TARGET);
    }

    static void saveRawTargets(Context context, String value) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_TARGETS, value.trim())
                .apply();
    }

    static void saveTargets(Context context, Collection<String> values) {
        StringBuilder raw = new StringBuilder();
        for (String value : values) {
            String normalized = value == null ? "" : value.trim();
            if (normalized.isEmpty()) {
                continue;
            }
            if (raw.length() > 0) {
                raw.append('\n');
            }
            raw.append(normalized);
        }
        saveRawTargets(context, raw.toString());
    }

    static Set<String> getTargets(Context context) {
        String raw = getRawTargets(context);
        Set<String> targets = new LinkedHashSet<>();
        for (String item : raw.split("[,，;；\\n\\r]+")) {
            String normalized = item.trim();
            if (!normalized.isEmpty()) {
                targets.add(normalized);
            }
        }
        return targets;
    }

    static Uri getRingtoneUri(Context context) {
        String value = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_RINGTONE_URI, "");
        return value == null || value.isEmpty() ? null : Uri.parse(value);
    }

    static String getRingtoneName(Context context) {
        String name = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_RINGTONE_NAME, DEFAULT_RINGTONE_NAME);
        if (name == null || name.trim().isEmpty()) {
            return DEFAULT_RINGTONE_NAME;
        }
        String normalized = name.trim();
        for (String prefix : new String[]{"内置铃声：", "系统铃声：", "音频文件："}) {
            if (normalized.startsWith(prefix)) {
                return normalized.substring(prefix.length()).trim();
            }
        }
        return normalized;
    }

    static void saveRingtone(Context context, Uri uri, String displayName) {
        SharedPreferences.Editor editor = context
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit();
        if (uri == null) {
            editor.remove(KEY_RINGTONE_URI)
                    .putString(KEY_RINGTONE_NAME, DEFAULT_RINGTONE_NAME);
        } else {
            editor.putString(KEY_RINGTONE_URI, uri.toString())
                    .putString(KEY_RINGTONE_NAME, displayName);
        }
        editor.apply();
    }

    static boolean isVibrationEnabled(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_VIBRATION_ENABLED, true);
    }

    static void setVibrationEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_VIBRATION_ENABLED, enabled)
                .apply();
    }
}
