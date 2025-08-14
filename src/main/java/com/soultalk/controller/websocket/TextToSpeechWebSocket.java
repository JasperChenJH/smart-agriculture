package com.soultalk.controller.websocket;

import com.alibaba.fastjson2.JSON;
import com.alibaba.nls.client.AccessToken;
import com.alibaba.nls.client.protocol.NlsClient;
import com.alibaba.nls.client.protocol.OutputFormatEnum;
import com.alibaba.nls.client.protocol.SampleRateEnum;
import com.alibaba.nls.client.protocol.tts.SpeechSynthesizer;
import com.alibaba.nls.client.protocol.tts.SpeechSynthesizerListener;
import com.alibaba.nls.client.protocol.tts.SpeechSynthesizerResponse;
import com.soultalk.config.Configs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文字转语音WebSocket
 * https://help.aliyun.com/zh/isi/developer-reference/sdk-for-java-9?spm=a2c4g.11186623.help-menu-30413.d_3_1_0_3.77731d63Sh4VoL
 */
@Slf4j
@Component
public class TextToSpeechWebSocket extends AbstractWebSocketHandler {
    //config
    private final String gatewayUrl = "wss://nls-gateway-cn-shanghai.aliyuncs.com/ws/v1";
    private final OutputFormatEnum type = OutputFormatEnum.MP3;
    private final SampleRateEnum sampleRate = SampleRateEnum.SAMPLE_RATE_16K;

    // 存储活跃的TTS会话
    private final Map<String, SpeechSynthesizer> activeSessions = new ConcurrentHashMap<>();
    private final NlsClient nlsClient;
    private final String appKey;

    public TextToSpeechWebSocket() {
        AccessToken accessToken = new AccessToken(Configs.ALI_ACCESSKEY_ID, Configs.ALI_ACCESSKEY_SECRET);
        try {
            accessToken.apply();
            this.nlsClient = new NlsClient(gatewayUrl, accessToken.getToken());
        } catch (IOException e) {
            throw new RuntimeException("连接TTS client失败", e);
        }
        this.appKey = Configs.ALI_TTS_APP_KEY;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        //解析建立参数
        String query = Objects.requireNonNull(session.getUri()).getQuery();
        Map<String, String> params = new HashMap<>();
        if (query != null && !query.isEmpty()) {
            for (String pair : query.split("&")) {
                int idx = pair.indexOf("=");
                if (idx > 0) {
                    String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                    String value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                    params.put(key, value);
                }
            }
        }

        //声音角色
        String speaker = params.getOrDefault("speaker", "chuangirl");
        //音调±500
        int pitch = Integer.parseInt(params.getOrDefault("pitch", "0"));
        //语速±500
        int rate = Integer.parseInt(params.getOrDefault("rate", "0"));
        //音量
        int volume = Integer.parseInt(params.getOrDefault("volume", "100"));

        // 创建唯一会话ID
        String sessionId = UUID.randomUUID().toString();
        session.getAttributes().put("sessionId", sessionId);

        // 初始化TTS合成器
        SpeechSynthesizerListener listener = createListener(session);
        SpeechSynthesizer synthesizer = new SpeechSynthesizer(nlsClient, listener);

        // 设置TTS参数
        synthesizer.setAppKey(appKey);
        synthesizer.setFormat(type);
        synthesizer.setSampleRate(sampleRate);
        synthesizer.setVoice(speaker);
        synthesizer.setPitchRate(pitch);
        synthesizer.setSpeechRate(rate);
        synthesizer.setVolume(volume);

        // 存储会话
        activeSessions.put(sessionId, synthesizer);

        // 通知前端会话已建立
        Map<String, Object> response = new HashMap<>();
        response.put("type", "session");
        response.put("sessionId", sessionId);
        response.put("sampleRate", 16000);
        session.sendMessage(new TextMessage(JSON.toJSON(response).toString()));
    }

    private SpeechSynthesizerListener createListener(WebSocketSession session) {
        return new SpeechSynthesizerListener() {
            private final String sessionId = (String) session.getAttributes().get("sessionId");

            @Override
            public void onMessage(ByteBuffer audioBuffer) {
                try {
                    if (session.isOpen()) {
                        // 发送数据
                        byte[] audioData = new byte[audioBuffer.remaining()];
                        audioBuffer.get(audioData);
                        session.sendMessage(new BinaryMessage(audioData));
                    }
                } catch (IOException e) {
                    log.error(e.getMessage());
                }
            }

            @Override
            public void onComplete(SpeechSynthesizerResponse response) {
                try {
                    if (session.isOpen()) {
                        // 通知前端合成完成
                        Map<String, Object> endMessage = new HashMap<>();
                        endMessage.put("type", "synthesis_complete");
                        endMessage.put("sessionId", sessionId);
                        session.sendMessage(new TextMessage(JSON.toJSON(endMessage).toString()));
                    }
                } catch (IOException e) {
                    log.error(e.getMessage());
                }
            }

            @Override
            public void onFail(SpeechSynthesizerResponse response) {
                try {
                    if (session.isOpen()) {
                        // 通知前端合成失败
                        Map<String, Object> errorMessage = new HashMap<>();
                        errorMessage.put("type", "synthesis_error");
                        errorMessage.put("sessionId", sessionId);
                        errorMessage.put("code", response.getStatus());
                        errorMessage.put("message", response.getStatusText());
                        session.sendMessage(new TextMessage(JSON.toJSON(errorMessage).toString()));
                    }
                } catch (IOException e) {
                    log.error(e.getMessage());
                }
            }
        };
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String sessionId = (String) session.getAttributes().get("sessionId");
        if (sessionId == null || !activeSessions.containsKey(sessionId)) {
            session.sendMessage(new TextMessage("{\"error\":\"session不存在\"}"));
            return;
        }

        // 解析前端发送的文本
        Map<String, Object> payload = JSON.parseObject(message.getPayload());
        String text = (String) payload.get("text");

        if (text == null || text.isEmpty()) {
            session.sendMessage(new TextMessage("{\"error\":\"文本为空\"}"));
            return;
        }

        // 获取该会话的TTS合成器
        SpeechSynthesizer synthesizer = activeSessions.get(sessionId);

        // 设置要合成的文本
        synthesizer.setText(text);

        // 开始合成
        synthesizer.start();

        // 通知前端开始合成
        Map<String, Object> response = new HashMap<>();
        response.put("type", "synthesis_start");
        response.put("sessionId", sessionId);
        session.sendMessage(new TextMessage(JSON.toJSON(response).toString()));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String sessionId = (String) session.getAttributes().get("sessionId");
        if (sessionId != null && activeSessions.containsKey(sessionId)) {
            // 关闭TTS合成器并清理资源
            SpeechSynthesizer synthesizer = activeSessions.remove(sessionId);
            synthesizer.close();
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        String sessionId = (String) session.getAttributes().get("sessionId");
        if (sessionId != null) {
            try {
                Map<String, Object> response = new HashMap<>();
                response.put("type", "error");
                response.put("sessionId", sessionId);
                response.put("message", "Transport error: " + exception.getMessage());
                session.sendMessage(new TextMessage(JSON.toJSON(response).toString()));
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        }
    }
}