package ru.misterparser.bonds.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.misterparser.bonds.dto.BondData;
import ru.misterparser.bonds.repository.BondRepository;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BondProcessingFacadeService {
    
    private static final Logger logger = LoggerFactory.getLogger(BondProcessingFacadeService.class);
    
    private final BondCsvParserService csvParserService;
    private final BondCalculationService calculationService;
    private final BondRepository bondRepository;
    
    @Scheduled(cron = "0 0 2 * * *")
    public void processAllBonds() {
        parseAndCalculateBonds();
    }
    
    public int parseAndCalculateBonds() {
        logger.info("Starting bond processing: parsing CSV and calculating derived metrics");
        
        // Шаг 1: Парсинг CSV данных
        List<BondData> bondDataList = csvParserService.parseBondsFromCsv();
        if (bondDataList.isEmpty()) {
            logger.warn("No bonds found in CSV data");
            return 0;
        }
        
        logger.info("Parsed {} bonds from CSV, starting calculations", bondDataList.size());
        
        // Шаг 2: Расчет производных показателей и сохранение
        int processedCount = 0;
        for (BondData bondData : bondDataList) {
            try {
                // Расчет НКД
                BigDecimal nkd = calculationService.calculateNkd(
                    bondData.getCouponDaysPassed(), 
                    bondData.getCouponValue(), 
                    bondData.getCouponLength()
                );
                
                // Расчет комиссии
                BigDecimal fee = calculationService.calculateFee(
                    bondData.getWaPrice(), 
                    bondData.getFaceValue()
                );
                
                // Расчет прибыли
                BigDecimal profit = calculationService.calculateProfit(
                    bondData.getFaceValue(), 
                    bondData.getCouponValue(), 
                    bondData.getWaPrice(), 
                    nkd, 
                    fee, 
                    bondData.getMaturityDate(), 
                    bondData.getCouponLength()
                );
                
                // Расчет чистой прибыли
                BigDecimal netProfit = calculationService.calculateNetProfit(profit);
                
                // Расчет затрат для годовой доходности
                BigDecimal costs = calculationService.calculateCosts(
                    bondData.getWaPrice(), 
                    bondData.getFaceValue(), 
                    nkd, 
                    fee
                );
                
                // Расчет годовой доходности
                BigDecimal annualYield = calculationService.calculateAnnualYield(
                    netProfit, 
                    bondData.getMaturityDate(), 
                    costs
                );
                
                // Сохранение в базу данных
                bondRepository.upsertBond(
                    bondData.getTicker(),
                    bondData.getShortName(),
                    bondData.getCouponValue(),
                    bondData.getMaturityDate(),
                    bondData.getWaPrice(),
                    bondData.getFaceValue(),
                    bondData.getCouponFrequency(),
                    bondData.getCouponLength(),
                    nkd,
                    fee,
                    profit,
                    netProfit,
                    annualYield
                );
                
                processedCount++;
                
                if (processedCount % 100 == 0) {
                    logger.info("Processed and saved {} bonds", processedCount);
                }
                
            } catch (Exception e) {
                logger.warn("Error processing bond {}: {}", bondData.getTicker(), e.getMessage());
            }
        }
        
        logger.info("Bond processing completed. Successfully processed {} out of {} bonds", 
            processedCount, bondDataList.size());
        
        return processedCount;
    }
}