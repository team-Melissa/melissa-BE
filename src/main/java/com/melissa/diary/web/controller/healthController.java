package com.melissa.diary.web.controller;

import com.melissa.diary.apiPayload.code.status.ErrorStatus;
import com.melissa.diary.apiPayload.exception.handler.ErrorHandler;
import com.melissa.diary.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class healthController {

    private final JwtProvider jwtProvider; // 주입(Autowired or RequiredArgsConstructor)


    @GetMapping("/health")
    public String checkHealth () {
        return "I'm healthy!";
    }

    @GetMapping("/jwt")
    public String checkHealthJWT () {
        // 이 시점에 SecurityContextHolder에 인증 정보가 들어있을 것
        return "JWT Clear! You are authenticated.";
    }
}
