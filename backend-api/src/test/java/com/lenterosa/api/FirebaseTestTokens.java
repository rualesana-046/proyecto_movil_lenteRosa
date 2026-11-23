package com.lenterosa.api;

import com.lenterosa.api.shared.security.FirebaseTokenValidators;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.function.Consumer;

/**
 * Tokens de prueba con la forma de un ID token de Firebase, firmados con una llave RSA generada
 * para las pruebas. Nunca salen de las pruebas.
 */
public final class FirebaseTestTokens {

    public static final String PROJECT_ID = "lente-rosa-test";

    private static final KeyPair KEYS = generateKeys();
    private static final KeyPair OTHER_KEYS = generateKeys();

    private FirebaseTestTokens() {
    }

    public static RSAPublicKey publicKey() {
        return (RSAPublicKey) KEYS.getPublic();
    }

    public static String valid(String uid, boolean emailVerified) {
        return sign(KEYS, uid, emailVerified, claims -> { });
    }

    public static String valid(String uid, boolean emailVerified, Consumer<JWTClaimsSet.Builder> customizer) {
        return sign(KEYS, uid, emailVerified, customizer);
    }

    /** Token bien formado pero firmado con otra llave: la firma no coincide. */
    public static String signedWithOtherKey(String uid) {
        return sign(OTHER_KEYS, uid, true, claims -> { });
    }

    private static String sign(KeyPair keys, String uid, boolean emailVerified, Consumer<JWTClaimsSet.Builder> customizer) {
        Instant now = Instant.now();
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                .issuer(FirebaseTokenValidators.issuer(PROJECT_ID))
                .audience(PROJECT_ID)
                .subject(uid)
                .issueTime(Date.from(now.minusSeconds(60)))
                .expirationTime(Date.from(now.plusSeconds(3_600)))
                .claim("auth_time", now.minusSeconds(120).getEpochSecond())
                .claim("email_verified", emailVerified);
        customizer.accept(claims);
        try {
            SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("prueba").build(), claims.build());
            jwt.sign(new RSASSASigner(keys.getPrivate()));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }

    private static KeyPair generateKeys() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
