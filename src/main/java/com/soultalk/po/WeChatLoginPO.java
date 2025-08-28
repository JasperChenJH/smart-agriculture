package com.soultalk.po;

import lombok.Data;

/**
 * @author Chenjh
 * @date 2025/8/18
 */
@Data
public class WeChatLoginPO {
    // 登录凭证
    private String token;
    // 微信 openid
    private String openId;
    // 微信session_key
    private String sessionKey;
    // 0:新用户 1:老用户
    private Integer isOldUser;
}
