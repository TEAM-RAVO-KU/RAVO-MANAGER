package org.ravo.client.bank.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.ravo.client.bank.domain.User;
import org.ravo.client.bank.service.BankService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/bank")
public class BankController {

    private final BankService bankService;

    /** 계좌 페이지 */
    @GetMapping
    public String accountPage(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loginUser");
        if (user == null) return "redirect:/login";

        long balance = bankService.getBalance(user);
        model.addAttribute("username", user.getName());
        model.addAttribute("balance", balance);
        return "client/bank"; // /templates/client/bank.html
    }

    /** 잔액 새로고침 */
    @GetMapping("/balance")
    public String refreshBalance(HttpSession session) {
        return "redirect:/bank";
    }

    /** 입금 */
    @PostMapping("/deposit")
    public String deposit(@RequestParam long amount, HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        if (user == null) return "redirect:/login";
        bankService.deposit(user, amount);
        return "redirect:/bank";
    }

    /** 출금 */
    @PostMapping("/withdraw")
    public String withdraw(@RequestParam long amount, HttpSession session, Model model) {
        User user = (User) session.getAttribute("loginUser");
        if (user == null) return "redirect:/login";

        boolean success = bankService.withdraw(user, amount);
        if (!success) {
            model.addAttribute("error", "잔액이 부족합니다.");
        }
        return "redirect:/bank";
    }
}
