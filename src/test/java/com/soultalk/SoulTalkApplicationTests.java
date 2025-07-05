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

	@Test
	void contextLoads() {

    }

}
