package org.ravo.ravomanager.service;

import org.ravo.ravomanager.data.dto.MetricData;
import org.springframework.stereotype.Service;
import java.util.Arrays;

@Service
public class MetricParser {

    public MetricData parse(String rawMetrics, String dbName) {
        MetricData metricData = new MetricData(dbName);

        if (rawMetrics == null || rawMetrics.isEmpty()) {
            metricData.setStatus(MetricData.Status.DOWN);
            return metricData;
        }

        Arrays.stream(rawMetrics.split("\n"))
                .filter(line -> !line.startsWith("#") && !line.trim().isEmpty())
                .forEach(line -> {
                    try {
                        int lastSpaceIndex = line.lastIndexOf(' ');
                        if (lastSpaceIndex == -1) return;

                        String keyPart = line.substring(0, lastSpaceIndex);
                        String valueStr = line.substring(lastSpaceIndex + 1);
                        double value = Double.parseDouble(valueStr);

                        // Changed: Regex 대신 수동 파싱으로 안정성 확보
                        if (keyPart.contains("{") && keyPart.endsWith("}")) {
                            int braceStart = keyPart.indexOf('{');
                            String metricName = keyPart.substring(0, braceStart);
                            String labels = keyPart.substring(braceStart + 1, keyPart.length() - 1);

                            if ("mysql_global_status_commands_total".equals(metricName)) {
                                String command = labels.split("=")[1].replace("\"", "");
                                metricData.addMetric(metricName + "_" + command, value);
                            } else {
                                metricData.addMetric(metricName, value);
                                Arrays.stream(labels.split(","))
                                        .map(label -> label.split("=", 2))
                                        .filter(parts -> parts.length == 2)
                                        .forEach(parts -> {
                                            String infoKey = metricName + "_" + parts[0].trim();
                                            String infoValue = parts[1].replace("\"", "").trim();
                                            metricData.addInfo(infoKey, infoValue);
                                        });
                            }
                        } else {
                            metricData.addMetric(keyPart, value);
                        }
                    } catch (Exception e) {
                        // 파싱 오류 무시
                    }
                });

        if (metricData.getMetrics().getOrDefault("mysql_up", 0.0) == 1.0) {
            metricData.setStatus(MetricData.Status.UP);
        } else {
            metricData.setStatus(MetricData.Status.DOWN);
        }
        return metricData;
    }
}