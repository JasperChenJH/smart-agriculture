package com.soultalk.aigc;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.fastjson.JSONObject;
import com.soultalk.config.Configs;
import com.soultalk.mapper.AgentMapper;
import com.soultalk.mapper.DiaMapper;
import com.soultalk.po.DiaPO;
import io.reactivex.Flowable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class TongYiSource implements AIGCSource {

    @Override
    public String call(String modelName, String systemPrompt, JSONObject content, String question) {
        return "";
    }

    @Override
    public Flowable<GenerationResult> streamCall(String modelName, String systemPrompt, JSONObject content, String question) throws NoApiKeyException, ApiException, InputRequiredException {
        List<Message> messageList = new ArrayList<>();

        //写入系统提示词
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            messageList.add(Message.builder()
                    .role(Role.SYSTEM.getValue())
                    .content(systemPrompt)
                    .build()
            );
        }

        //整理历史对话
        content.put(Role.USER.getValue(), question);
        for (Map.Entry<String, Object> map : content.entrySet()) {
            String mes = null;
            if (map.getKey().equals(Role.USER.getValue())) {
                mes = (String) map.getValue();
            } else {
                mes =JSONObject.parseObject((String)map.getValue()).getString("ans");
            }

            if (mes == null || mes.isEmpty()) continue;
            Message m = Message.builder()
                    .role(map.getKey())
                    .content(mes)
                    .build();
            messageList.add(m);
        }

        //调用API
        Generation gen = new Generation();
        GenerationParam param = GenerationParam.builder()
                .apiKey(Configs.DASHSCOPE_API_KEY)
                .model(modelName)
                .messages(messageList)
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                // Qwen3模型通过enable_thinking参数控制思考过程（开源版默认True，商业版默认False）
                // 使用Qwen3开源版模型时，若未启用流式输出，请将下行取消注释，否则会报错
                //.enableThinking(false)
                .incrementalOutput(true)
                .build();

        return gen.streamCall(param);
    }
}
