package com.kniazev.cards.word.telegram.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Validates the callback query parameters that Telegram's "Login Widget" redirects the
 * browser to after the user approves login, following Telegram's documented algorithm:
 *
 * <ol>
 *   <li>secret key = SHA-256(bot token) - a plain digest, unlike the Mini App's
 *       HMAC-SHA256(key = "WebAppData", data = bot token)</li>
 *   <li>data-check-string = all received fields except {@code hash}, sorted alphabetically,
 *       joined as {@code key=value} with '\n'</li>
 *   <li>expected hash = HMAC-SHA256(key = secret key, data = data-check-string), hex-encoded</li>
 * </ol>
 *
 * <p>The identity of the caller is derived <b>only</b> from a valid, signed callback -
 * never from the request body.
 */
@Slf4j
@Component
public class TelegramLoginWidgetValidator {

    private final byte[] secretKey;
    private final long maxAgeSeconds;

    /** Minimal projection of the Telegram user carried inside a Login Widget callback. */
    public record TelegramLoginUser(long id, String username, String firstName, String lastName, String photoUrl) {}

    public TelegramLoginWidgetValidator(@Value("${telegram.bot.token:}") String botToken,
                                         @Value("${telegram.login.max-age-seconds:60}") long maxAgeSeconds) {
        this.secretKey = sha256(botToken.getBytes(StandardCharsets.UTF_8));
        this.maxAgeSeconds = maxAgeSeconds;
    }

    /**
     * @throws SecurityException if the signature is missing/invalid, or the data is expired.
     */
    public TelegramLoginUser validateAndExtractUser(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            log.warn("Telegram login failed: callback has no parameters");
            throw new SecurityException("Empty callback");
        }

        String hash = params.get("hash");
        if (hash == null || hash.isBlank()) {
            log.warn("Telegram login failed: callback has no hash. fields={}", params.keySet());
            throw new SecurityException("Callback has no hash");
        }

        Map<String, String> withoutHash = new TreeMap<>(params);
        withoutHash.remove("hash");

        if (!hashMatches(withoutHash, hash)) {
            log.warn("Telegram login failed: hash mismatch. fields={}", params.keySet());
            throw new SecurityException("Callback hash mismatch");
        }

        String authDate = params.get("auth_date");
        if (authDate == null) {
            log.warn("Telegram login failed: callback has no auth_date. fields={}", params.keySet());
            throw new SecurityException("Callback has no auth_date");
        }

        try {
            long age = Instant.now().getEpochSecond() - Long.parseLong(authDate);
            if (age > maxAgeSeconds) {
                log.warn("Telegram login failed: callback expired (age={}s, max={}s)", age, maxAgeSeconds);
                throw new SecurityException("Callback expired");
            }
        } catch (NumberFormatException e) {
            log.warn("Telegram login failed: auth_date is not parseable: {}", authDate);
            throw new SecurityException("auth_date is not parseable", e);
        }

        String id = params.get("id");
        if (id == null) {
            log.warn("Telegram login failed: callback has no id. fields={}", params.keySet());
            throw new SecurityException("Callback has no id");
        }

        try {
            return new TelegramLoginUser(Long.parseLong(id), params.get("username"), params.get("first_name"),
                    params.get("last_name"), params.get("photo_url"));
        } catch (NumberFormatException e) {
            throw new SecurityException("id is not parseable", e);
        }
    }

    private boolean hashMatches(Map<String, String> fields, String providedHash) {
        String dataCheckString = fields.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("\n"));
        String expected = toHex(hmacSha256(secretKey, dataCheckString.getBytes(StandardCharsets.UTF_8)));

        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), providedHash.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] sha256(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 failed", e);
        }
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

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);

        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }

        return sb.toString();
    }
}
