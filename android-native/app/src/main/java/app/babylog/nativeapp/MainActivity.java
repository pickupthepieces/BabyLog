package app.babylog.nativeapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;

import java.io.File;
import java.io.FileOutputStream;
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

public final class MainActivity extends Activity {
    private static final int REQUEST_CAPTURE_ULTRASOUND = 1201;
    private static final int REQUEST_PICK_ULTRASOUND = 1202;
    private static final int REQUEST_EXPORT_BACKUP = 1203;
    private static final int REQUEST_IMPORT_BACKUP = 1204;

    private static final int BG = 0xFFEEF7F2;
    private static final int SURFACE = 0xFFFFFFFF;
    private static final int SURFACE_2 = 0xFFF7FBF8;
    private static final int BORDER = 0xFFD7E6DF;
    private static final int INK = 0xFF21342D;
    private static final int MUTED = 0xFF5F706A;
    private static final int TEXT_3 = 0xFF8B9994;
    private static final int PRIMARY = 0xFF1F9A8A;
    private static final int PRIMARY_SOFT = 0xFFD6F1EC;
    private static final int ACCENT = 0xFFF3A53A;
    private static final int ACCENT_SOFT = 0xFFFFF0CE;
    private static final int ROSE = 0xFFEF8A9B;
    private static final int BLUE = 0xFF85A9E9;
    private static final int VIOLET = 0xFFB899DD;
    private static final int GREEN = 0xFF82CDA6;
    private static final int YELLOW = 0xFFF6CF63;
    private static final int PEACH = 0xFFF5A77D;
    private static final int DANGER = 0xFFB54E4B;

    private BabyLogRepository repository;
    private BabyLogService service;
    private String activeTab = "home";
    private String pendingBackupJson;
    private String currentPhotoPath = "";
    private String currentPhotoName = "";
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = new BabyLogRepository(this);
        service = new BabyLogService(this, repository);
        render();
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
        BabyLogService.DashboardSnapshot dashboard = service.loadDashboard();
        content.addView(weekPanel(), matchWrapWithBottom(16));
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
        for (String label : labels) {
            TextView chip = label(label, 13, "全部".equals(label) ? 0xFFFFFFFF : MUTED, true);
            chip.setGravity(Gravity.CENTER);
            chip.setBackground(round("全部".equals(label) ? PRIMARY : SURFACE_2, 999, BORDER));
            chip.setPadding(dp(12), dp(8), dp(12), dp(8));
            filters.addView(chip, wrapWrapWithRight(8));
        }
        content.addView(filters, matchWrapWithBottom(14));
        content.addView(disclaimer("曲线和参考提示只用于家庭记录，不能替代医生判断。"), matchWrapWithBottom(14));

        List<BabyLogDomain.BabyLogEvent> events = service.listRecentEvents(50);
        if (events.isEmpty()) {
            content.addView(empty("暂无本地记录。快捷记录会先保存到本机，再等待同步上传。"));
            return;
        }
        for (BabyLogDomain.BabyLogEvent event : events) {
            content.addView(timelineRow(event), matchWrapWithBottom(10));
        }
    }

    private void renderLibrary(LinearLayout content) {
        List<BabyLogDomain.AttachmentRecord> attachments = service.listAttachmentsNewestFirst();
        int ultrasoundCount = 0;
        for (BabyLogDomain.AttachmentRecord attachment : attachments) {
            if ("ultrasound_image".equals(attachment.kind)) {
                ultrasoundCount += 1;
            }
        }
        content.addView(libraryItem("B 超单", ultrasoundCount + " 张", "已保存本机；OCR 待接入", R.drawable.ultrasound_sheet, view -> showAttachmentList("B 超单", attachments)), matchWrapWithBottom(12));
        content.addView(libraryItem("检查单", "0 张", "孕期常规检查、血检报告", R.drawable.baby_diary_notebook, null), matchWrapWithBottom(12));
        content.addView(libraryItem("出生证明", "0 张", "出生后启用", R.drawable.vaccine_card, null), matchWrapWithBottom(12));
        content.addView(libraryItem("疫苗本", "0 张", "出生后启用", R.drawable.vaccine_card, null), matchWrapWithBottom(12));
        content.addView(disclaimer("FGR / 成长曲线标准数据尚未落库；当前曲线只显示自有趋势。点击首页趋势卡进入全屏曲线。"));
    }

    private void renderSettings(LinearLayout content) {
        BabyLogService.DashboardSnapshot dashboard = service.loadDashboard();
        BabyLogDomain.BackendConfig settings = repository.loadSyncSettings();

        LinearLayout profile = settingsPanel("档案");
        profile.addView(settingRow("当前范围", "单胎 / 单宝宝", false, false));
        profile.addView(settingRow("预产期", "2026-08-05", false, false));
        profile.addView(settingRow("日界", "自然日 00:00", true, false));
        profile.addView(settingRow("夜间柔光", "跟随系统", true, false));
        content.addView(profile, matchWrapWithBottom(14));

        LinearLayout data = settingsPanel("数据");
        data.addView(actionRow("本地备份", "导出 JSON，含本机附件图片", "导出", view -> exportBackup()));
        data.addView(settingRow("本机用量", BabyLogFormatters.formatByteSize(dashboard.localBytes), false, false));
        data.addView(actionRow("从备份导入", "JSON", "导入", view -> importBackup()));
        data.addView(settingRow("清空本地数据", "", true, true));
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
        sync.addView(outlineButton("立即同步", view -> syncNow()), matchWrapWithBottom(8));
        sync.addView(label("当前所有记录先保存到本机；服务器地址只保存在当前手机，后续接入后端后再上传 pending 队列。", 12, MUTED, false));
        content.addView(sync, matchWrapWithBottom(14));

        LinearLayout about = settingsPanel("关于");
        about.addView(settingRow("版本", "0.1.0 · MVP", false, false));
        about.addView(settingRow("医疗免责声明", "", true, false));
        about.addView(settingRow("隐私说明", "", true, false));
        content.addView(about);
    }

    private void showQuickSheet() {
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(4), dp(8), dp(4), 0);
        final AlertDialog[] dialogRef = new AlertDialog[1];
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
                if (dialogRef[0] != null) {
                    dialogRef[0].dismiss();
                }
                handleQuickAction(action);
            });
            body.addView(row, matchWrapWithBottom(10));
        }
        dialogRef[0] = new AlertDialog.Builder(this)
                .setTitle("快捷记录")
                .setView(body)
                .setNegativeButton("关闭", null)
                .show();
    }

    private void handleQuickAction(BabyLogService.QuickAction action) {
        if ("ultrasound".equals(action.eventType)) {
            showUltrasoundForm();
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

    private void showUltrasoundForm() {
        currentPhotoPath = "";
        currentPhotoName = "";
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(4), dp(8), dp(4), 0);
        examDateInput = input("检查日期 yyyy-MM-dd", BabyLogFormatters.todayDateInput(), InputType.TYPE_CLASS_DATETIME);
        gestationalAgeInput = input("孕周，例如 28+3", "28+3", InputType.TYPE_CLASS_TEXT);
        bpdInput = input("双顶径 BPD mm", "", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        hcInput = input("头围 HC mm", "", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        acInput = input("腹围 AC mm", "", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        flInput = input("股骨长 FL mm", "", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        efwInput = input("估计胎重 EFW g", "", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        body.addView(examDateInput, matchWrapWithBottom(8));
        body.addView(gestationalAgeInput, matchWrapWithBottom(8));
        body.addView(bpdInput, matchWrapWithBottom(8));
        body.addView(hcInput, matchWrapWithBottom(8));
        body.addView(acInput, matchWrapWithBottom(8));
        body.addView(flInput, matchWrapWithBottom(8));
        body.addView(efwInput, matchWrapWithBottom(8));
        photoPreview = new ImageView(this);
        photoPreview.setAdjustViewBounds(true);
        photoPreview.setMaxHeight(dp(190));
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
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> saveUltrasound(dialog));
    }

    private void launchCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) == null) {
            Toast.makeText(this, "没有找到系统相机", Toast.LENGTH_SHORT).show();
            return;
        }
        startActivityForResult(intent, REQUEST_CAPTURE_ULTRASOUND);
    }

    private void pickImage() {
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
        if (BabyLogFormatters.parseGestationalAgeDays(gestationalAge) == null) {
            Toast.makeText(this, "请填写有效孕周，例如 28+3", Toast.LENGTH_SHORT).show();
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
            render();
        } catch (JSONException error) {
            Toast.makeText(this, "B 超保存失败：" + error.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            return;
        }
        if (requestCode == REQUEST_CAPTURE_ULTRASOUND) {
            saveCameraThumbnail(data);
        } else if (requestCode == REQUEST_PICK_ULTRASOUND && data != null && data.getData() != null) {
            copyPickedImage(data.getData());
        } else if (requestCode == REQUEST_EXPORT_BACKUP && data != null && data.getData() != null) {
            writeBackup(data.getData());
        } else if (requestCode == REQUEST_IMPORT_BACKUP && data != null && data.getData() != null) {
            readBackup(data.getData());
        }
    }

    private void saveCameraThumbnail(Intent data) {
        Object bitmapData = data == null || data.getExtras() == null ? null : data.getExtras().get("data");
        if (!(bitmapData instanceof Bitmap)) {
            Toast.makeText(this, "相机未返回图片", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            File file = service.createAttachmentFile("scan.jpg");
            try (FileOutputStream output = new FileOutputStream(file)) {
                ((Bitmap) bitmapData).compress(Bitmap.CompressFormat.JPEG, 90, output);
            }
            currentPhotoPath = file.getAbsolutePath();
            currentPhotoName = file.getName();
            if (photoPreview != null) {
                photoPreview.setImageBitmap(BitmapFactory.decodeFile(currentPhotoPath));
            }
            Toast.makeText(this, "照片已保存到本机", Toast.LENGTH_SHORT).show();
        } catch (IOException error) {
            Toast.makeText(this, "保存照片失败：" + error.getMessage(), Toast.LENGTH_LONG).show();
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
            Toast.makeText(this, "备份已生成", Toast.LENGTH_SHORT).show();
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
            render();
        } catch (JSONException | IOException error) {
            Toast.makeText(this, "导入失败：" + error.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void saveSyncSettings() {
        try {
            BabyLogDomain.BackendConfig saved = repository.saveSyncSettings(new BabyLogDomain.BackendConfig(
                    true,
                    text(syncUrlInput),
                    text(syncRegionInput),
                    null
            ));
            Toast.makeText(this, saved.enabled ? "同步配置已保存到本机" : "同步配置已清空，当前为本机模式", Toast.LENGTH_SHORT).show();
            render();
        } catch (JSONException error) {
            Toast.makeText(this, "保存同步配置失败：" + error.getMessage(), Toast.LENGTH_LONG).show();
        }
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
            if (!"ultrasound_image".equals(attachment.kind)) {
                continue;
            }
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
        LinearLayout row = panel(12, SURFACE);
        row.setOrientation(LinearLayout.VERTICAL);
        row.addView(label(BabyLogFormatters.formatEventDay(event.occurredAt) + " " + BabyLogFormatters.formatEventTime(event.occurredAt) + " · " + BabyLogFormatters.eventLabel(event.eventType), 13, MUTED, true));
        row.addView(label(BabyLogFormatters.eventSummary(event), 15, INK, false));
        if (!event.attachmentIds.isEmpty()) {
            TextView tag = label("附件 " + event.attachmentIds.size(), 12, PRIMARY, true);
            tag.setPadding(0, dp(6), 0, 0);
            row.addView(tag);
        }
        return row;
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
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(10), 0, dp(10));
        row.addView(label(label, 14, destructive ? DANGER : MUTED, false), weightWrap(1f));
        row.addView(label(value, 14, destructive ? DANGER : INK, true));
        if (interactive) {
            row.addView(label(" >", 14, destructive ? DANGER : TEXT_3, true));
        }
        return row;
    }

    private View actionRow(String title, String subtitle, String action, View.OnClickListener listener) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(10), 0, dp(10));
        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.addView(label(title, 14, INK, true));
        copy.addView(label(subtitle, 12, MUTED, false));
        row.addView(copy, weightWrap(1f));
        row.addView(primaryButton(action, listener), new LinearLayout.LayoutParams(dp(84), dp(44)));
        return row;
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

    private Button primaryButton(String text, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextColor(0xFFFFFFFF);
        button.setBackground(round(PRIMARY, 999, 0));
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
        ImageView image = new ImageView(this);
        image.setImageResource(resId);
        image.setAdjustViewBounds(true);
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
}
