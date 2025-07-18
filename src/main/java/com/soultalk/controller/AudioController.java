package com.soultalk.controller;

import com.soultalk.service.impl.AudioServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/audio")
public class AudioController {
    @Autowired
    private AudioServiceImpl audioService;

    @PostMapping(value = "/recognize", consumes = "application/octet-stream")
    public void recognize(@RequestParam String id, @RequestBody byte[] audioData) {
        audioService.uploadData(id, audioData);
    }

    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sseConnection() {
        return audioService.createRecognitionSession();
    }
}