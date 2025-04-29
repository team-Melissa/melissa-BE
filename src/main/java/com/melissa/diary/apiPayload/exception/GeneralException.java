package com.melissa.diary.apiPayload.exception;

import com.melissa.diary.apiPayload.code.BaseErrorCode;
import com.melissa.diary.apiPayload.code.ErrorReasonDTO;
import com.melissa.diary.apiPayload.code.status.ErrorStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GeneralException extends RuntimeException {

    private BaseErrorCode code;

    public ErrorReasonDTO getErrorReason() {
        return this.code.getReason();
    }

    public ErrorReasonDTO getErrorReasonHttpStatus(){
        return this.code.getReasonHttpStatus();
    }

    public BaseErrorCode getErrorCode() {
        return this.code;
    }
}
