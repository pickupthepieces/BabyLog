package app.babylog.nativeapp;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class BabyLogFamilyKeyDeriver {
    private static final byte[] SALT = "babylog-family-v1".getBytes(StandardCharsets.UTF_8);
    private static final byte[] INFO_LOOKUP = "lookup/v1".getBytes(StandardCharsets.UTF_8);
    private static final byte[] INFO_DATA = "data/v1".getBytes(StandardCharsets.UTF_8);
    private static final byte[] INFO_INDEX = "index/v1".getBytes(StandardCharsets.UTF_8);
    private static final byte[] INFO_ATTACHMENT = "attachment/v1".getBytes(StandardCharsets.UTF_8);
    private static final int KEY_BYTES = 32;

    private BabyLogFamilyKeyDeriver() {
    }

    public static byte[] deriveLookupKey(String familyKey) {
        return derive(familyKey, INFO_LOOKUP);
    }

    public static byte[] deriveDataKey(String familyKey) {
        return derive(familyKey, INFO_DATA);
    }

    public static byte[] deriveIndexKey(String familyKey) {
        return derive(familyKey, INFO_INDEX);
    }

    public static byte[] deriveAttachmentKey(String familyKey) {
        return derive(familyKey, INFO_ATTACHMENT);
    }

    public static String lookupHashHex(String familyKey) {
        String normalized = normalizeFamilyKey(familyKey);
        if (normalized.isEmpty()) {
            return "";
        }
        return toHex(deriveLookupKey(normalized));
    }

    public static byte[] hkdfSha256(byte[] ikm, byte[] salt, byte[] info, int length) {
        if (length < 0 || length > 255 * KEY_BYTES) {
            throw new IllegalArgumentException("invalid HKDF output length");
        }
        byte[] actualSalt = salt == null || salt.length == 0 ? new byte[KEY_BYTES] : salt;
        byte[] prk = hmac(actualSalt, ikm == null ? new byte[0] : ikm);
        byte[] okm = new byte[length];
        byte[] previous = new byte[0];
        int offset = 0;
        int counter = 1;
        while (offset < length) {
            byte[] input = new byte[previous.length + (info == null ? 0 : info.length) + 1];
            System.arraycopy(previous, 0, input, 0, previous.length);
            if (info != null) {
                System.arraycopy(info, 0, input, previous.length, info.length);
            }
            input[input.length - 1] = (byte) counter;
            previous = hmac(prk, input);
            int copy = Math.min(previous.length, length - offset);
            System.arraycopy(previous, 0, okm, offset, copy);
            offset += copy;
            counter++;
        }
        return okm;
    }

    private static byte[] derive(String familyKey, byte[] info) {
        return hkdfSha256(normalizeFamilyKey(familyKey).getBytes(StandardCharsets.UTF_8), SALT, info, KEY_BYTES);
    }

    private static String normalizeFamilyKey(String familyKey) {
        String trimmed = familyKey == null ? "" : familyKey.trim();
        return Normalizer.normalize(trimmed, Normalizer.Form.NFC);
    }

    private static byte[] hmac(byte[] key, byte[] input) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(input);
        } catch (Exception error) {
            throw new IllegalStateException("HmacSHA256 is unavailable", error);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value & 0xff));
        }
        return builder.toString();
    }
}
