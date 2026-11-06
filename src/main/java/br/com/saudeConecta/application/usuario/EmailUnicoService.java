package br.com.saudeConecta.application.usuario;

import br.com.saudeConecta.config.logging.MascaraLog;
import br.com.saudeConecta.domain.admin.AdminOrganizacao;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.domain.secretaria.Secretaria;
import br.com.saudeConecta.domain.usuario.Usuario;
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
        log.debug("email.unico verificar email={}", MascaraLog.email(email));
        
        boolean existeEmAdmin = adminOrganizacaoRepository.findByEmail(email).isPresent();
        boolean existeEmProfissional = profissionalRepository.findByEmail(email).isPresent();
        boolean existeEmSecretaria = secretariaRepository.findByEmail(email).isPresent();
        
        boolean emailExiste = existeEmAdmin || existeEmProfissional || existeEmSecretaria;
        
        if (emailExiste) {
            log.warn("email.unico negado motivo=JA_CADASTRADO email={}", MascaraLog.email(email));
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
        log.debug("email.unico verificar usuarioId={} email={}", usuarioId, MascaraLog.email(email));
        
        // Verificar em AdminOrganizacao
        Optional<AdminOrganizacao> adminOpt = adminOrganizacaoRepository.findByEmail(email);
        if (adminOpt.isPresent() && !pertenceAoUsuario(adminOpt.get().getUsuario(), usuarioId)) {
            log.warn("email.unico negado motivo=JA_CADASTRADO perfil=ADMIN email={}", MascaraLog.email(email));
            return true;
        }
        
        // Verificar em Profissional
        Optional<Profissional> profissionalOpt = profissionalRepository.findByEmail(email);
        if (profissionalOpt.isPresent() && !pertenceAoUsuario(profissionalOpt.get().getUsuario(), usuarioId)) {
            log.warn("email.unico negado motivo=JA_CADASTRADO perfil=PROFISSIONAL email={}", MascaraLog.email(email));
            return true;
        }
        
        // Verificar em Secretaria
        Optional<Secretaria> secretariaOpt = secretariaRepository.findByEmail(email);
        if (secretariaOpt.isPresent() && !pertenceAoUsuario(secretariaOpt.get().getUsuario(), usuarioId)) {
            log.warn("email.unico negado motivo=JA_CADASTRADO perfil=SECRETARIA email={}", MascaraLog.email(email));
            return true;
        }
        
        return false;
    }

    /**
     * Valida a troca de e-mail numa atualização de cadastro (admin, profissional ou secretária).
     * O "esqueci minha senha" procura o e-mail em Profissional, Secretaria e Admin, nessa ordem:
     * e-mail repetido desvia o reset para a conta errada e pode estourar o findByEmail (500).
     * Só consulta quando o e-mail muda, para não travar edições de registros legados.
     *
     * @param novoEmail  e-mail recebido na atualização (nulo/vazio = sem troca)
     * @param emailAtual e-mail gravado hoje no registro
     * @param usuarioId  ID do usuário dono do registro (ignorado na busca)
     * @throws IllegalStateException se o e-mail já pertencer a outro usuário
     */
    public void validarEmailLivreParaAtualizacao(String novoEmail, String emailAtual, Long usuarioId) {
        if (novoEmail == null || novoEmail.isBlank() || novoEmail.equalsIgnoreCase(emailAtual)) {
            return;
        }
        if (emailJaExisteParaOutroUsuario(novoEmail, usuarioId)) {
            throw new IllegalStateException("Email ja cadastrado no sistema para outro usuario");
        }
    }

    private static boolean pertenceAoUsuario(Usuario usuario, Long usuarioId) {
        return usuario != null && usuario.getId() != null && usuario.getId().equals(usuarioId);
    }

    /**
     * Retorna em qual tabela o email foi encontrado
     * @param email Email a ser verificado
     * @return String indicando a tabela onde o email foi encontrado
     */
    public String ondeEmailFoiEncontrado(String email) {
        if (adminOrganizacaoRepository.findByEmail(email).isPresent()) {
            return "GESTOR";
        }
        if (profissionalRepository.findByEmail(email).isPresent()) {
            return "CLINICO";
        }
        if (secretariaRepository.findByEmail(email).isPresent()) {
            return "SECRETARIA";
        }
        return null;
    }
}
