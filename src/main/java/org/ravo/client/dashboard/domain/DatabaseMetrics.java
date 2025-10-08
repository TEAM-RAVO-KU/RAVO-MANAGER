package org.ravo.client.dashboard.domain;

import lombok.Getter;

import java.util.Map;

@Getter
public class DatabaseMetrics {

    private String dbName;
    private String status;  // "UP" / "DOWN"
    private long uptime;    // 초 단위
    private int connections;
    private Map<String, Object> metrics; // CPU, Memory, Network 등
    private Map<String, Object> info;    // MySQL 버전 등 부가정보

    public DatabaseMetrics(String dbName, String status, long uptime, int connections,
                           Map<String, Object> metrics, Map<String, Object> info) {
        this.dbName = dbName;
        this.status = status;
        this.uptime = uptime;
        this.connections = connections;
        this.metrics = metrics;
        this.info = info;
    }
}
