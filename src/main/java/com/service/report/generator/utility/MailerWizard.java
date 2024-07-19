package com.service.report.generator.utility;

import com.service.report.generator.exception.EmailDispatcherException;
import com.service.report.generator.properties.mailer.EMailerConfigProperties;
import jakarta.mail.internet.MimeMessage;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.io.File;


@Component
public class MailerWizard {

    private final JavaMailSender javaMailSender;
    private final TextWizard textHelper;
    private final EMailerConfigProperties eMailerConfigProperties;
    public MailerWizard(
            JavaMailSender javaMailSender,
            TextWizard textHelper,
            EMailerConfigProperties eMailerConfigProperties
    ) {
        this.javaMailSender = javaMailSender;
        this.textHelper = textHelper;
        this.eMailerConfigProperties = eMailerConfigProperties;
    }



    public boolean sendMail(String recipient) {
        return mailSender(recipient, "no-reply", null, false, null);
    }

    public boolean sendMail(String recipient, String subject) {
        return mailSender(recipient, subject, null, false, null);
    }

    public boolean sendMail(String recipient, String subject, String messageBody) {
        return mailSender(recipient, subject, messageBody, false, null);
    }

    public boolean sendMail(String recipient, String subject, String messageBody, boolean isHtml) {
        return mailSender(recipient, subject, messageBody, isHtml, null);
    }

    public boolean sendMail(String recipient, String subject, String messageBody, String attachmentPath) {
        return mailSender(recipient, subject, messageBody, false, attachmentPath);
    }

    public boolean sendMail(String recipient, String subject, String messageBody, boolean isHtml, String attachmentPath) {
        return mailSender(recipient, subject, messageBody, isHtml, attachmentPath);
    }

    private boolean mailSender(
            String recipient,
            String subject,
            String messageBody,
            boolean isHtml,
            String attachmentPath
    ) {
        if (null == eMailerConfigProperties)
            throw new IllegalArgumentException("Failed to inject `${app.emailer.*}` values from the application properties.");

        if (textHelper.isBlank(eMailerConfigProperties.getDefaultEmail()))
            throw new IllegalArgumentException("Failed to inject `${app.emailer.defaultEmail}` value from the application properties.");

        if (textHelper.isBlank(recipient) || textHelper.isBlank(subject) || textHelper.isBlank(messageBody))
            return false;

        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true);
            mimeMessageHelper.setFrom(eMailerConfigProperties.getDefaultEmail());
            mimeMessageHelper.setTo(recipient);
            mimeMessageHelper.setSubject(subject);
            mimeMessageHelper.setText(messageBody, isHtml);

            if (!textHelper.isBlank(attachmentPath)) {
                File file = new File(attachmentPath);
                FileSystemResource fileSystemResource = new FileSystemResource(file);
                String attachmentFilename = file.getName();
                mimeMessageHelper.addAttachment(attachmentFilename, fileSystemResource);
            }

            javaMailSender.send(mimeMessage);
            return true;
        } catch (Exception exception) {
            throw new EmailDispatcherException(exception.getMessage());
        }

    }

}
