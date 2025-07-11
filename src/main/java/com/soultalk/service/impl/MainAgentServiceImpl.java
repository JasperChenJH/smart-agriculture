package com.soultalk.service.impl;

import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.aliyun.core.utils.StringUtils;
import com.soultalk.aigc.MainAgent;
import com.soultalk.config.Configs;
import com.soultalk.mapper.MainDiaMapper;
import com.soultalk.mapper.UserMapper;
import com.soultalk.po.MainDiaPO;
import com.soultalk.po.UserPO;
import com.soultalk.service.MainAgentService;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.SerialDisposable;
import io.reactivex.schedulers.Schedulers;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

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
    public Flowable<String> streamAsk(Long userId, String question) {
        //初始化参数
        Params params = prepare(userId);

        //模型api
        String api = params.api;
        //长期记忆ID
        String memoryId = params.memoryId;
        //上下文
        List<Map<String, String>> messageList = params.messageList;


        // 1. 结果缓存
        StringBuilder getSb = new StringBuilder();
        StringBuilder sendSb = new StringBuilder();
        StringBuilder buffer = new StringBuilder();
        AtomicBoolean lock = new AtomicBoolean(false);//锁定是否处理json

        // 2. 创建String流
        return Flowable.create(flowEmitter -> {
            // 创建Disposable管理资源
            final SerialDisposable disposable = new SerialDisposable();
            flowEmitter.setDisposable(disposable);  // 绑定资源

            //异步生成发送
            try {
                // 将订阅与Flowable绑定
                Disposable d = agentSource.streamAppCall(api, memoryId, messageList, question)
                        .subscribeOn(Schedulers.io())  // 在IO线程处理
                        .subscribe(
                                message -> {
                                    //解析ans,假设没有think
                                    String content = message.getOutput().getText();
                                    if (content != null && !content.isEmpty()) {
                                        getSb.append(content);
                                    }

                                    //处理只返回response
                                    if (!lock.get() && getSb.toString().contains("\"response\": \"")) {
                                        lock.set(true);
                                        int index = getSb.toString().indexOf("\"response\": \"")+13;
                                        String text=getSb.substring(index, getSb.length());
                                        flowEmitter.onNext(text);
                                        sendSb.append(text);
                                        return;
                                    }

                                    if ( lock.get()&&content != null && !content.isEmpty()) {
                                        buffer.append(content); // 先加入缓存

                                        // 当缓存长度 ≥4 时，处理可判定的字符
                                        while (buffer.length() >= 6) {
                                            int safeLen = buffer.length() - 5; // 除最后5字符外的长度

                                            // 安全内容
                                            flowEmitter.onNext(buffer.substring(0, safeLen));
                                            sendSb.append(buffer, 0, safeLen);


                                            buffer.delete(0, safeLen); // 保留最后5字符继续匹配
                                        }


                                    }
                                },

                                error -> {
                                    // 错误处理
                                    log.error(error.getMessage());
                                },
                                () -> {
                                    // 检查缓存
                                    if (buffer.length() >= 6 && buffer.substring(buffer.length() - 6).equals("}\n```")) {
                                        buffer.setLength(buffer.length() - 6); // 直接丢弃结尾的 "\"}```"
                                    }
                                    flowEmitter.onNext(buffer.toString()); // 追加剩余缓存
                                    sendSb.append(buffer);

                                    flowEmitter.onComplete();

                                    //保存数据库
                                    CompletableFuture.runAsync(
                                            flowEmitter::onComplete//结束flow流
                                    ).thenRun(() -> {
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
                                        diaPO.setSentence(sendSb.toString());
                                        diaPO.setTime(System.currentTimeMillis() + 1);
                                        mainDiaMapper.insert(diaPO);
                                    }).exceptionally(e -> {
                                        log.error(e.getMessage());
                                        return null;
                                    });
                                }
                        );

                disposable.set(d);  // 绑定资源

            } catch (NoApiKeyException | InputRequiredException e) {
                log.error(e.getMessage());
            }

        }, BackpressureStrategy.BUFFER);//背压

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
