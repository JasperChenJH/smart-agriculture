package com.soultalk;

import com.soultalk.aigc.MainAgentSource;
import com.soultalk.aigc.MainAgentSourceImpl;
import com.soultalk.config.Configs;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest
public class MemoryTest {
    @Resource
    private MainAgentSourceImpl mainAgentSource;

    @Test
    public void test() {
        try {
            String[] next= {""};
            mainAgentSource.listMemory(Configs.ALI_WORKSPACE_ID,2,next);
            System.out.println(next[0]);
            } catch (Exception e) {
            log.error(e.getMessage());
        }
    }
}
