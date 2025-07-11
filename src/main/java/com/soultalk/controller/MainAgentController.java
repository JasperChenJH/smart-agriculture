package com.soultalk.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.soultalk.context.BaseContext;
import com.soultalk.controller.request.R;
import com.soultalk.service.MainAgentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

/**
 * 主模型Controller
 */
@Slf4j
@CrossOrigin
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
            Long diaId = mainAgentService.initDia(userId);
            if (diaId == null) {
                throw new Exception("初始化对话失败");
            }

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
        return R.Success(mainAgentService.get(userId));
    }

    /**
     * 提交问题
     *
     * @return
     */
    @PostMapping("/ask")
    public SseEmitter ask(@RequestParam(value = "question") String question, @RequestParam(value = "stream", defaultValue = "0") int stream) {
        Long userId = Long.parseLong(BaseContext.getCurrentId());

        //是否流式输出
        if (stream == 1) {
            return mainAgentService.streamAsk(userId, question);
        } else {
            Map<String, String> result = mainAgentService.ask(userId, question);
            //新建sse
            SseEmitter emitter = new SseEmitter(60_000L);

            try {
                //获取到返回的消息
                JSONObject json = JSON.parseObject(result.get("answer"));
                String emotion = json.getString("emotion");
                String score = json.getString("score");
                String response= json.getString("response");

                //校验
                if(emotion == null || score == null || response == null){
                    emitter.send(SseEmitter.event().data("换个话题问问吧~"));
                    emitter.send(SseEmitter.event().data("END")); // 可选结束标记
                    emitter.complete();
                    return emitter;
                }

                //流式延迟发送
                int sample=2;//单次发送的字符数
                for (int i = 0; i < response.length(); i+= sample) {
                    emitter.send(SseEmitter.event()
                            .data(response.substring(i, Math.min(i + sample, response.length())))
                    );

                    //延迟
                    try {
                        Thread.sleep(25);
                    }catch (InterruptedException e){
                        log.error(e.getMessage());
                    }
                }

                //结束sse
                emitter.send(SseEmitter.event().data("END")); // 可选结束标记
                emitter.complete();

            } catch (IOException e) {
                log.error(e.getMessage());
                emitter.completeWithError(e);
            }

            return emitter;
        }
    }

    /**
     * 清除上下文
     *
     * @return
     */
    @GetMapping("/clear")
    public R clear() {
        try {
            Long userId = Long.parseLong(BaseContext.getCurrentId());
            mainAgentService.removeContent(userId);
            return R.Success("清除成功");
        } catch (Exception e) {
            log.error(e.getMessage());
            return R.Success(e);
        }
    }
}