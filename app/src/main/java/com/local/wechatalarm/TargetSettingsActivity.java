package com.local.wechatalarm;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class TargetSettingsActivity extends Activity {
    private final List<MatchRule> rules = new ArrayList<>();
    private LinearLayout ruleList;
    private TextView countView;
    private RadioButton wechatSource;
    private RadioButton wecomSource;
    private RadioButton qqSource;
    private CheckBox titleEnabled;
    private EditText titleInput;
    private RadioButton titleExact;
    private RadioButton titleContains;
    private CheckBox textEnabled;
    private EditText textInput;
    private RadioButton textExact;
    private RadioButton textContains;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        rules.addAll(AppPrefs.getRules(this));
        setContentView(buildContent());
    }

    private View buildContent() {
        LinearLayout root = AppUi.newRoot(this);
        AppUi.addBackHeader(this, root, "匹配规则");

        TextView description = new TextView(this);
        description.setText("每条规则先选择微信、企业微信或 QQ，再匹配通知标题、正文，"
                + "或同时匹配两者。同一规则内为“且”，多条规则之间为“或”。");
        description.setTextSize(16);
        description.setTextColor(Color.DKGRAY);
        description.setLineSpacing(0, 1.12f);
        root.addView(description, AppUi.fullWidth(this, 14));

        countView = new TextView(this);
        countView.setTextSize(16);
        countView.setTextColor(Color.rgb(20, 80, 55));
        countView.setPadding(AppUi.dp(this, 12), AppUi.dp(this, 11),
                AppUi.dp(this, 12), AppUi.dp(this, 11));
        AppUi.applyStatusPanel(this, countView, true);
        root.addView(countView, AppUi.fullWidth(this, 14));

        ruleList = new LinearLayout(this);
        ruleList.setOrientation(LinearLayout.VERTICAL);
        root.addView(ruleList, AppUi.fullWidth(this, 14));

        root.addView(AppUi.sectionTitle(this, "新增规则"),
                AppUi.fullWidth(this, 10));

        LinearLayout editor = new LinearLayout(this);
        editor.setOrientation(LinearLayout.VERTICAL);
        editor.setPadding(AppUi.dp(this, 14), AppUi.dp(this, 12),
                AppUi.dp(this, 14), AppUi.dp(this, 14));
        AppUi.applyCard(this, editor);

        TextView sourceLabel = new TextView(this);
        sourceLabel.setText("消息来源");
        sourceLabel.setTextSize(17);
        sourceLabel.setTextColor(AppUi.COLOR_TEXT);
        editor.addView(sourceLabel, AppUi.fullWidth(this, 4));

        RadioGroup sourceModes = new RadioGroup(this);
        sourceModes.setOrientation(RadioGroup.HORIZONTAL);
        sourceModes.setGravity(Gravity.CENTER_VERTICAL);
        wechatSource = new RadioButton(this);
        wechatSource.setId(View.generateViewId());
        wechatSource.setText("微信");
        wechatSource.setTextSize(16);
        sourceModes.addView(wechatSource, new RadioGroup.LayoutParams(
                0, RadioGroup.LayoutParams.WRAP_CONTENT, 1f));
        wecomSource = new RadioButton(this);
        wecomSource.setId(View.generateViewId());
        wecomSource.setText("企业微信");
        wecomSource.setTextSize(16);
        sourceModes.addView(wecomSource, new RadioGroup.LayoutParams(
                0, RadioGroup.LayoutParams.WRAP_CONTENT, 1f));
        qqSource = new RadioButton(this);
        qqSource.setId(View.generateViewId());
        qqSource.setText("QQ");
        qqSource.setTextSize(16);
        sourceModes.addView(qqSource, new RadioGroup.LayoutParams(
                0, RadioGroup.LayoutParams.WRAP_CONTENT, 1f));
        sourceModes.check(wechatSource.getId());
        editor.addView(sourceModes, AppUi.fullWidth(this, 12));

        titleEnabled = new CheckBox(this);
        titleEnabled.setText("匹配通知标题");
        titleEnabled.setTextSize(17);
        titleEnabled.setChecked(true);
        editor.addView(titleEnabled, AppUi.fullWidth(this, 4));

        titleInput = new EditText(this);
        titleInput.setSingleLine(true);
        titleInput.setTextSize(16);
        titleInput.setHint("输入标题，例如联系人或群名称");
        titleInput.setInputType(InputType.TYPE_CLASS_TEXT);
        editor.addView(titleInput, AppUi.fullWidth(this, 4));

        RadioGroup titleModes = createModeGroup();
        titleExact = (RadioButton) titleModes.getChildAt(0);
        titleContains = (RadioButton) titleModes.getChildAt(1);
        titleModes.check(titleExact.getId());
        editor.addView(titleModes, AppUi.fullWidth(this, 12));
        titleEnabled.setOnCheckedChangeListener((buttonView, checked) ->
                setFieldEnabled(titleInput, titleExact, titleContains, checked));

        textEnabled = new CheckBox(this);
        textEnabled.setText("匹配通知正文");
        textEnabled.setTextSize(17);
        textEnabled.setChecked(false);
        editor.addView(textEnabled, AppUi.fullWidth(this, 4));

        textInput = new EditText(this);
        textInput.setSingleLine(true);
        textInput.setTextSize(16);
        textInput.setHint("输入消息正文或关键词");
        textInput.setInputType(InputType.TYPE_CLASS_TEXT);
        editor.addView(textInput, AppUi.fullWidth(this, 4));

        RadioGroup textModes = createModeGroup();
        textExact = (RadioButton) textModes.getChildAt(0);
        textContains = (RadioButton) textModes.getChildAt(1);
        textModes.check(textExact.getId());
        editor.addView(textModes, AppUi.fullWidth(this, 12));
        setFieldEnabled(textInput, textExact, textContains, false);
        textEnabled.setOnCheckedChangeListener((buttonView, checked) ->
                setFieldEnabled(textInput, textExact, textContains, checked));

        Button addButton = AppUi.primaryButton(this, "添加匹配规则");
        addButton.setOnClickListener(v -> addRule());
        editor.addView(addButton, AppUi.fullWidth(this, 0));
        root.addView(editor, AppUi.fullWidth(this, 18));

        TextView help = new TextView(this);
        help.setText("默认是“微信 + 只匹配标题 + 完全匹配”。规则列表会把来源、标题条件和正文条件分行显示。"
                + "“完全匹配”要求整段文字一致；“部分匹配”只要通知中包含所填文字即可。"
                + "升级前的普通微信监听对象会自动迁移为标题完全匹配规则。");
        help.setTextSize(15);
        help.setTextColor(Color.DKGRAY);
        help.setLineSpacing(0, 1.12f);
        root.addView(help, AppUi.fullWidth(this, 0));

        refreshRuleList();
        return AppUi.wrap(this, root);
    }

    private RadioGroup createModeGroup() {
        RadioGroup group = new RadioGroup(this);
        group.setOrientation(RadioGroup.HORIZONTAL);
        group.setGravity(Gravity.CENTER_VERTICAL);

        RadioButton exact = new RadioButton(this);
        exact.setId(View.generateViewId());
        exact.setText("完全匹配");
        exact.setTextSize(15);
        group.addView(exact, new RadioGroup.LayoutParams(
                0, RadioGroup.LayoutParams.WRAP_CONTENT, 1f));

        RadioButton contains = new RadioButton(this);
        contains.setId(View.generateViewId());
        contains.setText("部分匹配（包含）");
        contains.setTextSize(15);
        group.addView(contains, new RadioGroup.LayoutParams(
                0, RadioGroup.LayoutParams.WRAP_CONTENT, 1f));
        return group;
    }

    private void setFieldEnabled(EditText input, RadioButton exact,
                                 RadioButton contains, boolean enabled) {
        input.setEnabled(enabled);
        exact.setEnabled(enabled);
        contains.setEnabled(enabled);
    }

    private void addRule() {
        boolean useTitle = titleEnabled.isChecked();
        boolean useText = textEnabled.isChecked();
        String titlePattern = titleInput.getText().toString().trim();
        String textPattern = textInput.getText().toString().trim();

        if (!useTitle && !useText) {
            Toast.makeText(this, "请至少选择标题或正文中的一项",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        if (useTitle && titlePattern.isEmpty()) {
            Toast.makeText(this, "请输入要匹配的通知标题",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        if (useText && textPattern.isEmpty()) {
            Toast.makeText(this, "请输入要匹配的通知正文",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        MatchRule.AppSource source = MatchRule.AppSource.WECHAT;
        if (wecomSource.isChecked()) {
            source = MatchRule.AppSource.WECOM;
        } else if (qqSource.isChecked()) {
            source = MatchRule.AppSource.QQ;
        }
        MatchRule rule = new MatchRule(
                source,
                useTitle,
                titleContains.isChecked() ? MatchRule.Mode.CONTAINS : MatchRule.Mode.EXACT,
                titlePattern,
                useText,
                textContains.isChecked() ? MatchRule.Mode.CONTAINS : MatchRule.Mode.EXACT,
                textPattern);
        rules.add(rule);
        persistRules();
        resetEditor();
        Toast.makeText(this, "匹配规则已添加", Toast.LENGTH_SHORT).show();
    }

    private void resetEditor() {
        wechatSource.setChecked(true);
        titleEnabled.setChecked(true);
        titleInput.setText("");
        titleExact.setChecked(true);
        textEnabled.setChecked(false);
        textInput.setText("");
        textExact.setChecked(true);
        titleInput.requestFocus();
    }

    private void removeRule(MatchRule rule) {
        rules.remove(rule);
        persistRules();
    }

    private void persistRules() {
        AppPrefs.saveRules(this, rules);
        refreshRuleList();
        MonitorNotification.refresh(this);
    }

    private void refreshRuleList() {
        if (ruleList == null) {
            return;
        }
        ruleList.removeAllViews();
        countView.setText(rules.isEmpty()
                ? "尚未添加匹配规则"
                : "已有 " + rules.size() + " 条匹配规则");

        if (rules.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("请在下方添加规则；没有规则时不会触发闹钟");
            empty.setTextSize(16);
            empty.setTextColor(Color.GRAY);
            empty.setPadding(AppUi.dp(this, 12), AppUi.dp(this, 12),
                    AppUi.dp(this, 12), AppUi.dp(this, 12));
            ruleList.addView(empty, AppUi.fullWidth(this, 6));
            return;
        }

        for (MatchRule rule : rules) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(AppUi.dp(this, 14), AppUi.dp(this, 9),
                    AppUi.dp(this, 7), AppUi.dp(this, 9));
            AppUi.applyCard(this, row);

            TextView summary = new TextView(this);
            summary.setText(rule.summary());
            summary.setTextSize(16);
            summary.setTextColor(Color.rgb(25, 45, 65));
            summary.setLineSpacing(0, 1.1f);
            row.addView(summary, new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            Button deleteButton = AppUi.dangerButton(this, "删除");
            deleteButton.setTextSize(15);
            deleteButton.setMinHeight(AppUi.dp(this, 44));
            deleteButton.setOnClickListener(v -> removeRule(rule));
            row.addView(deleteButton, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));

            ruleList.addView(row, AppUi.fullWidth(this, 8));
        }
    }
}
