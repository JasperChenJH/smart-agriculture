package com.soultalk.service;

import org.springframework.web.socket.WebSocketSession;

public interface AudioService {
    void uploadData(String id, byte[] audioData);

    String createRecognitionSession(WebSocketSession session, int sampleRate);

    String getSessionIdBySession(WebSocketSession session);

    WebSocketSession getSessionBySessionId(String sessionId);

    void completeSession(String sessionId);
}
