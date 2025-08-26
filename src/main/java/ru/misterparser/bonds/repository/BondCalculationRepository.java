package ru.misterparser.bonds.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.misterparser.bonds.model.Bond;

@Repository
@RequiredArgsConstructor
public class BondCalculationRepository {

    private final JdbcTemplate jdbcTemplate;

    public void saveOrUpdateCalculationData(Bond bond) {
        String sql = "INSERT INTO bonds_calc (isin, coupon_daily, nkd, costs, " +
                "coupon_redemption, profit, profit_net, annual_yield, " +
                "coupon_offer, profit_offer, profit_net_offer, annual_yield_offer) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT (isin) DO UPDATE SET " +
                "coupon_daily = EXCLUDED.coupon_daily, " +
                "nkd = EXCLUDED.nkd, " +
                "costs = EXCLUDED.costs, " +
                "coupon_redemption = EXCLUDED.coupon_redemption, " +
                "profit = EXCLUDED.profit, " +
                "profit_net = EXCLUDED.profit_net, " +
                "annual_yield = EXCLUDED.annual_yield, " +
                "coupon_offer = EXCLUDED.coupon_offer, " +
                "profit_offer = EXCLUDED.profit_offer, " +
                "profit_net_offer = EXCLUDED.profit_net_offer, " +
                "annual_yield_offer = EXCLUDED.annual_yield_offer, " +
                "updated_at = CURRENT_TIMESTAMP";
        
        jdbcTemplate.update(sql,
                bond.getIsin(),
                bond.getCouponDaily(),
                bond.getNkd(),
                bond.getCosts(),
                bond.getCouponRedemption(),
                bond.getProfit(),
                bond.getProfitNet(),
                bond.getAnnualYield(),
                bond.getCouponOffer(),
                bond.getProfitOffer(),
                bond.getProfitNetOffer(),
                bond.getAnnualYieldOffer()
        );
    }
}