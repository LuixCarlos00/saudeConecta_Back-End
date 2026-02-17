package br.com.saudeConecta.domain.usuario;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class StatusUsuarioConverter implements AttributeConverter<StatusUsuario, Integer> {

    @Override
    public Integer convertToDatabaseColumn(StatusUsuario status) {
        if (status == null) {
            return null;
        }
        return status == StatusUsuario.ATIVO ? 1 : 0;
    }

    @Override
    public StatusUsuario convertToEntityAttribute(Integer dbData) {
        if (dbData == null) {
            return StatusUsuario.ATIVO; // Default para ATIVO
        }
        return dbData == 1 ? StatusUsuario.ATIVO : StatusUsuario.INATIVO;
    }
}
