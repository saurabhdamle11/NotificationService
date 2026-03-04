package com.saurabh.notification.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saurabh.notification.dto.UserDTO;
import com.saurabh.notification.dto.UserShardMessage;
import com.saurabh.notification.entity.User;
import com.saurabh.notification.mapper.UserMapper;
import com.saurabh.notification.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Kafka consumer that processes one shard at a time.
 * Receives the shard message as a JSON string and deserializes it manually.
 * Fetches only the corresponding page of users from the DB — no full table scan.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${notification.kafka.topic}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeShard(
            String payload,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {
        UserShardMessage shardMessage;
        try {
            shardMessage = objectMapper.readValue(payload, UserShardMessage.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize shard message at partition={} offset={}: {}",
                    partition, offset, e.getMessage());
            return;
        }

        log.info("Consuming shard {}/{} for job {} [partition={}, offset={}]",
                shardMessage.getShardIndex() + 1, shardMessage.getTotalShards(),
                shardMessage.getJobId(), partition, offset);

        Pageable pageable = PageRequest.of(shardMessage.getPage(), shardMessage.getBatchSize());
        Page<User> userPage = userRepository.findAll(pageable);
        List<UserDTO> users = userMapper.toDTOList(userPage.getContent());

        users.forEach(user -> sendNotification(user, shardMessage));

        log.info("Shard {}/{} done: {} users notified via {} for job {}",
                shardMessage.getShardIndex() + 1, shardMessage.getTotalShards(),
                users.size(), shardMessage.getNotificationRequest().getChannel(),
                shardMessage.getJobId());
    }

    /**
     * Placeholder: replace with a real provider (SendGrid, Twilio, FCM, etc.).
     */
    private void sendNotification(UserDTO user, UserShardMessage shardMessage) {
        var request = shardMessage.getNotificationRequest();
        log.debug("[{}] Notifying user {} ({}) — title: \"{}\"",
                request.getChannel(), user.getId(), user.getEmail(), request.getTitle());
        // TODO: inject and call your EmailService / SmsService / PushService
    }
}
