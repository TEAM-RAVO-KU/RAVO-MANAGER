package org.ravo.ravomanager.manager.service;

import java.time.LocalDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ravo.ravomanager.manager.domain.SyncStatus;
import org.ravo.ravomanager.manager.dto.SynchronizationMetricsDto;
import org.ravo.ravomanager.manager.monitoring.MetricData;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class SynchronizationMetricsService {

    private final TableHashService tableHashService;

    @Qualifier("standbyJdbcTemplate")
    private final JdbcTemplate standbyJdbcTemplate;

    @Qualifier("batchJdbcTemplate")
    private final JdbcTemplate batchJdbcTemplate;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Active/Standby DB 동기화 메트릭 취합
     */
    public SynchronizationMetricsDto buildSyncMetrics(Map<String, MetricData> metrics, double syncRate) {
        MetricData activeData = metrics.get("active");
        MetricData standbyData = metrics.get("standby");

        try {
            // Sync rate 계산
            SyncStatus syncStatus = tableHashService.calculateSyncStatus();
            syncRate = syncStatus.getSyncPercent();

            // Active GTID (배치 DB 기록 중 최신)
            Map<String, Object> latestGtidRow = fetchLatestGtidRecord();

            String activeGtid = latestGtidRow != null
                    ? (String) latestGtidRow.getOrDefault("gtid_set", "N/A")
                    : "N/A";

            String lastSyncTime = latestGtidRow != null && latestGtidRow.get("created_at") != null
                    ? ((LocalDateTime) latestGtidRow.get("created_at")).format(FORMATTER)
                    : "N/A";

            // Standby GTID (Standby DB 직접 조회)
            String standbyGtid;
            try {
                standbyGtid = standbyJdbcTemplate.queryForObject(
                        "SELECT @@GLOBAL.GTID_EXECUTED", String.class);
            } catch (Exception e) {
                log.warn("Failed to fetch Standby GTID", e);
                standbyGtid = "ERROR";
            }

            // 데이터 전송량
            double activeBytesSent = activeData.getMetrics().getOrDefault("mysql_global_status_bytes_sent", 0.0);
            double standbyBytesSent = standbyData.getMetrics().getOrDefault("mysql_global_status_bytes_sent", 0.0);
            String activeDataTransferred = formatBytes(activeBytesSent);

            String stanadbyDataTransferred = formatBytes(standbyBytesSent);

            // DTO 구성
            return SynchronizationMetricsDto.builder()
                    .syncRate(syncRate)
                    .activeGtid(activeGtid)
                    .standbyGtid(standbyGtid)
                    .activeDataTransferred(activeDataTransferred)
                    .standbyDataTransferred(stanadbyDataTransferred)
                    .lastSyncTime(lastSyncTime)
                    .build();

        } catch (Exception e) {
            log.error("Failed to build synchronization metrics", e);
            return SynchronizationMetricsDto.builder()
                    .syncRate(0.0)
                    .activeGtid("ERROR")
                    .standbyGtid("ERROR")
                    .activeDataTransferred("0 GB")
                    .standbyDataTransferred("0 GB")
                    .lastSyncTime("N/A")
                    .build();
        }
    }
    /**
     * batchDB에서 최신 GTID 레코드 조회
     */
    private Map<String, Object> fetchLatestGtidRecord() {
        try {
            String query = """
                    SELECT gtid_set, created_at
                    FROM gtid_history
                    ORDER BY created_at DESC
                    LIMIT 1
                    """;
            return batchJdbcTemplate.queryForMap(query);
        } catch (Exception e) {
            log.warn("No GTID record found in batchDB", e);
            return null;
        }
    }
    private String formatBytes(double bytes) {
        if (bytes < 1024) return String.format("%.0f B", bytes);
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024 * 1024));
        return String.format("%.2f GB", bytes / (1024 * 1024 * 1024));
    }
}
