package com.saurabh.notification.controller;

import com.saurabh.notification.dto.NotificationRequestDTO;
import com.saurabh.notification.dto.NotificationResponseDTO;
import com.saurabh.notification.service.NotificationOrchestrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationOrchestrationService orchestrationService;

    /**
     * Triggers a broadcast notification to all users via Kafka sharding.
     * Returns immediately with a job ID — processing happens asynchronously.
     *
     * POST /api/v1/notifications/broadcast
     * Body: { "title": "...", "message": "...", "channel": "EMAIL|SMS|PUSH_NOTIFICATION" }
     */
    @PostMapping("/broadcast")
    public ResponseEntity<NotificationResponseDTO> broadcastNotification(
            @RequestBody @Valid NotificationRequestDTO request) {
        log.info("Broadcast notification request received. Channel={}", request.getChannel());
        NotificationResponseDTO response = orchestrationService.scheduleNotification(request);
        return ResponseEntity.accepted().body(response);
    }
}
