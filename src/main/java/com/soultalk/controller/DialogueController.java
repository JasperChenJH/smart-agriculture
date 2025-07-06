package com.soultalk.controller;

import com.soultalk.context.BaseContext;
import com.soultalk.controller.request.R;
import com.soultalk.po.DiaPO;
import com.soultalk.service.DiaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * 对话控制器
 */
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

    //通过ID获取对话详细
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

    //非流式请求模型
    @PostMapping("/question")
    public R question(@RequestParam("id") Long diaId, @RequestParam("question") String question) {

        return R.Success(diaService.question(diaId, question));
    }

    //流式输出
    @PostMapping("/streamQuestion")
    public SseEmitter streamQuestion(@RequestParam("id") Long diaId, @RequestParam("question") String question) {
        return diaService.streamQuestion(diaId, question);
    }

    //清除上下文
    @PostMapping("/remove/content")
    public R removeContent(@RequestParam Long diaId) {
        try {
            Long userId = Long.parseLong(BaseContext.getCurrentId());
            diaService.removeContent(userId, diaId);
            return R.Success(diaId + " 清除成功");
        } catch (Exception e) {
            return R.Success(e.getMessage());
        }
    }

    //删除对话
    @PostMapping("/remove/all")
    public R removeDia(@RequestParam Long diaId) {
        try {
            Long userId = Long.parseLong(BaseContext.getCurrentId());
            diaService.removeDia(userId, diaId);
            return R.Success(diaId + " 已删除");
        } catch (Exception e) {
            return R.Success(e.getMessage());
        }
    }

    //置顶与不置顶
    @PostMapping("/update/level")
    public R updateLevel(@RequestParam Long diaId, @RequestParam Integer level) {
        try {
            diaService.updateLevel(diaId, level);
            return R.Success(diaId + " 已更新");
        } catch (Exception e) {
            return R.Failed(e.getMessage());
        }
    }
}
