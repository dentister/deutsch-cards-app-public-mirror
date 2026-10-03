package com.kniazev.cards.word.telegram.web;

import static org.junit.jupiter.api.Assertions.*;

import com.kniazev.cards.word.telegram.web.TelegramLoginWidgetValidator.TelegramLoginUser;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

class TelegramLoginWidgetValidatorTests {

    private static final String TOKEN = "1234567890:FakeTestTokenForUnitTesting";

    @Test
    void acceptsValidPayloadAndExtractsUser() {
        TelegramLoginWidgetValidator validator = new TelegramLoginWidgetValidator(TOKEN, 60);

        TelegramLoginUser user = validator.validateAndExtractUser(
                buildPayload(TOKEN, Instant.now().getEpochSecond(), "vitalii"));

        assertEquals(42L, user.id());
        assertEquals("vitalii", user.username());
        assertEquals("Test", user.firstName());
    }

    @Test
    void rejectsTamperedHash() {
        TelegramLoginWidgetValidator validator = new TelegramLoginWidgetValidator(TOKEN, 60);

        Map<String, String> tampered = buildPayload(TOKEN, Instant.now().getEpochSecond(), "vitalii");
        tampered.put("hash", "deadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeef");

        assertThrows(SecurityException.class, () -> validator.validateAndExtractUser(tampered));
    }

    @Test
    void rejectsHashSignedWithAnotherToken() {
        // Signed with TOKEN, but the validator only knows a different token.
        TelegramLoginWidgetValidator validator = new TelegramLoginWidgetValidator("9999:CompletelyDifferentToken", 60);

        Map<String, String> payload = buildPayload(TOKEN, Instant.now().getEpochSecond(), "vitalii");

        assertThrows(SecurityException.class, () -> validator.validateAndExtractUser(payload));
    }

    @Test
    void rejectsExpiredAuthDate() {
        TelegramLoginWidgetValidator validator = new TelegramLoginWidgetValidator(TOKEN, 60);

        Map<String, String> stale = buildPayload(TOKEN, Instant.now().getEpochSecond() - 120, "vitalii");

        assertThrows(SecurityException.class, () -> validator.validateAndExtractUser(stale));
    }

    @Test
    void rejectsMissingAuthDate() {
        TelegramLoginWidgetValidator validator = new TelegramLoginWidgetValidator(TOKEN, 60);

        Map<String, String> payload = buildPayload(TOKEN, Instant.now().getEpochSecond(), "vitalii");
        payload.remove("auth_date");
        payload.remove("hash");
        payload.put("hash", sign(TOKEN, payload));

        assertThrows(SecurityException.class, () -> validator.validateAndExtractUser(payload));
    }

    @Test
    void rejectsEmptyPayload() {
        TelegramLoginWidgetValidator validator = new TelegramLoginWidgetValidator(TOKEN, 60);

        assertThrows(SecurityException.class, () -> validator.validateAndExtractUser(Map.of()));
        assertThrows(SecurityException.class, () -> validator.validateAndExtractUser(null));
    }

    @Test
    void acceptsPayloadWithoutUsername() {
        TelegramLoginWidgetValidator validator = new TelegramLoginWidgetValidator(TOKEN, 60);

        Map<String, String> payload = buildPayload(TOKEN, Instant.now().getEpochSecond(), null);

        TelegramLoginUser user = validator.validateAndExtractUser(payload);

        assertNull(user.username());
        assertEquals(42L, user.id());
    }

    /**
     * Builds a signed callback parameter map exactly as Telegram's Login Widget would
     * (secret key = SHA-256(bot token), not the Mini App's HMAC-with-"WebAppData" variant).
     */
    private static Map<String, String> buildPayload(String token, long authDate, String username) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("id", "42");
        fields.put("first_name", "Test");
        fields.put("auth_date", String.valueOf(authDate));
        if (username != null) {
            fields.put("username", username);
        }

        fields.put("hash", sign(token, fields));

        return fields;
    }

    private static String sign(String token, Map<String, String> fields) {
        String dataCheckString = new TreeMap<>(fields).entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("\n"));

        byte[] secret = sha256(token.getBytes(StandardCharsets.UTF_8));

        return hex(hmac(secret, dataCheckString.getBytes(StandardCharsets.UTF_8)));
    }

    private static byte[] sha256(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] hmac(byte[] key, byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }
}
