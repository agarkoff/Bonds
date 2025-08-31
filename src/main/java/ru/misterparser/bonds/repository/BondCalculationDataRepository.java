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
public class BondCalculationDataRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Bond> bondDataRowMapper = new RowMapper<Bond>() {
        @Override
        public Bond mapRow(ResultSet rs, int rowNum) throws SQLException {
            Bond bond = new Bond();
            
            // Основные данные из moex_bonds
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
            
            // Данные из tbank_bonds
            bond.setFigi(rs.getString("figi"));
            bond.setInstrumentUid(rs.getString("instrument_uid"));
            bond.setAssetUid(rs.getString("asset_uid"));
            bond.setBrandName(rs.getString("brand_name"));
            
            // Цена из tbank_prices
            bond.setPriceAsk(rs.getBigDecimal("price_ask"));
            bond.setPriceBid(rs.getBigDecimal("price_bid"));
            
            // Рейтинг из dohod_ratings
            bond.setRatingValue(rs.getString("rating_value"));
            Integer ratingCode = rs.getObject("rating_code", Integer.class);
            bond.setRatingCode(ratingCode);
            
            return bond;
        }
    };

    /**
     * Получает все облигации из исходных таблиц для расчета показателей.
     * Использует данные из moex_bonds, tbank_bonds, tbank_prices, dohod_ratings
     * без обращения к представлению bonds и таблице bonds_calc.
     */
    public List<Bond> findAllForCalculation() {
        String sql = "SELECT " +
                "mb.isin, " +
                "mb.isin as ticker, " +
                "mb.short_name, " +
                "mb.coupon_value, " +
                "mb.maturity_date, " +
                "mb.face_value, " +
                "mb.coupon_frequency, " +
                "mb.coupon_length, " +
                "mb.coupon_days_passed, " +
                "mb.offer_date, " +
                "tb.figi, " +
                "tb.instrument_uid, " +
                "tb.asset_uid, " +
                "tb.brand_name, " +
                "tp.price_ask, " +
                "tp.price_bid, " +
                "dr.rating_value, " +
                "dr.rating_code " +
                "FROM moex_bonds mb " +
                "LEFT JOIN tbank_bonds tb ON tb.ticker = mb.isin OR tb.figi = mb.isin " +
                "LEFT JOIN tbank_prices tp ON tp.figi = tb.figi " +
                "LEFT JOIN dohod_ratings dr ON dr.isin = mb.isin " +
                "WHERE mb.face_value IS NOT NULL " +
                "AND mb.coupon_value IS NOT NULL " +
                "AND mb.maturity_date IS NOT NULL " +
                "AND mb.coupon_length IS NOT NULL " +
                "ORDER BY mb.isin";
        
        return jdbcTemplate.query(sql, bondDataRowMapper);
    }

    /**
     * Получает данные конкретной облигации по ISIN для расчета показателей.
     */
    public Optional<Bond> findByIsinForCalculation(String isin) {
        String sql = "SELECT " +
                "mb.isin, " +
                "mb.isin as ticker, " +
                "mb.short_name, " +
                "mb.coupon_value, " +
                "mb.maturity_date, " +
                "mb.face_value, " +
                "mb.coupon_frequency, " +
                "mb.coupon_length, " +
                "mb.coupon_days_passed, " +
                "mb.offer_date, " +
                "tb.figi, " +
                "tb.instrument_uid, " +
                "tb.asset_uid, " +
                "tb.brand_name, " +
                "tp.price_ask, " +
                "dr.rating_value, " +
                "dr.rating_code " +
                "FROM moex_bonds mb " +
                "LEFT JOIN tbank_bonds tb ON tb.ticker = mb.isin OR tb.figi = mb.isin " +
                "LEFT JOIN tbank_prices tp ON tp.figi = tb.figi " +
                "LEFT JOIN dohod_ratings dr ON dr.isin = mb.isin " +
                "WHERE mb.isin = ?";
        
        List<Bond> bonds = jdbcTemplate.query(sql, bondDataRowMapper, isin);
        return bonds.isEmpty() ? Optional.empty() : Optional.of(bonds.get(0));
    }
}