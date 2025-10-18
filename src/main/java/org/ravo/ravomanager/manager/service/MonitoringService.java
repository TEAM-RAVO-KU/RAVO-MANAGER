package org.ravo.ravomanager.manager.service;

import org.ravo.ravomanager.manager.data.dto.MetricData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger; // Logger import 추가
import org.slf4j.LoggerFactory; // LoggerFactory import 추가

@Service
public class MonitoringService {

    private static final Logger log = LoggerFactory.getLogger(MonitoringService.class);

    private final WebClient webClient;
    private final MetricParser metricParser;

    @Value("${monitoring.endpoints.active}")
    private String activeDbUrl;

    @Value("${monitoring.endpoints.standby}")
    private String standbyDbUrl;

    public MonitoringService(WebClient.Builder webClientBuilder, MetricParser metricParser) {
        this.webClient = webClientBuilder.build();
        this.metricParser = metricParser;
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
}