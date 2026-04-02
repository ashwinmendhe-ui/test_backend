package com.dji.sample.test;

import com.dji.sample.security.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class TestController {

    @GetMapping("/api/v1/test/me")
    public Map<String, Object> me(@AuthenticationPrincipal CustomUserDetails user) {
        return Map.of(
                "userId", user.getUserId(),
                "username", user.getUsername(),
                "email", user.getEmail()
        );
    }
}