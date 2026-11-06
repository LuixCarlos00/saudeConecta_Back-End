package br.com.saudeConecta.config.tenant;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class TenantAspect {
    
    @Before("@annotation(requiresTenant)")
    public void validateTenant(JoinPoint joinPoint, RequiresTenant requiresTenant) {
        if (!TenantContext.hasTenant()) {
            throw new TenantNotDefinedException(
                "Operação requer tenant definido: " + joinPoint.getSignature().getName()
            );
        }
        log.debug("tenant.validar ok operacao={}", joinPoint.getSignature().getName());
    }
}
