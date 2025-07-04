package com.soultalk.aigc;

import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import io.reactivex.Flowable;
import org.springframework.stereotype.Component;

@Component
public interface AIGCSource {
    String call(Long diaId, String question);

    Flowable<GenerationResult> streamCall(Long diaId, String question) throws NoApiKeyException, ApiException, InputRequiredException;
}
