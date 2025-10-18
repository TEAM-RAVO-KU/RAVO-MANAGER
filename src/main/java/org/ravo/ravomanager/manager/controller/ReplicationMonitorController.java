package org.ravo.ravomanager.manager.controller;

import lombok.RequiredArgsConstructor;
import org.ravo.ravomanager.manager.dto.DashboardResponseDto;
import org.ravo.ravomanager.manager.service.ReplicationMonitorService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/replication")
public class ReplicationMonitorController {

    private final ReplicationMonitorService replicationMonitorService;
    
    public ReplicationMonitorController(ReplicationMonitorService replicationMonitorService) {
        this.replicationMonitorService = replicationMonitorService;
    }

    /**
     * 복제 모니터 대시보드 페이지
     */
    @GetMapping("/monitor")
    public String monitorPage(Model model) {
        System.out.println("=== Accessing /replication/monitor ===");
        // 초기 데이터 로드
        DashboardResponseDto initialData = replicationMonitorService.getDashboardData();
        model.addAttribute("initialData", initialData);
        System.out.println("=== Returning view: manager/replication-monitor ===");
        return "manager/replication-monitor";
    }

    /**
     * 대시보드 데이터 API (실시간 갱신용)
     */
    @GetMapping("/api/dashboard")
    @ResponseBody
    public ResponseEntity<DashboardResponseDto> getDashboardData() {
        DashboardResponseDto data = replicationMonitorService.getDashboardData();
        return ResponseEntity.ok(data);
    }
}
