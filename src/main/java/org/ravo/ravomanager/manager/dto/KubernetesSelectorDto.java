package org.ravo.ravomanager.manager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KubernetesSelectorDto {
    private String currentTarget;     // "active" or "standby"
    private String targetEndpoint;    // DB endpoint
    private String switchedAt;        // 마지막 전환 시간
    private Boolean isAutoFailover;   // 자동 failover 활성화 여부
}
