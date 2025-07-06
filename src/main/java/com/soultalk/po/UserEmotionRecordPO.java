package com.soultalk.po;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
@AllArgsConstructor
@NoArgsConstructor
@Data
public class UserEmotionRecordPO {
    /**
     * 情绪记录ID，主键自增
     */
    private Integer id;
    
    /**
     * 用户ID，关联用户表
     */
    private Long userId;
    
    /**
     * 情绪类型：happy/angry/sad/neutral等
     */
    private String emotion;
    
    /**
     * 情绪强度得分(0.00-10.00)
     */
    private BigDecimal score;
    
    /**
     * 用户的问题
     */
    private String context;

    /**
     * 状态：0-无效记录 1-有效记录 默认1
     */
    private int status;
    
    /**
     * 记录创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}