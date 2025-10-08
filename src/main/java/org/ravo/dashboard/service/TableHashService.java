package org.ravo.dashboard.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ravo.dashboard.domain.SyncStatus;
import org.ravo.dashboard.domain.TableSyncInfo;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TableHashService {

    private final JdbcTemplate liveJdbcTemplate;

    @Qualifier("standbyJdbcTemplate")
    private final JdbcTemplate standbyJdbcTemplate;

    // 동기화 확인할 주요 테이블 목록 (추후 추가 가능)
    private static final List<String> MONITORED_TABLES = List.of(
            "users"
    );

    /**
     * Active와 Standby DB의 주요 테이블 동기화 상태를 계산합니다.
     */
    public SyncStatus calculateSyncStatus() {
        List<TableSyncInfo> tableInfos = new ArrayList<>();
        int syncedCount = 0;
        double totalSyncPercent = 0.0;

        for (String tableName : MONITORED_TABLES) {
            try {
                TableSyncInfo info = compareTableByRows(tableName);
                tableInfos.add(info);
                
                // 100% 동기화된 테이블만 카운트
                if (info.isSynced()) {
                    syncedCount++;
                }
                
                // 전체 평균 동기화율 계산에 사용
                totalSyncPercent += info.getSyncPercent();
                
            } catch (Exception e) {
                log.error("Error comparing table: {}", tableName, e);
                // 에러 발생 시 동기화 실패로 간주
                TableSyncInfo errorInfo = new TableSyncInfo();
                errorInfo.setTableName(tableName);
                errorInfo.setActiveHash("ERROR");
                errorInfo.setStandbyHash("ERROR");
                errorInfo.setSynced(false);
                errorInfo.setActiveCount(-1);
                errorInfo.setStandbyCount(-1);
                errorInfo.setSyncPercent(0.0);
                tableInfos.add(errorInfo);
            }
        }

        int totalTables = MONITORED_TABLES.size();
        // 전체 동기화율은 각 테이블의 동기화율 평균
        double avgSyncPercent = totalTables > 0 ? (totalSyncPercent / totalTables) : 100.0;
        avgSyncPercent = Math.round(avgSyncPercent * 100.0) / 100.0; // 소수점 둘째 자리

        return new SyncStatus(avgSyncPercent, tableInfos, totalTables, syncedCount);
    }

    /**
     * 특정 테이블의 Active와 Standby를 행 단위로 비교합니다.
     * 각 행의 해시를 비교하여 일치율을 계산합니다.
     */
    private TableSyncInfo compareTableByRows(String tableName) {
        // 행별 해시맵 생성 (key: primary key, value: 행 해시)
        Map<String, String> activeRowHashes = getRowHashes(liveJdbcTemplate, tableName);
        Map<String, String> standbyRowHashes = getRowHashes(standbyJdbcTemplate, tableName);
        
        long activeCount = activeRowHashes.size();
        long standbyCount = standbyRowHashes.size();
        
        // 전체 행 개수 (합집합)
        Set<String> allKeys = new HashSet<>(activeRowHashes.keySet());
        allKeys.addAll(standbyRowHashes.keySet());
        int totalRows = allKeys.size();
        
        // 일치하는 행 개수 계산
        int matchedRows = 0;
        for (String key : allKeys) {
            String activeHash = activeRowHashes.get(key);
            String standbyHash = standbyRowHashes.get(key);
            
            // 양쪽 모두 존재하고 해시가 같으면 일치
            if (activeHash != null && activeHash.equals(standbyHash)) {
                matchedRows++;
            }
        }
        
        // 동기화율 계산
        double syncPercent = totalRows > 0 ? (matchedRows * 100.0 / totalRows) : 100.0;
        syncPercent = Math.round(syncPercent * 100.0) / 100.0; // 소수점 둘째 자리
        
        // 완전 동기화 여부 (100%일 때만 true)
        boolean synced = (syncPercent == 100.0);
        
        // 전체 테이블 해시 (요약용)
        String activeTableHash = computeSHA256(activeRowHashes.toString()).substring(0, 16);
        String standbyTableHash = computeSHA256(standbyRowHashes.toString()).substring(0, 16);

        TableSyncInfo info = new TableSyncInfo();
        info.setTableName(tableName);
        info.setActiveHash(activeTableHash);
        info.setStandbyHash(standbyTableHash);
        info.setSynced(synced);
        info.setActiveCount(activeCount);
        info.setStandbyCount(standbyCount);
        info.setSyncPercent(syncPercent);

        log.debug("Table: {}, Sync: {}%, Matched: {}/{}, Active: {}, Standby: {}", 
                tableName, syncPercent, matchedRows, totalRows, activeCount, standbyCount);

        return info;
    }

    /**
     * 테이블의 각 행에 대한 해시맵을 반환합니다.
     * Map<PrimaryKey, RowHash>
     */
    private Map<String, String> getRowHashes(JdbcTemplate jdbcTemplate, String tableName) {
        try {
            String query = buildRowHashQuery(tableName);
            
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(query);
            Map<String, String> rowHashes = new HashMap<>();
            
            for (Map<String, Object> row : rows) {
                // 첫 번째 컬럼은 primary key
                String primaryKey = String.valueOf(row.values().iterator().next());

                // 모든 컬럼 값을 연결하여 행 해시 생성
                StringBuilder rowData = new StringBuilder();
                for (Object value : row.values()) {
                    rowData.append(value != null ? value.toString() : "NULL").append("|");
                }
                
                String rowHash = computeSHA256(rowData.toString());
                rowHashes.put(primaryKey, rowHash);
            }
            
            return rowHashes;
            
        } catch (Exception e) {
            log.error("Failed to get row hashes for table: {}", tableName, e);
            return new HashMap<>();
        }
    }

    /**
     * 테이블별로 행 해시를 계산하는 쿼리를 생성합니다.
     */
    private String buildRowHashQuery(String tableName) {
        return switch (tableName.toLowerCase()) {
            case "users" -> 
                "SELECT id, user_id, password, name, balance FROM users ORDER BY id";
            default -> 
                "SELECT * FROM " + tableName + " ORDER BY id";
        };
    }

    /**
     * SHA-256 해시 계산
     */
    private String computeSHA256(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(data.getBytes());
            
            // 바이트 배열을 16진수 문자열로 변환
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not found", e);
            return "ERROR";
        }
    }
}
