package com.soultalk.controller;

import com.soultalk.context.BaseContext;
import com.soultalk.controller.request.R;
import com.soultalk.po.AgentPO;
import com.soultalk.service.AgentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 智能体控制器
 */
@Slf4j
@CrossOrigin
@RestController
@RequestMapping("/agent")
public class AgentController {
    @Autowired
    private AgentService agentService;


    //创建智能体
    @PostMapping("/create")
    public R create(@RequestParam("jsonData") String jsonData, @RequestParam("photo") MultipartFile photo) {
        try {
            Long userId = Long.parseLong(BaseContext.getCurrentId());
            return agentService.createAgent(userId, jsonData, photo);
        } catch (Exception e) {
            log.error(e.getMessage());
            return R.Failed(e.getMessage());
        }
    }

    //查询用户可见的所有智能体
    @GetMapping("/select/all")
    public R selectAll() {
        Long userId = Long.parseLong(BaseContext.getCurrentId());
        List<AgentPO> list = agentService.selectByUserId(userId);
        return R.Success(list);
    }

    //查找指定智能体
    @GetMapping("/select/info")
    public R selectAgentInfo(@RequestParam Long agentId) {
        Map<String, Object> map = agentService.selectAgentInfo(agentId);
        if (map == null) {
            return R.Failed("智能体不存在");
        } else {
            return R.Success(map);
        }
    }

    //搜索智能体(模糊匹配)
    @GetMapping("/select/like")
    public R selectAll(@RequestParam String name) {
        Long userId = Long.parseLong(BaseContext.getCurrentId());
        List<AgentPO> list = agentService.selectLikeAgent(userId, name);
        return R.Success(list);
    }

    //更新智能体
    @PostMapping("/update")
    public R updateAgent(@RequestParam("jsonData") String jsonData, @RequestParam("photo") MultipartFile photo) {
        try {
            Long userId = Long.parseLong(BaseContext.getCurrentId());
            return agentService.updateAgent(userId, jsonData, photo);
        } catch (Exception e) {
            log.error(e.getMessage());
            return R.Failed(e.getMessage());
        }
    }

    //查询个人创建的智能体
    @GetMapping("/select/myself")
    public R selectMyself() {
        Long user = Long.parseLong(BaseContext.getCurrentId());
        List<AgentPO> agentList = agentService.selectMyAgent(user);
        return R.Success(agentList);
    }

}
