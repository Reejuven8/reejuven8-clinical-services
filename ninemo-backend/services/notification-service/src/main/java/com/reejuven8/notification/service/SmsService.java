package com.reejuven8.notification.service;

import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SmsService {

    @Value("${twilio.account-sid}")
    private String accountSid;

    @Value("${twilio.sms-from}")
    private String fromNumber;

    @Value("${twilio.status-callback-url:}")
    private String statusCallbackUrl;

    public String send(String toPhoneNumber, String body) {
        if (accountSid.isBlank() || fromNumber.isBlank()) {
            log.warn("SMS not configured — skipping to {}", toPhoneNumber);
            return "SKIPPED";
        }
        try {
            var creator = Message.creator(
                new PhoneNumber(toPhoneNumber),
                new PhoneNumber(fromNumber),
                body
            );
            if (!statusCallbackUrl.isBlank()) {
                creator.setStatusCallback(statusCallbackUrl);
            }
            Message msg = creator.create();
            log.info("SMS sent sid={} to={}", msg.getSid(), toPhoneNumber);
            return msg.getSid();
        } catch (Exception e) {
            log.error("SMS send failed to={} error={}", toPhoneNumber, e.getMessage());
            throw new RuntimeException("SMS delivery failed", e);
        }
    }
}
