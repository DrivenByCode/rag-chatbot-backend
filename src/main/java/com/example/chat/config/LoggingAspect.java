package com.example.chat.config;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.UUID;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class LoggingAspect {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    public void restControllerPointcut() {
    }

    @Around("restControllerPointcut()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String requestId = UUID.randomUUID().toString().substring(0, 8);

        log.info("[{}] {} {} 요청 시작 - {}.{}({})",
                requestId,
                request.getMethod(),
                request.getRequestURI(),
                joinPoint.getSignature().getDeclaringTypeName(),
                joinPoint.getSignature().getName(),
                Arrays.toString(joinPoint.getArgs()));

        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();

            long endTime = System.currentTimeMillis();
            log.info("[{}] {} {} 요청 완료 - 처리 시간: {}ms",
                    requestId,
                    request.getMethod(),
                    request.getRequestURI(),
                    (endTime - startTime));

            return result;
        } catch (Exception e) {
            log.error("[{}] {} {} 요청 실패 - 에러: {}",
                    requestId,
                    request.getMethod(),
                    request.getRequestURI(),
                    e.getMessage());
            throw e;
        }
    }
} 