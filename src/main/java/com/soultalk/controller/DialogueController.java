package com.soultalk.controller;

import com.soultalk.context.BaseContext;
import com.soultalk.controller.request.R;
import com.soultalk.po.DiaPO;
import com.soultalk.service.DiaService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@Slf4j
@CrossOrigin
@RestController
@RequestMapping("/dia")
public class DialogueController {
    @Autowired
    private DiaService diaService;

    //新建对话
    @PostMapping("/create")
    public R createDia(@RequestParam(value = "agentId", required = false) Long agentId) {
        try {
            Long userId = Long.parseLong(BaseContext.getCurrentId());
            long i = diaService.createDia(userId, agentId);
            return R.Success(i);
        } catch (NumberFormatException e) {
            log.error(e.getMessage());
            return R.Failed(e.getMessage());
        }
    }

    //通过ID获取对话
    @GetMapping("/getDia")
    public R getDia(@RequestParam("id") Long diaId) {

        return R.Success(diaService.getDiaById(diaId));
    }

    //查找用户一定范围的对话详细
    @GetMapping("/getRangeDia")
    public R getRangeDia(@RequestParam Long start, @RequestParam Long end) {
        try {
            Long userId = Long.parseLong(BaseContext.getCurrentId());
            List<DiaPO> list = diaService.getRangeDia(userId, start, end);
            return R.Success(list);
        } catch (Exception e) {
            log.error(e.getMessage());
            return R.Failed(e.getMessage());
        }
    }

    //获取指定用户有多少个对话
    @GetMapping("/countDia")
    public R countDia() {
        Long userId = Long.parseLong(BaseContext.getCurrentId());
        return R.Success(diaService.countDiaByUserId(userId));
    }

    //流式输出
    @PostMapping("/streamQuestion")
    public SseEmitter streamQuestion(@RequestParam("id") Long diaId, @RequestParam("question") String question) {
        return diaService.streamQuestion(diaId, question);
    }

    //查找对话详细信息
//    @GetMapping("/select/info")
//    public R selectInfo(@RequestParam Integer diaId) {
//        return diaService.selectInfo(diaId);
//    }

    //清除上下文
//    @PostMapping("/remove/content")
//    public R removeContent(@RequestParam Integer diaId) {
//        return diaService.removeContent(diaId);
//    }

    //删除对话
//    @PostMapping("/remove/all")
//    public R removeDia(@RequestParam Integer diaId) {
//        return diaService.removeDia(diaId);
//    }

    //置顶与不置顶
//    @PostMapping("/update/level")
//    public R updateLevel(@RequestParam Integer diaId, @RequestParam Integer level) {
//        return diaService.updateLevel(diaId, level);
//    }

    //对话详细页
//    @GetMapping("/detail")
//    public R detail(@RequestParam Integer diaId, HttpServletRequest request) {
//        String userName = (String) request.getAttribute("username");
//        UserDTO user = userMapper.selectByName(userName);
//        if (user == null) {
//            return R.Failed("未找到用户信息");
//        }
//        return diaService.diaDetail(diaId, user);
//    }

}
