package com.local.wechatalarm;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

final class MatchRule {
    enum AppSource {
        WECHAT("微信", "com.tencent.mm"),
        WECOM("企业微信", "com.tencent.wework"),
        QQ("QQ", "com.tencent.mobileqq");

        final String displayName;
        final String packageName;

        AppSource(String displayName, String packageName) {
            this.displayName = displayName;
            this.packageName = packageName;
        }

        static AppSource fromPackageName(String packageName) {
            for (AppSource source : values()) {
                if (source.packageName.equals(packageName)) {
                    return source;
                }
            }
            return null;
        }
    }

    enum Mode {
        EXACT,
        CONTAINS
    }

    final AppSource source;
    final boolean titleEnabled;
    final Mode titleMode;
    final String titlePattern;
    final boolean textEnabled;
    final Mode textMode;
    final String textPattern;

    MatchRule(AppSource source,
              boolean titleEnabled, Mode titleMode, String titlePattern,
              boolean textEnabled, Mode textMode, String textPattern) {
        this.source = source == null ? AppSource.WECHAT : source;
        this.titleEnabled = titleEnabled;
        this.titleMode = titleMode == null ? Mode.EXACT : titleMode;
        this.titlePattern = normalize(titlePattern);
        this.textEnabled = textEnabled;
        this.textMode = textMode == null ? Mode.EXACT : textMode;
        this.textPattern = normalize(textPattern);
    }

    boolean isValid() {
        return (titleEnabled && !titlePattern.isEmpty())
                || (textEnabled && !textPattern.isEmpty());
    }

    boolean matches(AppSource notificationSource, String title, String text) {
        if (!isValid() || source != notificationSource) {
            return false;
        }
        return (!titleEnabled || matchesValue(normalize(title), titlePattern, titleMode))
                && (!textEnabled || matchesValue(normalize(text), textPattern, textMode));
    }

    String summary() {
        StringBuilder result = new StringBuilder(source.displayName);
        if (titleEnabled) {
            result.append('\n');
            appendCondition(result, "标题", titleMode, titlePattern);
        }
        if (textEnabled) {
            result.append('\n');
            appendCondition(result, "正文", textMode, textPattern);
        }
        return result.toString();
    }

    String serialize() {
        return source.name() + "|" + (titleEnabled ? "1" : "0") + "|"
                + titleMode.name() + "|" + encode(titlePattern) + "|"
                + (textEnabled ? "1" : "0") + "|" + textMode.name() + "|"
                + encode(textPattern);
    }

    static MatchRule deserialize(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        String[] fields = value.split("\\|", -1);
        if (fields.length != 7) {
            return null;
        }
        try {
            MatchRule rule = new MatchRule(
                    AppSource.valueOf(fields[0]),
                    "1".equals(fields[1]), Mode.valueOf(fields[2]), decode(fields[3]),
                    "1".equals(fields[4]), Mode.valueOf(fields[5]), decode(fields[6]));
            return rule.isValid() ? rule : null;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static boolean matchesValue(String actual, String expected, Mode mode) {
        return mode == Mode.CONTAINS ? actual.contains(expected) : actual.equals(expected);
    }

    private static void appendCondition(StringBuilder result, String field,
                                        Mode mode, String pattern) {
        result.append(field).append("：")
                .append(mode == Mode.CONTAINS ? "部分匹配“" : "完全匹配“")
                .append(pattern)
                .append('”');
    }

    private static String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String value) {
        if (value.isEmpty()) {
            return "";
        }
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
