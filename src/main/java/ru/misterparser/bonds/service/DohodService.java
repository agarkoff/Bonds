package ru.misterparser.bonds.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.misterparser.bonds.config.DohodConfig;
import ru.misterparser.bonds.model.DohodRating;
import ru.misterparser.bonds.repository.TBankBondRepository;
import ru.misterparser.bonds.repository.DohodRatingRepository;
import ru.misterparser.bonds.util.RatingUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class DohodService {

    private static final String BASE_URL = "https://www.dohod.ru/analytic/bonds";

    private final DohodConfig dohodConfig;
    private final DohodRatingRepository dohodRatingRepository;
    private final TBankBondRepository tBankBondRepository;

    @Transactional
    public void updateRatings() {
        if (!dohodConfig.isEnabled()) {
            log.info("Dohod ratings update is disabled");
            return;
        }

        log.info("Starting Dohod ratings update");

        WebDriver driver = null;
        try {
            driver = createWebDriver();
            
            driver.get(BASE_URL);
            
            // Согласиться с cookies
            try {
                WebElement cookieButton = driver.findElement(By.cssSelector("span.cookiemsg__bottom_close"));
                cookieButton.click();
                log.debug("Cookie consent accepted");
                Thread.sleep(1000); // Небольшая пауза после закрытия cookie-баннера
            } catch (Exception e) {
                log.debug("Cookie consent button not found or already accepted: {}", e.getMessage());
            }
            
            int processed = 0;
            int successful = 0;
            int errors = 0;
            int pageNumber = 1;

            while (true) {
                log.info("Processing page {}", pageNumber);
                
                List<WebElement> bondRows = driver.findElements(By.cssSelector("tr.bonds__item"));
                
                if (bondRows.isEmpty()) {
                    log.info("No more bond items found on page {}", pageNumber);
                    break;
                }

                for (WebElement row : bondRows) {
                    processed++;
                    try {
                        String isin = extractIsin(row);
                        String ratingValue = extractRating(row);

                        if (isin != null && ratingValue != null && RatingUtils.isValidRating(ratingValue)) {
                            // Проверяем есть ли облигация в tbank_bonds по ISIN
                            boolean bondExists = tBankBondRepository.findAll().stream()
                                .anyMatch(bond -> isin.equals(bond.getTicker()));
                            
                            if (bondExists) {
                                DohodRating rating = new DohodRating(
                                    isin, ratingValue, RatingUtils.getRatingCode(ratingValue)
                                );

                                dohodRatingRepository.saveOrUpdate(rating);
                                successful++;
                                log.debug("Successfully processed rating for ISIN: {}, Rating: {}", isin, ratingValue);
                            } else {
                                log.debug("Skipping rating for ISIN {}: not found in tbank_bonds", isin);
                            }
                        }

                    } catch (Exception e) {
                        errors++;
                        log.debug("Error processing bond row on page {}: {}", pageNumber, e.getMessage());
                    }
                }

                // Попытка перейти на следующую страницу
                try {
                    WebElement currentButton = driver.findElement(By.cssSelector("a.paginate_button.current"));
                    WebElement nextButton = currentButton.findElement(By.xpath("following-sibling::a[1]"));
                    
                    if (nextButton == null || nextButton.getAttribute("class").contains("disabled")) {
                        log.info("Reached last page");
                        break;
                    }
                    
                    nextButton.click();
                    pageNumber++;
                    
                    // Пауза между запросами страниц
                    Thread.sleep(dohodConfig.getDelays().getPageInterval() * 1000);
                    
                } catch (Exception e) {
                    log.info("No next page button found or unable to navigate to next page");
                    break;
                }
            }

            log.info("Dohod ratings update completed - Processed: {}, Successful: {}, Errors: {}, Pages: {}", 
                    processed, successful, errors, pageNumber);

        } catch (Exception e) {
            log.error("Error during Dohod ratings update", e);
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }

    private WebDriver createWebDriver() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--remote-allow-origins=*");
        
        WebDriver driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
        driver.manage().timeouts().pageLoadTimeout(dohodConfig.getDelays().getRequestTimeout(), TimeUnit.SECONDS);
        
        return driver;
    }

    private String extractIsin(WebElement row) {
        try {
            WebElement shortnameCell = row.findElement(By.cssSelector("td.shortname"));
            return shortnameCell.getAttribute("data-open-isin");
        } catch (Exception e) {
            log.debug("Error extracting ISIN: {}", e.getMessage());
            return null;
        }
    }

    private String extractRating(WebElement row) {
        try {
            WebElement ratingCell = row.findElement(By.cssSelector("td.credit_rating_text"));
            WebElement ratingSpan = ratingCell.findElement(By.tagName("span"));
            return "ru" + ratingSpan.getText().trim();
        } catch (Exception e) {
            log.debug("Error extracting rating: {}", e.getMessage());
            return null;
        }
    }
}