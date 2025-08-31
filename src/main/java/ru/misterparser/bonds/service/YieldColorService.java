package ru.misterparser.bonds.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class YieldColorService {

    /**
     * Возвращает цвет фона для ячейки доходности в зависимости от значения
     * Логика аналогична градиенту в основном рейтинге:
     * - На уровне 18% - зеленый цвет
     * - Выше 18% - постепенный переход: желтый → красный → бордовый
     * - При двукратном превышении (36%+) - бордовый цвет
     */
    public String getYieldColor(BigDecimal yield) {
        if (yield == null) {
            return "#6c757d"; // Серый для null значений
        }

        double yieldValue = yield.doubleValue();
        
        // Ключевая ставка
        double keyRate = 18.0;
        double maxYield = 36.0; // Двукратное превышение ключевой ставки

        if (yieldValue <= keyRate) {
            // До 18% - зеленый с градацией
            double intensity = Math.min(1.0, yieldValue / keyRate);
            int greenValue = (int)(40 + intensity * 127); // От темно-зеленого к зеленому
            return String.format("#%02x%02x%02x", Math.max(0, 167 - (int)(intensity * 100)), greenValue, Math.max(0, 69 - (int)(intensity * 30)));
        } else if (yieldValue <= keyRate * 1.5) {
            // 18-27% - переход от зеленого к желтому
            double ratio = (yieldValue - keyRate) / (keyRate * 0.5);
            int red = (int)(40 + ratio * 215); // Увеличиваем красный
            int green = (int)(167 - ratio * 50); // Уменьшаем зеленый
            return String.format("#%02x%02x%02x", red, green, 39);
        } else if (yieldValue <= maxYield) {
            // 27-36% - переход от желтого к красному
            double ratio = (yieldValue - keyRate * 1.5) / (keyRate * 0.5);
            int red = Math.min(255, (int)(255 - ratio * 35));
            int green = Math.max(0, (int)(117 - ratio * 117));
            return String.format("#%02x%02x%02x", red, green, 39);
        } else {
            // Свыше 36% - бордовый
            return "#8b0000"; // Темно-красный/бордовый
        }
    }

    /**
     * Возвращает текстовый цвет (белый или черный) в зависимости от яркости фона
     */
    public String getYieldTextColor(BigDecimal yield) {
        if (yield == null || yield.doubleValue() <= 10) {
            return "#000000"; // Черный для светлых фонов
        }
        return "#ffffff"; // Белый для темных фонов
    }
}