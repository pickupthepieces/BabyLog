import static app.babylog.nativeapp.SmokeAssert.*;

import app.babylog.nativeapp.BabyLogDomain;
import app.babylog.nativeapp.BabyLogSyncProtocol;

import org.json.JSONObject;

public final class BabyLogSyncProtocolSmokeTest {
    public static void main(String[] args) throws Exception {
        assertEquals("family_profiles", BabyLogSyncProtocol.collectionForEntityType("familyProfile"));
        assertEquals("child_profiles", BabyLogSyncProtocol.collectionForEntityType("childProfile"));
        assertEquals("family_members", BabyLogSyncProtocol.collectionForEntityType("familyMember"));
        assertEquals("events", BabyLogSyncProtocol.collectionForEntityType("event"));
        assertEquals("attachments", BabyLogSyncProtocol.collectionForEntityType("attachment"));

        assertEquals(true, BabyLogSyncProtocol.isSyncableEntityType("event"));
        assertEquals(false, BabyLogSyncProtocol.isSyncableEntityType("smartConfig"));
        assertEquals(false, BabyLogSyncProtocol.isSyncableEntityType("reminder"));
        assertEquals(false, BabyLogSyncProtocol.isSyncableEntityType("preVisitQuestion"));
        assertEquals(false, BabyLogSyncProtocol.isSyncableEntityType("disclaimerConsent"));

        JSONObject eventPayload = new JSONObject()
                .put("id", "evt_1")
                .put("familyId", BabyLogDomain.FAMILY_ID)
                .put("childId", BabyLogDomain.CHILD_ID)
                .put("eventType", "ultrasound")
                .put("updatedAt", "2026-05-21T09:30:00.000+0800");
        JSONObject envelope = BabyLogSyncProtocol.wrapRecord("event", "evt_1", eventPayload);
        assertEquals("events", envelope.getString("collection"));
        assertEquals("event", envelope.getString("entityType"));
        assertEquals("evt_1", envelope.getString("entityId"));
        assertEquals(BabyLogDomain.FAMILY_ID, envelope.getString("familyId"));
        assertEquals(BabyLogDomain.CHILD_ID, envelope.getString("childId"));
        assertEquals("2026-05-21T09:30:00.000+0800", envelope.getString("updatedAt"));
        assertEquals("ultrasound", envelope.getJSONObject("payload").getString("eventType"));

        expectIllegalArgument(new Runnable() {
            @Override
            public void run() {
                BabyLogSyncProtocol.collectionForEntityType("smartConfig");
            }
        });
        expectIllegalArgument(new Runnable() {
            @Override
            public void run() {
                try {
                    BabyLogSyncProtocol.wrapRecord("reminder", "rem_1", new JSONObject());
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                }
            }
        });

        String normalized = BabyLogSyncProtocol.normalizeFamilyKeyForTransport("  abc  ");
        assertEquals("abc", normalized);
        assertEquals(false, BabyLogSyncProtocol.hasFamilyKey("   "));
        assertEquals(true, BabyLogSyncProtocol.hasFamilyKey("family-secret"));
    }

    private static void expectIllegalArgument(Runnable runnable) {
        try {
            runnable.run();
        } catch (IllegalArgumentException expected) {
            return;
        } catch (RuntimeException wrapped) {
            Throwable cause = wrapped.getCause();
            if (cause instanceof IllegalArgumentException) {
                return;
            }
            throw wrapped;
        }
        throw new AssertionError("Expected IllegalArgumentException");
    }

}
