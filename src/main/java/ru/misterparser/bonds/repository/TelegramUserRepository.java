package ru.misterparser.bonds.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.misterparser.bonds.model.TelegramUser;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class TelegramUserRepository {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    private final RowMapper<TelegramUser> telegramUserRowMapper = new RowMapper<TelegramUser>() {
        @Override
        public TelegramUser mapRow(ResultSet rs, int rowNum) throws SQLException {
            TelegramUser user = new TelegramUser();
            user.setId(rs.getLong("id"));
            user.setTelegramId(rs.getLong("telegram_id"));
            user.setUsername(rs.getString("username"));
            user.setFirstName(rs.getString("first_name"));
            user.setLastName(rs.getString("last_name"));
            user.setPhotoUrl(rs.getString("photo_url"));
            user.setAuthDate(rs.getObject("auth_date", Long.class));
            user.setHash(rs.getString("hash"));
            user.setEnabled(rs.getBoolean("enabled"));
            user.setCreatedAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null);
            user.setUpdatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null);
            return user;
        }
    };
    
    public Optional<TelegramUser> findByTelegramId(Long telegramId) {
        List<TelegramUser> users = jdbcTemplate.query("SELECT * FROM telegram_users WHERE telegram_id = ?", telegramUserRowMapper, telegramId);
        return users.isEmpty() ? Optional.empty() : Optional.of(users.get(0));
    }
    
    public Optional<TelegramUser> findById(Long id) {
        List<TelegramUser> users = jdbcTemplate.query("SELECT * FROM telegram_users WHERE id = ?", telegramUserRowMapper, id);
        return users.isEmpty() ? Optional.empty() : Optional.of(users.get(0));
    }
    
    public TelegramUser save(TelegramUser user) {
        LocalDateTime now = LocalDateTime.now();
        
        if (user.getId() == null) {
            // Создание нового пользователя
            user.setCreatedAt(now);
            user.setUpdatedAt(now);
            
            String sql = "INSERT INTO telegram_users (telegram_id, username, first_name, last_name, photo_url, auth_date, hash, enabled, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                ps.setLong(1, user.getTelegramId());
                ps.setString(2, user.getUsername());
                ps.setString(3, user.getFirstName());
                ps.setString(4, user.getLastName());
                ps.setString(5, user.getPhotoUrl());
                if (user.getAuthDate() != null) {
                    ps.setLong(6, user.getAuthDate());
                } else {
                    ps.setNull(6, java.sql.Types.BIGINT);
                }
                ps.setString(7, user.getHash());
                ps.setBoolean(8, user.isEnabled());
                ps.setTimestamp(9, java.sql.Timestamp.valueOf(user.getCreatedAt()));
                ps.setTimestamp(10, java.sql.Timestamp.valueOf(user.getUpdatedAt()));
                return ps;
            }, keyHolder);
            
            // Получаем ID из автогенерированного ключа
            if (keyHolder.getKeys() != null && keyHolder.getKeys().containsKey("id")) {
                Number generatedId = (Number) keyHolder.getKeys().get("id");
                user.setId(generatedId.longValue());
            } else {
                throw new RuntimeException("Не удалось получить автогенерированный ID для TelegramUser");
            }
            
        } else {
            // Обновление существующего пользователя
            user.setUpdatedAt(now);
            
            String sql = "UPDATE telegram_users SET telegram_id = ?, username = ?, first_name = ?, last_name = ?, " +
                        "photo_url = ?, auth_date = ?, hash = ?, enabled = ?, updated_at = ? WHERE id = ?";
                        
            jdbcTemplate.update(sql, 
                user.getTelegramId(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhotoUrl(),
                user.getAuthDate(),
                user.getHash(),
                user.isEnabled(),
                java.sql.Timestamp.valueOf(user.getUpdatedAt()),
                user.getId()
            );
        }
        
        return user;
    }
}