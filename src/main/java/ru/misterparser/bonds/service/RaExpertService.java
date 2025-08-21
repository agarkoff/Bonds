package ru.misterparser.bonds.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
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
import ru.misterparser.bonds.config.RaExpertConfig;
import ru.misterparser.bonds.model.Rating;
import ru.misterparser.bonds.repository.BondRepository;
import ru.misterparser.bonds.repository.RatingRepository;
import ru.misterparser.bonds.util.RatingUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class RaExpertService {

    private static final Logger logger = LoggerFactory.getLogger(RaExpertService.class);
    private static final String BASE_URL = "https://raexpert.ru/ratings/debt_inst/";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final Pattern ISIN_PATTERN = Pattern.compile("([A-Z]{2}[A-Z0-9]{10})");

    @Autowired
    private RaExpertConfig raExpertConfig;

    @Autowired
    private RatingRepository ratingRepository;

    @Autowired
    private BondRepository bondRepository;

    @Transactional
    public void updateRatings() {
        if (!raExpertConfig.isEnabled()) {
            logger.info("RaExpert ratings update is disabled");
            return;
        }

        logger.info("Starting RaExpert ratings update");

        WebDriver driver = null;
        try {
            driver = createWebDriver();
            
            createCacheDirectory();
            
            List<String> bondUrls = collectBondUrls(driver);
            logger.info("Found {} bond URLs to process", bondUrls.size());

            int processed = 0;
            int successful = 0;
            int errors = 0;

            for (String bondUrl : bondUrls) {
                processed++;
                try {
                    List<Rating> ratings = parseBondPage(driver, bondUrl);
                    for (Rating rating : ratings) {
                        if (rating.getIsin() != null) {
                            ratingRepository.save(rating);
                            
                            // Обновляем рейтинг в таблице bonds
                            if (bondRepository.findByIsin(rating.getIsin()).isPresent()) {
                                bondRepository.updateRating(rating.getIsin(), rating.getRatingValue(), rating.getRatingCode());
                            }
                            
                            successful++;
                        }
                    }

                } catch (Exception e) {
                    errors++;
                    logger.debug("Error processing bond URL {}: {}", bondUrl, e.getMessage());
                }
            }

            logger.info("RaExpert ratings update completed - Processed: {}, Successful: {}, Errors: {}", 
                    processed, successful, errors);

        } catch (Exception e) {
            logger.error("Error during RaExpert ratings update", e);
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
        driver.manage().timeouts().pageLoadTimeout(raExpertConfig.getDelays().getRequestTimeout(), TimeUnit.SECONDS);
        
        return driver;
    }

    private void createCacheDirectory() throws IOException {
        Path cacheDir = Paths.get(raExpertConfig.getCache().getPath());
        if (!Files.exists(cacheDir)) {
            Files.createDirectories(cacheDir);
        }
    }

    private List<String> collectBondUrls(WebDriver driver) {
        List<String> bondUrls = new ArrayList<>();

        int pageIndex = 1;
        try {
            driver.get(BASE_URL);

            while (true) {
                WebElement currentPaginator = driver.findElement(By.cssSelector("span.b-paginator__link.-active"));
                logger.info("Текущая страница: {}", currentPaginator.getText());

                // Поиск ссылок на облигации в колонке "Эмиссия"
                List<WebElement> as = driver.findElements(By.cssSelector("div.b-actions__rates table tbody tr > td > span:first-child > a"));

                for (WebElement a : as) {
                    try {
                        String href = a.getAttribute("href");
                        if (href != null && !bondUrls.contains(href)) {
                            bondUrls.add(href);
                        }
                    } catch (Exception e) {
                        logger.debug("Error processing table row: {}", e.getMessage());
                    }
                }

                WebElement nextPaginator = currentPaginator.findElement(By.xpath("following-sibling::*[1]"));

                if (nextPaginator != null && !"»".equals(nextPaginator.getText())) {
                    logger.info("Переход на страницу {}", ++pageIndex);
                    nextPaginator.click();
                    Thread.sleep(2000); // Пауза для загрузки страницы
                } else {
                    logger.info("Страницы кончились");
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("Error collecting bond URLs", e);
        }
        
        return bondUrls;
    }

    private List<Rating> parseBondPage(WebDriver driver, String bondUrl) {
        List<Rating> ratings = new ArrayList<>();
        
        try {
            String pageContent = loadPageContent(driver, bondUrl);
            
            String isin = extractIsin(pageContent);
            String companyName = extractCompanyName(pageContent);
            
            if (isin != null) {
                List<Rating> bondRatings = extractRatings(pageContent, isin, companyName);
                ratings.addAll(bondRatings);
            }
            
        } catch (Exception e) {
            logger.debug("Error parsing bond page {}: {}", bondUrl, e.getMessage());
        }
        
        return ratings;
    }

    private String loadPageContent(WebDriver driver, String url) throws Exception {
        String cacheFileName = generateCacheFileName(url);
        Path cacheFilePath = Paths.get(raExpertConfig.getCache().getPath(), cacheFileName);
        
        // Проверяем кэш
        if (Files.exists(cacheFilePath)) {
            long fileAge = System.currentTimeMillis() - Files.getLastModifiedTime(cacheFilePath).toMillis();
            long maxAge = raExpertConfig.getCache().getExpiresDays() * 24 * 60 * 60 * 1000L;
            
            if (fileAge < maxAge) {
                logger.debug("Loading from cache: {}", cacheFileName);
                return Files.readString(cacheFilePath, StandardCharsets.UTF_8);
            }
        }

        // Пауза между запросами
        Thread.sleep(raExpertConfig.getDelays().getPageInterval() * 1000);
        
        // Загружаем страницу
        driver.get(url);
        String pageContent = driver.getPageSource();
        logger.info("Loaded from web: {}", url);
        
        // Сохраняем в кэш
        Files.writeString(cacheFilePath, pageContent, StandardCharsets.UTF_8);
        
        return pageContent;
    }

    private String generateCacheFileName(String url) {
        return url.replaceAll("[^a-zA-Z0-9]", "_") + ".html";
    }

    private String extractIsin(String pageContent) {
        Document doc = Jsoup.parse(pageContent);

        // Находим все элементы b-table__item
        Elements tableItems = doc.select("div.b-table__item");

        for (Element item : tableItems) {
            Element title = item.selectFirst("div.b-table__title");
            Element data = item.selectFirst("div.b-table__data");

            if (title != null && data != null) {
                String titleText = title.text().trim();
                if (titleText.equalsIgnoreCase("ISIN")) {
                    String isinValue = data.text().trim();
                    // Проверяем формат ISIN
                    if (isinValue.matches("[A-Z]{2}[A-Z0-9]{9}[0-9]")) {
                        return isinValue;
                    }
                }
            }
        }
        return null;
    }

    private String extractCompanyName(String pageContent) {
        Document doc = Jsoup.parse(pageContent);

        // Находим все элементы b-table__item
        Elements tableItems = doc.select("div.b-table__item");

        for (Element item : tableItems) {
            Element title = item.selectFirst("div.b-table__title");
            Element data = item.selectFirst("div.b-table__data");

            if (title != null && data != null) {
                String titleText = title.text().trim();
                if (titleText.equalsIgnoreCase("Компания")) {
                    return data.text().trim();
                }
            }
        }
        return null;
    }

    private List<Rating> extractRatings(String pageContent, String isin, String companyName) {
        List<Rating> ratings = new ArrayList<>();

        try {
            Document doc = Jsoup.parse(pageContent);

            // Находим таблицу рейтингов
            Elements ratingTable = doc.select("table.object-rating-table");

            if (!ratingTable.isEmpty()) {
                // Получаем все строки таблицы (кроме заголовка)
                Elements rows = ratingTable.select("tbody tr");

                for (Element row : rows) {
                    Elements cells = row.select("td");

                    // Проверяем, что есть минимум 2 колонки
                    if (cells.size() >= 2) {
                        // Извлекаем рейтинг из первого span в первой колонке
                        Element ratingCell = cells.get(0);
                        Elements ratingSpans = ratingCell.select("span");

                        if (!ratingSpans.isEmpty()) {
                            String ratingValue = ratingSpans.first().text().trim();

                            // Извлекаем дату из тега a во второй колонке
                            Element dateCell = cells.get(1);
                            Elements dateLinks = dateCell.select("a");

                            if (!dateLinks.isEmpty() && RatingUtils.isValidRating(ratingValue)) {
                                String dateStr = dateLinks.first().text().trim();

                                try {
                                    LocalDate ratingDate = LocalDate.parse(dateStr, DATE_FORMAT);

                                    Rating rating = new Rating(isin, ratingValue, ratingDate);
                                    rating.setCompanyName(companyName);
                                    rating.setRatingCode(RatingUtils.getRatingCode(ratingValue));

                                    ratings.add(rating);
                                } catch (Exception e) {
                                    logger.debug("Error parsing rating date: {}", dateStr);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error parsing ratings from page content", e);
        }

        return ratings;
    }
}