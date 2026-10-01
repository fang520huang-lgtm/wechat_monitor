package com.local.wechatalarm;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.LinkedHashSet;
import java.util.Set;

public class TargetSettingsActivity extends Activity {
    private final Set<String> targets = new LinkedHashSet<>();
    private LinearLayout targetList;
    private EditText newTargetInput;
    private TextView countView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        targets.addAll(AppPrefs.getTargets(this));
        setContentView(buildContent());
    }

    private android.view.View buildContent() {
        LinearLayout root = AppUi.newRoot(this);
        AppUi.addBackHeader(this, root, "监听对象");

        TextView description = new TextView(this);
        description.setText("添加微信通知中显示的准确备注名。名单修改后会立即生效。");
        description.setTextSize(16);
        description.setTextColor(Color.DKGRAY);
        root.addView(description, AppUi.fullWidth(this, 14));

        countView = new TextView(this);
        countView.setTextSize(16);
        countView.setTextColor(Color.rgb(20, 80, 55));
        countView.setPadding(AppUi.dp(this, 12), AppUi.dp(this, 11),
                AppUi.dp(this, 12), AppUi.dp(this, 11));
        AppUi.applyStatusPanel(this, countView, true);
        root.addView(countView, AppUi.fullWidth(this, 14));

        targetList = new LinearLayout(this);
        targetList.setOrientation(LinearLayout.VERTICAL);
        root.addView(targetList, AppUi.fullWidth(this, 8));

        LinearLayout addRow = new LinearLayout(this);
        addRow.setOrientation(LinearLayout.HORIZONTAL);
        addRow.setGravity(Gravity.CENTER_VERTICAL);

        newTargetInput = new EditText(this);
        newTargetInput.setSingleLine(true);
        newTargetInput.setTextSize(17);
        newTargetInput.setHint("输入微信备注名");
        newTargetInput.setInputType(InputType.TYPE_CLASS_TEXT);
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        inputParams.rightMargin = AppUi.dp(this, 8);
        addRow.addView(newTargetInput, inputParams);

        Button addButton = AppUi.primaryButton(this, "添加");
        addButton.setMinWidth(AppUi.dp(this, 86));
        addButton.setOnClickListener(v -> addTarget());
        addRow.addView(addButton, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        root.addView(addRow, AppUi.fullWidth(this, 22));

        TextView privacy = new TextView(this);
        privacy.setText("只有一个对象时，常驻通知会显示姓名；两个或更多对象时只显示“监听多个对象”。");
        privacy.setTextSize(15);
        privacy.setTextColor(Color.DKGRAY);
        root.addView(privacy, AppUi.fullWidth(this, 0));

        refreshTargetList();
        return AppUi.wrap(this, root);
    }

    private void addTarget() {
        String name = newTargetInput.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "请先输入微信备注名", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!targets.add(name)) {
            Toast.makeText(this, "这个对象已经在列表中", Toast.LENGTH_SHORT).show();
            return;
        }
        newTargetInput.setText("");
        persistTargets();
    }

    private void removeTarget(String name) {
        targets.remove(name);
        persistTargets();
    }

    private void persistTargets() {
        AppPrefs.saveTargets(this, targets);
        refreshTargetList();
        MonitorNotification.refresh(this);
    }

    private void refreshTargetList() {
        if (targetList == null) {
            return;
        }
        targetList.removeAllViews();
        countView.setText(targets.isEmpty()
                ? "尚未添加监听对象"
                : "已监听 " + targets.size() + " 个对象");

        if (targets.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("请在下方输入备注名并添加");
            empty.setTextSize(16);
            empty.setTextColor(Color.GRAY);
            empty.setPadding(AppUi.dp(this, 12), AppUi.dp(this, 12),
                    AppUi.dp(this, 12), AppUi.dp(this, 12));
            targetList.addView(empty, AppUi.fullWidth(this, 6));
            return;
        }

        for (String name : targets) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(AppUi.dp(this, 14), AppUi.dp(this, 7),
                    AppUi.dp(this, 7), AppUi.dp(this, 7));
            AppUi.applyCard(this, row);

            TextView nameView = new TextView(this);
            nameView.setText(name);
            nameView.setTextSize(18);
            nameView.setTextColor(Color.rgb(25, 45, 65));
            row.addView(nameView, new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            Button deleteButton = AppUi.dangerButton(this, "删除");
            deleteButton.setTextSize(15);
            deleteButton.setMinHeight(AppUi.dp(this, 44));
            deleteButton.setOnClickListener(v -> removeTarget(name));
            row.addView(deleteButton, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));

            targetList.addView(row, AppUi.fullWidth(this, 8));
        }
    }
}
