package com.soultalk.controller;

import com.soultalk.context.BaseContext;
import com.soultalk.controller.request.R;
import com.soultalk.po.PageResult;
import com.soultalk.po.UserEmotionRecordPO;
import com.soultalk.po.UserInfoPO;
import com.soultalk.service.MainAgentService;
import com.soultalk.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 用户信息
 */
@CrossOrigin
@RestController
@RequestMapping("/user")
public class UserController {
    @Resource
    private MainAgentService mainAgentService;
    @Autowired
    private UserService userService;

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
    /*
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
    */

    /**
     * 修改用户基本信息
     *
     * @param introduce 自我简介
     * @param photo     用户头像
     * @return
     */
    @PostMapping("/updateBaseInfo")
    public R updateBaseInfo(@RequestParam("introduce") String introduce,
                            @RequestParam("photo") MultipartFile photo) {
        userService.updateBaseInfo(introduce, photo);
        return R.Success("修改成功");
    }

    /**
     * 修改用户密码
     *
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     * @return
     */
    @PostMapping("/updatePassword")
    public R updatePassword(@RequestParam("oldpwd") String oldPassword,
                            @RequestParam("newpwd") String newPassword) {
        boolean flag = userService.updatePassword(oldPassword, newPassword);
        if (!flag) {
            return R.Failed("原始密码错误!");
        }
        return R.Success("修改成功!");
    }

    /**
     * 注销账号
     *
     * @return
     */
    @GetMapping("/drop")
    public R drop() {
        userService.dropUser();
        return R.Success("删除成功");
    }

    /**
     * 获取用户详细信息
     *
     * @return
     */
    @GetMapping("/detail/info")
    public R getDetailInfo() {
        Long userId = Long.parseLong(BaseContext.getCurrentId());
        UserInfoPO userInfo = userService.getDetailInfo(userId);
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
     * @param page 页码 默认1
     * @param size 每页数量 默认10
     * @return 用户情感得分列表
     */

    @GetMapping("/emotion/pagelist")
    public R getEmotionPageList(@RequestParam(value = "page", defaultValue = "1") Integer page, @RequestParam(value = "size", defaultValue = "10") Integer size) {
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
