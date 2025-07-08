package com.soultalk.po;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MainDiaPO {
    private Long id;
    private Long userId;
    private Boolean isUser;
    private String sentence;
    private Long time;
}
