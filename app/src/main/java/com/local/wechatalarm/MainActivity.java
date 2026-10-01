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

import java.util.Set;

public class MainActivity extends Activity {
    private static final int REQUEST_NOTIFICATIONS = 1001;

    private TextView statusView;
    private AppUi.NavigationRow targetsRow;
    private AppUi.NavigationRow soundRow;
    private AppUi.NavigationRow permissionsRow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AlarmController.ensureChannel(this);
        MonitorNotification.ensureChannel(this);
        setContentView(buildContent());
        requestNotificationPermissionIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateSummaries();
        if (ListenerAccess.isGranted(this)) {
            WeChatNotificationListener.requestReconnect(this);
        }
    }

    private android.view.View buildContent() {
        LinearLayout root = AppUi.newRoot(this);
        AppUi.addHomeHeader(this, root, "微信消息闹钟",
                "微信指定联系人来消息时持续响铃。");

        statusView = new TextView(this);
        statusView.setTextSize(15);
        statusView.setLineSpacing(0, 1.15f);
        statusView.setPadding(AppUi.dp(this, 16), AppUi.dp(this, 14),
                AppUi.dp(this, 16), AppUi.dp(this, 14));
        AppUi.applyStatusPanel(this, statusView, true);
        root.addView(statusView, AppUi.fullWidth(this, 22));

        root.addView(AppUi.sectionTitle(this, "设置"), AppUi.fullWidth(this, 10));

        LinearLayout settingsGroup = AppUi.group(this);
        targetsRow = AppUi.navigationRow(this, "监听对象");
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

        TextView note = new TextView(this);
        note.setText("提示：保留通知栏的“微信消息监听中”，并允许本 App 自启动、后台省电策略设为“不限制”。");
        note.setTextSize(15);
        note.setTextColor(Color.DKGRAY);
        note.setLineSpacing(0, 1.15f);
        root.addView(note, AppUi.fullWidth(this, 0));

        return AppUi.wrap(this, root);
    }

    private void updateSummaries() {
        if (statusView == null) {
            return;
        }
        boolean granted = ListenerAccess.isGranted(this);
        Set<String> targets = AppPrefs.getTargets(this);
        String targetSummary;
        String targetCardSummary;
        if (targets.isEmpty()) {
            targetSummary = "尚未添加监听对象";
            targetCardSummary = targetSummary;
        } else if (targets.size() == 1) {
            targetSummary = "监听对象：" + targets.iterator().next();
            targetCardSummary = targets.iterator().next();
        } else {
            targetSummary = "正在监听 " + targets.size() + " 个对象";
            targetCardSummary = "已监听 " + targets.size() + " 个对象";
        }

        statusView.setText(granted
                ? "●  监听运行中\n" + targetSummary
                : "需要完成通知监听授权\n进入“权限与后台”进行设置");
        statusView.setTextColor(granted ? AppUi.COLOR_ACCENT : Color.rgb(180, 92, 0));
        AppUi.applyStatusPanel(this, statusView, granted);

        String ringtoneSummary = AppPrefs.getRingtoneName(this);
        targetsRow.setSummary(targetCardSummary);
        soundRow.setSummary("铃声：" + ringtoneSummary
                + "\n震动：" + (AppPrefs.isVibrationEnabled(this) ? "开启" : "关闭"));
        permissionsRow.setSummary(granted ? "通知监听已授权" : "需要授予通知使用权");
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
