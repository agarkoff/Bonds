package ru.misterparser.bonds.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import ru.misterparser.bonds.service.TelegramUserDetailsService;

import javax.sql.DataSource;

@Configuration
@EnableWebSecurity
@Slf4j
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    
    @Autowired
    private DataSource dataSource;
    
    @Autowired
    private TelegramUserDetailsService telegramUserDetailsService;
    
    @Bean
    public PersistentTokenRepository persistentTokenRepository() {
        JdbcTokenRepositoryImpl tokenRepository = new JdbcTokenRepositoryImpl();
        tokenRepository.setDataSource(dataSource);
        
        try {
            // Попытка создать таблицу при первом запуске (JdbcTokenRepositoryImpl может это сделать)
            tokenRepository.setCreateTableOnStartup(false); // Не создавать автоматически, у нас есть Liquibase
            log.debug("PersistentTokenRepository настроен для использования существующей таблицы");
        } catch (Exception e) {
            log.warn("Проблема при настройке PersistentTokenRepository: {}", e.getMessage());
        }
        
        return tokenRepository;
    }
    
    @Bean
    public PersistentTokenBasedRememberMeServices rememberMeServices() {
        PersistentTokenBasedRememberMeServices rememberMeServices = 
            new PersistentTokenBasedRememberMeServices("bonds-remember-me-key", 
                                                      telegramUserDetailsService, 
                                                      persistentTokenRepository());
        rememberMeServices.setTokenValiditySeconds(60 * 60 * 24 * 30); // 30 дней
        rememberMeServices.setParameter("remember-me");
        rememberMeServices.setCookieName("bonds-remember-me");
        rememberMeServices.setAlwaysRemember(true); // Всегда запоминать пользователей Telegram
        return rememberMeServices;
    }
    
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .authorizeRequests(authorize -> authorize
                .antMatchers("/", "/login", "/register", "/telegram-login", "/auth/telegram/**", "/debug/**", "/proxy/**", "/css/**", "/js/**", "/images/**").permitAll()
                .antMatchers("/api/**").authenticated()
                .anyRequest().authenticated()
            )
            .formLogin().disable()
            .rememberMe(rememberMe -> rememberMe
                .rememberMeServices(rememberMeServices())
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID", "bonds-remember-me")
                .permitAll()
            )
            .csrf().disable();
    }
    
}
