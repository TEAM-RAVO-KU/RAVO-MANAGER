package org.ravo.client.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 에러 페이지 컨트롤러
 */
@Controller
@RequestMapping("/error")
public class ErrorController {

    /**
     * 에러 상세 정보 페이지
     */
    @GetMapping("/detail")
    public String errorDetail(HttpSession session, Model model) {
        // 세션에서 에러 정보 가져오기
        String timestamp = (String) session.getAttribute("errorTimestamp");
        String errorMessage = (String) session.getAttribute("errorMessage");
        String errorType = (String) session.getAttribute("errorType");
        
        // 에러 정보가 없으면 기본값 설정
        if (timestamp == null) {
            timestamp = "N/A";
        }
        if (errorMessage == null) {
            errorMessage = "알 수 없는 오류가 발생했습니다.";
        }
        if (errorType == null) {
            errorType = "Unknown";
        }
        
        model.addAttribute("timestamp", timestamp);
        model.addAttribute("errorMessage", errorMessage);
        model.addAttribute("errorType", errorType);
        
        return "client/db-error-detail";
    }
}
