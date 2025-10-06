package org.ravo.client.exception;

import jakarta.servlet.http.HttpSession;
import org.springframework.dao.DataAccessException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 데이터베이스 관련 예외 처리
     * - DataAccessException: Spring의 DB 접근 예외 (JDBC, JPA 등)
     */
    @ExceptionHandler(DataAccessException.class)
    public String handleDatabaseException(DataAccessException ex, Model model, HttpSession session) {
        // 현재 시간 추가
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        model.addAttribute("timestamp", timestamp);
        
        // 상세 정보를 세션에 저장 (상세 페이지에서 사용)
        session.setAttribute("errorTimestamp", timestamp);
        session.setAttribute("errorMessage", ex.getMessage());
        session.setAttribute("errorType", ex.getClass().getSimpleName());
        
        // 로그 출력 (실제 운영에서는 로깅 프레임워크 사용)
        System.err.println("[DB ERROR] " + timestamp + " - " + ex.getMessage());
        
        return "client/db-error";
    }

    /**
     * 일반적인 예외 처리 (선택사항)
     */
    @ExceptionHandler(Exception.class)
    public String handleGenericException(Exception ex, Model model, HttpSession session) {
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        model.addAttribute("timestamp", timestamp);
        
        // 상세 정보를 세션에 저장
        session.setAttribute("errorTimestamp", timestamp);
        session.setAttribute("errorMessage", ex.getMessage());
        session.setAttribute("errorType", ex.getClass().getSimpleName());
        
        System.err.println("[ERROR] " + timestamp + " - " + ex.getMessage());
        
        return "client/db-error";
    }
}
