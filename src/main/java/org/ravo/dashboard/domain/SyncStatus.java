package org.ravo.dashboard.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SyncStatus {
    private double syncPercent;
    private List<TableSyncInfo> tables;
    private int totalTables;
    private int syncedTables;
}