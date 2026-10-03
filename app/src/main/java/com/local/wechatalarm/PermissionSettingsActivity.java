package com.local.wechatalarm;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class PermissionSettingsActivity extends Activity {
    private TextView statusView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildContent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        ListenerMonitorService.ensureRunning(this);
        updateStatus();
    }

    private android.view.View buildContent() {
        LinearLayout root = AppUi.newRoot(this);
        AppUi.addBackHeader(this, root, "权限与后台");

        statusView = new TextView(this);
        statusView.setTextSize(17);
        statusView.setPadding(AppUi.dp(this, 14), AppUi.dp(this, 13),
                AppUi.dp(this, 14), AppUi.dp(this, 13));
        AppUi.applyStatusPanel(this, statusView, true);
        root.addView(statusView, AppUi.fullWidth(this, 18));

        Button accessButton = AppUi.primaryButton(this, "授予“通知使用权”");
        accessButton.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        root.addView(accessButton, AppUi.fullWidth(this, 10));

        Button reconnectButton = AppUi.button(this, "重新连接监听服务");
        reconnectButton.setOnClickListener(v -> {
            WeChatNotificationListener.requestReconnect(this);
            Toast.makeText(this, "正在强制重新连接监听服务", Toast.LENGTH_SHORT).show();
        });
        root.addView(reconnectButton, AppUi.fullWidth(this, 10));

        Button batteryButton = AppUi.button(this, "打开电池与后台设置");
        batteryButton.setOnClickListener(v -> openBatterySettings());
        root.addView(batteryButton, AppUi.fullWidth(this, 22));

        TextView help = new TextView(this);
        help.setText("后台使用要点\n\n"
                + "• 首次使用必须授予通知使用权。\n"
                + "• 请允许本 App 自启动，并把省电策略设为“不限制”。\n"
                + "• 请保留通知栏的“微信消息监听中”，不要从最近任务中划掉本 App，建议锁定。\n"
                + "• 微信停留在当前聊天页面时可能不生成系统通知，建议退到后台或锁屏。\n"
                + "• 双开微信能否同时监听取决于手机系统是否把双开通知提供给通知监听服务。");
        help.setTextSize(15);
        help.setTextColor(Color.DKGRAY);
        help.setLineSpacing(0, 1.15f);
        root.addView(help, AppUi.fullWidth(this, 0));

        return AppUi.wrap(this, root);
    }

    private void updateStatus() {
        if (statusView == null) {
            return;
        }
        boolean granted = ListenerAccess.isGranted(this);
        boolean connected = WeChatNotificationListener.isConnected();
        statusView.setText(granted
                ? (connected ? "✓ 通知监听已连接" : "⟳ 已授权，正在自动恢复连接")
                : "⚠ 尚未授予通知使用权\n请点击下方按钮完成授权");
        statusView.setTextColor(granted && connected
                ? Color.rgb(22, 110, 55) : Color.rgb(180, 75, 0));
        AppUi.applyStatusPanel(this, statusView, granted && connected);
    }

    private void openBatterySettings() {
        try {
            startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
        } catch (RuntimeException ignored) {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        }
    }
}
