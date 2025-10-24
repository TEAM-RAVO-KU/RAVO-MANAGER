package org.ravo.ravomanager.manager.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ravo.ravomanager.manager.dto.SelectorStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Kubernetes 상태 조회 서비스
 * 외부 K8s Watcher API에서 현재 서비스 타겟 정보를 가져옵니다.
 */
@Service
public class KubernetesStatusService {

    private static final Logger log = LoggerFactory.getLogger(KubernetesStatusService.class);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

    @Value("${application.failover.status-url}")
    private String serviceStatusUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // 메모리에 상태 저장 (단일 인스턴스 가정)
    private volatile SelectorStatus cachedStatus;

    public KubernetesStatusService(RestTemplateBuilder restTemplateBuilder, ObjectMapper objectMapper) {
        this.restTemplate = restTemplateBuilder
                .connectTimeout(REQUEST_TIMEOUT)
                .readTimeout(REQUEST_TIMEOUT)
                .build();
        this.objectMapper = objectMapper;
        this.cachedStatus = SelectorStatus.empty();
    }

    /**
     * K8s Selector 상태 조회 (동기 방식)
     * Response format: {"service_target": "mysql-active", "watcher_state": "active", ...}
     *
     * @return 현재 Selector 상태, 실패 시 기본값(empty) 반환
     */
    public SelectorStatus fetchStatus() {
        try {
            log.debug("Fetching K8s selector status from: {}", serviceStatusUrl);

            ResponseEntity<String> response = restTemplate.getForEntity(serviceStatusUrl, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.hasBody()) {
                return parseAndUpdateStatus(response.getBody());
            } else {
                log.debug("K8s selector status returned non-2xx status: {}", response.getStatusCode());
                return SelectorStatus.empty();
            }

        } catch (RestClientException e) {
            // 연결 실패, 타임아웃, HTTP 오류 등
            log.debug("Failed to fetch K8s selector status: {}", e.getMessage());
            return SelectorStatus.empty();
        } catch (Exception e) {
            // 예상치 못한 오류
            log.debug("Unexpected error while fetching K8s status: {}", e.getMessage());
            return SelectorStatus.empty();
        }
    }

    /**
     * JSON 파싱 및 상태 업데이트
     */
    private SelectorStatus parseAndUpdateStatus(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            String watcherState = root.path("watcher_state").asText("unknown");

            log.debug("Received watcher_state: {}", watcherState);

            // 정규화: "active" 또는 "standby"만 허용
            String normalizedState = normalizeState(watcherState);

            // 상태가 변경되었는지 확인
            if (cachedStatus == null || !cachedStatus.getCurrentTarget().equals(normalizedState)) {
                String previousState = cachedStatus != null ? cachedStatus.getCurrentTarget() : "unknown";
                log.info("K8s Selector state changed: {} -> {}", previousState, normalizedState);
                cachedStatus = new SelectorStatus(normalizedState);
            }

            return cachedStatus;
        } catch (Exception e) {
            log.warn("Failed to parse K8s status JSON: {}", json, e);
            return SelectorStatus.empty();
        }
    }

    /**
     * 상태 정규화 (active/standby만 허용)
     */
    private String normalizeState(String state) {
        if (state == null) {
            return "unknown";
        }

        String normalized = state.toLowerCase().trim();

        // "mysql-active" -> "active", "mysql-standby" -> "standby"
        if (normalized.contains("active")) {
            return "active";
        } else if (normalized.contains("standby")) {
            return "standby";
        }

        return normalized;
    }
}