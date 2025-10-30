package org.ravo.client.service;

import lombok.RequiredArgsConstructor;
import org.ravo.config.ClientEndPointProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DbFailoverControlService {

    private final RestTemplate restTemplate;
    private final ClientEndPointProperties endpoints;

    @Value("${application.failover.status-url}")
    private String statusUrl;

    /** Active DB를 Down 시뮬레이션 */
    public void activeDbDown() {
        callEndpoint(endpoints.getScaleDown(), "Active DB down");
    }

    /** Active DB를 Up(복구) 시뮬레이션 */
    public void activeDbUp() {
        callEndpoint(endpoints.getScaleUp(), "Active DB up");
    }

    private void callEndpoint(String url, String actionName) {
        if (url == null || url.isBlank()) {
            throw new IllegalStateException(actionName + " endpoint 가 설정되지 않았습니다.");
        }
        try {
            RequestEntity<Void> req = new RequestEntity<>(HttpMethod.GET, URI.create(url));
            restTemplate.exchange(req, Void.class);
        } catch (RestClientException e) {
            throw new RuntimeException(actionName + " 호출 실패: " + e.getMessage(), e);
        }
    }

    /** watcher_state 조회 (active|standby|unknown) */
    public String getWatcherState() {
        if (statusUrl == null || statusUrl.isBlank()) {
            throw new IllegalStateException("status endpoint 가 설정되지 않았습니다.");
        }

        try {
            var req = new RequestEntity<Void>(HttpMethod.GET, URI.create(statusUrl));
            var res = restTemplate.exchange(req, Map.class);
            if (!res.getStatusCode().is2xxSuccessful() || res.getBody() == null) return "unknown";

            Object state = res.getBody().get("watcher_state");
            return state == null ? "unknown" : state.toString();
        } catch (RestClientException e) {
            return "unknown";
        }
    }
}
