package ru.misterparser.bonds.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.misterparser.bonds.config.CalcConfig;
import ru.misterparser.bonds.config.MoexConfig;
import ru.misterparser.bonds.config.RaExpertConfig;
import ru.misterparser.bonds.config.TBankConfig;
import ru.misterparser.bonds.service.*;

@Component
public class DataUpdateScheduler {

    private static final Logger logger = LoggerFactory.getLogger(DataUpdateScheduler.class);

    @Autowired
    private MoexService moexService;

    @Autowired
    private TBankInstrumentsService tBankInstrumentsService;

    @Autowired
    private TBankMarketDataService tBankMarketDataService;

    @Autowired
    private RaExpertService raExpertService;

    @Autowired
    private CalculationService calculationService;

    @Autowired
    private MoexConfig moexConfig;

    @Autowired
    private TBankConfig tBankConfig;

    @Autowired
    private RaExpertConfig raExpertConfig;

    @Autowired
    private CalcConfig calcConfig;

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
            logger.info("Starting scheduled ratings update");
            try {
                raExpertService.updateRatings();
                logger.info("Scheduled ratings update completed");
            } catch (Exception e) {
                logger.error("Error during scheduled ratings update", e);
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