package com.melissa.diary.retry;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;

import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

class RetryClassifierTest {

    @Test
    void classifiesTimeoutAsRetryable() {
        assertThat(RetryClassifier.classify(new SocketTimeoutException("timeout")))
                .isEqualTo(RetryDecision.RETRYABLE);
    }

    @Test
    void classifiesHttp429And5xxAsRetryable() {
        assertThat(RetryClassifier.classify(webClientException(HttpStatus.TOO_MANY_REQUESTS)))
                .isEqualTo(RetryDecision.RETRYABLE);
        assertThat(RetryClassifier.classify(webClientException(HttpStatus.BAD_GATEWAY)))
                .isEqualTo(RetryDecision.RETRYABLE);
    }

    @Test
    void classifiesHttp4xxAsNonRetryable() {
        assertThat(RetryClassifier.classify(webClientException(HttpStatus.BAD_REQUEST)))
                .isEqualTo(RetryDecision.NON_RETRYABLE);
        assertThat(RetryClassifier.classify(webClientException(HttpStatus.FORBIDDEN)))
                .isEqualTo(RetryDecision.NON_RETRYABLE);
    }

    @Test
    void classifiesAwsThrottlingAsRetryable() {
        AwsServiceException exception = AwsServiceException.builder()
                .message("throttled")
                .statusCode(400)
                .awsErrorDetails(AwsErrorDetails.builder()
                        .errorCode("Throttling")
                        .build())
                .build();

        assertThat(RetryClassifier.classify(exception)).isEqualTo(RetryDecision.RETRYABLE);
    }

    private WebClientResponseException webClientException(HttpStatus status) {
        return WebClientResponseException.create(
                status.value(),
                status.getReasonPhrase(),
                null,
                null,
                null
        );
    }
}
