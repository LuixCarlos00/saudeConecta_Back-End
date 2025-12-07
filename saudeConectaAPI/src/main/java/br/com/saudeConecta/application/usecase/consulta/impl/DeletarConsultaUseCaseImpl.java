package br.com.saudeConecta.application.usecase.consulta.impl;

import br.com.saudeConecta.application.usecase.consulta.DeletarConsultaUseCase;
import br.com.saudeConecta.infrastructure.persistence.repository.ConsultaRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeletarConsultaUseCaseImpl implements DeletarConsultaUseCase {

    private final ConsultaRepository consultaRepository;

    @Override
    @Transactional
    public void executar(Long id) throws Exception {
        log.info("Iniciando exclusao da consulta ID: {}", id);

        if (id == null || id <= 0) {
            log.warn("Tentativa de exclusao com ID invalido: {}", id);
            throw new IllegalArgumentException("ID invalido");
        }

        if (!consultaRepository.existsById(id)) {
            log.warn("Consulta nao encontrada para exclusao ID: {}", id);
            throw new Exception("Registro nao encontrado");
        }

        try {
            consultaRepository.deleteById(id);
            log.info("Consulta ID: {} excluida com sucesso", id);
        } catch (Exception e) {
            log.error("Erro ao excluir consulta ID: {}", id, e);
            throw new Exception("Violacao de Integridade", e);
        }
    }
}
