package com.reejuven8.notification.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class WhatsAppService {

    @Value("${twilio.account-sid}")
    private String accountSid;

    @Value("${twilio.auth-token}")
    private String authToken;

    @Value("${twilio.whatsapp-from}")
    private String fromNumber;

    @Value("${twilio.status-callback-url:}")
    private String statusCallbackUrl;

    @PostConstruct
    public void init() {
        if (!accountSid.isBlank() && !authToken.isBlank()) {
            Twilio.init(accountSid, authToken);
        }
    }

    public String send(String toPhoneNumber, String body) {
        if (accountSid.isBlank()) {
            log.warn("Twilio not configured — skipping WhatsApp to {}", toPhoneNumber);
            return "SKIPPED";
        }
        try {
            var creator = Message.creator(
                new PhoneNumber("whatsapp:" + toPhoneNumber),
                new PhoneNumber(fromNumber),
                body
            );
            if (!statusCallbackUrl.isBlank()) {
                creator.setStatusCallback(statusCallbackUrl);
            }
            Message msg = creator.create();
            log.info("WhatsApp sent sid={} to={}", msg.getSid(), toPhoneNumber);
            return msg.getSid();
        } catch (Exception e) {
            log.error("WhatsApp send failed to={} error={}", toPhoneNumber, e.getMessage());
            throw new RuntimeException("WhatsApp delivery failed", e);
        }
    }
}
