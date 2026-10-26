package com.anubhav.Backend.github;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.pkcs.RSAPrivateKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Date;

@Service
public class GitHubJwtService {

    @Value("${github.app.id}")
    private Long appId;

    @Value("${github.app.client-id}")
    private String clientId;

    @Value("${github.app.private-key-path}")
    private String privateKeyPath;


    private KeyPair loadKeyPair() throws Exception {

        // Read the PEM file
        String pem = Files.readString(
                Path.of(privateKeyPath)
        );

        // Remove PEM header, footer and spaces
        pem = pem
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        // Base64 → bytes
        byte[] keyBytes = Base64.getDecoder().decode(pem);

        // Read PKCS#1 structure
        ASN1Primitive primitive;

        try (ASN1InputStream inputStream =
                     new ASN1InputStream(keyBytes)) {

            primitive = inputStream.readObject();
        }

        RSAPrivateKey rsaPrivateKey =
                RSAPrivateKey.getInstance(primitive);

        // Create Java private key
        RSAPrivateKeySpec privateKeySpec =
                new RSAPrivateKeySpec(
                        rsaPrivateKey.getModulus(),
                        rsaPrivateKey.getPrivateExponent()
                );

        KeyFactory keyFactory =
                KeyFactory.getInstance("RSA");

        PrivateKey privateKey =
                keyFactory.generatePrivate(privateKeySpec);


        // Create Java public key
        RSAPublicKeySpec publicKeySpec =
                new RSAPublicKeySpec(
                        rsaPrivateKey.getModulus(),
                        rsaPrivateKey.getPublicExponent()
                );

        PublicKey publicKey =
                keyFactory.generatePublic(publicKeySpec);


        return new KeyPair(
                publicKey,
                privateKey
        );
    }


    public String generateToken() throws Exception {

        KeyPair keyPair = loadKeyPair();

        Algorithm algorithm =
                Algorithm.RSA256(
                        (java.security.interfaces.RSAPublicKey)
                                keyPair.getPublic(),

                        (java.security.interfaces.RSAPrivateKey)
                                keyPair.getPrivate()
                );

        Date now = new Date();

        Date issuedAt =
                new Date(
                        now.getTime() - 60_000
                );

        Date expiresAt =
                new Date(
                        now.getTime() + 9 * 60_000
                );


        return JWT.create()
                .withIssuer(clientId)
                .withIssuedAt(issuedAt)
                .withExpiresAt(expiresAt)
                .sign(algorithm);
    }
}