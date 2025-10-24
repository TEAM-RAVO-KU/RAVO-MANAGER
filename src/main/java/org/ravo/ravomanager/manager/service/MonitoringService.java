package org.ravo.ravomanager.manager.service;

import org.ravo.ravomanager.manager.monitoring.MetricData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

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
     */
    public Mono<Map<String, MetricData>> fetchMetrics() {
        Mono<String> activeMetrics = fetchRawMetrics(activeDatabaseEndpoint)
                .onErrorResume(error -> {
                    log.warn("Failed to fetch Active DB metrics. Returning empty data.", error);
                    return Mono.just("");
                });

        Mono<String> standbyMetrics = fetchRawMetrics(standbyDatabaseEndpoint)
                .onErrorResume(error -> {
                    log.warn("Failed to fetch Standby DB metrics. Returning empty data.", error);
                    return Mono.just("");
                });

        return Mono.zip(activeMetrics, standbyMetrics)
                .map(tuple -> {
                    MetricData activeData = prometheusMetricParser.parse(tuple.getT1(), "Active");
                    MetricData standbyData = prometheusMetricParser.parse(tuple.getT2(), "Standby");

                    Map<String, MetricData> result = new HashMap<>();
                    result.put("active", activeData);
                    result.put("standby", standbyData);
                    return result;
                });
    }

    /**
     * 지정된 URL에서 Prometheus 메트릭 원본 데이터 조회
     */
    private Mono<String> fetchRawMetrics(String endpoint) {
        log.debug("Fetching metrics from: {}", endpoint);
        return webClient.get()
                .uri(endpoint)
                .retrieve()
                .bodyToMono(String.class);
    }
}