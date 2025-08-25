package ru.misterparser.bonds.scheduler;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.misterparser.bonds.config.CalcConfig;
import ru.misterparser.bonds.config.DohodConfig;
import ru.misterparser.bonds.config.MoexConfig;
import ru.misterparser.bonds.config.RaExpertConfig;
import ru.misterparser.bonds.config.TBankConfig;
import ru.misterparser.bonds.service.*;

@Component
@RequiredArgsConstructor
public class DataUpdateScheduler {

    private static final Logger logger = LoggerFactory.getLogger(DataUpdateScheduler.class);

    private final MoexService moexService;
    private final TBankInstrumentsService tBankInstrumentsService;
    private final TBankMarketDataService tBankMarketDataService;
    private final RaExpertService raExpertService;
    private final DohodService dohodService;
    private final CalculationService calculationService;
    private final MoexConfig moexConfig;
    private final TBankConfig tBankConfig;
    private final RaExpertConfig raExpertConfig;
    private final DohodConfig dohodConfig;
    private final CalcConfig calcConfig;

    @Scheduled(cron = "#{@moexConfig.cron}")
    public void updateMoexData() {
        if (moexConfig.isEnabled()) {
            logger.info("Starting scheduled MOEX data update");
            try {
                moexService.parseBonds();
                logger.info("Scheduled MOEX data update completed");
            } catch (Exception e) {
                logger.error("Error during scheduled MOEX data update", e);
            }
        }
    }

    @Scheduled(cron = "#{@tBankConfig.instruments.cron}")
    public void updateTBankInstruments() {
        if (tBankConfig.isEnabled()) {
            logger.info("Starting scheduled T-Bank instruments update");
            try {
                tBankInstrumentsService.updateBondsData();
                logger.info("Scheduled T-Bank instruments update completed");
            } catch (Exception e) {
                logger.error("Error during scheduled T-Bank instruments update", e);
            }
        }
    }

    @Scheduled(cron = "#{@tBankConfig.marketdata.cron}")
    public void updateTBankMarketData() {
        if (tBankConfig.isEnabled()) {
            logger.info("Starting scheduled T-Bank market data update");
            try {
                tBankMarketDataService.updatePrices();
                logger.info("Scheduled T-Bank market data update completed");
            } catch (Exception e) {
                logger.error("Error during scheduled T-Bank market data update", e);
            }
        }
    }

    @Scheduled(cron = "#{@raExpertConfig.cron}")
    public void updateRatings() {
        if (raExpertConfig.isEnabled()) {
            logger.info("Starting scheduled RaExpert ratings update");
            try {
                raExpertService.updateRatings();
                logger.info("Scheduled RaExpert ratings update completed");
            } catch (Exception e) {
                logger.error("Error during scheduled RaExpert ratings update", e);
            }
        }
    }

    @Scheduled(cron = "#{@dohodConfig.cron}")
    public void updateDohodRatings() {
        if (dohodConfig.isEnabled()) {
            logger.info("Starting scheduled Dohod ratings update");
            try {
                dohodService.updateRatings();
                logger.info("Scheduled Dohod ratings update completed");
            } catch (Exception e) {
                logger.error("Error during scheduled Dohod ratings update", e);
            }
        }
    }

    @Scheduled(fixedDelayString = "#{@calcConfig.periodMinutes * 60000}")
    public void calculateBonds() {
        logger.info("Starting scheduled bonds calculation");
        try {
            calculationService.calculateAllBonds();
            logger.info("Scheduled bonds calculation completed");
        } catch (Exception e) {
            logger.error("Error during scheduled bonds calculation", e);
        }
    }
}