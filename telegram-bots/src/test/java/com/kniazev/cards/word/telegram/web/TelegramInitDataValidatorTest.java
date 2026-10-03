package com.kniazev.cards.word.telegram.web;

import static org.junit.jupiter.api.Assertions.*;

import com.kniazev.cards.word.telegram.web.TelegramInitDataValidator.TelegramUser;

import org.junit.jupiter.api.Test;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

class TelegramInitDataValidatorTest {

    private static final String TOKEN = "1234567890:FakeTestTokenForUnitTesting";

    @Test
    void acceptsValidInitDataAndExtractsUser() throws Exception {
        TelegramInitDataValidator validator = new TelegramInitDataValidator(TOKEN, 86400);

        TelegramUser user = validator.validateAndExtractUser(
                buildInitData(TOKEN, Instant.now().getEpochSecond(), "vitalii"));

        assertEquals(42L, user.id());
        assertEquals("vitalii", user.username());
    }

    @Test
    void rejectsTamperedHash() throws Exception {
        TelegramInitDataValidator validator = new TelegramInitDataValidator(TOKEN, 86400);

        String tampered = buildInitData(TOKEN, Instant.now().getEpochSecond(), "vitalii")
                .replaceAll("hash=[0-9a-f]+", "hash=deadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeef");

        assertThrows(SecurityException.class, () -> validator.validateAndExtractUser(tampered));
    }

    @Test
    void rejectsHashSignedWithAnotherToken() throws Exception {
        // Signed with TOKEN, but the validator only knows a different token.
        TelegramInitDataValidator validator = new TelegramInitDataValidator("9999:CompletelyDifferentToken", 86400);

        assertThrows(SecurityException.class, () -> validator.validateAndExtractUser(
                buildInitData(TOKEN, Instant.now().getEpochSecond(), "vitalii")));
    }

    @Test
    void rejectsExpiredInitData() throws Exception {
        TelegramInitDataValidator validator = new TelegramInitDataValidator(TOKEN, 3600);

        String stale = buildInitData(TOKEN, Instant.now().getEpochSecond() - 7200, "vitalii");

        assertThrows(SecurityException.class, () -> validator.validateAndExtractUser(stale));
    }

    @Test
    void rejectsEmptyInitData() {
        TelegramInitDataValidator validator = new TelegramInitDataValidator(TOKEN, 86400);

        assertThrows(SecurityException.class, () -> validator.validateAndExtractUser(""));
        assertThrows(SecurityException.class, () -> validator.validateAndExtractUser(null));
    }

    /**
     * Builds a signed initData query string exactly as Telegram would: the hash is
     * computed over the decoded {@code key=value} fields (sorted, excluding hash), then
     * the values are URL-encoded into the query string.
     */
    private static String buildInitData(String token, long authDate, String username) throws Exception {
        String userJson = "{\"id\":42,\"first_name\":\"Test\",\"username\":\"" + username + "\"}";

        Map<String, String> decoded = new LinkedHashMap<>();
        decoded.put("auth_date", String.valueOf(authDate));
        decoded.put("query_id", "AAABBBCCC");
        decoded.put("user", userJson);

        String dataCheckString = decoded.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("\n"));

        byte[] secret = hmac("WebAppData".getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8));
        String hash = hex(hmac(secret, dataCheckString.getBytes(StandardCharsets.UTF_8)));

        return "auth_date=" + enc(String.valueOf(authDate))
                + "&query_id=" + enc("AAABBBCCC")
                + "&user=" + enc(userJson)
                + "&hash=" + hash;
    }

    private static byte[] hmac(byte[] key, byte[] data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data);
    }

    private static String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
