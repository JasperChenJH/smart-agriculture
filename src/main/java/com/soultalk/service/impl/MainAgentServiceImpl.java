package com.soultalk.service.impl;

import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.core.utils.StringUtils;
import com.soultalk.aigc.MainAgent;
import com.soultalk.config.Configs;
import com.soultalk.mapper.MainDiaMapper;
import com.soultalk.mapper.UserMapper;
import com.soultalk.po.MainDiaPO;
import com.soultalk.po.UserPO;
import com.soultalk.service.MainAgentService;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class MainAgentServiceImpl implements MainAgentService {
    @Resource
    private MainAgent agentSource;
    @Autowired
    private MainDiaMapper mainDiaMapper;
    @Autowired
    private UserMapper userMapper;

    @Override
    public Long initDia(Long userId) {
        MainDiaPO dia = new MainDiaPO();
        dia.setUserId(userId);
        //设置第一次的系统问候语
        dia.setIsUser(false);
        dia.setSentence("你好哇");
        dia.setTime(System.currentTimeMillis());
        mainDiaMapper.insert(dia);
        return dia.getId();
    }

    @Override
    public List<MainDiaPO> get(Long userId) {
        return mainDiaMapper.selectByUserId(userId);
    }

    /**
     * 初始化参数
     */
    private Params prepare(Long userId) {
        // 1. 设置API
        final String api = Configs.MAIN_MODEL_API;

        // 2. 处理记忆ID
        UserPO userPO = userMapper.selectById(userId);
        String memoryId = userPO.getMemoryId();
        if (memoryId == null) {
            createMemoryId(userId, Configs.ALI_WORKSPACE_ID + " " + userPO.getId());
            userPO = userMapper.selectById(userId);
            memoryId = userPO.getMemoryId();
        }
        if (memoryId == null) {
            throw new RuntimeException("MEMORY_ID错误");
        }

        // 3. 构建消息列表
        List<Map<String, String>> messageList = new ArrayList<>();

        List<MainDiaPO> dialogList = mainDiaMapper.selectByUserId(userId);
        //限定长度
        for (int i = 0; i < Configs.MODEL_CONTEXT_ROUND && i < dialogList.size(); i++) {
            MainDiaPO dia = dialogList.get(i);
            if (dia.getIsUser() && !StringUtils.isEmpty(dia.getSentence())) {
                Map<String, String> messageMap = new HashMap<>(1);
                messageMap.put(Role.USER.getValue(), dia.getSentence());
                messageList.add(messageMap);
            }
        }

        return new Params(api, memoryId, messageList);
    }

    @Override
    public SseEmitter streamAsk(Long userId, String question) {
        //初始化参数
        Params params = prepare(userId);

        //模型api
        String api = params.api;
        //长期记忆ID
        String memoryId = params.memoryId;
        //上下文
        List<Map<String, String>> messageList = params.messageList;

        // 1. 创建SseEmitter（超时设为3分钟）
        SseEmitter emitter = new SseEmitter(180_000L);

        // 2. 结果缓存
        StringBuilder ansSb = new StringBuilder();

        //异步生成发送
        try {
            Disposable disposable = agentSource.streamAppCall(api, memoryId, messageList, question)
                    .subscribeOn(Schedulers.io())  // 在IO线程处理
                    .subscribe(
                            message -> {
                                //解析think和ans
                                String content = message.getOutput().getText();
                                if (content != null && !content.isEmpty()) {
                                    ansSb.append(content);
                                    JSONObject answerObj = new JSONObject();
                                    answerObj.put("type", "answer");
                                    answerObj.put("data", content);
                                    emitter.send(SseEmitter.event().data(answerObj.toString()));
                                }
                            },
                            error -> {
                                // 错误处理
                                log.error(error.getMessage());
                            },
                            () -> {
                                // 流正常结束
                                //保存数据库
                                CompletableFuture.runAsync(() -> {
                                    MainDiaPO diaPO = new MainDiaPO();
                                    diaPO.setUserId(userId);
                                    diaPO.setIsUser(true);
                                    diaPO.setSentence(question);
                                    diaPO.setTime(System.currentTimeMillis());
                                    mainDiaMapper.insert(diaPO);
                                }).thenRun(() -> {
                                    MainDiaPO diaPO = new MainDiaPO();
                                    diaPO.setUserId(userId);
                                    diaPO.setIsUser(false);
                                    diaPO.setSentence(ansSb.toString());
                                    diaPO.setTime(System.currentTimeMillis() + 1);
                                    mainDiaMapper.insert(diaPO);
                                }).thenRun(() -> {
                                    try {
                                        emitter.send(SseEmitter.event().data("END")); // 可选结束标记
                                    } catch (IOException e) {
                                        log.error(e.getMessage());
                                    }
                                    emitter.complete();
                                }).exceptionally(e -> {
                                    emitter.completeWithError(e);
                                    return null;
                                });
                            }
                    );


            // 3. 绑定清理逻辑（客户端断开时取消订阅）
            emitter.onCompletion(disposable::dispose);
            emitter.onTimeout(disposable::dispose);

        } catch (NoApiKeyException | InputRequiredException e) {
            log.error(e.getMessage());
        }
        return emitter;
    }

    @Override
    public Map<String, String> ask(Long userId, String question) {
        //初始化参数
        Params params = prepare(userId);

        //模型api
        String api = params.api;
        //长期记忆ID
        String memoryId = params.memoryId;
        //上下文
        List<Map<String, String>> messageList = params.messageList;

        //执行请求
        Map<String, String> result = null;
        try {
            result = agentSource.appCall(api, memoryId, messageList, question);
        } catch (NoApiKeyException | InputRequiredException e) {
            log.error(e.getMessage());
            return result;
        }

        //保存数据库
        MainDiaPO diaPO1 = new MainDiaPO();
        diaPO1.setUserId(userId);
        diaPO1.setIsUser(true);
        diaPO1.setSentence(question);
        diaPO1.setTime(System.currentTimeMillis());
        mainDiaMapper.insert(diaPO1);

        MainDiaPO diaPO2 = new MainDiaPO();
        diaPO2.setUserId(userId);
        diaPO2.setIsUser(false);
        diaPO2.setSentence(result.get("answer"));
        diaPO2.setTime(System.currentTimeMillis());
        mainDiaMapper.insert(diaPO2);

        return result;
    }

    @Override
    public void removeContent(Long userId) throws Exception {
        Integer count = mainDiaMapper.countByUserId(userId);
        if (count == null || count <= 0) {
            throw new Exception("对话不存在");
        }
        mainDiaMapper.removeAllByUserId(userId);
    }

    @Override
    public void createMemoryId(Long userId, String description) {
        String memoryId = null;
        try {
            memoryId = agentSource.createMemoryId(Configs.ALI_WORKSPACE_ID, description);
            userMapper.setMemoryIdToId(userId, memoryId);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    @Override
    public String getMemoryId(Long userId) {
        UserPO userPO = userMapper.selectById(userId);
        return userPO.getMemoryId();
    }

    @Override
    public void resetMemory(Long userId) {
        try {
            UserPO userPO = userMapper.selectById(userId);
            if (userPO.getMemoryId() == null) {
                throw new Exception("未获取到记忆ID");
            }

            String newMemoryId = agentSource.clearMemory(Configs.ALI_WORKSPACE_ID, userPO.getMemoryId());
            userPO.setMemoryId(newMemoryId);
            userMapper.update(userPO);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    private record Params(String api, String memoryId, List<Map<String, String>> messageList) {
    }
}
