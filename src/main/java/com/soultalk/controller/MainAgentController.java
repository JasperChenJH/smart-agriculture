package com.soultalk.controller;

import com.soultalk.context.BaseContext;
import com.soultalk.controller.request.R;
import com.soultalk.service.MainAgentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RestController
@RequestMapping("/main")
public class MainAgentController {
    @Autowired
    private MainAgentService mainAgentService;

    /**
     * 创建主模型的对话
     */
    @PostMapping("/create")
    public R create() {
        Long userId = Long.parseLong(BaseContext.getCurrentId());
        try {
            Long diaId = mainAgentService.createDia(userId);
            assert diaId != null;

            return R.Success(diaId + " 已创建");
        } catch (Exception e) {
            log.error(e.getMessage());
            return R.Success(e.getMessage());
        }
    }

    /**
     * 获取主模型的对话
     *
     * @return
     */
    @GetMapping("/get")
    public R get() {
        Long userId = Long.parseLong(BaseContext.getCurrentId());

    }

    /**
     * 提交问题
     *
     * @return
     */
    @PostMapping("/ask")
    public SseEmitter ask() {
        return null;
    }

    /**
     * 清除上下文
     *
     * @return
     */
    @GetMapping("/clear")
    public R clear() {
        return R.Success();
    }
}