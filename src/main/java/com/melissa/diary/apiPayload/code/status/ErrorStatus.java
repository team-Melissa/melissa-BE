package com.melissa.diary.apiPayload.code.status;

import com.melissa.diary.apiPayload.code.BaseErrorCode;
import com.melissa.diary.apiPayload.code.ErrorReasonDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorStatus implements BaseErrorCode {

    // 가장 일반적인 응답
    _INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON500", "서버 에러, 관리자에게 문의 바랍니다."),
    _BAD_REQUEST(HttpStatus.BAD_REQUEST,"COMMON400","잘못된 요청입니다."),
    _UNAUTHORIZED(HttpStatus.UNAUTHORIZED,"COMMON401","인증이 필요합니다."),
    _FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON403", "금지된 요청입니다."),

    // For test
    TEMP_EXCEPTION(HttpStatus.BAD_REQUEST, "TEMP4001", "테스트 용도"),

    // Setting
    SETTING_NOT_FOUND(HttpStatus.NOT_FOUND, "SETTING4001", "해당 유저의 셋팅을 찾을 수 없습니다."),
    SETTING_ALREADY_ENROLL(HttpStatus.BAD_REQUEST, "SETTING4002", "해당 유저의 셋팅값이 이미 존재합니다."),

    // Profile
    PROFILE_NOT_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "SETTING4001", "해당 프로필에 접근할 수 있는 권한이 없습니다,"),
    PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND, "PROFILE4002", "해당 프로필이 존재하지 않습니다."),


    // Auth
    // 인증 관련 에러 상태
    SOCIAL_LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "AUTH4001", "소셜 로그인 인증에 실패했습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH4002", "토큰이 유효하지 않습니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH4003", "토큰이 만료되었습니다."),
    TOKEN_MISMATCH(HttpStatus.UNAUTHORIZED, "AUTH4004", "토큰이 일치하지 않습니다."),
    TOKEN_CREATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AUTH5001", "토큰 생성에 실패했습니다."),
    TOKEN_VERIFICATION_FAILED(HttpStatus.UNAUTHORIZED, "AUTH4005", "토큰 검증에 실패했습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "AUTH4006", "해당 유저가 존재하지 않습니다.");





    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ErrorReasonDTO getReason() {
        return ErrorReasonDTO.builder()
                .message(message)
                .code(code)
                .isSuccess(false)
                .build();
    }

    @Override
    public ErrorReasonDTO getReasonHttpStatus() {
        return ErrorReasonDTO.builder()
                .message(message)
                .code(code)
                .isSuccess(false)
                .httpStatus(httpStatus)
                .build()
                ;
    }
}
