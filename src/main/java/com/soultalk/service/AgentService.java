package com.soultalk.service;

import com.soultalk.controller.request.R;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;

public interface AgentService {
    //创建智能体
    R createAgent(HttpServletRequest request, String jsonData, MultipartFile photo);
}
