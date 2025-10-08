package org.ravo.client.dashboard.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TableSyncInfo {
    private String tableName;
    private String activeHash;
    private String standbyHash;
    private boolean synced;
    private long activeCount;
    private long standbyCount;
    private double syncPercent;  // 동기화 비율 (0.00 ~ 100.00)
}
