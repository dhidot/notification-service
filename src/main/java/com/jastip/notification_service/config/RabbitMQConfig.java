package com.jastip.notification_service.config;

/*
@Author Didot
Created on 02/08/2026
@Last Modified on 02/08/2026 15:57
Version 1.0
*/

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String AUTH_EXCHANGE =
            "auth.events";

    public static final String EMAIL_VERIFICATION_QUEUE =
            "email.verification.queue";

    public static final String USER_REGISTERED_ROUTING_KEY =
            "user.registered";

    @Bean
    public Queue emailVerificationQueue() {
        return new Queue(
                EMAIL_VERIFICATION_QUEUE,
                true
        );
    }

    @Bean
    public TopicExchange authExchange() {

        return new TopicExchange(
                AUTH_EXCHANGE
        );

    }

    @Bean
    public Binding emailBinding(
            Queue emailVerificationQueue,
            TopicExchange authExchange
    ) {

        return BindingBuilder
                .bind(emailVerificationQueue)
                .to(authExchange)
                .with(USER_REGISTERED_ROUTING_KEY);

    }
}

