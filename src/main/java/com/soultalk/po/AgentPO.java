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
    private Integer pub;
    private String prompt;
    private String introduction;
    private String photo;
    private String model;
    private String api;
}
