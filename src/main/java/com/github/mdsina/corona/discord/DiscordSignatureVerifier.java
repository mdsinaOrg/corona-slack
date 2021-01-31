package com.github.mdsina.corona.discord;

import com.google.crypto.tink.subtle.Ed25519Verify;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import javax.inject.Singleton;

@Singleton
public class DiscordSignatureVerifier {

    private final Ed25519Verify ed25519Verifier;

    public DiscordSignatureVerifier(DiscordProperties properties) {
        this.ed25519Verifier = new Ed25519Verify(properties.getPublicKey().getBytes(StandardCharsets.UTF_8));
    }

    public void verifyRequest(String rawBody, String timestamp, String signature) {
        String dataToValidate = timestamp + rawBody;

        Instant timeInstant = Instant.ofEpochSecond(Long.parseLong(timestamp));
        if (timeInstant.plus(5, ChronoUnit.MINUTES).compareTo(Instant.now()) < 0) {
            throw new RuntimeException("Timestamp too old.");
        }

        try {
            ed25519Verifier.verify(
                signature.getBytes(StandardCharsets.UTF_8),
                dataToValidate.getBytes(StandardCharsets.UTF_8)
            );
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

}
