package com.soultalk.config;

import com.soultalk.controller.websocket.SpeechToTextWebSocket;
import com.soultalk.controller.websocket.TextToSpeechWebSocket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    @Autowired
    private SpeechToTextWebSocket speechToTextWebSocket;
    @Autowired
    private TextToSpeechWebSocket textToSpeechWebSocket;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(speechToTextWebSocket, "/ws/stt")
                .setAllowedOrigins("*");
        registry.addHandler(textToSpeechWebSocket, "/ws/tts")
                .setAllowedOrigins("*");
    }
}