package ru.misterparser.bonds.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import ru.misterparser.bonds.model.TelegramUser;
import ru.misterparser.bonds.service.TelegramAuthService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class ApiSecurityInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(ApiSecurityInterceptor.class);
    private static final Long AUTHORIZED_USER_ID = 1L;

    @Autowired
    private TelegramAuthService telegramAuthService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestURI = request.getRequestURI();
        
        // Проверяем только endpoints начинающиеся с /api
        if (!requestURI.startsWith("/api")) {
            return true;
        }

        logger.debug("API security check for: {}", requestURI);

        try {
            // Получаем текущего пользователя через сессию
            TelegramUser currentUser = telegramAuthService.getCurrentUserFromSession(request);
            
            if (currentUser == null) {
                logger.warn("Unauthorized API access attempt to: {}", requestURI);
                sendUnauthorizedResponse(response, "Пользователь не авторизован");
                return false;
            }

            if (!AUTHORIZED_USER_ID.equals(currentUser.getId())) {
                logger.warn("Forbidden API access attempt by user ID {} to: {}", currentUser.getId(), requestURI);
                sendForbiddenResponse(response, "Доступ запрещен: требуется пользователь с ID " + AUTHORIZED_USER_ID);
                return false;
            }

            logger.debug("API access granted for user ID {} to: {}", currentUser.getId(), requestURI);
            return true;

        } catch (Exception e) {
            logger.error("Error during API security check for: {}", requestURI, e);
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