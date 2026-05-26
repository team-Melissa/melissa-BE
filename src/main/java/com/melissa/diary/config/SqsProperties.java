package com.melissa.diary.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "melissa.sqs")
public class SqsProperties {

    private boolean enabled = false;
    private String region = "ap-northeast-2";
    private String jobQueueUrl = "";
    private int maxMessagesPerPoll = 5;
    private int waitTimeSeconds = 20;
    private int visibilityTimeoutSeconds = 600;
    private int maxReceiveCount = 5;
    private int outboxPublishBatchSize = 10;
    private long outboxPublisherFixedDelayMs = 5000;
    private long consumerFixedDelayMs = 1000;

    @PostConstruct
    public void validate() {
        if (!enabled) {
            return;
        }
        if (jobQueueUrl == null || jobQueueUrl.isBlank()) {
            throw new IllegalStateException("AWS_SQS_JOB_QUEUE_URL must be set when MELISSA_SQS_ENABLED=true");
        }
        if (maxMessagesPerPoll < 1 || maxMessagesPerPoll > 10) {
            throw new IllegalStateException("melissa.sqs.max-messages-per-poll must be between 1 and 10");
        }
        if (waitTimeSeconds < 0 || waitTimeSeconds > 20) {
            throw new IllegalStateException("melissa.sqs.wait-time-seconds must be between 0 and 20");
        }
        if (visibilityTimeoutSeconds <= 0) {
            throw new IllegalStateException("melissa.sqs.visibility-timeout-seconds must be positive");
        }
        if (maxReceiveCount <= 0) {
            throw new IllegalStateException("melissa.sqs.max-receive-count must be positive");
        }
    }
}
