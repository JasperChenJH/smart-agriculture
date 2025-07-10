package com.soultalk.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.soultalk.controller.request.R;
import com.soultalk.mapper.AgentMapper;
import com.soultalk.po.AgentPO;
import com.soultalk.service.AgentService;
import com.soultalk.service.BaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class AgentServiceImpl implements AgentService {
    @Autowired
    private BaseService baseService;
    @Autowired
    private AgentMapper agentMapper;


    @Override
    public R createAgent(Long userId, String jsonData, MultipartFile photo) {
        //补充agent信息
        AgentPO agent;
        try {
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
            if (agent.getPub() == null) {
                agent.setPub(0);
            }
        } catch (NullPointerException e) {
            agent.setPub(0);
        }

        //api和模型必有一个
        if (agent.getModel() == null && agent.getApi() == null) {
            return R.Failed("数据结构错误");
        }

        //保存智能体
        agentMapper.insert(agent);
        return R.Success(agent.getName() + " 智能体已成功创建");
    }

    @Override
    public List<AgentPO> selectByUserId(Long userId) {
        return agentMapper.selectByUserId(userId);
    }

    @Override
    public Map<String, Object> selectAgentInfo(Long agentId) {
        AgentPO agent = agentMapper.selectById(agentId);
        if (agent == null) {
            return null;
        }

        return (Map<String, Object>) JSONObject.toJSON(agent);
    }

    @Override
    public List<AgentPO> selectLikeAgent(Long userId, String name) {
        List<AgentPO> list = agentMapper.selectLikeByName(name);
        list.removeIf(agent -> !agent.getCreator().equals(userId) && agent.getPub() == 0);
        return list;
    }

    @Override
    public R updateAgent(Long agentId, String jsonData, MultipartFile photo) {
        AgentPO agent = agentMapper.selectById(agentId);

        //读取agent信息
        AgentPO newAgentInfo;
        try {
            newAgentInfo = JSONObject.parseObject(jsonData, AgentPO.class);
        } catch (Exception e) {
            return R.Failed("数据格式有错误！");
        }

        //逐个校验数据
        if (newAgentInfo.getName() != null) {
            agent.setName(newAgentInfo.getName());
        }
        if (newAgentInfo.getIntroduction() != null) {
            agent.setIntroduction(newAgentInfo.getIntroduction());
        }
        if (newAgentInfo.getCreator() != null) {
            agent.setCreator(newAgentInfo.getCreator());
        }
        if (newAgentInfo.getPub() != null) {
            agent.setPub(newAgentInfo.getPub());
        }
        if (newAgentInfo.getModel() != null) {
            agent.setModel(newAgentInfo.getModel());
        }
        if (newAgentInfo.getPrompt() != null) {
            agent.setPrompt(newAgentInfo.getPrompt());
        }
        if (newAgentInfo.getApi() != null) {
            agent.setApi(newAgentInfo.getApi());
        }

        //解析图片
        String oldPhotoUrl = agent.getPhoto();
        try {
            if (!photo.isEmpty()) {
                String url = baseService.saveFileToOSS(photo.getOriginalFilename(), photo);

                if (url == null) throw new Exception("图片上传失败");
                agent.setPhoto(url);
                baseService.removeFileFromOSS(oldPhotoUrl);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            agent.setPhoto(oldPhotoUrl);
        }

        //api和模型必有一个
        if (agent.getModel() == null && agent.getApi() == null) {
            return R.Failed("数据结构错误");
        }

        //保存智能体
        agent.setCreateTime(System.currentTimeMillis());
        agentMapper.update(agent);
        return R.Success(agent.getName() + " 智能体信息已更新");
    }

    /**
     * 获取用户创建的智能体
     *
     * @param userId
     * @return
     */
    @Override
    public List<AgentPO> selectMyAgent(Long userId) {
        return agentMapper.selectMyAgent(userId);
    }
}
