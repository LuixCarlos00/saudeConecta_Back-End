package br.com.saudeConecta.application.prontuario;

import br.com.saudeConecta.application.cache.CacheEvictionService;
import br.com.saudeConecta.application.prontuario.dto.QuestionarioSaudeRequest;
import br.com.saudeConecta.config.tenant.TenantContext;
import br.com.saudeConecta.config.tenant.TenantHelper;
import br.com.saudeConecta.domain.consulta.Consulta;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.paciente.Paciente;
import br.com.saudeConecta.domain.prontuario.TermoAutorizacao;
import br.com.saudeConecta.infrastructure.persistence.repository.ConsultaRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.TermoAutorizacaoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
@DisplayName("TermoAutorizacaoService - isolamento por tenant e questionário público")
class TermoAutorizacaoServiceTest {

    private static final Long ORG = 1L;
    private static final Long OUTRA_ORG = 2L;

    @Mock private TermoAutorizacaoRepository termoRepository;
    @Mock private ConsultaRepository consultaRepository;
    @Mock private CacheEvictionService cacheEvictionService;
    @Spy private TenantHelper tenantHelper = new TenantHelper();

    @InjectMocks
    private TermoAutorizacaoService service;

    @BeforeEach
    void definirTenant() {
        TenantContext.setCurrentTenant(ORG);
    }

    @AfterEach
    void limpar() {
        TenantContext.clear();
    }

    private Consulta consulta(Long id, Long orgId) {
        Organizacao org = new Organizacao();
        org.setId(orgId);
        Consulta c = new Consulta();
        c.setId(id);
        c.setOrganizacao(org);
        c.setPaciente(new Paciente());
        return c;
    }

    private TermoAutorizacao termoPendente(Long orgId) {
        Consulta c = consulta(10L, orgId);
        return TermoAutorizacao.builder()
                .consulta(c)
                .organizacao(c.getOrganizacao())
                .token("abcdef-1234-5678")
                .dataExpiracao(LocalDateTime.now().plusHours(1))
                .status("PENDENTE")
                .build();
    }

    @Test
    @DisplayName("gerarLinkQuestionario recusa consulta de outra clínica e não cria termo")
    void gerarLinkOutraClinica() {
        lenient().when(consultaRepository.findById(10L)).thenReturn(Optional.of(consulta(10L, OUTRA_ORG)));

        assertThatThrownBy(() -> service.gerarLinkQuestionario(10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Consulta nao encontrada");
        verify(consultaRepository).findByIdAndOrganizacao_Id(10L, ORG);
        verify(termoRepository, never()).save(any());
    }

    @Test
    @DisplayName("gerarLinkQuestionario invalida o cache com a chave do tenant e não loga o token inteiro")
    void gerarLinkInvalidaCacheENaoLogaToken(CapturedOutput output) {
        when(consultaRepository.findByIdAndOrganizacao_Id(10L, ORG)).thenReturn(Optional.of(consulta(10L, ORG)));
        when(termoRepository.findByConsultaIdWithLock(10L)).thenReturn(Optional.empty());

        String token = service.gerarLinkQuestionario(10L);

        verify(cacheEvictionService).evictQuestionarioSaude(ORG, 10L);
        assertThat(output.getAll()).contains("token=" + token.substring(0, 6)).doesNotContain(token);
    }

    @Test
    @DisplayName("buscarPorConsultaId filtra pela organização do token")
    void buscarPorConsultaIdFiltrado() {
        service.buscarPorConsultaId(10L);

        verify(termoRepository).findByConsultaIdAndOrganizacaoId(10L, ORG);
        verify(termoRepository, never()).findByConsultaId(any());
    }

    @Test
    @DisplayName("responderQuestionario usa UPDATE condicional: segunda resposta concorrente é recusada")
    void responderUsaUpdateCondicional() {
        TenantContext.clear(); // rota pública, sem tenant
        TermoAutorizacao termo = termoPendente(ORG);
        when(termoRepository.findByToken("abcdef-1234-5678")).thenReturn(Optional.of(termo));
        when(termoRepository.responderSePendente(eq("abcdef-1234-5678"), any(), any(), any(), any()))
                .thenReturn(0);

        QuestionarioSaudeRequest request = new QuestionarioSaudeRequest();
        request.setToken("abcdef-1234-5678");

        assertThatThrownBy(() -> service.responderQuestionario(request, "10.0.0.1"))
                .isInstanceOf(IllegalStateException.class);
        verify(termoRepository, never()).save(any());
        verify(cacheEvictionService, never()).evictQuestionarioSaude(any(), any());
    }

    @Test
    @DisplayName("responderQuestionario invalida o cache da clínica dona do termo (sem tenant na rota pública)")
    void responderInvalidaCacheDaClinicaDoTermo(CapturedOutput output) {
        TenantContext.clear();
        TermoAutorizacao termo = termoPendente(OUTRA_ORG);
        when(termoRepository.findByToken("abcdef-1234-5678")).thenReturn(Optional.of(termo));
        when(termoRepository.responderSePendente(eq("abcdef-1234-5678"), any(), any(), any(), any()))
                .thenReturn(1);

        QuestionarioSaudeRequest request = new QuestionarioSaudeRequest();
        request.setToken("abcdef-1234-5678");

        service.responderQuestionario(request, "10.0.0.1");

        verify(cacheEvictionService).evictQuestionarioSaude(OUTRA_ORG, 10L);
        assertThat(output.getAll()).contains("token=abcdef***").doesNotContain("abcdef-1234-5678");
    }
}
