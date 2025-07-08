package com.soultalk;

import com.soultalk.service.DiaService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SoulTalkApplicationTests {
    @Resource
    private DiaService diaService;

    @Test
    void contextLoads() {

    }

}
