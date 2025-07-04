package com.soultalk.service;

import com.soultalk.controller.request.R;
import org.springframework.web.multipart.MultipartFile;

public interface AgentService {
    //创建智能体
    R createAgent(Long userId, String jsonData, MultipartFile photo);
}
