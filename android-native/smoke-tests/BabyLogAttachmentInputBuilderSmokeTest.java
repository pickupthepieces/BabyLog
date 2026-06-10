import static app.babylog.nativeapp.SmokeAssert.*;

import app.babylog.nativeapp.BabyLogAttachmentInputBuilder;
import app.babylog.nativeapp.BabyLogDomain;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;

public final class BabyLogAttachmentInputBuilderSmokeTest {
    public static void main(String[] args) throws Exception {
        assertAttachmentIdsPreserveOrder();
        assertMissingLocalFileIsMarkedForBackup();
        assertValidateAttachmentsRequiresBlobForActiveAttachment();
    }

    private static void assertAttachmentIdsPreserveOrder() throws Exception {
        BabyLogDomain.AttachmentRecord first = BabyLogDomain.createAttachment(
                "document_image", "a.jpg", "image/jpeg", 1, "missing-a.jpg");
        BabyLogDomain.AttachmentRecord second = BabyLogDomain.createAttachment(
                "document_image", "b.jpg", "image/jpeg", 1, "missing-b.jpg");

        List<String> ids = BabyLogAttachmentInputBuilder.attachmentIdsFromRecords(Arrays.asList(first, null, second));

        assertEquals(2, ids.size());
        assertEquals(first.id, ids.get(0));
        assertEquals(second.id, ids.get(1));
    }

    private static void assertMissingLocalFileIsMarkedForBackup() throws Exception {
        BabyLogDomain.AttachmentRecord attachment = BabyLogDomain.createAttachment(
                "document_image", "missing.jpg", "image/jpeg", 1, "does-not-exist.jpg");
        JSONArray attachments = new JSONArray().put(attachment.toJson());

        JSONArray sanitized = BabyLogAttachmentInputBuilder.sanitizeAttachmentsForBackup(
                attachments,
                "2026-05-25T10:00:00.000+0800"
        );

        JSONObject item = sanitized.getJSONObject(0);
        assertEquals("2026-05-25T10:00:00.000+0800", item.optString("deletedAt"));
        assertEquals("missing-local-file", item.optString("ocrStatus"));
    }

    private static void assertValidateAttachmentsRequiresBlobForActiveAttachment() throws Exception {
        BabyLogDomain.AttachmentRecord attachment = BabyLogDomain.createAttachment(
                "document_image", "missing.jpg", "image/jpeg", 1, "does-not-exist.jpg");
        JSONArray attachments = new JSONArray().put(attachment.toJson());

        boolean threw = false;
        try {
            BabyLogAttachmentInputBuilder.validateAttachments(attachments, new JSONArray());
        } catch (Exception expected) {
            threw = true;
        }

        if (!threw) {
            throw new AssertionError("expected missing attachment blob validation failure");
        }
    }

}
