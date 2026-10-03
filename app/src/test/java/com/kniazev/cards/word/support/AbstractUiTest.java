package com.kniazev.cards.word.support;

import com.kniazev.cards.word.WordCardsApplication;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.assertions.PlaywrightAssertions;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Base class for browser tests: the whole application with Vaadin switched on (and the real security chain, so the login
 * form works), listening on a random port, driven by a real headless Chromium through Playwright.
 * <p>
 * Containers, the {@code WordEnrichment} mock and the per-test data reset come from {@link AbstractIntegrationTest}.
 * Run with {@code -Dplaywright.headed=true} to watch the browser.
 */
@Tag("ui")
@SpringBootTest(
        classes = WordCardsApplication.class,
        webEnvironment = WebEnvironment.RANDOM_PORT,
        properties = {
                "telegram.bot.enabled=false",
                "admin.bot.enabled=false",
                "word.job.enabled=false",
                "spring.ai.google.genai.api-key=test-key-not-used"})
public abstract class AbstractUiTest extends AbstractIntegrationTest {

    private static final double DEFAULT_TIMEOUT_MS = 15_000;
    private static final long FRONTEND_BUILD_TIMEOUT_MS = 180_000;

    private static Playwright playwright;
    private static Browser browser;
    private static boolean frontendReady;

    @LocalServerPort
    private int port;

    protected BrowserContext context;
    protected Page page;

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium()
                .launch(new BrowserType.LaunchOptions().setHeadless(!Boolean.getBoolean("playwright.headed")));
    }

    @AfterAll
    static void closeBrowser() {
        browser.close();
        playwright.close();
    }

    @BeforeEach
    void openPage() {
        context = browser.newContext();
        page = context.newPage();
        page.setDefaultTimeout(DEFAULT_TIMEOUT_MS);
        PlaywrightAssertions.setDefaultAssertionTimeout(DEFAULT_TIMEOUT_MS);

        awaitFrontend();
    }

    @AfterEach
    void closePage() {
        context.close();
    }

    /**
     * Vaadin builds its dev-mode frontend bundle in the background after the context is up (about 15 seconds on a clean
     * checkout, nothing once it is cached), and a page requested meanwhile never renders. So the first test of the JVM
     * reloads the login page until the form shows up.
     */
    private void awaitFrontend() {
        if (frontendReady) {
            return;
        }

        long deadline = System.currentTimeMillis() + FRONTEND_BUILD_TIMEOUT_MS;

        while (true) {
            page.navigate(url("/login"));

            try {
                page.getByLabel("Username", new Page.GetByLabelOptions().setExact(true)).waitFor(new Locator.WaitForOptions().setTimeout(3_000));

                frontendReady = true;

                return;
            } catch (PlaywrightException e) {
                if (System.currentTimeMillis() > deadline) {
                    throw new IllegalStateException("The Vaadin frontend did not come up in time", e);
                }
            }
        }
    }

    protected String url(String path) {
        return "http://localhost:" + port + path;
    }
}
