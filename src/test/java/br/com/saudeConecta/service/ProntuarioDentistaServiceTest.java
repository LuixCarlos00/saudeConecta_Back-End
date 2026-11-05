package br.com.saudeConecta.service;

import br.com.saudeConecta.domain.consulta.Consulta;
import br.com.saudeConecta.domain.consulta.StatusConsulta;
import br.com.saudeConecta.domain.paciente.Paciente;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.domain.prontuario.ProntuarioDentista;
import br.com.saudeConecta.infra.tenant.TenantContext;
import br.com.saudeConecta.infra.tenant.TenantHelper;
import br.com.saudeConecta.infrastructure.persistence.repository.AtendimentoRascunhoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.ConsultaRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.PacienteRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.ProfissionalRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.ProntuarioDentistaRepository;
import br.com.saudeConecta.presentation.dto.prontuario.CadastrarProntuarioDentistaRequest;
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

    @BeforeEach
    void definirTenant() {
        TenantContext.setCurrentTenant(ORG);
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

            assertThatThrownBy(() -> service.cadastrarProntuarioByOrg(criarRequest()))
                    .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);

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

    // ========== buscarPorId ==========

    @Nested
    @DisplayName("buscarPorId")
    class BuscarPorId {

        @Test
        @DisplayName("Deve retornar prontuário quando encontrado")
        void deveRetornarProntuario() {
            ProntuarioDentista prontuario = criarProntuario(1L);
            when(prontuarioDentistaRepository.findByCodigoAndConsulta_Organizacao_Id(1L, ORG)).thenReturn(Optional.of(prontuario));

            ProntuarioDentista resultado = service.buscarPorId(1L);

            assertThat(resultado).isNotNull();
            assertThat(resultado.getCodigo()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Deve lançar exceção quando prontuário não encontrado")
        void deveLancarExcecaoQuandoNaoEncontrado() {
            when(prontuarioDentistaRepository.findByCodigoAndConsulta_Organizacao_Id(99L, ORG)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.buscarPorId(99L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Prontuario odontologico nao encontrado");
        }
    }

    // ========== listarPorConsulta ==========

    @Nested
    @DisplayName("listarPorConsulta")
    class ListarPorConsulta {

        @Test
        @DisplayName("Deve retornar lista de prontuários da consulta")
        void deveRetornarListaPorConsulta() {
            when(prontuarioDentistaRepository.findByConsultaIdAndOrganizacaoId(10L, ORG))
                    .thenReturn(List.of(criarProntuario(1L), criarProntuario(2L)));

            List<ProntuarioDentista> resultado = service.listarPorConsulta(10L);

            assertThat(resultado).hasSize(2);
        }

        @Test
        @DisplayName("Deve retornar lista vazia quando não há prontuários")
        void deveRetornarVazioQuandoSemProntuarios() {
            when(prontuarioDentistaRepository.findByConsultaIdAndOrganizacaoId(99L, ORG))
                    .thenReturn(Collections.emptyList());

            List<ProntuarioDentista> resultado = service.listarPorConsulta(99L);

            assertThat(resultado).isEmpty();
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

    // ========== listarPorProfissional / listarPorPaciente ==========

    @Nested
    @DisplayName("listarPorProfissional e listarPorPaciente")
    class ListarPorAssociacao {

        @Test
        @DisplayName("Deve retornar prontuários do profissional")
        void deveRetornarPorProfissional() {
            when(prontuarioDentistaRepository.findByProfissionalId(1L, ORG))
                    .thenReturn(List.of(criarProntuario(1L)));

            List<ProntuarioDentista> resultado = service.listarPorProfissional(1L);

            assertThat(resultado).hasSize(1);
        }

        @Test
        @DisplayName("Deve retornar prontuários do paciente")
        void deveRetornarPorPaciente() {
            when(prontuarioDentistaRepository.findByPacienteId(1L, ORG))
                    .thenReturn(List.of(criarProntuario(1L), criarProntuario(2L)));

            List<ProntuarioDentista> resultado = service.listarPorPaciente(1L);

            assertThat(resultado).hasSize(2);
        }

        @Test
        @DisplayName("Deve retornar lista vazia para paciente sem prontuários")
        void deveRetornarVazioParaPacienteSemProntuarios() {
            when(prontuarioDentistaRepository.findByPacienteId(99L, ORG))
                    .thenReturn(Collections.emptyList());

            List<ProntuarioDentista> resultado = service.listarPorPaciente(99L);

            assertThat(resultado).isEmpty();
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

            assertThatThrownBy(() -> service.atualizarProntuario(99L, criarRequest()))
                    .isInstanceOf(IllegalArgumentException.class)
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

            verify(prontuarioDentistaRepository, times(2)).saveAndFlush(prontuario);
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
        @DisplayName("Deve atualizar com planejamentos quando pacienteId informado")
        void deveAtualizarComPlanejamentosComPaciente() {
            ProntuarioDentista prontuario = criarProntuario(1L);
            CadastrarProntuarioDentistaRequest request = criarRequest();
            request.setPlanejamentos(List.of(criarItemPlanejamento(5L)));

            Paciente paciente = new Paciente();
            paciente.setPaciCodigo(5L);

            when(prontuarioDentistaRepository.findByCodigoAndConsulta_Organizacao_Id(1L, ORG)).thenReturn(Optional.of(prontuario));
            when(prontuarioDentistaRepository.saveAndFlush(any())).thenReturn(prontuario);
            when(prontuarioDentistaRepository.save(any())).thenReturn(prontuario);
            when(pacienteRepository.findByPaciCodigoAndOrganizacao_IdWithEndereco(5L, ORG)).thenReturn(Optional.of(paciente));

            service.atualizarProntuario(1L, request);

            verify(pacienteRepository).findByPaciCodigoAndOrganizacao_IdWithEndereco(5L, ORG);
            assertThat(prontuario.getPlanejamentos()).hasSize(1);
            assertThat(prontuario.getPlanejamentos().get(0).getProcedimentoRealizado())
                    .isEqualTo("Exodontia");
            assertThat(prontuario.getPlanejamentos().get(0).getPaciente()).isEqualTo(paciente);
        }

        @Test
        @DisplayName("Deve atualizar com planejamentos quando pacienteId é nulo")
        void deveAtualizarComPlanejamentosSemPaciente() {
            ProntuarioDentista prontuario = criarProntuario(1L);
            CadastrarProntuarioDentistaRequest request = criarRequest();
            request.setPlanejamentos(List.of(criarItemPlanejamento(null)));

            when(prontuarioDentistaRepository.findByCodigoAndConsulta_Organizacao_Id(1L, ORG)).thenReturn(Optional.of(prontuario));
            when(prontuarioDentistaRepository.saveAndFlush(any())).thenReturn(prontuario);
            when(prontuarioDentistaRepository.save(any())).thenReturn(prontuario);

            service.atualizarProntuario(1L, request);

            verify(pacienteRepository, never()).findByPaciCodigoAndOrganizacao_IdWithEndereco(any(), any());
            assertThat(prontuario.getPlanejamentos()).hasSize(1);
            assertThat(prontuario.getPlanejamentos().get(0).getPaciente()).isNull();
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
    }

    // ========== Isolamento multi-tenant ==========

    @Nested
    @DisplayName("Isolamento entre clínicas")
    class IsolamentoEntreClinicas {

        @Test
        @DisplayName("buscarPorId não devolve prontuário odontológico de outra clínica")
        void buscarPorIdOutraClinica() {
            lenient().when(prontuarioDentistaRepository.findById(1L)).thenReturn(Optional.of(criarProntuario(1L)));

            assertThatThrownBy(() -> service.buscarPorId(1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("nao encontrado");
            verify(prontuarioDentistaRepository).findByCodigoAndConsulta_Organizacao_Id(1L, ORG);
        }

        @Test
        @DisplayName("Listagens por consulta, profissional e paciente filtram pela organização do token")
        void listasFiltradas() {
            service.listarPorConsulta(10L);
            service.listarPorProfissional(8L);
            service.listarPorPaciente(7L);

            verify(prontuarioDentistaRepository).findByConsultaIdAndOrganizacaoId(10L, ORG);
            verify(prontuarioDentistaRepository).findByProfissionalId(8L, ORG);
            verify(prontuarioDentistaRepository).findByPacienteId(7L, ORG);
            verify(prontuarioDentistaRepository, never()).findByConsultaId(any());
        }

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

            assertThatThrownBy(() -> service.cadastrarProntuarioByOrg(criarRequest()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Consulta nao encontrada");
            verify(prontuarioDentistaRepository, never()).save(any());
        }

        @Test
        @DisplayName("atualizar recusa prontuário de outra clínica e não grava nada")
        void atualizarOutraClinica() {
            lenient().when(prontuarioDentistaRepository.findById(1L)).thenReturn(Optional.of(criarProntuario(1L)));

            assertThatThrownBy(() -> service.atualizarProntuario(1L, criarRequest()))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(prontuarioDentistaRepository, never()).saveAndFlush(any());
            verify(prontuarioDentistaRepository, never()).save(any());
        }
    }
}
