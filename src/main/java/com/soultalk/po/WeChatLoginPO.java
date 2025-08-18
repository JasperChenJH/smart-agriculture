package com.soultalk.po;

import lombok.Data;
import org.springframework.stereotype.Component;

/**
 * @author Chenjh
 * @date 2025/8/18
 */
@Data
public class WeChatLoginPO {
    private String token;
    private String openId;
    private String sessionKey;
}
