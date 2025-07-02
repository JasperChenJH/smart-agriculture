package com.soultalk.po;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentPO {
    private Long id;
    private String name;
    private Long creator;
    private Long createTime;
    private String prompt;
    private String introduction;
    private String photo;
    private String api;
    private Integer isPublic;
}
