package com.soultalk.aigc;

import com.alibaba.dashscope.audio.asr.recognition.RecognitionResult;
import com.alibaba.dashscope.exception.NoApiKeyException;
import io.reactivex.Flowable;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.nio.ByteBuffer;

public interface Speech {
    String fileCall(String filePath) throws UnsupportedAudioFileException, IOException;

    Flowable<RecognitionResult> streamCall(Flowable<ByteBuffer> audioFrame, String audioType, Integer sampleRate) throws NoApiKeyException;
}
