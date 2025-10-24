package org.ravo.ravomanager.manager.service;

import org.ravo.ravomanager.manager.monitoring.MetricData;
import org.ravo.ravomanager.manager.monitoring.MetricData.DatabaseStatus;
import org.springframework.stereotype.Service;
import java.util.Arrays;

/**
 * Prometheus 메트릭 원본 데이터를 파싱하는 서비스
 */
@Service
public class PrometheusMetricParser {

    /**
     * Prometheus 형식의 원본 메트릭 데이터를 MetricData 객체로 변환
     * 
     * @param rawMetrics Prometheus 형식의 원본 메트릭 문자열
     * @param databaseName 데이터베이스 이름 (Active/Standby)
     * @return 파싱된 MetricData 객체
     */
    public MetricData parse(String rawMetrics, String databaseName) {
        MetricData metricData = new MetricData(databaseName);

        if (rawMetrics == null || rawMetrics.isEmpty()) {
            metricData.setStatus(DatabaseStatus.DOWN);
            return metricData;
        }

        Arrays.stream(rawMetrics.split("\n"))
                .filter(line -> !line.startsWith("#") && !line.trim().isEmpty())
                .forEach(line -> parseSingleMetricLine(line, metricData));

        updateDatabaseStatus(metricData);
        return metricData;
    }

    /**
     * 단일 메트릭 라인을 파싱하여 MetricData에 추가
     */
    private void parseSingleMetricLine(String line, MetricData metricData) {
        try {
            int lastSpaceIndex = line.lastIndexOf(' ');
            if (lastSpaceIndex == -1) {
                return;
            }

            String keyPart = line.substring(0, lastSpaceIndex);
            String valueString = line.substring(lastSpaceIndex + 1);
            double value = Double.parseDouble(valueString);

            if (keyPart.contains("{") && keyPart.endsWith("}")) {
                parseMetricWithLabels(keyPart, value, metricData);
            } else {
                metricData.addMetric(keyPart, value);
            }
        } catch (Exception e) {
            // 파싱 실패한 라인은 무시
        }
    }

    /**
     * 레이블이 포함된 메트릭 파싱
     */
    private void parseMetricWithLabels(String keyPart, double value, MetricData metricData) {
        int braceStart = keyPart.indexOf('{');
        String metricName = keyPart.substring(0, braceStart);
        String labelsString = keyPart.substring(braceStart + 1, keyPart.length() - 1);

        if ("mysql_global_status_commands_total".equals(metricName)) {
            parseCommandMetric(labelsString, value, metricData);
        } else {
            parseGenericMetricWithLabels(metricName, labelsString, value, metricData);
        }
    }

    /**
     * MySQL 명령어 메트릭 파싱 (SELECT, INSERT, UPDATE, DELETE 등)
     */
    private void parseCommandMetric(String labelsString, double value, MetricData metricData) {
        String command = labelsString.split("=")[1].replace("\"", "").toLowerCase();
        metricData.addMetric("mysql_global_status_commands_total_" + command, value);
    }

    /**
     * 일반 레이블 메트릭 파싱
     */
    private void parseGenericMetricWithLabels(String metricName, String labelsString, double value, MetricData metricData) {
        metricData.addMetric(metricName, value);
        
        Arrays.stream(labelsString.split(","))
                .map(label -> label.split("=", 2))
                .filter(parts -> parts.length == 2)
                .forEach(parts -> {
                    String infoKey = metricName + "_" + parts[0].trim();
                    String infoValue = parts[1].replace("\"", "").trim();
                    metricData.addInfo(infoKey, infoValue);
                });
    }

    /**
     * mysql_up 메트릭을 기반으로 데이터베이스 상태 업데이트
     */
    private void updateDatabaseStatus(MetricData metricData) {
        double mysqlUpValue = metricData.getMetrics().getOrDefault("mysql_up", 0.0);
        metricData.setStatus(mysqlUpValue == 1.0 ? DatabaseStatus.UP : DatabaseStatus.DOWN);
    }
}