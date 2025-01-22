package com.melissa.diary.apiPayload.exception.handler;

import com.melissa.diary.apiPayload.code.BaseErrorCode;
import com.melissa.diary.apiPayload.exception.GeneralException;

public class ErrorHandler extends GeneralException {

    public ErrorHandler(BaseErrorCode errorCode) {
        super(errorCode);
    }
}