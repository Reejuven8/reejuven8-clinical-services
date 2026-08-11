package com.reejuven8.ninemo.clinical.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String CLINICAL_RISK_EXCHANGE = "clinical.risk.exchange";
    public static final String MILESTONE_REMINDER_EXCHANGE = "milestone.reminder.exchange";
    public static final String CLINICAL_RISK_ROUTING_KEY = "clinical.risk.detected";
    public static final String MILESTONE_REMINDER_ROUTING_KEY = "patient.milestone.due";

    @Bean
    public DirectExchange clinicalRiskExchange() {
        return new DirectExchange(CLINICAL_RISK_EXCHANGE);
    }

    @Bean
    public DirectExchange milestoneReminderExchange() {
        return new DirectExchange(MILESTONE_REMINDER_EXCHANGE);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
