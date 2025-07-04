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
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class TongYiSource implements AIGCSource {
    @Autowired
    private DiaMapper diaMapper;
    @Autowired
    private AgentMapper agentMapper;

    @Override
    public String call(Long diaId, String question) {
        return "";
    }

    @Override
    public Flowable<GenerationResult> streamCall(Long diaId, String question) throws NoApiKeyException, ApiException, InputRequiredException {
        //获取对话和智能体
        DiaPO diaPO = diaMapper.selectDiaById(diaId);
        assert diaPO != null;

        //获取历史对话
        List<Message> messageList = new ArrayList<>();
        JSONObject preMessage = new JSONObject();
        if (diaPO.getContent() != null && !diaPO.getContent().isEmpty()) {
            preMessage = JSONObject.parseObject(diaPO.getContent(), JSONObject.class);
        }

        preMessage.put(Role.USER.getValue(), question);
        for (String key : preMessage.keySet()) {
            Message m = Message.builder()
                    .role(key)
                    .content(preMessage.getString(key))
                    .build();
            messageList.add(m);
        }

        //写入本次问题
        messageList.add(Message.builder()
                .role(Role.USER.getValue())
                .content(question)
                .build()
        );

        //调用API
        Generation gen = new Generation();
        GenerationParam param = GenerationParam.builder()
                .apiKey(Configs.DASHSCOPE_API_KEY)
                .model(diaPO.getModel())
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
