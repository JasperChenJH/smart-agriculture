package com.soultalk.service;

import com.alibaba.fastjson.JSONObject;
import com.soultalk.controller.request.R;
import com.soultalk.po.MainDiaPO;
import io.reactivex.Flowable;

import java.util.List;
import java.util.Map;

public interface MainAgentService {
    //创建第一个问候对话,返回id
    Long initDia(Long userId);

    //获取某个用户的对话
    MainDiaPO get(Long userId, int index);

    //获取用户所有对话
    List<MainDiaPO> getAll(Long userId);

    //获取用户范围的对话
    List<MainDiaPO> getRange(Long userId, Long begin, int length);

    //流式请求应用
    Flowable<String> streamAsk(Long userId, String question);

    //非流式请求模型
    Map<String, String> ask(Long userId, String question);

    //清空上下文
    void removeContent(Long userId) throws Exception;

    //创建长期记忆ID
    void createMemoryId(Long userId, String description);

    //获取长期记忆ID
    String getMemoryId(Long userId);

    //获取长期记忆片段
    JSONObject listMemoryNodes(Long userId);

    //重置长期记忆
    String resetMemory(Long userId);

    //上传用户信息到长期记忆
    R uploadInfoToMemory(Long userId);
}