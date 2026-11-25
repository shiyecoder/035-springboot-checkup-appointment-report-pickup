package com.example.healthexam.controller;

import com.example.healthexam.common.R;
import com.example.healthexam.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 认证与个人中心 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public R<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        return R.ok(authService.login(body.get("username"), body.get("password")));
    }

    @PostMapping("/register")
    public R<Map<String, Object>> register(@RequestBody Map<String, String> body) {
        return R.ok(authService.register(body));
    }

    @GetMapping("/me")
    public R<Map<String, Object>> me() {
        return R.ok(authService.profile());
    }

    @PutMapping("/profile")
    public R<Void> updateProfile(@RequestBody Map<String, String> body) {
        authService.updateProfile(body);
        return R.ok();
    }

    @PutMapping("/password")
    public R<Void> changePassword(@RequestBody Map<String, String> body) {
        authService.changePassword(body.get("oldPassword"), body.get("newPassword"));
        return R.ok();
    }
}
