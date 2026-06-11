package app.babylog.nativeapp;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class BabyLogAttachmentInputBuilder {
    private final Context context;
    private final File filesDir;

    public BabyLogAttachmentInputBuilder(Context context) {
        this(context, null);
    }

    private BabyLogAttachmentInputBuilder(Context context, File filesDir) {
        this.context = context;
        this.filesDir = filesDir;
    }

    public static BabyLogAttachmentInputBuilder forSmokeTest(File filesDir) {
        return new BabyLogAttachmentInputBuilder(null, filesDir);
    }

    public List<BabyLogDomain.AttachmentRecord> createPregnancyAttachments(BabyLogService.PregnancyInput input) throws JSONException {
        List<BabyLogDomain.AttachmentRecord> attachments = new ArrayList<>();
        if (!BabyLogService.isPregnancyDocumentEvent(input.eventType) || isBlank(input.attachmentPath)) {
            return attachments;
        }
        File image = new File(input.attachmentPath);
        if (image.exists() && image.length() > 0) {
            BabyLogDomain.AttachmentRecord attachment = BabyLogDomain.createAttachment(
                    "document_image",
                    isBlank(input.attachmentName) ? image.getName() : input.attachmentName,
                    "image/jpeg",
                    image.length(),
                    input.attachmentPath
            );
            attachments.add(attachment);
        }
        return attachments;
    }

    public List<BabyLogDomain.AttachmentRecord> createUltrasoundAttachments(BabyLogService.UltrasoundInput input) throws JSONException {
        List<BabyLogDomain.AttachmentRecord> attachments = new ArrayList<>();
        if (input.photoPath != null && !input.photoPath.isEmpty()) {
            File image = new File(input.photoPath);
            if (image.exists() && image.length() > 0) {
                BabyLogDomain.AttachmentRecord attachment = BabyLogDomain.createAttachment(
                        "ultrasound_image",
                        input.photoName == null || input.photoName.isEmpty() ? image.getName() : input.photoName,
                        "image/jpeg",
                        image.length(),
                        input.photoPath
                );
                attachments.add(attachment);
            }
        }
        return attachments;
    }

    public String copyImageUriToPrivateFile(Uri uri, String nameHint) throws IOException {
        InputStream input = context.getContentResolver().openInputStream(uri);
        if (input == null) {
            throw new IOException("无法读取图片");
        }
        File raw = createCameraCaptureFile("selected-original.jpg");
        try (InputStream in = input; FileOutputStream out = new FileOutputStream(raw)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }
        try {
            return compressImageFileToPrivateFile(raw, nameHint == null ? "selected.jpg" : nameHint);
        } finally {
            raw.delete();
        }
    }

    public String compressImageFileToPrivateFile(File source, String nameHint) throws IOException {
        File output = createAttachmentFile(nameHint == null ? "scan.jpg" : nameHint);
        BabyLogImageUtils.compressFileToJpeg(source, output);
        return output.getAbsolutePath();
    }

    public File createCameraCaptureFile(String nameHint) throws IOException {
        File base = context == null ? filesDir() : context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File dir = new File(base == null ? filesDir() : base, "camera-captures");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("无法创建拍照目录");
        }
        String safeName = nameHint == null ? "scan.jpg" : nameHint.replaceAll("[^A-Za-z0-9._-]", "_");
        return File.createTempFile("babylog-", "-" + safeName, dir);
    }

    public File createAttachmentFile(String nameHint) {
        File dir = new File(filesDir(), "attachments");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String safeName = nameHint == null ? "scan.jpg" : nameHint.replaceAll("[^A-Za-z0-9._-]", "_");
        return new File(dir, UUID.randomUUID() + "-" + safeName);
    }

    public void clearLocalAttachmentFiles() {
        deleteRecursively(new File(filesDir(), "attachments"));
        deleteRecursively(new File(filesDir(), "camera-captures"));
        if (context != null) {
            File pictureBase = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            if (pictureBase != null) {
                deleteRecursively(new File(pictureBase, "camera-captures"));
            }
        }
    }

    public long estimateAttachmentBytes() {
        return directoryBytes(new File(filesDir(), "attachments"));
    }

    private File filesDir() {
        return context == null ? filesDir : context.getFilesDir();
    }

    public void deleteLocalFile(String path) {
        if (path == null || path.trim().isEmpty()) {
            return;
        }
        File file = new File(path);
        if (file.isFile()) {
            file.delete();
        }
    }

    public JSONArray createAttachmentBlobBackup(JSONArray attachments) throws IOException, JSONException {
        JSONArray blobs = new JSONArray();
        if (attachments == null) {
            return blobs;
        }
        for (int i = 0; i < attachments.length(); i++) {
            BabyLogDomain.AttachmentRecord attachment = BabyLogDomain.AttachmentRecord.fromJson(attachments.optJSONObject(i));
            if (attachment == null) {
                continue;
            }
            if (attachment.deletedAt != null) {
                continue;
            }
            File file = new File(attachment.localPath);
            if (!file.isFile()) {
                throw new IOException("附件文件不存在：" + attachment.originalName);
            }
            JSONObject blob = new JSONObject();
            blob.put("familyId", attachment.familyId);
            blob.put("attachmentId", attachment.id);
            blob.put("mimeType", attachment.mimeType);
            blob.put("byteSize", file.length());
            blob.put("createdAt", attachment.createdAt);
            blob.put("dataBase64", encodeBase64(readFileBytes(file)));
            blobs.put(blob);
        }
        return blobs;
    }

    public JSONArray restoreAttachmentBlobs(JSONArray attachments, JSONArray blobs) throws JSONException, IOException {
        if (attachments == null) {
            attachments = new JSONArray();
        }
        if (blobs == null) {
            return attachments;
        }

        Map<String, String> restoredPaths = new HashMap<>();
        for (int i = 0; i < blobs.length(); i++) {
            JSONObject blob = blobs.optJSONObject(i);
            if (blob == null) {
                continue;
            }
            String attachmentId = blob.optString("attachmentId");
            byte[] bytes = decodeBase64(blob.optString("dataBase64", ""));
            File file = createAttachmentFile(attachmentId + ".jpg");
            try (FileOutputStream output = new FileOutputStream(file)) {
                output.write(bytes);
            }
            restoredPaths.put(attachmentId, file.getAbsolutePath());
        }

        JSONArray restored = new JSONArray();
        for (int i = 0; i < attachments.length(); i++) {
            JSONObject attachment = attachments.optJSONObject(i);
            if (attachment == null) {
                continue;
            }
            String path = restoredPaths.get(attachment.optString("id"));
            if (path != null) {
                attachment.put("localPath", path);
                attachment.put("localBlobKey", path);
            }
            restored.put(attachment);
        }
        return restored;
    }

    public static List<String> attachmentIdsFromRecords(List<BabyLogDomain.AttachmentRecord> attachments) {
        List<String> ids = new ArrayList<>();
        if (attachments == null) {
            return ids;
        }
        for (BabyLogDomain.AttachmentRecord attachment : attachments) {
            if (attachment != null) {
                ids.add(attachment.id);
            }
        }
        return ids;
    }

    public static boolean hasUsableUltrasoundPhoto(String photoPath) {
        if (isBlank(photoPath)) {
            return false;
        }
        File image = new File(photoPath);
        return image.isFile() && image.length() > 0;
    }

    public static JSONArray sanitizeAttachmentsForBackup(JSONArray attachments, String missingAt) throws JSONException {
        JSONArray sanitized = new JSONArray();
        if (attachments == null) {
            return sanitized;
        }
        String timestamp = isBlank(missingAt) ? BabyLogFormatters.nowIso() : missingAt;
        for (int i = 0; i < attachments.length(); i++) {
            JSONObject json = attachments.optJSONObject(i);
            if (json == null) {
                continue;
            }
            JSONObject copy = new JSONObject(json.toString());
            BabyLogDomain.AttachmentRecord attachment = BabyLogDomain.AttachmentRecord.fromJson(copy);
            if (attachment != null && attachment.deletedAt == null) {
                File file = new File(attachment.localPath);
                if (!file.isFile()) {
                    copy.put("deletedAt", timestamp);
                    copy.put("updatedAt", timestamp);
                    copy.put("ocrStatus", "missing-local-file");
                }
            }
            sanitized.put(copy);
        }
        return sanitized;
    }

    public static void validateAttachments(JSONArray attachments, JSONArray blobs) throws JSONException {
        if (attachments == null || attachments.length() == 0) {
            return;
        }
        Set<String> blobAttachmentIds = new HashSet<>();
        if (blobs != null) {
            for (int i = 0; i < blobs.length(); i++) {
                JSONObject blob = blobs.optJSONObject(i);
                String attachmentId = blob == null ? "" : blob.optString("attachmentId");
                if (isBlank(attachmentId) || isBlank(blob.optString("dataBase64"))) {
                    throw new JSONException("Invalid attachment blob at index " + i);
                }
                blobAttachmentIds.add(attachmentId);
            }
        }
        for (int i = 0; i < attachments.length(); i++) {
            JSONObject json = attachments.optJSONObject(i);
            BabyLogDomain.AttachmentRecord attachment = BabyLogDomain.AttachmentRecord.fromJson(json);
            if (attachment == null || isBlank(attachment.id) || isBlank(attachment.kind) || isBlank(attachment.createdAt)) {
                throw new JSONException("Invalid attachment at index " + i);
            }
            if (attachment.deletedAt != null) {
                continue;
            }
            if (!blobAttachmentIds.contains(attachment.id)) {
                throw new JSONException("Missing attachment blob for " + attachment.id);
            }
        }
    }

    public static byte[] readFileBytes(File file) throws IOException {
        try (FileInputStream input = new FileInputStream(file); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        }
    }

    private static String encodeBase64(byte[] value) {
        try {
            return Base64.encodeToString(value, Base64.NO_WRAP);
        } catch (RuntimeException stubbedAndroidJar) {
            return java.util.Base64.getEncoder().encodeToString(value);
        }
    }

    private static byte[] decodeBase64(String value) {
        try {
            return Base64.decode(value, Base64.DEFAULT);
        } catch (RuntimeException stubbedAndroidJar) {
            return java.util.Base64.getDecoder().decode(value);
        }
    }

    private void deleteRecursively(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        file.delete();
    }

    private long directoryBytes(File file) {
        if (file == null || !file.exists()) {
            return 0;
        }
        if (file.isFile()) {
            return file.length();
        }
        long bytes = 0;
        File[] children = file.listFiles();
        if (children == null) {
            return 0;
        }
        for (File child : children) {
            bytes += directoryBytes(child);
        }
        return bytes;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
