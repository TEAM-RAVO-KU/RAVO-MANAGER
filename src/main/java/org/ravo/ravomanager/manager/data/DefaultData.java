package org.ravo.ravomanager.manager.data;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class DefaultData {

    public Map<String, Object> defaultFetchData(String message) {
        Map<String, Object> defaultMap = new HashMap<>();
        defaultMap.put("id", -1);
        defaultMap.put("data", message);
        defaultMap.put("checked_at", null);
        return defaultMap;
    }

    public Map<String, Boolean> defaultIntegrityResult() {
        Map<String, Boolean> integrityMap = new HashMap<>();
        integrityMap.put("dataExists", false);
        integrityMap.put("idValid", false);
        integrityMap.put("dateValid", false);
        integrityMap.put("overall", false);
        return integrityMap;
    }

    public Map<String, Boolean> defaultConsistencyResult() {
        Map<String, Boolean> consistencyMap = new HashMap<>();
        consistencyMap.put("idConsistent", false);
        consistencyMap.put("dataConsistent", false);
        consistencyMap.put("checkedAtConsistent", false);
        consistencyMap.put("overall", false);
        return consistencyMap;
    }
}

