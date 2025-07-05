package com.soultalk.aigc;

import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.fastjson.JSONObject;
import io.reactivex.Flowable;
import org.springframework.stereotype.Component;

public interface AIGCSource {
    String call(String modelName,String systemPrompt, JSONObject content,String question);

    Flowable<GenerationResult> streamCall(String modelName,String systemPrompt, JSONObject content,String question) throws NoApiKeyException, ApiException, InputRequiredException;
}
