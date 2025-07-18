package com.soultalk.controller;

import com.soultalk.service.AudioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.nio.ByteBuffer;

@Component
public class AudioWebSocket extends AbstractWebSocketHandler {
    private final int SAMPLE_RATE = 16000;

    @Autowired
    private AudioService audioService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 创建会话并发送sessionId
        String sessionId = audioService.createRecognitionSession(session, SAMPLE_RATE);
        session.sendMessage(new TextMessage("{\"type\":\"session\",\"sessionId\":\"" + sessionId + "\"}"));
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        // 处理音频数据
        ByteBuffer payload = message.getPayload();
        byte[] audioData = new byte[payload.remaining()];
        payload.get(audioData);

        String sessionId = audioService.getSessionIdBySession(session);
        if (sessionId != null) {
            audioService.uploadData(sessionId, audioData);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        // 清理会话
        String sessionId = audioService.getSessionIdBySession(session);
        if (sessionId != null) {
            audioService.completeSession(sessionId);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        // 处理传输错误
        String sessionId = audioService.getSessionIdBySession(session);
        if (sessionId != null) {
            audioService.completeSession(sessionId);
        }
    }
}