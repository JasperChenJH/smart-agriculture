package com.soultalk.service.impl;

import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson2.JSON;
import com.soultalk.aigc.AIGCSource;
import com.soultalk.mapper.AgentMapper;
import com.soultalk.mapper.DiaMapper;
import com.soultalk.po.AgentPO;
import com.soultalk.po.DiaPO;
import com.soultalk.service.DiaService;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class DiaServiceImpl implements DiaService {
    @Autowired
    private DiaMapper diaMapper;
    @Autowired
    private AgentMapper agentMapper;
    @Resource
    private AIGCSource tongYiSource;

    @Override
    public Long createDia(Long userId, Long agentId) {
        DiaPO dia = new DiaPO();
        dia.setUserId(userId);
        //检查绑定agent
        if (agentId != null && agentMapper.countById(agentId) > 0) {
            dia.setAgentId(agentId);
            dia.setIsAgent(1);
        } else {
            dia.setIsAgent(0);
        }
        dia.setContent(null);
        dia.setUpdateTime(System.currentTimeMillis());
        diaMapper.insert(dia);
        return dia.getId();
    }

    @Override
    public DiaPO getDiaById(Long id) {
        return diaMapper.selectDiaById(id);
    }

    @Override
    public Long countDiaByUserId(Long userId) {
        return diaMapper.countByUserId(userId);
    }

    @Override
    public List<DiaPO> getRangeDia(Long userId, Long start, Long end) {
        //交换start和end
        if (start > end) {
            start += end;
            end = start - end;
            start = start - end;
        }

        //调整最大id
        Long maxId = diaMapper.countByUserId(userId);
        if (end > maxId) {
            end = maxId;
        }

        Long length = end - start;
        return diaMapper.selectRangeDia(userId, start, length);
    }

    @Override
    public SseEmitter streamQuestion(Long diaId, String question) {
        //获取对话和模型情况

        AgentPO agent = null;
        DiaPO diaPO = diaMapper.selectDiaById(diaId);
        if (diaPO == null) {
            throw new RuntimeException("对话不存在");
        }

        if (diaPO.getIsAgent() == 1) {
            agent = agentMapper.selectById(diaPO.getAgentId());
        }

        //分辨是模型提问还是应用
        if (agent != null && agent.getApi() != null && !agent.getApi().isEmpty()) {
            //应用
            return streamAppQuestion(agent, diaPO, question);
        } else {
            return streamModelQuestion(agent, diaPO, question);
        }

    }

    @Override
    public SseEmitter streamModelQuestion(AgentPO agent, DiaPO diaPO, String question) {
        // 1. 创建SseEmitter（超时设为3分钟）
        SseEmitter emitter = new SseEmitter(180_000L);

        // 2. 结果缓存
        StringBuilder ansSb = new StringBuilder();
        StringBuilder thkSb = new StringBuilder();

        //确认模型
        String modelName = null;
        if (diaPO.getModel() == null) {
            if (diaPO.getIsAgent() == 1) {
                modelName = agentMapper.selectById(diaPO.getAgentId()).getModel();
            }
        } else {
            modelName = diaPO.getModel();
        }
        if (modelName == null || modelName.isEmpty()) {
            throw new RuntimeException("模型不存在");
        }

        //获取系统提示词
        String systemPrompt = null;
        if (agent != null) {
            systemPrompt = agent.getPrompt();
        }

        //获取上下文
        String str = diaPO.getContent();
        List<JSONObject> messageList;
        if (str == null || str.isEmpty()) {
            messageList = new ArrayList<>();
        } else {
            messageList = JSON.parseArray(str, JSONObject.class);
        }

        //异步生成发送
        try {
            Disposable disposable = tongYiSource.streamCall(modelName, systemPrompt, messageList, question)
                    .subscribeOn(Schedulers.io())  // 在IO线程处理
                    .subscribe(
                            message -> {
                                //解析think和ans
                                String content = message.getOutput().getChoices().get(0).getMessage().getContent();
                                String reason = message.getOutput().getChoices().get(0).getMessage().getReasoningContent();
                                if (content != null && !content.isEmpty()) {
                                    ansSb.append(content);
                                    JSONObject answerObj = new JSONObject();
                                    answerObj.put("type", "answer");
                                    answerObj.put("data", content);
                                    emitter.send(SseEmitter.event()
                                            .data(answerObj.toString())
                                            .reconnectTime(5000L)//5秒重连
                                    );
                                }
                                if (reason != null && !reason.isEmpty()) {
                                    thkSb.append(reason);
                                    JSONObject thinkObj = new JSONObject();
                                    thinkObj.put("type", "think");
                                    thinkObj.put("data", reason);
                                    emitter.send(SseEmitter.event()
                                            .data(thinkObj.toString())
                                            .reconnectTime(5000L)//5秒重连
                                    );
                                }
                            },
                            error -> {
                                // 错误处理
                                log.error(error.getMessage());
                            },
                            () -> {
                                // 流正常结束
                                // 异步执行数据库写入
                                CompletableFuture.runAsync(() -> {
                                    //打包提问JSON
                                    JSONObject ques = new JSONObject();
                                    ques.put(Role.USER.getValue(), question);
                                    //打包回答JSON
                                    JSONObject ans = new JSONObject();
                                    ans.put("thk", thkSb.toString());
                                    ans.put("ans", ansSb.toString());
                                    JSONObject answer = new JSONObject();
                                    answer.put(Role.SYSTEM.getValue(), ans);
                                    //保存数据库
                                    messageList.add(ques);
                                    messageList.add(answer);
                                    diaPO.setContent(messageList.toString());
                                    diaMapper.updateContent(diaPO.getId(), diaPO.getContent());
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
    public SseEmitter streamAppQuestion(AgentPO agent, DiaPO diaPO, String question) {
        // 1. 创建SseEmitter（超时设为3分钟）
        SseEmitter emitter = new SseEmitter(180_000L);

        // 2. 结果缓存
        StringBuilder ansSb = new StringBuilder();
        StringBuilder thkSb = new StringBuilder();

        //确认api
        if (agent == null || agent.getApi() == null) {
            throw new RuntimeException("请选择模型");
        }
        String api = agent.getApi();
        if (api.isEmpty()) {
            throw new RuntimeException("请选择模型");
        }

        //获取上下文
        String str = diaPO.getContent();
        List<JSONObject> messageList;
        if (str == null || str.isEmpty()) {
            messageList = new ArrayList<>();
        } else {
            messageList = JSON.parseArray(str, JSONObject.class);
        }

        //异步生成发送
        try {
            Disposable disposable = tongYiSource.streamAppCall(api, messageList, question)
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
                                //qwen系列模型没有的应用没有深度思考
//                                String reason= message.getOutput().getThoughts().toString();
//                                if ( reason!= null && !reason.isEmpty()) {
//                                    thkSb.append(reason);
//                                    JSONObject thinkObj = new JSONObject();
//                                    thinkObj.put("type", "think");
//                                    thinkObj.put("data", reason);
//                                    emitter.send(SseEmitter.event().data(thinkObj.toString()));
//                                }
                            },
                            error -> {
                                // 错误处理
                                log.error(error.getMessage());
                            },
                            () -> {
                                // 流正常结束
                                // 异步执行数据库写入
                                CompletableFuture.runAsync(() -> {
                                    //打包提问JSON
                                    JSONObject ques = new JSONObject();
                                    ques.put(Role.USER.getValue(), question);
                                    //打包回答JSON
                                    JSONObject ans = new JSONObject();
                                    ans.put("thk", thkSb.toString());
                                    ans.put("ans", ansSb.toString());
                                    JSONObject answer = new JSONObject();
                                    answer.put(Role.SYSTEM.getValue(), ans);
                                    //保存数据库
                                    messageList.add(ques);
                                    messageList.add(answer);
                                    diaPO.setContent(messageList.toString());
                                    diaMapper.updateContent(diaPO.getId(), diaPO.getContent());
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
    public Map<String, String> question(Long diaId, String question) {
        //获取对话和模型情况
        AgentPO agent = null;
        DiaPO diaPO = diaMapper.selectDiaById(diaId);
        if (diaPO == null) {
            throw new RuntimeException("空对话");
        }

        if (diaPO.getIsAgent() == 1) {
            agent = agentMapper.selectById(diaPO.getAgentId());
        }

        //获取上下文
        String str = diaPO.getContent();
        List<JSONObject> messageList;
        if (str == null || str.isEmpty()) {
            messageList = new ArrayList<>();
        } else {
            messageList = JSON.parseArray(str, JSONObject.class);
        }

        //识别model还是app处理
        Map<String, String> result = null;
        if (agent != null && agent.getApi() != null && !agent.getApi().isEmpty()) {
            result = this.appQuestion(agent, diaPO, messageList, question);
        } else {
            result = this.modelQuestion(agent, diaPO, messageList, question);
        }

        //打包提问JSON
        JSONObject ques = new JSONObject();
        ques.put(Role.USER.getValue(), question);
        //打包回答JSON
        JSONObject ans = new JSONObject();
        ans.put("thk", result.get("think"));
        ans.put("ans", result.get("answer"));
        JSONObject answer = new JSONObject();
        answer.put(Role.SYSTEM.getValue(), ans);
        //保存数据库
        messageList.add(ques);
        messageList.add(answer);
        diaPO.setContent(messageList.toString());
        diaMapper.updateContent(diaPO.getId(), diaPO.getContent());


        return result;
    }

    @Override
    public Map<String, String> modelQuestion(AgentPO agent, DiaPO diaPO, List<JSONObject> messageList, String question) {
        //确认模型
        String modelName = null;
        if (diaPO.getModel() == null) {
            if (diaPO.getIsAgent() == 1) {
                modelName = agentMapper.selectById(diaPO.getAgentId()).getModel();
            }
        } else {
            modelName = diaPO.getModel();
        }
        if (modelName == null || modelName.isEmpty()) {
            throw new RuntimeException("未指定模型");
        }

        //获取系统提示词
        String systemPrompt = null;
        if (agent != null) {
            systemPrompt = agent.getPrompt();
        }

        //执行请求
        Map<String, String> result = null;
        try {
            result = tongYiSource.call(modelName, systemPrompt, messageList, question);
        } catch (NoApiKeyException | InputRequiredException e) {
            log.error(e.getMessage());
        }
        return result;
    }


    @Override
    public Map<String, String> appQuestion(AgentPO agent, DiaPO diaPO, List<JSONObject> messageList, String question) {
        //确认api
        if (agent == null || agent.getApi() == null) {
            throw new RuntimeException("未指定api");
        }
        String api = agent.getApi();
        if (api.isEmpty()) {
            throw new RuntimeException("未指定api");
        }

        //执行请求
        Map<String, String> result = null;
        try {
            result = tongYiSource.appCall(api, messageList, question);
        } catch (NoApiKeyException | InputRequiredException e) {
            log.error(e.getMessage());
        }
        return result;
    }

    @Override
    public void removeContent(Long userId, Long diaId) throws Exception {
        DiaPO diaPO = diaMapper.selectDiaById(diaId);
        if (diaPO == null) {
            throw new Exception("对话不存在");
        }
        if (!diaPO.getUserId().equals(userId)) {
            throw new Exception("无权限");
        }
        diaMapper.deleteContent(diaId);
    }

    @Override
    public void removeDia(Long userId, Long diaId) throws Exception {
        DiaPO diaPO = diaMapper.selectDiaById(diaId);
        if (diaPO == null) {
            throw new Exception("对话不存在");
        }
        if (!diaPO.getUserId().equals(userId)) {
            throw new Exception("无权限");
        }
        diaMapper.delete(diaId);
    }

    @Override
    public void updateLevel(Long diaId, Integer level) {
        diaMapper.updateLevel(diaId, level);
    }

}
