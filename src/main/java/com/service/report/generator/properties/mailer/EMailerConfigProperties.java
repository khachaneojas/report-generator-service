package com.service.report.generator.properties.mailer;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.emailer")
@Getter
@Setter
public class EMailerConfigProperties {
    String defaultEmail;
}
