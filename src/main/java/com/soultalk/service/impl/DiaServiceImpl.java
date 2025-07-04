package com.soultalk.service.impl;

import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.fastjson.JSONObject;
import com.soultalk.aigc.AIGCSource;
import com.soultalk.mapper.AgentMapper;
import com.soultalk.mapper.DiaMapper;
import com.soultalk.po.DiaPO;
import com.soultalk.service.DiaService;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class DiaServiceImpl implements DiaService {
    @Autowired
    private DiaMapper diaMapper;
    @Autowired
    private AgentMapper agentMapper;
    @Autowired
    private AIGCSource aigcSource;

    @Override
    public long createDia(Long userId, Long agentId) {
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
    public long countDiaByUserId(Long userId) {
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
        // 1. 创建SseEmitter（超时设为3分钟）
        SseEmitter emitter = new SseEmitter(180_000L);

        // 2. 订阅Flowable
        StringBuilder sb = new StringBuilder();

        DiaPO diaPO = diaMapper.selectDiaById(diaId);
        assert diaPO != null;
        JSONObject preMessage;
        if (diaPO.getContent() != null && !diaPO.getContent().isEmpty()) {
            preMessage = JSONObject.parseObject(diaPO.getContent(), JSONObject.class);
        } else {
            preMessage = new JSONObject();
        }

        try {
            Disposable disposable = aigcSource.streamCall(diaId, question)
                    .subscribeOn(Schedulers.io())  // 在IO线程处理
                    .subscribe(
                            result -> {
                                // 发送单条 构造SSE消息格式
                                String eventData = result.getOutput().getChoices().get(0).getMessage().getContent();
                                emitter.send(SseEmitter.event()
                                        .data(eventData)
                                );
                            },
                            error -> {
                                // 错误处理
                                log.error(error.getMessage());
                            },
                            () -> {
                                // 流正常结束
                                // 异步执行数据库写入
                                CompletableFuture.runAsync(() -> {
                                    preMessage.put(Role.SYSTEM.getValue(), sb.toString());
                                    diaPO.setContent(preMessage.toString());
                                    diaMapper.insert(diaPO);
                                }).thenRun(() -> {
                                    try {
                                        emitter.send(SseEmitter.event().name("complete").data("END")); // 可选结束标记
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
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

}
