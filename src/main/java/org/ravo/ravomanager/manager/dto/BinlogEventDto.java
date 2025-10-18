package org.ravo.ravomanager.manager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BinlogEventDto {
    private String database;         // "active" or "standby"
    private String eventType;        // "UPDATE", "INSERT", "DELETE"
    private String binlogPosition;   // "mysql-bin.000123:45678850"
    private String query;            // SQL 쿼리문
    private String timestamp;        // 이벤트 발생 시간
}
