package com.soultalk.service.impl;

import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.sdk.service.bailian20231229.models.ListMemoryNodesResponseBody;
import com.soultalk.aigc.MainAgent;
import com.soultalk.config.Configs;
import com.soultalk.mapper.MainDiaMapper;
import com.soultalk.mapper.UserMapper;
import com.soultalk.po.MainDiaPO;
import com.soultalk.po.UserEmotionRecordPO;
import com.soultalk.po.UserPO;
import com.soultalk.service.MainAgentService;
import com.soultalk.service.UserService;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.SerialDisposable;
import io.reactivex.schedulers.Schedulers;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    private UserService userService;
    @Autowired
    private MainDiaMapper mainDiaMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private MainAgent mainAgent;

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
    public MainDiaPO get(Long userId, int index) {
        return mainDiaMapper.selectByUserIdAndIndex(userId, index);
    }


    @Override
    public List<MainDiaPO> getAll(Long userId) {
        return mainDiaMapper.selectAllByUserIdDesc(userId, 0, Integer.MAX_VALUE);
    }

    @Override
    public List<MainDiaPO> getRange(Long userId, Long begin, int length) {
        return mainDiaMapper.selectRangeByUserId(userId, begin, length);
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

        // 3. 构建消息session
        String sessionId = userPO.getSessionId();

        return new Params(api, memoryId, sessionId);
    }

    @Override
    public Flowable<String> streamAsk(Long userId, String question) {
        //初始化参数
        Params params = prepare(userId);

        //模型api
        String api = params.api;
        //长期记忆ID
        String memoryId = params.memoryId;
        //上下文session
        UserPO userPO = userMapper.selectById(userId);


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
                Disposable d = agentSource.streamAppCall(api, memoryId, userPO.getSessionId(), question)
                        .subscribeOn(Schedulers.io())  // 在IO线程处理
                        .subscribe(
                                message -> {
                                    if (message.getOutput().getText() != null && !message.getOutput().getSessionId().equals(userPO.getSessionId())) {
                                        userPO.setSessionId(message.getOutput().getSessionId());
                                        userMapper.update(userPO);
                                    }

                                    String content = message.getOutput().getText();

                                    //处理只返回response
                                    if (!lock.get() && getSb.toString().contains("\"response\":\"")) {
                                        lock.set(true);

                                        getSb.append(content);
                                        int index = getSb.toString().indexOf("\"response\":\"") + 12;
                                        String text = getSb.substring(index, getSb.length());

                                        //发送
                                        if (!text.isEmpty()) {
                                            flowEmitter.onNext(text);
                                            sendSb.append(text);
                                        }
                                        return;
                                    }

                                    if (lock.get() && content != null && !content.isEmpty()) {
                                        //处理换行转义
                                        StringBuilder contentSb = new StringBuilder();

                                        //获取历史1个char
                                        if (!buffer.isEmpty()) {
                                            contentSb.append(buffer.charAt(buffer.length() - 1));
                                        }
                                        contentSb.append(content);

                                        //转义
                                        content = contentSb.toString()
                                                .replace("\\n", "\\\\n")  // 替换字面"\n"为"\\n"
                                                .replace("\n", "\\\\n"); // 替换LF字符为"\\n"

                                        //删除历史的1个char
                                        if (!buffer.isEmpty()) {
                                            content = content.substring(1);
                                        }

                                        // 先加入缓存
                                        buffer.append(content);

                                        //get池中加入
                                        getSb.append(content);

                                        // 当缓存长度 ≥4 时，处理可判定的字符
                                        while (buffer.length() > 4) {
                                            int safeLen = buffer.length() - 4; // 除最后4字符外的长度

                                            // 安全内容
                                            flowEmitter.onNext(buffer.substring(0, safeLen));
                                            sendSb.append(buffer, 0, safeLen);

                                            buffer.delete(0, safeLen); // 保留最后字符继续匹配
                                        }

                                    }

                                    //get池中加入
                                    getSb.append(content);

                                },

                                error -> {
                                    // 错误处理
                                    log.error(error.getMessage());
                                },
                                () -> {
                                    // 检查缓存
                                    if (buffer.length() >= 4 && buffer.toString().contains("\"}")) {
                                        int index = buffer.indexOf("\"}");
                                        buffer.setLength(index); // 直接丢弃结尾的 "}
                                    }
                                    //最后一包
                                    if (!buffer.isEmpty()) {
                                        flowEmitter.onNext(buffer.toString()); // 追加剩余缓存
                                        sendSb.append(buffer);
                                    }

                                    flowEmitter.onComplete();

                                    //获取情绪分数等
                                    UserEmotionRecordPO record = new UserEmotionRecordPO(null, userId, "无效情绪", null, question, 0, LocalDateTime.now());
                                    try {
                                        //切割出JSON
                                        int l = getSb.indexOf("{");
                                        int r = getSb.lastIndexOf("}");
                                        JSONObject json = JSON.parseObject(getSb.substring(l, r + 1));
                                        String emotion = (String) json.getOrDefault("emotion", "无效情绪");
                                        record.setEmotion(emotion);


                                        String score = (String) json.getOrDefault("score", "5");
                                        record.setScore(new BigDecimal(score));

                                        String response = (String) json.getOrDefault("response", "");
                                        record.setStatus(response.isEmpty() ? 0 : 1);
                                    } catch (Exception e) {
                                        log.error(e.getMessage());
                                        record.setStatus(0);
                                    } finally {
                                        //插入分数
                                        userService.insertEmotionRecord(record);
                                    }

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
        String sessionId = params.sessionId;

        //执行请求
        Map<String, String> result = null;
        try {
            result = agentSource.appCall(api, memoryId, sessionId, question);
        } catch (NoApiKeyException | InputRequiredException e) {
            log.error(e.getMessage());
            return result;
        }

        //保存sessionId
        UserPO userPO = userMapper.selectById(userId);
        userPO.setSessionId(result.getOrDefault("sessionId", null));
        userMapper.update(userPO);

        //保存数据库 输入
        MainDiaPO diaPO1 = new MainDiaPO();
        diaPO1.setUserId(userId);
        diaPO1.setIsUser(true);
        diaPO1.setSentence(question);
        diaPO1.setTime(System.currentTimeMillis());
        mainDiaMapper.insert(diaPO1);

        //保存数据库 输出
        UserEmotionRecordPO record = new UserEmotionRecordPO(null, userId, "无效情绪", BigDecimal.ZERO, question, 0, LocalDateTime.now());
        String response = null;
        try {
            //切割出JSON
            String answer = result.get("answer");
            int l = answer.indexOf("{");
            int r = answer.lastIndexOf("}");
            JSONObject json = JSON.parseObject(answer.substring(l, r + 1));
            String emotion = (String) json.getOrDefault("emotion", "无效情绪");
            record.setEmotion(emotion);

            String score = (String) json.getOrDefault("score", "5");
            record.setScore(new BigDecimal(score));

            response = (String) json.getOrDefault("response", "");
            record.setStatus(response.isEmpty() ? 0 : 1);
        } catch (Exception e) {
            log.error(e.getMessage());
            record.setStatus(0);
        } finally {
            //插入分数
            userService.insertEmotionRecord(record);
        }

        MainDiaPO diaPO2 = new MainDiaPO();
        diaPO2.setUserId(userId);
        diaPO2.setIsUser(false);
        diaPO2.setSentence(response);
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
    public JSONObject listMemoryNodes(Long userId) {
        JSONObject json = new JSONObject();

        try {
            UserPO userPO = userMapper.selectById(userId);
            List<ListMemoryNodesResponseBody.MemoryNodes> list = mainAgent.listMemoryNodes(Configs.ALI_WORKSPACE_ID, userPO.getMemoryId(), 50, null);
            int i = 0;
            for (ListMemoryNodesResponseBody.MemoryNodes memoryNodes : list) {
                json.put(i + "0", memoryNodes.getMemoryNodeId());
                json.put(i + "1", memoryNodes.getContent());
                i++;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return json;
    }

    @Override
    public String resetMemory(Long userId) {
        try {
            UserPO userPO = userMapper.selectById(userId);

            String newMemoryId = agentSource.clearMemory(Configs.ALI_WORKSPACE_ID, userPO.getMemoryId());
            userPO.setMemoryId(newMemoryId);

            userMapper.update(userPO);
            return newMemoryId;
        } catch (Exception e) {
            log.error(e.getMessage());
            return null;
        }
    }

    private record Params(String api, String memoryId, String sessionId) {
    }
}