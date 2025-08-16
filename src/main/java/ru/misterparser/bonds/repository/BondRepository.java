package ru.misterparser.bonds.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.misterparser.bonds.model.Bond;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class BondRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
            bond.setRatingCode(rs.getInt("rating_code"));
            bond.setCouponDaily(rs.getBigDecimal("coupon_daily"));
            bond.setNkd(rs.getBigDecimal("nkd"));
            bond.setCosts(rs.getBigDecimal("costs"));
            bond.setFee(rs.getBigDecimal("fee"));
            bond.setCouponRedemption(rs.getBigDecimal("coupon_redemption"));
            bond.setProfit(rs.getBigDecimal("profit"));
            bond.setProfitNet(rs.getBigDecimal("profit_net"));
            bond.setAnnualYield(rs.getBigDecimal("annual_yield"));
            bond.setCouponOffer(rs.getBigDecimal("coupon_offer"));
            bond.setProfitOffer(rs.getBigDecimal("profit_offer"));
            bond.setProfitNetOffer(rs.getBigDecimal("profit_net_offer"));
            bond.setAnnualYieldOffer(rs.getBigDecimal("annual_yield_offer"));
            bond.setCreatedAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null);
            bond.setUpdatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null);
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

    public List<Bond> findTopByAnnualYieldAndMaturity(int limit, int weeksToMaturity) {
        String sql = "SELECT * FROM bonds WHERE annual_yield IS NOT NULL AND annual_yield <= 50 " +
                     "AND maturity_date IS NOT NULL AND maturity_date <= (CURRENT_DATE + INTERVAL '" + weeksToMaturity + " weeks') " +
                     "ORDER BY FLOOR(annual_yield) DESC, rating_code ASC, annual_yield DESC LIMIT ?";
        return jdbcTemplate.query(sql, bondRowMapper, limit);
    }

    public List<Bond> findTopByAnnualYieldAndMaturityRange(int limit, int minWeeksToMaturity, int maxWeeksToMaturity) {
        return findTopByAnnualYieldAndMaturityRange(limit, minWeeksToMaturity, maxWeeksToMaturity, false);
    }


    public List<Bond> findTopByAnnualYieldAndMaturityRange(int limit, int minWeeksToMaturity, int maxWeeksToMaturity, boolean useOfferYield) {
        if (!useOfferYield) {
            // Обычный режим - сортировка по annual_yield
            String sql = "SELECT * FROM bonds WHERE annual_yield IS NOT NULL AND annual_yield <= 50 " +
                         "AND maturity_date IS NOT NULL " +
                         "AND maturity_date >= (CURRENT_DATE + INTERVAL '" + minWeeksToMaturity + " weeks') " +
                         "AND maturity_date <= (CURRENT_DATE + INTERVAL '" + maxWeeksToMaturity + " weeks') " +
                         "ORDER BY FLOOR(annual_yield) DESC, rating_code ASC, annual_yield DESC LIMIT ?";
            return jdbcTemplate.query(sql, bondRowMapper, limit);
        }
        
        // Режим оферты: используем annual_yield_offer если доступно, иначе annual_yield
        String sql = "SELECT * " +
                     "FROM bonds " +
                     "WHERE " +
                     "  (annual_yield IS NOT NULL OR annual_yield_offer IS NOT NULL) " +
                     "  AND maturity_date IS NOT NULL " +
                     "  AND CASE " +
                     "    WHEN offer_date IS NOT NULL AND offer_date > CURRENT_DATE AND annual_yield_offer IS NOT NULL " +
                     "    THEN annual_yield_offer <= 50 " +
                     "    ELSE annual_yield <= 50 " +
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
                     "  END DESC " +
                     "LIMIT ?";
        
        return jdbcTemplate.query(sql, bondRowMapper, limit);
    }

    public Optional<Bond> findByIsin(String isin) {
        List<Bond> bonds = jdbcTemplate.query("SELECT * FROM bonds WHERE isin = ?", bondRowMapper, isin);
        return bonds.isEmpty() ? Optional.empty() : Optional.of(bonds.get(0));
    }

    public List<Bond> findAllWithFigi() {
        return jdbcTemplate.query("SELECT * FROM bonds WHERE figi IS NOT NULL", bondRowMapper);
    }

    public void save(Bond bond) {
        if (bond.getId() == null) {
            String sql = "INSERT INTO bonds (isin, ticker, short_name, coupon_value, maturity_date, face_value, " +
                    "coupon_frequency, coupon_length, coupon_days_passed, offer_date, figi, instrument_uid, asset_uid, brand_name, " +
                    "price, rating_value, rating_code, coupon_daily, nkd, costs, fee, coupon_redemption, " +
                    "profit, profit_net, annual_yield, coupon_offer, profit_offer, profit_net_offer, annual_yield_offer) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
            jdbcTemplate.update(sql,
                    bond.getIsin(),
                    bond.getTicker(),
                    bond.getShortName(),
                    bond.getCouponValue(),
                    bond.getMaturityDate(),
                    bond.getFaceValue(),
                    bond.getCouponFrequency(),
                    bond.getCouponLength(),
                    bond.getCouponDaysPassed(),
                    bond.getOfferDate(),
                    bond.getFigi(),
                    bond.getInstrumentUid(),
                    bond.getAssetUid(),
                    bond.getBrandName(),
                    bond.getPrice(),
                    bond.getRatingValue(),
                    bond.getRatingCode(),
                    bond.getCouponDaily(),
                    bond.getNkd(),
                    bond.getCosts(),
                    bond.getFee(),
                    bond.getCouponRedemption(),
                    bond.getProfit(),
                    bond.getProfitNet(),
                    bond.getAnnualYield(),
                    bond.getCouponOffer(),
                    bond.getProfitOffer(),
                    bond.getProfitNetOffer(),
                    bond.getAnnualYieldOffer()
            );
        } else {
            String sql = "UPDATE bonds SET ticker = ?, short_name = ?, coupon_value = ?, maturity_date = ?, face_value = ?, " +
                    "coupon_frequency = ?, coupon_length = ?, coupon_days_passed = ?, offer_date = ?, figi = ?, instrument_uid = ?, " +
                    "asset_uid = ?, brand_name = ?, price = ?, rating_value = ?, rating_code = ?, coupon_daily = ?, " +
                    "nkd = ?, costs = ?, fee = ?, coupon_redemption = ?, profit = ?, profit_net = ?, " +
                    "annual_yield = ?, coupon_offer = ?, profit_offer = ?, profit_net_offer = ?, annual_yield_offer = ?, " +
                    "updated_at = CURRENT_TIMESTAMP WHERE id = ?";
            
            jdbcTemplate.update(sql,
                    bond.getTicker(),
                    bond.getShortName(),
                    bond.getCouponValue(),
                    bond.getMaturityDate(),
                    bond.getFaceValue(),
                    bond.getCouponFrequency(),
                    bond.getCouponLength(),
                    bond.getCouponDaysPassed(),
                    bond.getOfferDate(),
                    bond.getFigi(),
                    bond.getInstrumentUid(),
                    bond.getAssetUid(),
                    bond.getBrandName(),
                    bond.getPrice(),
                    bond.getRatingValue(),
                    bond.getRatingCode(),
                    bond.getCouponDaily(),
                    bond.getNkd(),
                    bond.getCosts(),
                    bond.getFee(),
                    bond.getCouponRedemption(),
                    bond.getProfit(),
                    bond.getProfitNet(),
                    bond.getAnnualYield(),
                    bond.getCouponOffer(),
                    bond.getProfitOffer(),
                    bond.getProfitNetOffer(),
                    bond.getAnnualYieldOffer(),
                    bond.getId()
            );
        }
    }

    public void saveOrUpdate(Bond bond) {
        Optional<Bond> existing = findByIsin(bond.getIsin());
        if (existing.isPresent()) {
            bond.setId(existing.get().getId());
        }
        save(bond);
    }

    public void saveOrUpdateMoexData(Bond bond) {
        Optional<Bond> existing = findByIsin(bond.getIsin());
        if (existing.isPresent()) {
            updateMoexFields(bond);
        } else {
            save(bond);
        }
    }

    private void updateMoexFields(Bond bond) {
        String sql = "UPDATE bonds SET ticker = ?, short_name = ?, coupon_value = ?, maturity_date = ?, " +
                "face_value = ?, coupon_frequency = ?, coupon_length = ?, coupon_days_passed = ?, offer_date = ?, " +
                "updated_at = CURRENT_TIMESTAMP WHERE isin = ?";
        
        jdbcTemplate.update(sql,
                bond.getTicker(),
                bond.getShortName(),
                bond.getCouponValue(),
                bond.getMaturityDate(),
                bond.getFaceValue(),
                bond.getCouponFrequency(),
                bond.getCouponLength(),
                bond.getCouponDaysPassed(),
                bond.getOfferDate(),
                bond.getIsin()
        );
    }

    public void saveOrUpdateTBankData(Bond bond) {
        Optional<Bond> existing = findByIsin(bond.getIsin());
        if (existing.isPresent()) {
            updateTBankFields(bond);
        } else {
            save(bond);
        }
    }

    private void updateTBankFields(Bond bond) {
        String sql = "UPDATE bonds SET figi = ?, instrument_uid = ?, asset_uid = ?, brand_name = ?, " +
                "updated_at = CURRENT_TIMESTAMP WHERE isin = ?";
        
        jdbcTemplate.update(sql,
                bond.getFigi(),
                bond.getInstrumentUid(),
                bond.getAssetUid(),
                bond.getBrandName(),
                bond.getIsin()
        );
    }

    public void saveOrUpdateCalculationData(Bond bond) {
        Optional<Bond> existing = findByIsin(bond.getIsin());
        if (existing.isPresent()) {
            updateCalculationFields(bond);
        } else {
            save(bond);
        }
    }

    private void updateCalculationFields(Bond bond) {
        String sql = "UPDATE bonds SET coupon_daily = ?, nkd = ?, costs = ?, fee = ?, " +
                "coupon_redemption = ?, profit = ?, profit_net = ?, annual_yield = ?, " +
                "coupon_offer = ?, profit_offer = ?, profit_net_offer = ?, annual_yield_offer = ?, " +
                "updated_at = CURRENT_TIMESTAMP WHERE isin = ?";
        
        jdbcTemplate.update(sql,
                bond.getCouponDaily(),
                bond.getNkd(),
                bond.getCosts(),
                bond.getFee(),
                bond.getCouponRedemption(),
                bond.getProfit(),
                bond.getProfitNet(),
                bond.getAnnualYield(),
                bond.getCouponOffer(),
                bond.getProfitOffer(),
                bond.getProfitNetOffer(),
                bond.getAnnualYieldOffer(),
                bond.getIsin()
        );
    }

    public void updatePrice(String isin, java.math.BigDecimal price) {
        jdbcTemplate.update("UPDATE bonds SET price = ?, updated_at = CURRENT_TIMESTAMP WHERE isin = ?", 
                price, isin);
    }

    public void updateRating(String isin, String ratingValue, Integer ratingCode) {
        jdbcTemplate.update("UPDATE bonds SET rating_value = ?, rating_code = ?, updated_at = CURRENT_TIMESTAMP WHERE isin = ?", 
                ratingValue, ratingCode, isin);
    }

    public long count() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM bonds", Long.class);
    }
}