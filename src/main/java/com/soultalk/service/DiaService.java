package com.soultalk.service;

import com.alibaba.fastjson.JSONObject;
import com.soultalk.po.AgentPO;
import com.soultalk.po.DiaPO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

public interface DiaService {
    //创建对应智能体的对话，空agentId则为纯对话,返回id
    long createDia(Long userId, Long agentId);

    //获取某个id的对话
    DiaPO getDiaById(Long id);

    //获取用户有多少个对话
    long countDiaByUserId(Long userId);

    //查找用户一定范围的对话详细
    List<DiaPO> getRangeDia(Long userId, Long start, Long end);

    //流式请求+模型/应用的请求
    SseEmitter streamQuestion(Long diaId, String question);

    SseEmitter streamModelQuestion(AgentPO agent, DiaPO diaPO, String question);

    SseEmitter streamAppQuestion(AgentPO agent, DiaPO diaPO, String question);

    //非流式请求模型
    Map<String, String> question(Long diaId, String question);

    Map<String, String> modelQuestion(AgentPO agent, DiaPO diaPO, List<JSONObject> messageList, String question);

    Map<String, String> appQuestion(AgentPO agent, DiaPO diaPO, List<JSONObject> messageList, String question);

    //清空上下文
    void removeContent(Long userId, Long diaId) throws Exception;

    //删除对话
    void removeDia(Long userId, Long diaId) throws Exception;

    //调整置顶等级
    void updateLevel(Long diaId, Integer level);
}
