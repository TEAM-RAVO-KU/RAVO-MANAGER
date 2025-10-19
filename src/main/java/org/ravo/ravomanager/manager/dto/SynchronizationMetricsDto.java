package org.ravo.ravomanager.manager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SynchronizationMetricsDto {
    private Double syncRate;              // 99.80 (퍼센트)
    private String activeDataTransferred;       // "2.40 GB"
    private String standbyDataTransferred;       // "2.40 GB"
    private String activeGtid;  // "mysql-bin.000123:45678901"
    private String standbyGtid; // "mysql-bin.000123:45678899"
    private String lastSyncTime;          // 마지막 동기화 시간
}
