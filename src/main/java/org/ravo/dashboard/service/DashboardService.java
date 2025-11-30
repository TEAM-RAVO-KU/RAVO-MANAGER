package org.ravo.dashboard.service;

import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.ravo.dashboard.dto.BinlogEventDto;
import org.ravo.dashboard.dto.DashboardResponseDto;
import org.ravo.dashboard.dto.DatabaseStatusDto;
import org.ravo.dashboard.dto.ReadActivityDto;
import org.ravo.dashboard.dto.SelectorStatus;
import org.ravo.dashboard.dto.SynchronizationMetricsDto;
import org.ravo.dashboard.dto.SystemEventDto;
import org.ravo.dashboard.dto.WriteActivityDto;
import org.ravo.ravomanager.manager.monitoring.MetricData;
import org.ravo.ravomanager.manager.service.MonitoringService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);

    private final MonitoringService monitoringService;
    private final KubernetesStatusService k8sStatusService;
    private final SynchronizationMetricsService synchronizationMetricsService;
    private final SystemEventService systemEventService;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("오전 h:mm:ss");
    
    // 시계열 데이터 저장 (최근 30개 데이터 포인트)
    private static final int MAX_DATA_POINTS = 30;
    private final List<WriteActivitySnapshot> activeWriteHistory = new ArrayList<>();
    private final List<WriteActivitySnapshot> standbyWriteHistory = new ArrayList<>();
    private final List<ReadActivitySnapshot> activeReadHistory = new ArrayList<>();
    private final List<ReadActivitySnapshot> standbyReadHistory = new ArrayList<>();
    
    /**
     * 대시보드 전체 데이터를 조회합니다.
     * Active/Standby DB 상태, 동기화 메트릭, 복제 활동 등 모든 대시보드 데이터를 포함합니다.
     * 
     * @return 대시보드 전체 데이터
     */
    public DashboardResponseDto getDashboardData() {
        try {
            // 1. DB 메트릭 조회 (동기 방식으로 변환)
            Map<String, MetricData> metrics = monitoringService.fetchMetrics()
                    .timeout(Duration.ofSeconds(10))
                    .block();
            
            if (metrics == null || metrics.isEmpty()) {
                log.error("Metrics data is null or empty");
                return createFallbackResponse();
            }
            // 2. K8s 상태 조회 (동기 방식)
            SelectorStatus selectorStatus = k8sStatusService.fetchStatus();
            log.info("Selector status: {}", selectorStatus.getCurrentTarget());
            if (selectorStatus == null) {
                selectorStatus = SelectorStatus.empty();
            }

            // 3. 동기화율 계산
            double syncRate = 0.0;
            SynchronizationMetricsDto synchronizationMetricsDto = synchronizationMetricsService.buildSyncMetrics(
                    metrics, syncRate);

            return DashboardResponseDto.builder()
                    .activeDb(buildDatabaseStatusFrom(metrics.get("active"), "Active DB", "Active"))
                    .standbyDb(buildDatabaseStatusFrom(metrics.get("standby"), "Standby DB", "Standby"))
                    .syncMetrics(synchronizationMetricsDto)
                    .selectorStatus(selectorStatus)
                    .writeActivity(getWriteActivity(metrics))
                    .readActivity(getReadActivity(metrics))
                    .recentBinlogEvents(getRecentBinlogEvents())
                    .systemEvents(systemEventService.getRecentSystemEvents())
                    .isConnected(true)
                    .build();

        } catch (Exception e) {
            log.error("Fatal error in getDashboardData", e);
            // 에러 발생 시 기본값 반환
            return createFallbackResponse();
        }
    }

    /**
     * MetricData를 DatabaseStatusDto로 변환합니다.
     * 
     * @param metricData Prometheus 메트릭 데이터
     * @param name DB 이름
     * @param status DB 상태
     * @return 변환된 데이터베이스 상태 DTO
     */
    private DatabaseStatusDto buildDatabaseStatusFrom(MetricData metricData, String name, String status) {
        if (metricData == null || metricData.getStatus() == MetricData.DatabaseStatus.DOWN) {
            return DatabaseStatusDto.builder()
                    .name(name)
                    .status("Down")
                    .uptime("N/A")
                    .connections(0)
                    .qps(0.0)
                    .latency("N/A")
                    .lastHeartbeat(getCurrentTimestamp())
                    .isHealthy(false)
                    .build();
        }

        Map<String, Double> metrics = metricData.getMetrics();
        
        // Uptime 계산 (초 -> "Xd Xh Xm" 형식)
        Double uptimeSeconds = metrics.getOrDefault("mysql_global_status_uptime", 0.0);
        String uptime = formatUptime(uptimeSeconds.longValue());
        
        // Connections
        Integer connections = metrics.getOrDefault("mysql_global_status_threads_connected", 0.0).intValue();
        
        // QPS (Queries per second) - 소수점 2자리까지
        Double queries = metrics.getOrDefault("mysql_global_status_queries", 0.0);
        Double qps = queries / Math.max(1, uptimeSeconds);
        
        // Latency 계산 - InnoDB row lock 평균 대기 시간 기반
        String latency;
        Double innodbRowLockTime = metrics.getOrDefault("mysql_global_status_innodb_row_lock_time", 0.0);
        Double innodbRowLockWaits = metrics.getOrDefault("mysql_global_status_innodb_row_lock_waits", 0.0);
        
        if (innodbRowLockWaits > 0 && innodbRowLockTime > 0) {
            // Row lock 평균 대기 시간 (밀리초로 변환)
            double avgLockTimeMs = innodbRowLockTime / innodbRowLockWaits;
            latency = String.format("%.2fms", avgLockTimeMs);
        } else {
            // Lock time이 없으면 간단한 추정
            // Queries per second가 높을수록 latency는 낮음
            if (qps > 1000) {
                latency = "< 1ms";
            } else if (qps > 100) {
                latency = String.format("%.1fms", 1000.0 / qps);
            } else {
                latency = "N/A";
            }
        }
        
        return DatabaseStatusDto.builder()
                .name(name)
                .status(status)
                .uptime(uptime)
                .connections(connections)
                .qps(qps)
                .latency(latency)
                .lastHeartbeat(getCurrentTimestamp())
                .isHealthy(true)
                .build();
    }

    /**
     * 읽기 활동 타임라인 조회 (SELECT 쿼리 기반)
     */
    private ReadActivityDto getReadActivity(Map<String, MetricData> metrics) {
        try {
            MetricData activeData = metrics.get("active");
            MetricData standbyData = metrics.get("standby");
            
            if (activeData == null || standbyData == null) {
                log.warn("Active or Standby data is null, returning empty read activity");
                return createDefaultReadActivity();
            }
            
            // 현재 SELECT 쿼리 횟수 계산
            int activeReads = calculateReadQueries(activeData);
            int standbyReads = calculateReadQueries(standbyData);
            
            // 현재 스냅샷 추가
            String currentTime = getCurrentTimestamp();
            addReadSnapshot(activeReadHistory, new ReadActivitySnapshot(currentTime, activeReads));
            addReadSnapshot(standbyReadHistory, new ReadActivitySnapshot(currentTime, standbyReads));
            
            // DataPoint 리스트로 변환
            List<ReadActivityDto.DataPoint> activePoints = convertToReadDataPoints(activeReadHistory);
            List<ReadActivityDto.DataPoint> standbyPoints = convertToReadDataPoints(standbyReadHistory);
            
            log.debug("Read activity data - Active: {} reads, Standby: {} reads (history size: {})", 
                    activeReads, standbyReads, activeReadHistory.size());
            
            return ReadActivityDto.builder()
                    .activeDbReads(activePoints)
                    .standbyDbReads(standbyPoints)
                    .build();
            
        } catch (Exception e) {
            log.error("Error creating read activity", e);
            return createDefaultReadActivity();
        }
    }
    
    /**
     * 읽기 쿼리 총 횟수 계산 (SELECT)
     */
    private int calculateReadQueries(MetricData metricData) {
        Map<String, Double> metrics = metricData.getMetrics();
        
        double selects = metrics.getOrDefault("mysql_global_status_commands_total_select", 0.0);
        
        log.debug("Read queries - Selects: {}", (int) selects);
        
        return (int) selects;
    }
    
    /**
     * 히스토리에 READ 스냅샷 추가 (최대 30개 유지)
     */
    private void addReadSnapshot(List<ReadActivitySnapshot> history, ReadActivitySnapshot snapshot) {
        history.add(snapshot);
        if (history.size() > MAX_DATA_POINTS) {
            history.remove(0); // 가장 오래된 데이터 제거
        }
    }
    
    /**
     * ReadActivitySnapshot을 DataPoint로 변환
     */
    private List<ReadActivityDto.DataPoint> convertToReadDataPoints(List<ReadActivitySnapshot> history) {
        List<ReadActivityDto.DataPoint> dataPoints = new ArrayList<>();
        
        // 변화율 계산 (이전 값과의 차이)
        for (int i = 0; i < history.size(); i++) {
            ReadActivitySnapshot current = history.get(i);
            int rate;
            
            if (i == 0) {
                // 첫 번째 데이터는 0으로 시작
                rate = 0;
            } else {
                ReadActivitySnapshot previous = history.get(i - 1);
                // 이전 값과의 차이 (초당 읽기 횟수)
                rate = Math.max(0, current.totalReads - previous.totalReads);
            }
            
            dataPoints.add(ReadActivityDto.DataPoint.builder()
                    .timestamp(current.timestamp)
                    .count(rate)
                    .build());
        }
        
        return dataPoints;
    }
    
    /**
     * 읽기 활동 스냅샷 클래스
     */
    private static class ReadActivitySnapshot {
        final String timestamp;
        final int totalReads;
        
        ReadActivitySnapshot(String timestamp, int totalReads) {
            this.timestamp = timestamp;
            this.totalReads = totalReads;
        }
    }

    /**
     * 복제 활동 타임라인 조회 (실제 쓰기 쿼리 기반)
     */
    private WriteActivityDto getWriteActivity(Map<String, MetricData> metrics) {
        try {
            MetricData activeData = metrics.get("active");
            MetricData standbyData = metrics.get("standby");
            
            if (activeData == null || standbyData == null) {
                log.warn("Active or Standby data is null, returning empty activity");
                return createDefaultActivity();
            }
            
            // 현재 쓰기 쿼리 총합 계산 (INSERT + UPDATE + DELETE)
            int activeWrites = calculateWriteQueries(activeData);
            int standbyWrites = calculateWriteQueries(standbyData);
            
            // 현재 스냅샷 추가
            String currentTime = getCurrentTimestamp();
            addSnapshot(activeWriteHistory, new WriteActivitySnapshot(currentTime, activeWrites));
            addSnapshot(standbyWriteHistory, new WriteActivitySnapshot(currentTime, standbyWrites));
            
            // DataPoint 리스트로 변환
            List<WriteActivityDto.DataPoint> activePoints = convertToDataPoints(activeWriteHistory);
            List<WriteActivityDto.DataPoint> standbyPoints = convertToDataPoints(standbyWriteHistory);
            
            log.debug("Activity data - Active: {} writes, Standby: {} writes (history size: {})", 
                    activeWrites, standbyWrites, activeWriteHistory.size());
            
            return WriteActivityDto.builder()
                    .activeDbWrites(activePoints)
                    .standbyDbWrites(standbyPoints)
                    .build();
            
        } catch (Exception e) {
            log.error("Error creating replication activity", e);
            return createDefaultActivity();
        }
    }
    
    /**
     * 쓰기 쿼리 총 횟수 계산 (INSERT + UPDATE + DELETE)
     */
    private int calculateWriteQueries(MetricData metricData) {
        Map<String, Double> metrics = metricData.getMetrics();
        
        // 메트릭 키 확인을 위한 로그
        log.debug("Available metric keys: {}", metrics.keySet());
        
        double inserts = metrics.getOrDefault("mysql_global_status_commands_total_insert", 0.0);
        double updates = metrics.getOrDefault("mysql_global_status_commands_total_update", 0.0);
        double deletes = metrics.getOrDefault("mysql_global_status_commands_total_delete", 0.0);

        return (int) (inserts + updates + deletes);
    }
    
    /**
     * 히스토리에 스냅샷 추가 (최대 30개 유지)
     */
    private void addSnapshot(List<WriteActivitySnapshot> history, WriteActivitySnapshot snapshot) {
        history.add(snapshot);
        if (history.size() > MAX_DATA_POINTS) {
            history.remove(0); // 가장 오래된 데이터 제거
        }
    }
    
    /**
     * WriteActivitySnapshot을 DataPoint로 변환
     */
    private List<WriteActivityDto.DataPoint> convertToDataPoints(List<WriteActivitySnapshot> history) {
        List<WriteActivityDto.DataPoint> dataPoints = new ArrayList<>();
        
        // 변화율 계산 (이전 값과의 차이)
        for (int i = 0; i < history.size(); i++) {
            WriteActivitySnapshot current = history.get(i);
            int rate;
            
            if (i == 0) {
                // 첫 번째 데이터는 0으로 시작
                rate = 0;
            } else {
                WriteActivitySnapshot previous = history.get(i - 1);
                // 이전 값과의 차이 (초당 쓰기 횟수)
                rate = Math.max(0, current.totalWrites - previous.totalWrites);
            }
            
            dataPoints.add(WriteActivityDto.DataPoint.builder()
                    .timestamp(current.timestamp)
                    .count(rate)
                    .build());
        }
        
        return dataPoints;
    }
    
    /**
     * 쓰기 활동 스냅샷 클래스
     */
    private static class WriteActivitySnapshot {
        final String timestamp;
        final int totalWrites;
        
        WriteActivitySnapshot(String timestamp, int totalWrites) {
            this.timestamp = timestamp;
            this.totalWrites = totalWrites;
        }
    }

    /**
     * 최근 Binlog 이벤트 조회
     * TODO: 실제 bi
     * nlog 파싱 구현
     */
    private List<BinlogEventDto> getRecentBinlogEvents() {
        List<BinlogEventDto> events = new ArrayList<>();
        
        events.add(BinlogEventDto.builder()
                .database("active")
                .eventType("UPDATE")
                .binlogPosition("mysql-bin.000123:45678850")
                .query("UPDATE orders SET status = \"completed\" WHERE id = 5432")
                .timestamp(getCurrentTimestamp())
                .build());
        
        events.add(BinlogEventDto.builder()
                .database("standby")
                .eventType("UPDATE")
                .binlogPosition("mysql-bin.000123:45678850")
                .query("UPDATE orders SET status = \"completed\" WHERE id = 5432")
                .timestamp(getCurrentTimestamp())
                .build());
        
        events.add(BinlogEventDto.builder()
                .database("active")
                .eventType("DELETE")
                .binlogPosition("mysql-bin.000123:45678800")
                .query("DELETE FROM sessions WHERE expired_at < NOW()")
                .timestamp(getCurrentTimestamp())
                .build());
        
        return events;
    }


    /**
     * 에러 시 반환할 기본 응답
     */
    private DashboardResponseDto createFallbackResponse() {
        return DashboardResponseDto.builder()
                .activeDb(createDefaultDbStatus("Active DB", "Unknown"))
                .standbyDb(createDefaultDbStatus("Standby DB", "Unknown"))
                .syncMetrics(createDefaultSyncMetrics())
                .selectorStatus(SelectorStatus.empty())
                .writeActivity(createDefaultActivity())
                .readActivity(createDefaultReadActivity())
                .recentBinlogEvents(new ArrayList<>())
                .systemEvents(new ArrayList<>())
                .isConnected(false)
                .build();
    }

    private DatabaseStatusDto createDefaultDbStatus(String name, String status) {
        return DatabaseStatusDto.builder()
                .name(name)
                .status(status)
                .uptime("N/A")
                .connections(0)
                .qps(0.0)
                .latency("N/A")
                .lastHeartbeat(getCurrentTimestamp())
                .isHealthy(false)
                .build();
    }

    private SynchronizationMetricsDto createDefaultSyncMetrics() {
        return SynchronizationMetricsDto.builder()
                .syncRate(0.0)
                .activeDataTransferred("N/A")
                .standbyDataTransferred("N/A")
                .activeGtid("N/A")
                .standbyGtid("N/A")
                .lastSyncTime(getCurrentTimestamp())
                .build();
    }

    private WriteActivityDto createDefaultActivity() {
        List<WriteActivityDto.DataPoint> emptyList = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            emptyList.add(WriteActivityDto.DataPoint.builder()
                    .timestamp(getCurrentTimestamp())
                    .count(0)
                    .build());
        }
        
        return WriteActivityDto.builder()
                .activeDbWrites(new ArrayList<>(emptyList))
                .standbyDbWrites(new ArrayList<>(emptyList))
                .build();
    }

    private ReadActivityDto createDefaultReadActivity() {
        List<ReadActivityDto.DataPoint> emptyList = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            emptyList.add(ReadActivityDto.DataPoint.builder()
                    .timestamp(getCurrentTimestamp())
                    .count(0)
                    .build());
        }
        
        return ReadActivityDto.builder()
                .activeDbReads(new ArrayList<>(emptyList))
                .standbyDbReads(new ArrayList<>(emptyList))
                .build();
    }

    /**
     * Uptime 포맷팅 (초 -> "Xd Xh Xm Xs")
     */
    private String formatUptime(long seconds) {
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return String.format("%dd %dh %dm %ds", days, hours, minutes, secs);
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

    /**
     * 현재 한국시간 기준 타임스탬프 반환
     */

    private String getCurrentTimestamp() {
        DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("MM/dd HH:mm:ss");
        ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");
        return LocalDateTime.now(KST_ZONE).format(TIME_FORMATTER);
    }
}
