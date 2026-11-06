package br.com.saudeConecta.application.prontuario;

import br.com.saudeConecta.application.exception.BusinessException;
import br.com.saudeConecta.application.exception.ResourceNotFoundException;
import br.com.saudeConecta.application.prontuario.dto.CadastrarProntuarioDentistaRequest;
import br.com.saudeConecta.config.tenant.TenantContext;
import br.com.saudeConecta.config.tenant.TenantHelper;
import br.com.saudeConecta.domain.consulta.Consulta;
import br.com.saudeConecta.domain.consulta.StatusConsulta;
import br.com.saudeConecta.domain.paciente.Paciente;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.domain.prontuario.PlanejamentoTerapeutico;
import br.com.saudeConecta.domain.prontuario.ProntuarioDentista;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.infrastructure.persistence.repository.AtendimentoRascunhoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.ConsultaRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.PacienteRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.ProfissionalRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.ProntuarioDentistaRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProntuarioDentistaService - Testes unitários")
class ProntuarioDentistaServiceTest {

    private static final Long ORG = 1L;
    /** Usuário (login) do profissional da consulta: o dono do prontuário. */
    private static final Long USUARIO_DONO = 500L;

    @BeforeEach
    void definirTenant() {
        TenantContext.setCurrentTenant(ORG);
        TenantContext.setCurrentUser(USUARIO_DONO);
    }

    @AfterEach
    void limparTenantContext() {
        TenantContext.clear();
    }

    @Mock private ProntuarioDentistaRepository prontuarioDentistaRepository;
    @Mock private ProfissionalRepository profissionalRepository;
    @Mock private ConsultaRepository consultaRepository;
    @Mock private AtendimentoRascunhoRepository atendimentoRascunhoRepository;
    @Mock private PacienteRepository pacienteRepository;
    @Mock private EntityManager entityManager;
    @Spy private TenantHelper tenantHelper = new TenantHelper();

    @InjectMocks
    private ProntuarioDentistaService service;

    // ========== Helpers ==========

    private Profissional criarProfissional(Long id) {
        Profissional p = new Profissional();
        p.setId(id);
        p.setNome("Dr. Dentista");
        return p;
    }

    private Consulta criarConsulta(Long id) {
        Consulta c = new Consulta();
        c.setId(id);
        c.setDataHora(LocalDateTime.now());
        c.setStatus(StatusConsulta.AGENDADA);
        Profissional dono = criarProfissional(1L);
        Usuario usuario = new Usuario();
        usuario.setId(USUARIO_DONO);
        dono.setUsuario(usuario);
        c.setProfissional(dono);
        return c;
    }

    private ProntuarioDentista criarProntuario(Long id) {
        ProntuarioDentista p = new ProntuarioDentista();
        p.setCodigo(id);
        p.setConsulta(criarConsulta(10L));
        p.setProfissional(criarProfissional(1L));
        return p;
    }

    private CadastrarProntuarioDentistaRequest criarRequest() {
        CadastrarProntuarioDentistaRequest req = new CadastrarProntuarioDentistaRequest();
        req.setConsulta(10L);
        req.setCodigoMedico(1L);
        req.setQueixaPrincipal("Dor de dente");
        req.setAnamnese("Paciente relata...");
        req.setDiagnostico("Cárie");
        req.setOdontograma(Collections.emptyList());
        req.setPlanejamentos(Collections.emptyList());
        return req;
    }

    private CadastrarProntuarioDentistaRequest.PlanejamentoItem criarItemPlanejamento(Long pacienteId) {
        CadastrarProntuarioDentistaRequest.PlanejamentoItem item =
                new CadastrarProntuarioDentistaRequest.PlanejamentoItem();
        item.setProcedimentoRealizado("Exodontia");
        item.setValor(BigDecimal.valueOf(180.00));
        item.setDataProcedimento("2024-06-15");
        item.setPacienteId(pacienteId);
        return item;
    }

    private CadastrarProntuarioDentistaRequest.DenteRequest criarDente(Integer fdi, String status, String obs) {
        CadastrarProntuarioDentistaRequest.DenteRequest dr =
                new CadastrarProntuarioDentistaRequest.DenteRequest();
        dr.setNumeroFdi(fdi);
        dr.setStatus(status);
        dr.setObservacao(obs);
        return dr;
    }

    // ========== cadastrarProntuarioByOrg ==========

    @Nested
    @DisplayName("cadastrarProntuarioByOrg")
    class CadastrarProntuario {

        @Test
        @DisplayName("Finalizar apaga o rascunho da consulta depois de marcar REALIZADA")
        void finalizarApagaRascunhoDaConsulta() {
            Consulta consulta = criarConsulta(10L);
            when(profissionalRepository.findByIdAndOrganizacao_Id(1L, ORG)).thenReturn(Optional.of(criarProfissional(1L)));
            when(consultaRepository.buscarComLock(10L, ORG)).thenReturn(Optional.of(consulta));
            when(prontuarioDentistaRepository.save(any())).thenReturn(criarProntuario(100L));
            when(atendimentoRascunhoRepository.apagarPorConsulta(10L)).thenReturn(1);

            service.cadastrarProntuarioByOrg(criarRequest());

            var ordem = inOrder(consultaRepository, atendimentoRascunhoRepository);
            ordem.verify(consultaRepository).save(consulta);
            ordem.verify(atendimentoRascunhoRepository).apagarPorConsulta(10L);
            assertThat(consulta.getStatus()).isEqualTo(StatusConsulta.REALIZADA);
        }

        @Test
        @DisplayName("Falha ao gravar o prontuário mantém o rascunho")
        void falhaAoGravarProntuarioMantemRascunho() {
            when(profissionalRepository.findByIdAndOrganizacao_Id(1L, ORG)).thenReturn(Optional.of(criarProfissional(1L)));
            when(consultaRepository.buscarComLock(10L, ORG)).thenReturn(Optional.of(criarConsulta(10L)));
            when(prontuarioDentistaRepository.save(any())).thenThrow(new org.springframework.dao.DataIntegrityViolationException("falha"));

            var requisicao = criarRequest();
            assertThatThrownBy(() -> service.cadastrarProntuarioByOrg(requisicao))
                    .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);

            verify(atendimentoRascunhoRepository, never()).apagarPorConsulta(any());
        }

        @Test
        @DisplayName("Segundo Finalizar da mesma consulta recebe 409 PRONTUARIO_JA_FINALIZADO e não grava nada")
        void segundoFinalizarDaMesmaConsultaRecebe409() {
            when(profissionalRepository.findByIdAndOrganizacao_Id(1L, ORG)).thenReturn(Optional.of(criarProfissional(1L)));
            when(consultaRepository.buscarComLock(10L, ORG)).thenReturn(Optional.of(criarConsulta(10L)));
            when(prontuarioDentistaRepository.existsByConsulta_Id(10L)).thenReturn(true);

            var requisicao = criarRequest();
            assertThatThrownBy(() -> service.cadastrarProntuarioByOrg(requisicao))
                    .isInstanceOf(br.com.saudeConecta.application.exception.BusinessException.class)
                    .satisfies(ex -> {
                        var be = (br.com.saudeConecta.application.exception.BusinessException) ex;
                        assertThat(be.getStatus()).isEqualTo(org.springframework.http.HttpStatus.CONFLICT);
                        assertThat(be.getErrorCode()).isEqualTo("PRONTUARIO_JA_FINALIZADO");
                    });

            verify(prontuarioDentistaRepository, never()).save(any(ProntuarioDentista.class));
            verify(consultaRepository, never()).save(any());
            verify(atendimentoRascunhoRepository, never()).apagarPorConsulta(any());
        }

        @Test
        @DisplayName("Deve cadastrar prontuário odontológico com sucesso")
        void deveCadastrarComSucesso() {
            CadastrarProntuarioDentistaRequest request = criarRequest();
            ProntuarioDentista prontuario = criarProntuario(100L);

            when(profissionalRepository.findByIdAndOrganizacao_Id(1L, ORG)).thenReturn(Optional.of(criarProfissional(1L)));
            when(consultaRepository.buscarComLock(10L, ORG)).thenReturn(Optional.of(criarConsulta(10L)));
            when(prontuarioDentistaRepository.save(any())).thenReturn(prontuario);
            when(consultaRepository.save(any())).thenReturn(criarConsulta(10L));

            ProntuarioDentista resultado = service.cadastrarProntuarioByOrg(request);

            assertThat(resultado).isNotNull();
            assertThat(resultado.getCodigo()).isEqualTo(100L);
            verify(prontuarioDentistaRepository).save(any(ProntuarioDentista.class));
        }

        @Test
        @DisplayName("Deve lançar exceção quando profissional não encontrado")
        void deveLancarExcecaoQuandoProfissionalNaoEncontrado() {
            CadastrarProntuarioDentistaRequest request = criarRequest();
            when(profissionalRepository.findByIdAndOrganizacao_Id(1L, ORG)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.cadastrarProntuarioByOrg(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Profissional nao encontrado");
        }

        @Test
        @DisplayName("Deve lançar exceção quando consulta não encontrada")
        void deveLancarExcecaoQuandoConsultaNaoEncontrada() {
            CadastrarProntuarioDentistaRequest request = criarRequest();
            when(profissionalRepository.findByIdAndOrganizacao_Id(1L, ORG)).thenReturn(Optional.of(criarProfissional(1L)));
            when(consultaRepository.buscarComLock(10L, ORG)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.cadastrarProntuarioByOrg(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Consulta nao encontrada");
        }

        @Test
        @DisplayName("Deve atualizar status da consulta para REALIZADA após cadastro")
        void deveAtualizarStatusConsulta() {
            CadastrarProntuarioDentistaRequest request = criarRequest();
            Consulta consulta = criarConsulta(10L);
            ProntuarioDentista prontuario = criarProntuario(100L);

            when(profissionalRepository.findByIdAndOrganizacao_Id(1L, ORG)).thenReturn(Optional.of(criarProfissional(1L)));
            when(consultaRepository.buscarComLock(10L, ORG)).thenReturn(Optional.of(consulta));
            when(prontuarioDentistaRepository.save(any())).thenReturn(prontuario);
            when(consultaRepository.save(any())).thenReturn(consulta);

            service.cadastrarProntuarioByOrg(request);

            assertThat(consulta.getStatus()).isEqualTo(StatusConsulta.REALIZADA);
        }

        @Test
        @DisplayName("Deve cadastrar com dentes não-sadiós no odontograma")
        void deveCadastrarComDentesNaoSadios() {
            CadastrarProntuarioDentistaRequest request = criarRequest();
            request.setOdontograma(List.of(
                    criarDente(11, "cariado", "Cárie profunda"),
                    criarDente(12, "sadio", null)   // sadio sem obs — deve ser ignorado
            ));
            ProntuarioDentista prontuario = criarProntuario(100L);

            when(profissionalRepository.findByIdAndOrganizacao_Id(1L, ORG)).thenReturn(Optional.of(criarProfissional(1L)));
            when(consultaRepository.buscarComLock(10L, ORG)).thenReturn(Optional.of(criarConsulta(10L)));
            when(prontuarioDentistaRepository.save(any())).thenReturn(prontuario);
            when(consultaRepository.save(any())).thenReturn(criarConsulta(10L));

            ProntuarioDentista resultado = service.cadastrarProntuarioByOrg(request);

            assertThat(resultado).isNotNull();
            verify(prontuarioDentistaRepository).save(any(ProntuarioDentista.class));
        }

        @Test
        @DisplayName("Deve cadastrar com planejamentos quando pacienteId informado")
        void deveCadastrarComPlanejamentosComPaciente() {
            CadastrarProntuarioDentistaRequest request = criarRequest();
            request.setPlanejamentos(List.of(criarItemPlanejamento(5L)));

            ProntuarioDentista prontuario = criarProntuario(100L);
            Paciente paciente = new Paciente();
            paciente.setPaciCodigo(5L);

            when(profissionalRepository.findByIdAndOrganizacao_Id(1L, ORG)).thenReturn(Optional.of(criarProfissional(1L)));
            when(consultaRepository.buscarComLock(10L, ORG)).thenReturn(Optional.of(criarConsulta(10L)));
            when(prontuarioDentistaRepository.save(any())).thenReturn(prontuario);
            when(pacienteRepository.findByPaciCodigoAndOrganizacao_IdWithEndereco(5L, ORG)).thenReturn(Optional.of(paciente));
            when(consultaRepository.save(any())).thenReturn(criarConsulta(10L));

            ProntuarioDentista resultado = service.cadastrarProntuarioByOrg(request);

            assertThat(resultado).isNotNull();
            verify(pacienteRepository).findByPaciCodigoAndOrganizacao_IdWithEndereco(5L, ORG);
            verify(prontuarioDentistaRepository, times(2)).save(any());
        }

        @Test
        @DisplayName("Deve cadastrar com planejamentos quando pacienteId é nulo")
        void deveCadastrarComPlanejamentosSemPaciente() {
            CadastrarProntuarioDentistaRequest request = criarRequest();
            request.setPlanejamentos(List.of(criarItemPlanejamento(null)));

            ProntuarioDentista prontuario = criarProntuario(100L);

            when(profissionalRepository.findByIdAndOrganizacao_Id(1L, ORG)).thenReturn(Optional.of(criarProfissional(1L)));
            when(consultaRepository.buscarComLock(10L, ORG)).thenReturn(Optional.of(criarConsulta(10L)));
            when(prontuarioDentistaRepository.save(any())).thenReturn(prontuario);
            when(consultaRepository.save(any())).thenReturn(criarConsulta(10L));

            ProntuarioDentista resultado = service.cadastrarProntuarioByOrg(request);

            assertThat(resultado).isNotNull();
            verify(pacienteRepository, never()).findByPaciCodigoAndOrganizacao_IdWithEndereco(any(), any());
            verify(prontuarioDentistaRepository, times(2)).save(any());
        }
    }

    // ========== buscarMaisRecentePorConsulta ==========

    @Nested
    @DisplayName("buscarMaisRecentePorConsulta")
    class BuscarMaisRecente {

        @Test
        @DisplayName("Deve retornar prontuário mais recente da consulta")
        void deveRetornarMaisRecente() {
            ProntuarioDentista prontuario = criarProntuario(5L);
            when(prontuarioDentistaRepository.findIdMaisRecentePorConsulta(10L, ORG))
                    .thenReturn(Optional.of(5L));
            when(prontuarioDentistaRepository.findByIdComDentes(5L))
                    .thenReturn(Optional.of(prontuario));

            ProntuarioDentista resultado = service.buscarMaisRecentePorConsulta(10L);

            assertThat(resultado).isNotNull();
            assertThat(resultado.getCodigo()).isEqualTo(5L);
        }

        @Test
        @DisplayName("Deve lançar exceção quando não há prontuário recente")
        void deveLancarExcecaoQuandoNaoEncontrado() {
            when(prontuarioDentistaRepository.findIdMaisRecentePorConsulta(99L, ORG))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.buscarMaisRecentePorConsulta(99L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Nenhum Prontuario odontologico encontrado");
        }
    }

    // ========== atualizarProntuario ==========

    @Nested
    @DisplayName("atualizarProntuario")
    class AtualizarProntuario {

        @Test
        @DisplayName("Deve lançar exceção quando prontuário não encontrado ao atualizar")
        void deveLancarExcecaoQuandoNaoEncontrado() {
            when(prontuarioDentistaRepository.findByCodigoAndConsulta_Organizacao_Id(99L, ORG)).thenReturn(Optional.empty());

            var requisicao = criarRequest();
            assertThatThrownBy(() -> service.atualizarProntuario(99L, requisicao))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Prontuario odontologico nao encontrado");
        }

        @Test
        @DisplayName("Deve atualizar campos clínicos e limpar odontograma/planejamentos")
        void deveAtualizarSemOdontogramaEPlanejamentos() {
            ProntuarioDentista prontuario = criarProntuario(1L);
            CadastrarProntuarioDentistaRequest request = criarRequest();

            when(prontuarioDentistaRepository.findByCodigoAndConsulta_Organizacao_Id(1L, ORG)).thenReturn(Optional.of(prontuario));
            when(prontuarioDentistaRepository.saveAndFlush(any())).thenReturn(prontuario);
            when(prontuarioDentistaRepository.save(any())).thenReturn(prontuario);

            service.atualizarProntuario(1L, request);

            verify(prontuarioDentistaRepository).saveAndFlush(prontuario);
            verify(prontuarioDentistaRepository).save(prontuario);
            assertThat(prontuario.getQueixaPrincipal()).isEqualTo("Dor de dente");
            assertThat(prontuario.getDiagnostico()).isEqualTo("Cárie");
        }

        @Test
        @DisplayName("Deve atualizar com dentes não-sadiós no odontograma")
        void deveAtualizarComDentesNaoSadios() {
            ProntuarioDentista prontuario = criarProntuario(1L);
            CadastrarProntuarioDentistaRequest request = criarRequest();
            request.setOdontograma(List.of(
                    criarDente(21, "obturado", "Restauração anterior"),
                    criarDente(22, "sadio", "")    // sadio sem obs — deve ser ignorado
            ));

            when(prontuarioDentistaRepository.findByCodigoAndConsulta_Organizacao_Id(1L, ORG)).thenReturn(Optional.of(prontuario));
            when(prontuarioDentistaRepository.saveAndFlush(any())).thenReturn(prontuario);
            when(prontuarioDentistaRepository.save(any())).thenReturn(prontuario);

            service.atualizarProntuario(1L, request);

            assertThat(prontuario.getDentes()).hasSize(1);
            assertThat(prontuario.getDentes().iterator().next().getStatus()).isEqualTo("obturado");
        }

        @Test
        @DisplayName("Item novo usa o paciente da consulta; pacienteId do payload é ignorado")
        void itemNovoUsaPacienteDaConsulta() {
            ProntuarioDentista prontuario = criarProntuario(1L);
            Paciente paciente = new Paciente();
            paciente.setPaciCodigo(5L);
            prontuario.getConsulta().setPaciente(paciente);
            CadastrarProntuarioDentistaRequest request = criarRequest();
            request.setPlanejamentos(List.of(criarItemPlanejamento(99L)));
            when(prontuarioDentistaRepository.findByCodigoAndConsulta_Organizacao_Id(1L, ORG)).thenReturn(Optional.of(prontuario));

            service.atualizarProntuario(1L, request);

            assertThat(prontuario.getPlanejamentos()).singleElement().satisfies(p -> {
                assertThat(p.getProcedimentoRealizado()).isEqualTo("Exodontia");
                assertThat(p.getPaciente()).isSameAs(paciente);
            });
            verifyNoInteractions(pacienteRepository);
        }

        @Test
        @DisplayName("Deve limpar planejamentos antes de recriar")
        void deveLimparPlanejamentosAntesDeRecriar() {
            ProntuarioDentista prontuario = criarProntuario(1L);

            when(prontuarioDentistaRepository.findByCodigoAndConsulta_Organizacao_Id(1L, ORG)).thenReturn(Optional.of(prontuario));
            when(prontuarioDentistaRepository.saveAndFlush(any())).thenReturn(prontuario);
            when(prontuarioDentistaRepository.save(any())).thenReturn(prontuario);

            service.atualizarProntuario(1L, criarRequest());

            assertThat(prontuario.getPlanejamentos()).isEmpty();
            assertThat(prontuario.getDentes()).isEmpty();
        }

        private PlanejamentoTerapeutico assinado() {
            PlanejamentoTerapeutico p = PlanejamentoTerapeutico.builder()
                    .id(10L).procedimentoRealizado("Exodontia").valor(new BigDecimal("180.00"))
                    .dataProcedimento(LocalDate.of(2026, 11, 1)).statusAssinatura("ASSINADO").build();
            p.setAssinaturaBase64("data:image/png;base64,AAAA");
            p.setDataAssinatura(LocalDateTime.of(2026, 11, 2, 10, 0));
            p.setIpOrigem("10.0.0.1");
            return p;
        }

        @Test
        @DisplayName("Outro profissional, secretária ou gestor da mesma clínica recebem 404 e nada é gravado")
        void naoDonoRecebe404() {
            ProntuarioDentista prontuario = criarProntuario(1L);
            when(prontuarioDentistaRepository.findByCodigoAndConsulta_Organizacao_Id(1L, ORG)).thenReturn(Optional.of(prontuario));
            for (Long outroUsuario : List.of(501L, 502L, 503L)) {
                TenantContext.setCurrentUser(outroUsuario);
                var requisicao = criarRequest();
                assertThatThrownBy(() -> service.atualizarProntuario(1L, requisicao))
                        .isInstanceOf(ResourceNotFoundException.class);
            }
            verify(prontuarioDentistaRepository, never()).saveAndFlush(any());
            verify(prontuarioDentistaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Reenviar o item assinado preserva assinatura, data, IP e status")
        void preservaAssinado() {
            ProntuarioDentista prontuario = criarProntuario(1L);
            PlanejamentoTerapeutico assinado = assinado();
            prontuario.getPlanejamentos().add(assinado);
            when(prontuarioDentistaRepository.findByCodigoAndConsulta_Organizacao_Id(1L, ORG)).thenReturn(Optional.of(prontuario));
            CadastrarProntuarioDentistaRequest request = criarRequest();
            CadastrarProntuarioDentistaRequest.PlanejamentoItem mesmo = criarItemPlanejamento(null);
            mesmo.setId(10L);
            request.setPlanejamentos(List.of(mesmo, criarItemPlanejamento(null)));

            service.atualizarProntuario(1L, request);

            assertThat(prontuario.getPlanejamentos()).hasSize(2).contains(assinado);
            assertThat(assinado.getStatusAssinatura()).isEqualTo("ASSINADO");
            assertThat(assinado.getAssinaturaBase64()).isEqualTo("data:image/png;base64,AAAA");
            assertThat(assinado.getDataAssinatura()).isEqualTo(LocalDateTime.of(2026, 11, 2, 10, 0));
            assertThat(assinado.getIpOrigem()).isEqualTo("10.0.0.1");
        }

        @Test
        @DisplayName("Mudar o valor do item assinado responde 409 PLANEJAMENTO_ASSINADO")
        void alterarAssinadoRecusado() {
            ProntuarioDentista prontuario = criarProntuario(1L);
            prontuario.getPlanejamentos().add(assinado());
            when(prontuarioDentistaRepository.findByCodigoAndConsulta_Organizacao_Id(1L, ORG)).thenReturn(Optional.of(prontuario));
            CadastrarProntuarioDentistaRequest request = criarRequest();
            CadastrarProntuarioDentistaRequest.PlanejamentoItem alterado = criarItemPlanejamento(null);
            alterado.setId(10L);
            alterado.setValor(new BigDecimal("1.00"));
            request.setPlanejamentos(List.of(alterado));

            assertThatThrownBy(() -> service.atualizarProntuario(1L, request))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo("PLANEJAMENTO_ASSINADO"));
            verify(prontuarioDentistaRepository, never()).save(any());
        }
    }

    // ========== Isolamento multi-tenant ==========

    @Nested
    @DisplayName("Isolamento entre clínicas")
    class IsolamentoEntreClinicas {

        @Test
        @DisplayName("buscarMaisRecentePorConsulta filtra pela organização do token")
        void maisRecenteFiltrado() {
            when(prontuarioDentistaRepository.findIdMaisRecentePorConsulta(10L, ORG)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.buscarMaisRecentePorConsulta(10L))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(prontuarioDentistaRepository, never()).findByIdComDentes(any());
        }

        @Test
        @DisplayName("cadastrar recusa consulta de outra clínica e não grava nada")
        void cadastrarConsultaOutraClinica() {
            when(profissionalRepository.findByIdAndOrganizacao_Id(1L, ORG)).thenReturn(Optional.of(criarProfissional(1L)));
            lenient().when(consultaRepository.findById(10L)).thenReturn(Optional.of(criarConsulta(10L)));

            var requisicao = criarRequest();
            assertThatThrownBy(() -> service.cadastrarProntuarioByOrg(requisicao))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Consulta nao encontrada");
            verify(prontuarioDentistaRepository, never()).save(any());
        }

        @Test
        @DisplayName("atualizar recusa prontuário de outra clínica e não grava nada")
        void atualizarOutraClinica() {
            lenient().when(prontuarioDentistaRepository.findById(1L)).thenReturn(Optional.of(criarProntuario(1L)));

            var requisicao = criarRequest();
            assertThatThrownBy(() -> service.atualizarProntuario(1L, requisicao))
                    .isInstanceOf(ResourceNotFoundException.class);
            verify(prontuarioDentistaRepository, never()).saveAndFlush(any());
            verify(prontuarioDentistaRepository, never()).save(any());
        }
    }
}
