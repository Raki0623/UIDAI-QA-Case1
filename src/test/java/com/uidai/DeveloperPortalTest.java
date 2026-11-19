package com.uidai;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeveloperPortalKeyTest {

    private WebDriver driver;
    private WebDriverWait wait;

    private static final Pattern FULL_KEY =
            Pattern.compile("sk_test_[A-Za-z0-9]+");

    private static final Pattern MASKED_KEY =
            Pattern.compile("sk_test_\\*{4}[A-Za-z0-9]{4}");

    @BeforeEach
    void setUp() {

        ChromeOptions options = new ChromeOptions();

        options.addArguments(
                "--headless=new",
                "--window-size=1440,1000",
                "--no-sandbox",
                "--disable-dev-shm-usage"
        );

        driver = new ChromeDriver(options);

        wait = new WebDriverWait(
                driver,
                Duration.ofSeconds(15)
        );
    }

    @AfterEach
    void tearDown() {

        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    void generatedKeyIsVisibleForTenSecondsThenMasked() {

        DeveloperPortalPage portal =
                new DeveloperPortalPage(driver, wait);

        portal.openDashboard(
                "http://localhost:3000/developer/dashboard"
        );

        long plainTextObservedAt =
                portal.generateNewKeyAndWaitForPlainText();

        String plainTextKey = portal.keyText();

        assertTrue(
                FULL_KEY.matcher(plainTextKey).matches(),
                "Expected a full plain-text test key"
        );

        assertFalse(
                plainTextKey.contains("****"),
                "The key must initially be visible"
        );

        portal.waitUntilKeyIsMasked();

        long maskedAt = System.nanoTime();

        long visibleMillis =
                (maskedAt - plainTextObservedAt) / 1_000_000;

        assertTrue(
                visibleMillis >= 10_000,
                "The key was masked too early: "
                        + visibleMillis + " ms"
        );

        assertTrue(
                MASKED_KEY.matcher(portal.keyText()).matches(),
                "Expected the key to be masked with its final four characters"
        );
    }

    static final class DeveloperPortalPage {

        private static final By GENERATE_NEW_KEY =
                By.cssSelector("[data-testid='generate-new-key']");

        private static final By API_KEY =
                By.cssSelector("[data-testid='api-key-value']");

        private final WebDriver driver;
        private final WebDriverWait wait;

        DeveloperPortalPage(
                WebDriver driver,
                WebDriverWait wait
        ) {
            this.driver = driver;
            this.wait = wait;
        }

        void openDashboard(String url) {

            driver.get(url);

            wait.until(
                    ExpectedConditions.urlContains(
                            "/developer/dashboard"
                    )
            );

            wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            GENERATE_NEW_KEY
                    )
            );
        }

        long generateNewKeyAndWaitForPlainText() {

            wait.until(
                    ExpectedConditions.elementToBeClickable(
                            GENERATE_NEW_KEY
                    )
            ).click();

            wait.until(driver -> {

                String value = driver.findElement(API_KEY)
                        .getText().trim();

                return FULL_KEY.matcher(value).matches();

            });

            return System.nanoTime();
        }

        void waitUntilKeyIsMasked() {

            wait.until(driver -> {

                WebElement keyElement =
                        driver.findElement(API_KEY);

                String value = keyElement.getText().trim();

                return MASKED_KEY.matcher(value).matches();

            });
        }

        String keyText() {

            return wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            API_KEY
                    )
            ).getText().trim();
        }
    }
}
