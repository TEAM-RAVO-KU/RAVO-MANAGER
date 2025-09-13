package org.ravo.ravomanager.service;

import org.ravo.ravomanager.data.dto.MetricData;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MetricParser {

    public MetricData parse(String rawMetrics, String dbName) {
        MetricData metricData = new MetricData(dbName);

        Arrays.stream(rawMetrics.split("\n"))
                .filter(line -> !line.startsWith("#") && !line.trim().isEmpty())
                .forEach(line -> {
                    try {
                        // Changed: 마지막 공백을 기준으로 메트릭 키와 값을 분리하여 안정성 확보
                        int lastSpaceIndex = line.lastIndexOf(' ');
                        if (lastSpaceIndex == -1) return; // 형식이 맞지 않으면 건너뜀

                        String metricKey = line.substring(0, lastSpaceIndex);
                        String valueStr = line.substring(lastSpaceIndex + 1);
                        double value = Double.parseDouble(valueStr);

                        if (metricKey.contains("{")) { // 레이블이 있는 메트릭
                            String metricName = metricKey.substring(0, metricKey.indexOf('{'));
                            if ("go_info".equals(metricName)) {
                                String labels = metricKey.substring(metricKey.indexOf('{') + 1, metricKey.indexOf('}'));
                                // Changed: label.split("=")의 결과가 String[]이므로, p[0]과 p[1]로 접근하여 Map 생성
                                Map<String, String> infoMap = Arrays.stream(labels.split(","))
                                        .map(label -> label.split("="))
                                        .collect(Collectors.toMap(
                                                p -> p[0], // p[0]은 키 (예: "version")
                                                p -> p[1].replace("\"", "") // p[1]은 값 (예: "\"go1.23.6\"")
                                        ));
                                metricData.addInfo("go_version", infoMap.getOrDefault("version", "N/A"));
                            }
                            metricData.addMetric(metricName, value);
                        } else { // 레이블이 없는 메트릭
                            metricData.addMetric(metricKey, value);
                        }
                    } catch (Exception e) {
                        // 파싱 중 오류가 발생한 라인은 무시
                        System.err.println("Failed to parse line: " + line);
                    }
                });

        // mysql_up 메트릭을 기반으로 최종 상태 결정
        if (metricData.getMetrics().getOrDefault("mysql_up", 0.0) == 1.0) {
            metricData.setStatus(MetricData.Status.UP);
        } else {
            metricData.setStatus(MetricData.Status.DOWN);
        }

        return metricData;
    }
}