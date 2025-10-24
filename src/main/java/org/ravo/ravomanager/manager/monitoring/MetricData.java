package org.ravo.ravomanager.manager.monitoring;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Prometheus 메트릭 데이터를 담는 클래스
 */
@Data
@NoArgsConstructor
public class MetricData {

    private String databaseName;
    private DatabaseStatus status = DatabaseStatus.UNKNOWN;
    private final Map<String, Double> metrics = new HashMap<>();
    private final Map<String, String> info = new HashMap<>();

    public enum DatabaseStatus {
        UP, DOWN, UNKNOWN
    }

    public MetricData(String databaseName) {
        this.databaseName = databaseName;
    }

    public void addMetric(String key, Double value) {
        this.metrics.put(key, value);
    }

    public void addInfo(String key, String value) {
        this.info.put(key, value);
    }

    /**
     * 장애 발생 시 사용할 안전한 기본 MetricData
     */
    public static MetricData empty(String roleName) {
        MetricData empty = new MetricData(roleName);
        empty.status = DatabaseStatus.DOWN;
        empty.metrics.put("cpu_usage", 0.0);
        empty.metrics.put("memory_usage", 0.0);
        empty.metrics.put("qps", 0.0);
        empty.metrics.put("uptime", 0.0);
        empty.info.put("message", "No metrics available (DB unreachable)");
        return empty;
    }

    /**
     * 현재 MetricData가 사실상 비어있는지 판단하는 메서드
     */
    public boolean isEmpty() {
        return metrics.isEmpty() || metrics.values().stream().allMatch(v -> v == 0.0);
    }
}
