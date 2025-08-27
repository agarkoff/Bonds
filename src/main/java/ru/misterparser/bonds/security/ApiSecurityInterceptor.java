package ru.misterparser.bonds.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import ru.misterparser.bonds.model.TelegramUser;
import ru.misterparser.bonds.service.TelegramAuthService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApiSecurityInterceptor implements HandlerInterceptor {

    private static final Long AUTHORIZED_USER_ID = 1L;
    
    private final TelegramAuthService telegramAuthService;
    private final Environment environment;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestURI = request.getRequestURI();
        
        // Проверяем только endpoints начинающиеся с /api
        if (!requestURI.startsWith("/api")) {
            return true;
        }

        // Отключаем авторизацию для профиля 'test'
        if (Arrays.asList(environment.getActiveProfiles()).contains("no-auth")) {
            log.debug("API security disabled for no-auth profile: {}", requestURI);
            return true;
        }

        log.debug("API security check for: {}", requestURI);

        try {
            // Получаем текущего пользователя через сессию
            TelegramUser currentUser = telegramAuthService.getCurrentUserFromSession(request);
            
            if (currentUser == null) {
                log.warn("Unauthorized API access attempt to: {}", requestURI);
                sendUnauthorizedResponse(response, "Пользователь не авторизован");
                return false;
            }

            if (!AUTHORIZED_USER_ID.equals(currentUser.getId())) {
                log.warn("Forbidden API access attempt by user ID {} to: {}", currentUser.getId(), requestURI);
                sendForbiddenResponse(response, "Доступ запрещен: требуется пользователь с ID " + AUTHORIZED_USER_ID);
                return false;
            }

            log.debug("API access granted for user ID {} to: {}", currentUser.getId(), requestURI);
            return true;

        } catch (Exception e) {
            log.error("Error during API security check for: {}", requestURI, e);
            sendInternalErrorResponse(response, "Ошибка проверки авторизации");
            return false;
        }
    }

    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }

    private void sendForbiddenResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }

    private void sendInternalErrorResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }
}