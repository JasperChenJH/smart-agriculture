package com.soultalk.service;

import com.soultalk.po.DiaPO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface DiaService {
    //创建对应智能体的对话，空agentId则为纯对话,返回id
    long createDia(Long userId, Long agentId);

    //获取某个id的对话
    DiaPO getDiaById(Long id);

    //获取用户有多少个对话
    long countDiaByUserId(Long userId);

    //查找用户一定范围的对话详细
    List<DiaPO> getRangeDia(Long userId, Long start, Long end);

    //流式请求
    SseEmitter streamQuestion(Long diaId, String question);
}
