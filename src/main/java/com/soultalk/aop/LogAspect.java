package com.soultalk.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Aspect
public class LogAspect {

    /**
     * 日志输出
     *
     * @param joinPoint 切点
     * @throws Throwable
     */
    @Before("execution(* com.soultalk.controller.*.*(..))")
    public void sysLog(JoinPoint joinPoint) throws Throwable {
        String name = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        log.info("Method {} is called with parameters: {}", name, args);
    }

}
