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

public final class BabyLogSmartConfigStore {
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "BabyLogSmartConfigApiKey";
    private static final String PREF_FILE_NAME = "babylog_smart_config";

    private static final String PREF_BASE_URL = "base_url";
    private static final String PREF_MODEL = "model";
    private static final String PREF_ENABLED = "enabled";
    private static final String PREF_API_KEY_CIPHER_TEXT = "api_key_cipher_text";
    private static final String PREF_API_KEY_IV = "api_key_iv";
    private static final String PREF_SPEECH_MODEL = "speech_model";
    private static final String PREF_SPEECH_ENABLED = "speech_enabled";
    private static final String PREF_SPEECH_API_KEY_CIPHER_TEXT = "speech_api_key_cipher_text";
    private static final String PREF_SPEECH_API_KEY_IV = "speech_api_key_iv";

    private static final int GCM_TAG_BITS = 128;
    private final Context appContext;

    public BabyLogSmartConfigStore(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("context == null");
        }
        this.appContext = context.getApplicationContext();
    }

    public Config load() throws GeneralSecurityException, IOException {
        SharedPreferences preferences = getPreferences();
        String baseUrl = preferences.getString(PREF_BASE_URL, "");
        String model = preferences.getString(PREF_MODEL, "");
        boolean enabled = preferences.getBoolean(PREF_ENABLED, false);
        String encryptedApiKey = preferences.getString(PREF_API_KEY_CIPHER_TEXT, "");
        String iv = preferences.getString(PREF_API_KEY_IV, "");
        String apiKey = "";
        if (!TextUtils.isEmpty(encryptedApiKey) && !TextUtils.isEmpty(iv)) {
            apiKey = decrypt(encryptedApiKey, iv);
        }
        return new Config(baseUrl, model, apiKey, enabled);
    }

    public void save(Config config) throws GeneralSecurityException, IOException {
        if (config == null) {
            throw new IllegalArgumentException("config == null");
        }

        SharedPreferences.Editor editor = getPreferences().edit()
                .putString(PREF_BASE_URL, nullToEmpty(config.getBaseUrl()))
                .putString(PREF_MODEL, nullToEmpty(config.getModel()))
                .putBoolean(PREF_ENABLED, config.isEnabled());

        String apiKey = config.getApiKey();
        if (TextUtils.isEmpty(apiKey)) {
            editor.remove(PREF_API_KEY_CIPHER_TEXT);
            editor.remove(PREF_API_KEY_IV);
        } else {
            EncryptedValue encryptedValue = encrypt(apiKey);
            editor.putString(PREF_API_KEY_CIPHER_TEXT, encryptedValue.cipherText);
            editor.putString(PREF_API_KEY_IV, encryptedValue.iv);
        }

        if (!editor.commit()) {
            throw new IOException("Failed to save smart config");
        }
    }

    public SpeechConfig loadSpeechConfig() throws GeneralSecurityException, IOException {
        SharedPreferences preferences = getPreferences();
        String model = preferences.getString(PREF_SPEECH_MODEL, "");
        boolean enabled = preferences.getBoolean(PREF_SPEECH_ENABLED, false);
        String encryptedApiKey = preferences.getString(PREF_SPEECH_API_KEY_CIPHER_TEXT, "");
        String iv = preferences.getString(PREF_SPEECH_API_KEY_IV, "");
        String apiKey = "";
        if (!TextUtils.isEmpty(encryptedApiKey) && !TextUtils.isEmpty(iv)) {
            apiKey = decrypt(encryptedApiKey, iv);
        }
        return new SpeechConfig(apiKey, model, enabled);
    }

    public void saveSpeechConfig(SpeechConfig config) throws GeneralSecurityException, IOException {
        if (config == null) {
            throw new IllegalArgumentException("config == null");
        }

        SharedPreferences.Editor editor = getPreferences().edit()
                .putString(PREF_SPEECH_MODEL, nullToEmpty(config.getModel()))
                .putBoolean(PREF_SPEECH_ENABLED, config.isEnabled());

        String apiKey = config.getApiKey();
        if (TextUtils.isEmpty(apiKey)) {
            editor.remove(PREF_SPEECH_API_KEY_CIPHER_TEXT);
            editor.remove(PREF_SPEECH_API_KEY_IV);
        } else {
            EncryptedValue encryptedValue = encrypt(apiKey);
            editor.putString(PREF_SPEECH_API_KEY_CIPHER_TEXT, encryptedValue.cipherText);
            editor.putString(PREF_SPEECH_API_KEY_IV, encryptedValue.iv);
        }

        if (!editor.commit()) {
            throw new IOException("Failed to save speech config");
        }
    }

    public void clear() throws IOException {
        SharedPreferences.Editor editor = getPreferences().edit().clear();
        if (!editor.commit()) {
            throw new IOException("Failed to clear smart config");
        }
    }

    public boolean isConfigured() {
        SharedPreferences preferences = getPreferences();
        return preferences.getBoolean(PREF_ENABLED, false)
                && !TextUtils.isEmpty(preferences.getString(PREF_BASE_URL, ""))
                && !TextUtils.isEmpty(preferences.getString(PREF_MODEL, ""))
                && !TextUtils.isEmpty(preferences.getString(PREF_API_KEY_CIPHER_TEXT, ""))
                && !TextUtils.isEmpty(preferences.getString(PREF_API_KEY_IV, ""));
    }

    public boolean isSpeechConfigured() {
        SharedPreferences preferences = getPreferences();
        return preferences.getBoolean(PREF_SPEECH_ENABLED, false)
                && !TextUtils.isEmpty(preferences.getString(PREF_SPEECH_MODEL, ""))
                && !TextUtils.isEmpty(preferences.getString(PREF_SPEECH_API_KEY_CIPHER_TEXT, ""))
                && !TextUtils.isEmpty(preferences.getString(PREF_SPEECH_API_KEY_IV, ""));
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

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static boolean isEmptyString(String value) {
        return value == null || value.length() == 0;
    }

    private static final class EncryptedValue {
        final String cipherText;
        final String iv;

        EncryptedValue(String cipherText, String iv) {
            this.cipherText = cipherText;
            this.iv = iv;
        }
    }

    public static final class Config {
        private final String baseUrl;
        private final String model;
        private final String apiKey;
        private final boolean enabled;

        public Config(String baseUrl, String model, String apiKey, boolean enabled) {
            this.baseUrl = nullToEmpty(baseUrl);
            this.model = nullToEmpty(model);
            this.apiKey = nullToEmpty(apiKey);
            this.enabled = enabled;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public String getModel() {
            return model;
        }

        public String getApiKey() {
            return apiKey;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public boolean isConfigured() {
            return enabled
                    && !isEmptyString(baseUrl)
                    && !isEmptyString(model)
                    && !isEmptyString(apiKey);
        }

        @Override
        public String toString() {
            return "Config{"
                    + "baseUrl='" + baseUrl + '\''
                    + ", model='" + model + '\''
                    + ", apiKeyConfigured=" + !isEmptyString(apiKey)
                    + ", enabled=" + enabled
                    + '}';
        }
    }

    public static final class SpeechConfig {
        private final String apiKey;
        private final String model;
        private final boolean enabled;

        public SpeechConfig(String apiKey, String model, boolean enabled) {
            this.apiKey = nullToEmpty(apiKey);
            this.model = nullToEmpty(model);
            this.enabled = enabled;
        }

        public String getApiKey() {
            return apiKey;
        }

        public String getModel() {
            return model;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public boolean isConfigured() {
            return enabled
                    && !isEmptyString(apiKey)
                    && !isEmptyString(model);
        }

        @Override
        public String toString() {
            return "SpeechConfig{"
                    + "apiKeyConfigured=" + !isEmptyString(apiKey)
                    + ", model='" + model + '\''
                    + ", enabled=" + enabled
                    + '}';
        }
    }
}
