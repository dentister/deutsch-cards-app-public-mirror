package com.kniazev.cards.word.telegram;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Validates the {@code initData} string that a Telegram Mini App sends with every
 * request, following Telegram's documented algorithm:
 *
 * <ol>
 *   <li>secret key = HMAC-SHA256(key = "WebAppData", data = bot token)</li>
 *   <li>data-check-string = all received fields except {@code hash} and {@code signature},
 *       sorted alphabetically, joined as {@code key=value} with '\n'</li>
 *   <li>expected hash = HMAC-SHA256(key = secret key, data = data-check-string), hex-encoded</li>
 * </ol>
 *
 * <p>The identity of the caller is derived <b>only</b> from a valid, signed initData —
 * never from the request body.
 */
@Slf4j
@Component
public class TelegramInitDataValidator {

    private final byte[] secretKey;
    private final long maxAgeSeconds;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TelegramInitDataValidator(@Value("${telegram.bot.token:}") String botToken,
                                     @Value("${miniapp.initdata.max-age-seconds:86400}") long maxAgeSeconds) {
        this.secretKey = hmacSha256("WebAppData".getBytes(StandardCharsets.UTF_8),
                botToken.getBytes(StandardCharsets.UTF_8));
        this.maxAgeSeconds = maxAgeSeconds;
    }

    /** Minimal projection of the Telegram user carried inside initData. */
    public record TelegramUser(long id, String username, String firstName) {}

    /**
     * @throws SecurityException if the signature is missing/invalid, the data is expired,
     *                           or the user object is absent/unparseable.
     */
    public TelegramUser validateAndExtractUser(String initData) {
        if (initData == null || initData.isBlank()) {
            log.warn("Mini App auth failed: initData is empty (header missing, or app opened outside Telegram)");
            throw new SecurityException("Empty initData");
        }

        Map<String, String> params = new TreeMap<>();
        for (String pair : initData.split("&")) {
            int eq = pair.indexOf('=');
            if (eq < 0) {
                continue;
            }
            params.put(urlDecode(pair.substring(0, eq)), urlDecode(pair.substring(eq + 1)));
        }

        String hash = params.get("hash");
        if (hash == null) {
            log.warn("Mini App auth failed: initData has no hash. fields={}", params.keySet());
            throw new SecurityException("initData has no hash");
        }

        // Telegram's handling of the newer `signature` field inside the data-check-string
        // varies across clients/versions, so accept the hash whether or not `signature`
        // is part of it. Both variants still require a valid bot-token HMAC.
        Map<String, String> withoutSignature = new TreeMap<>(params);
        withoutSignature.remove("hash");
        withoutSignature.remove("signature");

        Map<String, String> withSignature = new TreeMap<>(params);
        withSignature.remove("hash");

        if (!hashMatches(withoutSignature, hash) && !hashMatches(withSignature, hash)) {
            log.warn("Mini App auth failed: hash mismatch. fields={}, initDataLen={}, hasSignature={}",
                    params.keySet(), initData.length(), params.containsKey("signature"));
            throw new SecurityException("initData hash mismatch");
        }

        String authDate = params.get("auth_date");
        if (maxAgeSeconds > 0 && authDate != null) {
            try {
                long age = Instant.now().getEpochSecond() - Long.parseLong(authDate);
                if (age > maxAgeSeconds) {
                    log.warn("Mini App auth failed: initData expired (age={}s, max={}s)", age, maxAgeSeconds);
                    throw new SecurityException("initData expired");
                }
            } catch (NumberFormatException ignored) {
                // no usable auth_date — leave freshness unchecked
            }
        }

        String userJson = params.get("user");
        if (userJson == null) {
            log.warn("Mini App auth failed: initData has no user field. fields={}", params.keySet());
            throw new SecurityException("initData has no user");
        }
        try {
            JsonNode node = objectMapper.readTree(userJson);
            long id = node.get("id").asLong();
            String username = node.hasNonNull("username") ? node.get("username").asText() : null;
            String firstName = node.hasNonNull("first_name") ? node.get("first_name").asText() : null;
            return new TelegramUser(id, username, firstName);
        } catch (Exception e) {
            throw new SecurityException("initData user is not parseable", e);
        }
    }

    private boolean hashMatches(Map<String, String> fields, String providedHash) {
        String dataCheckString = fields.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("\n"));
        String expected = toHex(hmacSha256(secretKey, dataCheckString.getBytes(StandardCharsets.UTF_8)));
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                providedHash.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] hmacSha256(byte[] key, byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA256 failed", e);
        }
    }

    private static String urlDecode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }
}
