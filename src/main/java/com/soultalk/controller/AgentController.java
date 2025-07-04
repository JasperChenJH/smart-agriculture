package com.soultalk.controller;

import com.soultalk.context.BaseContext;
import com.soultalk.controller.request.R;
import com.soultalk.mapper.UserMapper;
import com.soultalk.service.AgentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@CrossOrigin
@RestController
@RequestMapping("/agent")
public class AgentController {
    @Autowired
    private AgentService agentService;
    @Autowired
    private UserMapper userMapper;

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

    //查询所有智能体
//    @GetMapping("/select/all")
//    public R selectAll(HttpServletRequest request) {
//        String username = (String) request.getAttribute("username");
//        List<AgentDTO> list = agentService.selectAllAgentComplex(username);
//        return R.Success(list);
//    }

    //查找指定智能体
//    @GetMapping("/select/info")
//    public R selectAgentInfo(@RequestParam Integer agentId) {
//        Map<String, Object> map = agentService.selectAgentInfo(agentId);
//        if (map == null) {
//            return R.Failed("智能体不存在");
//        } else {
//            return R.Success(map);
//        }
//    }

    //查询智能体(模糊匹配)
//    @GetMapping("/select/like")
//    public R selectAll(HttpServletRequest request, @RequestParam String name) {
//        String username = (String) request.getAttribute("username");
//        UserDTO user = userMapper.selectByName(username);
//        List<AgentInfoDTO> list = agentService.selectLikeAgent(user, name);
//        return R.Success(list);
//    }

    //更新智能体
//    @PostMapping("/update")
//    public R updateAgent(HttpServletRequest request, @RequestParam("jsonData") String jsonData, @RequestParam("file") MultipartFile file) throws IOException {
//        return agentService.updateAgent(request, jsonData, file);
//    }

    //查询个人创建的智能体
//    @GetMapping("/select/myself")
//    public R selectMyself(HttpServletRequest request) {
//        String username = (String) request.getAttribute("username");
//        UserDTO user = userMapper.selectByName(username);
//        List<AgentDTO> agentList = agentService.selectMyAgent(user);
//        return R.Success(agentList);
//    }

}
