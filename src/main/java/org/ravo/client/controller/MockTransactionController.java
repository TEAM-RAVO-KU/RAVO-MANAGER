package org.ravo.client.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ravo.client.service.MockTransactionService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequestMapping("/client")
@RequiredArgsConstructor
@Slf4j
public class MockTransactionController {

    private final MockTransactionService mockTransactionService;

    /**
     * 트랜잭션 자동 생성 토글 API
     * - 실행 중이면 중단, 중단 상태면 시작
     */
    @PostMapping("/admin/mock-tx")
    public String toggleMockTransaction() {
        mockTransactionService.toggleMockTx();

        return "redirect:/client/dashboard";
    }
    /**
     * Mock Transaction 생성 로그 조회
     */
    @ResponseBody
    @GetMapping("/api/mock-logs")
    public List<String> mockLogs() {
        return mockTransactionService.getRecentLogs();
    }
}
