package org.ravo.dashboard.service;

import lombok.extern.slf4j.Slf4j;
import org.ravo.dashboard.domain.SyncStatus;
import org.ravo.ravomanager.manager.data.dto.MetricData;
import org.ravo.ravomanager.manager.service.MetricParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;


@Slf4j
@Service
public class ClientDashboardService {

    private final WebClient webClient;
    private final MetricParser metricParser;
    private final TableHashService tableHashService;

    @Value("${monitoring.endpoints.active}")
    private String activeDbUrl;

    @Value("${monitoring.endpoints.standby}")
    private String standbyDbUrl;

    public ClientDashboardService(WebClient.Builder webClientBuilder,
                                  MetricParser metricParser,
                                  TableHashService tableHashService) {
        this.webClient = webClientBuilder.build();
        this.metricParser = metricParser;
        this.tableHashService = tableHashService;
    }

    public Mono<Map<String, MetricData>> fetchMetrics() {
        Mono<String> activeMetricsMono = fetchRawMetrics(activeDbUrl)
                .onErrorResume(e -> {
                    log.warn("Active DB metrics request failed. Returning empty string.", e);
                    return Mono.just(""); // 에러 발생 시 빈 문자열 반환
                });

        Mono<String> standbyMetricsMono = fetchRawMetrics(standbyDbUrl)
                .onErrorResume(e -> {
                    log.warn("Standby DB metrics request failed. Returning empty string.", e);
                    return Mono.just(""); // 에러 발생 시 빈 문자열 반환
                });

        return Mono.zip(activeMetricsMono, standbyMetricsMono)
                .map(tuple -> {
                    MetricData activeData = metricParser.parse(tuple.getT1(), "Active");
                    MetricData standbyData = metricParser.parse(tuple.getT2(), "Standby");

                    // 수정 가능한 HashMap을 생성하여 반환
                    Map<String, MetricData> result = new HashMap<>();
                    result.put("active", activeData);
                    result.put("standby", standbyData);
                    return result;
                });
    }

    private Mono<String> fetchRawMetrics(String url) {
        log.info("Fetching metrics from: {}", url);

        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class);
    }


    public void turnActiveDbOff() {
        System.out.println("[RAVO] Active DB → OFF");
        // 실제로는 DB 연결 해제 or docker container stop 명령 등
    }

    public void turnActiveDbOn() {
        System.out.println("[RAVO] Active DB → ON");
        // 실제 DB start 또는 복구 스크립트 호출
    }

    /**
     * 테이블 해시값 기반 실제 동기화 상태 계산
     */
    public SyncStatus calculateTableSyncStatus() {
        return tableHashService.calculateSyncStatus();
    }


}