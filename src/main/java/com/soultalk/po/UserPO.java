package com.soultalk.po;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserPO {
    private Long id;
    private String name;
    private String password;
    private Long time;
    private String introduce;
    private String photo;
}
