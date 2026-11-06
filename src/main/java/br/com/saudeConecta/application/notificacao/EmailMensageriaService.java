package br.com.saudeConecta.application.notificacao;

import br.com.saudeConecta.config.logging.MascaraLog;
import br.com.saudeConecta.domain.mensageria.Mensageria;
import br.com.saudeConecta.domain.mensageria.StatusMensagem;
import br.com.saudeConecta.domain.mensageria.TipoMensagem;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.infrastructure.persistence.repository.MensageriaRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.OrganizacaoRepository;
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

    /**
     * Registra o sucesso de envio de email na mensageria.
     *
     * @param envio      dados do envio (organização, destinatário, assunto, corpo e tipo)
     * @param tentativas número de tentativas até o sucesso
     */
    public void registrarSucesso(RegistroEnvioEmail envio, int tentativas) {
        Long organizacaoId = envio.organizacaoId();
        String email = envio.email();
        TipoMensagem tipo = envio.tipo();
        if (organizacaoId == null) {
            log.debug("email.mensageria sucesso.registrar ignorado motivo=SEM_ORGANIZACAO email={}", MascaraLog.email(email));
            return;
        }

        try {
            Organizacao organizacao = organizacaoRepository.findById(organizacaoId)
                    .orElseThrow(() -> new IllegalArgumentException("Organização não encontrada: " + organizacaoId));

            Mensageria mensageria = Mensageria.builder()
                    .organizacao(organizacao)
                    .destinatarioEntidadeId(envio.entidadeId())
                    .destinatarioEmail(email)
                    .destinatarioNome(envio.nome())
                    .assunto(envio.assunto())
                    .corpoMensagem(envio.corpo())
                    .tipoMensagem(tipo)
                    .status(StatusMensagem.ENVIADO)
                    .tentativas(tentativas)
                    .adminNotificado(true)
                    .build();

            mensageriaRepository.save(mensageria);
            log.info("email.mensageria sucesso.registrar ok email={} tipo={}", MascaraLog.email(email), tipo);
        } catch (Exception ex) {
            log.error("email.mensageria sucesso.registrar falha email={}", MascaraLog.email(email), ex);
        }
    }

    /**
     * Registra a falha de envio de email na mensageria.
     *
     * @param envio       dados do envio (organização, destinatário, assunto, corpo e tipo)
     * @param erroDetalhe detalhe do erro
     * @param tentativas  número de tentativas realizadas
     */
    public void registrarFalha(RegistroEnvioEmail envio, String erroDetalhe, int tentativas) {
        Long organizacaoId = envio.organizacaoId();
        String email = envio.email();
        TipoMensagem tipo = envio.tipo();
        if (organizacaoId == null) {
            log.warn("email.mensageria falha.registrar negado motivo=SEM_ORGANIZACAO email={}", MascaraLog.email(email));
            return;
        }

        try {
            Organizacao organizacao = organizacaoRepository.findById(organizacaoId)
                    .orElseThrow(() -> new IllegalArgumentException("Organização não encontrada: " + organizacaoId));

            Mensageria mensageria = Mensageria.builder()
                    .organizacao(organizacao)
                    .destinatarioEntidadeId(envio.entidadeId())
                    .destinatarioEmail(email)
                    .destinatarioNome(envio.nome())
                    .assunto(envio.assunto())
                    .corpoMensagem(envio.corpo())
                    .tipoMensagem(tipo)
                    .status(StatusMensagem.FALHOU)
                    .erroDetalhe(erroDetalhe)
                    .tentativas(tentativas)
                    .adminNotificado(false)
                    .build();

            mensageriaRepository.save(mensageria);
            log.info("email.mensageria falha.registrar ok email={} tipo={}", MascaraLog.email(email), tipo);
        } catch (Exception ex) {
            log.error("email.mensageria falha.registrar falha email={} tipo={} acao=REGISTRO_PERDIDO", MascaraLog.email(email), tipo, ex);
        }
    }

    /**
     * Atualiza um registro existente na mensageria para ENVIADO (usado no reenvio).
     *
     * @param mensageriaId ID do registro existente
     * @param tentativas   número de tentativas até o sucesso
     */
    @Transactional
    public void atualizarSucesso(Long mensageriaId, int tentativas) {
        try {
            Mensageria mensageria = mensageriaRepository.findById(mensageriaId)
                    .orElseThrow(() -> new IllegalArgumentException("Mensagem não encontrada: " + mensageriaId));

            mensageria.setStatus(StatusMensagem.ENVIADO);
            mensageria.setTentativas(tentativas);
            mensageria.setErroDetalhe(null);
            mensageriaRepository.save(mensageria);
            log.info("email.mensageria reenvio.sucesso ok mensageriaId={}", mensageriaId);
        } catch (Exception ex) {
            log.error("email.mensageria reenvio.sucesso falha mensageriaId={} acao=REGISTRO_NAO_ATUALIZADO", mensageriaId, ex);
        }
    }

    /**
     * Atualiza um registro existente na mensageria para FALHOU (usado no reenvio).
     *
     * @param mensageriaId ID do registro existente
     * @param erroDetalhe  detalhe do erro
     * @param tentativas   número de tentativas realizadas
     */
    @Transactional
    public void atualizarFalha(Long mensageriaId, String erroDetalhe, int tentativas) {
        try {
            Mensageria mensageria = mensageriaRepository.findById(mensageriaId)
                    .orElseThrow(() -> new IllegalArgumentException("Mensagem não encontrada: " + mensageriaId));

            mensageria.setStatus(StatusMensagem.FALHOU);
            mensageria.setErroDetalhe(erroDetalhe);
            mensageria.setTentativas(tentativas);
            mensageriaRepository.save(mensageria);
            log.info("email.mensageria reenvio.falha ok mensageriaId={}", mensageriaId);
        } catch (Exception ex) {
            log.error("email.mensageria reenvio.falha falha mensageriaId={} acao=REGISTRO_NAO_ATUALIZADO", mensageriaId, ex);
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
            case "paciente" -> TipoMensagem.EMAIL_CREDENCIAIS_PACIENTE;
            case "recuperacao" -> TipoMensagem.EMAIL_RECUPERACAO_SENHA;
            case "chamado" -> TipoMensagem.EMAIL_CHAMADO_SUPORTE;
            default -> TipoMensagem.EMAIL_GENERICO;
        };
    }
}
