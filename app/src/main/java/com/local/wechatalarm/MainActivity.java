package com.local.wechatalarm;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;

import java.util.List;

public class MainActivity extends Activity {
    private static final int REQUEST_NOTIFICATIONS = 1001;

    private TextView statusView;
    private TextView noteView;
    private AppUi.ToggleRow monitoringRow;
    private AppUi.NavigationRow targetsRow;
    private AppUi.NavigationRow soundRow;
    private AppUi.NavigationRow permissionsRow;
    private boolean updatingMonitoringSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AlarmController.ensureChannel(this);
        MonitorNotification.ensureChannel(this);
        ListenerMonitorService.ensureRunning(this);
        setContentView(buildContent());
        requestNotificationPermissionIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateSummaries();
        if (ListenerAccess.isGranted(this)) {
            ListenerMonitorService.ensureRunning(this);
        }
    }

    private android.view.View buildContent() {
        LinearLayout root = AppUi.newRoot(this);
        AppUi.addHomeHeader(this, root, "消息通知闹钟",
                "微信、企业微信或 QQ 通知符合规则时持续提醒。");

        LinearLayout monitoringGroup = AppUi.group(this);
        monitoringRow = AppUi.toggleRow(this, "消息监听");
        monitoringRow.setOnCheckedChangeListener((buttonView, checked) -> {
            if (!updatingMonitoringSwitch) {
                setMonitoringEnabled(checked);
            }
        });
        monitoringGroup.addView(monitoringRow, AppUi.fullWidth(this, 0));
        root.addView(monitoringGroup, AppUi.fullWidth(this, 14));

        statusView = new TextView(this);
        statusView.setTextSize(15);
        statusView.setLineSpacing(0, 1.15f);
        statusView.setPadding(AppUi.dp(this, 16), AppUi.dp(this, 14),
                AppUi.dp(this, 16), AppUi.dp(this, 14));
        AppUi.applyStatusPanel(this, statusView, true);
        root.addView(statusView, AppUi.fullWidth(this, 22));

        root.addView(AppUi.sectionTitle(this, "设置"), AppUi.fullWidth(this, 10));

        LinearLayout settingsGroup = AppUi.group(this);
        targetsRow = AppUi.navigationRow(this, "匹配规则");
        targetsRow.setOnClickListener(v ->
                startActivity(new Intent(this, TargetSettingsActivity.class)));
        settingsGroup.addView(targetsRow, AppUi.fullWidth(this, 0));
        AppUi.addDivider(this, settingsGroup);

        soundRow = AppUi.navigationRow(this, "铃声与震动");
        soundRow.setOnClickListener(v ->
                startActivity(new Intent(this, SoundSettingsActivity.class)));
        settingsGroup.addView(soundRow, AppUi.fullWidth(this, 0));
        AppUi.addDivider(this, settingsGroup);

        permissionsRow = AppUi.navigationRow(this, "权限与后台");
        permissionsRow.setOnClickListener(v ->
                startActivity(new Intent(this, PermissionSettingsActivity.class)));
        settingsGroup.addView(permissionsRow, AppUi.fullWidth(this, 0));
        root.addView(settingsGroup, AppUi.fullWidth(this, 26));

        root.addView(AppUi.sectionTitle(this, "快捷操作"), AppUi.fullWidth(this, 10));

        Button testButton = AppUi.primaryButton(this, "测试闹钟");
        testButton.setOnClickListener(v ->
                AlarmController.start(this, "闹钟测试", "点击“停止闹钟”结束"));
        root.addView(testButton, AppUi.fullWidth(this, 10));

        Button stopButton = AppUi.button(this, "停止闹钟");
        stopButton.setOnClickListener(v -> AlarmController.stop(this));
        root.addView(stopButton, AppUi.fullWidth(this, 22));

        noteView = new TextView(this);
        noteView.setTextSize(15);
        noteView.setTextColor(Color.DKGRAY);
        noteView.setLineSpacing(0, 1.15f);
        root.addView(noteView, AppUi.fullWidth(this, 0));

        return AppUi.wrap(this, root);
    }

    private void updateSummaries() {
        if (statusView == null) {
            return;
        }
        boolean granted = ListenerAccess.isGranted(this);
        boolean monitoringEnabled = AppPrefs.isMonitoringEnabled(this);
        List<MatchRule> rules = AppPrefs.getRules(this);
        String ruleSummary;
        String ruleCardSummary;
        if (rules.isEmpty()) {
            ruleSummary = "尚未添加匹配规则";
            ruleCardSummary = ruleSummary;
        } else if (rules.size() == 1) {
            ruleSummary = "匹配规则：" + rules.get(0).summary();
            ruleCardSummary = rules.get(0).summary();
        } else {
            ruleSummary = "正在使用 " + rules.size() + " 条匹配规则";
            ruleCardSummary = "已设置 " + rules.size() + " 条匹配规则";
        }

        updatingMonitoringSwitch = true;
        monitoringRow.setChecked(monitoringEnabled);
        updatingMonitoringSwitch = false;
        monitoringRow.setSummary(monitoringEnabled
                ? "已开启，微信、企业微信或 QQ 通知符合规则时提醒"
                : "已关闭，不监听消息且不显示常驻通知");

        if (!monitoringEnabled) {
            statusView.setText("监听已关闭\n打开上方开关即可恢复");
            statusView.setTextColor(AppUi.COLOR_SUBTEXT);
            AppUi.applyPausedStatusPanel(this, statusView);
        } else {
            statusView.setText(granted
                    ? "●  监听运行中\n" + ruleSummary
                    : "需要完成通知监听授权\n进入“权限与后台”进行设置");
            statusView.setTextColor(granted
                    ? AppUi.COLOR_ACCENT : Color.rgb(180, 92, 0));
            AppUi.applyStatusPanel(this, statusView, granted);
        }

        boolean soundEnabled = AppPrefs.isSoundEnabled(this);
        boolean vibrationEnabled = AppPrefs.isVibrationEnabled(this);
        String alertMode = soundEnabled && vibrationEnabled
                ? "铃声 + 震动" : (soundEnabled ? "仅铃声" : "仅震动");
        targetsRow.setSummary(ruleCardSummary);
        soundRow.setSummary("提醒方式：" + alertMode
                + (soundEnabled ? "\n铃声：" + AppPrefs.getRingtoneName(this) : ""));
        permissionsRow.setSummary(!monitoringEnabled
                ? (granted ? "通知使用权已保留，开启后自动连接" : "需要授予通知使用权")
                : (granted ? "通知监听已连接" : "需要授予通知使用权"));
        noteView.setText(monitoringEnabled
                ? "提示：保留通知栏的“消息通知监听中”，并允许本 App 自启动、后台省电策略设为“不限制”。"
                : "监听关闭期间不会读取微信、企业微信或 QQ 通知，也不会显示常驻通知。");
    }

    private void setMonitoringEnabled(boolean enabled) {
        AppPrefs.setMonitoringEnabled(this, enabled);
        if (enabled) {
            ListenerMonitorService.ensureRunning(this);
            WeChatNotificationListener.resume(this);
            requestNotificationPermissionIfNeeded();
        } else {
            AlarmController.stop(this);
            ListenerMonitorService.stopRunning(this);
            WeChatNotificationListener.pause(this);
        }
        updateSummaries();
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_NOTIFICATIONS);
        }
    }
}
