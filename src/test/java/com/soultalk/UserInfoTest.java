package com.soultalk;

import com.soultalk.po.UserEmotionRecordPO;
import com.soultalk.service.UserService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

@SpringBootTest
public class UserInfoTest {
    @Resource
    UserService userService;

    @Test
    public void testGetDetailInfo() {
        UserEmotionRecordPO userEmotionRecord = new UserEmotionRecordPO();
        userEmotionRecord.setUserId(8L);
        userEmotionRecord.setEmotion("invalid");
        userEmotionRecord.setScore(BigDecimal.valueOf(0.5f));
        userService.insertEmotionRecord(userEmotionRecord);
    }
}
