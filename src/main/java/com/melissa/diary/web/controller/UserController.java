package com.melissa.diary.web.controller;

import com.melissa.diary.apiPayload.ApiResponse;
import com.melissa.diary.service.UserService;
import com.melissa.diary.web.dto.UserResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@Tag(name = "UserAPI", description = "유저 관련 API")
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(description = "회원탈퇴하고, 유저 정보를 리턴합니다.")
    @DeleteMapping
    public ApiResponse<UserResponseDTO.DeleteResultDTO> deleteUser(Principal principal) {
        Long userId = Long.parseLong(principal.getName());

        UserResponseDTO.DeleteResultDTO response = userService.deleteUser(userId);

        return ApiResponse.onSuccess(response);
    }

}
