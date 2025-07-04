package com.soultalk;

import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.soultalk.mapper.DiaMapper;
import com.soultalk.po.DiaPO;
import com.soultalk.aigc.AIGCSource;
import com.soultalk.aigc.TongYiSource;
import io.reactivex.Flowable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SoulTalkApplicationTests {
	@Autowired
	private DiaMapper diaMapper;

	@Test
	void contextLoads() {
		AIGCSource ai=new TongYiSource();
		DiaPO dia=diaMapper.selectDiaById(1L);
        try {
			Flowable<GenerationResult> result =ai.streamCall(1L,"你是谁");
			result.blockingForEach(message -> handleGenerationResult(message));
			System.out.println(sb.toString());


        } catch (NoApiKeyException | InputRequiredException e) {
            throw new RuntimeException(e);
        }

    }

	private StringBuilder sb=new StringBuilder();
	private void handleGenerationResult(GenerationResult message) {
		String content=message.getOutput().getChoices().get(0).getMessage().getContent();
		sb.append(content);
		System.out.println(content);
	}

}
