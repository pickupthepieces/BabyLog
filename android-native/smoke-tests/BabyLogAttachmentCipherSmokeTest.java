import static app.babylog.nativeapp.SmokeAssert.*;

import app.babylog.nativeapp.BabyLogAttachmentCipher;
import app.babylog.nativeapp.BabyLogFamilyKeyDeriver;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;

public final class BabyLogAttachmentCipherSmokeTest {
    public static void main(String[] args) throws Exception {
        byte[] attachmentKey = BabyLogFamilyKeyDeriver.deriveAttachmentKey("family-secret");
        byte[] wrongKey = BabyLogFamilyKeyDeriver.deriveAttachmentKey("another-family");
        byte[] aad = "att_001|1|family-hash".getBytes(StandardCharsets.UTF_8);
        byte[] plaintext = "local ultrasound bytes".getBytes(StandardCharsets.UTF_8);

        byte[] sealed = BabyLogAttachmentCipher.sealFile(attachmentKey, aad, plaintext);
        assertTrue(sealed.length > plaintext.length + 12);
        assertTrue(Arrays.equals(plaintext, BabyLogAttachmentCipher.openFile(attachmentKey, aad, sealed)));

        byte[] sealedAgain = BabyLogAttachmentCipher.sealFile(attachmentKey, aad, plaintext);
        assertFalse(Arrays.equals(Arrays.copyOfRange(sealed, 0, 12), Arrays.copyOfRange(sealedAgain, 0, 12)));

        byte[] tampered = sealed.clone();
        tampered[tampered.length - 1] ^= 0x01;
        assertThrows(() -> BabyLogAttachmentCipher.openFile(attachmentKey, aad, tampered));

        assertThrows(() -> BabyLogAttachmentCipher.openFile(
                wrongKey,
                aad,
                sealed
        ));
        assertThrows(() -> BabyLogAttachmentCipher.openFile(
                attachmentKey,
                "att_001|1|other-family".getBytes(StandardCharsets.UTF_8),
                sealed
        ));

        byte[] large = new byte[2 * 1024 * 1024];
        new SecureRandom(new byte[]{1, 2, 3, 4}).nextBytes(large);
        byte[] largeSealed = BabyLogAttachmentCipher.sealFile(attachmentKey, aad, large);
        assertTrue(Arrays.equals(large, BabyLogAttachmentCipher.openFile(attachmentKey, aad, largeSealed)));

        assertThrows(() -> BabyLogAttachmentCipher.openFile(attachmentKey, aad, new byte[11]));
    }




}
