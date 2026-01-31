package br.com.saudeConecta.application.service;

import br.com.saudeConecta.application.port.in.administrador.AdministradorInputPort;
import br.com.saudeConecta.application.port.out.administrador.AdministradorOutputPort;
import br.com.saudeConecta.domain.administrador.Administrador;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdministradorService implements AdministradorInputPort {

    private final AdministradorOutputPort administradorOutputPort;

    @Override
    public Optional<Administrador> buscarPorId(Long id) {
        log.debug("Buscando administrador por ID: {}", id);
        return administradorOutputPort.findById(id);
    }

    @Override
    public Optional<Administrador> buscarPorIdUsuario(Long usuarioId) {
        log.debug("Buscando administrador por ID de usuário: {}", usuarioId);
        return administradorOutputPort.findByAdmUsuario_Id(usuarioId);
    }

    @Override
    public List<Administrador> buscarTodos() {
        log.debug("Buscando todos os administradores");
        return administradorOutputPort.findAll();
    }

    @Override
    public Page<Administrador> buscarTodos(Pageable pageable) {
        log.debug("Buscando todos os administradores com paginação");
        return administradorOutputPort.findAll(pageable);
    }

    @Override
    public Administrador cadastrar(Administrador administrador) {
        log.info("Cadastrando novo administrador: {}", administrador.getAdmNome());
        Administrador administradorSalvo = administradorOutputPort.save(administrador);
        log.info("Administrador cadastrado com sucesso. ID: {}", administradorSalvo.getAdmCodigo());
        return administradorSalvo;
    }

    @Override
    public void deletar(Long id) throws Exception {
        log.info("Iniciando exclusão do administrador ID: {}", id);

        if (id == null || id <= 0) {
            log.warn("Tentativa de exclusão com ID inválido: {}", id);
            throw new IllegalArgumentException("ID inválido");
        }

        if (!administradorOutputPort.existsById(id)) {
            log.warn("Administrador não encontrado para exclusão ID: {}", id);
            throw new Exception("Registro não encontrado");
        }

        try {
            administradorOutputPort.deleteById(id);
            log.info("Administrador ID: {} excluído com sucesso", id);
        } catch (Exception e) {
            log.error("Erro ao excluir administrador ID: {}", id, e);
            throw new Exception("Violação de Integridade", e);
        }
    }
}
