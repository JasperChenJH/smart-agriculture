package com.soultalk.context;

public class BaseContext {
    private static ThreadLocal<String> threadLocal = new ThreadLocal<>();
    public static void setCurrentId(Long id) {
        threadLocal.set(String.valueOf(id));
    }
    public static String getCurrentId() {
        return threadLocal.get();
    }
    public static void remove() {
        threadLocal.remove();
    }
}
