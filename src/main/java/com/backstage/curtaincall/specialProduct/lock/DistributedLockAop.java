package com.backstage.curtaincall.specialProduct.lock;

import java.lang.reflect.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class DistributedLockAop {

    private final RedissonClient redissonClient;
    private static final String LOCK_PREFIX = "lock:";

    @Around("@annotation(com.backstage.curtaincall.specialProduct.lock.DistributedLock)")
    public Object applyLock(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        DistributedLock lock = method.getAnnotation(DistributedLock.class);

        String key = LOCK_PREFIX + SpringElParser.parse(signature.getParameterNames(), joinPoint.getArgs(), lock.key());
        RLock rLock = redissonClient.getLock(key);

        boolean isLocked = false;
        try {
            // leaseTime 제거 -> 와치독(Watchdog)을 활성화
            // isLocked = rLock.tryLock(lock.waitTime(), lock.leaseTime(), lock.timeUnit());
            isLocked = rLock.tryLock(lock.waitTime(), lock.timeUnit());
            if (!isLocked) {
                log.warn("락 획득 실패 - key: {}", key);
                throw new RuntimeException("분산 락 획득 실패: " + key); // 실패 시 명시적으로 예외
            }

            // 락 획득 성공 후 비즈니스 로직 실행
            log.info("락 획득 성공 - key: {}", key);
            return joinPoint.proceed();

        } finally {
            if (isLocked && rLock.isHeldByCurrentThread()) {
                rLock.unlock();
                log.info("락 해제 완료 - key: {}", key);
            }
        }
    }
}
