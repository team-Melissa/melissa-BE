package com.melissa.diary.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.melissa.diary.config.SqsProperties;
import com.melissa.diary.service.DiaryImageJobProcessor;
import com.melissa.diary.sqs.SqsJobMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "melissa.sqs", name = "enabled", havingValue = "true")
public class SqsJobConsumerScheduler {

    private final SqsClient sqsClient;
    private final SqsProperties sqsProperties;
    private final ObjectMapper objectMapper;
    private final DiaryImageJobProcessor diaryImageJobProcessor;

    @Scheduled(fixedDelayString = "${melissa.sqs.consumer-fixed-delay-ms:1000}")
    public void pollAndProcess() {
        try {
            var response = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                    .queueUrl(sqsProperties.getJobQueueUrl())
                    .maxNumberOfMessages(sqsProperties.getMaxMessagesPerPoll())
                    .waitTimeSeconds(sqsProperties.getWaitTimeSeconds())
                    .visibilityTimeout(sqsProperties.getVisibilityTimeoutSeconds())
                    .messageSystemAttributeNames(MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT)
                    .build());

            for (Message message : response.messages()) {
                processMessage(message);
            }
        } catch (Exception e) {
            log.error("[SqsJobConsumer] poll cycle failed", e);
        }
    }

    private void processMessage(Message message) {
        int receiveCount = parseReceiveCount(message);

        try {
            SqsJobMessage jobMessage = objectMapper.readValue(message.body(), SqsJobMessage.class);
            DiaryImageJobProcessor.ProcessResult result = diaryImageJobProcessor.process(jobMessage, receiveCount);
            if (result == DiaryImageJobProcessor.ProcessResult.DELETE_MESSAGE) {
                deleteMessage(message);
            }
        } catch (Exception e) {
            log.error("[SqsJobConsumer] message processing failed. messageId={}, receiveCount={}",
                    message.messageId(), receiveCount, e);
        }
    }

    private int parseReceiveCount(Message message) {
        String value = message.attributes().get(MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT);
        if (value == null || value.isBlank()) {
            return 1;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private void deleteMessage(Message message) {
        sqsClient.deleteMessage(DeleteMessageRequest.builder()
                .queueUrl(sqsProperties.getJobQueueUrl())
                .receiptHandle(message.receiptHandle())
                .build());
        log.info("[SqsJobConsumer] message deleted. messageId={}", message.messageId());
    }
}
