package app.babylog.nativeapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.text.TextUtils;
import android.util.Base64;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public final class BabyLogSyncSecretStore {
    public static final String PREF_FILE_NAME = "babylog_sync_secrets";

    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "BabyLogFamilySyncKey";
    private static final String PREF_FAMILY_KEY_CIPHER_TEXT = "family_key_cipher_text";
    private static final String PREF_FAMILY_KEY_IV = "family_key_iv";
    private static final int GCM_TAG_BITS = 128;

    private final Context appContext;

    public BabyLogSyncSecretStore(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("context == null");
        }
        appContext = context.getApplicationContext();
    }

    public String loadFamilyKey() throws GeneralSecurityException, IOException {
        SharedPreferences preferences = getPreferences();
        String cipherText = preferences.getString(PREF_FAMILY_KEY_CIPHER_TEXT, "");
        String iv = preferences.getString(PREF_FAMILY_KEY_IV, "");
        if (TextUtils.isEmpty(cipherText) || TextUtils.isEmpty(iv)) {
            return "";
        }
        return decrypt(cipherText, iv);
    }

    public void saveFamilyKey(String familyKey) throws GeneralSecurityException, IOException {
        String normalized = normalizeFamilyKeyForStorage(familyKey);
        if (normalized.isEmpty()) {
            clearFamilyKey();
            return;
        }
        EncryptedValue encryptedValue = encrypt(normalized);
        boolean ok = getPreferences().edit()
                .putString(PREF_FAMILY_KEY_CIPHER_TEXT, encryptedValue.cipherText)
                .putString(PREF_FAMILY_KEY_IV, encryptedValue.iv)
                .commit();
        if (!ok) {
            throw new IOException("Failed to save family sync key");
        }
    }

    public void clearFamilyKey() throws IOException {
        boolean ok = getPreferences().edit()
                .remove(PREF_FAMILY_KEY_CIPHER_TEXT)
                .remove(PREF_FAMILY_KEY_IV)
                .commit();
        if (!ok) {
            throw new IOException("Failed to clear family sync key");
        }
    }

    public boolean hasFamilyKey() {
        SharedPreferences preferences = getPreferences();
        return !TextUtils.isEmpty(preferences.getString(PREF_FAMILY_KEY_CIPHER_TEXT, ""))
                && !TextUtils.isEmpty(preferences.getString(PREF_FAMILY_KEY_IV, ""));
    }

    public static boolean hasUsableFamilyKey(String familyKey) {
        return !normalizeFamilyKeyForStorage(familyKey).isEmpty();
    }

    public static String normalizeFamilyKeyForStorage(String familyKey) {
        return familyKey == null ? "" : familyKey.trim();
    }

    private EncryptedValue encrypt(String plainText) throws GeneralSecurityException, IOException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey());
        byte[] cipherBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        byte[] iv = cipher.getIV();
        if (iv == null || iv.length == 0) {
            throw new GeneralSecurityException("Keystore did not provide an IV");
        }
        return new EncryptedValue(encode(cipherBytes), encode(iv));
    }

    private String decrypt(String cipherText, String ivText) throws GeneralSecurityException, IOException {
        byte[] cipherBytes = decode(cipherText);
        byte[] iv = decode(ivText);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), new GCMParameterSpec(GCM_TAG_BITS, iv));
        byte[] plainBytes = cipher.doFinal(cipherBytes);
        return new String(plainBytes, StandardCharsets.UTF_8);
    }

    private SecretKey getOrCreateSecretKey() throws GeneralSecurityException, IOException {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
        keyStore.load(null);
        KeyStore.Entry existingEntry = keyStore.getEntry(KEY_ALIAS, null);
        if (existingEntry instanceof KeyStore.SecretKeyEntry) {
            return ((KeyStore.SecretKeyEntry) existingEntry).getSecretKey();
        }

        KeyGenerator keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE);
        KeyGenParameterSpec keySpec = new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build();
        keyGenerator.init(keySpec);
        return keyGenerator.generateKey();
    }

    private SharedPreferences getPreferences() {
        return appContext.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE);
    }

    private static String encode(byte[] value) {
        return Base64.encodeToString(value, Base64.NO_WRAP);
    }

    private static byte[] decode(String value) {
        return Base64.decode(value, Base64.NO_WRAP);
    }

    private static final class EncryptedValue {
        final String cipherText;
        final String iv;

        EncryptedValue(String cipherText, String iv) {
            this.cipherText = cipherText;
            this.iv = iv;
        }
    }
}
