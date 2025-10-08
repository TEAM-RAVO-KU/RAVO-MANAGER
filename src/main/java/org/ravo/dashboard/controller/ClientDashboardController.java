package org.ravo.dashboard.controller;

import lombok.RequiredArgsConstructor;
import org.ravo.client.service.MockTransactionService;
import org.ravo.dashboard.domain.DatabaseMetrics;
import org.ravo.dashboard.domain.SyncStatus;
import org.ravo.dashboard.service.ClientDashboardService;
import org.ravo.ravomanager.manager.data.dto.MetricData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/client")
@RequiredArgsConstructor
public class ClientDashboardController {

    private static final Logger log = LoggerFactory.getLogger(ClientDashboardController.class);
    private final ClientDashboardService clientDashboardService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Map<String, MetricData> metrics = clientDashboardService.fetchMetrics().block();

        // Null 체크 및 방어 코드 추가
        // metrics 맵 자체가 null일 경우를 대비해 빈 맵으로 초기화합니다.
        if (metrics == null) {
            metrics = new HashMap<>();
        }

        // "active"와 "standby" 데이터가 없으면, 템플릿 오류 방지를 위해 비어있는 객체를 생성
        metrics.putIfAbsent("active", new MetricData("Active (Not Found)"));
        metrics.putIfAbsent("standby", new MetricData("Standby (Not Found)"));

        model.addAttribute("syncPercent", "97%");
        model.addAttribute("kafkaLag", "3");


        model.addAttribute("initialData", metrics);

        return "client/dashboard";
    }

    /**
     * 테이블 동기화 상태 조회 (실시간)
     */
    @GetMapping("/api/sync-status")
    @ResponseBody
    public SyncStatus syncStatus() {
        return clientDashboardService.calculateTableSyncStatus();
    }

    @PostMapping("/admin/db-off")
    public String dbOff() {
        log.warn("[RAVO] Active DB → OFF triggered manually.");
        clientDashboardService.turnActiveDbOff();
        return "redirect:/client/dashboard";
    }

    @PostMapping("/admin/db-on")
    public String dbOn() {
        log.info("[RAVO] Active DB → ON triggered manually.");
        clientDashboardService.turnActiveDbOn();
        return "redirect:/client/dashboard";
    }
}
