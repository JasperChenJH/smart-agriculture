package com.soultalk.po;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiaPO {
    private Long id;
    private Long userId;
    private Integer isAgent;
    private Long agentId;
    private String content;
    private Long updateTime;
}
