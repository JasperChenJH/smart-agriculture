package com.soultalk.controller;

import com.soultalk.context.BaseContext;
import com.soultalk.controller.request.R;
import com.soultalk.mapper.UserMapper;
import com.soultalk.po.PageResult;
import com.soultalk.po.UserEmotionRecordPO;
import com.soultalk.po.UserInfoPO;
import com.soultalk.po.UserPO;
import com.soultalk.service.AuthService;
import com.soultalk.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 用户信息
 */
@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private AuthService authService;
    @Autowired
    private UserMapper userMapper;

    /**
     * 测试
     *
     * @return
     */
    @PostMapping("/hello")
    public ResponseEntity<?> hello() {
        System.out.println(BaseContext.getCurrentId());
        return ResponseEntity.ok("hello");
    }

    /**
     * 获取用户基本信息
     *
     * @return
     */
    @GetMapping("/info")
    public R info() {
        Long id = Long.valueOf(BaseContext.getCurrentId());
        return R.Success(userService.info(id));
    }

    /**
     * 修改用户基本信息
     *
     * @param name
     * @param password
     * @param introduce
     * @param photo
     * @return
     */
    @PostMapping("/update")
    public R update(@RequestParam("name") String name,
                    @RequestParam("password") String password,
                    @RequestParam("introduce") String introduce,
                    @RequestParam("photo") MultipartFile photo) {

        if (authService.check(password, password)) {
            UserPO user = userMapper.selectByName(name);
            return R.Success(userService.update(user, introduce, photo));
        } else {
            return R.Success("密码错误");
        }
    }

    /**
     * 获取用户详细信息
     *
     * @return
     */
    @GetMapping("/detail/info")
    public R getDetailInfo() {
        UserInfoPO userInfo = userService.getDetailInfo();
        return R.Success(userInfo);
    }

    /**
     * 修改用户详细信息
     *
     * @param userInfo
     * @return
     */
    @PostMapping("/detail/update")
    public R updateDetailInfo(@RequestBody UserInfoPO userInfo) {
        userService.updateDetailInfo(userInfo);
        return R.Success("修改成功");
    }

    /**
     * 用户情感得分列表
     *
     * @param page
     * @param size
     * @return
     */

    @GetMapping("/emotion/pagelist")
    public R getEmotionPageList(@RequestParam("page") Integer page, @RequestParam("size") Integer size) {
        PageResult pageResult = userService.getEmotionPageList(page, size);
        return R.Success(pageResult);
    }

    /**
     * 删除用户情感得分记录
     *
     * @param ids 情感id集合
     */
    @DeleteMapping("/emotion/delete")
    public R delete(@RequestParam List<Long> ids) {
        userService.deleteBatch(ids);
        return R.Success("删除成功");
    }

    /**
     * 绘制情感得分图
     *
     * @param items 记录条数 默认20条
     * @param days  最近多少天 默认3天
     * @return 情感得分集合
     */
    @GetMapping("/emotion/chart")
    public R getEmotionChart(@RequestParam(value = "items", defaultValue = "20") Integer items,
                             @RequestParam(value = "days", defaultValue = "3") Integer days) {
        List<UserEmotionRecordPO> result = userService.getEmotionChart(items, days);
        return R.Success(result);
    }

}
