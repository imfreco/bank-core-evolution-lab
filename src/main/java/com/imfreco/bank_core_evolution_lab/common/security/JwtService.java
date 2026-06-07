package com.imfreco.bank_core_evolution_lab.common.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private final ObjectMapper objectMapper;
    private final byte[] secret;
    private final long expirationSeconds;

    public JwtService(
            ObjectMapper objectMapper,
            @Value("${bank.security.jwt.secret:bank-core-evolution-lab-demo-secret-change-me}")
                    String secret,
            @Value("${bank.security.jwt.expiration-seconds:3600}") long expirationSeconds) {
        this.objectMapper = objectMapper;
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.expirationSeconds = expirationSeconds;
    }

    public String generateToken(AuthenticatedUser user) {
        long issuedAt = Instant.now().getEpochSecond();
        long expiresAt = issuedAt + expirationSeconds;

        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", user.username());
        payload.put("roles", user.roles());
        if (user.customerId() != null) {
            payload.put("customerId", user.customerId().toString());
        }
        payload.put("iat", issuedAt);
        payload.put("exp", expiresAt);

        String unsignedToken = encodeJson(header) + "." + encodeJson(payload);
        return unsignedToken + "." + encode(sign(unsignedToken));
    }

    public AuthenticatedUser validate(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid JWT format");
        }

        String unsignedToken = parts[0] + "." + parts[1];
        byte[] expectedSignature = sign(unsignedToken);
        byte[] actualSignature = decode(parts[2]);
        if (!MessageDigest.isEqual(expectedSignature, actualSignature)) {
            throw new IllegalArgumentException("Invalid JWT signature");
        }

        JsonNode payload = readPayload(parts[1]);
        long expiresAt = payload.path("exp").asLong(0);
        if (expiresAt <= Instant.now().getEpochSecond()) {
            throw new IllegalArgumentException("Expired JWT");
        }

        String username = payload.path("sub").asText(null);
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("JWT subject is required");
        }

        List<String> roles = new ArrayList<>();
        payload.path("roles").forEach(role -> roles.add(role.asText()));
        if (roles.isEmpty()) {
            throw new IllegalArgumentException("JWT roles are required");
        }

        UUID customerId = null;
        if (payload.hasNonNull("customerId") && !payload.path("customerId").asText().isBlank()) {
            customerId = UUID.fromString(payload.path("customerId").asText());
        }

        return new AuthenticatedUser(username, roles, customerId);
    }

    public long expirationSeconds() {
        return expirationSeconds;
    }

    private String encodeJson(Object value) {
        try {
            return encode(objectMapper.writeValueAsBytes(value));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not serialize JWT content", exception);
        }
    }

    private JsonNode readPayload(String encodedPayload) {
        try {
            return objectMapper.readTree(decode(encodedPayload));
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid JWT payload", exception);
        }
    }

    private byte[] sign(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secret, HMAC_SHA256));
            return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not sign JWT", exception);
        }
    }

    private String encode(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private byte[] decode(String value) {
        return Base64.getUrlDecoder().decode(value);
    }
}
