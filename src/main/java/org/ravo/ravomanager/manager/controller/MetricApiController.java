package org.ravo.ravomanager.manager.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ravo.ravomanager.manager.monitoring.MetricData;
import org.ravo.ravomanager.manager.service.MonitoringService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Prometheus 메트릭 데이터 API 컨트롤러
 * Active/Standby DB의 실시간 성능 메트릭을 제공합니다.
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MetricApiController {

    private final MonitoringService monitoringService;

    /**
     * Active/Standby DB의 실시간 메트릭 데이터를 반환합니다.
     * 
     * @return Active 및 Standby DB의 메트릭 데이터 맵
     */
    @GetMapping("/metrics")
    public Mono<Map<String, MetricData>> getMetrics() {
        log.debug("메트릭 데이터 요청");
        return monitoringService.fetchMetrics();
    }
}
