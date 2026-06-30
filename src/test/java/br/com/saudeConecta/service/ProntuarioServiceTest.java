package br.com.saudeConecta.service;

import br.com.saudeConecta.domain.consulta.Consulta;
import br.com.saudeConecta.domain.consulta.StatusConsulta;
import br.com.saudeConecta.domain.paciente.Paciente;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.domain.prontuario.Prontuario;
import br.com.saudeConecta.infra.tenant.TenantContext;
import br.com.saudeConecta.infrastructure.persistence.repository.ConsultaRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.PacienteRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.ProfissionalRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.ProntuarioRepository;
import br.com.saudeConecta.presentation.dto.prontuario.CadastrarProntuarioRequest;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
@DisplayName("ProntuarioService - Testes unitários")
class ProntuarioServiceTest {

    @AfterEach
    void limparTenantContext() {
        TenantContext.clear();
    }

    @Mock private ProntuarioRepository prontuarioRepository;
    @Mock private ProfissionalRepository profissionalRepository;
    @Mock private ConsultaRepository consultaRepository;
    @Mock private PacienteRepository pacienteRepository;
    @Mock private PlanejamentoTerapeuticoService planejamentoService;

    @InjectMocks
    private ProntuarioService service;

    // ========== Helpers ==========

    private Profissional criarProfissional(Long id) {
        Profissional p = new Profissional();
        p.setId(id);
        p.setNome("Dr. Silva");
        return p;
    }

    private Consulta criarConsulta(Long id) {
        Consulta c = new Consulta();
        c.setId(id);
        c.setDataHora(LocalDateTime.now());
        c.setStatus(StatusConsulta.AGENDADA);
        return c;
    }

    private Prontuario criarProntuario(Long id) {
        Prontuario p = new Prontuario();
        p.setProntCodigoProntuario(id);
        p.setConsulta(criarConsulta(10L));
        p.setProfissional(criarProfissional(1L));
        return p;
    }

    private CadastrarProntuarioRequest criarRequest() {
        CadastrarProntuarioRequest req = new CadastrarProntuarioRequest();
        req.setConsulta(10L);
        req.setCodigoMedico(1L);
        req.setQueixaPrincipal("Dor de cabeça");
        req.setAnamnese("Paciente relata...");
        req.setDiagnostico("Enxaqueca");
        req.setPlanejamentos(Collections.emptyList());
        return req;
    }

    private CadastrarProntuarioRequest.PlanejamentoItem criarItemComPaciente(Long pacienteId) {
        CadastrarProntuarioRequest.PlanejamentoItem item = new CadastrarProntuarioRequest.PlanejamentoItem();
        item.setProcedimentoRealizado("Consulta de rotina");
        item.setValor(BigDecimal.valueOf(250.00));
        item.setDataProcedimento("2024-06-01");
        item.setPacienteId(pacienteId);
        return item;
    }

    // ========== cadastrarProntuarioMedico ==========

    @Nested
    @DisplayName("cadastrarProntuarioMedico")
    class CadastrarProntuarioMedico {

        @Test
        @DisplayName("Deve cadastrar prontuário médico com sucesso")
        void deveCadastrarComSucesso() {
            CadastrarProntuarioRequest request = criarRequest();
            Profissional profissional = criarProfissional(1L);
            Consulta consulta = criarConsulta(10L);
            Prontuario prontuario = criarProntuario(100L);

            when(profissionalRepository.findById(1L)).thenReturn(Optional.of(profissional));
            when(consultaRepository.findById(10L)).thenReturn(Optional.of(consulta));
            when(prontuarioRepository.save(any())).thenReturn(prontuario);
            when(consultaRepository.save(any())).thenReturn(consulta);

            Prontuario resultado = service.cadastrarProntuarioMedico(request);

            assertThat(resultado).isNotNull();
            assertThat(resultado.getProntCodigoProntuario()).isEqualTo(100L);
            verify(prontuarioRepository).save(any(Prontuario.class));
        }

        @Test
        @DisplayName("Deve lançar exceção quando profissional não encontrado")
        void deveLancarExcecaoQuandoProfissionalNaoEncontrado() {
            CadastrarProntuarioRequest request = criarRequest();
            when(profissionalRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.cadastrarProntuarioMedico(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Profissional nao encontrado");
        }

        @Test
        @DisplayName("Deve lançar exceção quando consulta não encontrada")
        void deveLancarExcecaoQuandoConsultaNaoEncontrada() {
            CadastrarProntuarioRequest request = criarRequest();
            when(profissionalRepository.findById(1L)).thenReturn(Optional.of(criarProfissional(1L)));
            when(consultaRepository.findById(10L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.cadastrarProntuarioMedico(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Consulta nao encontrada");
        }

        @Test
        @DisplayName("Deve atualizar status da consulta para REALIZADA após cadastro")
        void deveAtualizarStatusConsultaParaRealizada() {
            CadastrarProntuarioRequest request = criarRequest();
            Consulta consulta = criarConsulta(10L);
            Prontuario prontuario = criarProntuario(100L);

            when(profissionalRepository.findById(1L)).thenReturn(Optional.of(criarProfissional(1L)));
            when(consultaRepository.findById(10L)).thenReturn(Optional.of(consulta));
            when(prontuarioRepository.save(any())).thenReturn(prontuario);
            when(consultaRepository.save(any())).thenReturn(consulta);

            service.cadastrarProntuarioMedico(request);

            assertThat(consulta.getStatus()).isEqualTo(StatusConsulta.REALIZADA);
        }

        @Test
        @DisplayName("Deve cadastrar prontuário com planejamentos quando pacienteId informado")
        void deveCadastrarComPlanejamentosComPaciente() {
            TenantContext.setCurrentTenant(1L);

            CadastrarProntuarioRequest request = criarRequest();
            request.setPlanejamentos(List.of(criarItemComPaciente(5L)));

            Profissional profissional = criarProfissional(1L);
            Consulta consulta = criarConsulta(10L);
            Prontuario prontuarioSalvo = criarProntuario(100L);
            Paciente paciente = new Paciente();
            paciente.setPaciCodigo(5L);

            when(profissionalRepository.findById(1L)).thenReturn(Optional.of(profissional));
            when(consultaRepository.findById(10L)).thenReturn(Optional.of(consulta));
            when(prontuarioRepository.save(any())).thenReturn(prontuarioSalvo);
            when(pacienteRepository.findById(5L)).thenReturn(Optional.of(paciente));
            when(consultaRepository.save(any())).thenReturn(consulta);

            Prontuario resultado = service.cadastrarProntuarioMedico(request);

            assertThat(resultado).isNotNull();
            verify(pacienteRepository).findById(5L);
            verify(prontuarioRepository, times(2)).save(any());
        }

        @Test
        @DisplayName("Deve cadastrar prontuário com planejamentos quando pacienteId é nulo")
        void deveCadastrarComPlanejamentosSemPaciente() {
            TenantContext.setCurrentTenant(1L);

            CadastrarProntuarioRequest request = criarRequest();
            request.setPlanejamentos(List.of(criarItemComPaciente(null)));

            Profissional profissional = criarProfissional(1L);
            Consulta consulta = criarConsulta(10L);
            Prontuario prontuarioSalvo = criarProntuario(100L);

            when(profissionalRepository.findById(1L)).thenReturn(Optional.of(profissional));
            when(consultaRepository.findById(10L)).thenReturn(Optional.of(consulta));
            when(prontuarioRepository.save(any())).thenReturn(prontuarioSalvo);
            when(consultaRepository.save(any())).thenReturn(consulta);

            Prontuario resultado = service.cadastrarProntuarioMedico(request);

            assertThat(resultado).isNotNull();
            verify(pacienteRepository, never()).findById(any());
            verify(prontuarioRepository, times(2)).save(any());
        }
    }

    // ========== buscarPorId ==========

    @Nested
    @DisplayName("buscarPorId")
    class BuscarPorId {

        @Test
        @DisplayName("Deve retornar prontuário quando encontrado")
        void deveRetornarProntuario() {
            Prontuario prontuario = criarProntuario(1L);
            when(prontuarioRepository.findById(1L)).thenReturn(Optional.of(prontuario));

            Prontuario resultado = service.buscarPorId(1L);

            assertThat(resultado).isNotNull();
            assertThat(resultado.getProntCodigoProntuario()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Deve lançar exceção quando prontuário não encontrado")
        void deveLancarExcecaoQuandoNaoEncontrado() {
            when(prontuarioRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.buscarPorId(99L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Prontuario médico nao encontrado");
        }
    }

    // ========== buscarProntuarioById ==========

    @Nested
    @DisplayName("buscarProntuarioById")
    class BuscarProntuarioById {

        @Test
        @DisplayName("Deve retornar prontuário por ID da consulta")
        void deveRetornarPorConsultaId() {
            Prontuario prontuario = criarProntuario(1L);
            when(prontuarioRepository.findByConsulta_IdWithFetch(10L)).thenReturn(prontuario);

            Prontuario resultado = service.buscarProntuarioById(10L);

            assertThat(resultado).isNotNull();
        }

        @Test
        @DisplayName("Deve lançar EntityNotFoundException quando prontuário não encontrado por consulta")
        void deveLancarExcecaoQuandoNaoEncontrado() {
            when(prontuarioRepository.findByConsulta_IdWithFetch(99L)).thenReturn(null);

            assertThatThrownBy(() -> service.buscarProntuarioById(99L))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    // ========== buscarPorPaciente / buscarPorProfissional ==========

    @Nested
    @DisplayName("buscarPorPaciente e buscarPorProfissional")
    class BuscarPorPacienteProfissional {

        @Test
        @DisplayName("Deve retornar prontuários do paciente")
        void deveRetornarPorPaciente() {
            when(prontuarioRepository.findByConsulta_Paciente_PaciCodigo(1L))
                    .thenReturn(List.of(criarProntuario(1L), criarProntuario(2L)));

            List<Prontuario> resultado = service.buscarPorPaciente(1L);

            assertThat(resultado).hasSize(2);
        }

        @Test
        @DisplayName("Deve retornar lista vazia quando paciente sem prontuários")
        void deveRetornarListaVaziaParaPacienteSemProntuario() {
            when(prontuarioRepository.findByConsulta_Paciente_PaciCodigo(99L))
                    .thenReturn(Collections.emptyList());

            List<Prontuario> resultado = service.buscarPorPaciente(99L);

            assertThat(resultado).isEmpty();
        }

        @Test
        @DisplayName("Deve retornar prontuários do profissional")
        void deveRetornarPorProfissional() {
            when(prontuarioRepository.findByProfissional_Id(1L))
                    .thenReturn(List.of(criarProntuario(1L)));

            List<Prontuario> resultado = service.buscarPorProfissional(1L);

            assertThat(resultado).hasSize(1);
        }
    }

    // ========== buscarMaisRecentePorConsulta ==========

    @Nested
    @DisplayName("buscarMaisRecentePorConsulta")
    class BuscarMaisRecente {

        @Test
        @DisplayName("Deve retornar o prontuário mais recente da consulta")
        void deveRetornarMaisRecente() {
            Prontuario prontuario = criarProntuario(1L);
            when(prontuarioRepository.findIdMaisRecentePorConsulta(10L)).thenReturn(Optional.of(1L));
            when(prontuarioRepository.findByIdWithFullFetch(1L)).thenReturn(Optional.of(prontuario));

            Prontuario resultado = service.buscarMaisRecentePorConsulta(10L);

            assertThat(resultado).isNotNull();
        }

        @Test
        @DisplayName("Deve lançar EntityNotFoundException quando não há prontuário recente")
        void deveLancarExcecaoQuandoNaoEncontrado() {
            when(prontuarioRepository.findIdMaisRecentePorConsulta(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.buscarMaisRecentePorConsulta(99L))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    // ========== atualizarProntuarioMedico ==========

    @Nested
    @DisplayName("atualizarProntuarioMedico")
    class AtualizarProntuario {

        @Test
        @DisplayName("Deve lançar exceção quando prontuário não encontrado ao atualizar")
        void deveLancarExcecaoQuandoNaoEncontrado() {
            when(prontuarioRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.atualizarProntuarioMedico(99L, criarRequest()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Prontuário não encontrado");
        }

        @Test
        @DisplayName("Deve atualizar prontuário sem planejamentos — apenas campos clínicos")
        void deveAtualizarSemPlanejamentos() {
            Prontuario prontuario = criarProntuario(1L);
            CadastrarProntuarioRequest request = criarRequest();

            when(prontuarioRepository.findById(1L)).thenReturn(Optional.of(prontuario));
            when(prontuarioRepository.saveAndFlush(any())).thenReturn(prontuario);
            when(prontuarioRepository.save(any())).thenReturn(prontuario);

            service.atualizarProntuarioMedico(1L, request);

            verify(prontuarioRepository).saveAndFlush(prontuario);
            verify(prontuarioRepository).save(prontuario);
            assertThat(prontuario.getProntAnamnese()).isEqualTo("Paciente relata...");
            assertThat(prontuario.getProntDiagnostico()).isEqualTo("Enxaqueca");
        }

        @Test
        @DisplayName("Deve atualizar prontuário com planejamentos quando pacienteId informado")
        void deveAtualizarComPlanejamentosComPaciente() {
            TenantContext.setCurrentTenant(1L);

            Prontuario prontuario = criarProntuario(1L);
            CadastrarProntuarioRequest request = criarRequest();
            request.setPlanejamentos(List.of(criarItemComPaciente(5L)));

            Paciente paciente = new Paciente();
            paciente.setPaciCodigo(5L);

            when(prontuarioRepository.findById(1L)).thenReturn(Optional.of(prontuario));
            when(prontuarioRepository.saveAndFlush(any())).thenReturn(prontuario);
            when(prontuarioRepository.save(any())).thenReturn(prontuario);
            when(pacienteRepository.findById(5L)).thenReturn(Optional.of(paciente));

            service.atualizarProntuarioMedico(1L, request);

            verify(pacienteRepository).findById(5L);
            verify(prontuarioRepository).save(prontuario);
            assertThat(prontuario.getPlanejamentos()).hasSize(1);
            assertThat(prontuario.getPlanejamentos().get(0).getProcedimentoRealizado())
                    .isEqualTo("Consulta de rotina");
        }

        @Test
        @DisplayName("Deve atualizar prontuário com planejamentos quando pacienteId é nulo")
        void deveAtualizarComPlanejamentosSemPaciente() {
            TenantContext.setCurrentTenant(1L);

            Prontuario prontuario = criarProntuario(1L);
            CadastrarProntuarioRequest request = criarRequest();
            request.setPlanejamentos(List.of(criarItemComPaciente(null)));

            when(prontuarioRepository.findById(1L)).thenReturn(Optional.of(prontuario));
            when(prontuarioRepository.saveAndFlush(any())).thenReturn(prontuario);
            when(prontuarioRepository.save(any())).thenReturn(prontuario);

            service.atualizarProntuarioMedico(1L, request);

            verify(pacienteRepository, never()).findById(any());
            assertThat(prontuario.getPlanejamentos()).hasSize(1);
            assertThat(prontuario.getPlanejamentos().get(0).getPaciente()).isNull();
        }

        @Test
        @DisplayName("Deve limpar planejamentos antigos antes de recriar")
        void deveLimparPlanejamentosAntesDeRecriar() {
            TenantContext.setCurrentTenant(1L);

            Prontuario prontuario = criarProntuario(1L);
            CadastrarProntuarioRequest requestSemPlanejamento = criarRequest();

            when(prontuarioRepository.findById(1L)).thenReturn(Optional.of(prontuario));
            when(prontuarioRepository.saveAndFlush(any())).thenReturn(prontuario);
            when(prontuarioRepository.save(any())).thenReturn(prontuario);

            service.atualizarProntuarioMedico(1L, requestSemPlanejamento);

            assertThat(prontuario.getPlanejamentos()).isEmpty();
            verify(prontuarioRepository).saveAndFlush(prontuario);
        }
    }
}
