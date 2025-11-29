package org.ravo.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 읽기 활동(Read Activity) 데이터 DTO
 * SELECT 쿼리의 시계열 데이터를 담습니다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReadActivityDto {
    
    /**
     * Active DB의 읽기 작업 시계열 데이터
     */
    private List<DataPoint> activeDbReads;
    
    /**
     * Standby DB의 읽기 작업 시계열 데이터
     */
    private List<DataPoint> standbyDbReads;
    
    /**
     * 시계열 데이터 포인트
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataPoint {
        /**
         * 타임스탬프
         */
        private String timestamp;
        
        /**
         * 해당 시점의 읽기 작업 횟수 (초당 횟수)
         */
        private Integer count;
    }
}
