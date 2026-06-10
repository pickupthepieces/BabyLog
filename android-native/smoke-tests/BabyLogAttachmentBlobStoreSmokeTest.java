import static app.babylog.nativeapp.SmokeAssert.*;

import app.babylog.nativeapp.BabyLogAttachmentBlobStore;
import app.babylog.nativeapp.BabyLogRepository;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BabyLogAttachmentBlobStoreSmokeTest {
    public static void main(String[] args) throws Exception {
        assertBlobRoundTripAndIsolation();
        assertDownloadQueueRoundTrip();
        assertLegacyValuesMigrateAndClear();
    }

    private static void assertBlobRoundTripAndIsolation() throws Exception {
        BabyLogAttachmentBlobStore store = BabyLogAttachmentBlobStore.forSmokeTest();
        byte[] bytes = new byte[]{1, 2, 3, 4};

        assertTrue(store.putAttachmentBlobFromRemote("att_a", bytes, "hash_a"));

        assertTrue(store.hasAttachmentBlob("att_a"));
        assertEquals("hash_a", store.attachmentBlobContentHash("att_a"));
        assertTrue(Arrays.equals(bytes, store.findAttachmentBlobBytes("att_a")));
        assertFalse(store.hasAttachmentBlob("att_b"));
        assertEquals("", store.attachmentBlobContentHash("att_b"));
        assertEquals(null, store.findAttachmentBlobBytes("att_b"));
    }

    private static void assertDownloadQueueRoundTrip() throws Exception {
        BabyLogAttachmentBlobStore store = BabyLogAttachmentBlobStore.forSmokeTest();
        store.enqueueAttachmentDownload("att_a", "remote_a", "sealed.bin", "v1");
        store.enqueueAttachmentDownload("att_b", "remote_b", "sealed2.bin", "v2");

        List<BabyLogRepository.AttachmentDownloadRequest> queue = store.listAttachmentDownloadQueue();
        assertEquals(2, queue.size());
        assertEquals("att_a", queue.get(0).attachmentId);
        assertEquals("remote_a", queue.get(0).remoteRecordId);
        assertEquals("sealed.bin", queue.get(0).filename);
        assertEquals("v1", queue.get(0).fileVersion);

        store.removeFromAttachmentDownloadQueue("att_a");

        queue = store.listAttachmentDownloadQueue();
        assertEquals(1, queue.size());
        assertEquals("att_b", queue.get(0).attachmentId);
    }

    private static void assertLegacyValuesMigrateAndClear() throws Exception {
        File legacyBlob = File.createTempFile("babylog-legacy-blob", ".bin");
        try (FileOutputStream output = new FileOutputStream(legacyBlob)) {
            output.write(new byte[]{9, 8, 7});
        }
        JSONArray legacyBlobs = new JSONArray()
                .put(new JSONObject()
                        .put("id", "legacy_att")
                        .put("attachmentId", "legacy_att")
                        .put("localPath", legacyBlob.getAbsolutePath())
                        .put("localBlobKey", legacyBlob.getAbsolutePath())
                        .put("byteSize", 3)
                        .put("contentHash", "legacy_hash"));
        JSONArray legacyQueue = new JSONArray()
                .put(new JSONObject()
                        .put("id", "legacy_att")
                        .put("attachmentId", "legacy_att")
                        .put("remoteRecordId", "remote_legacy")
                        .put("filename", "legacy.bin")
                        .put("fileVersion", "v3"));
        Map<String, String> legacyValues = new LinkedHashMap<>();
        legacyValues.put("attachmentBlobs", legacyBlobs.toString());
        legacyValues.put("attachmentDownloadQueue", legacyQueue.toString());

        BabyLogAttachmentBlobStore store = BabyLogAttachmentBlobStore.forSmokeTestMigrating(legacyValues);

        assertTrue(Arrays.equals(new byte[]{9, 8, 7}, store.findAttachmentBlobBytes("legacy_att")));
        assertEquals("legacy_hash", store.attachmentBlobContentHash("legacy_att"));
        assertEquals(1, store.listAttachmentDownloadQueue().size());
        assertFalse(legacyValues.containsKey("attachmentBlobs"));
        assertFalse(legacyValues.containsKey("attachmentDownloadQueue"));
    }



}
