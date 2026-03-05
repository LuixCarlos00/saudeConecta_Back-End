package br.com.saudeConecta.email;

import br.com.saudeConecta.domain.mensageria.Mensageria;
import br.com.saudeConecta.domain.mensageria.StatusMensagem;
import br.com.saudeConecta.domain.mensageria.TipoMensagem;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.infrastructure.persistence.repository.MensageriaRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.OrganizacaoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.ProfissionalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço responsável exclusivamente pelo registro de envios de email na tabela de mensageria.
 * Acessa os repositórios diretamente para evitar dependência circular com MensageriaService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailMensageriaService {

    private final MensageriaRepository mensageriaRepository;
    private final OrganizacaoRepository organizacaoRepository;
    private final ProfissionalRepository profissionalRepository;

    /**
     * Registra o sucesso de envio de email na mensageria.
     *
     * @param organizacaoId  ID da organização
     * @param profissionalId ID do profissional (pode ser nulo)
     * @param email          email do destinatário
     * @param nome           nome do destinatário
     * @param assunto        assunto do email
     * @param corpo          corpo da mensagem
     * @param tipo           tipo da mensagem
     * @param tentativas     número de tentativas até o sucesso
     */
    public void registrarSucesso(Long organizacaoId, Long profissionalId, String email,
                                  String nome, String assunto, String corpo,
                                  TipoMensagem tipo, int tentativas) {
        if (organizacaoId == null) {
            log.debug("Sucesso de email não registrado: organizacaoId nulo para {}", email);
            return;
        }

        try {
            Organizacao organizacao = organizacaoRepository.findById(organizacaoId)
                    .orElseThrow(() -> new IllegalArgumentException("Organização não encontrada: " + organizacaoId));

            Profissional profissional = null;
            if (profissionalId != null) {
                profissional = profissionalRepository.findById(profissionalId).orElse(null);
            }

            Mensageria mensageria = Mensageria.builder()
                    .organizacao(organizacao)
                    .destinatarioProfissional(profissional)
                    .destinatarioEmail(email)
                    .destinatarioNome(nome)
                    .assunto(assunto)
                    .corpoMensagem(corpo)
                    .tipoMensagem(tipo)
                    .status(StatusMensagem.ENVIADO)
                    .tentativas(tentativas)
                    .adminNotificado(true)
                    .build();

            mensageriaRepository.save(mensageria);
            log.info("Sucesso registrado na mensageria para: {} [{}]", email, tipo);
        } catch (Exception ex) {
            log.error("Erro ao registrar sucesso na mensageria para {}: {}", email, ex.getMessage(), ex);
        }
    }

    /**
     * Registra a falha de envio de email na mensageria.
     *
     * @param organizacaoId  ID da organização
     * @param profissionalId ID do profissional (pode ser nulo)
     * @param email          email do destinatário
     * @param nome           nome do destinatário
     * @param assunto        assunto do email
     * @param corpo          corpo da mensagem
     * @param tipo           tipo da mensagem
     * @param erroDetalhe    detalhe do erro
     * @param tentativas     número de tentativas realizadas
     */
    public void registrarFalha(Long organizacaoId, Long profissionalId, String email,
                                String nome, String assunto, String corpo,
                                TipoMensagem tipo, String erroDetalhe, int tentativas) {
        if (organizacaoId == null) {
            log.warn("Falha de email não registrada: organizacaoId nulo para {}", email);
            return;
        }

        try {
            Organizacao organizacao = organizacaoRepository.findById(organizacaoId)
                    .orElseThrow(() -> new IllegalArgumentException("Organização não encontrada: " + organizacaoId));

            Profissional profissional = null;
            if (profissionalId != null) {
                profissional = profissionalRepository.findById(profissionalId).orElse(null);
            }

            Mensageria mensageria = Mensageria.builder()
                    .organizacao(organizacao)
                    .destinatarioProfissional(profissional)
                    .destinatarioEmail(email)
                    .destinatarioNome(nome)
                    .assunto(assunto)
                    .corpoMensagem(corpo)
                    .tipoMensagem(tipo)
                    .status(StatusMensagem.FALHOU)
                    .erroDetalhe(erroDetalhe)
                    .tentativas(tentativas)
                    .adminNotificado(false)
                    .build();

            mensageriaRepository.save(mensageria);
            log.info("Falha registrada na mensageria para: {} [{}]", email, tipo);
        } catch (Exception ex) {
            log.error("Erro CRÍTICO ao registrar falha na mensageria para {}: {}", email, ex.getMessage(), ex);
        }
    }

    /**
     * Resolve o TipoMensagem com base no tipo de usuário em String.
     *
     * @param tipoUsuario tipo do usuário (medico, secretaria, administrador, recuperacao)
     * @return TipoMensagem correspondente
     */
    public TipoMensagem resolverTipoMensagem(String tipoUsuario) {
        return switch (tipoUsuario.toLowerCase()) {
            case "medico" -> TipoMensagem.EMAIL_CREDENCIAIS_CLINICO;
            case "secretaria" -> TipoMensagem.EMAIL_CREDENCIAIS_SECRETARIA;
            case "administrador" -> TipoMensagem.EMAIL_CREDENCIAIS_ADMINISTRADOR;
            case "recuperacao" -> TipoMensagem.EMAIL_RECUPERACAO_SENHA;
            default -> TipoMensagem.EMAIL_GENERICO;
        };
    }
}
