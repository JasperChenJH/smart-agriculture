package com.soultalk.controller;

import com.soultalk.controller.request.JwtResponse;
import com.soultalk.controller.request.R;
import com.soultalk.po.AdminPO;
import com.soultalk.service.AdminService;
import com.soultalk.service.FileService;
import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping("/admin")
public class AdminController {
    @Resource
    private AdminService adminService;
    @Resource
    private FileService fileService;


    /**
     * 登录
     *
     * @param nickname
     * @param password
     * @return
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam String nickname, @RequestParam String password) {
        return adminService.login(nickname, password);
    }
    /**
     * 注册
     *
     * @param name
     * @param password
     * @return
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestParam("name") String name, @RequestParam("password") String password) {
        return adminService.register(name, password);
    }
    /**
     * 重置密码
     *
     * @param id
     * @return
     */

    @PostMapping("/resetPassword")
    public R resetPassword(@RequestParam("id") String id) {
        adminService.resetPassword(Long.parseLong(id));
        return R.Success("密码已重置为 a12345");
    }
    /**
     * 获取管理员信息
     *
     * @param adminId
     * @return
     */

    @GetMapping("/info")
    public R getInfo(@RequestParam Integer adminId) {
        AdminPO admin = adminService.getById(adminId);
        if (admin == null) {
            return R.Failed("管理员不存在");
        }
        return R.Success(admin);
    }
    /**
     * 修改管理员昵称
     *
     * @param admin
     * @return
     */

    @PostMapping("/update")
    public R update(@RequestBody AdminPO admin) {
        adminService.update(admin);
        return R.Success("更新成功");
    }

    @DeleteMapping("/delete")
    public R delete(@RequestParam Integer adminId) {
        adminService.deleteById(adminId);
        return R.Success("删除成功");
    }

    /**
     * 获取所有用户账户状态
     *
     * @return
     */
    @GetMapping("/users/status")
    public R getAllUserStatus() {
        return R.Success(adminService.getAllUserStatus());
    }

    /**
     * 上传Word文件
     *
     * @param file
     * @return
     */
    @PostMapping("/word/upload")
    public R uploadWordFile(@RequestParam("file") MultipartFile file) {
        java.util.Map<String, String> result = fileService.uploadWordFile(file);
        if ("true".equals(result.get("success"))) {
            return R.Success(result);
        } else {
            return R.Failed(result.get("message"));
        }
    }

    /**
     * 读取Word文件内容
     *
     * @param fileName
     * @return
     */
    @GetMapping("/word/read")
    public R readWordFile(@RequestParam("fileName") String fileName) {
        String content = fileService.readWordFile(fileName);
        if (content.equals("文件不存在")) {
            return R.Failed(content);
        } else if (content.startsWith("读取文件失败")) {
            return R.Failed(content);
        }
        return R.Success(content);
    }

    /**
     * 删除Word文件
     *
     * @param fileName
     * @return
     */
    @DeleteMapping("/word/delete")
    public R deleteWordFile(@RequestParam("fileName") String fileName) {
        boolean success = fileService.deleteWordFile(fileName);
        if (success) {
            return R.Success("删除成功");
        } else {
            return R.Failed("删除失败，文件不存在");
        }
    }

    /**
     * 列出所有Word文件
     *
     * @return
     */
    @GetMapping("/word/list")
    public R listWordFiles() {
        return R.Success(fileService.listWordFiles());
    }

}