package com.reejuven8.notification.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String CLINICAL_RISK_EXCHANGE   = "clinical.risk.exchange";
    public static final String MILESTONE_EXCHANGE        = "milestone.reminder.exchange";
    public static final String CLINICAL_RISK_QUEUE      = "clinical.risk.detected";
    public static final String MILESTONE_QUEUE          = "patient.milestone.due";
    public static final String CLINICAL_RISK_DLQ        = "clinical.risk.detected.dlq";
    public static final String MILESTONE_DLQ            = "patient.milestone.due.dlq";
    public static final String DLX                      = "notification.dlx";

    @Bean
    public DirectExchange clinicalRiskExchange() {
        return new DirectExchange(CLINICAL_RISK_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange milestoneExchange() {
        return new DirectExchange(MILESTONE_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange dlx() {
        return new DirectExchange(DLX, true, false);
    }

    @Bean
    public Queue clinicalRiskQueue() {
        return QueueBuilder.durable(CLINICAL_RISK_QUEUE)
            .withArgument("x-dead-letter-exchange", DLX)
            .withArgument("x-dead-letter-routing-key", CLINICAL_RISK_QUEUE)
            .build();
    }

    @Bean
    public Queue milestoneQueue() {
        return QueueBuilder.durable(MILESTONE_QUEUE)
            .withArgument("x-dead-letter-exchange", DLX)
            .withArgument("x-dead-letter-routing-key", MILESTONE_QUEUE)
            .build();
    }

    @Bean
    public Queue clinicalRiskDlq() { return QueueBuilder.durable(CLINICAL_RISK_DLQ).build(); }

    @Bean
    public Queue milestoneDlq() { return QueueBuilder.durable(MILESTONE_DLQ).build(); }

    @Bean
    public Binding clinicalRiskBinding() {
        return BindingBuilder.bind(clinicalRiskQueue()).to(clinicalRiskExchange()).with(CLINICAL_RISK_QUEUE);
    }

    @Bean
    public Binding milestoneBinding() {
        return BindingBuilder.bind(milestoneQueue()).to(milestoneExchange()).with(MILESTONE_QUEUE);
    }

    @Bean
    public Binding clinicalRiskDlqBinding() {
        return BindingBuilder.bind(clinicalRiskDlq()).to(dlx()).with(CLINICAL_RISK_QUEUE);
    }

    @Bean
    public Binding milestoneDlqBinding() {
        return BindingBuilder.bind(milestoneDlq()).to(dlx()).with(MILESTONE_QUEUE);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory factory) {
        RabbitTemplate template = new RabbitTemplate(factory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
