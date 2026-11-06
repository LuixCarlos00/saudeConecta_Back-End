package br.com.saudeConecta.application.assinatura;

import br.com.saudeConecta.application.assinatura.dto.PendenciaManualResponse;
import br.com.saudeConecta.application.exception.BusinessException;
import br.com.saudeConecta.application.exception.ResourceNotFoundException;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.planos.MpWebhookEvento;
import br.com.saudeConecta.infrastructure.persistence.repository.MpWebhookEventoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.OrganizacaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PendenciaManualService - Testes unitários")
class PendenciaManualServiceTest {

    private static final ZoneId SP = ZoneId.of("America/Sao_Paulo");
    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 11, 5, 15, 30);

    @Mock private MpWebhookEventoRepository eventoRepository;
    @Mock private OrganizacaoRepository organizacaoRepository;

    private PendenciaManualService service;

    @BeforeEach
    void criar() {
        service = new PendenciaManualService(eventoRepository, organizacaoRepository,
                Clock.fixed(AGORA.atZone(SP).toInstant(), SP));
    }

    private MpWebhookEvento pendencia(Long id, String motivo, Long organizacaoId) {
        MpWebhookEvento e = MpWebhookEvento.builder().id(id).tipo("subscription_authorized_payment")
                .dataId("fat-" + id).recebidoEm(AGORA.minusDays(1)).organizacaoId(organizacaoId).build();
        e.marcarParaAtencao(motivo, "contexto " + id);
        return e;
    }

    private Organizacao org(Long id, String nome) {
        Organizacao o = new Organizacao();
        o.setId(id);
        o.setNome(nome);
        return o;
    }

    @Test
    @DisplayName("Lista as abertas com o código do motivo e o nome da clínica (uma query de nomes)")
    void listarAbertas() {
        List<MpWebhookEvento> eventos = List.of(
                pendencia(1L, MpWebhookEvento.MOTIVO_PAGAMENTO_SEM_ASSINATURA, null),
                pendencia(2L, MpWebhookEvento.MOTIVO_PAGAMENTO_EM_CORTESIA, 7L),
                pendencia(3L, MpWebhookEvento.MOTIVO_PREAPPROVAL_SUBSTITUIDO_AUTORIZADO, 7L),
                pendencia(4L, MpWebhookEvento.MOTIVO_PAGAMENTO_EM_CANCELADA, 8L),
                pendencia(5L, MpWebhookEvento.MOTIVO_EVENTO_EXPIRADO, null));
        when(eventoRepository.findByRequerAtencaoTrueOrderByRecebidoEmDesc()).thenReturn(eventos);
        when(organizacaoRepository.findAllById(List.of(7L, 8L))).thenReturn(List.of(org(7L, "Alfa"), org(8L, "Beta")));

        List<PendenciaManualResponse> lista = service.listar(false);

        assertThat(lista).extracting(PendenciaManualResponse::motivoCodigo).containsExactly(
                "PAGAMENTO_SEM_ASSINATURA", "PAGAMENTO_EM_CORTESIA", "PREAPPROVAL_SUBSTITUIDO_AUTORIZADO",
                "PAGAMENTO_EM_CANCELADA", "EVENTO_EXPIRADO");
        assertThat(lista).extracting(PendenciaManualResponse::organizacaoNome)
                .containsExactly(null, "Alfa", "Alfa", "Beta", null);
        assertThat(lista).allSatisfy(p -> assertThat(p.resolvida()).isFalse());
    }

    @Test
    @DisplayName("resolvidas=true usa a lista das resolvidas")
    void listarResolvidas() {
        MpWebhookEvento e = pendencia(1L, MpWebhookEvento.MOTIVO_PAGAMENTO_EM_CORTESIA, null);
        e.resolver(99L, "estornado", AGORA);
        when(eventoRepository.findByResolvidoEmIsNotNullOrderByResolvidoEmDesc()).thenReturn(List.of(e));
        when(organizacaoRepository.findAllById(List.of())).thenReturn(List.of());

        assertThat(service.listar(true)).singleElement().satisfies(p -> {
            assertThat(p.resolvida()).isTrue();
            assertThat(p.observacaoResolucao()).isEqualTo("estornado");
            assertThat(p.resolvidoPorUsuarioId()).isEqualTo(99L);
        });
        verify(eventoRepository, never()).findByRequerAtencaoTrueOrderByRecebidoEmDesc();
    }

    @Test
    @DisplayName("Resolver: requer_atencao=false, guarda quem, quando e a observação")
    void resolver() {
        MpWebhookEvento e = pendencia(1L, MpWebhookEvento.MOTIVO_PAGAMENTO_EM_CANCELADA, 7L);
        when(eventoRepository.findByIdParaAtualizacao(1L)).thenReturn(Optional.of(e));
        when(organizacaoRepository.findById(7L)).thenReturn(Optional.of(org(7L, "Alfa")));

        PendenciaManualResponse r = service.resolver(1L, 99L, "  Estornado no painel do MP  ");

        assertThat(e.getRequerAtencao()).isFalse();
        assertThat(e.getResolvidoEm()).isEqualTo(AGORA);
        assertThat(e.getResolvidoPorUsuarioId()).isEqualTo(99L);
        assertThat(e.getObservacaoResolucao()).isEqualTo("Estornado no painel do MP");
        assertThat(r.resolvida()).isTrue();
        assertThat(r.organizacaoNome()).isEqualTo("Alfa");
        verify(eventoRepository).save(e);
    }

    @Test
    @DisplayName("Resolver duas vezes: 409 PENDENCIA_JA_RESOLVIDA e a primeira resolução fica")
    void resolverDuasVezes() {
        MpWebhookEvento e = pendencia(1L, MpWebhookEvento.MOTIVO_PAGAMENTO_EM_CANCELADA, null);
        e.resolver(98L, "primeira", AGORA.minusHours(1));
        when(eventoRepository.findByIdParaAtualizacao(1L)).thenReturn(Optional.of(e));

        assertThatThrownBy(() -> service.resolver(1L, 99L, "segunda"))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo("PENDENCIA_JA_RESOLVIDA"));
        assertThat(e.getObservacaoResolucao()).isEqualTo("primeira");
        verify(eventoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Resolver evento inexistente ou que nunca foi pendência: 404")
    void resolverNaoPendencia() {
        MpWebhookEvento comum = MpWebhookEvento.builder().id(2L).tipo("x").dataId("y").build();
        when(eventoRepository.findByIdParaAtualizacao(2L)).thenReturn(Optional.of(comum));
        when(eventoRepository.findByIdParaAtualizacao(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolver(2L, 99L, "x")).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.resolver(3L, 99L, "x")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Reentrega da notificação não reabre a pendência resolvida")
    void reentregaNaoReabre() {
        MpWebhookEvento e = pendencia(1L, MpWebhookEvento.MOTIVO_PAGAMENTO_SEM_ASSINATURA, null);
        e.resolver(99L, "registrado à mão", AGORA);

        e.marcarParaAtencao(MpWebhookEvento.MOTIVO_PAGAMENTO_SEM_ASSINATURA, "de novo");

        assertThat(e.getRequerAtencao()).isFalse();
        assertThat(e.getMotivoAtencao()).contains("contexto 1");
    }
}
