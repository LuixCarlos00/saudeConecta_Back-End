package br.com.saudeConecta.util;

import br.com.saudeConecta.domain.admin.AdminOrganizacao;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.domain.secretaria.Secretaria;
import br.com.saudeConecta.infrastructure.persistence.repository.AdminOrganizacaoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.ProfissionalRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.SecretariaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailUnicoService {

    private final AdminOrganizacaoRepository adminOrganizacaoRepository;
    private final ProfissionalRepository profissionalRepository;
    private final SecretariaRepository secretariaRepository;

    /**
     * Verifica se o email já existe em qualquer uma das tabelas (Admin, Profissional, Secretaria)
     * @param email Email a ser verificado
     * @return true se o email já existir, false caso contrário
     */
    public boolean emailJaExiste(String email) {
        log.debug("Verificando existência do email: {}", email);
        
        boolean existeEmAdmin = adminOrganizacaoRepository.findByEmail(email).isPresent();
        boolean existeEmProfissional = profissionalRepository.findByEmail(email).isPresent();
        boolean existeEmSecretaria = secretariaRepository.findByEmail(email).isPresent();
        
        boolean emailExiste = existeEmAdmin || existeEmProfissional || existeEmSecretaria;
        
        if (emailExiste) {
            log.warn("Email {} já está cadastrado no sistema", email);
        }
        
        return emailExiste;
    }

    /**
     * Verifica se o email já existe em qualquer uma das tabelas, exceto para um ID específico
     * Útil para atualizações, onde não queremos comparar com o próprio registro
     * @param email Email a ser verificado
     * @param usuarioId ID do usuário que está sendo atualizado (para ignorar na verificação)
     * @return true se o email já existir para outro usuário, false caso contrário
     */
    public boolean emailJaExisteParaOutroUsuario(String email, Long usuarioId) {
        log.debug("Verificando existência do email: {} para outro usuário (ID: {})", email, usuarioId);
        
        // Verificar em AdminOrganizacao
        Optional<AdminOrganizacao> adminOpt = adminOrganizacaoRepository.findByEmail(email);
        if (adminOpt.isPresent() && !adminOpt.get().getUsuario().getId().equals(usuarioId)) {
            log.warn("Email {} já está cadastrado para outro administrador", email);
            return true;
        }
        
        // Verificar em Profissional
        Optional<Profissional> profissionalOpt = profissionalRepository.findByEmail(email);
        if (profissionalOpt.isPresent() && !profissionalOpt.get().getUsuario().getId().equals(usuarioId)) {
            log.warn("Email {} já está cadastrado para outro profissional", email);
            return true;
        }
        
        // Verificar em Secretaria
        Optional<Secretaria> secretariaOpt = secretariaRepository.findByEmail(email);
        if (secretariaOpt.isPresent() && !secretariaOpt.get().getUsuario().getId().equals(usuarioId)) {
            log.warn("Email {} já está cadastrado para outra secretária", email);
            return true;
        }
        
        return false;
    }

    /**
     * Retorna em qual tabela o email foi encontrado
     * @param email Email a ser verificado
     * @return String indicando a tabela onde o email foi encontrado
     */
    public String ondeEmailFoiEncontrado(String email) {
        if (adminOrganizacaoRepository.findByEmail(email).isPresent()) {
            return "ADMIN_ORG";
        }
        if (profissionalRepository.findByEmail(email).isPresent()) {
            return "PROFISSIONAL";
        }
        if (secretariaRepository.findByEmail(email).isPresent()) {
            return "SECRETARIA";
        }
        return null;
    }
}
