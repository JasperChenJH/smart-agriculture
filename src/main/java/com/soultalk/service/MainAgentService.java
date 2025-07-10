package com.soultalk.service;

import com.soultalk.po.MainDiaPO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

public interface MainAgentService {
    //创建第一个问候对话,返回id
    Long initDia(Long userId);

    //获取某个用户的对话
    List<MainDiaPO> get(Long userId);

    //流式请求应用
    SseEmitter streamAsk(Long userId, String question);

    //非流式请求模型
    Map<String, String> ask(Long userId, String question);

    //清空上下文
    void removeContent(Long userId) throws Exception;

    //创建长期记忆ID
    void createMemoryId(Long userId, String description);

    //获取长期记忆ID
    String getMemoryId(Long userId);

    //重置长期记忆
    void resetMemory(Long userId);
}