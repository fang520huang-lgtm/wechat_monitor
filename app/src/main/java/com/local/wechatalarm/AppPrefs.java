package com.local.wechatalarm;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class AppPrefs {
    static final String PREFS = "wechat_alarm_prefs";
    static final String DEFAULT_RINGTONE_NAME = "消息通知闹钟默认铃声";

    private static final String KEY_RULES = "match_rules_v2";
    private static final String KEY_LEGACY_TARGETS = "target_names";
    private static final String DEFAULT_LEGACY_TARGET = "导师";
    private static final String KEY_RINGTONE_URI = "ringtone_uri";
    private static final String KEY_RINGTONE_NAME = "ringtone_name";
    private static final String KEY_SOUND_ENABLED = "sound_enabled";
    private static final String KEY_VIBRATION_ENABLED = "vibration_enabled";
    private static final String KEY_MONITORING_ENABLED = "monitoring_enabled";
    private static final String KEY_LISTENER_PROBE_ACK = "listener_probe_ack";

    private AppPrefs() {}

    static List<MatchRule> getRules(Context context) {
        SharedPreferences preferences =
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!preferences.contains(KEY_RULES)) {
            List<MatchRule> migrated = migrateLegacyTargets(preferences);
            saveRules(context, migrated);
            return migrated;
        }

        String raw = preferences.getString(KEY_RULES, "");
        List<MatchRule> rules = new ArrayList<>();
        if (raw == null || raw.trim().isEmpty()) {
            return rules;
        }
        for (String line : raw.split("\n")) {
            MatchRule rule = MatchRule.deserialize(line);
            if (rule != null) {
                rules.add(rule);
            }
        }
        return rules;
    }

    static void saveRules(Context context, Collection<MatchRule> rules) {
        StringBuilder raw = new StringBuilder();
        for (MatchRule rule : rules) {
            if (rule == null || !rule.isValid()) {
                continue;
            }
            if (raw.length() > 0) {
                raw.append('\n');
            }
            raw.append(rule.serialize());
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_RULES, raw.toString())
                .apply();
    }

    private static List<MatchRule> migrateLegacyTargets(SharedPreferences preferences) {
        String raw = preferences.getString(KEY_LEGACY_TARGETS, DEFAULT_LEGACY_TARGET);
        Set<String> names = new LinkedHashSet<>();
        if (raw != null) {
            for (String item : raw.split("[,，;；\\n\\r]+")) {
                String normalized = item.trim();
                if (!normalized.isEmpty()) {
                    names.add(normalized);
                }
            }
        }

        List<MatchRule> rules = new ArrayList<>();
        for (String name : names) {
            rules.add(new MatchRule(
                    MatchRule.AppSource.WECHAT,
                    true, MatchRule.Mode.EXACT, name,
                    false, MatchRule.Mode.EXACT, ""));
        }
        return rules;
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

    static boolean isSoundEnabled(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_SOUND_ENABLED, true);
    }

    static void setSoundEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_SOUND_ENABLED, enabled)
                .apply();
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

    static boolean isMonitoringEnabled(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_MONITORING_ENABLED, true);
    }

    static void setMonitoringEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_MONITORING_ENABLED, enabled)
                .apply();
    }

    static long getListenerProbeAck(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getLong(KEY_LISTENER_PROBE_ACK, 0L);
    }

    static void acknowledgeListenerProbe(Context context, long token) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putLong(KEY_LISTENER_PROBE_ACK, token)
                .apply();
    }
}
