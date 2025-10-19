package org.ravo.ravomanager.manager.monitoring;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 기본 메트릭 데이터를 제공하는 클래스
 */
@Component
public class DefaultMetricDataProvider {

    /**
     * 기본 조회 데이터 생성
     */
    public Map<String, Object> createDefaultFetchData(String message) {
        Map<String, Object> defaultData = new HashMap<>();
        defaultData.put("id", -1);
        defaultData.put("data", message);
        defaultData.put("checked_at", null);
        return defaultData;
    }

    /**
     * 기본 무결성 검증 결과 생성
     */
    public Map<String, Boolean> createDefaultIntegrityResult() {
        Map<String, Boolean> integrityResult = new HashMap<>();
        integrityResult.put("dataExists", false);
        integrityResult.put("idValid", false);
        integrityResult.put("dateValid", false);
        integrityResult.put("overall", false);
        return integrityResult;
    }

    /**
     * 기본 일관성 검증 결과 생성
     */
    public Map<String, Boolean> createDefaultConsistencyResult() {
        Map<String, Boolean> consistencyResult = new HashMap<>();
        consistencyResult.put("idConsistent", false);
        consistencyResult.put("dataConsistent", false);
        consistencyResult.put("checkedAtConsistent", false);
        consistencyResult.put("overall", false);
        return consistencyResult;
    }
}
