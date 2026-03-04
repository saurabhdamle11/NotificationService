package com.saurabh.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saurabh.notification.dto.UserShardMessage;
import com.saurabh.notification.exception.NotificationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${notification.kafka.topic}")
    private String topicName;

    public void publishShard(UserShardMessage message) {
        try {
            String payload = objectMapper.writeValueAsString(message);
            kafkaTemplate.send(topicName, message.getJobId(), payload)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish shard {}/{} for job {}: {}",
                                    message.getShardIndex() + 1, message.getTotalShards(),
                                    message.getJobId(), ex.getMessage());
                        } else {
                            log.debug("Published shard {}/{} for job {} → partition={} offset={}",
                                    message.getShardIndex() + 1, message.getTotalShards(),
                                    message.getJobId(),
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        }
                    });
        } catch (JsonProcessingException e) {
            throw new NotificationException(
                    "Failed to serialize shard message for job " + message.getJobId(),
                    HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }
}
