package org.ravo.client.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.ravo.client.domain.User;
import org.ravo.client.service.BankService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/bank/api")
public class BankApiController {

    private final BankService bankService;

    private User loginUser(HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        if (user == null) throw new IllegalStateException("로그인이 필요합니다.");
        return user;
    }

    @GetMapping("/balance")
    public ResponseEntity<?> balance(HttpSession session) {
        User user = loginUser(session);
        long balance = bankService.getBalance(user);
        String accountNo = bankService.getMyAccountNo(user);

        return ResponseEntity.ok(Map.of("ok", true, "balance", balance, "accountNo", accountNo));
    }

    @PostMapping("/deposit")
    public ResponseEntity<?> deposit(@RequestParam String requestId,
                                     @RequestParam long amount,
                                     HttpSession session) {
        User user = loginUser(session);
        bankService.deposit(user, amount, requestId);
        long balance = bankService.getBalance(user);

        return ResponseEntity.ok(Map.of("ok", true, "message", "입금이 완료되었습니다.", "balance", balance));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<?> withdraw(@RequestParam String requestId,
                                      @RequestParam long amount,
                                      HttpSession session) {
        User user = loginUser(session);
        boolean ok = bankService.withdraw(user, amount, requestId);
        if (!ok) return ResponseEntity.ok(Map.of("ok", false, "message", "잔액이 부족합니다."));
        long balance = bankService.getBalance(user);

        return ResponseEntity.ok(Map.of("ok", true, "message", "출금이 완료되었습니다.", "balance", balance));
    }

    @PostMapping("/transfer")
    public ResponseEntity<?> transfer(@RequestParam String requestId,
                                      @RequestParam String toAccountNo,
                                      @RequestParam long amount,
                                      HttpSession session) {
        User user = loginUser(session);
        boolean ok = bankService.transfer(user, toAccountNo, amount, requestId);
        if (!ok) return ResponseEntity.ok(Map.of("ok", false, "message", "잔액이 부족합니다."));
        long balance = bankService.getBalance(user);
        
        return ResponseEntity.ok(Map.of("ok", true, "message", "송금이 완료되었습니다.", "balance", balance));
    }
}
