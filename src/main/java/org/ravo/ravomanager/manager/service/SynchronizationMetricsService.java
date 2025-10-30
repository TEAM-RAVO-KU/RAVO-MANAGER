package org.ravo.ravomanager.manager.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.ravo.ravomanager.manager.domain.SyncStatus;
import org.ravo.ravomanager.manager.dto.SynchronizationMetricsDto;
import org.ravo.ravomanager.manager.monitoring.MetricData;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class SynchronizationMetricsService {

    private final TableHashService tableHashService;
    private final JdbcTemplate standbyJdbcTemplate;
    private final JdbcTemplate batchJdbcTemplate;

    public SynchronizationMetricsService(TableHashService tableHashService, @Qualifier("standbyJdbcTemplate")JdbcTemplate standbyJdbcTemplate,
                                         @Qualifier("batchJdbcTemplate") JdbcTemplate batchJdbcTemplate) {
        this.tableHashService = tableHashService;
        this.standbyJdbcTemplate = standbyJdbcTemplate;
        this.batchJdbcTemplate = batchJdbcTemplate;
    }

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


    /**
     * Active/Standby DB 동기화 메트릭 취합
     * 연결 실패 시 캐시된 값 또는 기본값 반환
     */
    public SynchronizationMetricsDto buildSyncMetrics(Map<String, MetricData> metrics, double syncRate) {
        try {
            MetricData activeData = metrics.get("active");
            MetricData standbyData = metrics.get("standby");

            // 메트릭 데이터가 없거나 DOWN 상태면 캐시/기본값 반환
            if (activeData == null || activeData.getStatus() == MetricData.DatabaseStatus.DOWN) {
                log.debug("Active DB is down or metrics unavailable, returning cached/default metrics");
                return createDefaultMetrics();
            }

            if (standbyData == null || standbyData.getStatus() == MetricData.DatabaseStatus.DOWN) {
                log.debug("Standby DB is down or metrics unavailable, returning cached/default metrics");
                return createDefaultMetrics();
            }

            // Sync rate 계산
            SyncStatus syncStatus = tableHashService.calculateSyncStatus();
            syncRate = syncStatus.getSyncPercent();

            // Active GTID (배치 DB 기록 중 최신)
            Map<String, Object> latestGtidRow = fetchLatestGtidRecord();

            String activeGtid = latestGtidRow != null
                    ? (String) latestGtidRow.getOrDefault("gtid_set", "N/A")
                    : "N/A";

            String lastSyncTime = "N/A";

            if (latestGtidRow != null && latestGtidRow.get("created_at") != null) {
                // DB에서 가져온 UTC 기준 시간
                LocalDateTime createdAtUtc = (LocalDateTime) latestGtidRow.get("created_at");

                // 변환에 필요한 객체들을 메서드 내에서 선언
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd HH:mm");
                ZoneId utcZone = ZoneId.of("UTC");
                ZoneId kstZone = ZoneId.of("Asia/Seoul");

                // UTC → KST 변환
                ZonedDateTime createdAtKst = createdAtUtc.atZone(utcZone).withZoneSameInstant(kstZone);

                // 포맷팅
                lastSyncTime = createdAtKst.format(formatter);
            }

            // Standby GTID (Standby DB 직접 조회)
            String standbyGtid = fetchStandbyGtid();

            // 데이터 전송량
            double activeBytesSent = activeData.getMetrics().getOrDefault("mysql_global_status_bytes_sent", 0.0);
            double standbyBytesSent = standbyData.getMetrics().getOrDefault("mysql_global_status_bytes_sent", 0.0);
            String activeDataTransferred = formatBytes(activeBytesSent);
            String standbyDataTransferred = formatBytes(standbyBytesSent);

            // DTO 구성
            SynchronizationMetricsDto dto = SynchronizationMetricsDto.builder()
                    .syncRate(syncRate)
                    .activeGtid(activeGtid)
                    .standbyGtid(standbyGtid)
                    .activeDataTransferred(activeDataTransferred)
                    .standbyDataTransferred(standbyDataTransferred)
                    .lastSyncTime(lastSyncTime)
                    .build();

            return dto;

        } catch (DataAccessException e) {
            // DB 연결 실패
            log.debug("Database connection failed during metrics build: {}", e.getMessage());
            return createDefaultMetrics();
        } catch (Exception e) {
            // 기타 예외
            log.debug("Unexpected error during metrics build: {}", e.getMessage());
            return createDefaultMetrics();
        }
    }

    /**
     * Standby GTID 조회 (예외 처리 포함)
     */
    private String fetchStandbyGtid() {
        try {
            return standbyJdbcTemplate.queryForObject(
                    "SELECT @@GLOBAL.GTID_EXECUTED", String.class);
        } catch (DataAccessException e) {
            log.debug("Failed to fetch Standby GTID: {}", e.getMessage());
            return "N/A";
        }
    }

    /**
     * 기본 메트릭 생성
     */
    private SynchronizationMetricsDto createDefaultMetrics() {
        return SynchronizationMetricsDto.builder()
                .syncRate(0.0)
                .activeGtid("N/A")
                .standbyGtid("N/A")
                .activeDataTransferred("0 B")
                .standbyDataTransferred("0 B")
                .lastSyncTime("N/A")
                .build();
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
            log.debug("No GTID record found in batchDB: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 바이트 크기 포맷팅
     */
    private String formatBytes(double bytes) {
        if (bytes < 1024) return String.format("%.0f B", bytes);
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024 * 1024));
        return String.format("%.2f GB", bytes / (1024 * 1024 * 1024));
    }
}
