package com.soultalk.controller.request;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public enum ResultCodeEnum {

    SUCCESS(100200, "返回成功"),
    SYSTEM_EXCEPTION(100500, "系统异常"),
    REQUEST_PARAM_ERROR(100401, "请求参数错误"),
    REQUEST_OUT_OVERTIME(100408, "请求超时"),
    REQUEST_NOT_FOUND(100404, "请求的资源或服务未找到"),
    REQUEST_LENGTH_LIMIT(100414, "请求URI太长"),
    REQUEST_Format_NOT_SUPPORTED(100415, "请求的格式不支持"),
    TOKEN_EXPIRED_REDIRECT(100302, "token过期,请重新登录"),
    DUPLICATE_KEY_EXCEPTION(100416, "数据已存在，请勿重复操作"),

    INTERFACE_BASIC_INFORMATION_NULL(100001, "接口基本信息为空"),
    INTERFACE_NAME_NULL(100002, "接口名称为空"),
    INTERFACE_SOURCE_NULL(100005, "接口来源为空"),
    INTERFACE_PROTOCOL_NULL(100003, "接口协议为空"),
    INTERFACE_NOT_ENABLED(100123, "接口未启用"),
    INTERFACE_IP_PORT_NULL(100012, "ip和端口为空"),
    INTERFACE_PATH_NULL(100004, "接口路径为空"),
    INTERFACE_REQUEST_METHOD_NULL(100008, "接口请求方式"),
    REQUEST_TIMEOUT(100014, "超时时限为空"),
    REQUEST_PARAM_NULL(100012, "请求参数为空"),
    INTERFACE_BASE_NOT_FOUND(100013, "未找到该条接口基本信息"),
    INTERFACE_SETTING_NOT_FOUND(100015, "未找到该条接口配置信息"),
    INTERFACE_BASE_EXIST(100026, "接口基本信息已存在"),
    INTERFACE_BASE_NOT_EXIST(100025, "接口基本信息不存在"),
    INTERFACE_SETTING_EXIST(100027, "接口配置信息已存在"),
    INTERFACE_SETTING_NOT_EXIST(100028, "接口配置信息不存在"),
    INTERFACE_PATH_ILLEGAL(100009, "接口路径不符合规范"),
    REQUIRED_FIELD_IS_BLANK(100007, "入参必填字段为空： "),
    REQUEST_OUT_API_FAIL(100018, "请求外部接口失败，返回状态码："),
    REQUEST_OUT_GET_OVERTIME(100019, "GET请求外部接口失败，请求超时"),
    REQUEST_OUT_POST_OVERTIME(100020, "POST请求外部接口失败，请求超时"),
    REQUEST_OUT_PUT_OVERTIME(100021, "PUT请求外部接口失败，请求超时"),
    REQUEST_OUT_DELETE_OVERTIME(100022, "DELETE请求外部接口失败，请求超时"),
    OTHER_METHODS_NOT_SUPPORTED(100023, "现阶段不支持其他请求方式"),
    PARAM_BODY_CONTENT_ERROR(100024, "请求body不是json格式"),
    SPLICING_PARAM_IS_FAIL(100031, "拼接GET请求入参出错，请检查参数"),

    //登录登出模块
    PASSWORD_FAIL(210001, "登录账户或密码错误,请重新输入!"),
    INSUFFICIENT_PERMISSIONS(210002, "用户权限不足"),
    ACCOUNT_NOT_FIND_RESOURCE(210003, "用户未查询到资源"),
    APP_WAS_NOT_QUERIED(210004, "应用id不存在"),
    SIGNATURE_VERIFICATION_FAILED(210005, "验签未通过"),
    RANDOM_CODE_EXPIRED(210006, "随机码已过期"),
    INCORRECT_RANDOM_CODE_VALUE(210007, "随机码值不正确"),
    TOKEN_GENERATION_FAILED(210008, "token 生成失败"),
    NO_TOKEN_PASSED_IN(210009, "未传入 token"),
    NO_USER_FOUND_TOKEN(210009, "token 未查询到用户"),
    TOKEN_VERIFICATION_FAILED(210009, "token 校验失败"),
    ILLEGAL_TOKEN_INFORMATION(210010, "token 信息不匹配"),
    USER_DOES_NOT_EXIST(210011, "用户不存在"),
    SIGN_EXPIRED(210012, "sign 已过期"),
    PASSWORD_VIOLATION(210013, "密码需包含六位及以上字母和数字"),

    //需要跳转登录的code
    NO_REFRESH_PASSED_IN(210019, "未传入 token"),
    NO_REFRESH_USER_FOUND_TOKEN(210019, "token 未查询到用户"),
    REFRESH_VERIFICATION_FAILED(210019, "token 校验失败"),
    JSON_CONVERSION_ERROR(210019, "json转换出错"),
    REFRESH_GENERATION_FAILED(210019, "token 生成失败"),

    NO_LOGIN_STRATEGY(210020, "%s该认证策略不存在"),

    NOT_HAVE_ACCOUNT(202003, "账户不存在"),
    ;

    /**
     * 枚举值
     */
    private final Integer code;

    /**
     * 枚举描述
     */
    private final String message;

    /**
     * 构造一个<code>LocalCacheEnum</code>枚举对象
     *
     * @param code    枚举值
     * @param message 枚举描述
     */
    ResultCodeEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 获取全部枚举
     *
     * @return List<LocalCacheEnum>
     */
    public static List<ResultCodeEnum> getAllEnum() {
        List<ResultCodeEnum> list = new ArrayList<>();
        Collections.addAll(list, values());
        return list;
    }

    /**
     * 获取全部枚举值
     *
     * @return List<String>
     */
    public static List<Integer> getAllEnumCode() {
        List<Integer> list = new ArrayList<>();
        for (ResultCodeEnum each : values()) {
            list.add(each.code);
        }
        return list;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public <T> R<T> result() {
        return new R(getCode(), getMessage());
    }

    public <T> R<T> result(String message) {
        return new R(getCode(), message);
    }

    /**
     * 将返回码标准的信息 填充到message里面，message必须包含一个%s
     */
    public <T> R<T> resultFillingMessage(String message) {
        return new R(getCode(), String.format(message, getMessage()));
    }

    /**
     * 将message添加到返回码标准的信息后面 再返回{@link R}
     */
    public <T> R<T> resultAppendMessage(String message) {
        return new R(getCode(), getMessage() + message);
    }

}