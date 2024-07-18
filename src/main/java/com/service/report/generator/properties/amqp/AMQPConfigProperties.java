package com.service.report.generator.properties.amqp;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.amqp")
@Getter
@Setter
public class AMQPConfigProperties {
    AMQPType queue;
    AMQPType routingKey;
    String exchange;
    long retryDelay;
    int retryLimit;
}
