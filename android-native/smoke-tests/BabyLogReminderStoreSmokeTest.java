import app.babylog.nativeapp.BabyLogDomain;
import app.babylog.nativeapp.BabyLogFormatters;
import app.babylog.nativeapp.BabyLogReminderStore;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class BabyLogReminderStoreSmokeTest {
    public static void main(String[] args) throws Exception {
        String today = BabyLogFormatters.todayDateInput();
        String expectedDueDate = BabyLogFormatters.offsetDateInput(today, 140);
        BabyLogDomain.ChildProfile pregnancy = BabyLogDomain.ChildProfile.createForNewFamily(
                "栗子",
                "female",
                expectedDueDate,
                "",
                BabyLogDomain.STAGE_PREGNANCY,
                true
        );

        String nextVisitDate = BabyLogFormatters.offsetDateInput(today, 7);
        BabyLogDomain.BabyLogEvent checkup = BabyLogDomain.createEvent(
                "pregnancy_checkup",
                today + "T12:00:00.000+0800",
                new JSONObject()
                        .put("nextVisitDate", nextVisitDate)
                        .put("summary", "产检"),
                Collections.emptyList(),
                "manual"
        );

        List<BabyLogReminderStore.Reminder> reminders = BabyLogReminderStore.generateSystemReminders(
                pregnancy,
                Collections.singletonList(checkup)
        );
        assertContainsKind(reminders, BabyLogReminderStore.KIND_CHECKUP_TODO);
        assertContainsKind(reminders, BabyLogReminderStore.KIND_SCREENING_WINDOW);
        assertContainsKind(reminders, BabyLogReminderStore.KIND_BACKUP);
        assertContainsTitle(reminders, "3 天后产检");
        assertNoBannedWords(reminders);

        BabyLogDomain.ChildProfile fetalObservationProfile = BabyLogDomain.ChildProfile.createForNewFamily(
                "栗子",
                "female",
                BabyLogFormatters.offsetDateInput(today, 84),
                "",
                BabyLogDomain.STAGE_PREGNANCY,
                true
        );
        List<BabyLogReminderStore.Reminder> fetalObservationReminders =
                BabyLogReminderStore.generateSystemReminders(fetalObservationProfile, Collections.singletonList(checkup));
        assertContainsTitle(fetalObservationReminders, "今天可以观察一下宝宝的活动模式");
        assertNoBannedWords(fetalObservationReminders);

        BabyLogReminderStore.Reminder first = reminders.get(0);
        assertTrue(BabyLogReminderStore.isActionable(first));
        BabyLogReminderStore.Reminder completed = new BabyLogReminderStore.Reminder(
                first.id,
                first.kind,
                first.title,
                first.note,
                first.dueAtIso,
                first.cronLite,
                first.source,
                true,
                "",
                BabyLogFormatters.nowIso(),
                first.createdAt,
                BabyLogFormatters.nowIso()
        );
        assertFalse(BabyLogReminderStore.isActionable(completed));

        BabyLogDomain.ChildProfile ended = BabyLogDomain.ChildProfile.createForNewFamily(
                "栗子",
                "female",
                expectedDueDate,
                "",
                BabyLogDomain.STAGE_PREGNANCY_ENDED,
                true
        );
        BabyLogDomain.ChildProfile paused = BabyLogDomain.ChildProfile.createForNewFamily(
                "栗子",
                "female",
                expectedDueDate,
                "",
                BabyLogDomain.STAGE_PAUSED,
                true
        );
        assertTrue(BabyLogReminderStore.isSystemMuted(ended));
        assertTrue(BabyLogReminderStore.isSystemMuted(paused));
        assertEquals(0, BabyLogReminderStore.generateSystemReminders(ended, Collections.singletonList(checkup)).size());

        BabyLogDomain.BabyLogEvent pastCheckup = BabyLogDomain.createEvent(
                "pregnancy_checkup",
                today + "T12:00:00.000+0800",
                new JSONObject()
                        .put("nextVisitDate", BabyLogFormatters.offsetDateInput(today, -2))
                        .put("summary", "历史产检"),
                Collections.emptyList(),
                "manual"
        );
        List<BabyLogReminderStore.Reminder> pastReminders = BabyLogReminderStore.generateSystemReminders(
                pregnancy,
                Collections.singletonList(pastCheckup)
        );
        assertNoKind(pastReminders, BabyLogReminderStore.KIND_CHECKUP_TODO);

        BabyLogReminderStore.Reminder systemReminder = new BabyLogReminderStore.Reminder(
                "sys_checkup_2026-05-20_0",
                BabyLogReminderStore.KIND_CHECKUP_TODO,
                "今天有产检安排",
                "",
                today + "T09:00:00.000+0800",
                "",
                BabyLogReminderStore.SOURCE_SYSTEM,
                true,
                "",
                "",
                BabyLogFormatters.nowIso(),
                BabyLogFormatters.nowIso()
        );
        BabyLogReminderStore.Reminder userReminder = new BabyLogReminderStore.Reminder(
                "rem_custom",
                BabyLogReminderStore.KIND_USER_CUSTOM,
                "自定义提醒",
                "",
                today + "T09:00:00.000+0800",
                "",
                BabyLogReminderStore.SOURCE_USER,
                true,
                "",
                "",
                BabyLogFormatters.nowIso(),
                BabyLogFormatters.nowIso()
        );
        List<BabyLogReminderStore.Reminder> mutedMerge = BabyLogReminderStore.mergeSystemReminders(
                Arrays.asList(systemReminder, userReminder),
                Collections.emptyList(),
                true
        );
        assertEquals(1, mutedMerge.size());
        assertEquals(BabyLogReminderStore.SOURCE_USER, mutedMerge.get(0).source);
    }

    private static void assertContainsKind(List<BabyLogReminderStore.Reminder> reminders, String kind) {
        for (BabyLogReminderStore.Reminder reminder : reminders) {
            if (kind.equals(reminder.kind)) {
                return;
            }
        }
        throw new AssertionError("expected reminder kind " + kind);
    }

    private static void assertContainsTitle(List<BabyLogReminderStore.Reminder> reminders, String title) {
        for (BabyLogReminderStore.Reminder reminder : reminders) {
            if (title.equals(reminder.title)) {
                return;
            }
        }
        throw new AssertionError("expected reminder title " + title);
    }

    private static void assertNoKind(List<BabyLogReminderStore.Reminder> reminders, String kind) {
        for (BabyLogReminderStore.Reminder reminder : reminders) {
            if (kind.equals(reminder.kind)) {
                throw new AssertionError("unexpected reminder kind " + kind + ": " + reminder.title);
            }
        }
    }

    private static void assertNoBannedWords(List<BabyLogReminderStore.Reminder> reminders) {
        String[] banned = {"流产", "终止", "胎死", "异常", "危险", "严重", "必须立刻", "过期"};
        for (BabyLogReminderStore.Reminder reminder : reminders) {
            String text = reminder.title + "\n" + reminder.note;
            for (String word : banned) {
                if (text.contains(word)) {
                    throw new AssertionError("reminder text contains banned word " + word + ": " + text);
                }
            }
        }
    }

    private static void assertEquals(Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("expected " + expected + " but got " + actual);
        }
    }

    private static void assertTrue(boolean value) {
        if (!value) {
            throw new AssertionError("expected true");
        }
    }

    private static void assertFalse(boolean value) {
        if (value) {
            throw new AssertionError("expected false");
        }
    }
}
