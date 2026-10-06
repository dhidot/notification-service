package com.jastip.notification_service.event;

/*
@Author Didot
Created on 02/08/2026
@Last Modified on 02/08/2026 15:58
Version 1.0
*/

import com.jastip.notification_service.config.enums.EventType;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserRegisteredEvent(

        UUID eventId,

        Integer version,

        EventType eventType,

        LocalDateTime occurredAt,

        UUID userId,

        String username,

        String email,

        UUID verificationToken

) {
}

