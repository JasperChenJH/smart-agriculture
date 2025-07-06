package com.soultalk.service;

import com.alibaba.fastjson.JSONObject;
import com.soultalk.po.AgentPO;
import com.soultalk.po.DiaPO;
import com.soultalk.po.MainDiaPO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

public interface MainAgentService {
    //创建对应智能体的对话，空agentId则为纯对话,返回id
    Long createDia(Long userId);

    //获取某个用户的对话
    MainDiaPO getDiaById(Long id);

    //流式请求应用
    SseEmitter streamQuestion(Long userId, String question);

    //非流式请求模型
    Map<String, String> question(Long userId, String question);

    //清空上下文
    void removeContent(Long userId, Long diaId) throws Exception;
}
