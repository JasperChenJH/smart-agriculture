package com.soultalk.service;

import com.soultalk.controller.request.R;
import com.soultalk.po.UserPO;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {
    //获取该name的用户信息
    R info(String name);

    //更新个人主页信息
    R update(UserPO user, String introduce, MultipartFile photo);
}
