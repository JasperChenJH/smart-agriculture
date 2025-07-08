package com.soultalk;

import com.soultalk.service.UserService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class UserInfoTest {
    @Resource
    UserService userService;

    @Test
    public void testGetDetailInfo() {

    }
}
