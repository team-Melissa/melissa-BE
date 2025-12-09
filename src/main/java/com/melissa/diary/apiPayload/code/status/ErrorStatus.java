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
    SETTING_INVALID_TIME_FORMAT(HttpStatus.BAD_REQUEST, "SETTING4003", "유효하지 않은 시간 형식입니다. HH:mm 또는 HH:mm:ss로 전달해주세요."),

    // AI
    PARSING_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "AI5001", "llm 파싱 도중 실패했습니다."),

    // AI Profile
    PROFILE_FORBIDDEN(HttpStatus.FORBIDDEN, "PROFILE4001", "해당 AI 프로필에 접근할 수 있는 권한이 없습니다."),
    PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND, "PROFILE4002", "해당 AI 프로필이 존재하지 않습니다."),

    // Calendar & Thread 관련 에러
    CALENDAR_NOT_FOUND(HttpStatus.NOT_FOUND, "CALENDAR4001", "해당 날짜 또는 월의 데이터가 존재하지 않습니다."),
    CALENDAR_FORBIDDEN(HttpStatus.FORBIDDEN, "CALENDAR4002", "해당 캘린더 데이터를 조회할 권한이 없습니다."),
    CALENDAR_INVALID_DATE(HttpStatus.BAD_REQUEST, "CALENDAR4003", "유효하지 않은 날짜입니다."),
    CALENDAR_PROCESSING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "CALENDAR5001", "캘린더 데이터를 처리하는 중 오류가 발생했습니다."),
    THREAD_ALREADY_ENROLL(HttpStatus.BAD_REQUEST, "THREAD4001", "해당 날짜의 스레드가 이미 존재합니다."),
    THREAD_LATEST_NOT_FOUND(HttpStatus.NOT_FOUND, "THREAD4002", "최근 스레드를 찾을 수 없습니다."),
    THREAD_TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "THREAD4003", "요약 요청이 너무 자주 발생했습니다. 1분 후 다시 시도해주세요."),
    THREAD_NOT_FOUND(HttpStatus.NOT_FOUND, "THREAD4004", "해당 Thread를 찾을 수 없습니다."),
    THREAD_FORBIDDEN(HttpStatus.FORBIDDEN, "THREAD4005", "해당 Thread에 접근할 권한이 없습니다."),

    // CHAT
    CHAT_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT4001", "해당 날짜의 채팅로그를 찾을 수 없습니다."),
    CHAT_LOG_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT4002", "해당 채팅 메시지를 찾을 수 없습니다."),
    CHAT_LOG_FORBIDDEN(HttpStatus.FORBIDDEN, "CHAT4003", "해당 채팅 메시지에 접근할 권한이 없습니다."),
    CHAT_LOG_AI_MESSAGE(HttpStatus.BAD_REQUEST, "CHAT4004", "AI 메시지는 수정할 수 없습니다."),

    // Quota
    QUOTA_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "QUOTA4001", "일일 사용량을 초과하였습니다."),

    // Auth
    // 인증 관련 에러 상태
    SOCIAL_LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "AUTH4001", "소셜 로그인 인증에 실패했습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH4002", "토큰이 유효하지 않습니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH4003", "토큰이 만료되었습니다."),
    TOKEN_MISMATCH(HttpStatus.UNAUTHORIZED, "AUTH4004", "토큰이 일치하지 않습니다."),
    TOKEN_CREATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AUTH5001", "토큰 생성에 실패했습니다."),
    TOKEN_VERIFICATION_FAILED(HttpStatus.UNAUTHORIZED, "AUTH4005", "토큰 검증에 실패했습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "AUTH4006", "해당 유저가 존재하지 않습니다."),

    // Expo Push Token
    EXPO_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "EXPO4001", "해당 Expo Push Token을 찾을 수 없습니다."),
    EXPO_TOKEN_ALREADY_EXISTS(HttpStatus.CONFLICT, "EXPO4002", "이미 등록된 Expo Push Token입니다."),
    EXPO_TOKEN_INVALID_FORMAT(HttpStatus.BAD_REQUEST, "EXPO4003", "유효하지 않은 Expo Push Token 형식입니다."),

    // Diary (v1.3.0)
    DIARY_NOT_FOUND(HttpStatus.NOT_FOUND, "DIARY4001", "해당 일기를 찾을 수 없습니다."),
    DIARY_FORBIDDEN(HttpStatus.FORBIDDEN, "DIARY4002", "해당 일기에 접근할 권한이 없습니다."),
    DIARY_ALREADY_DELETED(HttpStatus.BAD_REQUEST, "DIARY4003", "이미 삭제된 일기입니다."),
    DIARY_MAX_COUNT_EXCEEDED(HttpStatus.BAD_REQUEST, "DIARY4004", "하루 최대 3개까지만 일기를 작성할 수 있습니다.");





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
