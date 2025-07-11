package com.soultalk;

import com.soultalk.aigc.MainAgentSource;
import com.soultalk.config.Configs;
import com.soultalk.mapper.MainDiaMapper;
import com.soultalk.service.MainAgentService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest
public class MemoryTest {
    @Autowired
    private MainAgentService mainAgentService;
    @Autowired
    private MainDiaMapper mainDiaMapper;

    @Test
    public void test() {
        try {
            mainDiaMapper.removeAllByUserId(1L);
            mainAgentService.resetMemory(1L);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }
}
