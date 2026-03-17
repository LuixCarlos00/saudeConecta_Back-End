package br.com.saudeConecta.util.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Annotation para validar que o campo não contém padrões de SQL injection ou XSS.
 * Aplique em campos String de DTOs/Records para proteção automática.
 */
@Documented
@Constraint(validatedBy = SafeInputValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface SafeInput {
    String message() default "O campo contém caracteres ou padrões não permitidos";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
