package com.threestar.trainus.global.utils;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.threestar.trainus.global.annotation.RedissonLock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Aspect
@Order(1)
@Component
@RequiredArgsConstructor
public class RedssionLockAspect {

	private final RedissonClient redissonClient;

	@Around("@annotation(com.threestar.trainus.global.annotation.RedissonLock)")
	public Object redissonLock(ProceedingJoinPoint joinPoint) throws Throwable {
		MethodSignature signature = (MethodSignature)joinPoint.getSignature();
		Method method = signature.getMethod();
		RedissonLock annotation = method.getAnnotation(RedissonLock.class);
		String lockKey =
			method.getName() + CustomSpringELParser.getDynamicValue(signature.getParameterNames(),
				joinPoint.getArgs(), annotation.value());

		RLock lock = redissonClient.getFairLock(lockKey);

		try {
			boolean lockable = lock.tryLock(annotation.waitTime(), annotation.leaseTime(), TimeUnit.MILLISECONDS);
			if (!lockable) {
				log.info("Lock 획득 실패={}", lockKey);
				return null;
			}
			log.info("로직 수행");
			return joinPoint.proceed();
		} catch (InterruptedException e) {
			log.info("에러 발생");
			throw e;
		} finally {
			if (lock.isHeldByCurrentThread()) {
				lock.unlock();
				log.info("락 해제 완료={}", lockKey);
			}
		}

	}
}
