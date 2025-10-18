package org.ravo.ravomanager.manager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReplicationActivityDto {
    private List<DataPoint> activeDbWrites;   // Active DB 쓰기 작업 타임라인
    private List<DataPoint> standbyDbWrites;  // Standby DB 쓰기 작업 타임라인
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataPoint {
        private String timestamp;  // 시간
        private Integer count;     // 쓰기 횟수
    }
}
