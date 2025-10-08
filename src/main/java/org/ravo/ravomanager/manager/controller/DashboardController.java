package org.ravo.ravomanager.manager.controller;

import org.ravo.ravomanager.manager.data.dto.MetricData;
import org.ravo.ravomanager.manager.service.MonitoringService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger; // Logger import 추가
import org.slf4j.LoggerFactory; // LoggerFactory import 추가

@Controller
public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(MonitoringService.class);

    private final MonitoringService monitoringService;

    public DashboardController(MonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // block()을 사용하여 동기적으로 데이터를 가져옵니다.
        Map<String, MetricData> metrics = monitoringService.fetchMetrics().block();

        // Null 체크 및 방어 코드 추가
        // metrics 맵 자체가 null일 경우를 대비해 빈 맵으로 초기화합니다.
        if (metrics == null) {
            metrics = new HashMap<>();
        }

        // "active"와 "standby" 데이터가 없으면, 템플릿 오류 방지를 위해 비어있는 객체를 생성
        metrics.putIfAbsent("active", new MetricData("Active (Not Found)"));
        metrics.putIfAbsent("standby", new MetricData("Standby (Not Found)"));

        log.info("[DashboardController] Metrics Size : " + metrics.size());

        model.addAttribute("initialData", metrics);
        return "manager/dashboard";
    }
}