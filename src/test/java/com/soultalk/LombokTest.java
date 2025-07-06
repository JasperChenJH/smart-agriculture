package com.soultalk;

import lombok.Data;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Data
public class LombokTest {
    private String name;
    private int age;

    @Test
    public void test() {
        LombokTest lombokTest = new LombokTest();
        lombokTest.setName("张三");
    }
}
