package org.ravo.dashboard.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ravo.dashboard.dto.DashboardResponseDto;
import org.ravo.dashboard.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 데이터베이스 복제 모니터링 컨트롤러
 * Active/Standby DB의 동기화 상태 및 성능 지표를 모니터링하는 대시보드를 제공합니다.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class DashboardController {

    private static final String REPLICATION_MONITOR_VIEW = "/manager/dashboard";
    private static final String INITIAL_DATA_ATTRIBUTE = "initialData";

    private final DashboardService dashboardService;

    /**
     * 복제 모니터 대시보드 페이지를 반환합니다.
     * 
     * @param model 뷰에 전달할 데이터 모델
     * @return 복제 모니터 뷰 이름
     */
    @GetMapping("/dashboard")
    public String showMonitorPage(Model model) {
        log.info("복제 모니터 페이지 요청");
        
        DashboardResponseDto initialData = dashboardService.getDashboardData();
        model.addAttribute(INITIAL_DATA_ATTRIBUTE, initialData);
        
        log.debug("초기 데이터 로드 완료: {}", initialData);
        return REPLICATION_MONITOR_VIEW;
    }

    /**
     * 대시보드 실시간 갱신용 데이터 API
     * 
     * @return 현재 대시보드 데이터
     */
    @GetMapping("/api/dashboard")
    @ResponseBody
    public ResponseEntity<DashboardResponseDto> getDashboardData() {
        log.debug("대시보드 데이터 API 요청");
        
        DashboardResponseDto dashboardData = dashboardService.getDashboardData();
        return ResponseEntity.ok(dashboardData);
    }
}
