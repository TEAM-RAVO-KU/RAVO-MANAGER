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
public class DashboardResponseDto {
    private DatabaseStatusDto activeDb;
    private DatabaseStatusDto standbyDb;
    private SynchronizationMetricsDto syncMetrics;
    private SelectorStatus selectorStatus;
    private WriteActivityDto writeActivity;
    private ReadActivityDto readActivity;
    private List<BinlogEventDto> recentBinlogEvents;  // 최근 binlog 이벤트 (최대 10개)
    private List<SystemEventDto> systemEvents;         // 최근 시스템 이벤트 (최대 20개)
    private Boolean isConnected;                       // 전체 시스템 연결 상태
}
