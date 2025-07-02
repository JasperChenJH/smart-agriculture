package com.soultalk.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.soultalk.context.BaseContext;
import com.soultalk.controller.request.R;
import com.soultalk.mapper.AgentMapper;
import com.soultalk.mapper.UserMapper;
import com.soultalk.po.AgentPO;
import com.soultalk.service.AgentService;
import com.soultalk.service.BaseService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
public class AgentServiceImpl implements AgentService {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private BaseService baseService;
    @Autowired
    private AgentMapper agentMapper;


    @Override
    public R createAgent(HttpServletRequest request, String jsonData, MultipartFile photo) {
        //补充agent信息
        long userId;
        AgentPO agent;
        try {
            userId = Long.parseLong(BaseContext.getCurrentId());//获取用户ID
            agent = JSONObject.parseObject(jsonData, AgentPO.class);
        } catch (Exception e) {
            return R.Failed("数据格式有错误！");
        }

        //解析图片
        String url = baseService.saveFileToOSS(photo.getOriginalFilename(), photo);
        if (url != null) {
            //url = Configs.PHOTO_URL + url; //本地
            agent.setPhoto(url);
        }

        //创建者
        agent.setCreator(userId);
        agent.setCreateTime(System.currentTimeMillis());

        //是否公开
        try {
            if (agent.getIsPublic() == null) {
                agent.setIsPublic(0);
            }
        } catch (NullPointerException e) {
            agent.setIsPublic(0);
        }

        //保存智能体
        agentMapper.insert(agent);
        return R.Success(agent.getName() + " 智能体已成功创建");
    }
}
