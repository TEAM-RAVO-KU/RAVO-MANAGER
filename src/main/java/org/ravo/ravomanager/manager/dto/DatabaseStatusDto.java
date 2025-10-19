package org.ravo.ravomanager.manager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatabaseStatusDto {
    private String name;              // "Active DB" or "Standby DB"
    private String status;            // "Active", "Standby", "Down"
    private String uptime;            // "1d 0h 0m"
    private Integer connections;      // 현재 연결 수
    private Double qps;               // Queries per second (소수점 포함)
    private String latency;           // "2.3ms"
    private String lastHeartbeat;     // 마지막 하트비트 시간
    private Boolean isHealthy;        // 헬스 체크 상태
}
