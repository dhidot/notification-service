package com.jastip.notification_service.consumer;

/*
@Author Didot
Created on 02/08/2026
@Last Modified on 02/08/2026 16:02
Version 1.0
*/

import com.jastip.notification_service.config.RabbitMQConfig;
import com.jastip.notification_service.event.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserRegisteredConsumer {

    @RabbitListener(
            queues = RabbitMQConfig.EMAIL_VERIFICATION_QUEUE
    )
    public void consume(
            UserRegisteredEvent event
    ) {

        System.out.println(event);

    }

}
