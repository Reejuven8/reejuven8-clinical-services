package com.reejuven8.healthdata.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String DOCUMENT_EXCHANGE = "document.unstructured.exchange";
    public static final String DOCUMENT_QUEUE = "document.unstructured.uploaded";
    public static final String DOCUMENT_DLX = "document.unstructured.dlx";
    public static final String DOCUMENT_DLQ = "document.unstructured.uploaded.dlq";
    public static final String DOCUMENT_ROUTING_KEY = "document.unstructured.uploaded";

    @Bean
    public DirectExchange documentExchange() {
        return new DirectExchange(DOCUMENT_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange documentDlx() {
        return new DirectExchange(DOCUMENT_DLX, true, false);
    }

    @Bean
    public Queue documentQueue() {
        return QueueBuilder.durable(DOCUMENT_QUEUE)
            .withArgument("x-dead-letter-exchange", DOCUMENT_DLX)
            .withArgument("x-dead-letter-routing-key", DOCUMENT_ROUTING_KEY)
            .build();
    }

    @Bean
    public Queue documentDlq() {
        return QueueBuilder.durable(DOCUMENT_DLQ).build();
    }

    @Bean
    public Binding documentBinding() {
        return BindingBuilder.bind(documentQueue()).to(documentExchange()).with(DOCUMENT_ROUTING_KEY);
    }

    @Bean
    public Binding documentDlqBinding() {
        return BindingBuilder.bind(documentDlq()).to(documentDlx()).with(DOCUMENT_ROUTING_KEY);
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
