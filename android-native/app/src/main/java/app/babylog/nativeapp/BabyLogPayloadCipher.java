package app.babylog.nativeapp;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public final class BabyLogPayloadCipher {
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int TAG_BITS = 128;
    private static final int NONCE_BYTES = 12;
    private static final SecureRandom RANDOM = new SecureRandom();

    private BabyLogPayloadCipher() {
    }

    public static SealResult seal(byte[] dataKey, byte[] aad, byte[] plaintext) throws GeneralSecurityException {
        byte[] nonce = new byte[NONCE_BYTES];
        RANDOM.nextBytes(nonce);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(requireAes256Key(dataKey), "AES"), new GCMParameterSpec(TAG_BITS, nonce));
        if (aad != null && aad.length > 0) {
            cipher.updateAAD(aad);
        }
        return new SealResult(nonce, cipher.doFinal(plaintext == null ? new byte[0] : plaintext));
    }

    public static byte[] open(byte[] dataKey, byte[] aad, byte[] nonce, byte[] ciphertext) throws GeneralSecurityException {
        if (nonce == null || nonce.length != NONCE_BYTES) {
            throw new GeneralSecurityException("invalid nonce");
        }
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(requireAes256Key(dataKey), "AES"), new GCMParameterSpec(TAG_BITS, nonce));
        if (aad != null && aad.length > 0) {
            cipher.updateAAD(aad);
        }
        return cipher.doFinal(ciphertext == null ? new byte[0] : ciphertext);
    }

    private static byte[] requireAes256Key(byte[] key) throws GeneralSecurityException {
        if (key == null || key.length != 32) {
            throw new GeneralSecurityException("AES-256 key must be 32 bytes");
        }
        return key;
    }

    public static final class SealResult {
        public final byte[] nonce;
        public final byte[] ciphertext;

        public SealResult(byte[] nonce, byte[] ciphertext) {
            this.nonce = nonce == null ? new byte[0] : nonce.clone();
            this.ciphertext = ciphertext == null ? new byte[0] : ciphertext.clone();
        }
    }
}
