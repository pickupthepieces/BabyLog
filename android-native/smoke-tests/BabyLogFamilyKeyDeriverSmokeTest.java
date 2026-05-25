import app.babylog.nativeapp.BabyLogFamilyKeyDeriver;
import app.babylog.nativeapp.BabyLogSyncProtocol;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class BabyLogFamilyKeyDeriverSmokeTest {
    public static void main(String[] args) {
        byte[] lookup = BabyLogFamilyKeyDeriver.deriveLookupKey(" family-secret ");
        byte[] data = BabyLogFamilyKeyDeriver.deriveDataKey("family-secret");
        byte[] index = BabyLogFamilyKeyDeriver.deriveIndexKey("family-secret");
        byte[] attachment = BabyLogFamilyKeyDeriver.deriveAttachmentKey("family-secret");

        assertEquals(32, lookup.length);
        assertEquals(32, data.length);
        assertEquals(32, index.length);
        assertEquals(32, attachment.length);
        assertTrue(Arrays.equals(lookup, BabyLogFamilyKeyDeriver.deriveLookupKey("family-secret")));
        assertTrue(Arrays.equals(attachment, BabyLogFamilyKeyDeriver.deriveAttachmentKey(" family-secret ")));
        assertFalse(Arrays.equals(lookup, data));
        assertFalse(Arrays.equals(data, index));
        assertFalse(Arrays.equals(data, attachment));
        assertFalse(Arrays.equals(index, attachment));
        assertFalse(Arrays.equals(lookup, attachment));
        assertFalse(Arrays.equals(lookup, BabyLogFamilyKeyDeriver.deriveLookupKey("another-family")));

        String composed = "\u00e9-family";
        String decomposed = "e\u0301-family";
        assertTrue(Arrays.equals(
                BabyLogFamilyKeyDeriver.deriveLookupKey(composed),
                BabyLogFamilyKeyDeriver.deriveLookupKey(decomposed)
        ));

        String lookupHash = BabyLogFamilyKeyDeriver.lookupHashHex("family-secret");
        assertEquals(64, lookupHash.length());
        assertTrue(lookupHash.matches("[0-9a-f]+"));
        assertEquals(lookupHash, BabyLogSyncProtocol.hashFamilyKeyForLookup(" family-secret "));

        assertRfc5869TestCase1();
    }

    private static void assertRfc5869TestCase1() {
        byte[] ikm = repeat((byte) 0x0b, 22);
        byte[] salt = hex("000102030405060708090a0b0c");
        byte[] info = hex("f0f1f2f3f4f5f6f7f8f9");
        byte[] okm = BabyLogFamilyKeyDeriver.hkdfSha256(ikm, salt, info, 42);
        assertEquals(
                "3cb25f25faacd57a90434f64d0362f2a"
                        + "2d2d0a90cf1a5a4c5db02d56ecc4c5bf"
                        + "34007208d5b887185865",
                toHex(okm)
        );
    }

    private static byte[] repeat(byte value, int count) {
        byte[] bytes = new byte[count];
        Arrays.fill(bytes, value);
        return bytes;
    }

    private static byte[] hex(String value) {
        byte[] bytes = new byte[value.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(value.substring(i * 2, i * 2 + 2), 16);
        }
        return bytes;
    }

    private static String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value & 0xff));
        }
        return builder.toString();
    }

    private static void assertEquals(Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("expected " + expected + " but got " + actual);
        }
    }

    private static void assertTrue(boolean value) {
        if (!value) {
            throw new AssertionError("expected true");
        }
    }

    private static void assertFalse(boolean value) {
        if (value) {
            throw new AssertionError("expected false");
        }
    }
}
