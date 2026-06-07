package com.melissa.diary.retry;

import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.Locale;
import java.util.concurrent.TimeoutException;

public final class RetryClassifier {

    private RetryClassifier() {
    }

    public static RetryDecision classify(Throwable throwable) {
        if (throwable == null) {
            return RetryDecision.NON_RETRYABLE;
        }

        Throwable current = throwable;
        while (current != null) {
            RetryDecision decision = classifySingle(current);
            if (decision != null) {
                return decision;
            }
            current = current.getCause();
        }

        return RetryDecision.RETRYABLE;
    }

    public static boolean isRetryable(Throwable throwable) {
        return classify(throwable) == RetryDecision.RETRYABLE;
    }

    private static RetryDecision classifySingle(Throwable throwable) {
        if (throwable instanceof IllegalArgumentException) {
            return RetryDecision.NON_RETRYABLE;
        }

        if (throwable instanceof WebClientResponseException e) {
            return classifyHttpStatus(e.getStatusCode());
        }

        if (throwable instanceof HttpStatusCodeException e) {
            return classifyHttpStatus(e.getStatusCode());
        }

        if (throwable instanceof AwsServiceException e) {
            return classifyAwsSdkV2ServiceException(e);
        }

        if (throwable instanceof WebClientRequestException
                || throwable instanceof ResourceAccessException
                || throwable instanceof SdkClientException
                || throwable instanceof SocketTimeoutException
                || throwable instanceof ConnectException
                || throwable instanceof TimeoutException) {
            return RetryDecision.RETRYABLE;
        }

        if (throwable instanceof IOException) {
            return RetryDecision.RETRYABLE;
        }

        return null;
    }

    private static RetryDecision classifyHttpStatus(HttpStatusCode statusCode) {
        int status = statusCode.value();
        if (status == 408 || status == 409 || status == 425 || status == 429 || status >= 500) {
            return RetryDecision.RETRYABLE;
        }
        if (status >= 400) {
            return RetryDecision.NON_RETRYABLE;
        }
        return RetryDecision.RETRYABLE;
    }

    private static RetryDecision classifyAwsSdkV2ServiceException(AwsServiceException exception) {
        int status = exception.statusCode();
        String errorCode = exception.awsErrorDetails() == null || exception.awsErrorDetails().errorCode() == null
                ? ""
                : exception.awsErrorDetails().errorCode().toLowerCase(Locale.ROOT);

        if (status == 429
                || status >= 500
                || errorCode.contains("throttl")
                || errorCode.contains("requestlimit")
                || errorCode.contains("slowdown")) {
            return RetryDecision.RETRYABLE;
        }

        if (status >= 400) {
            return RetryDecision.NON_RETRYABLE;
        }

        return RetryDecision.RETRYABLE;
    }
}
