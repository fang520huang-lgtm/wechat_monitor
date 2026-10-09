package com.local.wechatalarm;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public final class SmokeInstrumentation extends Instrumentation {
    @Override
    public void onCreate(Bundle arguments) {
        super.onCreate(arguments);
        start();
    }

    @Override
    public void onStart() {
        Bundle result = new Bundle();
        try {
            verifyRuleSettingsSupportsAllSourcesAndLineBreaks();
            verifySoundSettingsOffersThreeAlertModes();
            verifyAlarmPrefersBuiltInSpeaker();
            result.putString("stream", "Smoke instrumentation passed\n");
            finish(Activity.RESULT_OK, result);
        } catch (Throwable throwable) {
            result.putString("stream", Log.getStackTraceString(throwable));
            finish(Activity.RESULT_CANCELED, result);
        }
    }

    private void verifyRuleSettingsSupportsAllSourcesAndLineBreaks() {
        Context context = getTargetContext();
        List<MatchRule> oldRules = AppPrefs.getRules(context);
        List<MatchRule> testRules = new ArrayList<>(oldRules);
        testRules.add(new MatchRule(
                MatchRule.AppSource.QQ,
                true, MatchRule.Mode.EXACT, "测试联系人",
                false, MatchRule.Mode.EXACT, ""));
        AppPrefs.saveRules(context, testRules);
        Intent intent = new Intent(context, TargetSettingsActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Activity activity = startActivitySync(intent);
        try {
            View root = activity.getWindow().getDecorView();
            require(containsText(root, "微信"), "Missing WeChat source");
            require(containsText(root, "企业微信"), "Missing WeCom source");
            require(containsText(root, "QQ"), "Missing QQ source");
            require(containsTextFragment(root, "\n标题："),
                    "Existing rule conditions are not displayed on separate lines");
        } finally {
            runOnMainSync(activity::finish);
            AppPrefs.saveRules(context, oldRules);
        }
    }

    private void verifySoundSettingsOffersThreeAlertModes() {
        Context context = getTargetContext();
        boolean oldSoundEnabled = AppPrefs.isSoundEnabled(context);
        boolean oldVibrationEnabled = AppPrefs.isVibrationEnabled(context);
        AppPrefs.setSoundEnabled(context, true);
        AppPrefs.setVibrationEnabled(context, true);
        Intent intent = new Intent(context, SoundSettingsActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Activity activity = startActivitySync(intent);
        try {
            View root = activity.getWindow().getDecorView();
            require(containsText(root, "收到消息时持续播放铃声"),
                    "Missing sound toggle");
            require(containsText(root, "收到消息时持续震动"),
                    "Missing vibration toggle");
            require(containsTextFragment(root, "铃声和震动至少保留一项"),
                    "Missing at-least-one alert-mode guidance");
            Switch sound = findSwitch(root, "收到消息时持续播放铃声");
            Switch vibration = findSwitch(root, "收到消息时持续震动");
            require(sound != null && vibration != null, "Missing alert switches");

            runOnMainSync(vibration::performClick);
            require(sound.isChecked() && !vibration.isChecked(),
                    "Sound-only mode failed");
            runOnMainSync(vibration::performClick);
            require(sound.isChecked() && vibration.isChecked(),
                    "Sound-plus-vibration mode failed");
            runOnMainSync(sound::performClick);
            require(!sound.isChecked() && vibration.isChecked(),
                    "Vibration-only mode failed");
            runOnMainSync(vibration::performClick);
            require(vibration.isChecked(), "Both alert modes were allowed to turn off");
        } finally {
            runOnMainSync(activity::finish);
            AppPrefs.setSoundEnabled(context, oldSoundEnabled);
            AppPrefs.setVibrationEnabled(context, oldVibrationEnabled);
        }
    }

    private void verifyAlarmPrefersBuiltInSpeaker() throws Exception {
        Context context = getTargetContext();
        boolean oldSoundEnabled = AppPrefs.isSoundEnabled(context);
        boolean oldVibrationEnabled = AppPrefs.isVibrationEnabled(context);
        try {
            AppPrefs.setSoundEnabled(context, true);
            AppPrefs.setVibrationEnabled(context, false);
            AlarmController.start(context, "扬声器路由测试", "约 1.5 秒后自动停止");
            Thread.sleep(1500L);
        } finally {
            AlarmController.stop(context);
            AppPrefs.setSoundEnabled(context, oldSoundEnabled);
            AppPrefs.setVibrationEnabled(context, oldVibrationEnabled);
        }
    }

    private boolean containsText(View view, String expected) {
        return view instanceof TextView
                && expected.contentEquals(((TextView) view).getText())
                || containsInChildren(view, expected, false);
    }

    private boolean containsTextFragment(View view, String expected) {
        return view instanceof TextView
                && ((TextView) view).getText().toString().contains(expected)
                || containsInChildren(view, expected, true);
    }

    private boolean containsInChildren(View view, String expected, boolean fragment) {
        if (!(view instanceof ViewGroup)) {
            return false;
        }
        ViewGroup group = (ViewGroup) view;
        for (int index = 0; index < group.getChildCount(); index++) {
            View child = group.getChildAt(index);
            boolean found = fragment
                    ? containsTextFragment(child, expected)
                    : containsText(child, expected);
            if (found) {
                return true;
            }
        }
        return false;
    }

    private Switch findSwitch(View view, String text) {
        if (view instanceof Switch && text.contentEquals(((Switch) view).getText())) {
            return (Switch) view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int index = 0; index < group.getChildCount(); index++) {
                Switch found = findSwitch(group.getChildAt(index), text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

}
