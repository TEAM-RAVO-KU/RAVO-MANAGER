package org.ravo.ravomanager.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
public class ManagerService {

    private final JdbcTemplate liveJdbcTemplate;
    private final JdbcTemplate standbyJdbcTemplate;

    public ManagerService(@Qualifier("liveJdbcTemplate") JdbcTemplate liveJdbcTemplate,
                          @Qualifier("standbyJdbcTemplate") JdbcTemplate standbyJdbcTemplate) {
        this.liveJdbcTemplate = liveJdbcTemplate;
        this.standbyJdbcTemplate = standbyJdbcTemplate;
    }

    public Map<String, Object> fetchLatestData(JdbcTemplate jdbcTemplate) {
        return jdbcTemplate.queryForMap(
                "SELECT * FROM integrity_data ORDER BY checked_at DESC LIMIT 1");
    }

    public boolean checkIntegrity(Map<String, Object> data) {
        String value = (String) data.get("data");
        return value != null && !value.isBlank();
    }

    public boolean checkConsistency() {
        Map<String, Object> liveData = fetchLatestData(liveJdbcTemplate);
        Map<String, Object> standbyData = fetchLatestData(standbyJdbcTemplate);
        return Objects.equals(liveData.get("data"), standbyData.get("data"));
    }

    public Map<String, Object> getAllStatuses() {
        Map<String, Object> statuses = new HashMap<>();
        statuses.put("liveData", fetchLatestData(liveJdbcTemplate));
        statuses.put("standbyData", fetchLatestData(standbyJdbcTemplate));
        statuses.put("liveIntegrity", checkIntegrity(fetchLatestData(liveJdbcTemplate)));
        statuses.put("standbyIntegrity", checkIntegrity(fetchLatestData(standbyJdbcTemplate)));
        statuses.put("consistency", checkConsistency());
        return statuses;
    }
}
