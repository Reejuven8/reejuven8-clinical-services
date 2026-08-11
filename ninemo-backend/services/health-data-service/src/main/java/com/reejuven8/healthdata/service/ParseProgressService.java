package com.reejuven8.healthdata.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@Slf4j
public class ParseProgressService {

    private static final long EMITTER_TIMEOUT_MS = Duration.ofMinutes(5).toMillis();
    private static final Duration COMPLETED_RETENTION = Duration.ofMinutes(30);
    private static final String EVENT_NAME = "parse-progress";

    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final Map<String, Instant> completed = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String documentId) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);

        if (completed.containsKey(documentId)) {
            send(emitter, "PARSED");
            emitter.complete();
            return emitter;
        }

        emitters.computeIfAbsent(documentId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        Runnable cleanup = () -> remove(documentId, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> remove(documentId, emitter));

        send(emitter, "PROCESSING");
        return emitter;
    }

    public void notifyParsed(String documentId) {
        completed.put(documentId, Instant.now());
        evictExpiredCompleted();

        List<SseEmitter> subscribers = emitters.remove(documentId);
        if (subscribers == null) return;
        subscribers.forEach(emitter -> {
            send(emitter, "PARSED");
            emitter.complete();
        });
        log.info("Parse progress PARSED broadcast documentId={} subscribers={}", documentId, subscribers.size());
    }

    private void send(SseEmitter emitter, String status) {
        try {
            emitter.send(SseEmitter.event().name(EVENT_NAME).data(status));
        } catch (IOException | IllegalStateException e) {
            emitter.completeWithError(e);
        }
    }

    private void remove(String documentId, SseEmitter emitter) {
        List<SseEmitter> list = emitters.get(documentId);
        if (list == null) return;
        list.remove(emitter);
        if (list.isEmpty()) emitters.remove(documentId, list);
    }

    private void evictExpiredCompleted() {
        Instant cutoff = Instant.now().minus(COMPLETED_RETENTION);
        completed.entrySet().removeIf(e -> e.getValue().isBefore(cutoff));
    }
}
