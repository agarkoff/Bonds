package ru.misterparser.bonds.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.misterparser.bonds.model.Bond;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class BondRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Bond> bondRowMapper = new RowMapper<Bond>() {
        @Override
        public Bond mapRow(ResultSet rs, int rowNum) throws SQLException {
            Bond bond = new Bond();
            bond.setId(rs.getLong("id"));
            bond.setIsin(rs.getString("isin"));
            bond.setTicker(rs.getString("ticker"));
            bond.setShortName(rs.getString("short_name"));
            bond.setCouponValue(rs.getBigDecimal("coupon_value"));
            bond.setMaturityDate(rs.getDate("maturity_date") != null ? rs.getDate("maturity_date").toLocalDate() : null);
            bond.setFaceValue(rs.getBigDecimal("face_value"));
            bond.setCouponFrequency(rs.getInt("coupon_frequency"));
            bond.setCouponLength(rs.getInt("coupon_length"));
            bond.setCouponDaysPassed(rs.getInt("coupon_days_passed"));
            bond.setOfferDate(rs.getDate("offer_date") != null ? rs.getDate("offer_date").toLocalDate() : null);
            bond.setFigi(rs.getString("figi"));
            bond.setInstrumentUid(rs.getString("instrument_uid"));
            bond.setAssetUid(rs.getString("asset_uid"));
            bond.setBrandName(rs.getString("brand_name"));
            bond.setPrice(rs.getBigDecimal("price"));
            bond.setRatingValue(rs.getString("rating_value"));
            Integer ratingCode = rs.getObject("rating_code", Integer.class);
            bond.setRatingCode(ratingCode);
            bond.setCouponDaily(rs.getBigDecimal("coupon_daily"));
            bond.setNkd(rs.getBigDecimal("nkd"));
            bond.setCosts(rs.getBigDecimal("costs"));
            bond.setCouponRedemption(rs.getBigDecimal("coupon_redemption"));
            bond.setProfit(rs.getBigDecimal("profit"));
            bond.setProfitNet(rs.getBigDecimal("profit_net"));
            bond.setAnnualYield(rs.getBigDecimal("annual_yield"));
            bond.setCouponOffer(rs.getBigDecimal("coupon_offer"));
            bond.setProfitOffer(rs.getBigDecimal("profit_offer"));
            bond.setProfitNetOffer(rs.getBigDecimal("profit_net_offer"));
            bond.setAnnualYieldOffer(rs.getBigDecimal("annual_yield_offer"));
            // Отдельные даты обновления исходных сущностей
            bond.setMoexUpdatedAt(rs.getTimestamp("moex_updated_at") != null ? rs.getTimestamp("moex_updated_at").toLocalDateTime() : null);
            bond.setTbankBondsUpdatedAt(rs.getTimestamp("tbank_bonds_updated_at") != null ? rs.getTimestamp("tbank_bonds_updated_at").toLocalDateTime() : null);
            bond.setTbankPricesUpdatedAt(rs.getTimestamp("tbank_prices_updated_at") != null ? rs.getTimestamp("tbank_prices_updated_at").toLocalDateTime() : null);
            bond.setDohodRatingsUpdatedAt(rs.getTimestamp("dohod_ratings_updated_at") != null ? rs.getTimestamp("dohod_ratings_updated_at").toLocalDateTime() : null);
            bond.setBondsCalcUpdatedAt(rs.getTimestamp("bonds_calc_updated_at") != null ? rs.getTimestamp("bonds_calc_updated_at").toLocalDateTime() : null);
            
            return bond;
        }
    };

    public List<Bond> findAll() {
        return jdbcTemplate.query("SELECT * FROM bonds WHERE annual_yield IS NULL OR annual_yield <= 50 ORDER BY annual_yield DESC", bondRowMapper);
    }

    public List<Bond> findTopByAnnualYield(int limit) {
        return jdbcTemplate.query("SELECT * FROM bonds WHERE annual_yield IS NOT NULL AND annual_yield <= 50 ORDER BY FLOOR(annual_yield) DESC, rating_code ASC, annual_yield DESC LIMIT ?",
                bondRowMapper, limit);
    }



    public List<Bond> findTopByAnnualYieldAndMaturityRange(int minWeeksToMaturity, int maxWeeksToMaturity, boolean useOfferYield, double minYield, double maxYield) {
        if (!useOfferYield) {
            // Обычный режим - сортировка по annual_yield
            String sql = "SELECT * FROM bonds WHERE annual_yield IS NOT NULL AND annual_yield >= " + minYield + " AND annual_yield <= " + maxYield + " " +
                         "AND maturity_date IS NOT NULL " +
                         "AND maturity_date >= (CURRENT_DATE + INTERVAL '" + minWeeksToMaturity + " weeks') " +
                         "AND maturity_date <= (CURRENT_DATE + INTERVAL '" + maxWeeksToMaturity + " weeks') " +
                         "ORDER BY FLOOR(annual_yield) DESC, rating_code ASC, annual_yield DESC";
            return jdbcTemplate.query(sql, bondRowMapper);
        }
        
        // Режим оферты: используем annual_yield_offer если доступно, иначе annual_yield
        String sql = "SELECT * " +
                     "FROM bonds " +
                     "WHERE " +
                     "  (annual_yield IS NOT NULL OR annual_yield_offer IS NOT NULL) " +
                     "  AND maturity_date IS NOT NULL " +
                     "  AND CASE " +
                     "    WHEN offer_date IS NOT NULL AND offer_date > CURRENT_DATE AND annual_yield_offer IS NOT NULL " +
                     "    THEN annual_yield_offer >= " + minYield + " AND annual_yield_offer <= " + maxYield + " " +
                     "    ELSE annual_yield >= " + minYield + " AND annual_yield <= " + maxYield + " " +
                     "  END " +
                     "  AND CASE " +
                     "    WHEN offer_date IS NOT NULL AND offer_date > CURRENT_DATE AND annual_yield_offer IS NOT NULL " +
                     "    THEN offer_date >= (CURRENT_DATE + INTERVAL '" + minWeeksToMaturity + " weeks') " +
                     "         AND offer_date <= (CURRENT_DATE + INTERVAL '" + maxWeeksToMaturity + " weeks') " +
                     "    ELSE maturity_date >= (CURRENT_DATE + INTERVAL '" + minWeeksToMaturity + " weeks') " +
                     "         AND maturity_date <= (CURRENT_DATE + INTERVAL '" + maxWeeksToMaturity + " weeks') " +
                     "  END " +
                     "ORDER BY " +
                     "  FLOOR(CASE " +
                     "    WHEN offer_date IS NOT NULL AND offer_date > CURRENT_DATE AND annual_yield_offer IS NOT NULL " +
                     "    THEN annual_yield_offer " +
                     "    ELSE annual_yield " +
                     "  END) DESC, " +
                     "  rating_code ASC, " +
                     "  CASE " +
                     "    WHEN offer_date IS NOT NULL AND offer_date > CURRENT_DATE AND annual_yield_offer IS NOT NULL " +
                     "    THEN annual_yield_offer " +
                     "    ELSE annual_yield " +
                     "  END DESC";
        
        return jdbcTemplate.query(sql, bondRowMapper);
    }

    public Optional<Bond> findByIsin(String isin) {
        List<Bond> bonds = jdbcTemplate.query("SELECT * FROM bonds WHERE isin = ?", bondRowMapper, isin);
        return bonds.isEmpty() ? Optional.empty() : Optional.of(bonds.get(0));
    }

    public List<Bond> findAllWithFigi() {
        return jdbcTemplate.query("SELECT * FROM bonds WHERE figi IS NOT NULL", bondRowMapper);
    }

    // Методы записи удалены - используется представление только для чтения
    // Для записи данных используются соответствующие репозитории:
    // - MoexBondRepository для данных MOEX
    // - TBankBondRepository для данных T-Bank
    // - TBankPriceRepository для цен
    // - DohodRatingRepository для рейтингов
    // - BondCalculationRepository для расчетных данных (таблица bonds_calc)

    public long count() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM bonds", Long.class);
    }

    public List<Bond> findBondsWithOffersInDays(int days) {
        String sql = "SELECT * FROM bonds " +
                    "WHERE offer_date IS NOT NULL " +
                    "AND offer_date > CURRENT_DATE " +
                    "AND offer_date <= CURRENT_DATE + INTERVAL '" + days + " days' " +
                    "ORDER BY offer_date ASC";
        return jdbcTemplate.query(sql, bondRowMapper);
    }
}