package app.babylog.nativeapp;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CAPTURE_ULTRASOUND = 1201;
    private static final int REQUEST_PICK_ULTRASOUND = 1202;
    private static final int REQUEST_EXPORT_BACKUP = 1203;
    private static final int REQUEST_IMPORT_BACKUP = 1204;
    private static final int REQUEST_PERMISSION_CAMERA = 2201;
    private static final int REQUEST_PERMISSION_IMAGES = 2202;
    private static final String META_PREFS_NAME = "babylog_native_meta_v1";
    private static final String LAST_BACKUP_EXPORT_MS = "lastBackupExportMs";
    private static final String STATE_ACTIVE_TAB = "activeTab";
    private static final String STATE_TIMELINE_FILTER = "timelineFilter";
    private static final String STATE_OPEN_FORM = "openForm";
    private static final String STATE_BABY_CARE_EVENT = "babyCareEvent";
    private static final String STATE_PRIMARY = "primary";
    private static final String STATE_SECONDARY = "secondary";
    private static final String STATE_TERTIARY = "tertiary";
    private static final String STATE_NOTE = "note";
    private static final String STATE_EXAM_DATE = "examDate";
    private static final String STATE_GESTATIONAL_AGE = "gestationalAge";
    private static final String STATE_BPD = "bpd";
    private static final String STATE_HC = "hc";
    private static final String STATE_AC = "ac";
    private static final String STATE_FL = "fl";
    private static final String STATE_EFW = "efw";
    private static final String STATE_PHOTO_PATH = "photoPath";
    private static final String STATE_PHOTO_NAME = "photoName";
    private static final String STATE_RANGE_CONFIRMED = "rangeConfirmed";
    private static final String FORM_BABY_CARE = "babyCare";
    private static final String FORM_ULTRASOUND = "ultrasound";

    private int BG;
    private int SURFACE;
    private int SURFACE_2;
    private int BORDER;
    private int INK;
    private int MUTED;
    private int TEXT_3;
    private int PRIMARY;
    private int PRIMARY_SOFT;
    private int ACCENT;
    private int ACCENT_SOFT;
    private int ROSE;
    private int BLUE;
    private int VIOLET;
    private int GREEN;
    private int YELLOW;
    private int PEACH;
    private int DANGER;

    private BabyLogRepository repository;
    private BabyLogService service;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private String activeTab = "home";
    private String selectedTimelineFilter = "all";
    private BabyLogService.DashboardSnapshot dashboardSnapshot;
    private List<BabyLogDomain.BabyLogEvent> timelineEvents;
    private List<BabyLogDomain.AttachmentRecord> libraryAttachments;
    private boolean dashboardLoading;
    private boolean timelineLoading;
    private boolean attachmentsLoading;
    private int dataVersion;
    private String pendingBackupJson;
    private String currentPhotoPath = "";
    private String currentPhotoName = "";
    private File pendingCameraFile;
    private Uri pendingCameraUri;
    private boolean ultrasoundRangeWarningConfirmed;
    private ImageView photoPreview;
    private EditText examDateInput;
    private EditText gestationalAgeInput;
    private EditText bpdInput;
    private EditText hcInput;
    private EditText acInput;
    private EditText flInput;
    private EditText efwInput;
    private EditText syncUrlInput;
    private EditText syncRegionInput;
    private AlertDialog activeBabyCareDialog;
    private String activeBabyCareEventType = "";
    private EditText babyCarePrimaryInput;
    private EditText babyCareSecondaryInput;
    private EditText babyCareTertiaryInput;
    private EditText babyCareNoteInput;
    private AlertDialog activeUltrasoundDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadColors();
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        repository = new BabyLogRepository(this);
        service = new BabyLogService(this, repository);
        if (savedInstanceState != null) {
            activeTab = savedInstanceState.getString(STATE_ACTIVE_TAB, "home");
            selectedTimelineFilter = savedInstanceState.getString(STATE_TIMELINE_FILTER, "all");
            currentPhotoPath = savedInstanceState.getString(STATE_PHOTO_PATH, "");
            currentPhotoName = savedInstanceState.getString(STATE_PHOTO_NAME, "");
        }
        render();
        restoreOpenForm(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_ACTIVE_TAB, activeTab);
        outState.putString(STATE_TIMELINE_FILTER, selectedTimelineFilter);
        outState.putString(STATE_PHOTO_PATH, currentPhotoPath);
        outState.putString(STATE_PHOTO_NAME, currentPhotoName);
        if (activeBabyCareDialog != null && activeBabyCareDialog.isShowing()) {
            outState.putString(STATE_OPEN_FORM, FORM_BABY_CARE);
            outState.putString(STATE_BABY_CARE_EVENT, activeBabyCareEventType);
            outState.putString(STATE_PRIMARY, text(babyCarePrimaryInput));
            outState.putString(STATE_SECONDARY, text(babyCareSecondaryInput));
            outState.putString(STATE_TERTIARY, text(babyCareTertiaryInput));
            outState.putString(STATE_NOTE, text(babyCareNoteInput));
        } else if (activeUltrasoundDialog != null && activeUltrasoundDialog.isShowing()) {
            outState.putString(STATE_OPEN_FORM, FORM_ULTRASOUND);
            outState.putString(STATE_EXAM_DATE, text(examDateInput));
            outState.putString(STATE_GESTATIONAL_AGE, text(gestationalAgeInput));
            outState.putString(STATE_BPD, text(bpdInput));
            outState.putString(STATE_HC, text(hcInput));
            outState.putString(STATE_AC, text(acInput));
            outState.putString(STATE_FL, text(flInput));
            outState.putString(STATE_EFW, text(efwInput));
            outState.putBoolean(STATE_RANGE_CONFIRMED, ultrasoundRangeWarningConfirmed);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ioExecutor.shutdownNow();
    }

    private void restoreOpenForm(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }
        String openForm = savedInstanceState.getString(STATE_OPEN_FORM, "");
        if (FORM_BABY_CARE.equals(openForm)) {
            BabyLogService.QuickAction action = findQuickAction(savedInstanceState.getString(STATE_BABY_CARE_EVENT, ""));
            if (action != null) {
                showBabyCareForm(action, savedInstanceState);
            }
        } else if (FORM_ULTRASOUND.equals(openForm)) {
            showUltrasoundForm(savedInstanceState);
        }
    }

    private BabyLogService.QuickAction findQuickAction(String eventType) {
        for (BabyLogService.QuickAction action : quickActions()) {
            if (action.eventType.equals(eventType)) {
                return action;
            }
        }
        return null;
    }

    private void loadColors() {
        BG = color(R.color.bg);
        SURFACE = color(R.color.surface);
        SURFACE_2 = color(R.color.surface_2);
        BORDER = color(R.color.border);
        INK = color(R.color.ink);
        MUTED = color(R.color.muted);
        TEXT_3 = color(R.color.text_3);
        PRIMARY = color(R.color.primary);
        PRIMARY_SOFT = color(R.color.primary_soft);
        ACCENT = color(R.color.accent);
        ACCENT_SOFT = color(R.color.accent_soft);
        ROSE = color(R.color.rose);
        BLUE = color(R.color.blue);
        VIOLET = color(R.color.violet);
        GREEN = color(R.color.green);
        YELLOW = color(R.color.yellow);
        PEACH = color(R.color.peach);
        DANGER = color(R.color.danger);
    }

    private int color(int resId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return getResources().getColor(resId, getTheme());
        }
        return getResources().getColor(resId);
    }

    private void render() {
        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setBackgroundColor(BG);
        shell.addView(header(), matchWrap());

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(14), dp(18), dp(18));
        scroll.addView(content);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
        );
        shell.addView(scroll, scrollParams);

        if ("timeline".equals(activeTab)) {
            renderTimeline(content);
        } else if ("library".equals(activeTab)) {
            renderLibrary(content);
        } else if ("settings".equals(activeTab)) {
            renderSettings(content);
        } else {
            renderHome(content);
        }

        shell.addView(bottomNav(), matchWrap());
        setContentView(shell);
    }

    private void invalidateLoadedData() {
        dataVersion += 1;
        dashboardSnapshot = null;
        timelineEvents = null;
        libraryAttachments = null;
        dashboardLoading = false;
        timelineLoading = false;
        attachmentsLoading = false;
    }

    private void loadDashboardInBackground() {
        if (dashboardLoading) {
            return;
        }
        dashboardLoading = true;
        int version = dataVersion;
        ioExecutor.execute(() -> {
            BabyLogService.DashboardSnapshot snapshot = service.loadDashboard();
            mainHandler.post(() -> {
                if (isActivityGone()) {
                    return;
                }
                dashboardLoading = false;
                if (version == dataVersion) {
                    dashboardSnapshot = snapshot;
                    render();
                }
            });
        });
    }

    private void loadTimelineInBackground() {
        if (timelineLoading) {
            return;
        }
        timelineLoading = true;
        int version = dataVersion;
        ioExecutor.execute(() -> {
            List<BabyLogDomain.BabyLogEvent> events = service.listRecentEvents(50);
            mainHandler.post(() -> {
                if (isActivityGone()) {
                    return;
                }
                timelineLoading = false;
                if (version == dataVersion) {
                    timelineEvents = events;
                    render();
                }
            });
        });
    }

    private void loadAttachmentsInBackground() {
        if (attachmentsLoading) {
            return;
        }
        attachmentsLoading = true;
        int version = dataVersion;
        ioExecutor.execute(() -> {
            List<BabyLogDomain.AttachmentRecord> attachments = service.listAttachmentsNewestFirst();
            mainHandler.post(() -> {
                if (isActivityGone()) {
                    return;
                }
                attachmentsLoading = false;
                if (version == dataVersion) {
                    libraryAttachments = attachments;
                    render();
                }
            });
        });
    }

    private boolean isActivityGone() {
        return isFinishing() || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && isDestroyed());
    }

    private View header() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(18), dp(14), dp(18), dp(12));
        header.setBackgroundColor(0xEEFFFFFF);
        if ("home".equals(activeTab)) {
            header.addView(label("单胎 / 本机模式", 12, MUTED, true));
        }
        header.addView(label(tabTitle(), 24, INK, true));
        return header;
    }

    private void renderHome(LinearLayout content) {
        BabyLogService.DashboardSnapshot dashboard = dashboardSnapshot;
        content.addView(weekPanel(), matchWrapWithBottom(16));
        if (dashboard == null) {
            loadDashboardInBackground();
            content.addView(empty("正在读取本机记录..."), matchWrapWithBottom(16));
            return;
        }
        content.addView(todayPanel(dashboard), matchWrapWithBottom(16));
        content.addView(sectionHeader("最近记录", "全部记录", view -> {
            activeTab = "timeline";
            render();
        }));
        List<BabyLogDomain.BabyLogEvent> recent = dashboard.recentEvents.size() > 3
                ? dashboard.recentEvents.subList(0, 3)
                : dashboard.recentEvents;
        if (recent.isEmpty()) {
            content.addView(empty("暂无本地记录，点 + 开始记录。"), matchWrapWithBottom(16));
        } else {
            for (BabyLogDomain.BabyLogEvent event : recent) {
                content.addView(timelineRow(event), matchWrapWithBottom(10));
            }
        }
        content.addView(sectionHeader("趋势", "点击查看曲线", null));
        LinearLayout trendGrid = new LinearLayout(this);
        trendGrid.setOrientation(LinearLayout.HORIZONTAL);
        trendGrid.addView(trendCard("胎儿 EFW", latestEfwValue(dashboard), latestEfwCaption(dashboard), ROSE), weightWrap(1f));
        trendGrid.addView(space(10, 1));
        trendGrid.addView(trendCard("孕妈体重", "60.4 kg", "较孕前 +8.6 kg", GREEN), weightWrap(1f));
        content.addView(trendGrid, matchWrapWithBottom(16));
    }

    private View weekPanel() {
        LinearLayout panel = panel(20, SURFACE);
        panel.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setGravity(Gravity.CENTER_VERTICAL);
        TextView chip = label("孕期", 12, 0xFF8A6521, true);
        chip.setGravity(Gravity.CENTER);
        chip.setBackground(round(ACCENT_SOFT, 999, 0));
        chip.setPadding(dp(10), dp(5), dp(10), dp(5));
        copy.addView(chip, wrapWrapWithBottom(10));
        copy.addView(label("孕 28+3 周", 32, INK, true));
        copy.addView(label("距预产期 82 天 · 预产期 2026-08-05", 14, MUTED, false));
        panel.addView(copy, weightWrap(1f));
        ImageView mascot = image(R.drawable.star_mascot, 108);
        panel.addView(mascot, wrapWrap());
        return panel;
    }

    private View todayPanel(BabyLogService.DashboardSnapshot dashboard) {
        LinearLayout panel = panel(16, SURFACE);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.addView(sectionHeader("今日", "00:00 起算", null));
        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.HORIZONTAL);
        int total = 0;
        for (int count : dashboard.todayCounts.values()) {
            total += count;
        }
        BabyLogDomain.BabyLogEvent latest = dashboard.recentEvents.isEmpty() ? null : dashboard.recentEvents.get(0);
        grid.addView(metricCard("今日记录", total + " 条", "已保存到本机"), weightWrap(1f));
        grid.addView(space(8, 1));
        grid.addView(metricCard("上次记录", latest == null ? "暂无" : BabyLogFormatters.formatRelativeTime(latest.occurredAt), latest == null ? "点 + 开始" : BabyLogFormatters.eventLabel(latest.eventType)), weightWrap(1f));
        grid.addView(space(8, 1));
        grid.addView(metricCard("待同步", dashboard.pendingSyncCount > 0 ? "待同步 " + dashboard.pendingSyncCount + " 条" : "0 条", "服务器未配置"), weightWrap(1f));
        panel.addView(grid);
        return panel;
    }

    private void renderTimeline(LinearLayout content) {
        LinearLayout filters = panel(14, SURFACE);
        filters.setOrientation(LinearLayout.HORIZONTAL);
        String[] labels = {"全部", "孕期", "育儿", "B 超", "体温", "产检"};
        String[] filtersKeys = {"all", "pregnancy", "baby", "ultrasound", "temperature", "checkup"};
        for (int i = 0; i < labels.length; i++) {
            String label = labels[i];
            String filter = filtersKeys[i];
            boolean selected = filter.equals(selectedTimelineFilter);
            TextView chip = label(label, 13, selected ? 0xFFFFFFFF : MUTED, true);
            chip.setGravity(Gravity.CENTER);
            chip.setBackground(round(selected ? PRIMARY : SURFACE_2, 999, BORDER));
            chip.setPadding(dp(12), dp(8), dp(12), dp(8));
            chip.setOnClickListener(view -> {
                selectedTimelineFilter = filter;
                render();
            });
            filters.addView(chip, wrapWrapWithRight(8));
        }
        content.addView(filters, matchWrapWithBottom(14));
        content.addView(disclaimer("曲线和参考提示只用于家庭记录，不能替代医生判断。"), matchWrapWithBottom(14));

        List<BabyLogDomain.BabyLogEvent> events = timelineEvents;
        if (events == null) {
            loadTimelineInBackground();
            content.addView(empty("正在读取时间线..."));
            return;
        }
        boolean hasVisibleEvent = false;
        for (BabyLogDomain.BabyLogEvent event : events) {
            if (BabyLogFormatters.matchesTimelineFilter(event.eventType, selectedTimelineFilter)) {
                hasVisibleEvent = true;
                break;
            }
        }
        if (!hasVisibleEvent) {
            content.addView(empty("暂无本地记录。快捷记录会先保存到本机，再等待同步上传。"));
            return;
        }
        for (BabyLogDomain.BabyLogEvent event : events) {
            if (BabyLogFormatters.matchesTimelineFilter(event.eventType, selectedTimelineFilter)) {
                content.addView(timelineRow(event), matchWrapWithBottom(10));
            }
        }
    }

    private void renderLibrary(LinearLayout content) {
        List<BabyLogDomain.AttachmentRecord> attachments = libraryAttachments;
        if (attachments == null) {
            loadAttachmentsInBackground();
            content.addView(empty("正在读取本机附件..."));
            return;
        }
        List<BabyLogDomain.AttachmentRecord> ultrasound = filterAttachments(attachments, "ultrasound_image");
        List<BabyLogDomain.AttachmentRecord> documents = filterAttachments(attachments, "document");
        List<BabyLogDomain.AttachmentRecord> vaccines = filterAttachments(attachments, "vaccine_card");
        content.addView(libraryItem("B 超单", attachmentCount(ultrasound), "已保存本机；OCR 待接入", R.drawable.ultrasound_sheet, view -> showAttachmentList("B 超单", ultrasound)), matchWrapWithBottom(12));
        content.addView(libraryItem("检查单", attachmentCount(documents), documents.isEmpty() ? "待支持新增入口；可显示已导入附件" : "本机检查附件", R.drawable.baby_diary_notebook, documents.isEmpty() ? null : view -> showAttachmentList("检查单", documents)), matchWrapWithBottom(12));
        content.addView(libraryItem("出生证明", "待支持", "出生资料归档入口待补；不伪装为可用功能", R.drawable.vaccine_card, null), matchWrapWithBottom(12));
        content.addView(libraryItem("疫苗本", attachmentCount(vaccines), vaccines.isEmpty() ? "出生后启用；可显示已导入附件" : "本机疫苗附件", R.drawable.vaccine_card, vaccines.isEmpty() ? null : view -> showAttachmentList("疫苗本", vaccines)), matchWrapWithBottom(12));
        content.addView(disclaimer("FGR / 成长曲线标准数据尚未落库；当前曲线只显示自有趋势。点击首页趋势卡进入全屏曲线。"));
    }

    private List<BabyLogDomain.AttachmentRecord> filterAttachments(List<BabyLogDomain.AttachmentRecord> attachments, String kind) {
        List<BabyLogDomain.AttachmentRecord> filtered = new ArrayList<>();
        for (BabyLogDomain.AttachmentRecord attachment : attachments) {
            if (kind.equals(attachment.kind)) {
                filtered.add(attachment);
            }
        }
        return filtered;
    }

    private String attachmentCount(List<BabyLogDomain.AttachmentRecord> attachments) {
        return attachments.size() + " 张";
    }

    private void renderSettings(LinearLayout content) {
        BabyLogService.DashboardSnapshot dashboard = dashboardSnapshot;
        BabyLogDomain.BackendConfig settings = repository.loadSyncSettings();

        LinearLayout profile = settingsPanel("档案");
        profile.addView(settingRow("当前范围", "单胎 / 单宝宝", false, false));
        profile.addView(settingRow("预产期", "2026-08-05", false, false));
        profile.addView(settingRow("日界", "自然日 00:00（待配置）", false, false));
        profile.addView(settingRow("夜间柔光", "跟随系统（待配置）", false, false));
        content.addView(profile, matchWrapWithBottom(14));
        if (dashboard == null) {
            loadDashboardInBackground();
            content.addView(empty("正在读取本机数据..."));
            return;
        }

        LinearLayout data = settingsPanel("数据");
        long lastBackupMs = getSharedPreferences(META_PREFS_NAME, MODE_PRIVATE).getLong(LAST_BACKUP_EXPORT_MS, 0L);
        data.addView(backupActionRow(lastBackupMs));
        data.addView(settingRow("本机用量", BabyLogFormatters.formatByteSize(dashboard.localBytes) + "（含附件）", false, false));
        data.addView(settingRow("可用空间", BabyLogFormatters.formatByteSize(dashboard.freeBytes), false, false));
        data.addView(actionRow("从备份导入", "JSON", "导入", view -> importBackup()));
        data.addView(actionRow("清空本地数据", "删除本机记录、附件和待同步队列", "清空", DANGER, view -> confirmClearLocalData()));
        content.addView(data, matchWrapWithBottom(14));

        LinearLayout sync = settingsPanel("同步");
        sync.addView(settingRow("后端", settings.enabled ? "已配置" : "后端未配置", false, false));
        sync.addView(settingRow("待同步记录", dashboard.pendingSyncCount + " 条待上传", false, false));
        sync.addView(settingRow("失败记录", dashboard.failedSyncCount + " 条需重试", false, false));
        sync.addView(settingRow("服务端地域", settings.region.isEmpty() ? "-" : settings.region, false, false));
        sync.addView(settingRow("最近健康检查", "-", false, false));
        syncUrlInput = input("服务器地址", settings.backendBaseUrl, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        syncRegionInput = input("服务端地域", settings.region, InputType.TYPE_CLASS_TEXT);
        sync.addView(syncUrlInput, matchWrapWithBottom(8));
        sync.addView(syncRegionInput, matchWrapWithBottom(10));
        sync.addView(outlineButton("保存同步配置", view -> saveSyncSettings()), matchWrapWithBottom(8));
        Button syncButton = outlineButton(settings.enabled ? "立即同步" : "配置后可同步", view -> syncNow());
        syncButton.setEnabled(settings.enabled);
        syncButton.setAlpha(settings.enabled ? 1f : 0.45f);
        sync.addView(syncButton, matchWrapWithBottom(8));
        sync.addView(label("当前所有记录先保存到本机；服务器地址只保存在当前手机，后续接入后端后再上传 pending 队列。", 12, MUTED, false));
        content.addView(sync, matchWrapWithBottom(14));

        LinearLayout about = settingsPanel("关于");
        about.addView(settingRow("版本", "0.1.0 · MVP", false, false));
        about.addView(settingRow("医疗免责声明", "查看", true, false, view -> showInfoDialog("医疗免责声明", "BabyLog 只做家庭记录、趋势整理和复诊沟通辅助；任何曲线、范围或提示都不能替代医生判断。OCR 接入后也必须人工确认后再保存。")));
        about.addView(settingRow("隐私说明", "查看", true, false, view -> showInfoDialog("隐私说明", "阶段一数据仅保存在当前手机本机目录。导出备份会包含结构化记录和本机附件图片，请妥善保存；启用云同步前会单独确认服务器地址、地域和敏感医疗数据上传风险。")));
        content.addView(about);
    }

    private void showQuickSheet() {
        Dialog dialog = new Dialog(this);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(18), dp(16), dp(18), dp(18));
        body.setBackground(round(SURFACE, 22, BORDER));
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(label("快捷记录", 20, INK, true), weightWrap(1f));
        Button close = outlineButton("关闭", view -> dialog.dismiss());
        header.addView(close, new LinearLayout.LayoutParams(dp(76), dp(42)));
        body.addView(header, matchWrapWithBottom(12));
        for (BabyLogService.QuickAction action : quickActions()) {
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(12), dp(10), dp(12), dp(10));
            row.setBackground(round(softTone(action.toneColor), 14, 0));
            row.addView(image(action.assetResId, 42), wrapWrapWithRight(12));
            LinearLayout copy = new LinearLayout(this);
            copy.setOrientation(LinearLayout.VERTICAL);
            copy.addView(label(action.label, 16, INK, true));
            copy.addView(label(action.hint, 12, MUTED, false));
            row.addView(copy, weightWrap(1f));
            row.setOnClickListener(view -> {
                dialog.dismiss();
                handleQuickAction(action);
            });
            body.addView(row, matchWrapWithBottom(10));
        }
        dialog.setContentView(body);
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setGravity(Gravity.BOTTOM);
            WindowManager.LayoutParams params = window.getAttributes();
            params.width = WindowManager.LayoutParams.MATCH_PARENT;
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(params);
        }
    }

    private void handleQuickAction(BabyLogService.QuickAction action) {
        if ("ultrasound".equals(action.eventType)) {
            showUltrasoundForm();
            return;
        }
        if (isBabyCareAction(action.eventType)) {
            showBabyCareForm(action);
            return;
        }
        try {
            service.recordQuickEvent(action);
            Toast.makeText(this, action.label + "已保存到本机，等待同步", Toast.LENGTH_SHORT).show();
            render();
        } catch (JSONException error) {
            Toast.makeText(this, "保存失败：" + error.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private boolean isBabyCareAction(String eventType) {
        return "feed".equals(eventType)
                || "sleep".equals(eventType)
                || "diaper".equals(eventType)
                || "temperature".equals(eventType)
                || "medication".equals(eventType);
    }

    private void showBabyCareForm(BabyLogService.QuickAction action) {
        showBabyCareForm(action, null);
    }

    private void showBabyCareForm(BabyLogService.QuickAction action, Bundle restoredState) {
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(4), dp(8), dp(4), 0);
        if ("temperature".equals(action.eventType) || "medication".equals(action.eventType)) {
            body.addView(disclaimer("仅供家庭记录和复诊沟通参考，不能替代医生判断。"), matchWrapWithBottom(10));
        }

        EditText primaryInput;
        EditText secondaryInput;
        EditText tertiaryInput = null;
        EditText noteInput = null;
        int textInput = InputType.TYPE_CLASS_TEXT;
        int numberInput = InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL;

        if ("feed".equals(action.eventType)) {
            primaryInput = input("方式，例如 母乳 / 奶瓶 / 辅食", restoredText(restoredState, STATE_PRIMARY), textInput);
            secondaryInput = input("奶量 ml（可空）", restoredText(restoredState, STATE_SECONDARY), numberInput);
            noteInput = input("备注（可空）", restoredText(restoredState, STATE_NOTE), textInput);
        } else if ("sleep".equals(action.eventType)) {
            primaryInput = input("开始时间，例如 22:10", restoredText(restoredState, STATE_PRIMARY), textInput);
            secondaryInput = input("结束时间，例如 01:20", restoredText(restoredState, STATE_SECONDARY), textInput);
            tertiaryInput = input("地点，例如 卧室 / 推车", restoredText(restoredState, STATE_TERTIARY), textInput);
            noteInput = input("备注（可空）", restoredText(restoredState, STATE_NOTE), textInput);
        } else if ("diaper".equals(action.eventType)) {
            primaryInput = input("类型，例如 尿 / 便 / 混合", restoredText(restoredState, STATE_PRIMARY), textInput);
            secondaryInput = input("性状 / 颜色 / 备注", restoredText(restoredState, STATE_SECONDARY), textInput);
        } else if ("temperature".equals(action.eventType)) {
            primaryInput = input("体温 ℃", restoredText(restoredState, STATE_PRIMARY), numberInput);
            secondaryInput = input("测量方式，例如 腋温 / 耳温", restoredText(restoredState, STATE_SECONDARY), textInput);
            noteInput = input("备注（可空）", restoredText(restoredState, STATE_NOTE), textInput);
        } else {
            primaryInput = input("药名", restoredText(restoredState, STATE_PRIMARY), textInput);
            secondaryInput = input("剂量和单位，例如 2 ml", restoredText(restoredState, STATE_SECONDARY), textInput);
            tertiaryInput = input("原因，例如 发热", restoredText(restoredState, STATE_TERTIARY), textInput);
        }

        body.addView(primaryInput, matchWrapWithBottom(8));
        body.addView(secondaryInput, matchWrapWithBottom(8));
        if (tertiaryInput != null) {
            body.addView(tertiaryInput, matchWrapWithBottom(8));
        }
        if (noteInput != null) {
            body.addView(noteInput, matchWrapWithBottom(8));
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(action.label + "记录")
                .setView(body)
                .setNegativeButton("关闭", null)
                .setPositiveButton("保存", null)
                .show();
        activeBabyCareDialog = dialog;
        activeBabyCareEventType = action.eventType;
        babyCarePrimaryInput = primaryInput;
        babyCareSecondaryInput = secondaryInput;
        babyCareTertiaryInput = tertiaryInput;
        babyCareNoteInput = noteInput;
        dialog.setOnDismissListener(ignored -> clearBabyCareFormRefs());
        EditText finalTertiaryInput = tertiaryInput;
        EditText finalNoteInput = noteInput;
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view ->
                saveBabyCareForm(action, dialog, primaryInput, secondaryInput, finalTertiaryInput, finalNoteInput));
    }

    private String restoredText(Bundle state, String key) {
        return state == null ? "" : state.getString(key, "");
    }

    private void clearBabyCareFormRefs() {
        activeBabyCareDialog = null;
        activeBabyCareEventType = "";
        babyCarePrimaryInput = null;
        babyCareSecondaryInput = null;
        babyCareTertiaryInput = null;
        babyCareNoteInput = null;
    }

    private void saveBabyCareForm(
            BabyLogService.QuickAction action,
            AlertDialog dialog,
            EditText primaryInput,
            EditText secondaryInput,
            EditText tertiaryInput,
            EditText noteInput
    ) {
        if ("feed".equals(action.eventType)
                && !text(secondaryInput).isEmpty()
                && BabyLogFormatters.parseOptionalNumber(text(secondaryInput)) == null) {
            Toast.makeText(this, "奶量请输入数字，单位 ml", Toast.LENGTH_SHORT).show();
            return;
        }
        if ("temperature".equals(action.eventType)
                && BabyLogFormatters.parseOptionalNumber(text(primaryInput)) == null) {
            Toast.makeText(this, "请填写有效体温数值", Toast.LENGTH_SHORT).show();
            return;
        }
        if ("medication".equals(action.eventType)
                && (text(primaryInput).isEmpty() || text(secondaryInput).isEmpty())) {
            Toast.makeText(this, "请填写药名和剂量单位", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            service.recordBabyCareEvent(createBabyCareInput(action.eventType, primaryInput, secondaryInput, tertiaryInput, noteInput));
            Toast.makeText(this, action.label + "已保存到本机，等待同步", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            invalidateLoadedData();
            render();
        } catch (JSONException error) {
            Toast.makeText(this, "保存失败：" + error.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private BabyLogService.BabyCareInput createBabyCareInput(
            String eventType,
            EditText primaryInput,
            EditText secondaryInput,
            EditText tertiaryInput,
            EditText noteInput
    ) {
        if ("feed".equals(eventType)) {
            return BabyLogService.BabyCareInput.feed(text(primaryInput), text(secondaryInput), text(noteInput));
        }
        if ("sleep".equals(eventType)) {
            return BabyLogService.BabyCareInput.sleep(text(primaryInput), text(secondaryInput), text(tertiaryInput), text(noteInput));
        }
        if ("diaper".equals(eventType)) {
            return BabyLogService.BabyCareInput.diaper(text(primaryInput), text(secondaryInput), text(noteInput));
        }
        if ("temperature".equals(eventType)) {
            return BabyLogService.BabyCareInput.temperature(text(primaryInput), text(secondaryInput), text(noteInput));
        }
        return BabyLogService.BabyCareInput.medication(text(primaryInput), text(secondaryInput), text(tertiaryInput));
    }

    private void showUltrasoundForm() {
        showUltrasoundForm(null);
    }

    private void showUltrasoundForm(Bundle restoredState) {
        if (restoredState == null) {
            currentPhotoPath = "";
            currentPhotoName = "";
            ultrasoundRangeWarningConfirmed = false;
        } else {
            currentPhotoPath = restoredState.getString(STATE_PHOTO_PATH, "");
            currentPhotoName = restoredState.getString(STATE_PHOTO_NAME, "");
            ultrasoundRangeWarningConfirmed = restoredState.getBoolean(STATE_RANGE_CONFIRMED, false);
        }
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(4), dp(8), dp(4), 0);
        body.addView(disclaimer("仅供家庭记录和复诊沟通参考，不能替代医生判断。"), matchWrapWithBottom(10));
        examDateInput = input("检查日期 yyyy-MM-dd", restoredText(restoredState, STATE_EXAM_DATE, BabyLogFormatters.todayDateInput()), InputType.TYPE_CLASS_DATETIME);
        gestationalAgeInput = input("孕周，例如 28+3", restoredText(restoredState, STATE_GESTATIONAL_AGE, "28+3"), InputType.TYPE_CLASS_TEXT);
        bpdInput = input("双顶径 BPD", restoredText(restoredState, STATE_BPD), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        hcInput = input("头围 HC", restoredText(restoredState, STATE_HC), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        acInput = input("腹围 AC", restoredText(restoredState, STATE_AC), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        flInput = input("股骨长 FL", restoredText(restoredState, STATE_FL), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        efwInput = input("估计胎重 EFW", restoredText(restoredState, STATE_EFW), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        applyTabularNumbers(bpdInput);
        applyTabularNumbers(hcInput);
        applyTabularNumbers(acInput);
        applyTabularNumbers(flInput);
        applyTabularNumbers(efwInput);
        body.addView(examDateInput, matchWrapWithBottom(8));
        body.addView(gestationalAgeInput, matchWrapWithBottom(8));
        body.addView(unitInputRow(bpdInput, "mm"), matchWrapWithBottom(8));
        body.addView(unitInputRow(hcInput, "mm"), matchWrapWithBottom(8));
        body.addView(unitInputRow(acInput, "mm"), matchWrapWithBottom(8));
        body.addView(unitInputRow(flInput, "mm"), matchWrapWithBottom(8));
        body.addView(unitInputRow(efwInput, "g"), matchWrapWithBottom(8));
        photoPreview = new ImageView(this);
        photoPreview.setAdjustViewBounds(true);
        photoPreview.setMaxHeight(dp(190));
        if (!currentPhotoPath.isEmpty()) {
            photoPreview.setImageBitmap(BitmapFactory.decodeFile(currentPhotoPath));
        }
        body.addView(photoPreview, matchWrapWithBottom(8));
        LinearLayout photoActions = new LinearLayout(this);
        photoActions.setOrientation(LinearLayout.HORIZONTAL);
        photoActions.addView(outlineButton("拍 B 超单", view -> launchCamera()), weightWrap(1f));
        photoActions.addView(space(8, 1));
        photoActions.addView(outlineButton("从相册选", view -> pickImage()), weightWrap(1f));
        body.addView(photoActions, matchWrapWithBottom(8));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("B 超记录")
                .setView(body)
                .setNegativeButton("关闭", null)
                .setPositiveButton("保存 B 超记录", null)
                .show();
        activeUltrasoundDialog = dialog;
        dialog.setOnDismissListener(ignored -> clearUltrasoundFormRefs());
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> saveUltrasound(dialog));
    }

    private String restoredText(Bundle state, String key, String fallback) {
        return state == null ? fallback : state.getString(key, fallback);
    }

    private void clearUltrasoundFormRefs() {
        activeUltrasoundDialog = null;
        photoPreview = null;
        examDateInput = null;
        gestationalAgeInput = null;
        bpdInput = null;
        hcInput = null;
        acInput = null;
        flInput = null;
        efwInput = null;
    }

    private void launchCamera() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_PERMISSION_CAMERA);
            return;
        }
        startCameraCapture();
    }

    private void startCameraCapture() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) == null) {
            Toast.makeText(this, "没有找到系统相机", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            pendingCameraFile = service.createCameraCaptureFile("ultrasound-scan.jpg");
            pendingCameraUri = BabyLogFileProvider.getUriForFile(this, pendingCameraFile);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, pendingCameraUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            grantCameraUriPermissions(intent);
            startActivityForResult(intent, REQUEST_CAPTURE_ULTRASOUND);
        } catch (IOException error) {
            Toast.makeText(this, "启动相机失败：" + error.getMessage(), Toast.LENGTH_LONG).show();
            cleanupPendingCamera();
        }
    }

    private void pickImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES}, REQUEST_PERMISSION_IMAGES);
            return;
        }
        startImagePicker();
    }

    private void startImagePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_PICK_ULTRASOUND);
    }

    private void saveUltrasound(AlertDialog dialog) {
        String date = text(examDateInput);
        String gestationalAge = text(gestationalAgeInput);
        if (!BabyLogFormatters.isValidDateInput(date)) {
            Toast.makeText(this, "请填写检查日期", Toast.LENGTH_SHORT).show();
            return;
        }
        Integer gestationalAgeDays = BabyLogFormatters.parseGestationalAgeDays(gestationalAge);
        if (gestationalAgeDays == null) {
            Toast.makeText(this, "请填写有效孕周，例如 28+3", Toast.LENGTH_SHORT).show();
            return;
        }
        Double bpdMm = BabyLogFormatters.parseOptionalNumber(text(bpdInput));
        Double hcMm = BabyLogFormatters.parseOptionalNumber(text(hcInput));
        Double acMm = BabyLogFormatters.parseOptionalNumber(text(acInput));
        Double flMm = BabyLogFormatters.parseOptionalNumber(text(flInput));
        Double efwGram = BabyLogFormatters.parseOptionalNumber(text(efwInput));
        String warnings = BabyLogFormatters.formatUltrasoundSoftRangeWarnings(gestationalAgeDays, bpdMm, hcMm, acMm, flMm, efwGram);
        markUltrasoundSoftRanges(gestationalAgeDays, bpdMm, hcMm, acMm, flMm, efwGram);
        if (!warnings.isEmpty() && !ultrasoundRangeWarningConfirmed) {
            new AlertDialog.Builder(this)
                    .setTitle("请复核 B 超单位")
                    .setMessage(warnings + "\n\n这些只是常用范围提醒，不是诊断。请确认是否录入单位或小数点无误。")
                    .setNegativeButton("返回修改", null)
                    .setPositiveButton("仍然保存", (confirmDialog, which) -> {
                        ultrasoundRangeWarningConfirmed = true;
                        saveUltrasound(dialog);
                    })
                    .show();
            return;
        }
        try {
            service.recordUltrasound(new BabyLogService.UltrasoundInput(
                    date,
                    gestationalAge,
                    text(bpdInput),
                    text(hcInput),
                    text(acInput),
                    text(flInput),
                    text(efwInput),
                    currentPhotoPath,
                    currentPhotoName
            ));
            Toast.makeText(this, "B 超已保存到本机，等待同步", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            invalidateLoadedData();
            render();
        } catch (JSONException error) {
            Toast.makeText(this, "B 超保存失败：" + error.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void markUltrasoundSoftRanges(Integer gestationalAgeDays, Double bpdMm, Double hcMm, Double acMm, Double flMm, Double efwGram) {
        markGestationalAgeRangeInput(gestationalAgeInput, gestationalAgeDays);
        markSoftRangeInput(bpdInput, bpdMm, 10, 120);
        markSoftRangeInput(hcInput, hcMm, 50, 400);
        markSoftRangeInput(acInput, acMm, 50, 400);
        markSoftRangeInput(flInput, flMm, 5, 90);
        markSoftRangeInput(efwInput, efwGram, 50, 6000);
    }

    private void markSoftRangeInput(EditText input, Double value, double min, double max) {
        if (input == null) {
            return;
        }
        if (BabyLogFormatters.isOutsideSoftRange(value, min, max)) {
            input.setBackground(round(0xFFFFE9E5, 10, DANGER));
        } else {
            input.setBackground(round(SURFACE, 10, BORDER));
        }
    }

    private void markGestationalAgeRangeInput(EditText input, Integer days) {
        if (input == null) {
            return;
        }
        if (days != null && (days < 70 || days > 294)) {
            input.setBackground(round(0xFFFFE9E5, 10, DANGER));
        } else {
            input.setBackground(round(SURFACE, 10, BORDER));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            if (requestCode == REQUEST_CAPTURE_ULTRASOUND) {
                cleanupPendingCamera();
            }
            return;
        }
        if (requestCode == REQUEST_CAPTURE_ULTRASOUND) {
            importCapturedImage();
        } else if (requestCode == REQUEST_PICK_ULTRASOUND && data != null && data.getData() != null) {
            copyPickedImage(data.getData());
        } else if (requestCode == REQUEST_EXPORT_BACKUP && data != null && data.getData() != null) {
            writeBackup(data.getData());
        } else if (requestCode == REQUEST_IMPORT_BACKUP && data != null && data.getData() != null) {
            readBackup(data.getData());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCameraCapture();
            } else {
                Toast.makeText(this, "未授权相机，无法拍 B 超单", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_PERMISSION_IMAGES) {
            if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "未授权媒体读取，改用系统文件选择器", Toast.LENGTH_SHORT).show();
            }
            startImagePicker();
        }
    }

    private void importCapturedImage() {
        if (pendingCameraFile == null || !pendingCameraFile.exists() || pendingCameraFile.length() == 0) {
            Toast.makeText(this, "相机未返回图片", Toast.LENGTH_SHORT).show();
            cleanupPendingCamera();
            return;
        }
        try {
            currentPhotoPath = service.compressImageFileToPrivateFile(pendingCameraFile, "scan.jpg");
            currentPhotoName = new File(currentPhotoPath).getName();
            if (photoPreview != null) {
                photoPreview.setImageBitmap(BitmapFactory.decodeFile(currentPhotoPath));
            }
            Toast.makeText(this, "照片已压缩保存到本机", Toast.LENGTH_SHORT).show();
        } catch (IOException error) {
            Toast.makeText(this, "保存照片失败：" + error.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            cleanupPendingCamera();
        }
    }

    private void copyPickedImage(Uri uri) {
        try {
            currentPhotoPath = service.copyImageUriToPrivateFile(uri, "selected.jpg");
            currentPhotoName = new File(currentPhotoPath).getName();
            if (photoPreview != null) {
                photoPreview.setImageBitmap(BitmapFactory.decodeFile(currentPhotoPath));
            }
            Toast.makeText(this, "图片已保存到本机", Toast.LENGTH_SHORT).show();
        } catch (IOException error) {
            Toast.makeText(this, "读取图片失败：" + error.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void grantCameraUriPermissions(Intent intent) {
        if (pendingCameraUri == null) {
            return;
        }
        int flags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
        List<ResolveInfo> resolved = getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo info : resolved) {
            if (info.activityInfo != null) {
                grantUriPermission(info.activityInfo.packageName, pendingCameraUri, flags);
            }
        }
    }

    private void cleanupPendingCamera() {
        if (pendingCameraUri != null) {
            revokeUriPermission(pendingCameraUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            pendingCameraUri = null;
        }
        if (pendingCameraFile != null && pendingCameraFile.exists()) {
            pendingCameraFile.delete();
        }
        pendingCameraFile = null;
    }

    private void exportBackup() {
        try {
            pendingBackupJson = service.createBackupJson();
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/json");
            intent.putExtra(Intent.EXTRA_TITLE, "babylog-backup-" + new SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(new Date()) + ".json");
            startActivityForResult(intent, REQUEST_EXPORT_BACKUP);
        } catch (JSONException | IOException error) {
            Toast.makeText(this, "备份失败：" + error.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void writeBackup(Uri uri) {
        try (OutputStream output = getContentResolver().openOutputStream(uri)) {
            if (output == null) {
                throw new IOException("无法写入备份文件");
            }
            output.write(pendingBackupJson.getBytes(StandardCharsets.UTF_8));
            getSharedPreferences(META_PREFS_NAME, MODE_PRIVATE)
                    .edit()
                    .putLong(LAST_BACKUP_EXPORT_MS, System.currentTimeMillis())
                    .apply();
            Toast.makeText(this, "备份已生成", Toast.LENGTH_SHORT).show();
            invalidateLoadedData();
            render();
        } catch (IOException error) {
            Toast.makeText(this, "备份写入失败：" + error.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void importBackup() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        startActivityForResult(intent, REQUEST_IMPORT_BACKUP);
    }

    private void readBackup(Uri uri) {
        try (InputStream input = getContentResolver().openInputStream(uri)) {
            if (input == null) {
                throw new IOException("无法读取备份文件");
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            String raw = new String(output.toByteArray(), StandardCharsets.UTF_8);
            int count = service.importBackupJson(raw);
            Toast.makeText(this, "导入完成：" + count + " 条记录", Toast.LENGTH_SHORT).show();
            invalidateLoadedData();
            render();
        } catch (JSONException | IOException error) {
            Toast.makeText(this, "导入失败：" + error.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void showInfoDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("知道了", null)
                .show();
    }

    private void saveSyncSettings() {
        String normalizedUrl = BabyLogFormatters.normalizeBackendBaseUrl(text(syncUrlInput));
        if (!normalizedUrl.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("确认启用同步")
                    .setMessage("启用后，家庭记录、医疗相关记录和本机待上传队列会发送到你配置的服务器。请确认服务器地址、地域和数据合规风险都已知晓。")
                    .setNegativeButton("返回修改", null)
                    .setPositiveButton("我已知晓并保存", (dialog, which) -> persistSyncSettings(normalizedUrl))
                    .show();
            return;
        }
        persistSyncSettings("");
    }

    private void persistSyncSettings(String backendBaseUrl) {
        try {
            BabyLogDomain.BackendConfig saved = repository.saveSyncSettings(new BabyLogDomain.BackendConfig(
                    !backendBaseUrl.isEmpty(),
                    backendBaseUrl,
                    text(syncRegionInput),
                    null
            ));
            Toast.makeText(this, saved.enabled ? "同步配置已保存到本机" : "同步配置已清空，当前为本机模式", Toast.LENGTH_SHORT).show();
            invalidateLoadedData();
            render();
        } catch (JSONException error) {
            Toast.makeText(this, "保存同步配置失败：" + error.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void confirmClearLocalData() {
        new AlertDialog.Builder(this)
                .setTitle("清空本地数据")
                .setMessage("将删除本机记录、附件和待同步队列，保留同步配置。此操作不会删除你已经导出的备份文件。")
                .setNegativeButton("取消", null)
                .setPositiveButton("清空", (dialog, which) -> clearLocalData())
                .show();
    }

    private void clearLocalData() {
        service.clearLocalData();
        Toast.makeText(this, "本地数据已清空", Toast.LENGTH_SHORT).show();
        invalidateLoadedData();
        render();
    }

    private void syncNow() {
        try {
            BabyLogService.SyncResult result = service.runSyncNow();
            if (result.ok) {
                Toast.makeText(this, "没有待同步记录", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "同步失败：" + formatSyncError(result.code) + "，记录仍保留在待上传队列", Toast.LENGTH_LONG).show();
            }
            render();
        } catch (JSONException error) {
            Toast.makeText(this, "同步失败：" + error.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void showAttachmentList(String title, List<BabyLogDomain.AttachmentRecord> allAttachments) {
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        boolean hasAny = false;
        for (BabyLogDomain.AttachmentRecord attachment : allAttachments) {
            hasAny = true;
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(12), dp(10), dp(12), dp(10));
            row.setBackground(round(SURFACE_2, 14, BORDER));
            row.addView(image(R.drawable.ultrasound_sheet, 42), wrapWrapWithRight(12));
            LinearLayout copy = new LinearLayout(this);
            copy.setOrientation(LinearLayout.VERTICAL);
            copy.addView(label(attachment.originalName, 15, INK, true));
            copy.addView(label(BabyLogFormatters.formatDateTime(attachment.createdAt) + " · " + BabyLogFormatters.formatByteSize(attachment.byteSize), 12, MUTED, false));
            copy.addView(label(BabyLogFormatters.ocrStatusLabel(attachment.ocrStatus), 12, TEXT_3, false));
            row.addView(copy, weightWrap(1f));
            row.setOnClickListener(view -> showAttachmentPreview(attachment));
            body.addView(row, matchWrapWithBottom(10));
        }
        if (!hasAny) {
            body.addView(empty("这里还没有本机附件。"));
        }
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(body)
                .setNegativeButton("关闭", null)
                .show();
    }

    private void showAttachmentPreview(BabyLogDomain.AttachmentRecord attachment) {
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        File file = new File(attachment.localPath);
        if (file.exists()) {
            ImageView preview = new ImageView(this);
            preview.setAdjustViewBounds(true);
            preview.setImageBitmap(BitmapFactory.decodeFile(file.getAbsolutePath()));
            body.addView(preview, matchWrap());
        } else {
            body.addView(empty("本机图片数据未找到"));
        }
        new AlertDialog.Builder(this)
                .setTitle(attachment.originalName)
                .setView(body)
                .setNegativeButton("关闭", null)
                .show();
    }

    private View bottomNav() {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(8), dp(8), dp(8), dp(8));
        nav.setBackgroundColor(SURFACE);
        nav.addView(navButton("home", "首页", R.drawable.baby_diary_notebook), weightWrap(1f));
        nav.addView(navButton("timeline", "时间线", R.drawable.calendar), weightWrap(1f));
        Button quick = primaryButton("+", view -> showQuickSheet());
        quick.setTextSize(24);
        nav.addView(quick, new LinearLayout.LayoutParams(dp(62), dp(54)));
        nav.addView(navButton("library", "资料", R.drawable.growth_ruler), weightWrap(1f));
        nav.addView(navButton("settings", "设置", R.drawable.icon_settings), weightWrap(1f));
        return nav;
    }

    private View navButton(String key, String text, int asset) {
        LinearLayout button = new LinearLayout(this);
        button.setOrientation(LinearLayout.VERTICAL);
        button.setGravity(Gravity.CENTER);
        button.setPadding(0, dp(4), 0, dp(4));
        button.addView(image(asset, 28), wrapWrap());
        button.addView(label(text, 11, key.equals(activeTab) ? PRIMARY : MUTED, true));
        button.setOnClickListener(view -> {
            activeTab = key;
            render();
        });
        return button;
    }

    private List<BabyLogService.QuickAction> quickActions() {
        List<BabyLogService.QuickAction> actions = new ArrayList<>();
        actions.add(new BabyLogService.QuickAction("喂养", "母乳 / 奶瓶 / 辅食", R.drawable.feeding_bottle, PEACH, "feed", "快捷记录 · 待补充奶量/方式"));
        actions.add(new BabyLogService.QuickAction("睡眠", "开始 / 结束 / 地点", R.drawable.sleep_moon, BLUE, "sleep", "快捷记录 · 待补充睡眠时长"));
        actions.add(new BabyLogService.QuickAction("尿布", "尿 / 便 / 性状", R.drawable.diaper, YELLOW, "diaper", "快捷记录 · 待补充尿/便细节"));
        actions.add(new BabyLogService.QuickAction("体温", "温度 / 测量方式", R.drawable.thermometer, GREEN, "temperature", "快捷记录 · 待补充温度数值"));
        actions.add(new BabyLogService.QuickAction("用药", "药名 / 剂量 / 时间", R.drawable.icon_pill, VIOLET, "medication", "快捷记录 · 待补充药名/剂量"));
        actions.add(new BabyLogService.QuickAction("B超", "指标 / 照片 / OCR占位", R.drawable.ultrasound_sheet, ROSE, "ultrasound", "B 超快捷记录 · 待补指标/照片"));
        return actions;
    }

    private View timelineRow(BabyLogDomain.BabyLogEvent event) {
        int tone = eventTone(event.eventType);
        LinearLayout row = panel(0, softTone(tone));
        row.setOrientation(LinearLayout.HORIZONTAL);
        View ribbon = new View(this);
        ribbon.setBackgroundColor(tone);
        row.addView(ribbon, new LinearLayout.LayoutParams(dp(5), LinearLayout.LayoutParams.MATCH_PARENT));
        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setPadding(dp(12), dp(12), dp(12), dp(12));
        copy.addView(label(BabyLogFormatters.formatEventDay(event.occurredAt) + " " + BabyLogFormatters.formatEventTime(event.occurredAt) + " · " + BabyLogFormatters.eventLabel(event.eventType), 13, MUTED, true));
        copy.addView(label(BabyLogFormatters.eventSummary(event), 15, INK, false));
        if (!event.attachmentIds.isEmpty()) {
            TextView tag = label("附件 " + event.attachmentIds.size(), 12, PRIMARY, true);
            tag.setPadding(0, dp(6), 0, 0);
            copy.addView(tag);
        }
        row.addView(copy, weightWrap(1f));
        return row;
    }

    private int eventTone(String eventType) {
        String group = BabyLogFormatters.timelineFilterGroup(eventType);
        if ("pregnancy".equals(group)) return ACCENT;
        if ("baby".equals(group)) return PEACH;
        if ("ultrasound".equals(group)) return ROSE;
        if ("temperature".equals(group)) return GREEN;
        if ("checkup".equals(group)) return VIOLET;
        return BLUE;
    }

    private View libraryItem(String title, String count, String note, int asset, View.OnClickListener listener) {
        LinearLayout item = panel(14, SURFACE);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.addView(image(asset, 58), wrapWrapWithRight(12));
        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        LinearLayout line = new LinearLayout(this);
        line.setGravity(Gravity.CENTER_VERTICAL);
        line.addView(label(title, 16, INK, true), weightWrap(1f));
        line.addView(label(count, 14, PRIMARY, true));
        copy.addView(line);
        copy.addView(label(note, 12, MUTED, false));
        item.addView(copy, weightWrap(1f));
        if (listener != null) {
            item.setOnClickListener(listener);
        }
        return item;
    }

    private LinearLayout settingsPanel(String title) {
        LinearLayout panel = panel(16, SURFACE);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.addView(label(title, 18, INK, true), matchWrapWithBottom(8));
        return panel;
    }

    private View settingRow(String label, String value, boolean interactive, boolean destructive) {
        return settingRow(label, value, interactive, destructive, null);
    }

    private View settingRow(String label, String value, boolean interactive, boolean destructive, View.OnClickListener listener) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(10), 0, dp(10));
        row.addView(label(label, 14, destructive ? DANGER : MUTED, false), weightWrap(1f));
        row.addView(label(value, 14, destructive ? DANGER : INK, true));
        if (interactive) {
            row.addView(label(" >", 14, destructive ? DANGER : TEXT_3, true));
        }
        if (listener != null) {
            row.setOnClickListener(listener);
        }
        return row;
    }

    private View actionRow(String title, String subtitle, String action, View.OnClickListener listener) {
        return actionRow(title, subtitle, action, PRIMARY, MUTED, listener);
    }

    private View actionRow(String title, String subtitle, String action, int actionColor, View.OnClickListener listener) {
        return actionRow(title, subtitle, action, actionColor, MUTED, listener);
    }

    private View actionRow(String title, String subtitle, String action, int actionColor, int subtitleColor, View.OnClickListener listener) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(10), 0, dp(10));
        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.addView(label(title, 14, INK, true));
        copy.addView(label(subtitle, 12, subtitleColor, false));
        row.addView(copy, weightWrap(1f));
        row.addView(solidButton(action, actionColor, listener), new LinearLayout.LayoutParams(dp(84), dp(44)));
        return row;
    }

    private View backupActionRow(long lastBackupMs) {
        long now = System.currentTimeMillis();
        int level = BabyLogFormatters.backupAgeLevel(lastBackupMs, now);
        int subtitleColor = level == 2 ? DANGER : level == 1 ? 0xFF8A6521 : MUTED;
        return actionRow(
                "本地备份",
                "导出 JSON，含本机附件图片 · " + BabyLogFormatters.formatBackupAgeLabel(lastBackupMs, now),
                "导出",
                PRIMARY,
                subtitleColor,
                view -> exportBackup()
        );
    }

    private View sectionHeader(String title, String action, View.OnClickListener listener) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, 0, 0, dp(10));
        row.addView(label(title, 18, INK, true), weightWrap(1f));
        if (action != null) {
            TextView right = label(action, 13, listener == null ? TEXT_3 : PRIMARY, true);
            if (listener != null) {
                right.setOnClickListener(listener);
            }
            row.addView(right);
        }
        return row;
    }

    private View metricCard(String title, String value, String sub) {
        LinearLayout card = panel(12, SURFACE_2);
        card.setOrientation(LinearLayout.VERTICAL);
        card.addView(label(title, 11, MUTED, false));
        card.addView(label(value, 17, INK, true));
        card.addView(label(sub, 10, TEXT_3, false));
        return card;
    }

    private View trendCard(String title, String value, String sub, int tone) {
        LinearLayout card = panel(14, softTone(tone));
        card.setOrientation(LinearLayout.VERTICAL);
        card.addView(label(title, 12, MUTED, false));
        card.addView(label(value, 20, INK, true));
        card.addView(label(sub, 12, MUTED, false));
        return card;
    }

    private View disclaimer(String text) {
        TextView view = label(text, 13, 0xFF8A6521, false);
        view.setPadding(dp(14), dp(12), dp(14), dp(12));
        view.setBackground(round(0xFFFFF4D7, 14, 0));
        return view;
    }

    private TextView empty(String text) {
        TextView view = label(text, 14, MUTED, false);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(14), dp(18), dp(14), dp(18));
        view.setBackground(round(SURFACE, 14, BORDER));
        return view;
    }

    private LinearLayout panel(int padding, int color) {
        LinearLayout panel = new LinearLayout(this);
        panel.setPadding(dp(padding), dp(padding), dp(padding), dp(padding));
        panel.setBackground(round(color, 18, BORDER));
        return panel;
    }

    private TextView label(String text, int sp, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setLineSpacing(0, 1.18f);
        if (bold) {
            view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }
        return view;
    }

    private EditText input(String hint, String value, int inputType) {
        EditText editText = new EditText(this);
        editText.setHint(hint);
        editText.setText(value);
        editText.setInputType(inputType);
        editText.setTextColor(INK);
        editText.setHintTextColor(TEXT_3);
        editText.setSingleLine(false);
        editText.setPadding(dp(12), 0, dp(12), 0);
        editText.setBackground(round(SURFACE, 10, BORDER));
        return editText;
    }

    private View unitInputRow(EditText input, String unit) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(input, weightWrap(1f));
        TextView badge = label(unit, 13, PRIMARY, true);
        applyTabularNumbers(badge);
        badge.setGravity(Gravity.CENTER);
        badge.setPadding(dp(12), 0, dp(12), 0);
        badge.setBackground(round(PRIMARY_SOFT, 999, 0));
        row.addView(badge, new LinearLayout.LayoutParams(dp(60), dp(48)));
        return row;
    }

    private void applyTabularNumbers(TextView view) {
        view.setFontFeatureSettings("tnum");
        view.setTypeface(Typeface.MONOSPACE, view.getTypeface() == null ? Typeface.NORMAL : view.getTypeface().getStyle());
    }

    private Button primaryButton(String text, View.OnClickListener listener) {
        return solidButton(text, PRIMARY, listener);
    }

    private Button solidButton(String text, int color, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextColor(0xFFFFFFFF);
        button.setBackground(round(color, 999, 0));
        button.setOnClickListener(listener);
        return button;
    }

    private Button outlineButton(String text, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextColor(PRIMARY);
        button.setBackground(round(PRIMARY_SOFT, 999, 0));
        button.setOnClickListener(listener);
        return button;
    }

    private ImageView image(int resId, int dpSize) {
        ImageView image = new FixedSizeImageView(this, dp(dpSize));
        image.setImageResource(resId);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setLayoutParams(new LinearLayout.LayoutParams(dp(dpSize), dp(dpSize)));
        return image;
    }

    private GradientDrawable round(int color, int radius, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        if (strokeColor != 0) {
            drawable.setStroke(dp(1), strokeColor);
        }
        return drawable;
    }

    private int softTone(int tone) {
        if (tone == ROSE) return 0xFFFFE8ED;
        if (tone == BLUE) return 0xFFE8F0FF;
        if (tone == VIOLET) return 0xFFF1E9FB;
        if (tone == GREEN) return 0xFFE5F7EE;
        if (tone == YELLOW) return 0xFFFFF4CE;
        if (tone == PEACH) return 0xFFFFE8DA;
        return SURFACE_2;
    }

    private String latestEfwValue(BabyLogService.DashboardSnapshot dashboard) {
        for (BabyLogDomain.BabyLogEvent event : dashboard.recentEvents) {
            if ("ultrasound".equals(event.eventType) && event.payload.has("efwGram")) {
                return BabyLogFormatters.formatNumber(event.payload.optDouble("efwGram")) + " g";
            }
        }
        return "1320 g";
    }

    private String latestEfwCaption(BabyLogService.DashboardSnapshot dashboard) {
        for (BabyLogDomain.BabyLogEvent event : dashboard.recentEvents) {
            if ("ultrasound".equals(event.eventType) && event.payload.has("gestationalAgeDays")) {
                return BabyLogFormatters.formatGestationalAge(event.payload.optInt("gestationalAgeDays"));
            }
        }
        return "28+3 周";
    }

    private String tabTitle() {
        if ("timeline".equals(activeTab)) return "时间线";
        if ("library".equals(activeTab)) return "资料";
        if ("settings".equals(activeTab)) return "设置";
        return "BabyLog";
    }

    private String formatSyncError(String code) {
        if ("BACKEND_NOT_CONFIGURED".equals(code)) {
            return "后端未配置";
        }
        if ("BACKEND_UNREACHABLE".equals(code)) {
            return "后端不可达";
        }
        return code;
    }

    private String text(EditText editText) {
        return editText == null ? "" : editText.getText().toString().trim();
    }

    private View space(int widthDp, int heightDp) {
        View view = new View(this);
        view.setLayoutParams(new LinearLayout.LayoutParams(dp(widthDp), dp(heightDp)));
        return view;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
    }

    private LinearLayout.LayoutParams wrapWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
    }

    private LinearLayout.LayoutParams weightWrap(float weight) {
        return new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight);
    }

    private LinearLayout.LayoutParams matchWrapWithBottom(int bottomDp) {
        LinearLayout.LayoutParams params = matchWrap();
        params.setMargins(0, 0, 0, dp(bottomDp));
        return params;
    }

    private LinearLayout.LayoutParams wrapWrapWithBottom(int bottomDp) {
        LinearLayout.LayoutParams params = wrapWrap();
        params.setMargins(0, 0, 0, dp(bottomDp));
        return params;
    }

    private LinearLayout.LayoutParams wrapWrapWithRight(int rightDp) {
        LinearLayout.LayoutParams params = wrapWrap();
        params.setMargins(0, 0, dp(rightDp), 0);
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static final class FixedSizeImageView extends ImageView {
        private final int sizePx;

        FixedSizeImageView(Context context, int sizePx) {
            super(context);
            this.sizePx = sizePx;
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            setMeasuredDimension(sizePx, sizePx);
        }
    }
}
