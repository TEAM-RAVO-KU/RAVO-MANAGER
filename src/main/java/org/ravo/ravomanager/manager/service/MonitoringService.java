package org.ravo.ravomanager.manager.service;

import org.ravo.ravomanager.manager.monitoring.MetricData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Prometheus Exporter로부터 메트릭을 수집하는 서비스
 */
@Service
public class MonitoringService {

    private static final Logger log = LoggerFactory.getLogger(MonitoringService.class);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

    private final WebClient webClient;
    private final PrometheusMetricParser prometheusMetricParser;

    @Value("${monitoring.endpoints.active}")
    private String activeDatabaseEndpoint;

    @Value("${monitoring.endpoints.standby}")
    private String standbyDatabaseEndpoint;

    public MonitoringService(WebClient.Builder webClientBuilder, PrometheusMetricParser prometheusMetricParser) {
        this.webClient = webClientBuilder.build();
        this.prometheusMetricParser = prometheusMetricParser;
    }

    /**
     * Active DB와 Standby DB의 메트릭을 동시에 조회
     * 연결 실패 시 빈 MetricData를 반환하여 서비스가 중단되지 않도록 처리
     */
    public Mono<Map<String, MetricData>> fetchMetrics() {
        Mono<MetricData> activeMetrics = fetchMetricsForDatabase(activeDatabaseEndpoint, "Active");
        Mono<MetricData> standbyMetrics = fetchMetricsForDatabase(standbyDatabaseEndpoint, "Standby");

        return Mono.zip(activeMetrics, standbyMetrics)
                .map(tuple -> {
                    Map<String, MetricData> result = new HashMap<>();
                    result.put("active", tuple.getT1());
                    result.put("standby", tuple.getT2());
                    return result;
                })
                .onErrorResume(error -> {
                    // 예상치 못한 에러 발생 시에도 기본값 반환
                    log.debug("Unexpected error during metrics fetch, returning default values", error);
                    Map<String, MetricData> fallback = new HashMap<>();
                    fallback.put("active", MetricData.empty("Active"));
                    fallback.put("standby", MetricData.empty("Standby"));
                    return Mono.just(fallback);
                });
    }

    /**
     * 개별 데이터베이스의 메트릭을 조회하고 파싱
     * 실패 시 빈 MetricData 반환 (에러 로그 출력 없음)
     */
    private Mono<MetricData> fetchMetricsForDatabase(String endpoint, String dbName) {
        return fetchRawMetrics(endpoint)
                .map(rawMetrics -> prometheusMetricParser.parse(rawMetrics, dbName))
                .onErrorResume(WebClientRequestException.class, error -> {
                    // 연결 실패 (네트워크 오류, 타임아웃 등)
                    log.debug("{} DB connection failed: {}", dbName, error.getMessage());
                    return Mono.just(MetricData.empty(dbName));
                })
                .onErrorResume(WebClientResponseException.class, error -> {
                    // HTTP 에러 응답 (4xx, 5xx)
                    log.debug("{} DB returned error response: {} {}", 
                            dbName, error.getStatusCode(), error.getMessage());
                    return Mono.just(MetricData.empty(dbName));
                })
                .onErrorResume(error -> {
                    // 그 외 모든 예외 (파싱 오류 등)
                    log.debug("{} DB metrics fetch failed: {}", dbName, error.getMessage());
                    return Mono.just(MetricData.empty(dbName));
                });
    }

    /**
     * 지정된 URL에서 Prometheus 메트릭 원본 데이터 조회
     * 타임아웃 설정 포함
     */
    private Mono<String> fetchRawMetrics(String endpoint) {
        log.debug("Fetching metrics from: {}", endpoint);
        return webClient.get()
                .uri(endpoint)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(REQUEST_TIMEOUT);
    }
}
