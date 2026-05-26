package com.melissa.diary.sqs;

import com.melissa.diary.domain.OutboxEvent;

public record SqsJobMessage(
        Long eventId,
        String eventType,
        Long jobId,
        Long targetId
) {
    public static SqsJobMessage from(OutboxEvent event) {
        return new SqsJobMessage(
                event.getId(),
                event.getEventType().name(),
                event.getJobId(),
                event.getTargetId()
        );
    }
}
