import app.babylog.nativeapp.BabyLogDomain;
import app.babylog.nativeapp.BabyLogRepository;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class BabyLogRepositoryStorageSmokeTest {
    public static void main(String[] args) throws Exception {
        BabyLogRepository repository = BabyLogRepository.forSmokeTest();
        BabyLogDomain.BabyLogEvent event = BabyLogDomain.createEvent(
                "maternal_metric",
                "2026-06-09T07:40:00.000+0800",
                new JSONObject().put("weightKg", "59.6"),
                Collections.emptyList(),
                "manual"
        );
        BabyLogDomain.AttachmentRecord attachment = BabyLogDomain.AttachmentRecord.fromJson(
                new JSONObject()
                        .put("id", "attachment_storage_smoke")
                        .put("familyId", BabyLogDomain.FAMILY_ID)
                        .put("childId", BabyLogDomain.CHILD_ID)
                        .put("kind", "checkup_file")
                        .put("originalName", "storage-smoke.pdf")
                        .put("mimeType", "application/pdf")
                        .put("byteSize", 128)
                        .put("localPath", "")
                        .put("ocrStatus", "ready")
                        .put("createdAt", event.createdAt)
                        .put("updatedAt", event.updatedAt)
                        .put("updatedBy", BabyLogDomain.UPDATED_BY_LOCAL)
                        .put("schemaVersion", BabyLogDomain.SCHEMA_VERSION)
        );
        BabyLogDomain.SyncChange change = BabyLogDomain.createSyncChange("event", event.id, "upsert");

        assertTrue(repository.putEventWithAttachmentsAndSyncChanges(event, Arrays.asList(attachment), Arrays.asList(change)));
        assertEquals(event.id, repository.findEventById(event.id).id);
        assertEquals(1, repository.listEvents().size());
        assertEquals(1, repository.listAttachments().size());
        assertEquals(1, repository.listSyncChanges().size());
        assertEquals(1, repository.exportEvents().length());
        assertEquals(1, repository.exportAttachments().length());

        repository.putEvent(eventAt("2026-06-09T09:00:00.000+0800"));
        repository.putEvent(eventAt("2026-06-09T10:00:00.000+0800"));
        repository.putEvent(eventAt("2026-06-09T11:00:00.000+0800"));
        List<BabyLogDomain.BabyLogEvent> secondPage = repository.listEvents(2, 1);
        assertEquals(2, secondPage.size());
        assertEquals("2026-06-09T10:00:00.000+0800", secondPage.get(0).occurredAt);
        assertEquals("2026-06-09T09:00:00.000+0800", secondPage.get(1).occurredAt);
        assertEquals(0, repository.listEvents(0, 0).size());

        repository.importData(
                new JSONArray(),
                new JSONArray(),
                new JSONArray(),
                new JSONArray().put(event.withDeletedAt("2026-06-09T08:00:00.000+0800").toJson()),
                new JSONArray(),
                new JSONArray()
        );
        assertEquals(0, repository.listEvents().size());
        assertEquals(1, repository.listDeletedEvents().size());
        repository.clearLocalData();
        assertEquals(0, repository.exportEvents().length());
        assertEquals(0, repository.exportAttachments().length());
        assertEquals(0, repository.exportSyncChanges().length());
    }

    private static BabyLogDomain.BabyLogEvent eventAt(String occurredAt) throws Exception {
        return BabyLogDomain.createEvent(
                "maternal_metric",
                occurredAt,
                new JSONObject().put("weightKg", "59.6"),
                Collections.emptyList(),
                "manual"
        );
    }

    private static void assertTrue(boolean value) {
        if (!value) {
            throw new AssertionError("expected true");
        }
    }

    private static void assertEquals(Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("expected <" + expected + "> but was <" + actual + ">");
        }
    }
}
