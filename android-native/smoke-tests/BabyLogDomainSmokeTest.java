import app.babylog.nativeapp.BabyLogDomain;

public final class BabyLogDomainSmokeTest {
    public static void main(String[] args) {
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
