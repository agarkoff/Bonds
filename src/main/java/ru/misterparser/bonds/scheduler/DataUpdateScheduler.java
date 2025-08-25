package ru.misterparser.bonds.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class DataUpdateScheduler {

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
            log.info("Starting scheduled MOEX data update");
            try {
                moexService.parseBonds();
                log.info("Scheduled MOEX data update completed");
            } catch (Exception e) {
                log.error("Error during scheduled MOEX data update", e);
            }
        }
    }

    @Scheduled(cron = "#{@tBankConfig.instruments.cron}")
    public void updateTBankInstruments() {
        if (tBankConfig.isEnabled()) {
            log.info("Starting scheduled T-Bank instruments update");
            try {
                tBankInstrumentsService.updateBondsData();
                log.info("Scheduled T-Bank instruments update completed");
            } catch (Exception e) {
                log.error("Error during scheduled T-Bank instruments update", e);
            }
        }
    }

    @Scheduled(cron = "#{@tBankConfig.marketdata.cron}")
    public void updateTBankMarketData() {
        if (tBankConfig.isEnabled()) {
            log.info("Starting scheduled T-Bank market data update");
            try {
                tBankMarketDataService.updatePrices();
                log.info("Scheduled T-Bank market data update completed");
            } catch (Exception e) {
                log.error("Error during scheduled T-Bank market data update", e);
            }
        }
    }

    @Scheduled(cron = "#{@raExpertConfig.cron}")
    public void updateRatings() {
        if (raExpertConfig.isEnabled()) {
            log.info("Starting scheduled RaExpert ratings update");
            try {
                raExpertService.updateRatings();
                log.info("Scheduled RaExpert ratings update completed");
            } catch (Exception e) {
                log.error("Error during scheduled RaExpert ratings update", e);
            }
        }
    }

    @Scheduled(cron = "#{@dohodConfig.cron}")
    public void updateDohodRatings() {
        if (dohodConfig.isEnabled()) {
            log.info("Starting scheduled Dohod ratings update");
            try {
                dohodService.updateRatings();
                log.info("Scheduled Dohod ratings update completed");
            } catch (Exception e) {
                log.error("Error during scheduled Dohod ratings update", e);
            }
        }
    }

    @Scheduled(fixedDelayString = "#{@calcConfig.periodMinutes * 60000}")
    public void calculateBonds() {
        log.info("Starting scheduled bonds calculation");
        try {
            calculationService.calculateAllBonds();
            log.info("Scheduled bonds calculation completed");
        } catch (Exception e) {
            log.error("Error during scheduled bonds calculation", e);
        }
    }
}