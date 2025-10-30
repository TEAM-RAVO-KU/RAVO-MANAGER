package org.ravo.client.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.ravo.client.domain.User;
import org.ravo.client.service.DbFailoverControlService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/bank")
public class BankController {

    private final DbFailoverControlService dbControlService;

    /** 계좌 페이지 */
    @GetMapping
    public String accountPage(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loginUser");
        if (user == null) return "redirect:/login";

        model.addAttribute("username", user.getName());
        return "client/bank"; // /templates/client/bank.html
    }

    /** Active DB Down */
    @PostMapping("/active-db/down")
    @ResponseBody
    public ResponseEntity<?> activeDbDown() {
        dbControlService.activeDbDown();
        return ResponseEntity.ok(Map.of("ok", true, "message", "Active DB를 down 시뮬레이션했습니다."));
    }

    /** Active DB Up */
    @PostMapping("/active-db/up")
    @ResponseBody
    public ResponseEntity<?> activeDbUp() {
        dbControlService.activeDbUp();
        return ResponseEntity.ok(Map.of("ok", true, "message", "Active DB를 up(복구) 시뮬레이션했습니다."));
    }

    /** failover watcher 엔드포인트 **/
    @GetMapping("/failover/status")
    @ResponseBody
    public ResponseEntity<?> getFailoverStatus() {
        String watcher = dbControlService.getWatcherState(); // active | standby | unknown
        return ResponseEntity.ok(Map.of("ok", true, "watcher", watcher));
    }
}
