package com.kniazev.cards.word.ai.draft;

import com.google.genai.errors.ApiException;
import com.kniazev.cards.word.ai.draft.DraftTask.TaskStatus;
import com.kniazev.cards.word.ai.draft.WordDraftMapper.ValidationResult;
import com.kniazev.cards.word.model.dictionary.Word;
import com.kniazev.cards.word.service.WordService;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Single-threaded background job that walks the {@code tasks} queue one task at a time, asks the AI to draft a
 * word card for it (same prompt as the admin bot) and saves the word with {@code created_by = "job"}.
 * <p>
 * It is paced to stay inside the free tier of the Gemini API: a fixed pause between requests (requests-per-minute
 * limit), a daily request budget (requests-per-day limit, which Google resets at midnight Pacific time) and a
 * back-off when the API still answers 429. Off by default, see {@code word.job.*}.
 * <p>
 * Not transactional as a whole - the slow AI call must not hold a DB connection; every DB write is its own
 * short transaction.
 */
@Slf4j
@RequiredArgsConstructor

@Component
@ConditionalOnProperty(name = "word.job.enabled", havingValue = "true")
public class WordDraftJob {
    public static final String CREATED_BY = "job";
    
    static final ZoneId QUOTA_ZONE = ZoneId.of("America/Los_Angeles");

    private static final int MAX_ATTEMPTS = 3;
    private static final int MAX_ERROR_LENGTH = 500;
    private static final Pattern QUOTA_CODE = Pattern.compile("\\b429\\b");
    /** Above this share of the daily budget a 429 is treated as "the day is used up", not as a burst limit. */
    private static final double DAY_EXHAUSTED_SHARE = 0.8;

    private final DraftTaskRepository taskRepository;
    private final WordDraftService wordDraftService;
    private final WordService wordService;
    private final Clock clock;

    @Value("${word.job.daily-budget:400}")
    private int dailyBudget;

    @Value("${word.job.quota-backoff:30m}")
    private Duration quotaBackoff;

    private LocalDate quotaDay;
    private int callsToday;
    private Instant pausedUntil = Instant.EPOCH;

    @Scheduled(fixedDelayString = "${word.job.pause-seconds:10}", initialDelayString = "${word.job.initial-delay-seconds:60}", timeUnit = TimeUnit.SECONDS)
    public void run() {
        runOnce();
    }

    private void runOnce() {
        Instant now = clock.instant();
        LocalDate today = LocalDate.now(clock.withZone(QUOTA_ZONE));

        if (!today.equals(quotaDay)) {
            quotaDay = today;
            callsToday = 0;
        }

        if (now.isBefore(pausedUntil)) {
            return;
        }

        if (callsToday >= dailyBudget) {
            pausedUntil = startOfNextQuotaDay(today);
            log.info("Word job: daily budget of {} requests used up, resuming at {}", dailyBudget, pausedUntil);
            return;
        }

        Result result = processNext();

        switch (result) {
            case SAVED, FAILED -> {
                callsToday++;
                log.info("Word job: {} requests today (budget {})", callsToday, dailyBudget);
            }
            case QUOTA_EXHAUSTED -> {
                pausedUntil = callsToday >= dailyBudget * DAY_EXHAUSTED_SHARE
                        ? startOfNextQuotaDay(today)
                        : now.plus(quotaBackoff);
                log.warn("Word job: API quota exhausted after {} requests today, paused until {}", callsToday, pausedUntil);
            }
            case IDLE, SKIPPED_EXISTING -> {
            }
        }
    }

    private Result processNext() {
        Optional<DraftTask> next = taskRepository.findFirstByStatusOrderByAttemptsAscIdAsc(TaskStatus.NEW);
        if (next.isEmpty()) {
            return Result.IDLE;
        }

        DraftTask task = next.get();

        if (wordService.findOneByDeAndWordType(task.getDeWord(), task.getWordType()).isPresent()) {
            markDone(task);
            log.info("Task {} [{} {}]: already in dictionary", task.getId(), task.getWordType(), task.getDeWord());
            return Result.SKIPPED_EXISTING;
        }

        WordDraft draft;
        try {
            draft = wordDraftService.generateDraftOrThrow(task.getDeWord());
        } catch (Exception e) {
            if (isQuotaExceeded(e)) {
                log.warn("Task {} [{}]: AI quota exhausted: {}", task.getId(), task.getDeWord(), e.getMessage());
                return Result.QUOTA_EXHAUSTED;
            }
            return fail(task, "AI call failed: " + e.getMessage());
        }

        if (draft == null) {
            return fail(task, "empty AI response");
        }

        if (draft.wordType() != task.getWordType()) {
            return fail(task, "type mismatch: AI=" + draft.wordType() + " task=" + task.getWordType());
        }

        ValidationResult validation = WordDraftMapper.validate(draft);
        if (validation.hasErrors()) {
            return fail(task, "invalid draft: " + String.join(" ", validation.errors()));
        }

        try {
            Word word = WordDraftMapper.toEntity(draft);
            word.setCreatedBy(CREATED_BY);

            Word saved = wordService.createOrNothing(word);

            markDone(task);
            log.info("Task {} [{} {}]: saved word id={}", task.getId(), task.getWordType(), task.getDeWord(), saved.getId());
            return Result.SAVED;
        } catch (RuntimeException e) {
            return fail(task, "save failed: " + e.getMessage());
        }
    }

    private void markDone(DraftTask task) {
        task.setStatus(TaskStatus.DONE);
        task.setError(null);
        taskRepository.save(task);
    }

    private Result fail(DraftTask task, String reason) {
        task.setAttempts(task.getAttempts() + 1);
        task.setError(StringUtils.abbreviate(reason, MAX_ERROR_LENGTH));

        if (task.getAttempts() >= MAX_ATTEMPTS) {
            task.setStatus(TaskStatus.FAILED);
        }

        taskRepository.save(task);

        log.warn("Task {} [{} {}]: attempt {}/{} failed: {}", task.getId(), task.getWordType(), task.getDeWord(), task.getAttempts(), MAX_ATTEMPTS, reason);
        return Result.FAILED;
    }

    private static boolean isQuotaExceeded(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause() == t ? null : t.getCause()) {
            if (t instanceof ApiException api && api.code() == 429) {
                return true;
            }
            String message = t.getMessage();
            if (message != null && (message.contains("RESOURCE_EXHAUSTED") || QUOTA_CODE.matcher(message).find())) {
                return true;
            }
        }
        return false;
    }

    private static Instant startOfNextQuotaDay(LocalDate today) {
        return today.plusDays(1).atStartOfDay(QUOTA_ZONE).toInstant();
    }
    
    private enum Result {
        IDLE, SKIPPED_EXISTING, SAVED, FAILED, QUOTA_EXHAUSTED
    }

}
