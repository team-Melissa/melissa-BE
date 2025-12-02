package com.melissa.diary.apiPayload;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.melissa.diary.apiPayload.code.BaseCode;
import com.melissa.diary.apiPayload.code.status.SuccessStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonPropertyOrder({"isSuccess", "code", "message", "result"})
@Schema(description = "API 응답 래퍼")
public class ApiResponse<T> {

    @JsonProperty("isSuccess")
    @Schema(description = "요청 성공 여부", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private final Boolean isSuccess;
    
    @Schema(description = "응답 코드", example = "COMMON200", requiredMode = Schema.RequiredMode.REQUIRED)
    private final String code;
    
    @Schema(description = "응답 메시지", example = "성공입니다.", requiredMode = Schema.RequiredMode.REQUIRED)
    private final String message;
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "응답 데이터", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
    private T result;


    // 성공한 경우 응답 생성
    public static <T> ApiResponse<T> onSuccess(T result){
        return new ApiResponse<>(true, SuccessStatus._OK.getCode() , SuccessStatus._OK.getMessage(), result);
    }

    public static <T> ApiResponse<T> of(BaseCode code, T result){
        return new ApiResponse<>(true, code.getReasonHttpStatus().getCode() , code.getReasonHttpStatus().getMessage(), result);
    }

    // 실패한 경우 응답 생성
    public static <T> ApiResponse<T> onFailure(String code, String message, T data){
        return new ApiResponse<>(false, code, message, data);
    }
}
