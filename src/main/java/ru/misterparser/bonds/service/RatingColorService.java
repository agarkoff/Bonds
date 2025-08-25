package ru.misterparser.bonds.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class RatingColorService {

    private List<String> colors = new ArrayList<>();
    private Map<String, String> ratingBalls = new HashMap<>();

    @PostConstruct
    public void init() {
        loadColors();
        loadRatingBalls();
    }

    public void loadColors() {
        try {
            ClassPathResource resource = new ClassPathResource("rating-gradient.js");
            String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            // Парсим JS файл для извлечения hex цветов
            parseColorsFromJS(content);
            
            log.info("Loaded {} colors from rating-gradient.js", colors.size());
            
        } catch (IOException e) {
            log.error("Failed to load rating-gradient.js, using default colors", e);
            loadDefaultColors();
        }
    }

    private void parseColorsFromJS(String jsContent) {
        // Регулярное выражение для поиска hex цветов в формате hex: '#XXXXXX'
        Pattern pattern = Pattern.compile("hex:\\s*'(#[0-9A-Fa-f]{6})'");
        Matcher matcher = pattern.matcher(jsContent);
        
        colors.clear();
        while (matcher.find()) {
            colors.add(matcher.group(1));
        }
        
        if (colors.isEmpty()) {
            log.warn("No colors found in JS file, loading defaults");
            loadDefaultColors();
        }
    }

    private void loadDefaultColors() {
        colors.clear();
        // Fallback цвета если файл не загрузился
        colors.add("#CC0000"); // Тёмно-красный
        colors.add("#CC1500"); 
        colors.add("#CC2900"); 
        colors.add("#CC3D00"); 
        colors.add("#CC5200"); 
        colors.add("#CC6600"); 
        colors.add("#CC7A00"); 
        colors.add("#CC8F00"); 
        colors.add("#CCA300"); 
        colors.add("#CCB800"); 
        colors.add("#CCCC00"); 
        colors.add("#B8CC00"); 
        colors.add("#A3CC00"); 
        colors.add("#8FCC00"); 
        colors.add("#7ACC00"); 
        colors.add("#66CC00"); 
        colors.add("#52CC00"); 
        colors.add("#3DCC00"); 
        colors.add("#29CC00"); 
        colors.add("#15CC00"); 
        colors.add("#00CC00"); // Тёмно-зелёный
    }

    /**
     * Возвращает цвет для рейтинга облигации
     * Градиент от красного (рискованные) до зелёного (надёжные)
     * 
     * @param ratingCode числовой код рейтинга (чем меньше код, тем лучше рейтинг)
     * @return цвет в формате hex
     */
    public String getRatingColor(Integer ratingCode) {
        if (ratingCode == null) {
            return "#6c757d"; // Серый для отсутствующего рейтинга
        }

        if (colors.isEmpty()) {
            loadDefaultColors();
        }

        // Диапазон рейтингов согласно ratings.md (с учётом дыр в шкале):
        // 48-54: отличные рейтинги (ruAAA - ruA-) - зелёные цвета (лучшие - самые зелёные)
        // 146-154: хорошие/средние рейтинги (ruBBB+ - ruB-) - жёлто-оранжевые цвета
        // 249-253: низкие рейтинги (ruCCC - ruD) - красные цвета (худшие - самые красные)

        int totalColors = colors.size();

        if (ratingCode >= 48 && ratingCode <= 54) {
            // Группа A: ruAAA - ruA- (коды 48-54) - зелёные оттенки (последняя треть массива)
            // ruAAA (48) -> самый зелёный (последний цвет), ruA- (54) -> менее зелёный
            double groupRatio = (ratingCode - 48.0) / (54.0 - 48.0);
            int greenStart = (int) (totalColors * 0.67); // Начало зелёной зоны (67%)
            int colorIndex = (int) (totalColors - 1 - groupRatio * (totalColors - 1 - greenStart));
            return colors.get(Math.max(greenStart, Math.min(totalColors - 1, colorIndex)));
            
        } else if (ratingCode >= 146 && ratingCode <= 154) {
            // Группа B: ruBBB+ - ruB- (коды 146-154) - жёлто-оранжевые оттенки (средняя треть)
            // ruBBB+ (146) -> более зелёный, ruB- (154) -> более оранжевый
            double groupRatio = (ratingCode - 146.0) / (154.0 - 146.0);
            int orangeStart = (int) (totalColors * 0.33); // Начало оранжевой зоны (33%)
            int orangeEnd = (int) (totalColors * 0.67); // Конец оранжевой зоны (67%)
            int colorIndex = (int) (orangeEnd - 1 - groupRatio * (orangeEnd - orangeStart - 1));
            return colors.get(Math.max(orangeStart, Math.min(orangeEnd - 1, colorIndex)));

        } else if (ratingCode >= 249 && ratingCode <= 253) {
            // Группа C: ruCCC - ruD (коды 249-253) - красные оттенки (первая треть)
            // ruCCC (249) -> менее красный, ruD (253) -> самый красный (первый цвет)
            double groupRatio = (ratingCode - 249.0) / (253.0 - 249.0);
            int redEnd = (int) (totalColors * 0.33); // Конец красной зоны (33%)
            int colorIndex = (int) (redEnd - 1 - groupRatio * (redEnd - 1));
            return colors.get(Math.max(0, Math.min(redEnd - 1, colorIndex)));
            
        } else if (ratingCode < 48) {
            // Лучше ruAAA - самый зелёный
            return colors.get(totalColors - 1);
        } else if (ratingCode > 253) {
            // Хуже ruD - самый красный
            return colors.get(0);
        } else {
            // Попадает в дыры шкалы - определяем ближайшую группу
            if (ratingCode < 146) {
                // Между A и BBB группами - переходный зелёно-жёлтый (67% от массива)
                return colors.get((int) (totalColors * 0.67));
            } else if (ratingCode < 249) {
                // Между BBB и CCC группами - переходный оранжевый (33% от массива)
                return colors.get((int) (totalColors * 0.33));
            } else {
                // Не должно сюда попасть, но на всякий случай
                return colors.get(0);
            }
        }
    }

    /**
     * Загружает маппинг рейтингов к цветовым шарам из файла rating-ball.md
     */
    private void loadRatingBalls() {
        try {
            ClassPathResource resource = new ClassPathResource("rating-ball.md");

            if (!resource.exists()) {
                log.warn("Файл rating-ball.md не найден в resources, цветовые шары для рейтингов не будут отображаться");
                return;
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

                String line;
                int lineNumber = 0;

                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    line = line.trim();

                    if (line.isEmpty() || line.startsWith("#")) {
                        continue; // Пропускаем пустые строки и комментарии
                    }

                    // Парсим строку формата: "ruAAA 🟢🟢🟢"
                    String[] parts = line.split("\\s+", 2);
                    if (parts.length == 2) {
                        String rating = parts[0].trim();
                        String balls = parts[1].trim();

                        ratingBalls.put(rating, balls);
                        log.debug("Загружен маппинг рейтинга: {} -> {}", rating, balls);
                    } else {
                        log.warn("Неверный формат строки {} в файле rating-ball.md: {}", lineNumber, line);
                    }
                }

                log.info("Загружено {} маппингов рейтингов из файла rating-ball.md", ratingBalls.size());

            }
        } catch (IOException e) {
            log.error("Ошибка при загрузке файла rating-ball.md", e);
        }
    }

    /**
     * Получает цветовые шары для рейтинга
     *
     * @param rating рейтинг облигации (например: "ruAAA", "ruBBB+")
     * @return цветовые шары или пустую строку, если маппинг не найден
     */
    public String getRatingBalls(String rating) {
        if (rating == null || rating.trim().isEmpty()) {
            return "";
        }

        String normalizedRating = rating.trim();
        String balls = ratingBalls.get(normalizedRating);

        if (balls == null) {
            // Попробуем найти без учёта регистра
            for (Map.Entry<String, String> entry : ratingBalls.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(normalizedRating)) {
                    balls = entry.getValue();
                    break;
                }
            }
        }

        return balls != null ? balls : "";
    }

    /**
     * Форматирует рейтинг с цветовыми шарами для отображения
     *
     * @param rating рейтинг облигации
     * @return отформатированная строка с рейтингом и шарами
     */
    public String formatRatingWithBalls(String rating) {
        if (rating == null || rating.trim().isEmpty()) {
            return "";
        }

        String balls = getRatingBalls(rating);
        if (balls.isEmpty()) {
            return rating; // Возвращаем только рейтинг, если шары не найдены
        }

        return rating + " " + balls;
    }

}