package ru.misterparser.bonds.service;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.misterparser.bonds.config.DohodConfig;
import ru.misterparser.bonds.model.Rating;
import ru.misterparser.bonds.repository.BondRepository;
import ru.misterparser.bonds.repository.RatingRepository;
import ru.misterparser.bonds.util.RatingUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class DohodService {

    private static final Logger logger = LoggerFactory.getLogger(DohodService.class);
    private static final String BASE_URL = "https://www.dohod.ru/analytic/bonds";

    @Autowired
    private DohodConfig dohodConfig;

    @Autowired
    private RatingRepository ratingRepository;

    @Autowired
    private BondRepository bondRepository;

    @Transactional
    public void updateRatings() {
        if (!dohodConfig.isEnabled()) {
            logger.info("Dohod ratings update is disabled");
            return;
        }

        logger.info("Starting Dohod ratings update");

        WebDriver driver = null;
        try {
            driver = createWebDriver();
            
            driver.get(BASE_URL);
            
            // Согласиться с cookies
            try {
                WebElement cookieButton = driver.findElement(By.cssSelector("span.cookiemsg__bottom_close"));
                cookieButton.click();
                logger.debug("Cookie consent accepted");
                Thread.sleep(1000); // Небольшая пауза после закрытия cookie-баннера
            } catch (Exception e) {
                logger.debug("Cookie consent button not found or already accepted: {}", e.getMessage());
            }
            
            int processed = 0;
            int successful = 0;
            int errors = 0;
            int pageNumber = 1;

            while (true) {
                logger.info("Processing page {}", pageNumber);
                
                List<WebElement> bondRows = driver.findElements(By.cssSelector("tr.bonds__item"));
                
                if (bondRows.isEmpty()) {
                    logger.info("No more bond items found on page {}", pageNumber);
                    break;
                }

                for (WebElement row : bondRows) {
                    processed++;
                    try {
                        String isin = extractIsin(row);
                        String ratingValue = extractRating(row);

                        if (isin != null && ratingValue != null && RatingUtils.isValidRating(ratingValue)) {
                            Rating rating = new Rating(isin, ratingValue, LocalDate.now());
                            rating.setRatingCode(RatingUtils.getRatingCode(ratingValue));

                            ratingRepository.save(rating);

                            // Обновляем рейтинг в таблице bonds
                            if (bondRepository.findByIsin(isin).isPresent()) {
                                bondRepository.updateRating(isin, ratingValue, rating.getRatingCode());
                            }

                            successful++;
                            logger.debug("Successfully processed rating for ISIN: {}, Rating: {}", isin, ratingValue);
                        }

                    } catch (Exception e) {
                        errors++;
                        logger.debug("Error processing bond row on page {}: {}", pageNumber, e.getMessage());
                    }
                }

                // Попытка перейти на следующую страницу
                try {
                    WebElement currentButton = driver.findElement(By.cssSelector("a.paginate_button.current"));
                    WebElement nextButton = currentButton.findElement(By.xpath("following-sibling::a[1]"));
                    
                    if (nextButton == null || nextButton.getAttribute("class").contains("disabled")) {
                        logger.info("Reached last page");
                        break;
                    }
                    
                    nextButton.click();
                    pageNumber++;
                    
                    // Пауза между запросами страниц
                    Thread.sleep(dohodConfig.getDelays().getPageInterval() * 1000);
                    
                } catch (Exception e) {
                    logger.info("No next page button found or unable to navigate to next page");
                    break;
                }
            }

            logger.info("Dohod ratings update completed - Processed: {}, Successful: {}, Errors: {}, Pages: {}", 
                    processed, successful, errors, pageNumber);

        } catch (Exception e) {
            logger.error("Error during Dohod ratings update", e);
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
            logger.debug("Error extracting ISIN: {}", e.getMessage());
            return null;
        }
    }

    private String extractRating(WebElement row) {
        try {
            WebElement ratingCell = row.findElement(By.cssSelector("td.credit_rating_text"));
            WebElement ratingSpan = ratingCell.findElement(By.tagName("span"));
            return "ru" + ratingSpan.getText().trim();
        } catch (Exception e) {
            logger.debug("Error extracting rating: {}", e.getMessage());
            return null;
        }
    }
}