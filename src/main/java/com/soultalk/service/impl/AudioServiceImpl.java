package com.soultalk.service.impl;

import com.alibaba.dashscope.exception.NoApiKeyException;
import com.soultalk.aigc.Speech;
import com.soultalk.service.AudioService;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.*;

@Service
@Slf4j
public class AudioServiceImpl implements AudioService {
    private static final int SAMPLE_RATE = 16000;
    private final ConcurrentMap<String, SessionState> sessionStates = new ConcurrentHashMap<>();
    private final ExecutorService recognitionExecutor;
    private final ScheduledExecutorService cleanupExecutor;
    @Autowired
    private Speech speech;

    public AudioServiceImpl() {
        int corePoolSize = Runtime.getRuntime().availableProcessors();
        this.recognitionExecutor = Executors.newFixedThreadPool(corePoolSize * 2);
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor();
        this.cleanupExecutor.scheduleAtFixedRate(this::cleanInactiveSessions, 5, 5, TimeUnit.MINUTES);
    }

    @PreDestroy
    public void destroy() {
        sessionStates.keySet().forEach(this::completeSession);

        recognitionExecutor.shutdown();
        cleanupExecutor.shutdown();
        try {
            if (!recognitionExecutor.awaitTermination(3, TimeUnit.SECONDS)) {
                recognitionExecutor.shutdownNow();
            }
            if (!cleanupExecutor.awaitTermination(3, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            recognitionExecutor.shutdownNow();
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public void uploadData(String id, byte[] audioData) {
        if (audioData.length == 0) return;
        SessionState session = sessionStates.get(id);
        if (session == null) {
            return;
        }

        ByteBuffer buffer = ByteBuffer.wrap(audioData);

        if (!session.completed) {
            session.audioBufferQueue.offer(buffer.duplicate());
            if (session.rxEmitter != null && !session.rxEmitter.isCancelled()) {
                session.rxEmitter.onNext(buffer);
            }
        }
    }


    public SseEmitter createRecognitionSession() {
        String sessionId = UUID.randomUUID().toString();
        SessionState session = new SessionState();
        sessionStates.put(sessionId, session);

        SseEmitter emitter = new SseEmitter(600_000L);
        session.sseEmitter = emitter;

        //连接返回sessionId
        try {
            emitter.send("{\"sessionId\":\"" + sessionId + "\"}");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        emitter.onCompletion(() -> completeSession(sessionId));
        emitter.onTimeout(() -> completeSession(sessionId));
        emitter.onError((ex) -> completeSession(sessionId));

        recognitionExecutor.submit(() -> {
            try {
                startRecognition(sessionId);
            } catch (Exception e) {
                emitter.completeWithError(e);
                completeSession(sessionId);
            }
        });

        return emitter;
    }

    private void startRecognition(String sessionId) throws NoApiKeyException {
        SessionState session = sessionStates.get(sessionId);
        if (session == null || session.completed) return;

        Flowable<ByteBuffer> audioSource = Flowable.create(
                emitter -> {
                    synchronized (session) {
                        session.rxEmitter = emitter;
                        // Flush buffered data
                        while (!session.audioBufferQueue.isEmpty()) {
                            ByteBuffer buffered = session.audioBufferQueue.poll();
                            if (buffered != null) emitter.onNext(buffered);
                        }
                    }
                },
                BackpressureStrategy.BUFFER
        );


        speech.streamCall(audioSource, "pcm", SAMPLE_RATE)
                .blockingForEach(result -> {
                    sendRecognitionResult(sessionId,
                            result.getSentence().getText(),
                            result.isSentenceEnd());
                });

        completeSession(sessionId);
    }

    private void sendRecognitionResult(String sessionId, String text, boolean isFinal) {
        SessionState session = sessionStates.get(sessionId);
        if (session == null || session.completed || session.sseEmitter == null) return;

        try {
            session.sseEmitter.send("{\"text\":\"" + text + "\", \"final\":" + isFinal + "}");
        } catch (IOException e) {
            completeSession(sessionId);
        }
    }

    private void completeSession(String sessionId) {
        SessionState session = sessionStates.get(sessionId);
        if (session == null) return;

        synchronized (session) {
            session.completed = true;
            if (session.rxEmitter != null && !session.rxEmitter.isCancelled()) {
                session.rxEmitter.onComplete();
            }
            if (session.sseEmitter != null) {
                session.sseEmitter.complete();
            }
            sessionStates.remove(sessionId);
        }
    }

    private void cleanInactiveSessions() {
        sessionStates.entrySet().removeIf(entry -> {
            SessionState session = entry.getValue();
            synchronized (session) {
                return session.completed || session.sseEmitter == null;
            }
        });
    }

    private static class SessionState {
        FlowableEmitter<ByteBuffer> rxEmitter;
        SseEmitter sseEmitter;
        BlockingQueue<ByteBuffer> audioBufferQueue = new LinkedBlockingQueue<>();
        volatile boolean completed = false;
    }
}
