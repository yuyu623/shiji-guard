package com.shiji.guard;

import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.LinkedHashSet;

public class SettingsScreen extends Screen {

    private boolean built;
    private final TextView[] permStatus = new TextView[5];
    private final TextView[] permAction = new TextView[5];
    private EditText urlField;
    private EditText modelField;
    private EditText keyField;
    private TextView apiStatus;
    private Toggle trackerSwitch;
    private Toggle.Listener trackerListener;
    private TextView runnerStatus;
    private boolean testing;

    private LinearLayout styleGrid;
    private LinearLayout intensityGrid;
    private LinearLayout focusGrid;
    private LinearLayout lengthGrid;
    private TextView intensityValue;
    private TextView evalStatus;
    private EditText customField;
    private String styleId = AiStyle.ID_DIRECT;
    private int styleIntensity = 4;
    private int styleLength = 1;
    private final LinkedHashSet<String> focusSet = new LinkedHashSet<>();

    public SettingsScreen(MainActivity a) {
        super(a);
    }

    @Override
    public void refresh() {
        if (!built) {
            build();
            built = true;
        }
        updatePermissions();
        if (runnerStatus != null) {
            boolean running = Engine.running();
            runnerStatus.setText(running ? "运行中" : "已停止");
            runnerStatus.setTextColor(running ? Ui.ACCENT : Ui.DANGER);
        }
        if (trackerSwitch != null) {
            trackerSwitch.setListener(null);
            trackerSwitch.setChecked(App.get().prefs().trackerEnabled());
            trackerSwitch.setListener(trackerListener);
        }
    }

    private void build() {
        Prefs prefs = App.get().prefs();

        add(Ui.label(ctx, "设置"));
        add(Ui.space(ctx, 30));

        add(Ui.section(ctx, "外观"));
        add(Ui.space(ctx, 12));
        LinearLayout look = card();
        LinearLayout themeRow = Ui.row(ctx);
        String[] themeNames = {"跟随系统", "浅色", "深色"};
        int mode = prefs.themeMode();
        for (int i = 0; i < themeNames.length; i++) {
            final int value = i;
            TextView chip = Ui.chip(ctx, themeNames[i], mode == value);
            chip.setOnClickListener(v -> chooseTheme(value));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMargins(Ui.dp(ctx, 3), 0, Ui.dp(ctx, 3), 0);
            themeRow.addView(chip, lp);
        }
        look.addView(themeRow);
        look.addView(Ui.space(ctx, 12));
        look.addView(Ui.tv(ctx, "跟随系统会随手机的深色模式自动切换。", 11, Ui.FAINT));
        add(look);
        add(Ui.space(ctx, 32));

        add(Ui.section(ctx, "AI 评价"));
        add(Ui.space(ctx, 12));
        LinearLayout eval = card();

        TextView evalHint = Ui.tv(ctx, "决定 AI 怎么写；只影响点评文字，不影响统计和限额。", 11.5f, Ui.FAINT);
        evalHint.setLineSpacing(Ui.dp(ctx, 6), 1.2f);
        eval.addView(evalHint);
        eval.addView(Ui.space(ctx, 18));

        styleId = prefs.aiStyle();
        if (AiStyle.indexOf(styleId) < 0) styleId = AiStyle.ID_DIRECT;
        styleIntensity = prefs.aiIntensity();
        styleLength = prefs.aiLength();
        focusSet.clear();
        focusSet.addAll(AiStyle.parseFocus(prefs.aiFocus()));

        eval.addView(Ui.tv(ctx, "风格", 10.5f, Ui.DIM));
        eval.addView(Ui.space(ctx, 8));
        styleGrid = Ui.col(ctx);
        eval.addView(styleGrid);
        renderStyles();

        eval.addView(Ui.space(ctx, 18));
        LinearLayout intHead = Ui.row(ctx);
        intHead.addView(Ui.tv(ctx, "强度", 10.5f, Ui.DIM));
        intHead.addView(spacer());
        intensityValue = Ui.tv(ctx, AiStyle.intensityLabel(styleIntensity), 10.5f, Ui.FAINT);
        intHead.addView(intensityValue);
        eval.addView(intHead);
        eval.addView(Ui.space(ctx, 8));
        intensityGrid = Ui.col(ctx);
        eval.addView(intensityGrid);
        renderIntensity();
        LinearLayout intScale = Ui.row(ctx);
        intScale.addView(Ui.tv(ctx, "温和", 10, Ui.FAINT));
        intScale.addView(spacer());
        intScale.addView(Ui.tv(ctx, "很狠", 10, Ui.FAINT));
        eval.addView(intScale);

        eval.addView(Ui.space(ctx, 18));
        LinearLayout focusHead = Ui.row(ctx);
        focusHead.addView(Ui.tv(ctx, "关注点", 10.5f, Ui.DIM));
        focusHead.addView(spacer());
        focusHead.addView(Ui.tv(ctx, "可多选", 10, Ui.FAINT));
        eval.addView(focusHead);
        eval.addView(Ui.space(ctx, 8));
        focusGrid = Ui.col(ctx);
        eval.addView(focusGrid);
        renderFocus();

        eval.addView(Ui.space(ctx, 18));
        LinearLayout lenHead = Ui.row(ctx);
        lenHead.addView(Ui.tv(ctx, "长度", 10.5f, Ui.DIM));
        lenHead.addView(spacer());
        lenHead.addView(Ui.tv(ctx, "高级", 10, Ui.FAINT));
        eval.addView(lenHead);
        eval.addView(Ui.space(ctx, 8));
        lengthGrid = Ui.col(ctx);
        eval.addView(lengthGrid);
        renderLength();

        eval.addView(Ui.space(ctx, 18));
        eval.addView(Ui.tv(ctx, "自定义风格（选择「自定义」时生效）", 10.5f, Ui.DIM));
        eval.addView(Ui.space(ctx, 7));
        customField = new EditText(ctx);
        customField.setText(prefs.aiCustom());
        customField.setHint("例如：像损友，先损我两句，再告诉我今晚该关哪个 App");
        customField.setTextSize(13);
        customField.setTextColor(Ui.TEXT);
        customField.setHintTextColor(Ui.FAINT);
        customField.setPadding(Ui.dp(ctx, 14), Ui.dp(ctx, 12), Ui.dp(ctx, 14), Ui.dp(ctx, 12));
        customField.setBackground(Ui.rounded(Ui.FIELD, Ui.dp(ctx, 12)));
        customField.setGravity(Gravity.TOP | Gravity.START);
        customField.setMinLines(2);
        customField.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        customField.setFilters(new InputFilter[]{new InputFilter.LengthFilter(160)});
        eval.addView(customField, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        customField.setAlpha(AiStyle.ID_CUSTOM.equals(styleId) ? 1f : 0.55f);
        customField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int st, int cnt, int aft) {
            }

            @Override
            public void onTextChanged(CharSequence s, int st, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String v = s.toString().trim();
                App.get().prefs().setAiCustom(v);
                if (AiStyle.ID_CUSTOM.equals(styleId)) {
                    AiStyle.markChanged(App.get().prefs(), App.get().repo());
                    if (evalStatus != null) {
                        evalStatus.setText(v.isEmpty()
                                ? "自定义风格还没填写，填写后返回点评会自动重写"
                                : "自定义风格已保存 · 返回点评会自动重写");
                        evalStatus.setTextColor(v.isEmpty() ? Ui.DANGER : Ui.ACCENT);
                    }
                }
            }
        });

        eval.addView(Ui.space(ctx, 14));
        evalStatus = Ui.tv(ctx, "切换后 24 小时内打开点评，会自动按新风格重写。", 10.5f, Ui.FAINT);
        eval.addView(evalStatus);
        add(eval);
        add(Ui.space(ctx, 32));

        add(Ui.section(ctx, "权限"));
        add(Ui.space(ctx, 12));
        LinearLayout perms = Ui.col(ctx);
        perms.setBackground(Ui.rounded(Ui.SURFACE, Ui.dp(ctx, 18)));
        perms.setPadding(Ui.dp(ctx, 4), Ui.dp(ctx, 4), Ui.dp(ctx, 4), Ui.dp(ctx, 4));
        perms.addView(permissionRow("使用情况访问", 0, () -> Permissions.openUsage(act)));
        perms.addView(Ui.hline(ctx, Ui.LINE));
        perms.addView(permissionRow("无障碍服务", 1, () -> Permissions.openAccessibility(act)));
        perms.addView(Ui.hline(ctx, Ui.LINE));
        perms.addView(permissionRow("通知", 2, () -> Permissions.openNotifications(act)));
        perms.addView(Ui.hline(ctx, Ui.LINE));
        perms.addView(permissionRow("媒体会话（后台音频）", 3, () -> Permissions.openNotificationAccess(act)));
        perms.addView(Ui.hline(ctx, Ui.LINE));
        perms.addView(permissionRow("电池优化", 4, () -> Permissions.openBattery(act)));
        add(perms);

        add(Ui.space(ctx, 32));
        add(Ui.section(ctx, "AI 接口"));
        add(Ui.space(ctx, 12));
        LinearLayout ai = card();

        TextView hint = Ui.tv(ctx, "支持 DeepSeek 以及 OpenAI 兼容的 /chat/completions 接口。只有生成点评时（含切换风格后的自动重写）才会上传汇总数据。",
                11.5f, Ui.FAINT);
        hint.setLineSpacing(Ui.dp(ctx, 6), 1.2f);
        ai.addView(hint);

        urlField = field(ai, "接口地址", prefs.baseUrl(),
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        modelField = field(ai, "模型", prefs.model(), InputType.TYPE_CLASS_TEXT);
        keyField = field(ai, "API Key", prefs.apiKey(),
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        ai.addView(Ui.space(ctx, 20));
        LinearLayout buttons = Ui.row(ctx);
        TextView save = Ui.button(ctx, "保存配置", true);
        TextView test = Ui.button(ctx, "测试连接", false);
        Ui.click(save, this::saveSettings);
        Ui.click(test, this::testApi);
        buttons.addView(save, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        View gap = new View(ctx);
        buttons.addView(gap, new LinearLayout.LayoutParams(Ui.dp(ctx, 10), 1));
        buttons.addView(test, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        ai.addView(buttons);

        TextView reset = Ui.tv(ctx, "恢复默认接口（DeepSeek · 清空 Key）", 11, Ui.FAINT);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rlp.topMargin = Ui.dp(ctx, 14);
        Ui.click(reset, () -> {
            Prefs p = App.get().prefs();
            p.setBaseUrl("https://api.deepseek.com");
            p.setModel("deepseek-chat");
            p.setApiKey("");
            urlField.setText(p.baseUrl());
            modelField.setText(p.model());
            keyField.setText("");
            apiStatus.setTextColor(Ui.DIM);
            apiStatus.setText("已恢复默认接口，请填写你的 API Key");
        });
        ai.addView(reset, rlp);

        apiStatus = Ui.tv(ctx, prefs.apiKey().trim().isEmpty() ? "尚未填写 API Key" : "已配置 Key",
                11, prefs.apiKey().trim().isEmpty() ? Ui.FAINT : Ui.DIM);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.topMargin = Ui.dp(ctx, 12);
        ai.addView(apiStatus, slp);
        add(ai);

        add(Ui.space(ctx, 32));
        add(Ui.section(ctx, "运行"));
        add(Ui.space(ctx, 12));
        LinearLayout run = card();
        LinearLayout trackerRow = Ui.row(ctx);
        LinearLayout trackerCol = Ui.col(ctx);
        trackerCol.addView(Ui.tv(ctx, "持续记录", 13.5f, Ui.TEXT));
        trackerCol.addView(Ui.space(ctx, 5));
        trackerCol.addView(Ui.tv(ctx, "后台每 10 秒同步一次使用记录", 11, Ui.FAINT));
        trackerRow.addView(trackerCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        trackerSwitch = new Toggle(ctx);
        trackerListener = checked -> {
            App.get().prefs().setTrackerEnabled(checked);
            if (checked) TrackerService.start(act);
            else TrackerService.stop(act);
            if (runnerStatus != null) {
                runnerStatus.setText(checked ? "启动中" : "已停止");
                runnerStatus.setTextColor(checked ? Ui.ACCENT : Ui.DANGER);
            }
        };
        trackerSwitch.setListener(trackerListener);
        trackerSwitch.setChecked(prefs.trackerEnabled());
        trackerRow.addView(trackerSwitch);
        run.addView(trackerRow);

        run.addView(Ui.space(ctx, 16));
        run.addView(Ui.hline(ctx, Ui.LINE));
        run.addView(Ui.space(ctx, 14));

        LinearLayout serviceRow = Ui.row(ctx);
        serviceRow.addView(Ui.tv(ctx, "记录服务", 12.5f, Ui.DIM));
        serviceRow.addView(spacer());
        runnerStatus = Ui.tv(ctx, Engine.running() ? "运行中" : "已停止", 11.5f, Ui.ACCENT);
        serviceRow.addView(runnerStatus);
        TextView restart = Ui.tv(ctx, "重启", 11.5f, Ui.TEXT);
        restart.setPadding(Ui.dp(ctx, 14), 0, 0, 0);
        Ui.click(restart, () -> {
            TrackerService.stop(act);
            root.postDelayed(() -> TrackerService.start(act), 600);
            Toast.makeText(ctx, "正在重启记录服务", Toast.LENGTH_SHORT).show();
        });
        serviceRow.addView(restart);
        run.addView(serviceRow);

        run.addView(Ui.space(ctx, 14));
        run.addView(Ui.hline(ctx, Ui.LINE));
        run.addView(Ui.space(ctx, 14));
        LinearLayout bootRow = Ui.row(ctx);
        bootRow.addView(Ui.tv(ctx, "自启动 / 后台运行", 12.5f, Ui.DIM));
        bootRow.addView(spacer());
        TextView bootAction = Ui.tv(ctx, "去设置", 11.5f, Ui.TEXT);
        Ui.click(bootAction, () -> Permissions.openAutostart(act));
        bootRow.addView(bootAction);
        Ui.click(bootRow, () -> Permissions.openAutostart(act));
        run.addView(bootRow);

        run.addView(Ui.space(ctx, 14));
        TextView note = Ui.tv(ctx, "HyperOS 请在「应用信息 → 省电策略」选择「无限制」，并允许自启动。否则锁屏后系统可能清理后台，统计会出现空档。",
                11, Ui.FAINT);
        note.setLineSpacing(Ui.dp(ctx, 6), 1.2f);
        run.addView(note);
        add(run);

        add(Ui.space(ctx, 32));
        add(Ui.section(ctx, "数据"));
        add(Ui.space(ctx, 12));
        LinearLayout data = card();
        LinearLayout clearAi = Ui.row(ctx);
        clearAi.setPadding(0, Ui.dp(ctx, 2), 0, Ui.dp(ctx, 2));
        clearAi.addView(Ui.tv(ctx, "清空 AI 点评", 13, Ui.DIM));
        clearAi.addView(spacer());
        clearAi.addView(Ui.tv(ctx, "重新生成", 11, Ui.FAINT));
        Ui.click(clearAi, () -> Dialogs.confirm(ctx, "清空 AI 点评",
                "将删除已缓存的日/周/月点评，下次打开时可以重新生成。",
                () -> {
                    App.get().repo().clearAi();
                    Toast.makeText(ctx, "已清空", Toast.LENGTH_SHORT).show();
                    act.refreshNow();
                }));
        data.addView(clearAi);
        data.addView(Ui.space(ctx, 14));
        data.addView(Ui.hline(ctx, Ui.LINE));
        data.addView(Ui.space(ctx, 14));
        LinearLayout clearBlocks = Ui.row(ctx);
        clearBlocks.setPadding(0, Ui.dp(ctx, 2), 0, Ui.dp(ctx, 2));
        clearBlocks.addView(Ui.tv(ctx, "清空拦截记录", 13, Ui.DIM));
        clearBlocks.addView(spacer());
        clearBlocks.addView(Ui.tv(ctx, "仅次数", 11, Ui.FAINT));
        Ui.click(clearBlocks, () -> Dialogs.confirm(ctx, "清空拦截记录",
                "只清除「今天被拦截 N 次」的计数，应用使用时长不受影响。",
                () -> {
                    App.get().repo().clearBlocks();
                    Toast.makeText(ctx, "已清空", Toast.LENGTH_SHORT).show();
                    act.refreshNow();
                }));
        data.addView(clearBlocks);
        data.addView(Ui.space(ctx, 14));
        data.addView(Ui.hline(ctx, Ui.LINE));
        data.addView(Ui.space(ctx, 14));
        LinearLayout clearRow = Ui.row(ctx);
        clearRow.setPadding(0, Ui.dp(ctx, 2), 0, Ui.dp(ctx, 2));
        clearRow.addView(Ui.tv(ctx, "清空使用记录", 13, Ui.DANGER));
        clearRow.addView(spacer());
        clearRow.addView(Ui.tv(ctx, "不可恢复", 11, Ui.FAINT));
        Ui.click(clearRow, () -> Dialogs.confirm(ctx, "清空全部记录",
                "将删除本机保存的所有应用时长、拦截记录与 AI 点评，限额规则会保留。",
                () -> {
                    App.get().repo().clearStats();
                    Toast.makeText(ctx, "已清空", Toast.LENGTH_SHORT).show();
                    act.refreshNow();
                }));
        data.addView(clearRow);
        add(data);

        add(Ui.space(ctx, 34));
        add(Ui.section(ctx, "关于"));
        add(Ui.space(ctx, 12));
        TextView about = Ui.tv(ctx, "时迹 1.3\n统计与限额都在本机完成。无障碍权限只用于识别前台应用，不读取屏幕内容；媒体会话权限只用于识别后台播放，不读取通知内容。",
                12, Ui.FAINT);
        about.setLineSpacing(Ui.dp(ctx, 7), 1.25f);
        add(about);
        add(Ui.space(ctx, 28));
        add(Ui.tv(ctx, "为专注而做 · 轻量、离线优先", 10.5f, Ui.FAINT));
    }

    private LinearLayout permissionRow(String name, int idx, Runnable action) {
        LinearLayout row = Ui.row(ctx);
        row.setPadding(Ui.dp(ctx, 14), Ui.dp(ctx, 13), Ui.dp(ctx, 14), Ui.dp(ctx, 13));
        LinearLayout mid = Ui.col(ctx);
        mid.addView(Ui.tv(ctx, name, 13, Ui.TEXT));
        TextView status = Ui.tv(ctx, "检查中…", 11, Ui.FAINT);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.topMargin = Ui.dp(ctx, 5);
        mid.addView(status, slp);
        row.addView(mid, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView act = Ui.tv(ctx, "设置", 11, Ui.DIM);
        act.setPadding(Ui.dp(ctx, 10), Ui.dp(ctx, 6), 0, Ui.dp(ctx, 6));
        row.addView(act);
        Ui.click(row, action);
        permStatus[idx] = status;
        permAction[idx] = act;
        return row;
    }

    private void updatePermissions() {
        setPerm(0, Permissions.usage(ctx), "已授权", "未授权 · 无法统计");
        setPerm(1, Permissions.accessibility(ctx), "已开启", "未开启 · 限额不生效");
        setPerm(2, Permissions.notifications(ctx), "已允许", "未允许 · 通知不可见");
        setPerm(3, Permissions.notificationAccess(ctx), "已开启", "未开启 · 后台音频不统计");
        setPerm(4, Permissions.battery(ctx), "已忽略电池优化", "未加入白名单");
    }

    private void setPerm(int idx, boolean ok, String yes, String no) {
        if (permStatus[idx] == null) return;
        permStatus[idx].setText(ok ? yes : no);
        permStatus[idx].setTextColor(ok ? Ui.OK : Ui.DANGER);
        if (permAction[idx] != null) {
            permAction[idx].setText(ok ? "查看" : "去开启");
            permAction[idx].setTextColor(ok ? Ui.FAINT : Ui.TEXT);
        }
    }

    private EditText field(LinearLayout parent, String label, String value, int inputType) {
        parent.addView(Ui.space(ctx, 16));
        parent.addView(Ui.tv(ctx, label, 10.5f, Ui.DIM));
        parent.addView(Ui.space(ctx, 7));
        EditText et = new EditText(ctx);
        et.setText(value);
        et.setTextSize(13);
        et.setTextColor(Ui.TEXT);
        et.setHintTextColor(Ui.FAINT);
        et.setSingleLine(true);
        et.setInputType(inputType);
        et.setPadding(Ui.dp(ctx, 14), Ui.dp(ctx, 12), Ui.dp(ctx, 14), Ui.dp(ctx, 12));
        et.setBackground(Ui.rounded(Ui.FIELD, Ui.dp(ctx, 12)));
        parent.addView(et, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return et;
    }

    private void renderStyles() {
        if (styleGrid == null) return;
        styleGrid.removeAllViews();
        for (int start = 0; start < AiStyle.NAMES.length; start += 3) {
            LinearLayout row = Ui.row(ctx);
            for (int j = 0; j < 3; j++) {
                int idx = start + j;
                if (idx >= AiStyle.NAMES.length) {
                    row.addView(new View(ctx), chipLp());
                    continue;
                }
                final String id = AiStyle.IDS[idx];
                TextView chip = Ui.chip(ctx, AiStyle.NAMES[idx], id.equals(styleId));
                Ui.click(chip, () -> selectStyle(id));
                row.addView(chip, chipLp());
            }
            styleGrid.addView(row);
        }
    }

    private LinearLayout.LayoutParams chipLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(Ui.dp(ctx, 4), Ui.dp(ctx, 4), Ui.dp(ctx, 4), Ui.dp(ctx, 4));
        return lp;
    }

    private void selectStyle(String id) {
        if (id.equals(styleId)) return;
        styleId = id;
        App.get().prefs().setAiStyle(id);
        AiStyle.markChanged(App.get().prefs(), App.get().repo());
        renderStyles();
        if (customField != null) {
            customField.setAlpha(AiStyle.ID_CUSTOM.equals(id) ? 1f : 0.55f);
        }
        if (AiStyle.ID_CUSTOM.equals(id) && customField != null
                && customField.getText().toString().trim().isEmpty()) {
            evalStatus.setText("自定义风格还没填写，填写后返回点评会自动重写");
            evalStatus.setTextColor(Ui.DANGER);
        } else {
            evalStatus.setText("已切换为「" + AiStyle.name(id) + "」 · 返回点评会自动重写");
            evalStatus.setTextColor(Ui.ACCENT);
        }
    }

    private void renderIntensity() {
        if (intensityGrid == null) return;
        intensityGrid.removeAllViews();
        LinearLayout row = Ui.row(ctx);
        for (int i = 1; i <= 7; i++) {
            final int value = i;
            TextView chip = Ui.chip(ctx, String.valueOf(i), i == styleIntensity);
            Ui.click(chip, () -> selectIntensity(value));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMargins(Ui.dp(ctx, 2), 0, Ui.dp(ctx, 2), 0);
            row.addView(chip, lp);
        }
        intensityGrid.addView(row);
        if (intensityValue != null) intensityValue.setText(AiStyle.intensityLabel(styleIntensity));
    }

    private void selectIntensity(int value) {
        if (value == styleIntensity) return;
        styleIntensity = value;
        App.get().prefs().setAiIntensity(value);
        AiStyle.markChanged(App.get().prefs(), App.get().repo());
        renderIntensity();
        evalStatus.setText("强度已改为 " + AiStyle.intensityLabel(value) + " · 返回点评会自动重写");
        evalStatus.setTextColor(Ui.ACCENT);
    }

    private void renderFocus() {
        if (focusGrid == null) return;
        focusGrid.removeAllViews();
        for (int start = 0; start < AiStyle.FOCUS_IDS.length; start += 3) {
            LinearLayout row = Ui.row(ctx);
            for (int j = 0; j < 3; j++) {
                int idx = start + j;
                if (idx >= AiStyle.FOCUS_IDS.length) {
                    row.addView(new View(ctx), chipLp());
                    continue;
                }
                final String id = AiStyle.FOCUS_IDS[idx];
                TextView chip = Ui.chip(ctx, AiStyle.FOCUS_NAMES[idx], focusSet.contains(id));
                Ui.click(chip, () -> toggleFocus(id));
                row.addView(chip, chipLp());
            }
            focusGrid.addView(row);
        }
    }

    private void toggleFocus(String id) {
        if (!focusSet.remove(id)) focusSet.add(id);
        App.get().prefs().setAiFocus(AiStyle.focusCsv(focusSet));
        AiStyle.markChanged(App.get().prefs(), App.get().repo());
        renderFocus();
        evalStatus.setText("关注点已更新 · 返回点评会自动重写");
        evalStatus.setTextColor(Ui.ACCENT);
    }

    private void renderLength() {
        if (lengthGrid == null) return;
        lengthGrid.removeAllViews();
        LinearLayout row = Ui.row(ctx);
        for (int i = 0; i < AiStyle.LENGTH_NAMES.length; i++) {
            final int value = i;
            TextView chip = Ui.chip(ctx, AiStyle.LENGTH_NAMES[i], i == styleLength);
            Ui.click(chip, () -> selectLength(value));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMargins(Ui.dp(ctx, 4), 0, Ui.dp(ctx, 4), 0);
            row.addView(chip, lp);
        }
        lengthGrid.addView(row);
    }

    private void selectLength(int value) {
        if (value == styleLength) return;
        styleLength = value;
        App.get().prefs().setAiLength(value);
        AiStyle.markChanged(App.get().prefs(), App.get().repo());
        renderLength();
        evalStatus.setText("长度已改为「" + AiStyle.LENGTH_NAMES[value] + "」 · 返回点评会自动重写");
        evalStatus.setTextColor(Ui.ACCENT);
    }

    private void chooseTheme(int mode) {
        if (App.get().prefs().themeMode() == mode) return;
        App.get().prefs().setThemeMode(mode);
        Ui.applyTheme(ctx);
        act.recreate();
    }

    private void saveSettings() {
        Prefs p = App.get().prefs();
        p.setBaseUrl(urlField.getText().toString().trim());
        p.setModel(modelField.getText().toString().trim());
        p.setApiKey(keyField.getText().toString().trim());
        apiStatus.setTextColor(Ui.ACCENT);
        apiStatus.setText("已保存");
    }

    private void testApi() {
        if (testing) return;
        saveSettings();
        testing = true;
        apiStatus.setTextColor(Ui.DIM);
        apiStatus.setText("测试中…");
        AiHelper.test(new AiHelper.Callback() {
            @Override
            public void onText(String text) {
                testing = false;
                apiStatus.setTextColor(Ui.ACCENT);
                apiStatus.setText("连接成功 · " + (text.length() > 24 ? text.substring(0, 24) : text));
            }

            @Override
            public void onError(String message) {
                testing = false;
                apiStatus.setTextColor(Ui.DANGER);
                apiStatus.setText("失败 · " + message);
            }
        });
    }

    private LinearLayout card() {
        LinearLayout card = Ui.col(ctx);
        card.setBackground(Ui.rounded(Ui.SURFACE, Ui.dp(ctx, 18)));
        card.setPadding(Ui.dp(ctx, 16), Ui.dp(ctx, 16), Ui.dp(ctx, 16), Ui.dp(ctx, 16));
        return card;
    }

    private View spacer() {
        View v = new View(ctx);
        v.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1f));
        return v;
    }
}
