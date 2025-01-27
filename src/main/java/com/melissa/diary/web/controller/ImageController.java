package com.melissa.diary.web.controller;

import com.melissa.diary.ai.ImageGenerator;
import com.melissa.diary.apiPayload.ApiResponse;
import com.melissa.diary.aws.s3.AmazonS3Manager;
import com.melissa.diary.domain.Uuid;
import com.melissa.diary.repository.UuidRepository;
import com.melissa.diary.web.dto.UserSettingResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Base64;
import java.util.UUID;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class ImageController {

    private final ImageGenerator imageGenerator;
    private final AmazonS3Manager amazonS3Manager;
    private final UuidRepository uuidRepository;

    @Operation(summary = "Test : Base64 AI 이미지 받아서 S3에 업로드")
    @GetMapping("/image-upload")
    public ApiResponse<String> getAiImageB64Upload() {

        // ImageGenerator에서 prompt로 Base64 응답 받기 (b64_json)
        String prompt = "활발한 아기새 프로필 만화툰";
        String base64Img = imageGenerator.generateB64(prompt);

        // keyName 생성 (ex. "profile/1679999999999.png")
        String uuid = UUID.randomUUID().toString();
        Uuid savedUuid = uuidRepository.save(Uuid.builder()
                .uuid(uuid).build());

        String keyName = amazonS3Manager.generateAiProfileKeyName(savedUuid);

        // S3 업로드
        String s3Url = amazonS3Manager.uploadFileFromBase64(keyName, base64Img, "image/png");

        // 결과 반환
        return ApiResponse.onSuccess(s3Url);
    }


    @Operation(summary = "Test : Base64 AI 이미지값")
    @GetMapping("/image-b64")
    public ApiResponse<String> getAiImageB64() {

        // ImageGenerator에서 prompt로 Base64 응답 받기 (b64_json)
        String prompt = "아기새 만화툰";
        String base64Img = imageGenerator.generateB64(prompt);


        // 결과 반환
        return ApiResponse.onSuccess(base64Img);
    }

}
