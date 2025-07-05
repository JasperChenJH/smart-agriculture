package com.soultalk.aigc;

import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.app.ApplicationResult;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.fastjson.JSONObject;
import io.reactivex.Flowable;

import java.util.List;
import java.util.Map;

public interface AIGCSource {
    Map<String,String> call(String modelName, String systemPrompt, List<JSONObject> contentList, String question)throws NoApiKeyException, ApiException, InputRequiredException ;

    Flowable<GenerationResult> streamCall(String modelName, String systemPrompt, List<JSONObject> contentList, String question) throws NoApiKeyException, ApiException, InputRequiredException;

    Map<String,String> appCall(String appKey,List<JSONObject> contentList, String question)throws NoApiKeyException , InputRequiredException;

    Flowable<ApplicationResult> streamAppCall(String appKey, List<JSONObject> contentList, String question) throws NoApiKeyException, InputRequiredException;
}
