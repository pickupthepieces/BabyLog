package app.babylog.nativeapp;

import java.io.ByteArrayOutputStream;

public final class BabyLogSyncBase64 {
    private static final char[] ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();
    private static final int[] REVERSE = new int[128];

    static {
        for (int i = 0; i < REVERSE.length; i++) {
            REVERSE[i] = -1;
        }
        for (int i = 0; i < ALPHABET.length; i++) {
            REVERSE[ALPHABET[i]] = i;
        }
    }

    private BabyLogSyncBase64() {
    }

    public static String encode(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder(((bytes.length + 2) / 3) * 4);
        for (int i = 0; i < bytes.length; i += 3) {
            int b0 = bytes[i] & 0xff;
            int b1 = i + 1 < bytes.length ? bytes[i + 1] & 0xff : 0;
            int b2 = i + 2 < bytes.length ? bytes[i + 2] & 0xff : 0;
            builder.append(ALPHABET[b0 >>> 2]);
            builder.append(ALPHABET[((b0 & 0x03) << 4) | (b1 >>> 4)]);
            builder.append(i + 1 < bytes.length ? ALPHABET[((b1 & 0x0f) << 2) | (b2 >>> 6)] : '=');
            builder.append(i + 2 < bytes.length ? ALPHABET[b2 & 0x3f] : '=');
        }
        return builder.toString();
    }

    public static byte[] decode(String value) {
        if (value == null || value.trim().isEmpty()) {
            return new byte[0];
        }
        String compact = value.replaceAll("\\s", "");
        if (compact.length() % 4 != 0) {
            throw new IllegalArgumentException("invalid base64 length");
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream((compact.length() / 4) * 3);
        for (int i = 0; i < compact.length(); i += 4) {
            int c0 = valueOf(compact.charAt(i));
            int c1 = valueOf(compact.charAt(i + 1));
            char ch2 = compact.charAt(i + 2);
            char ch3 = compact.charAt(i + 3);
            int c2 = ch2 == '=' ? 0 : valueOf(ch2);
            int c3 = ch3 == '=' ? 0 : valueOf(ch3);
            output.write((c0 << 2) | (c1 >>> 4));
            if (ch2 != '=') {
                output.write(((c1 & 0x0f) << 4) | (c2 >>> 2));
            }
            if (ch3 != '=') {
                output.write(((c2 & 0x03) << 6) | c3);
            }
        }
        return output.toByteArray();
    }

    private static int valueOf(char value) {
        if (value >= REVERSE.length || REVERSE[value] < 0) {
            throw new IllegalArgumentException("invalid base64 character");
        }
        return REVERSE[value];
    }
}
