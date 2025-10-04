package org.ravo.ravomanager.manager.data.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
public class MetricData {

    private String dbName; // "Active" 또는 "Standby"
    private Status status = Status.UNKNOWN;
    private final Map<String, Double> metrics = new HashMap<>();
    private final Map<String, String> info = new HashMap<>();

    public enum Status {
        UP, DOWN, UNKNOWN
    }

    public MetricData(String dbName) {
        this.dbName = dbName;
    }

    public void addMetric(String key, Double value) {
        this.metrics.put(key, value);
    }

    public void addInfo(String key, String value) {
        this.info.put(key, value);
    }
}