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
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.*;

@Service
@Slf4j
public class AudioServiceImpl implements AudioService {
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

    @Override
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

    @Override
    public String createRecognitionSession(WebSocketSession session, int sampleRate) {
        String sessionId = UUID.randomUUID().toString();
        SessionState sessionState = new SessionState();
        sessionState.webSocketSession = session;
        sessionStates.put(sessionId, sessionState);

        recognitionExecutor.submit(() -> {
            try {
                startRecognition(sessionId, sampleRate);
            } catch (Exception e) {
                log.error("Recognition error", e);
                completeSession(sessionId);
            }
        });

        return sessionId;
    }

    @Override
    public String getSessionIdBySession(WebSocketSession session) {
        for (ConcurrentMap.Entry<String, SessionState> entry : sessionStates.entrySet()) {
            if (entry.getValue().webSocketSession == session) {
                return entry.getKey();
            }
        }
        return null;
    }

    @Override
    public WebSocketSession getSessionBySessionId(String sessionId) {
        SessionState sessionState = sessionStates.get(sessionId);
        if (sessionState == null) {
            return null;
        } else {
            return sessionState.webSocketSession;
        }
    }

    private void startRecognition(String sessionId, int sampleRate) throws NoApiKeyException {
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

        speech.streamCall(audioSource, "pcm", sampleRate)
                .blockingForEach(result -> {
                    sendRecognitionResult(sessionId,
                            result.getSentence().getText(),
                            result.isSentenceEnd());
                });

        completeSession(sessionId);
    }

    private void sendRecognitionResult(String sessionId, String text, boolean isFinal) {
        SessionState session = sessionStates.get(sessionId);
        if (session == null || session.completed || session.webSocketSession == null) return;

        try {
            String json = "{\"type\":\"result\",\"text\":\"" + text + "\",\"final\":" + isFinal + "}";
            session.webSocketSession.sendMessage(new TextMessage(json));
        } catch (IOException e) {
            log.error("Failed to send WebSocket message", e);
            completeSession(sessionId);
        }
    }

    @Override
    public void completeSession(String sessionId) {
        SessionState session = sessionStates.get(sessionId);
        if (session == null) return;

        synchronized (session) {
            session.completed = true;
            if (session.rxEmitter != null && !session.rxEmitter.isCancelled()) {
                session.rxEmitter.onComplete();
            }
            if (session.webSocketSession != null && session.webSocketSession.isOpen()) {
                try {
                    session.webSocketSession.close();
                } catch (IOException e) {
                    log.error("Error closing WebSocket session", e);
                }
            }
            sessionStates.remove(sessionId);
        }
    }

    private void cleanInactiveSessions() {
        sessionStates.entrySet().removeIf(entry -> {
            SessionState session = entry.getValue();
            synchronized (session) {
                return session.completed || session.webSocketSession == null;
            }
        });
    }

    private static class SessionState {
        FlowableEmitter<ByteBuffer> rxEmitter;
        WebSocketSession webSocketSession;
        BlockingQueue<ByteBuffer> audioBufferQueue = new LinkedBlockingQueue<>();
        volatile boolean completed = false;
    }
}