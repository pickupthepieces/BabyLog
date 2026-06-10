import static app.babylog.nativeapp.SmokeAssert.*;

import app.babylog.nativeapp.BabyLogFamilyKeyDeriver;
import app.babylog.nativeapp.BabyLogPayloadCipher;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;

public final class BabyLogPayloadCipherSmokeTest {
    public static void main(String[] args) throws Exception {
        byte[] dataKey = BabyLogFamilyKeyDeriver.deriveDataKey("family-secret");
        byte[] wrongKey = BabyLogFamilyKeyDeriver.deriveDataKey("another-family");
        byte[] aad = "client-1|1|family-hash".getBytes(StandardCharsets.UTF_8);
        byte[] plaintext = "{\"note\":\"sensitive content\"}".getBytes(StandardCharsets.UTF_8);

        BabyLogPayloadCipher.SealResult sealed = BabyLogPayloadCipher.seal(dataKey, aad, plaintext);
        assertEquals(12, sealed.nonce.length);
        assertTrue(sealed.ciphertext.length > plaintext.length);
        assertTrue(Arrays.equals(plaintext, BabyLogPayloadCipher.open(dataKey, aad, sealed.nonce, sealed.ciphertext)));

        BabyLogPayloadCipher.SealResult sealedAgain = BabyLogPayloadCipher.seal(dataKey, aad, plaintext);
        assertFalse(Arrays.equals(sealed.nonce, sealedAgain.nonce));

        byte[] tamperedCiphertext = sealed.ciphertext.clone();
        tamperedCiphertext[0] ^= 0x01;
        assertThrows(() -> BabyLogPayloadCipher.open(dataKey, aad, sealed.nonce, tamperedCiphertext));

        byte[] tamperedNonce = sealed.nonce.clone();
        tamperedNonce[0] ^= 0x01;
        assertThrows(() -> BabyLogPayloadCipher.open(dataKey, aad, tamperedNonce, sealed.ciphertext));

        assertThrows(() -> BabyLogPayloadCipher.open(
                dataKey,
                "client-1|1|other-family-hash".getBytes(StandardCharsets.UTF_8),
                sealed.nonce,
                sealed.ciphertext
        ));
        assertThrows(() -> BabyLogPayloadCipher.open(wrongKey, aad, sealed.nonce, sealed.ciphertext));

        BabyLogPayloadCipher.SealResult empty = BabyLogPayloadCipher.seal(dataKey, aad, new byte[0]);
        assertEquals(0, BabyLogPayloadCipher.open(dataKey, aad, empty.nonce, empty.ciphertext).length);
    }





}
