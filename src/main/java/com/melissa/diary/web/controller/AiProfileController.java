package com.melissa.diary.web.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "AiProfileAPI", description = "AI 프로필 관련 API")
@RequestMapping("/api/v1/ai-profiles")
@RequiredArgsConstructor
public class AiProfileController {


}
