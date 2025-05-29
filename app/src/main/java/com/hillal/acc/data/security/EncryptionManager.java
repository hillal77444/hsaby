package com.hillal.acc.data.security;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class EncryptionManager {
    private static final String TAG = "EncryptionManager";
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "AppEncryptionKey";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;

    private final Context context;
    private KeyStore keyStore;

    public EncryptionManager(Context context) {
        this.context = context;
        initializeKeyStore();
    }

    private void initializeKeyStore() {
        try {
            keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            keyStore.load(null);
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                generateKey();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing KeyStore: " + e.getMessage());
        }
    }

    private void generateKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE);

        KeyGenParameterSpec keyGenParameterSpec = new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build();

        keyGenerator.init(keyGenParameterSpec);
        keyGenerator.generateKey();
    }

    public String encrypt(String plaintext) {
        if (keyStore == null) {
            Log.e(TAG, "KeyStore not initialized");
            return null;
        }

        try {
            SecretKey secretKey = (SecretKey) keyStore.getKey(KEY_ALIAS, null);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);

            byte[] iv = cipher.getIV();
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

            return Base64.encodeToString(combined, Base64.DEFAULT);
        } catch (Exception e) {
            Log.e(TAG, "Error encrypting data: " + e.getMessage());
            return null;
        }
    }

    public String decrypt(String encryptedData) {
        if (keyStore == null) {
            Log.e(TAG, "KeyStore not initialized");
            return null;
        }

        try {
            byte[] combined = Base64.decode(encryptedData, Base64.DEFAULT);
            byte[] iv = new byte[12];
            byte[] encrypted = new byte[combined.length - 12];

            System.arraycopy(combined, 0, iv, 0, 12);
            System.arraycopy(combined, 12, encrypted, 0, encrypted.length);

            SecretKey secretKey = (SecretKey) keyStore.getKey(KEY_ALIAS, null);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            Log.e(TAG, "Error decrypting data: " + e.getMessage());
            return null;
        }
    }

    public void enableEncryption(boolean enable) {
        context.getSharedPreferences("security_prefs", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("encryption_enabled", enable)
                .apply();
    }

    public boolean isEncryptionEnabled() {
        return context.getSharedPreferences("security_prefs", Context.MODE_PRIVATE)
                .getBoolean("encryption_enabled", true);
    }
} 