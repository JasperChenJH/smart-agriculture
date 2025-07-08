package com.soultalk.service.impl;

import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.fastjson.JSONObject;
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

    @Override
    public SseEmitter streamAsk(Long userId, String question) {
        //确认api
        String api = Configs.MAIN_MODEL_API;

        //获取长期记忆ID
        String memoryId = null;
        UserPO userPO = userMapper.selectById(userId);
        if (userPO.getMemoryId() == null) {
            try {
                memoryId = agentSource.createMemoryId(Configs.ALI_WORKSPACE_ID, "");
                userMapper.setMemoryIdToId(userId, memoryId);
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        } else {
            memoryId = userPO.getMemoryId();
        }
        assert memoryId != null;


        //处理上下文
        List<MainDiaPO> list = mainDiaMapper.selectByUserId(userId);

        List<Map<String, String>> messageList = new ArrayList<>();
        for (MainDiaPO dia : list) {
            if (dia.getIsUser() && dia.getSentence() != null && !dia.getSentence().isEmpty()) {
                Map<String, String> m = new HashMap<>(1);
                if (dia.getIsUser()) {
                    m.put(Role.USER.getValue(), dia.getSentence());
                } else {
                    m.put(Role.SYSTEM.getValue(), dia.getSentence());
                }
                messageList.add(m);
            }
        }

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
        //确认api
        String api = Configs.MAIN_MODEL_API;

        //获取长期记忆ID
        String memoryId = null;
        UserPO userPO = userMapper.selectById(userId);
        if (userPO.getMemoryId() == null) {
            try {
                memoryId = agentSource.createMemoryId(Configs.ALI_WORKSPACE_ID, "");
                userMapper.setMemoryIdToId(userId, memoryId);
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        } else {
            memoryId = userPO.getMemoryId();
        }
        assert memoryId != null;

        //处理上下文
        List<MainDiaPO> list = mainDiaMapper.selectByUserId(userId);

        List<Map<String, String>> messageList = new ArrayList<>();
        for (MainDiaPO dia : list) {
            if (dia.getIsUser() && dia.getSentence() != null && !dia.getSentence().isEmpty()) {
                Map<String, String> m = new HashMap<>(1);
                if (dia.getIsUser()) {
                    m.put(Role.USER.getValue(), dia.getSentence());
                } else {
                    m.put(Role.SYSTEM.getValue(), dia.getSentence());
                }
                messageList.add(m);
            }
        }

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
}
