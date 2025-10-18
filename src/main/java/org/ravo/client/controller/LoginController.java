package org.ravo.client.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.ravo.client.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class LoginController {

    private final UserService userService;

    /** 로그인 페이지 */
    @GetMapping("/login")
    public String loginPage() {
        return "client/login";
    }

    /** 로그인 처리 */
    @PostMapping("/login")
    public String login(@RequestParam String userId,
                        @RequestParam String password,
                        HttpSession session,
                        Model model) {

        return userService.login(userId, password)
                .map(user -> {
                    session.setAttribute("loginUser", user);
                    return "redirect:/bank";
                })
                .orElseGet(() -> {
                    model.addAttribute("error", "아이디 또는 비밀번호가 올바르지 않습니다.");
                    return "client/login";
                });
    }

    /** 회원가입 페이지 */
    @GetMapping("/register")
    public String registerPage() {
        return "client/register"; // register.html
    }

    /** 회원가입 처리 */
    @PostMapping("/register")
    public String register(@RequestParam String userId,
                           @RequestParam String password,
                           @RequestParam String name,
                           Model model) {

        boolean success = userService.register(userId, password, name);
        if (success) {
            model.addAttribute("message", "회원가입이 완료되었습니다. 로그인 해주세요.");
            return "client/login";
        } else {
            model.addAttribute("error", "이미 존재하는 아이디입니다.");
            return "client/register";
        }
    }

    /** 로그아웃 */
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }


}
