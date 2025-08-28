package com.soultalk.service.impl;

import com.alibaba.dashscope.app.ApplicationOutput;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.sdk.service.bailian20231229.models.ListMemoryNodesResponseBody;
import com.soultalk.aigc.MainAgent;
import com.soultalk.config.Configs;
import com.soultalk.controller.request.R;
import com.soultalk.mapper.MainDiaMapper;
import com.soultalk.mapper.UserMapper;
import com.soultalk.po.MainDiaPO;
import com.soultalk.po.UserEmotionRecordPO;
import com.soultalk.po.UserInfoPO;
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
import org.springframework.context.annotation.Lazy;
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
    @Lazy  // 延迟注入
    @Resource
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
        AtomicBoolean lock = new AtomicBoolean(false); // 锁定是否处理json
        AtomicBoolean isFinish = new AtomicBoolean(false);

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
                                    final ApplicationOutput output = message.getOutput();

                                    //校验切换session
                                    if (output.getText() != null && !output.getSessionId().equals(userPO.getSessionId())) {
                                        userPO.setSessionId(output.getSessionId());
                                        userMapper.update(userPO);
                                    }

                                    // 校验内容
                                    String content = output.getText();
                                    if (content == null) {
                                        return;
                                    }

                                    getSb.append(content);

                                    // 监测开始头
                                    final String getContent = getSb.toString();
                                    if (!lock.get() && getContent.contains("\"response\":\"")) {
                                        lock.set(true);

                                        int startIndex = getContent.indexOf("\"response\":\"") + 12;
                                        if (startIndex < getContent.length()) {
                                            String response = getContent.substring(startIndex);
                                            flowEmitter.onNext(response);
                                            sendSb.append(response);
                                        }
                                        return;
                                    }

                                    // 非response内容检查
                                    if (!lock.get()) {
                                        return;
                                    }

                                    //转义
                                    if (buffer.isEmpty()) {
                                        content = content
                                                .replace("\\n", "\\\\n")
                                                .replace("\n", "\\\\n");
                                    } else {
                                        char lastChar = buffer.charAt(buffer.length() - 1);
                                        content = (lastChar + content)
                                                .replace("\\n", "\\\\n")
                                                .replace("\n", "\\\\n")
                                                .substring(1);
                                    }

                                    //提前跳出发送流程
                                    if (isFinish.get()) {
                                        return;
                                    }

                                    buffer.append(content);

                                    // 结束标记检测
                                    final int endMarkIndex = buffer.indexOf("\",\"emo");
                                    if (endMarkIndex != -1) {
                                        String remaining = buffer.substring(0, endMarkIndex);
                                        if (!remaining.isEmpty()) {
                                            flowEmitter.onNext(remaining);
                                            sendSb.append(remaining);
                                        }

                                        isFinish.set(true);
                                        return;
                                    }

                                    // 滑动窗口
                                    if (buffer.length() > 7) {
                                        int safeLen = buffer.length() - 7;
                                        String safePart = buffer.substring(0, safeLen);

                                        //发送
                                        if (!safePart.isEmpty()) {
                                            flowEmitter.onNext(safePart);
                                            sendSb.append(safePart);
                                        }

                                        buffer.delete(0, safeLen);
                                    }

                                },

                                error -> {
                                    // 错误处理
                                    log.error(error.getMessage());
                                },
                                () -> {
                                    flowEmitter.onComplete();

                                    //获取情绪分数等
                                    UserEmotionRecordPO record = new UserEmotionRecordPO(null, userId, null, null, question, 0, LocalDateTime.now());
                                    try {
                                        //切割出JSON
                                        int l = getSb.indexOf("{");
                                        int r = getSb.lastIndexOf("}");
                                        JSONObject json = JSON.parseObject(getSb.substring(l, r + 1));
                                        String emotion = (String) json.getOrDefault("emotion", "无效情绪");
                                        record.setEmotion(emotion);

                                        BigDecimal score = (BigDecimal) json.getOrDefault("score", "5");
                                        record.setScore(score);

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
                JSONObject j = new JSONObject();
                j.put("id", memoryNodes.getMemoryNodeId());
                j.put("content", memoryNodes.getContent());
                json.put(i + "", j);
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

    @Override
    public R uploadInfoToMemory(Long userId) {
        UserInfoPO userInfoPO = userService.getDetailInfo(userId);
        UserPO userPO = userMapper.selectById(userId);

        //校验
        if (userInfoPO == null) {
            return R.Failed("未找到该ID");
        }

        String content = "记住用户的个人信息：" +
                "昵称：" +
                userInfoPO.getNickName() +
                ";" +
                "性别：" +
                ("1".equals(userInfoPO.getSex()) ? "男" : "女") +
                ";" +
                "生日：" +
                userInfoPO.getBirthday() +
                ";" +
                "年龄：" +
                userInfoPO.getAge() +
                ";" +
                "星座：" +
                userInfoPO.getZodiac() +
                ";" +
                "MBTI人格类型：" +
                userInfoPO.getPersonalityType() +
                ";" +
                "居住地址：" +
                (userInfoPO.getCountry() == null ? "" : userInfoPO.getCountry()) +
                (userInfoPO.getProvince() == null ? "" : userInfoPO.getProvince()) +
                (userInfoPO.getCity() == null ? "" : userInfoPO.getCity()) +
                ";" +
                "兴趣爱好：" +
                userInfoPO.getHobbies() +
                ";" +
                "病史：" +
                userInfoPO.getMedicalHistory() +
                ";";
        try {
            //写入新nodeId
            String oldNodeId = userPO.getMemoryInfoId();
            String newNodeId = mainAgent.createMemoryNode(Configs.ALI_WORKSPACE_ID, userPO.getMemoryId(), content);
            userPO.setMemoryInfoId(newNodeId);

            //校验nodeId是否存在
            if (oldNodeId != null && !oldNodeId.isEmpty()) {
                mainAgent.removeMemoryNode(Configs.ALI_WORKSPACE_ID, userPO.getMemoryId(), oldNodeId);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            return R.Failed(e.getMessage());
        } finally {
            //回写数据库
            userMapper.update(userPO);
        }

        return R.Success("已同步个人预设到大模型");
    }

    private record Params(String api, String memoryId, String sessionId) {
    }
}