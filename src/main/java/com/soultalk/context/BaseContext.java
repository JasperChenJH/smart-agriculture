package com.soultalk.context;

/**
 * 基于ThreadLocal封装工具类，用于保存和获取当前登录用户id
 * ThreadLocal：线程局部变量，每个线程都拥有自己的变量副本，线程之间互不干扰
 */
public class BaseContext {
    private static final ThreadLocal<String> threadLocal = new ThreadLocal<>();

    public static String getCurrentId() {
        return threadLocal.get();
    }

    public static void setCurrentId(Long id) {
        threadLocal.set(String.valueOf(id));
    }

    public static void remove() {
        threadLocal.remove();
    }
}
