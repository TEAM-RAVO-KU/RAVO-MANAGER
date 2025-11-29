package org.ravo.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemEventDto {
    private String eventType;     // "sync", "connection", "performance", "recovery", "error"
    private String severity;      // "success", "warning", "error", "info"
    private String title;         // "Synchronization completed successfully"
    private String description;   // 상세 설명
    private String timestamp;     // 이벤트 발생 시간
    private String details;       // 추가 정보 (예: "Binlog position: mysql-bin.000123:45678901")
}
