package app.babylog.nativeapp;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public final class BabyLogAttachmentCipher {
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int TAG_BITS = 128;
    private static final int NONCE_BYTES = 12;
    private static final SecureRandom RANDOM = new SecureRandom();

    private BabyLogAttachmentCipher() {
    }

    public static byte[] sealFile(byte[] attachmentKey, byte[] aad, byte[] plaintext) throws GeneralSecurityException {
        byte[] nonce = new byte[NONCE_BYTES];
        RANDOM.nextBytes(nonce);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(requireAes256Key(attachmentKey), "AES"), new GCMParameterSpec(TAG_BITS, nonce));
        if (aad != null && aad.length > 0) {
            cipher.updateAAD(aad);
        }
        byte[] ciphertext = cipher.doFinal(plaintext == null ? new byte[0] : plaintext);
        byte[] sealed = new byte[NONCE_BYTES + ciphertext.length];
        System.arraycopy(nonce, 0, sealed, 0, NONCE_BYTES);
        System.arraycopy(ciphertext, 0, sealed, NONCE_BYTES, ciphertext.length);
        return sealed;
    }

    public static byte[] openFile(byte[] attachmentKey, byte[] aad, byte[] sealed) throws GeneralSecurityException {
        if (sealed == null || sealed.length <= NONCE_BYTES) {
            throw new GeneralSecurityException("invalid sealed attachment");
        }
        byte[] nonce = Arrays.copyOfRange(sealed, 0, NONCE_BYTES);
        byte[] ciphertext = Arrays.copyOfRange(sealed, NONCE_BYTES, sealed.length);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(requireAes256Key(attachmentKey), "AES"), new GCMParameterSpec(TAG_BITS, nonce));
        if (aad != null && aad.length > 0) {
            cipher.updateAAD(aad);
        }
        return cipher.doFinal(ciphertext);
    }

    private static byte[] requireAes256Key(byte[] key) throws GeneralSecurityException {
        if (key == null || key.length != 32) {
            throw new GeneralSecurityException("AES-256 key must be 32 bytes");
        }
        return key;
    }
}
