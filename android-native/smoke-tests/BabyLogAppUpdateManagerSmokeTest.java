import static app.babylog.nativeapp.SmokeAssert.*;

import app.babylog.nativeapp.BabyLogAppUpdateManager;

import java.io.File;
import java.io.FileOutputStream;

public final class BabyLogAppUpdateManagerSmokeTest {
    public static void main(String[] args) throws Exception {
        String manifest = "{"
                + "\"versionCode\":2,"
                + "\"versionName\":\"0.1.1\","
                + "\"apkUrl\":\"https://github.com/pickupthepieces/BabyLog/releases/download/v0.1.1/babylog-release.apk\","
                + "\"sha256\":\"0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef\","
                + "\"notes\":\"修复同步\""
                + "}";
        BabyLogAppUpdateManager.UpdateInfo update = BabyLogAppUpdateManager.parseManifest(manifest, 1);
        assertNotNull(update);
        assertEquals(2, update.versionCode);
        assertEquals("0.1.1", update.versionName);
        assertEquals("修复同步", update.notes);
        assertEquals("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef", update.sha256);

        assertEquals(null, BabyLogAppUpdateManager.parseManifest(manifest, 2));
        assertThrows(() -> BabyLogAppUpdateManager.parseManifest(manifest.replace("https://", "http://"), 1));
        assertThrows(() -> BabyLogAppUpdateManager.parseManifest(manifest.replace("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef", "bad"), 1));

        File temp = File.createTempFile("babylog-update", ".bin");
        try (FileOutputStream out = new FileOutputStream(temp)) {
            out.write("abc".getBytes("UTF-8"));
        }
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", BabyLogAppUpdateManager.sha256Hex(temp));
        if (!BabyLogAppUpdateManager.verifySha256(temp, "BA7816BF8F01CFEA414140DE5DAE2223B00361A396177A9CB410FF61F20015AD")) {
            throw new AssertionError("Expected case-insensitive sha256 match");
        }
        if (BabyLogAppUpdateManager.verifySha256(temp, "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef")) {
            throw new AssertionError("Expected sha256 mismatch");
        }
        //noinspection ResultOfMethodCallIgnored
        temp.delete();
    }




}
