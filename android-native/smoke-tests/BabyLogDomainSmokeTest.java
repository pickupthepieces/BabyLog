import app.babylog.nativeapp.BabyLogDomain;
import app.babylog.nativeapp.BabyLogService;

import java.util.Arrays;

public final class BabyLogDomainSmokeTest {
    public static void main(String[] args) throws Exception {
        BabyLogDomain.FamilyProfile family = BabyLogDomain.FamilyProfile.localDefault();
        assertEquals(BabyLogDomain.FAMILY_ID, family.id);
        assertEquals("我的家庭", family.name);
        assertEquals("active", family.status);

        BabyLogDomain.ChildProfile child = BabyLogDomain.ChildProfile.createForNewFamily(
                "栗子",
                "female",
                "2026-08-05",
                "",
                "pregnancy",
                true
        );
        assertEquals("栗子", child.nickname);
        assertEquals("female", child.sex);
        assertEquals("2026-08-05", child.expectedDueDate);
        assertEquals("", child.birthDate);
        assertEquals("pregnancy", child.stageOverride);
        assertEquals(true, child.setupCompleted);

        BabyLogDomain.FamilyMember manager = BabyLogDomain.FamilyMember.localManager();
        assertEquals(BabyLogDomain.LOCAL_MEMBER_ID, manager.id);
        assertEquals("manager", manager.role);
        assertEquals("active", manager.status);

        if (!contains(BabyLogDomain.EVENT_TYPES, "maternal_metric")) {
            throw new AssertionError("maternal_metric should be a first-class event type");
        }

        BabyLogDomain.BabyLogEvent event = new BabyLogDomain.BabyLogEvent(
                "evt_test",
                BabyLogDomain.FAMILY_ID,
                BabyLogDomain.CHILD_ID,
                "fetal_movement",
                "2026-05-18T20:17:00.000+0800",
                null,
                Arrays.asList("att_1"),
                "manual",
                "2026-05-18T20:00:00.000+0800",
                "2026-05-18T20:00:00.000+0800",
                BabyLogDomain.UPDATED_BY_LOCAL,
                BabyLogDomain.SCHEMA_VERSION,
                null
        );
        BabyLogDomain.BabyLogEvent deleted = event.withDeletedAt("2026-05-18T20:30:00.000+0800");
        assertEquals(event.id, deleted.id);
        assertEquals(event.eventType, deleted.eventType);
        assertEquals(event.occurredAt, deleted.occurredAt);
        assertEquals(1, deleted.attachmentIds.size());
        assertEquals("att_1", deleted.attachmentIds.get(0));
        assertEquals("2026-05-18T20:30:00.000+0800", deleted.deletedAt);
        assertEquals("2026-05-18T20:30:00.000+0800", deleted.updatedAt);

        BabyLogDomain.BabyLogEvent restored = deleted.withRestoredAt("2026-05-18T20:35:00.000+0800");
        assertEquals(event.id, restored.id);
        assertEquals(event.eventType, restored.eventType);
        assertEquals(null, restored.deletedAt);
        assertEquals("2026-05-18T20:35:00.000+0800", restored.updatedAt);

        assertEquals(false, BabyLogService.isTrashExpired("2026-05-18T20:30:00.000+0800", "2026-05-25T20:29:59.000+0800"));
        assertEquals(true, BabyLogService.isTrashExpired("2026-05-18T20:30:00.000+0800", "2026-05-25T20:30:00.000+0800"));
    }

    private static void assertEquals(Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("expected " + expected + " but got " + actual);
        }
    }

    private static boolean contains(String[] values, String expected) {
        for (String value : values) {
            if (expected.equals(value)) {
                return true;
            }
        }
        return false;
    }
}
