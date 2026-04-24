package com.eu.habbo.networking.gameserver.crypto;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

public final class WsSessionCrypto {

    public static final int  HANDSHAKE_MAGIC   = 0xC0DEC0DE;
    public static final byte TYPE_SERVER_HELLO = 0x01;
    public static final byte TYPE_CLIENT_HELLO = 0x02;

    public static final String HKDF_INFO = "nitro-ws-v1";
    public static final int    AES_KEY_LEN = 32;
    public static final int    NONCE_LEN   = 12;
    public static final int    GCM_TAG_BITS = 128;

    private static final SecureRandom RNG = new SecureRandom();

    private WsSessionCrypto() {}

    public static KeyPair generateEphemeralKeyPair() throws GeneralSecurityException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(new java.security.spec.ECGenParameterSpec("secp256r1"), RNG);
        return kpg.generateKeyPair();
    }

    public static byte[] encodePublicKeySpki(PublicKey publicKey) {
        return publicKey.getEncoded();
    }

    public static PublicKey decodePublicKeySpki(byte[] spki) throws GeneralSecurityException {
        KeyFactory kf = KeyFactory.getInstance("EC");
        return kf.generatePublic(new X509EncodedKeySpec(spki));
    }

    public static byte[] deriveSharedSecret(PrivateKey ourPrivate, PublicKey theirPublic) throws GeneralSecurityException {
        KeyAgreement ka = KeyAgreement.getInstance("ECDH");
        ka.init(ourPrivate);
        ka.doPhase(theirPublic, true);
        return ka.generateSecret();
    }

    public static byte[] hkdfSha256(byte[] ikm, byte[] salt, byte[] info, int outLen) throws GeneralSecurityException {
        if (salt == null || salt.length == 0) salt = new byte[32];

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(salt, "HmacSHA256"));
        byte[] prk = mac.doFinal(ikm);

        int hashLen = 32;
        int n = (outLen + hashLen - 1) / hashLen;
        if (n > 255) throw new GeneralSecurityException("HKDF output too long");

        ByteArrayOutputStream okm = new ByteArrayOutputStream();
        byte[] t = new byte[0];

        for (int i = 1; i <= n; i++) {
            mac.init(new SecretKeySpec(prk, "HmacSHA256"));
            mac.update(t);
            if (info != null) mac.update(info);
            mac.update((byte) i);
            t = mac.doFinal();
            okm.write(t, 0, t.length);
        }

        byte[] result = okm.toByteArray();
        return (result.length == outLen) ? result : Arrays.copyOf(result, outLen);
    }

    public static byte[] deriveAesKey(byte[] sharedSecret) throws GeneralSecurityException {
        return hkdfSha256(sharedSecret, null, HKDF_INFO.getBytes(StandardCharsets.UTF_8), AES_KEY_LEN);
    }

    public static byte[] aesGcmEncrypt(byte[] key, byte[] nonce, byte[] plaintext) throws GeneralSecurityException {
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(GCM_TAG_BITS, nonce));
        return c.doFinal(plaintext);
    }

    public static byte[] aesGcmDecrypt(byte[] key, byte[] nonce, byte[] ciphertextWithTag) throws GeneralSecurityException {
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(GCM_TAG_BITS, nonce));
        return c.doFinal(ciphertextWithTag);
    }

    public static byte[] randomNonce() {
        byte[] n = new byte[NONCE_LEN];
        RNG.nextBytes(n);
        return n;
    }
}
