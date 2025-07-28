package com.soultalk.aigc;

import com.alibaba.dashscope.app.Application;
import com.alibaba.dashscope.app.ApplicationOutput;
import com.alibaba.dashscope.app.ApplicationParam;
import com.alibaba.dashscope.app.ApplicationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.aliyun.auth.credentials.Credential;
import com.aliyun.auth.credentials.provider.StaticCredentialProvider;
import com.aliyun.sdk.service.bailian20231229.AsyncClient;
import com.aliyun.sdk.service.bailian20231229.models.*;
import com.soultalk.config.Configs;
import darabonba.core.client.ClientOverrideConfiguration;
import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Slf4j
public class MainAgentSource implements MainAgent {

    //建立长期记忆HTTP异步连接
    private static AsyncClient getAsyncClient() {
        StaticCredentialProvider provider = StaticCredentialProvider.create(Credential.builder()
                //OSS的密钥通用
                .accessKeyId(Configs.Ali_ACCESSKEY_ID)
                .accessKeySecret(Configs.Ali_ACCESSKEY_SECRET)
                .build());

        AsyncClient client = AsyncClient.builder()
                .region("cn-beijing") // Region ID
                .credentialsProvider(provider)
                .overrideConfiguration(
                        ClientOverrideConfiguration.create()
                                // Endpoint 请参考 https://api.aliyun.com/product/bailian
                                .setEndpointOverride("bailian.cn-beijing.aliyuncs.com")
                        //.setConnectTimeout(Duration.ofSeconds(30))
                )
                .build();
        return client;
    }

    @Override
    public Flowable<ApplicationResult> streamAppCall(String appKey, String memoryId, String sessionId, String question) throws NoApiKeyException, InputRequiredException {
        //带上时间戳
        SimpleDateFormat sdf24 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        question = "[当前时间 " + sdf24.format(new Date()) + "] " + question;

        //调用API
        ApplicationParam param = ApplicationParam.builder()
                .apiKey(Configs.DASHSCOPE_API_KEY)
                //api id
                .appId(appKey)
                //本次问题
                .prompt(question)
                //历史对话
                .sessionId(sessionId)
                // 长期记忆id
                .memoryId(memoryId)
                // 增量输出
                .incrementalOutput(true)
                // 替换为实际指定的知识库ID，逗号隔开多个
//                .ragOptions(RagOptions.builder()
//                        // 替换为实际指定的知识库ID，逗号隔开多个
//                        .pipelineIds(List.of("PIPELINES_ID1", "PIPELINES_ID2"))
//                        .build())
                .build();

        Application application = new Application();
        return application.streamCall(param);
    }

    @Override
    public Map<String, String> appCall(String appKey, String memoryId, String sessionId, String question) throws NoApiKeyException, InputRequiredException {
        Map<String, String> result = new HashMap<>();

        //临时存储
        StringBuilder thkSb = new StringBuilder();
        StringBuilder ansSb = new StringBuilder();

        // 添加流处理完成标记
        AtomicBoolean completed = new AtomicBoolean(false);

        //流式转非流式
        this.streamAppCall(appKey, memoryId, sessionId, question)
                .subscribeOn(Schedulers.io())
                //阻塞处理
                .blockingSubscribe(
                        message -> {
                            if (message.getOutput().getSessionId() != null) {
                                result.put("sessionId", message.getOutput().getSessionId());
                            }

                            //解析think和ans
                            String content = message.getOutput().getText();
                            if (content != null && !content.isEmpty()) {
                                ansSb.append(content);
                            }
                            List<ApplicationOutput.Thought> thoughtList = message.getOutput().getThoughts();
                            if (thoughtList != null && thoughtList.size() == 1 && thoughtList.get(0) != null && thoughtList.get(0).getThought() != null) {
                                thkSb.append(thoughtList.get(0).getThought());
                            }
                        },
                        throwable -> {
                            log.error("error: {}", throwable.getMessage());
                            completed.set(true);
                        },
                        () -> {
                            completed.set(true);
                        }
                );

        //阻塞等待
        while (!completed.get()) {
            Thread.yield();
        }

        //打包结果
        result.put("think", thkSb.toString());
        result.put("answer", ansSb.toString());
        return result;
    }

    @Override
    public Flowable<ApplicationResult> streamAppCall(String appKey, String memoryId, List<Map<String, String>> conntentList, String question) throws NoApiKeyException, InputRequiredException {
        List<Message> messageList = new ArrayList<>();

        //整理历史对话
        for (Map<String, String> map : conntentList) {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                //构建单个语句
                Message mess = Message.builder()
                        .role(entry.getKey())
                        .content(entry.getValue())
                        .build();

                if (mess != null && !mess.getContent().isEmpty()) {
                    messageList.add(mess);
                }
            }
        }

        //调用API
        ApplicationParam param = ApplicationParam.builder()
                .apiKey(Configs.DASHSCOPE_API_KEY)
                //api id
                .appId(appKey)
                //本次问题
                .prompt(question)
                //历史对话
                .messages(messageList)
                // 长期记忆id
                .memoryId(memoryId)
                // 增量输出
                .incrementalOutput(true)
                // 替换为实际指定的知识库ID，逗号隔开多个
//                .ragOptions(RagOptions.builder()
//                        // 替换为实际指定的知识库ID，逗号隔开多个
//                        .pipelineIds(List.of("PIPELINES_ID1", "PIPELINES_ID2"))
//                        .build())
                .build();

        Application application = new Application();
        return application.streamCall(param);
    }

    @Override
    public Map<String, String> appCall(String appKey, String memoryId, List<Map<String, String>> conntentList, String question) throws NoApiKeyException, InputRequiredException {
        //临时存储
        StringBuilder thkSb = new StringBuilder();
        StringBuilder ansSb = new StringBuilder();

        // 添加流处理完成标记
        AtomicBoolean completed = new AtomicBoolean(false);

        //流式转非流式
        this.streamAppCall(appKey, memoryId, conntentList, question)
                .subscribeOn(Schedulers.io())
                //阻塞处理
                .blockingSubscribe(
                        message -> {
                            //解析think和ans
                            String content = message.getOutput().getText();
                            if (content != null && !content.isEmpty()) {
                                ansSb.append(content);
                            }
                            List<ApplicationOutput.Thought> thoughtList = message.getOutput().getThoughts();
                            if (thoughtList != null && thoughtList.size() == 1 && thoughtList.get(0) != null && thoughtList.get(0).getThought() != null) {
                                thkSb.append(thoughtList.get(0).getThought());
                            }
                        },
                        throwable -> {
                            log.error("error: {}", throwable.getMessage());
                            completed.set(true);
                        },
                        () -> {
                            completed.set(true);
                        }
                );

        //阻塞等待
        while (!completed.get()) {
            Thread.yield();
        }

        //打包结果
        Map<String, String> result = new HashMap<>();
        result.put("think", thkSb.toString());
        result.put("answer", ansSb.toString());
        return result;
    }

    @Override
    public String createMemoryId(String workspaceId, String description) throws Exception {
        AsyncClient client = getAsyncClient();

        CreateMemoryRequest createMemoryRequest = CreateMemoryRequest.builder()
                .workspaceId(workspaceId)
                .description(description)
                .build();

        CompletableFuture<CreateMemoryResponse> response = client.createMemory(createMemoryRequest);
        CreateMemoryResponse resp = response.get();
        client.close();

        return resp.getBody().getMemoryId();
    }

    @Override
    public String createMemoryNode(String workspaceId, String memoryId, String content) throws ExecutionException, InterruptedException {
        AsyncClient client = getAsyncClient();

        CreateMemoryNodeRequest createMemoryNodeRequest = CreateMemoryNodeRequest.builder()
                .workspaceId(workspaceId)
                .memoryId(memoryId)
                .content(content)
                .build();

        CompletableFuture<CreateMemoryNodeResponse> response = client.createMemoryNode(createMemoryNodeRequest);
        CreateMemoryNodeResponse resp = response.get();
        client.close();

        return resp.getBody().getMemoryNodeId();
    }

    @Override
    public Map<String, Object> getMemory(String workspaceId, String memoryId) throws Exception {
        AsyncClient client = getAsyncClient();

        GetMemoryRequest getMemoryRequest = GetMemoryRequest.builder()
                .workspaceId(workspaceId)
                .memoryId(memoryId)
                .build();

        CompletableFuture<GetMemoryResponse> response = client.getMemory(getMemoryRequest);
        GetMemoryResponse resp = response.get();
        client.close();
        return resp.getBody().toMap();
    }

    @Override
    public List<ListMemoryNodesResponseBody.MemoryNodes> listMemoryNodes(String workspaceId, String memoryId, int length, String nextToken) throws Exception {
        if (length > 50) {
            length = 50;
        }

        AsyncClient client = getAsyncClient();

        ListMemoryNodesRequest listMemoryNodesRequest = ListMemoryNodesRequest.builder()
                .workspaceId(workspaceId)
                .memoryId(memoryId)
                .maxResults(length)
                .nextToken(nextToken)
                .build();

        CompletableFuture<ListMemoryNodesResponse> response = client.listMemoryNodes(listMemoryNodesRequest);
        ListMemoryNodesResponse resp = response.get();
        client.close();

        return resp.getBody().getMemoryNodes();
    }

    @Override
    public void removeMemory(String workspaceId, String memoryId) throws Exception {
        AsyncClient client = getAsyncClient();

        DeleteMemoryRequest deleteMemoryRequest = DeleteMemoryRequest.builder()
                .workspaceId(workspaceId)
                .memoryId(memoryId)
                .build();

        CompletableFuture<DeleteMemoryResponse> response = client.deleteMemory(deleteMemoryRequest);
        DeleteMemoryResponse resp = response.get();
        client.close();
    }

    @Override
    public String clearMemory(String workspaceId, String memoryId) throws Exception {
        //暂存description
        String description = "";

        try {
            Map<String, Object> map = this.getMemory(workspaceId, memoryId);
            if (map != null && map.containsKey("description")) {
                description = String.valueOf(map.get("description"));
            }
            this.removeMemory(workspaceId, memoryId);
        } catch (Exception e) {
            log.error("error: {}", e.getMessage());

            //notfound就继续创建
            if (!e.getMessage().contains("MemoryIdNotFound")) {
                throw e;
            }
        }

        //生成新ID
        try {
            return this.createMemoryId(workspaceId, description);
        } catch (Exception e) {
            log.error("error: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public void removeMemoryNode(String workspaceId, String memoryId, String memoryNodeId) throws Exception {
        AsyncClient client = getAsyncClient();

        DeleteMemoryNodeRequest deleteMemoryNodeRequest = DeleteMemoryNodeRequest.builder()
                .workspaceId(workspaceId)
                .memoryId(memoryId)
                .memoryNodeId(memoryNodeId)
                .build();

        CompletableFuture<DeleteMemoryNodeResponse> response = client.deleteMemoryNode(deleteMemoryNodeRequest);
        DeleteMemoryNodeResponse resp = response.get();
    }

    @Override
    public List<Map<String, String>> listMemory(String workspaceId, Integer depart, String[] nextToken) throws Exception {
        if (nextToken == null || nextToken.length != 1) {
            throw new Exception("nextToken长度为1并为数组");
        }

        AsyncClient client = getAsyncClient();

        ListMemoriesRequest listMemoriesRequest = ListMemoriesRequest.builder()
                .workspaceId(workspaceId)
                .maxResults(depart)
                .nextToken(nextToken[0])
                .build();

        CompletableFuture<ListMemoriesResponse> response = client.listMemories(listMemoriesRequest);
        ListMemoriesResponse resp = response.get();
        client.close();

        Map<String, Object> map = resp.getBody().toMap();
        nextToken[0] = (String) map.getOrDefault("nextToken", "");
        return (List<Map<String, String>>) map.get("memories");
    }

    @Override
    public void updateMemoryDescription(String workspaceId, String memoryId, String description) throws Exception {
        AsyncClient client = getAsyncClient();

        UpdateMemoryRequest updateMemoryRequest = UpdateMemoryRequest.builder()
                .workspaceId(workspaceId)
                .memoryId(memoryId)
                .description(description)
                .build();

        CompletableFuture<UpdateMemoryResponse> response = client.updateMemory(updateMemoryRequest);
        UpdateMemoryResponse resp = response.get();
        client.close();
    }
}
