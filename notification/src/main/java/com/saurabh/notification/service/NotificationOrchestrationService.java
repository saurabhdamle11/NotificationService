package com.saurabh.notification.service;

import com.saurabh.notification.dto.NotificationRequestDTO;
import com.saurabh.notification.dto.NotificationResponseDTO;
import com.saurabh.notification.dto.UserShardMessage;
import com.saurabh.notification.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Orchestrates the broadcast notification flow:
 * 1. Counts total users in the DB.
 * 2. Divides them into fixed-size shards.
 * 3. Publishes one Kafka message per shard — consumers do the actual DB fetch + send.
 *
 * This ensures the DB is never scanned in full at once; each shard reads only
 * its page (batchSize rows) from PostgreSQL.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationOrchestrationService {

    private final UserRepository userRepository;
    private final KafkaProducerService kafkaProducerService;

    @Value("${notification.sharding.batch-size}")
    private int batchSize;

    public NotificationResponseDTO scheduleNotification(NotificationRequestDTO request) {
        String jobId = UUID.randomUUID().toString();
        long totalUsers = userRepository.count();

        if (totalUsers == 0) {
            log.warn("No users found in the database. Job {} not scheduled.", jobId);
            return NotificationResponseDTO.builder()
                    .jobId(jobId)
                    .totalShards(0)
                    .totalUsers(0)
                    .status("SKIPPED")
                    .message("No users to notify")
                    .build();
        }

        int totalShards = (int) Math.ceil((double) totalUsers / batchSize);

        log.info("Scheduling notification job {}. Channel={}, TotalUsers={}, BatchSize={}, Shards={}",
                jobId, request.getChannel(), totalUsers, batchSize, totalShards);

        for (int i = 0; i < totalShards; i++) {
            UserShardMessage shardMessage = UserShardMessage.builder()
                    .jobId(jobId)
                    .shardIndex(i)
                    .totalShards(totalShards)
                    .page(i)
                    .batchSize(batchSize)
                    .notificationRequest(request)
                    .build();

            kafkaProducerService.publishShard(shardMessage);
        }

        log.info("Published {} shard messages for job {}", totalShards, jobId);

        return NotificationResponseDTO.builder()
                .jobId(jobId)
                .totalShards(totalShards)
                .totalUsers(totalUsers)
                .status("SCHEDULED")
                .message("Notification job scheduled. " + totalShards + " shards dispatched.")
                .build();
    }
}
