package br.com.saudeConecta.util.validation;

import br.com.saudeConecta.util.InputSanitizer;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validador para a annotation @SafeInput.
 * Verifica se o campo não contém padrões de SQL injection ou XSS.
 */
public class SafeInputValidator implements ConstraintValidator<SafeInput, String> {

    @Override
    public void initialize(SafeInput constraintAnnotation) {
        // Nenhuma inicialização necessária
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true; // Campos nulos/vazios são validados por @NotBlank
        }
        return InputSanitizer.isSafe(value);
    }
}
