package com.service.report.generator.config;

import com.service.report.generator.properties.amqp.AMQPConfigProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@RequiredArgsConstructor
public class RabbitMQConfig {

    public final AMQPConfigProperties amqpConfiguration;

    @Bean("standardQueue")
    public Queue standardQueue() {
        return new Queue(amqpConfiguration.getQueue().getStandard());
    }
    @Bean("mailerQueue")
    public Queue mailerQueue() {
        return new Queue(amqpConfiguration.getQueue().getMailer());
    }



    @Bean("standardBinding")
    public Binding standardBinding(@Qualifier("standardQueue") Queue queue, @Qualifier("exchange") TopicExchange exchange) {
        return BindingBuilder
                .bind(queue)
                .to(exchange)
                .with(amqpConfiguration.getRoutingKey().getStandard());
    }
    @Bean("mailerBinding")
    Binding mailerBinding(@Qualifier("mailerQueue") Queue queue, @Qualifier("exchange") TopicExchange exchange) {
        return BindingBuilder
                .bind(queue)
                .to(exchange)
                .with(amqpConfiguration.getRoutingKey().getMailer());
    }



    @Bean("exchange")
    public TopicExchange exchange() {
        return new TopicExchange(amqpConfiguration.getExchange());
    }




    @Bean("converter")
    public MessageConverter converter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean("template")
    public AmqpTemplate template(ConnectionFactory connectionFactory, @Qualifier("converter") MessageConverter converter) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(converter);
        return rabbitTemplate;
    }


}
