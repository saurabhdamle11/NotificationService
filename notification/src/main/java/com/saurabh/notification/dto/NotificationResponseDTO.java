package com.saurabh.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponseDTO {

    private String jobId;
    private int totalShards;
    private long totalUsers;
    private String status;
    private String message;
}
