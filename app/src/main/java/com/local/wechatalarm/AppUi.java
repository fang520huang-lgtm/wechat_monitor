package com.local.wechatalarm;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

final class AppUi {
    static final int COLOR_TEXT = Color.rgb(18, 18, 20);
    static final int COLOR_SUBTEXT = Color.rgb(104, 104, 112);
    static final int COLOR_PRIMARY = Color.rgb(22, 22, 24);
    static final int COLOR_ACCENT = Color.rgb(24, 91, 255);
    private static final int COLOR_SURFACE = Color.WHITE;
    private static final int COLOR_BORDER = Color.rgb(232, 232, 235);

    private AppUi() {}

    static LinearLayout newRoot(Activity activity) {
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(activity, 20), dp(activity, 22),
                dp(activity, 20), dp(activity, 34));
        root.setBackgroundColor(Color.rgb(250, 250, 250));
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            int topInset;
            int bottomInset;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                topInset = insets.getInsets(WindowInsets.Type.systemBars()).top;
                bottomInset = insets.getInsets(WindowInsets.Type.systemBars()).bottom;
            } else {
                topInset = insets.getSystemWindowInsetTop();
                bottomInset = insets.getSystemWindowInsetBottom();
            }
            view.setPadding(dp(activity, 20), dp(activity, 10) + topInset,
                    dp(activity, 20), dp(activity, 34) + bottomInset);
            return insets;
        });
        return root;
    }

    static ScrollView wrap(Activity activity, LinearLayout root) {
        ScrollView scrollView = new ScrollView(activity);
        scrollView.setFillViewport(true);
        scrollView.setClipToPadding(false);
        scrollView.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));
        return scrollView;
    }

    static void addHomeHeader(Activity activity, LinearLayout root,
                              String titleText, String subtitleText) {
        TextView title = new TextView(activity);
        title.setText(titleText);
        title.setTextSize(27);
        title.setTextColor(COLOR_TEXT);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.START);
        title.setPadding(0, 0, 0, dp(activity, 7));
        root.addView(title, fullWidth(activity, 0));

        TextView subtitle = new TextView(activity);
        subtitle.setText(subtitleText);
        subtitle.setTextSize(15);
        subtitle.setTextColor(COLOR_SUBTEXT);
        subtitle.setGravity(Gravity.START);
        root.addView(subtitle, fullWidth(activity, 20));
    }

    static void addBackHeader(Activity activity, LinearLayout root, String titleText) {
        LinearLayout header = new LinearLayout(activity);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        Button back = button(activity, "‹");
        back.setTextSize(28);
        back.setGravity(Gravity.CENTER);
        back.setBackground(roundedRipple(
                activity, Color.rgb(242, 242, 244), Color.rgb(242, 242, 244), 24));
        back.setTextColor(COLOR_TEXT);
        back.setElevation(0);
        back.setPadding(0, 0, 0, 0);
        back.setMinHeight(dp(activity, 44));
        back.setMinWidth(dp(activity, 44));
        back.setOnClickListener(v -> activity.finish());
        header.addView(back, new LinearLayout.LayoutParams(
                dp(activity, 44), dp(activity, 44)));

        TextView title = new TextView(activity);
        title.setText(titleText);
        title.setTextSize(24);
        title.setTextColor(COLOR_TEXT);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        titleParams.leftMargin = dp(activity, 14);
        header.addView(title, titleParams);
        root.addView(header, fullWidth(activity, 24));
    }

    static TextView sectionTitle(Activity activity, String text) {
        TextView view = new TextView(activity);
        view.setText(text);
        view.setTextSize(19);
        view.setTextColor(COLOR_TEXT);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    static Button button(Activity activity, String text) {
        Button button = new Button(activity);
        button.setText(text);
        button.setTextSize(17);
        button.setTextColor(COLOR_TEXT);
        button.setAllCaps(false);
        button.setMinHeight(dp(activity, 52));
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(activity, 16), dp(activity, 10),
                dp(activity, 16), dp(activity, 10));
        button.setBackground(roundedRipple(
                activity, Color.rgb(244, 244, 245), Color.rgb(244, 244, 245), 28));
        button.setElevation(0);
        button.setStateListAnimator(null);
        return button;
    }

    static Button primaryButton(Activity activity, String text) {
        Button button = button(activity, text);
        button.setTextColor(Color.WHITE);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setBackground(roundedRipple(
                activity, COLOR_PRIMARY, COLOR_PRIMARY, 28));
        button.setElevation(0);
        return button;
    }

    static Button dangerButton(Activity activity, String text) {
        Button button = button(activity, text);
        button.setTextColor(Color.rgb(196, 55, 65));
        button.setBackground(roundedRipple(
                activity, Color.rgb(255, 242, 243), Color.rgb(255, 242, 243), 24));
        return button;
    }

    static LinearLayout group(Activity activity) {
        LinearLayout group = new LinearLayout(activity);
        group.setOrientation(LinearLayout.VERTICAL);
        group.setBackground(roundedDrawable(activity, COLOR_SURFACE, COLOR_BORDER, 24));
        group.setClipToOutline(true);
        return group;
    }

    static NavigationRow navigationRow(Activity activity, String title) {
        return new NavigationRow(activity, title);
    }

    static ToggleRow toggleRow(Activity activity, String title) {
        return new ToggleRow(activity, title);
    }

    static void addDivider(Activity activity, LinearLayout group) {
        View divider = new View(activity);
        divider.setBackgroundColor(Color.rgb(236, 236, 238));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(activity, 1));
        params.leftMargin = dp(activity, 18);
        params.rightMargin = dp(activity, 16);
        group.addView(divider, params);
    }

    static final class NavigationRow extends LinearLayout {
        private final TextView subtitleView;

        NavigationRow(Activity activity, String titleText) {
            super(activity);
            setOrientation(HORIZONTAL);
            setGravity(Gravity.CENTER_VERTICAL);
            setPadding(dp(activity, 16), dp(activity, 14),
                    dp(activity, 14), dp(activity, 14));
            setMinimumHeight(dp(activity, 84));
            setClickable(true);
            setFocusable(true);
            setBackground(new RippleDrawable(
                    ColorStateList.valueOf(Color.argb(24, 24, 91, 255)),
                    new ColorDrawable(Color.TRANSPARENT), null));

            LinearLayout labels = new LinearLayout(activity);
            labels.setOrientation(VERTICAL);
            LinearLayout.LayoutParams labelsParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);

            TextView title = new TextView(activity);
            title.setText(titleText);
            title.setTextSize(17);
            title.setTextColor(COLOR_TEXT);
            title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            labels.addView(title, new LinearLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

            subtitleView = new TextView(activity);
            subtitleView.setTextSize(14);
            subtitleView.setTextColor(COLOR_SUBTEXT);
            subtitleView.setPadding(0, dp(activity, 3), 0, 0);
            subtitleView.setMaxLines(3);
            subtitleView.setLineSpacing(0, 1.08f);
            labels.addView(subtitleView, new LinearLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            addView(labels, labelsParams);

            TextView chevron = new TextView(activity);
            chevron.setText("›");
            chevron.setTextSize(28);
            chevron.setTextColor(Color.rgb(176, 176, 182));
            chevron.setGravity(Gravity.CENTER);
            addView(chevron, new LinearLayout.LayoutParams(
                    dp(activity, 28), LayoutParams.MATCH_PARENT));
        }

        void setSummary(String summary) {
            subtitleView.setText(summary);
        }
    }

    static final class ToggleRow extends LinearLayout {
        private final TextView subtitleView;
        private final Switch switchView;

        ToggleRow(Activity activity, String titleText) {
            super(activity);
            setOrientation(HORIZONTAL);
            setGravity(Gravity.CENTER_VERTICAL);
            setPadding(dp(activity, 16), dp(activity, 14),
                    dp(activity, 14), dp(activity, 14));
            setMinimumHeight(dp(activity, 82));

            LinearLayout labels = new LinearLayout(activity);
            labels.setOrientation(VERTICAL);
            LinearLayout.LayoutParams labelsParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);

            TextView title = new TextView(activity);
            title.setText(titleText);
            title.setTextSize(17);
            title.setTextColor(COLOR_TEXT);
            title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            labels.addView(title, new LinearLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

            subtitleView = new TextView(activity);
            subtitleView.setTextSize(14);
            subtitleView.setTextColor(COLOR_SUBTEXT);
            subtitleView.setPadding(0, dp(activity, 3), dp(activity, 12), 0);
            labels.addView(subtitleView, new LinearLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            addView(labels, labelsParams);

            int[][] states = new int[][]{
                    new int[]{android.R.attr.state_checked},
                    new int[]{}
            };
            switchView = new Switch(activity);
            switchView.setShowText(false);
            switchView.setThumbTintList(new ColorStateList(
                    states, new int[]{COLOR_ACCENT, Color.rgb(182, 182, 188)}));
            switchView.setTrackTintList(new ColorStateList(
                    states, new int[]{Color.rgb(162, 188, 255), Color.rgb(222, 222, 226)}));
            addView(switchView, new LinearLayout.LayoutParams(
                    LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

            setOnClickListener(v -> switchView.toggle());
        }

        void setSummary(String summary) {
            subtitleView.setText(summary);
        }

        void setChecked(boolean checked) {
            switchView.setChecked(checked);
        }

        void setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener listener) {
            switchView.setOnCheckedChangeListener(listener);
        }
    }

    static void applyCard(Activity activity, View view) {
        view.setBackground(roundedDrawable(
                activity, Color.rgb(244, 244, 245), Color.rgb(244, 244, 245), 18));
        view.setElevation(0);
    }

    static void applyStatusPanel(Activity activity, View view, boolean success) {
        int fill = success ? Color.rgb(239, 244, 255) : Color.rgb(255, 246, 228);
        int border = success ? Color.rgb(219, 229, 255) : Color.rgb(255, 226, 174);
        view.setBackground(roundedDrawable(activity, fill, border, 20));
    }

    static void applyPausedStatusPanel(Activity activity, View view) {
        view.setBackground(roundedDrawable(
                activity, Color.rgb(244, 244, 246), Color.rgb(228, 228, 232), 20));
    }

    private static RippleDrawable roundedRipple(Activity activity, int fill,
                                                 int stroke, int radiusDp) {
        return new RippleDrawable(
                ColorStateList.valueOf(Color.argb(28, 24, 91, 255)),
                roundedDrawable(activity, fill, stroke, radiusDp),
                null);
    }

    private static GradientDrawable roundedDrawable(Activity activity, int fill,
                                                     int stroke, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(activity, radiusDp));
        drawable.setStroke(dp(activity, 1), stroke);
        return drawable;
    }

    private static LinearLayout.LayoutParams fullWidth(int bottomPx) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = bottomPx;
        return params;
    }

    static LinearLayout.LayoutParams fullWidth(Activity activity, int bottomDp) {
        return fullWidth(dp(activity, bottomDp));
    }

    static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
