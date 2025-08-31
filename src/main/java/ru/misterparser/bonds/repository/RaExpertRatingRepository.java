package ru.misterparser.bonds.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.misterparser.bonds.model.RaExpertRating;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class RaExpertRatingRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<RaExpertRating> rowMapper = new RowMapper<RaExpertRating>() {
        @Override
        public RaExpertRating mapRow(ResultSet rs, int rowNum) throws SQLException {
            RaExpertRating rating = new RaExpertRating();
            rating.setId(rs.getLong("id"));
            rating.setIsin(rs.getString("isin"));
            rating.setCompanyName(rs.getString("company_name"));
            rating.setRatingValue(rs.getString("rating_value"));
            rating.setRatingCode(rs.getInt("rating_code"));
            rating.setRatingDate(rs.getDate("rating_date").toLocalDate());
            rating.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            rating.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
            return rating;
        }
    };

    public void saveOrUpdate(RaExpertRating rating) {
        try {
            String sql = "INSERT INTO raexpert_ratings (isin, company_name, rating_value, rating_code, rating_date) " +
                    "VALUES (?, ?, ?, ?, ?) " +
                    "ON CONFLICT (isin, rating_date) " +
                    "DO UPDATE SET " +
                    "company_name = EXCLUDED.company_name, " +
                    "rating_value = EXCLUDED.rating_value, " +
                    "rating_code = EXCLUDED.rating_code, " +
                    "updated_at = CURRENT_TIMESTAMP";
            
            jdbcTemplate.update(sql,
                rating.getIsin(),
                rating.getCompanyName(),
                rating.getRatingValue(),
                rating.getRatingCode(),
                rating.getRatingDate()
            );
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to save or update RaExpert rating for ISIN: " + rating.getIsin(), e);
        }
    }

    public List<RaExpertRating> findAll() {
        try {
            String sql = "SELECT * FROM raexpert_ratings ORDER BY rating_date DESC, isin";
            return jdbcTemplate.query(sql, rowMapper);
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to load RaExpert ratings", e);
        }
    }

}
