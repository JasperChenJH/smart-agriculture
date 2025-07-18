package com.soultalk.aigc;

import com.alibaba.dashscope.audio.asr.recognition.Recognition;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionParam;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionResult;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.soultalk.config.Configs;
import io.reactivex.Flowable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;

@Slf4j
@Component
public class SpeechSource implements Speech {

    @Override
    public String fileCall(String filePath) throws UnsupportedAudioFileException, IOException {
        File file = new File(filePath);
        if (!file.exists()) return null;

        //解析采样率
        int sampleRate = 16000;
        String type;
        try (AudioInputStream audioStream = AudioSystem.getAudioInputStream(file)) {
            sampleRate = (int) audioStream.getFormat().getSampleRate();
            type = filePath.split("\\.")[filePath.split("\\.").length - 1];
        }

        // 创建RecognitionParam
        RecognitionParam param =
                RecognitionParam.builder()
                        // 若没有将API Key配置到环境变量中，需将下面这行代码注释放开，并将apiKey替换为自己的API Key
                        .apiKey(Configs.DASHSCOPE_API_KEY)
                        .model("paraformer-realtime-v2")
                        .format(type)
                        .sampleRate(sampleRate)
                        // “language_hints”只支持paraformer-realtime-v2模型
                        .parameter("language_hints", new String[]{"zh", "en"})
                        //设置是否过滤语气词
                        .disfluencyRemovalEnabled(false)
                        .parameters(Map.of(
                                //true语义断句准确性更高,false断句延迟较低，适合交互场景
                                "enable_semantic_segmentation", true,
                                //静音分句
                                "max_sentence_silence", 800,
                                //设置是否在识别结果中自动添加标点
                                "punctuation_prediction_enabled", true,
                                //需要与服务端保持长连接
                                "heartbeat", false,
                                //开启后，中文数字将转换为阿拉伯数字
                                "inverse_text_normalization_enabled", false
                        ))
                        .build();

        // 创建Recognition实例
        Recognition recognizer = new Recognition();

        try {
            return recognizer.call(param, file);
        } catch (Exception e) {
            log.error("error:", e);
            return null;
        }
    }

    @Override
    public Flowable<RecognitionResult> streamCall(Flowable<ByteBuffer> audioFrame, String audioType, Integer sampleRate) throws NoApiKeyException {
        // 创建RecognitionParam
        RecognitionParam param =
                RecognitionParam.builder()
                        // 若没有将API Key配置到环境变量中，需将下面这行代码注释放开，并将apiKey替换为自己的API Key
                        .apiKey(Configs.DASHSCOPE_API_KEY)
                        .model("paraformer-realtime-v2")
                        .format(audioType)
                        .sampleRate(sampleRate)
                        // “language_hints”只支持paraformer-realtime-v2模型
                        .parameter("language_hints", new String[]{"zh", "en"})
                        //设置是否过滤语气词
                        .disfluencyRemovalEnabled(false)
                        .parameters(Map.of(
                                //true语义断句准确性更高,false断句延迟较低，适合交互场景
                                "enable_semantic_segmentation", true,
                                //静音分句
                                "max_sentence_silence", 800,
                                //设置是否在识别结果中自动添加标点
                                "punctuation_prediction_enabled", true,
                                //需要与服务端保持长连接
                                "heartbeat", false,
                                //开启后，中文数字将转换为阿拉伯数字
                                "inverse_text_normalization_enabled", true
                        ))
                        .build();

        // 创建Recognition实例
        Recognition recognizer = new Recognition();

        return recognizer.streamCall(param, audioFrame);
    }
}
