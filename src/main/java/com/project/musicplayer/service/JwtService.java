package com.project.musicplayer.service;

import com.project.musicplayer.model.Role;
import com.project.musicplayer.model.InvalidatedToken;
import com.project.musicplayer.repository.JwtRepository;
import com.project.musicplayer.utility.TokenType;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    @Value("${jwt.secret-key}")
    private String secretKey;

    private final JwtRepository jwtRepository;

    Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();

    public SecretKeySpec getSecretKeySpec() {
        return new SecretKeySpec(secretKey.getBytes(), "HS256");
    }

    public String extractEmailForPayload(String payload) {
        JSONObject payloadJson = new JSONObject(payload);
        return payloadJson.optString("email");
    }

    public String extractRoleForPayload(String payload) {
        JSONObject payloadJson = new JSONObject(payload);
        return payloadJson.optString("role");
    }

    public boolean isTokenExpired(String payload) {
        JSONObject payloadJson = new JSONObject(payload);
        long exp = payloadJson.optLong("exp", 0);
        long iat = payloadJson.optLong("iat", 0);

        Optional<InvalidatedToken> invalidatedToken = jwtRepository.findById(extractEmailForPayload(payload));
        if (invalidatedToken.isPresent() && iat <= invalidatedToken.get().getInvalidatedTokenTime()) {
            return true;
        }
        return System.currentTimeMillis() / 1000 >= exp;
    }

    public String generateJwtToken(@Nonnull String email, @NotNull Role role, @Nonnull TokenType tokenType) throws NoSuchAlgorithmException, InvalidKeyException {
        JSONObject header = new JSONObject();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        JSONObject payload = new JSONObject();
        payload.put("email", email);
        payload.put("role", role.toString());
        payload.put("type", tokenType.toString());
        payload.put("iat", System.currentTimeMillis() / 1000);

        if (tokenType.toString().equals("access")) {
            payload.put("exp", (System.currentTimeMillis() / 1000) + 60 * 60);
        } else {
            payload.put("exp", (System.currentTimeMillis() / 1000) + 24 * 60 * 60);
        }

        String encodedHeader = encoder.encodeToString(header.toString().getBytes(StandardCharsets.UTF_8));
        String encodedPayload = encoder.encodeToString(payload.toString().getBytes(StandardCharsets.UTF_8));

        return encodedHeader + "." + encodedPayload + "." + generateSignature(header.toString(), payload.toString());
    }

    public String generateSignature(String header, String payload) throws NoSuchAlgorithmException, InvalidKeyException {
        String headerPayload = header + "." + payload;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(getSecretKeySpec());
        byte[] signatureBytes = mac.doFinal(headerPayload.getBytes(StandardCharsets.UTF_8));
        return encoder.encodeToString(signatureBytes);
    }

    public InvalidatedToken invalidateAllUserTokens(String email) {
        if (!jwtRepository.existsById(email)) {
            InvalidatedToken invalidatedToken = new InvalidatedToken();
            invalidatedToken.setEmail(email);
            invalidatedToken.setInvalidatedTokenTime(System.currentTimeMillis() / 1000);
            return jwtRepository.save(invalidatedToken);
        }
        return null;
    }
}
