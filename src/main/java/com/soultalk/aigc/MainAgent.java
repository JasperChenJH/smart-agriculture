package com.soultalk.aigc;

import com.alibaba.dashscope.app.ApplicationResult;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.aliyun.sdk.service.bailian20231229.models.ListMemoryNodesResponseBody;
import io.reactivex.Flowable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public interface MainAgent {
    Flowable<ApplicationResult> streamAppCall(String appKey, String memoryId, String sessionId, String question) throws NoApiKeyException, InputRequiredException;

    Map<String, String> appCall(String appKey, String memoryId, String sessionId, String question) throws NoApiKeyException, InputRequiredException;

    Flowable<ApplicationResult> streamAppCall(String appKey, String memoryId, List<Map<String, String>> conntentList, String question) throws NoApiKeyException, InputRequiredException;

    Map<String, String> appCall(String appKey, String memoryId, List<Map<String, String>> conntentList, String question) throws NoApiKeyException, InputRequiredException;

    //创建长期记忆ID
    String createMemoryId(String workspaceId, String description) throws Exception;

    //创建记忆片段
    String createMemoryNode(String workspaceId, String memoryId, String content) throws ExecutionException, InterruptedException;

    //获取长期记忆体
    Map<String, Object> getMemory(String workspaceId, String memoryId) throws Exception;

    //获取记忆体中的记忆片段
    List<ListMemoryNodesResponseBody.MemoryNodes> listMemoryNodes(String workspaceId, String memoryId, int length, String nextToken) throws Exception;

    //删除长期记忆体
    void removeMemory(String workspaceId, String memoryId) throws Exception;

    //清空长期记忆体（返回新ID）
    String clearMemory(String workspaceId, String memoryId) throws Exception;

    //列出所有长期记忆（workspace，单页个数，上次返回的token（引用））
    List<Map<String, String>> listMemory(String workspaceId, Integer depart, String[] nextToken) throws Exception;

    //更新长期记忆体的description
    void updateMemoryDescription(String workspaceId, String memoryId, String description) throws Exception;
}
