package com.soultalk.po;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Date;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class UserInfoPO {
    /**
     * 主键
     */
    private Integer id;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 昵称
     */
    private String nickName;

    /**
     * 性别 1--男,0--女
     */
    private String sex;

    /**
     * 出生日期
     */

    private LocalDate birthday;

    /**
     * 年龄
     */
    private Integer age;

    /**
     * 星座
     */
    private String zodiac;

    /**
     * MBTI人格类型
     */
    private String personalityType;

    /**
     * 用户爱好
     */
    private String hobbies;

    /**
     * 国家
     */
    private String country;

    /**
     * 省份
     */
    private String province;

    /**
     * 城市
     */
    private String city;

    /**
     * 创建时间 自动设置不必修改
     */
    private Date createTime;

    /**
     * 修改时间 自动设置不必修改
     */
    private Date updateTime;
}