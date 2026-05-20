package app.babylog.nativeapp;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class BabyLogReminderStore {
    public static final String PREF_FILE_NAME = "babylog_reminders";
    private static final String REMINDERS_KEY = "reminders";

    public static final String KIND_CHECKUP_TODO = "CHECKUP_TODO";
    public static final String KIND_SCREENING_WINDOW = "SCREENING_WINDOW";
    public static final String KIND_FETAL_OBSERVATION_HINT = "FETAL_OBSERVATION_HINT";
    public static final String KIND_BACKUP = "BACKUP";
    public static final String KIND_USER_CUSTOM = "USER_CUSTOM";
    public static final String SOURCE_SYSTEM = "SYSTEM";
    public static final String SOURCE_USER = "USER";

    private final Context appContext;

    public BabyLogReminderStore(Context context) {
        appContext = context.getApplicationContext();
    }

    public List<Reminder> listReminders() {
        return parseReminderArray(readArray());
    }

    public Reminder findReminder(String id) {
        if (isBlank(id)) {
            return null;
        }
        for (Reminder reminder : listReminders()) {
            if (id.equals(reminder.id)) {
                return reminder;
            }
        }
        return null;
    }

    public Reminder saveUserReminder(String id, String title, String note, String dueAtIso, boolean enabled) throws JSONException {
        String cleanTitle = title == null ? "" : title.trim();
        if (cleanTitle.isEmpty()) {
            throw new JSONException("reminder title is blank");
        }
        String targetId = isBlank(id) ? "rem_" + UUID.randomUUID() : id.trim();
        String now = BabyLogFormatters.nowIso();
        Reminder existing = findReminder(targetId);
        Reminder reminder = new Reminder(
                targetId,
                KIND_USER_CUSTOM,
                cleanTitle,
                note == null ? "" : note.trim(),
                dueAtIso == null ? "" : dueAtIso.trim(),
                "",
                SOURCE_USER,
                enabled,
                existing == null ? "" : existing.dismissedAt,
                existing == null ? "" : existing.completedAt,
                existing == null ? now : existing.createdAt,
                now
        );
        upsert(reminder);
        return reminder;
    }

    public void setEnabled(String id, boolean enabled) {
        updateStatus(id, enabled, null, null);
    }

    public void dismiss(String id) {
        updateStatus(id, null, BabyLogFormatters.nowIso(), null);
    }

    public void complete(String id) {
        updateStatus(id, null, null, BabyLogFormatters.nowIso());
    }

    public void delete(String id) {
        if (isBlank(id)) {
            return;
        }
        JSONArray updated = new JSONArray();
        for (Reminder reminder : listReminders()) {
            if (!id.equals(reminder.id)) {
                try {
                    updated.put(reminder.toJson());
                } catch (JSONException ignored) {
                    // Generated models are valid; skip defensively.
                }
            }
        }
        preferences().edit().putString(REMINDERS_KEY, updated.toString()).commit();
    }

    public void syncSystemReminders(BabyLogDomain.ChildProfile profile, List<BabyLogDomain.BabyLogEvent> events) throws JSONException {
        if (isSystemMuted(profile)) {
            return;
        }
        List<Reminder> generated = generateSystemReminders(profile, events);
        Map<String, Reminder> current = new HashMap<>();
        for (Reminder reminder : listReminders()) {
            current.put(reminder.id, reminder);
        }
        JSONArray updated = new JSONArray();
        Set<String> generatedIds = new HashSet<>();
        for (Reminder generatedReminder : generated) {
            generatedIds.add(generatedReminder.id);
            Reminder existing = current.get(generatedReminder.id);
            Reminder next = existing == null ? generatedReminder : generatedReminder.withLocalStatus(existing);
            updated.put(next.toJson());
        }
        for (Reminder reminder : current.values()) {
            if (!SOURCE_SYSTEM.equals(reminder.source) || generatedIds.contains(reminder.id)) {
                if (!generatedIds.contains(reminder.id)) {
                    updated.put(reminder.toJson());
                }
            }
        }
        preferences().edit().putString(REMINDERS_KEY, updated.toString()).commit();
    }

    public static List<Reminder> generateSystemReminders(
            BabyLogDomain.ChildProfile profile,
            List<BabyLogDomain.BabyLogEvent> events
    ) {
        List<Reminder> reminders = new ArrayList<>();
        if (profile == null || !BabyLogDomain.STAGE_PREGNANCY.equals(BabyLogFormatters.resolveCareStage(profile, BabyLogFormatters.todayDateInput()))) {
            return reminders;
        }
        String today = BabyLogFormatters.todayDateInput();
        Set<String> completedTypes = new HashSet<>();
        if (events != null) {
            for (BabyLogDomain.BabyLogEvent event : events) {
                if (event != null && event.deletedAt == null && !isBlank(event.eventType)) {
                    completedTypes.add(event.eventType);
                }
            }
        }
        addCheckupReminders(reminders, events);
        addScreeningWindowReminders(reminders, profile, completedTypes, today);
        int gestationalDays = gestationalDays(profile);
        if (gestationalDays >= 28 * 7) {
            reminders.add(systemReminder(
                    "sys_fetal_observation_" + today,
                    KIND_FETAL_OBSERVATION_HINT,
                    "今天可以观察一下宝宝的活动模式",
                    "记录本次会话用时与次数，与你以往模式比较。",
                    dueIso(today, 9, 0),
                    "daily"
            ));
        }
        reminders.add(systemReminder(
                "sys_backup_" + today.substring(0, 7),
                KIND_BACKUP,
                "可以做一次本地备份",
                "备份由你手动导出到本机文件。",
                dueIso(today, 10, 0),
                "monthly"
        ));
        return reminders;
    }

    public static boolean isSystemMuted(BabyLogDomain.ChildProfile profile) {
        return profile != null && (
                BabyLogDomain.STAGE_PREGNANCY_ENDED.equals(profile.stageOverride)
                        || BabyLogDomain.STAGE_PAUSED.equals(profile.stageOverride)
        );
    }

    public static boolean isActionable(Reminder reminder) {
        return reminder != null
                && reminder.enabled
                && isBlank(reminder.dismissedAt)
                && isBlank(reminder.completedAt);
    }

    private static void addCheckupReminders(List<Reminder> reminders, List<BabyLogDomain.BabyLogEvent> events) {
        if (events == null) {
            return;
        }
        Set<String> dates = new HashSet<>();
        for (BabyLogDomain.BabyLogEvent event : events) {
            if (event == null || !"pregnancy_checkup".equals(event.eventType) || event.payload == null) {
                continue;
            }
            String nextVisitDate = event.payload.optString("nextVisitDate", "").trim();
            if (BabyLogFormatters.isValidDateInput(nextVisitDate)) {
                dates.add(nextVisitDate);
            }
        }
        for (String date : dates) {
            addCheckupReminder(reminders, date, -3, "3 天后产检");
            addCheckupReminder(reminders, date, -1, "1 天后产检");
            addCheckupReminder(reminders, date, 0, "今天有产检安排");
        }
    }

    private static void addCheckupReminder(List<Reminder> reminders, String checkupDate, int dayOffset, String title) {
        String dueDate = BabyLogFormatters.offsetDateInput(checkupDate, dayOffset);
        reminders.add(systemReminder(
                String.format(Locale.US, "sys_checkup_%s_%d", checkupDate, dayOffset),
                KIND_CHECKUP_TODO,
                title,
                "来自产检记录的下次产检日期：" + checkupDate,
                dueIso(dueDate, 9, 0),
                ""
        ));
    }

    private static void addScreeningWindowReminders(
            List<Reminder> reminders,
            BabyLogDomain.ChildProfile profile,
            Set<String> completedTypes,
            String today
    ) {
        int days = gestationalDays(profile);
        addScreeningIfInWindow(reminders, completedTypes, days, "screening_nt", 11, 13, 6, "NT", "11-13+6 周", today);
        addScreeningIfInWindow(reminders, completedTypes, days, "screening_serum", 15, 20, 6, "唐筛", "15-20+6 周", today);
        addScreeningIfInWindow(reminders, completedTypes, days, "screening_nipt", 12, 22, 6, "无创 DNA", "约 12-22 周", today);
        addScreeningIfInWindow(reminders, completedTypes, days, "screening_anomaly", 20, 24, 0, "大排畸 / 系统超声", "20-24 周", today);
        addScreeningIfInWindow(reminders, completedTypes, days, "screening_ogtt", 24, 28, 0, "糖耐 OGTT", "24-28 周", today);
        addScreeningIfInWindow(reminders, completedTypes, days, "screening_gbs", 35, 37, 0, "GBS", "35-37 周", today);
    }

    private static void addScreeningIfInWindow(
            List<Reminder> reminders,
            Set<String> completedTypes,
            int gestationalDays,
            String eventType,
            int startWeek,
            int endWeek,
            int endExtraDays,
            String label,
            String window,
            String today
    ) {
        int start = startWeek * 7;
        int end = endWeek * 7 + endExtraDays;
        if (gestationalDays < start || gestationalDays > end || completedTypes.contains(eventType)) {
            return;
        }
        reminders.add(systemReminder(
                "sys_screening_" + eventType,
                KIND_SCREENING_WINDOW,
                label + "窗口可与医生确认安排",
                window + "（参考）",
                dueIso(today, 9, 30),
                "byGestationalWeek"
        ));
    }

    private static int gestationalDays(BabyLogDomain.ChildProfile profile) {
        if (profile == null || !BabyLogFormatters.isValidDateInput(profile.expectedDueDate)) {
            return 0;
        }
        return 280 - BabyLogFormatters.daysBetweenDateInputs(BabyLogFormatters.todayDateInput(), profile.expectedDueDate);
    }

    private static Reminder systemReminder(
            String id,
            String kind,
            String title,
            String note,
            String dueAtIso,
            String cronLite
    ) {
        String now = BabyLogFormatters.nowIso();
        return new Reminder(id, kind, title, note, dueAtIso, cronLite, SOURCE_SYSTEM, true, "", "", now, now);
    }

    private void updateStatus(String id, Boolean enabled, String dismissedAt, String completedAt) {
        Reminder reminder = findReminder(id);
        if (reminder == null) {
            return;
        }
        try {
            upsert(new Reminder(
                    reminder.id,
                    reminder.kind,
                    reminder.title,
                    reminder.note,
                    reminder.dueAtIso,
                    reminder.cronLite,
                    reminder.source,
                    enabled == null ? reminder.enabled : enabled,
                    dismissedAt == null ? reminder.dismissedAt : dismissedAt,
                    completedAt == null ? reminder.completedAt : completedAt,
                    reminder.createdAt,
                    BabyLogFormatters.nowIso()
            ));
        } catch (JSONException ignored) {
        }
    }

    private void upsert(Reminder reminder) throws JSONException {
        JSONArray updated = new JSONArray();
        boolean replaced = false;
        for (Reminder current : listReminders()) {
            if (reminder.id.equals(current.id)) {
                updated.put(reminder.toJson());
                replaced = true;
            } else {
                updated.put(current.toJson());
            }
        }
        if (!replaced) {
            updated.put(reminder.toJson());
        }
        preferences().edit().putString(REMINDERS_KEY, updated.toString()).commit();
    }

    private JSONArray readArray() {
        try {
            return new JSONArray(preferences().getString(REMINDERS_KEY, "[]"));
        } catch (JSONException ignored) {
            return new JSONArray();
        }
    }

    private SharedPreferences preferences() {
        return appContext.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE);
    }

    private static List<Reminder> parseReminderArray(JSONArray array) {
        List<Reminder> reminders = new ArrayList<>();
        if (array == null) {
            return reminders;
        }
        for (int i = 0; i < array.length(); i++) {
            Reminder reminder = Reminder.fromJson(array.optJSONObject(i));
            if (reminder != null) {
                reminders.add(reminder);
            }
        }
        return reminders;
    }

    private static String dueIso(String date, int hour, int minute) {
        if (!BabyLogFormatters.isValidDateInput(date)) {
            date = BabyLogFormatters.todayDateInput();
        }
        return String.format(Locale.US, "%sT%02d:%02d:00.000+0800", date, hour, minute);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static final class Reminder {
        public final String id;
        public final String kind;
        public final String title;
        public final String note;
        public final String dueAtIso;
        public final String cronLite;
        public final String source;
        public final boolean enabled;
        public final String dismissedAt;
        public final String completedAt;
        public final String createdAt;
        public final String updatedAt;

        public Reminder(
                String id,
                String kind,
                String title,
                String note,
                String dueAtIso,
                String cronLite,
                String source,
                boolean enabled,
                String dismissedAt,
                String completedAt,
                String createdAt,
                String updatedAt
        ) {
            this.id = id == null ? "" : id;
            this.kind = kind == null ? KIND_USER_CUSTOM : kind;
            this.title = title == null ? "" : title;
            this.note = note == null ? "" : note;
            this.dueAtIso = dueAtIso == null ? "" : dueAtIso;
            this.cronLite = cronLite == null ? "" : cronLite;
            this.source = source == null ? SOURCE_USER : source;
            this.enabled = enabled;
            this.dismissedAt = dismissedAt == null ? "" : dismissedAt;
            this.completedAt = completedAt == null ? "" : completedAt;
            this.createdAt = createdAt == null ? "" : createdAt;
            this.updatedAt = updatedAt == null ? "" : updatedAt;
        }

        Reminder withLocalStatus(Reminder existing) {
            return new Reminder(
                    id,
                    kind,
                    title,
                    note,
                    dueAtIso,
                    cronLite,
                    source,
                    existing.enabled,
                    existing.dismissedAt,
                    existing.completedAt,
                    existing.createdAt,
                    BabyLogFormatters.nowIso()
            );
        }

        JSONObject toJson() throws JSONException {
            return new JSONObject()
                    .put("id", id)
                    .put("kind", kind)
                    .put("title", title)
                    .put("note", note)
                    .put("dueAtIso", dueAtIso)
                    .put("cronLite", cronLite)
                    .put("source", source)
                    .put("enabled", enabled)
                    .put("dismissedAt", dismissedAt)
                    .put("completedAt", completedAt)
                    .put("createdAt", createdAt)
                    .put("updatedAt", updatedAt);
        }

        static Reminder fromJson(JSONObject json) {
            if (json == null) {
                return null;
            }
            String id = json.optString("id", "").trim();
            String title = json.optString("title", "").trim();
            if (id.isEmpty() || title.isEmpty()) {
                return null;
            }
            return new Reminder(
                    id,
                    json.optString("kind", KIND_USER_CUSTOM),
                    title,
                    json.optString("note", ""),
                    json.optString("dueAtIso", ""),
                    json.optString("cronLite", ""),
                    json.optString("source", SOURCE_USER),
                    json.optBoolean("enabled", true),
                    json.optString("dismissedAt", ""),
                    json.optString("completedAt", ""),
                    json.optString("createdAt", ""),
                    json.optString("updatedAt", "")
            );
        }
    }
}
