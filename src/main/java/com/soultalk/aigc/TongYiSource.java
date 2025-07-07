package com.soultalk.aigc;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.app.Application;
import com.alibaba.dashscope.app.ApplicationParam;
import com.alibaba.dashscope.app.ApplicationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.soultalk.config.Configs;
import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class TongYiSource implements AIGCSource {

    @Override
    public Map<String, String> call(String modelName, String systemPrompt, List<JSONObject> contentList, String question) throws NoApiKeyException, ApiException, InputRequiredException {
        //临时存储
        StringBuilder thkSb = new StringBuilder();
        StringBuilder ansSb = new StringBuilder();

        //请求流式的api
        this.streamCall(modelName, systemPrompt, contentList, question)
                .subscribeOn(Schedulers.io())
                //阻塞处理
                .blockingSubscribe(
                        message -> {
                            //解析think和ans
                            String content = message.getOutput().getChoices().get(0).getMessage().getContent();
                            String reason = message.getOutput().getChoices().get(0).getMessage().getReasoningContent();
                            if (content != null && !content.isEmpty()) {
                                ansSb.append(content);
                            }
                            if (reason != null && !reason.isEmpty()) {
                                thkSb.append(reason);
                            }
                        },
                        throwable -> {
                            log.error("error: {}", throwable.getMessage());
                        },
                        () -> {
                        }
                );

        //打包
        Map<String, String> result = new HashMap<>();
        result.put("think", thkSb.toString());
        result.put("answer", ansSb.toString());
        return result;
    }

    @Override
    public Flowable<GenerationResult> streamCall(String modelName, String systemPrompt, List<JSONObject> contentList, String question) throws NoApiKeyException, ApiException, InputRequiredException {
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
        String mess = "";
        for (JSONObject json : contentList) {
            for (Map.Entry<String, Object> entry : json.entrySet()) {
                if (entry.getKey().equals(Role.USER.getValue())) {
                    mess = entry.getValue().toString();
                } else if (entry.getKey().equals(Role.SYSTEM.getValue())) {
                    mess = JSON.parseObject(entry.getValue().toString(), JSONObject.class).getString("ans");
                }

                if (mess.isEmpty()) {
                    continue;
                }
                Message m = Message.builder()
                        .role(entry.getKey())
                        .content(mess)
                        .build();
                messageList.add(m);
            }
        }

        //调用API
        Generation gen = new Generation();
        GenerationParam param = GenerationParam.builder()
                .apiKey(Configs.DASHSCOPE_API_KEY)
                //选择模型
                .model(modelName)
                //本次问题
                .prompt(question)
                //历史对话
                .messages(messageList)
                // Qwen3开源版模型只支持设定为"message"
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                // 使用Qwen3开源版模型时，若未启用流式输出，请将下行取消注释，否则会报错
                .enableThinking(true)
                //限制思考长度
                .thinkingBudget(100)
                // Qwen3开源版模型只支持 true；为了更好的体验，其它模型也推荐您优先设定为 true
                .incrementalOutput(true)
                .build();

        return gen.streamCall(param);
    }

    @Override
    public Map<String, String> appCall(String appKey, List<JSONObject> contentList, String question) throws NoApiKeyException, InputRequiredException {
        //临时存储
        StringBuilder thkSb = new StringBuilder();
        StringBuilder ansSb = new StringBuilder();

        //流式转非流式
        this.streamAppCall(appKey, contentList, question)
                .subscribeOn(Schedulers.io())
                //阻塞处理
                .blockingSubscribe(
                        message -> {
                            //解析think和ans
                            String content = message.getOutput().getText();
                            String reason = message.getOutput().getThoughts().toString();
                            if (content != null && !content.isEmpty()) {
                                ansSb.append(content);
                            }
                            if (reason != null && !reason.isEmpty()) {
                                thkSb.append(reason);
                            }
                        },
                        throwable -> log.error("error: {}", throwable.getMessage()),
                        () -> {
                        }
                );

        //打包结果
        Map<String, String> result = new HashMap<>();
        result.put("think", thkSb.toString());
        result.put("answer", ansSb.toString());
        return result;
    }

    @Override
    public Flowable<ApplicationResult> streamAppCall(String appKey, List<JSONObject> contentList, String question) throws NoApiKeyException, InputRequiredException {
        List<Message> messageList = new ArrayList<>();

        //整理历史对话
        String mess = "";
        for (JSONObject json : contentList) {
            for (Map.Entry<String, Object> entry : json.entrySet()) {
                if (entry.getKey().equals(Role.USER.getValue())) {
                    mess = entry.getValue().toString();
                } else if (entry.getKey().equals(Role.SYSTEM.getValue())) {
                    mess = JSON.parseObject(entry.getValue().toString(), JSONObject.class).getString("ans");
                }

                if (mess.isEmpty()) {
                    continue;
                }
                Message m = Message.builder()
                        .role(entry.getKey())
                        .content(mess)
                        .build();
                messageList.add(m);
            }
        }

        //调用API
        ApplicationParam param = ApplicationParam.builder()
                .apiKey(Configs.DASHSCOPE_API_KEY)
                //api id
                .appId(appKey)
                //本次问题
                .prompt(question)
                //历史对话
                .messages(messageList)
                // 深度思考
                .hasThoughts(true)
                // 增量输出
                .incrementalOutput(true)
                // 替换为实际指定的知识库ID，逗号隔开多个
//                .ragOptions(RagOptions.builder()
//                        // 替换为实际指定的知识库ID，逗号隔开多个
//                        .pipelineIds(List.of("PIPELINES_ID1", "PIPELINES_ID2"))
//                        .build())
                .build();

        Application application = new Application();
        return application.streamCall(param);
    }
}
