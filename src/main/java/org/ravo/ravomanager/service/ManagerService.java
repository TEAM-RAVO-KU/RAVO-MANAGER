package org.ravo.ravomanager.service;

import lombok.extern.slf4j.Slf4j;
import org.ravo.ravomanager.data.DefaultData;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
public class ManagerService {

    private final DefaultData defaultData;
    private final JdbcTemplate liveJdbcTemplate;
    private final JdbcTemplate standbyJdbcTemplate;

    public ManagerService(@Qualifier("liveJdbcTemplate") JdbcTemplate liveJdbcTemplate,
                          @Qualifier("standbyJdbcTemplate") JdbcTemplate standbyJdbcTemplate) {
        this.liveJdbcTemplate = liveJdbcTemplate;
        this.standbyJdbcTemplate = standbyJdbcTemplate;
        this.defaultData = new DefaultData();
    }

    public boolean isDbAlive(JdbcTemplate jdbcTemplate) {
        try {
            jdbcTemplate.execute("SELECT 1");
            return true;
        } catch (Exception e) {
            log.warn("[isDbAlive] DB connection failed: {}", e.getMessage());
            return false;
        }
    }

    public Map<String, Object> fetchLatestData(JdbcTemplate jdbcTemplate) {
        try {
            List<Map<String, Object>> results = jdbcTemplate.queryForList(
                    "SELECT * FROM integrity_data ORDER BY checked_at DESC LIMIT 1");
            return results.isEmpty() ? defaultData.defaultFetchData("(No Data)") : results.get(0);
        } catch (Exception e) {
            log.warn("[fetchLatestDataSafe] DB Error: {}", e.getMessage());
            return defaultData.defaultFetchData("(DB Error)");
        }
    }

    private LocalDateTime convertToLocalDateTime(Object obj) {
        if (obj instanceof Timestamp ts) {
            return ts.toLocalDateTime();
        } else if (obj instanceof LocalDateTime ldt) {
            return ldt;
        } else if (obj instanceof String str) {
            return LocalDateTime.parse(str, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } else {
            log.warn("Unknown type: {}", obj.getClass().getName());
            return null;
        }
    }

    public Map<String, Boolean> checkIntegrity(Map<String, Object> data) {
        Map<String, Boolean> integrityResult = new HashMap<>();

        // 데이터 값 검사 (NULL/공백여부)
        String value = (String) data.get("data");
        integrityResult.put("dataExists", value != null && !value.isBlank());

        // ID 검사 (ID는 양수여야 함)
        Object idObj = data.get("id");
        boolean idValid = idObj instanceof Number && ((Number) idObj).longValue() > 0;
        integrityResult.put("idValid", idValid);

        integrityResult.put("overall", integrityResult.values().stream().allMatch(Boolean::booleanValue));

        return integrityResult;
    }


    public Map<String, Boolean> checkConsistency(Map<String, Object> liveData, Map<String, Object> standbyData) {
        Map<String, Boolean> consistencyResult = new HashMap<>();

        // ID 일치 검사
        boolean idMatch = Objects.equals(liveData.get("id"), standbyData.get("id"));
        consistencyResult.put("idConsistent", idMatch);

        // 데이터 값 일치 검사
        boolean dataMatch = Objects.equals(liveData.get("data"), standbyData.get("data"));
        consistencyResult.put("dataConsistent", dataMatch);

        // checked_at 일치 검사 (1분 이내 허용 오차)
        Object liveCheckedAt = liveData.get("checked_at");
        Object standbyCheckedAt = standbyData.get("checked_at");
        boolean dateConsistent = false;

        if (liveCheckedAt != null && standbyCheckedAt != null) {
            LocalDateTime liveTime = convertToLocalDateTime(liveCheckedAt);
            LocalDateTime standbyTime = convertToLocalDateTime(standbyCheckedAt);

            if (liveTime != null && standbyTime != null) {
                long diffSeconds = Math.abs(Duration.between(liveTime, standbyTime).toSeconds());
                dateConsistent = diffSeconds <= 60; // 최대 60초 허용
            } else {
                log.warn("[ManagerService]-[checkConsistency] Time conversion failed.");
            }
        }

        consistencyResult.put("checkedAtConsistent", dateConsistent);
        consistencyResult.put("overall", consistencyResult.values().stream().allMatch(Boolean::booleanValue));

        return consistencyResult;
    }


    public Map<String, Object> getAllStatuses() {
        Map<String, Object> statuses = new HashMap<>();

        boolean liveDbAlive = isDbAlive(liveJdbcTemplate);
        boolean standbyDbAlive = isDbAlive(standbyJdbcTemplate);

        statuses.put("liveDbStatus", liveDbAlive);
        statuses.put("standbyDbStatus", standbyDbAlive);

        Map<String, Object> liveData = liveDbAlive ?
                fetchLatestData(liveJdbcTemplate) : defaultData.defaultFetchData("N/A");
        Map<String, Object> standbyData = standbyDbAlive ?
                fetchLatestData(standbyJdbcTemplate) : defaultData.defaultFetchData("N/A");

        statuses.put("liveData", liveData);
        statuses.put("standbyData", standbyData);

        statuses.put("liveIntegrity", liveDbAlive ?
                checkIntegrity(liveData) : defaultData.defaultIntegrityResult());
        statuses.put("standbyIntegrity", standbyDbAlive ?
                checkIntegrity(standbyData) : defaultData.defaultIntegrityResult());

        statuses.put("consistency", (liveDbAlive && standbyDbAlive) ?
                checkConsistency(liveData, standbyData) : defaultData.defaultConsistencyResult());

        return statuses;
    }
}
