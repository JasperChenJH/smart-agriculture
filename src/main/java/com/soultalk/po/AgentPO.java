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
    private String introduction;
    private Long creator;
    private Long createTime;
    private Integer pub;
    private String model;
    private String prompt;
    private String api;
    private String photo;
}
