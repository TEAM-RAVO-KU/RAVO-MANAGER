package org.ravo.ravomanager.manager.controller;

import org.ravo.ravomanager.manager.data.dto.MetricData;
import org.ravo.ravomanager.manager.service.MonitoringService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
public class MetricApiController {

    private static final Logger log = LoggerFactory.getLogger(MonitoringService.class);

    private final MonitoringService monitoringService;

    public MetricApiController(MonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    @GetMapping("/api/metrics")
    public Mono<Map<String, MetricData>> getMetrics() {
        // API 요청에는 Mono를 직접 반환하여 비동기 처리
        return monitoringService.fetchMetrics();
    }
}