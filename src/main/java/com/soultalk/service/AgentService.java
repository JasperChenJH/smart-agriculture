package com.soultalk.service;

import com.soultalk.controller.request.R;
import com.soultalk.po.AgentPO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface AgentService {
    //创建智能体
    R createAgent(Long userId, String jsonData, MultipartFile photo);

    List<AgentPO> selectByUserId(Long userId);

    Map<String, Object> selectAgentInfo(Long agentId);

    List<AgentPO> selectLikeAgent(Long userId, String name);

    //更新智能体信息
    R updateAgent(Long agentId, String jsonData, MultipartFile photo);

    //查询自己创建的智能体
    List<AgentPO> selectMyAgent(Long user);
}
