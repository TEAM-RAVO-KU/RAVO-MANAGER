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
}
