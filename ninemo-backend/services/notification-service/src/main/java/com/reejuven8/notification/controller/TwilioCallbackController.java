package com.reejuven8.notification.controller;

import com.reejuven8.notification.service.TwilioCallbackService;
import com.twilio.security.RequestValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications/callbacks")
@Slf4j
@Tag(name = "Delivery Callbacks")
public class TwilioCallbackController {

    private final TwilioCallbackService callbackService;
    private final RequestValidator requestValidator;
    private final String statusCallbackUrl;

    public TwilioCallbackController(
        TwilioCallbackService callbackService,
        @Value("${twilio.auth-token}") String authToken,
        @Value("${twilio.status-callback-url:}") String statusCallbackUrl
    ) {
        this.callbackService = callbackService;
        this.requestValidator = authToken.isBlank() ? null : new RequestValidator(authToken);
        this.statusCallbackUrl = statusCallbackUrl;
    }

    @Operation(summary = "Twilio delivery-status webhook (form-encoded; X-Twilio-Signature verified)")
    @PostMapping(value = "/twilio", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> twilioStatus(
        @RequestParam Map<String, String> params,
        @RequestHeader(value = "X-Twilio-Signature", required = false) String signature
    ) {
        if (!isAuthentic(params, signature)) {
            log.warn("Twilio callback rejected — invalid signature");
            return ResponseEntity.status(403).build();
        }
        callbackService.applyStatus(
            params.get("MessageSid"),
            params.get("MessageStatus"),
            params.get("ErrorCode")
        );
        return ResponseEntity.ok().build();
    }

    private boolean isAuthentic(Map<String, String> params, String signature) {
        if (requestValidator == null || statusCallbackUrl.isBlank()) {
            // Dev/sandbox: Twilio not configured — accept but say so loudly
            log.warn("Twilio signature validation skipped (auth token or callback URL not configured)");
            return true;
        }
        return signature != null && requestValidator.validate(statusCallbackUrl, params, signature);
    }
}
