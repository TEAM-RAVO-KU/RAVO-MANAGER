package org.ravo.ravomanager.manager.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Kubernetes Service Selector 상태
 */
@Getter
public class SelectorStatus {
    // 사람이 보기 편한 형식: "10/19 15:30"
    private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("MM/dd HH:mm");
    
    private final String currentTarget;      // "active" or "standby"
    
    @JsonIgnore  // JSON 직렬화에서 LocalDateTime 제외
    private final LocalDateTime switchedAt;

    /**
     * 새로운 상태 생성 (상태 변경 시 호출)
     */
    public SelectorStatus(String currentTarget) {
        this.currentTarget = currentTarget;
        this.switchedAt = LocalDateTime.now();
    }

    /**
     * 초기 상태 또는 에러 시 사용
     */
    public static SelectorStatus empty() {
        return new SelectorStatus("unknown");
    }

    /**
     * 마지막 전환 시간 포맷팅 (JSON 직렬화에 포함)
     * 예: "10/19 15:30"
     */
    public String getLastSwitchedFormatted() {
        if (switchedAt == null) {
            return "Unknown";
        }
        return switchedAt.format(DISPLAY_FORMATTER);
    }

    @Override
    public String toString() {
        return String.format("SelectorStatus{currentTarget='%s', switchedAt=%s}", 
                currentTarget, getLastSwitchedFormatted());
    }
}
