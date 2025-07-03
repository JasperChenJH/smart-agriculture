package com.soultalk.service;

import com.soultalk.controller.request.R;

public interface DiaService {
    //创建对应智能体的对话，空agentId则为纯对话,返回id
    long createDia(Long userId, Long agentId);
}
