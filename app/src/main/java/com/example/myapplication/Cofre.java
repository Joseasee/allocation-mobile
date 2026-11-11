package com.example.myapplication;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Cofre: criptografa textos (usuário e senha do site) com uma chave que fica dentro do Android Keystore. */
public final class Cofre {
    private static final String ALIAS = "autosetor_chave";
    private static final String PROVEDOR = "AndroidKeyStore";
    private static final String TRANSFORMACAO = "AES/GCM/NoPadding";
    private static final int TAM_IV = 12;

    private Cofre() {}

    private static SecretKey chave() throws Exception {
        KeyStore ks = KeyStore.getInstance(PROVEDOR);
        ks.load(null);
        if (ks.containsAlias(ALIAS)) {
            return (SecretKey) ks.getKey(ALIAS, null);
        }
        KeyGenerator kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVEDOR);
        kg.init(new KeyGenParameterSpec.Builder(ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build());
        return kg.generateKey();
    }

    public static String cifrar(String texto) throws Exception {
        Cipher c = Cipher.getInstance(TRANSFORMACAO);
        c.init(Cipher.ENCRYPT_MODE, chave());
        byte[] iv = c.getIV();
        byte[] dados = c.doFinal(texto.getBytes(StandardCharsets.UTF_8));
        byte[] tudo = new byte[iv.length + dados.length];
        System.arraycopy(iv, 0, tudo, 0, iv.length);
        System.arraycopy(dados, 0, tudo, iv.length, dados.length);
        return Base64.encodeToString(tudo, Base64.NO_WRAP);
    }

    public static String decifrar(String base64) throws Exception {
        byte[] tudo = Base64.decode(base64, Base64.NO_WRAP);
        byte[] iv = Arrays.copyOfRange(tudo, 0, TAM_IV);
        byte[] dados = Arrays.copyOfRange(tudo, TAM_IV, tudo.length);
        Cipher c = Cipher.getInstance(TRANSFORMACAO);
        c.init(Cipher.DECRYPT_MODE, chave(), new GCMParameterSpec(128, iv));
        return new String(c.doFinal(dados), StandardCharsets.UTF_8);
    }
}
