package com.retail.userservice.config;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Enforces that developers cannot override the service-wide transaction
 * isolation level via @Transactional(isolation = ...).
 *
 * The default isolation is READ_COMMITTED, set at the HikariCP connection
 * level in application.yaml. This advice rejects any @Transactional that
 * explicitly sets a different isolation level, failing fast at runtime
 * with a clear error message instead of silently running with the wrong
 * isolation and causing subtle data bugs.
 */
@Slf4j
@Aspect
@Component
public class TransactionIsolationEnforcerAdvice {

    private static final Isolation ALLOWED_ISOLATION = Isolation.READ_COMMITTED;

    @Before("@annotation(org.springframework.transaction.annotation.Transactional)")
    public void enforceIsolation(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Transactional txAnnotation = signature.getMethod().getAnnotation(Transactional.class);

        if (txAnnotation != null) {
            Isolation declared = txAnnotation.isolation();
            if (declared != Isolation.DEFAULT && declared != ALLOWED_ISOLATION) {
                String method = signature.getDeclaringTypeName() + "." + signature.getName();
                log.error("BLOCKED: {} declares isolation={}, but service policy requires {}",
                        method, declared, ALLOWED_ISOLATION);
                throw new IllegalStateException(
                        "Transaction isolation override not allowed. " +
                        "Service policy enforces " + ALLOWED_ISOLATION + ". " +
                        "Found " + declared + " on " + method);
            }
        }
    }
}
