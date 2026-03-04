package com.saurabh.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kafka message representing a shard of users to be notified.
 * The consumer uses page + batchSize to fetch the correct slice from the DB,
 * avoiding loading all 1M users at once.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserShardMessage {

    private String jobId;
    private int shardIndex;
    private int totalShards;
    private int page;       // JPA page number (0-based)
    private int batchSize;  // page size
    private NotificationRequestDTO notificationRequest;
}
