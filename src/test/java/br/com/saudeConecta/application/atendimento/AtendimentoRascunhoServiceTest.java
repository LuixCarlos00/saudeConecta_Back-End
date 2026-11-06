package br.com.saudeConecta.application.atendimento;

import br.com.saudeConecta.application.atendimento.dto.RascunhoAtendimentoResponse;
import br.com.saudeConecta.application.atendimento.dto.RascunhoAtendimentoResumoResponse;
import br.com.saudeConecta.application.atendimento.dto.SalvarRascunhoAtendimentoRequest;
import br.com.saudeConecta.application.exception.BusinessException;
import br.com.saudeConecta.application.exception.ResourceNotFoundException;
import br.com.saudeConecta.config.tenant.TenantContext;
import br.com.saudeConecta.config.tenant.TenantHelper;
import br.com.saudeConecta.domain.atendimento.AtendimentoRascunho;
import br.com.saudeConecta.domain.atendimento.RascunhoAtendimentoResumo;
import br.com.saudeConecta.domain.atendimento.TipoAtendimentoRascunho;
import br.com.saudeConecta.domain.consulta.Consulta;
import br.com.saudeConecta.domain.consulta.StatusConsulta;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.infrastructure.persistence.repository.AtendimentoRascunhoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.ConsultaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AtendimentoRascunhoService - Testes unitários")
class AtendimentoRascunhoServiceTest {

    private static final Long ORG = 1L;
    private static final Long USUARIO_DONO = 50L;
    private static final Long OUTRO_USUARIO = 51L;
    private static final Long CONSULTA = 822L;

    @Mock private AtendimentoRascunhoRepository rascunhoRepository;
    @Mock private ConsultaRepository consultaRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private AtendimentoRascunhoService service;

    @BeforeEach
    void setUp() {
        TenantContext.setCurrentTenant(ORG);
        TenantContext.setCurrentUser(USUARIO_DONO);
        service = new AtendimentoRascunhoService(rascunhoRepository, consultaRepository, new TenantHelper(), objectMapper);
    }

    @AfterEach
    void limpar() {
        TenantContext.clear();
    }

    // ========== Helpers ==========

    private Consulta consultaDe(Long usuarioId, StatusConsulta status) {
        Usuario usuario = new Usuario();
        usuario.setId(usuarioId);
        Profissional profissional = new Profissional();
        profissional.setId(7L);
        profissional.setUsuario(usuario);
        Organizacao org = new Organizacao();
        org.setId(ORG);
        Consulta c = new Consulta();
        c.setId(CONSULTA);
        c.setOrganizacao(org);
        c.setProfissional(profissional);
        c.setStatus(status);
        c.setDataHora(LocalDateTime.now());
        return c;
    }

    private AtendimentoRascunho rascunhoExistente(Consulta consulta, long versao) {
        return AtendimentoRascunho.builder()
                .id(3L).consulta(consulta).organizacao(consulta.getOrganizacao()).profissional(consulta.getProfissional())
                .tipo(TipoAtendimentoRascunho.MEDICO).dados("{\"exame\":{\"peso\":70}}")
                .tempoAcumuladoSegundos(100).versao(versao).atualizadoEm(LocalDateTime.now())
                .build();
    }

    private JsonNode json(String texto) throws Exception {
        return objectMapper.readTree(texto);
    }

    private void consultaComLock(Consulta consulta) {
        when(consultaRepository.buscarComLock(CONSULTA, ORG)).thenReturn(Optional.of(consulta));
    }

    /** Simula o Hibernate: define id na inserção e incrementa a versão. */
    private void saveAndFlushIncrementaVersao() {
        when(rascunhoRepository.saveAndFlush(any())).thenAnswer(inv -> {
            AtendimentoRascunho r = inv.getArgument(0);
            if (r.getId() == null) {
                r.setId(3L);
            }
            r.setVersao(r.getVersao() == null ? 0L : r.getVersao() + 1);
            r.setAtualizadoEm(LocalDateTime.of(2026, 11, 5, 14, 32, 10));
            return r;
        });
    }

    private static void assertCodigo(Throwable t, HttpStatus status, String code) {
        assertThat(t).isInstanceOf(BusinessException.class);
        BusinessException be = (BusinessException) t;
        assertThat(be.getStatus()).isEqualTo(status);
        assertThat(be.getErrorCode()).isEqualTo(code);
    }

    // ========== salvar ==========

    @Nested
    @DisplayName("salvar (PUT)")
    class Salvar {

        @Test
        @DisplayName("Primeira gravação (versao null) cria o rascunho com dono, clínica e dados")
        void criaRascunho() throws Exception {
            Consulta consulta = consultaDe(USUARIO_DONO, StatusConsulta.AGENDADA);
            consultaComLock(consulta);
            when(rascunhoRepository.findByConsulta_IdAndOrganizacao_Id(CONSULTA, ORG)).thenReturn(Optional.empty());
            saveAndFlushIncrementaVersao();

            RascunhoAtendimentoResponse resp = service.salvar(CONSULTA, new SalvarRascunhoAtendimentoRequest(
                    "MEDICO", json("{\"identificacao\":{\"nome\":\"x\"}}"), 754, null));

            assertThat(resp.consultaId()).isEqualTo(CONSULTA);
            assertThat(resp.tipo()).isEqualTo("MEDICO");
            assertThat(resp.versao()).isZero();
            assertThat(resp.tempoAcumuladoSegundos()).isEqualTo(754);
            assertThat(resp.dados().get("identificacao").get("nome").asText()).isEqualTo("x");
            verify(rascunhoRepository).saveAndFlush(argThat(r -> r.getProfissional() == consulta.getProfissional()
                    && r.getOrganizacao() == consulta.getOrganizacao() && r.getConsulta() == consulta));
        }

        @Test
        @DisplayName("Atualiza com a versão atual e devolve a versão nova")
        void atualizaRascunho() throws Exception {
            Consulta consulta = consultaDe(USUARIO_DONO, StatusConsulta.CONFIRMADA);
            consultaComLock(consulta);
            AtendimentoRascunho existente = rascunhoExistente(consulta, 3L);
            when(rascunhoRepository.findByConsulta_IdAndOrganizacao_Id(CONSULTA, ORG)).thenReturn(Optional.of(existente));
            saveAndFlushIncrementaVersao();

            RascunhoAtendimentoResponse resp = service.salvar(CONSULTA, new SalvarRascunhoAtendimentoRequest(
                    "DENTISTA", json("{\"codigos\":[1,2]}"), 900, 3L));

            assertThat(resp.versao()).isEqualTo(4L);
            assertThat(resp.tipo()).isEqualTo("DENTISTA");
            assertThat(resp.dados().get("codigos").size()).isEqualTo(2);
            assertThat(existente.getDados()).isEqualTo("{\"codigos\":[1,2]}");
            assertThat(existente.getTempoAcumuladoSegundos()).isEqualTo(900);
        }

        @Test
        @DisplayName("tempoAcumuladoSegundos ausente vira 0")
        void tempoAusenteViraZero() throws Exception {
            consultaComLock(consultaDe(USUARIO_DONO, StatusConsulta.AGENDADA));
            when(rascunhoRepository.findByConsulta_IdAndOrganizacao_Id(CONSULTA, ORG)).thenReturn(Optional.empty());
            saveAndFlushIncrementaVersao();

            assertThat(service.salvar(CONSULTA, new SalvarRascunhoAtendimentoRequest("MEDICO", json("{}"), null, null))
                    .tempoAcumuladoSegundos()).isZero();
        }

        @Test
        @DisplayName("Versão enviada diferente da atual: 409 RASCUNHO_DESATUALIZADO, sem gravar")
        void versaoDesatualizada() throws Exception {
            Consulta consulta = consultaDe(USUARIO_DONO, StatusConsulta.AGENDADA);
            consultaComLock(consulta);
            when(rascunhoRepository.findByConsulta_IdAndOrganizacao_Id(CONSULTA, ORG))
                    .thenReturn(Optional.of(rascunhoExistente(consulta, 5L)));
            var req = new SalvarRascunhoAtendimentoRequest("MEDICO", json("{}"), 1, 4L);

            assertThatThrownBy(() -> service.salvar(CONSULTA, req))
                    .satisfies(t -> assertCodigo(t, HttpStatus.CONFLICT, "RASCUNHO_DESATUALIZADO"));
            verify(rascunhoRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Primeira gravação (versao null) com rascunho já existente: 409 RASCUNHO_DESATUALIZADO")
        void versaoNullComRascunhoExistente() throws Exception {
            Consulta consulta = consultaDe(USUARIO_DONO, StatusConsulta.AGENDADA);
            consultaComLock(consulta);
            when(rascunhoRepository.findByConsulta_IdAndOrganizacao_Id(CONSULTA, ORG))
                    .thenReturn(Optional.of(rascunhoExistente(consulta, 0L)));
            var req = new SalvarRascunhoAtendimentoRequest("MEDICO", json("{}"), 1, null);

            assertThatThrownBy(() -> service.salvar(CONSULTA, req))
                    .satisfies(t -> assertCodigo(t, HttpStatus.CONFLICT, "RASCUNHO_DESATUALIZADO"));
        }

        @Test
        @DisplayName("Versão enviada mas rascunho descartado em outra aba: 409 RASCUNHO_DESATUALIZADO")
        void versaoInformadaSemRascunho() throws Exception {
            consultaComLock(consultaDe(USUARIO_DONO, StatusConsulta.AGENDADA));
            when(rascunhoRepository.findByConsulta_IdAndOrganizacao_Id(CONSULTA, ORG)).thenReturn(Optional.empty());
            var req = new SalvarRascunhoAtendimentoRequest("MEDICO", json("{}"), 1, 2L);

            assertThatThrownBy(() -> service.salvar(CONSULTA, req))
                    .satisfies(t -> assertCodigo(t, HttpStatus.CONFLICT, "RASCUNHO_DESATUALIZADO"));
        }

        @Test
        @DisplayName("Duas primeiras gravações simultâneas: a que bate no UNIQUE recebe 409, não 500")
        void corridaNoUniqueVira409() throws Exception {
            consultaComLock(consultaDe(USUARIO_DONO, StatusConsulta.AGENDADA));
            when(rascunhoRepository.findByConsulta_IdAndOrganizacao_Id(CONSULTA, ORG)).thenReturn(Optional.empty());
            when(rascunhoRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("uk_atendimento_rascunho_consulta"));
            var req = new SalvarRascunhoAtendimentoRequest("MEDICO", json("{}"), 1, null);

            assertThatThrownBy(() -> service.salvar(CONSULTA, req))
                    .satisfies(t -> assertCodigo(t, HttpStatus.CONFLICT, "RASCUNHO_DESATUALIZADO"));
        }

        @Test
        @DisplayName("Atualização que perde a corrida do @Version recebe 409 RASCUNHO_DESATUALIZADO")
        void corridaNaVersaoVira409() throws Exception {
            Consulta consulta = consultaDe(USUARIO_DONO, StatusConsulta.AGENDADA);
            consultaComLock(consulta);
            when(rascunhoRepository.findByConsulta_IdAndOrganizacao_Id(CONSULTA, ORG))
                    .thenReturn(Optional.of(rascunhoExistente(consulta, 1L)));
            when(rascunhoRepository.saveAndFlush(any()))
                    .thenThrow(new ObjectOptimisticLockingFailureException(AtendimentoRascunho.class, 3L));
            var req = new SalvarRascunhoAtendimentoRequest("MEDICO", json("{}"), 1, 1L);

            assertThatThrownBy(() -> service.salvar(CONSULTA, req))
                    .satisfies(t -> assertCodigo(t, HttpStatus.CONFLICT, "RASCUNHO_DESATUALIZADO"));
        }

        @ParameterizedTest
        @EnumSource(value = StatusConsulta.class, names = {"REALIZADA", "PAGO", "CANCELADA"})
        @DisplayName("Consulta encerrada: 409 CONSULTA_ENCERRADA")
        void consultaEncerrada(StatusConsulta status) throws Exception {
            consultaComLock(consultaDe(USUARIO_DONO, status));
            var req = new SalvarRascunhoAtendimentoRequest("MEDICO", json("{}"), 1, null);

            assertThatThrownBy(() -> service.salvar(CONSULTA, req))
                    .satisfies(t -> assertCodigo(t, HttpStatus.CONFLICT, "CONSULTA_ENCERRADA"));
            verify(rascunhoRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Consulta de outra clínica: 404")
        void consultaDeOutraClinica() throws Exception {
            when(consultaRepository.buscarComLock(CONSULTA, ORG)).thenReturn(Optional.empty());
            var req = new SalvarRascunhoAtendimentoRequest("MEDICO", json("{}"), 1, null);

            assertThatThrownBy(() -> service.salvar(CONSULTA, req)).isInstanceOf(ResourceNotFoundException.class);
            verify(rascunhoRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Consulta de outro profissional da mesma clínica: 404")
        void consultaDeOutroProfissional() throws Exception {
            consultaComLock(consultaDe(OUTRO_USUARIO, StatusConsulta.AGENDADA));
            var req = new SalvarRascunhoAtendimentoRequest("MEDICO", json("{}"), 1, null);

            assertThatThrownBy(() -> service.salvar(CONSULTA, req)).isInstanceOf(ResourceNotFoundException.class);
            verify(rascunhoRepository, never()).saveAndFlush(any());
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"", "medico", "ADMIN", "MEDICO "})
        @DisplayName("Tipo inválido: 422 RASCUNHO_INVALIDO")
        void tipoInvalido(String tipo) throws Exception {
            consultaComLock(consultaDe(USUARIO_DONO, StatusConsulta.AGENDADA));
            var req = new SalvarRascunhoAtendimentoRequest(tipo, json("{}"), 1, null);

            assertThatThrownBy(() -> service.salvar(CONSULTA, req))
                    .satisfies(t -> assertCodigo(t, HttpStatus.UNPROCESSABLE_ENTITY, "RASCUNHO_INVALIDO"));
        }

        @Test
        @DisplayName("Dados ausentes (null ou JSON null): 422 RASCUNHO_INVALIDO")
        void dadosAusentes() {
            consultaComLock(consultaDe(USUARIO_DONO, StatusConsulta.AGENDADA));

            assertThatThrownBy(() -> service.salvar(CONSULTA, new SalvarRascunhoAtendimentoRequest("MEDICO", null, 1, null)))
                    .satisfies(t -> assertCodigo(t, HttpStatus.UNPROCESSABLE_ENTITY, "RASCUNHO_INVALIDO"));
            assertThatThrownBy(() -> service.salvar(CONSULTA,
                    new SalvarRascunhoAtendimentoRequest("MEDICO", NullNode.getInstance(), 1, null)))
                    .satisfies(t -> assertCodigo(t, HttpStatus.UNPROCESSABLE_ENTITY, "RASCUNHO_INVALIDO"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"\"texto\"", "[1,2]", "42", "true"})
        @DisplayName("Dados que não são objeto JSON (string, array, número): 422 RASCUNHO_INVALIDO")
        void dadosNaoObjeto(String jsonDados) throws Exception {
            consultaComLock(consultaDe(USUARIO_DONO, StatusConsulta.AGENDADA));
            var req = new SalvarRascunhoAtendimentoRequest("MEDICO", json(jsonDados), 1, null);

            assertThatThrownBy(() -> service.salvar(CONSULTA, req))
                    .satisfies(t -> assertCodigo(t, HttpStatus.UNPROCESSABLE_ENTITY, "RASCUNHO_INVALIDO"));
            verify(rascunhoRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Tempo negativo: 422 RASCUNHO_INVALIDO")
        void tempoNegativo() throws Exception {
            consultaComLock(consultaDe(USUARIO_DONO, StatusConsulta.AGENDADA));
            var req = new SalvarRascunhoAtendimentoRequest("MEDICO", json("{}"), -1, null);

            assertThatThrownBy(() -> service.salvar(CONSULTA, req))
                    .satisfies(t -> assertCodigo(t, HttpStatus.UNPROCESSABLE_ENTITY, "RASCUNHO_INVALIDO"));
        }

        @Test
        @DisplayName("Dados acima de 2 MB: 422 RASCUNHO_MUITO_GRANDE; exatamente no limite passa")
        void limiteDeTamanho() throws Exception {
            consultaComLock(consultaDe(USUARIO_DONO, StatusConsulta.AGENDADA));
            when(rascunhoRepository.findByConsulta_IdAndOrganizacao_Id(CONSULTA, ORG)).thenReturn(Optional.empty());
            saveAndFlushIncrementaVersao();
            // {"a":"xxx..."} = 8 bytes de moldura + conteúdo
            int moldura = "{\"a\":\"\"}".length();
            String noLimite = "{\"a\":\"" + "x".repeat(AtendimentoRascunhoService.LIMITE_DADOS_BYTES - moldura) + "\"}";
            String acima = "{\"a\":\"" + "x".repeat(AtendimentoRascunhoService.LIMITE_DADOS_BYTES - moldura + 1) + "\"}";

            assertThat(service.salvar(CONSULTA, new SalvarRascunhoAtendimentoRequest("MEDICO", json(noLimite), 1, null)))
                    .isNotNull();
            var reqAcima = new SalvarRascunhoAtendimentoRequest("MEDICO", json(acima), 1, null);
            assertThatThrownBy(() -> service.salvar(CONSULTA, reqAcima))
                    .satisfies(t -> assertCodigo(t, HttpStatus.UNPROCESSABLE_ENTITY, "RASCUNHO_MUITO_GRANDE"));
        }
    }

    // ========== buscar ==========

    @Nested
    @DisplayName("buscar (GET)")
    class Buscar {

        @Test
        @DisplayName("Devolve dados como objeto JSON, igual ao salvo")
        void devolveRascunho() {
            Consulta consulta = consultaDe(USUARIO_DONO, StatusConsulta.AGENDADA);
            when(consultaRepository.findByIdAndOrganizacao_Id(CONSULTA, ORG)).thenReturn(Optional.of(consulta));
            when(rascunhoRepository.findByConsulta_IdAndOrganizacao_Id(CONSULTA, ORG))
                    .thenReturn(Optional.of(rascunhoExistente(consulta, 2L)));

            RascunhoAtendimentoResponse resp = service.buscar(CONSULTA).orElseThrow();

            assertThat(resp.dados().isObject()).isTrue();
            assertThat(resp.dados().get("exame").get("peso").asInt()).isEqualTo(70);
            assertThat(resp.versao()).isEqualTo(2L);
        }

        @Test
        @DisplayName("Sem rascunho: vazio (204)")
        void semRascunho() {
            when(consultaRepository.findByIdAndOrganizacao_Id(CONSULTA, ORG))
                    .thenReturn(Optional.of(consultaDe(USUARIO_DONO, StatusConsulta.AGENDADA)));
            when(rascunhoRepository.findByConsulta_IdAndOrganizacao_Id(CONSULTA, ORG)).thenReturn(Optional.empty());

            assertThat(service.buscar(CONSULTA)).isEmpty();
        }

        @Test
        @DisplayName("Consulta de outro profissional: 404 sem consultar o rascunho")
        void outroProfissional() {
            when(consultaRepository.findByIdAndOrganizacao_Id(CONSULTA, ORG))
                    .thenReturn(Optional.of(consultaDe(OUTRO_USUARIO, StatusConsulta.AGENDADA)));

            assertThatThrownBy(() -> service.buscar(CONSULTA)).isInstanceOf(ResourceNotFoundException.class);
            verifyNoInteractions(rascunhoRepository);
        }

        @Test
        @DisplayName("Consulta de outra clínica: 404")
        void outraClinica() {
            when(consultaRepository.findByIdAndOrganizacao_Id(CONSULTA, ORG)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.buscar(CONSULTA)).isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ========== descartar ==========

    @Nested
    @DisplayName("descartar (DELETE)")
    class Descartar {

        @Test
        @DisplayName("Idempotente: apaga quando existe e não falha quando não existe")
        void idempotente() {
            when(consultaRepository.buscarComLock(CONSULTA, ORG))
                    .thenReturn(Optional.of(consultaDe(USUARIO_DONO, StatusConsulta.AGENDADA)));
            when(rascunhoRepository.apagarPorConsulta(CONSULTA)).thenReturn(1, 0);

            service.descartar(CONSULTA);
            service.descartar(CONSULTA);

            verify(rascunhoRepository, times(2)).apagarPorConsulta(CONSULTA);
        }

        @Test
        @DisplayName("Consulta de outro profissional: 404 e nada é apagado")
        void outroProfissional() {
            when(consultaRepository.buscarComLock(CONSULTA, ORG))
                    .thenReturn(Optional.of(consultaDe(OUTRO_USUARIO, StatusConsulta.AGENDADA)));

            assertThatThrownBy(() -> service.descartar(CONSULTA)).isInstanceOf(ResourceNotFoundException.class);
            verify(rascunhoRepository, never()).apagarPorConsulta(anyLong());
        }
    }

    // ========== listarMeus ==========

    @Nested
    @DisplayName("listarMeus (GET /rascunhos/meus)")
    class ListarMeus {

        private RascunhoAtendimentoResumo resumo(Long consultaId, int tempo) {
            return new RascunhoAtendimentoResumo() {
                @Override public Long getConsultaId() { return consultaId; }
                @Override public LocalDateTime getAtualizadoEm() { return LocalDateTime.of(2026, 11, 5, 14, 32, 10); }
                @Override public Integer getTempoAcumuladoSegundos() { return tempo; }
            };
        }

        @Test
        @DisplayName("Filtra pela clínica do token e pelo usuário logado")
        void filtraPorProfissionalLogado() {
            when(rascunhoRepository.listarDoUsuario(ORG, USUARIO_DONO)).thenReturn(List.of(resumo(822L, 754)));

            List<RascunhoAtendimentoResumoResponse> lista = service.listarMeus();

            assertThat(lista).containsExactly(
                    new RascunhoAtendimentoResumoResponse(822L, LocalDateTime.of(2026, 11, 5, 14, 32, 10), 754));
            verify(rascunhoRepository).listarDoUsuario(ORG, USUARIO_DONO);
        }

        @Test
        @DisplayName("Sem usuário no contexto: lista vazia sem ir ao banco")
        void semUsuario() {
            TenantContext.setCurrentUser(null);

            assertThat(service.listarMeus()).isEmpty();
            verifyNoInteractions(rascunhoRepository);
        }
    }
}
