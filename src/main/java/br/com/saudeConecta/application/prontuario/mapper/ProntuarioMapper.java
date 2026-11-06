package br.com.saudeConecta.application.prontuario.mapper;

import br.com.saudeConecta.application.prontuario.dto.CadastrarProntuarioRequest;
import br.com.saudeConecta.domain.consulta.Consulta;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.domain.prontuario.Prontuario;

/**
 * Converte o pedido de cadastro do prontuário médico na entidade.
 *
 * <p>Fica fora da entidade de propósito: o domínio não conhece DTO (regra 1 do ArquiteturaTest).
 * Quando os DTOs forem para {@code application/<area>/dto} (fase B5), este mapper vai junto.
 */
public final class ProntuarioMapper {

    private ProntuarioMapper() {
    }

    /**
     * Monta o prontuário médico a partir do pedido, já ligado ao profissional e à consulta.
     * Não grava nada e não trata os planejamentos, que o service adiciona depois de salvar.
     *
     * @param request      dados do atendimento enviados pela tela
     * @param profissional profissional dono do prontuário (já validado no tenant)
     * @param consulta     consulta atendida (já validada no tenant)
     * @return entidade nova, ainda não persistida
     */
    public static Prontuario paraEntidade(CadastrarProntuarioRequest request, Profissional profissional,
                                          Consulta consulta) {
        return Prontuario.builder()
                // anamnese
                .prontQueixaPricipal(request.getQueixaPrincipal())
                .prontAnamnese(request.getAnamnese())
                .prontObservacao(request.getObservacao())
                // exame clínico — sinais vitais
                .prontPressao(request.getPressao())
                .prontFrequenciaRespiratoria(request.getFrequenciaRespiratoria())
                .prontFrequenciaArterialSistolica(request.getFrequenciaArterialSistolica())
                .prontFrequenciaArterialDiastolica(request.getFrequenciaArterialDiastolica())
                .prontPulso(request.getPulso())
                .prontAltura(request.getAltura())
                .prontTemperatura(request.getTemperatura())
                .prontPeso(request.getPeso())
                .prontSaturacao(request.getSaturacao())
                .prontHemoglobina(request.getHemoglobina())
                // diagnóstico
                .prontDiagnostico(request.getDiagnostico())
                .prontOrientacoes(request.getOrientacoes())
                // prescrição
                .prontTituloPrescricao(request.getTituloPrescricao())
                .prontPrescricao(request.getPrescricao())
                // controle
                .prontDataFinalizado(request.getDataFinalizado())
                .prontTempoDuracao(request.getTempoDuracao())
                // identificação do paciente
                .prontResponsavel(request.getResponsavel())
                // TUSS, CID e exames
                .prontTussTexto(request.getTussTexto())
                .prontCidTexto(request.getCidTexto())
                .prontSolicitacaoExameTexto(request.getSolicitacaoExameTexto())
                // relacionamentos
                .profissional(profissional)
                .consulta(consulta)
                .build();
    }
}
