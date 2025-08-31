package ru.misterparser.bonds.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.misterparser.bonds.model.Bond;
import ru.misterparser.bonds.model.TelegramUser;
import ru.misterparser.bonds.model.UserOrder;
import ru.misterparser.bonds.repository.BondRepository;
import ru.misterparser.bonds.repository.UserOrderRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserOrderService {

    private final UserOrderRepository userOrderRepository;
    private final BondRepository bondRepository;
    private final TelegramAuthService telegramAuthService;
    private final CalculationService calculationService;

    /**
     * Создает новую сделку с расчетом всех параметров
     */
    public UserOrder createOrder(UserOrder order, Authentication authentication) {
        TelegramUser currentUser = telegramAuthService.getCurrentUser(authentication);
        if (currentUser == null) {
            throw new IllegalStateException("Пользователь не авторизован");
        }

        order.setTelegramUserId(currentUser.getId());
        
        // Получаем данные облигации из БД
        Optional<Bond> bondOpt = bondRepository.findByIsin(order.getIsin());
        if (bondOpt.isEmpty()) {
            throw new IllegalArgumentException("Облигация с ISIN " + order.getIsin() + " не найдена");
        }
        
        Bond bond = bondOpt.get();
        
        // Заполняем данные из БД
        fillOrderFromBond(order, bond);
        
        // Рассчитываем НКД на дату покупки
        calculateNkd(order, bond);
        
        // Рассчитываем все финансовые параметры
        calculateOrderFinancials(order);
        
        return userOrderRepository.save(order);
    }

    /**
     * Обновляет существующую сделку
     */
    public UserOrder updateOrder(Long orderId, UserOrder updateData, Authentication authentication) {
        TelegramUser currentUser = telegramAuthService.getCurrentUser(authentication);
        if (currentUser == null) {
            throw new IllegalStateException("Пользователь не авторизован");
        }

        Optional<UserOrder> existingOrderOpt = userOrderRepository.findById(orderId);
        if (existingOrderOpt.isEmpty()) {
            throw new IllegalArgumentException("Сделка не найдена");
        }

        UserOrder existingOrder = existingOrderOpt.get();
        if (!existingOrder.getTelegramUserId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Доступ запрещен");
        }

        // Обновляем только переданные поля (не null)
        if (updateData.getPurchaseDate() != null) {
            existingOrder.setPurchaseDate(updateData.getPurchaseDate());
        }
        if (updateData.getPrice() != null) {
            existingOrder.setPrice(updateData.getPrice());
        }
        if (updateData.getFeePercent() != null) {
            existingOrder.setFeePercent(updateData.getFeePercent());
        }
        if (updateData.getMaturityDate() != null) {
            existingOrder.setMaturityDate(updateData.getMaturityDate());
        }

        // Пересчитываем НКД если изменилась дата
        Optional<Bond> bondOpt = bondRepository.findByIsin(existingOrder.getIsin());
        if (bondOpt.isPresent()) {
            calculateNkd(existingOrder, bondOpt.get());
        }

        // Пересчитываем финансовые параметры
        calculateOrderFinancials(existingOrder);

        return userOrderRepository.save(existingOrder);
    }

    /**
     * Получает все сделки пользователя
     */
    public List<UserOrder> getUserOrders(Authentication authentication) {
        TelegramUser currentUser = telegramAuthService.getCurrentUser(authentication);
        if (currentUser == null) {
            throw new IllegalStateException("Пользователь не авторизован");
        }

        return userOrderRepository.findByTelegramUserId(currentUser.getId());
    }

    /**
     * Удаляет сделку
     */
    public boolean deleteOrder(Long orderId, Authentication authentication) {
        TelegramUser currentUser = telegramAuthService.getCurrentUser(authentication);
        if (currentUser == null) {
            throw new IllegalStateException("Пользователь не авторизован");
        }

        Optional<UserOrder> orderOpt = userOrderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return false;
        }

        UserOrder order = orderOpt.get();
        if (!order.getTelegramUserId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Доступ запрещен");
        }

        return userOrderRepository.deleteById(orderId);
    }

    /**
     * Получает список доступных облигаций для автодополнения
     */
    public List<Bond> getAvailableBonds() {
        return bondRepository.findAllBondsForFiltering();
    }

    /**
     * Заполняет данные сделки из облигации
     */
    private void fillOrderFromBond(UserOrder order, Bond bond) {
        order.setTicker(bond.getTicker());
        order.setBondName(bond.getBrandName() != null ? bond.getBrandName() : bond.getShortName());
        order.setRating(bond.getRatingValue());
        order.setCouponValue(bond.getCouponValue());
        order.setCouponPeriod(bond.getCouponLength());
        order.setFaceValue(bond.getFaceValue());
        
        // Устанавливаем дату погашения или оферты
        if (order.getMaturityDate() == null) {
            if (bond.getOfferDate() != null && bond.getOfferDate().isAfter(LocalDate.now())) {
                order.setMaturityDate(bond.getOfferDate());
            } else {
                order.setMaturityDate(bond.getMaturityDate());
            }
        }
        
        // Устанавливаем текущую цену если не указана
        if (order.getPrice() == null && bond.getPrice() != null) {
            order.setPrice(bond.getPrice());
        }
    }

    /**
     * Рассчитывает НКД на дату покупки
     */
    private void calculateNkd(UserOrder order, Bond bond) {
        if (order.getPurchaseDate() != null && !order.getPurchaseDate().equals(LocalDate.now())) {
            // Если дата покупки не текущая, пересчитываем НКД
            // Упрощенный расчет - используем пропорцию от текущего НКД
            if (bond.getNkd() != null && bond.getCouponDaily() != null) {
                // Примерный расчет НКД на дату покупки
                long daysSincePurchase = ChronoUnit.DAYS.between(order.getPurchaseDate(), LocalDate.now());
                BigDecimal nkdAdjustment = bond.getCouponDaily().multiply(BigDecimal.valueOf(daysSincePurchase));
                BigDecimal calculatedNkd = bond.getNkd().subtract(nkdAdjustment);
                order.setNkd(calculatedNkd.max(BigDecimal.ZERO));
            } else {
                order.setNkd(bond.getNkd());
            }
        } else {
            order.setNkd(bond.getNkd());
        }
    }

    /**
     * Рассчитывает все финансовые параметры сделки
     */
    private void calculateOrderFinancials(UserOrder order) {
        if (order.getPrice() == null || order.getFaceValue() == null) {
            return;
        }

        // Рассчитываем комиссию
        BigDecimal feeAmount = BigDecimal.ZERO;
        if (order.getFeePercent() != null) {
            feeAmount = order.getPrice().multiply(order.getFeePercent()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }

        // Общие затраты = Цена + НКД + Комиссия
        BigDecimal nkd = order.getNkd() != null ? order.getNkd() : BigDecimal.ZERO;
        order.setTotalCosts(order.getPrice().add(nkd).add(feeAmount));

        // Рассчитываем общий купонный доход до погашения
        calculateTotalCoupon(order);

        // Общий доход = Номинал + Купоны
        BigDecimal totalCoupon = order.getTotalCoupon() != null ? order.getTotalCoupon() : BigDecimal.ZERO;
        order.setTotalIncome(order.getFaceValue().add(totalCoupon));

        // Чистая прибыль = Доход - Затраты
        order.setNetProfit(order.getTotalIncome().subtract(order.getTotalCosts()));

        // Годовая доходность
        calculateAnnualYield(order);
    }

    /**
     * Рассчитывает общий купонный доход до погашения
     */
    private void calculateTotalCoupon(UserOrder order) {
        if (order.getCouponValue() == null || order.getCouponPeriod() == null || 
            order.getPurchaseDate() == null || order.getMaturityDate() == null) {
            order.setTotalCoupon(BigDecimal.ZERO);
            return;
        }

        // Количество дней до погашения
        long daysToMaturity = ChronoUnit.DAYS.between(order.getPurchaseDate(), order.getMaturityDate());
        if (daysToMaturity <= 0) {
            order.setTotalCoupon(BigDecimal.ZERO);
            return;
        }

        // Количество купонных периодов
        double periodsToMaturity = (double) daysToMaturity / order.getCouponPeriod();
        BigDecimal totalCouponPayments = order.getCouponValue().multiply(BigDecimal.valueOf(periodsToMaturity));
        
        order.setTotalCoupon(totalCouponPayments.setScale(2, RoundingMode.HALF_UP));
    }

    /**
     * Рассчитывает годовую доходность
     */
    private void calculateAnnualYield(UserOrder order) {
        if (order.getNetProfit() == null || order.getTotalCosts() == null || 
            order.getPurchaseDate() == null || order.getMaturityDate() == null ||
            order.getTotalCosts().equals(BigDecimal.ZERO)) {
            order.setAnnualYield(BigDecimal.ZERO);
            return;
        }

        long daysToMaturity = ChronoUnit.DAYS.between(order.getPurchaseDate(), order.getMaturityDate());
        if (daysToMaturity <= 0) {
            order.setAnnualYield(BigDecimal.ZERO);
            return;
        }

        // Доходность = (Чистая прибыль / Затраты) * (365 / дни до погашения) * 100
        BigDecimal profitability = order.getNetProfit().divide(order.getTotalCosts(), 6, RoundingMode.HALF_UP);
        BigDecimal annualizationFactor = BigDecimal.valueOf(365.0).divide(BigDecimal.valueOf(daysToMaturity), 6, RoundingMode.HALF_UP);
        BigDecimal annualYield = profitability.multiply(annualizationFactor).multiply(BigDecimal.valueOf(100));
        
        order.setAnnualYield(annualYield.setScale(2, RoundingMode.HALF_UP));
    }
}