package com.saurabh.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequestDTO {

    @NotBlank(message = "Title must not be blank")
    private String title;

    @NotBlank(message = "Message must not be blank")
    private String message;

    @NotNull(message = "Channel must not be null")
    private NotificationChannel channel;
}
