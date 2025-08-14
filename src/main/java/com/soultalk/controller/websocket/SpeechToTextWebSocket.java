package com.soultalk.controller.websocket;

import com.soultalk.service.AudioService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.nio.ByteBuffer;

/**
 * 语音转文字WebsSocket
 * https://help.aliyun.com/zh/model-studio/paraformer-speech-recognition/?spm=a2c4g.11186623.help-menu-2400256.d_2_6_3.6ba953c0wCMC1C
 */
@Slf4j
@Component
public class SpeechToTextWebSocket extends AbstractWebSocketHandler {
    // 采样率
    private final int SAMPLE_RATE = 16000;

    @Autowired
    private AudioService audioService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        try {
            // 创建会话并发送sessionId
            String sessionId = audioService.createRecognitionSession(session, SAMPLE_RATE);
            session.sendMessage(new TextMessage("{\"type\":\"session\",\"sessionId\":\"" + sessionId + "\"}"));
        } catch (Exception e) {
            log.error(e.getMessage());
            session.close(CloseStatus.SERVER_ERROR);
        }
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
        if (sessionId == null) {
            return;
        }

        // 延迟清理确保识别线程完成
        new Thread(() -> {
            try {
                // 给识别线程处理结束的时间
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
            }
            audioService.completeSession(sessionId);
        }).start();

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