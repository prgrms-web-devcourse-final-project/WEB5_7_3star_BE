package com.threestar.trainus.global.aop;

import com.threestar.trainus.global.annotation.DistributedLock;
import com.threestar.trainus.global.exception.domain.ErrorCode;
import com.threestar.trainus.global.exception.handler.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;

@Aspect
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class DistributedLockAspect {

	private final RedissonClient redissonClient;

	@Around("@annotation(distributedLock)")
	public Object lock(final ProceedingJoinPoint joinPoint, final DistributedLock distributedLock) throws Throwable {
		String lockName = createDynamicKey(joinPoint, distributedLock.key());
		RLock lock = redissonClient.getFairLock(lockName); // FairLock으로 순서 보장

		try {
			// 락 획득 시도
			boolean isLocked = lock.tryLock(distributedLock.waitTime(), distributedLock.leaseTime(),
				distributedLock.timeUnit());
			if (!isLocked) {
				log.warn("Failed to acquire lock: {}", lockName);
				throw new BusinessException(ErrorCode.LOCK_ACQUISITION_FAILED);
			}
			log.info("Acquired lock: {}", lockName);

			// 실제 타겟 메소드 실행
			return joinPoint.proceed();

		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new BusinessException(ErrorCode.LOCK_INTERRUPTED);
		} finally {
			// 락 해제
			if (lock.isLocked() && lock.isHeldByCurrentThread()) {
				lock.unlock();
				log.info("Released lock: {}", lockName);
			}
		}
	}

	private String createDynamicKey(ProceedingJoinPoint joinPoint, String key) {
		MethodSignature signature = (MethodSignature)joinPoint.getSignature();
		String[] parameterNames = signature.getParameterNames();
		Object[] args = joinPoint.getArgs();

		ExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext context = new StandardEvaluationContext();

		for (int i = 0; i < parameterNames.length; i++) {
			context.setVariable(parameterNames[i], args[i]);
		}

		return parser.parseExpression(key).getValue(context, String.class);
	}
}
