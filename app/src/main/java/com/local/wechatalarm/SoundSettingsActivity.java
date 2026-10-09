package com.local.wechatalarm;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SoundSettingsActivity extends Activity {
    private static final int REQUEST_AUDIO_FILE = 2001;

    private TextView currentToneView;
    private Ringtone previewRingtone;
    private Switch soundSwitch;
    private Switch vibrationSwitch;
    private boolean updatingAlertSwitches;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildContent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateRingtoneSummary();
    }

    @Override
    protected void onDestroy() {
        stopPreview();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_AUDIO_FILE || resultCode != RESULT_OK
                || data == null || data.getData() == null) {
            return;
        }

        Uri sourceUri = data.getData();
        String fileName = getFileName(sourceUri);
        Uri importedUri = importAudioFile(sourceUri);
        if (importedUri == null) {
            Toast.makeText(this, "无法读取这个音频文件，请换一个文件重试",
                    Toast.LENGTH_LONG).show();
            return;
        }
        AppPrefs.saveRingtone(this, importedUri, fileName);
        updateRingtoneSummary();
        Toast.makeText(this, "音频文件已保存", Toast.LENGTH_SHORT).show();
    }

    private android.view.View buildContent() {
        LinearLayout root = AppUi.newRoot(this);
        AppUi.addBackHeader(this, root, "铃声与震动");

        TextView description = new TextView(this);
        description.setText("可选择仅铃声、仅震动，或铃声与震动同时提醒。"
                + "闹钟播放时会优先使用手机内置扬声器。");
        description.setTextSize(16);
        description.setTextColor(Color.DKGRAY);
        root.addView(description, AppUi.fullWidth(this, 14));

        currentToneView = new TextView(this);
        currentToneView.setTextSize(16);
        currentToneView.setTextColor(Color.rgb(30, 55, 80));
        currentToneView.setPadding(AppUi.dp(this, 14), AppUi.dp(this, 13),
                AppUi.dp(this, 14), AppUi.dp(this, 13));
        AppUi.applyCard(this, currentToneView);
        root.addView(currentToneView, AppUi.fullWidth(this, 12));
        updateRingtoneSummary();

        Button systemToneButton = AppUi.button(this, "从本机系统铃声中选择");
        systemToneButton.setOnClickListener(v -> showSystemRingtoneDialog());
        root.addView(systemToneButton, AppUi.fullWidth(this, 10));

        Button fileButton = AppUi.button(this, "选择手机中的音频文件");
        fileButton.setOnClickListener(v -> chooseAudioFile());
        root.addView(fileButton, AppUi.fullWidth(this, 10));

        Button bundledButton = AppUi.button(this, "恢复 App 内置铃声");
        bundledButton.setOnClickListener(v -> {
            stopPreview();
            AppPrefs.saveRingtone(this, null, AppPrefs.DEFAULT_RINGTONE_NAME);
            updateRingtoneSummary();
            Toast.makeText(this, "已恢复内置铃声", Toast.LENGTH_SHORT).show();
        });
        root.addView(bundledButton, AppUi.fullWidth(this, 12));

        soundSwitch = new Switch(this);
        soundSwitch.setText("收到消息时持续播放铃声");
        soundSwitch.setTextSize(17);
        soundSwitch.setPadding(AppUi.dp(this, 16), AppUi.dp(this, 14),
                AppUi.dp(this, 16), AppUi.dp(this, 14));
        soundSwitch.setChecked(AppPrefs.isSoundEnabled(this));
        AppUi.applyCard(this, soundSwitch);
        root.addView(soundSwitch, AppUi.fullWidth(this, 10));

        vibrationSwitch = new Switch(this);
        vibrationSwitch.setText("收到消息时持续震动");
        vibrationSwitch.setTextSize(17);
        vibrationSwitch.setPadding(AppUi.dp(this, 16), AppUi.dp(this, 14),
                AppUi.dp(this, 16), AppUi.dp(this, 14));
        vibrationSwitch.setChecked(AppPrefs.isVibrationEnabled(this));
        AppUi.applyCard(this, vibrationSwitch);
        root.addView(vibrationSwitch, AppUi.fullWidth(this, 10));

        soundSwitch.setOnCheckedChangeListener((buttonView, checked) -> {
            if (updatingAlertSwitches) {
                return;
            }
            if (!checked && !vibrationSwitch.isChecked()) {
                restoreRequiredSwitch(soundSwitch);
                return;
            }
            AppPrefs.setSoundEnabled(this, checked);
            updateRingtoneSummary();
        });
        vibrationSwitch.setOnCheckedChangeListener((buttonView, checked) -> {
            if (updatingAlertSwitches) {
                return;
            }
            if (!checked && !soundSwitch.isChecked()) {
                restoreRequiredSwitch(vibrationSwitch);
                return;
            }
            AppPrefs.setVibrationEnabled(this, checked);
        });

        TextView modeHelp = new TextView(this);
        modeHelp.setText("铃声和震动至少保留一项。Android 9 及以上会优先把铃声路由到手机内置扬声器。"
                + "Android 8.x 受系统接口限制，仍使用系统默认音频路由。");
        modeHelp.setTextSize(14);
        modeHelp.setTextColor(Color.DKGRAY);
        modeHelp.setLineSpacing(0, 1.1f);
        root.addView(modeHelp, AppUi.fullWidth(this, 24));

        root.addView(AppUi.sectionTitle(this, "试听最终效果"),
                AppUi.fullWidth(this, 8));

        Button testButton = AppUi.primaryButton(this, "测试闹钟");
        testButton.setOnClickListener(v -> {
            stopPreview();
            AlarmController.start(this, "闹钟测试", "点击“停止闹钟”结束");
        });
        root.addView(testButton, AppUi.fullWidth(this, 10));

        Button stopButton = AppUi.button(this, "停止闹钟");
        stopButton.setOnClickListener(v -> AlarmController.stop(this));
        root.addView(stopButton, AppUi.fullWidth(this, 0));

        return AppUi.wrap(this, root);
    }

    private void showSystemRingtoneDialog() {
        stopPreview();
        List<String> names = new ArrayList<>();
        List<Uri> uris = new ArrayList<>();
        Set<String> seenUris = new HashSet<>();

        try {
            RingtoneManager manager = new RingtoneManager(this);
            manager.setType(RingtoneManager.TYPE_ALARM | RingtoneManager.TYPE_RINGTONE);
            try (Cursor cursor = manager.getCursor()) {
                while (cursor.moveToNext()) {
                    Uri uri = manager.getRingtoneUri(cursor.getPosition());
                    if (uri == null || !seenUris.add(uri.toString())) {
                        continue;
                    }
                    String title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX);
                    names.add(title == null || title.trim().isEmpty()
                            ? "未命名铃声" : title.trim());
                    uris.add(uri);
                }
            }
        } catch (RuntimeException exc) {
            Toast.makeText(this, "无法读取本机系统铃声", Toast.LENGTH_LONG).show();
            return;
        }

        if (uris.isEmpty()) {
            Toast.makeText(this, "本机没有可用的系统铃声", Toast.LENGTH_LONG).show();
            return;
        }

        Uri currentUri = AppPrefs.getRingtoneUri(this);
        int initialIndex = 0;
        if (currentUri != null) {
            for (int index = 0; index < uris.size(); index++) {
                if (currentUri.toString().equals(uris.get(index).toString())) {
                    initialIndex = index;
                    break;
                }
            }
        }
        int[] selectedIndex = {initialIndex};
        String[] nameArray = names.toArray(new String[0]);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("选择系统铃声（点击可试听）")
                .setSingleChoiceItems(nameArray, initialIndex, (itemDialog, which) -> {
                    selectedIndex[0] = which;
                    playPreview(uris.get(which));
                })
                .setNegativeButton("取消", null)
                .setPositiveButton("使用此铃声", (itemDialog, which) -> {
                    int selected = selectedIndex[0];
                    AppPrefs.saveRingtone(this, uris.get(selected),
                            names.get(selected));
                    updateRingtoneSummary();
                    Toast.makeText(this, "系统铃声已保存", Toast.LENGTH_SHORT).show();
                })
                .create();
        dialog.setOnDismissListener(ignored -> stopPreview());
        dialog.show();
    }

    private void playPreview(Uri uri) {
        stopPreview();
        try {
            previewRingtone = RingtoneManager.getRingtone(this, uri);
            if (previewRingtone != null) {
                previewRingtone.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build());
                previewRingtone.play();
            }
        } catch (RuntimeException exc) {
            stopPreview();
            Toast.makeText(this, "这个铃声无法试听", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopPreview() {
        if (previewRingtone != null) {
            try {
                previewRingtone.stop();
            } catch (RuntimeException ignored) {
            }
            previewRingtone = null;
        }
    }

    private void chooseAudioFile() {
        stopPreview();
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("audio/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivityForResult(intent, REQUEST_AUDIO_FILE);
        } catch (RuntimeException exc) {
            Intent fallback = new Intent(Intent.ACTION_GET_CONTENT);
            fallback.addCategory(Intent.CATEGORY_OPENABLE);
            fallback.setType("audio/*");
            fallback.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(fallback, REQUEST_AUDIO_FILE);
        }
    }

    private Uri importAudioFile(Uri sourceUri) {
        File destination = new File(getFilesDir(), "selected_alarm_audio");
        try (InputStream input = getContentResolver().openInputStream(sourceUri);
             FileOutputStream output = new FileOutputStream(destination, false)) {
            if (input == null) {
                return null;
            }
            byte[] buffer = new byte[32 * 1024];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            output.flush();
            return Uri.fromFile(destination);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String getFileName(Uri uri) {
        try (Cursor cursor = getContentResolver().query(
                uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) {
                    String name = cursor.getString(index);
                    if (name != null && !name.trim().isEmpty()) {
                        return name.trim();
                    }
                }
            }
        } catch (RuntimeException ignored) {
        }
        String fallback = uri.getLastPathSegment();
        return fallback == null || fallback.isEmpty() ? "已选择的音频" : fallback;
    }

    private void updateRingtoneSummary() {
        if (currentToneView != null) {
            String status = AppPrefs.isSoundEnabled(this)
                    ? "已启用，将优先从手机扬声器播放"
                    : "当前为仅震动，重新开启铃声后使用";
            currentToneView.setText("当前铃声\n" + AppPrefs.getRingtoneName(this)
                    + "\n" + status);
        }
    }

    private void restoreRequiredSwitch(Switch target) {
        updatingAlertSwitches = true;
        target.setChecked(true);
        updatingAlertSwitches = false;
        Toast.makeText(this, "铃声和震动至少需要开启一项",
                Toast.LENGTH_SHORT).show();
    }
}
