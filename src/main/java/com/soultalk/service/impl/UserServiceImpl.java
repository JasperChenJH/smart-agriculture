package com.soultalk.service.impl;

import com.soultalk.context.BaseContext;
import com.soultalk.controller.request.R;
import com.soultalk.mapper.UserMapper;
import com.soultalk.po.UserPO;
import com.soultalk.service.BaseService;
import com.soultalk.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
public class UserServiceImpl implements UserService {
    @Autowired
    private BaseService baseService;
    @Autowired
    private UserMapper userMapper;

    @Override
    public R info() {
        Long  id = Long.valueOf(BaseContext.getCurrentId());
        UserPO user = userMapper.selectById(id);
        user.setPassword(null);
        return R.Success(user);
    }

    @Override
    public R update(UserPO user, String introduce, MultipartFile photo) {
        if (!introduce.isEmpty()) {
            user.setIntroduce(introduce);
        }

        if (!photo.isEmpty()) {
            String url = baseService.saveFileToOSS(photo.getOriginalFilename(), photo);
            if (url != null) {
                user.setPhoto(url);
            }
        }

        userMapper.update(user);
        return R.Success(user.getName() + " 修改成功");
    }
}
