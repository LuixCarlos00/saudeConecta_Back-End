package br.com.saudeConecta.service;

import br.com.saudeConecta.domain.consulta.Consulta;
import br.com.saudeConecta.domain.consulta.StatusConsulta;
import br.com.saudeConecta.domain.paciente.Paciente;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.domain.prontuario.ProntuarioDentista;
import br.com.saudeConecta.infra.tenant.TenantContext;
import br.com.saudeConecta.infrastructure.persistence.repository.ConsultaRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.PacienteRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.ProfissionalRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.ProntuarioDentistaRepository;
import br.com.saudeConecta.presentation.dto.prontuario.CadastrarProntuarioDentistaRequest;
import jakarta.persistence.EntityManager;
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
@DisplayName("ProntuarioDentistaService - Testes unitários")
class ProntuarioDentistaServiceTest {

    @AfterEach
    void limparTenantContext() {
        TenantContext.clear();
    }

    @Mock private ProntuarioDentistaRepository prontuarioDentistaRepository;
    @Mock private ProfissionalRepository profissionalRepository;
    @Mock private ConsultaRepository consultaRepository;
    @Mock private PacienteRepository pacienteRepository;
    @Mock private EntityManager entityManager;

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
        @DisplayName("Deve cadastrar prontuário odontológico com sucesso")
        void deveCadastrarComSucesso() {
            CadastrarProntuarioDentistaRequest request = criarRequest();
            ProntuarioDentista prontuario = criarProntuario(100L);

            when(profissionalRepository.findById(1L)).thenReturn(Optional.of(criarProfissional(1L)));
            when(consultaRepository.findById(10L)).thenReturn(Optional.of(criarConsulta(10L)));
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
            when(profissionalRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.cadastrarProntuarioByOrg(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Profissional nao encontrado");
        }

        @Test
        @DisplayName("Deve lançar exceção quando consulta não encontrada")
        void deveLancarExcecaoQuandoConsultaNaoEncontrada() {
            CadastrarProntuarioDentistaRequest request = criarRequest();
            when(profissionalRepository.findById(1L)).thenReturn(Optional.of(criarProfissional(1L)));
            when(consultaRepository.findById(10L)).thenReturn(Optional.empty());

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

            when(profissionalRepository.findById(1L)).thenReturn(Optional.of(criarProfissional(1L)));
            when(consultaRepository.findById(10L)).thenReturn(Optional.of(consulta));
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

            when(profissionalRepository.findById(1L)).thenReturn(Optional.of(criarProfissional(1L)));
            when(consultaRepository.findById(10L)).thenReturn(Optional.of(criarConsulta(10L)));
            when(prontuarioDentistaRepository.save(any())).thenReturn(prontuario);
            when(consultaRepository.save(any())).thenReturn(criarConsulta(10L));

            ProntuarioDentista resultado = service.cadastrarProntuarioByOrg(request);

            assertThat(resultado).isNotNull();
            verify(prontuarioDentistaRepository).save(any(ProntuarioDentista.class));
        }

        @Test
        @DisplayName("Deve cadastrar com planejamentos quando pacienteId informado")
        void deveCadastrarComPlanejamentosComPaciente() {
            TenantContext.setCurrentTenant(1L);

            CadastrarProntuarioDentistaRequest request = criarRequest();
            request.setPlanejamentos(List.of(criarItemPlanejamento(5L)));

            ProntuarioDentista prontuario = criarProntuario(100L);
            Paciente paciente = new Paciente();
            paciente.setPaciCodigo(5L);

            when(profissionalRepository.findById(1L)).thenReturn(Optional.of(criarProfissional(1L)));
            when(consultaRepository.findById(10L)).thenReturn(Optional.of(criarConsulta(10L)));
            when(prontuarioDentistaRepository.save(any())).thenReturn(prontuario);
            when(pacienteRepository.findById(5L)).thenReturn(Optional.of(paciente));
            when(consultaRepository.save(any())).thenReturn(criarConsulta(10L));

            ProntuarioDentista resultado = service.cadastrarProntuarioByOrg(request);

            assertThat(resultado).isNotNull();
            verify(pacienteRepository).findById(5L);
            verify(prontuarioDentistaRepository, times(2)).save(any());
        }

        @Test
        @DisplayName("Deve cadastrar com planejamentos quando pacienteId é nulo")
        void deveCadastrarComPlanejamentosSemPaciente() {
            TenantContext.setCurrentTenant(1L);

            CadastrarProntuarioDentistaRequest request = criarRequest();
            request.setPlanejamentos(List.of(criarItemPlanejamento(null)));

            ProntuarioDentista prontuario = criarProntuario(100L);

            when(profissionalRepository.findById(1L)).thenReturn(Optional.of(criarProfissional(1L)));
            when(consultaRepository.findById(10L)).thenReturn(Optional.of(criarConsulta(10L)));
            when(prontuarioDentistaRepository.save(any())).thenReturn(prontuario);
            when(consultaRepository.save(any())).thenReturn(criarConsulta(10L));

            ProntuarioDentista resultado = service.cadastrarProntuarioByOrg(request);

            assertThat(resultado).isNotNull();
            verify(pacienteRepository, never()).findById(any());
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
            when(prontuarioDentistaRepository.findById(1L)).thenReturn(Optional.of(prontuario));

            ProntuarioDentista resultado = service.buscarPorId(1L);

            assertThat(resultado).isNotNull();
            assertThat(resultado.getCodigo()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Deve lançar exceção quando prontuário não encontrado")
        void deveLancarExcecaoQuandoNaoEncontrado() {
            when(prontuarioDentistaRepository.findById(99L)).thenReturn(Optional.empty());

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
            when(prontuarioDentistaRepository.findByConsultaId(10L))
                    .thenReturn(List.of(criarProntuario(1L), criarProntuario(2L)));

            List<ProntuarioDentista> resultado = service.listarPorConsulta(10L);

            assertThat(resultado).hasSize(2);
        }

        @Test
        @DisplayName("Deve retornar lista vazia quando não há prontuários")
        void deveRetornarVazioQuandoSemProntuarios() {
            when(prontuarioDentistaRepository.findByConsultaId(99L))
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
            when(prontuarioDentistaRepository.findIdMaisRecentePorConsulta(10L))
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
            when(prontuarioDentistaRepository.findIdMaisRecentePorConsulta(99L))
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
            when(prontuarioDentistaRepository.findByProfissionalId(1L))
                    .thenReturn(List.of(criarProntuario(1L)));

            List<ProntuarioDentista> resultado = service.listarPorProfissional(1L);

            assertThat(resultado).hasSize(1);
        }

        @Test
        @DisplayName("Deve retornar prontuários do paciente")
        void deveRetornarPorPaciente() {
            when(prontuarioDentistaRepository.findByPacienteId(1L))
                    .thenReturn(List.of(criarProntuario(1L), criarProntuario(2L)));

            List<ProntuarioDentista> resultado = service.listarPorPaciente(1L);

            assertThat(resultado).hasSize(2);
        }

        @Test
        @DisplayName("Deve retornar lista vazia para paciente sem prontuários")
        void deveRetornarVazioParaPacienteSemProntuarios() {
            when(prontuarioDentistaRepository.findByPacienteId(99L))
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
            when(prontuarioDentistaRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.atualizarProntuario(99L, criarRequest()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Prontuario odontologico nao encontrado");
        }

        @Test
        @DisplayName("Deve atualizar campos clínicos e limpar odontograma/planejamentos")
        void deveAtualizarSemOdontogramaEPlanejamentos() {
            ProntuarioDentista prontuario = criarProntuario(1L);
            CadastrarProntuarioDentistaRequest request = criarRequest();

            when(prontuarioDentistaRepository.findById(1L)).thenReturn(Optional.of(prontuario));
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

            when(prontuarioDentistaRepository.findById(1L)).thenReturn(Optional.of(prontuario));
            when(prontuarioDentistaRepository.saveAndFlush(any())).thenReturn(prontuario);
            when(prontuarioDentistaRepository.save(any())).thenReturn(prontuario);

            service.atualizarProntuario(1L, request);

            assertThat(prontuario.getDentes()).hasSize(1);
            assertThat(prontuario.getDentes().iterator().next().getStatus()).isEqualTo("obturado");
        }

        @Test
        @DisplayName("Deve atualizar com planejamentos quando pacienteId informado")
        void deveAtualizarComPlanejamentosComPaciente() {
            TenantContext.setCurrentTenant(1L);

            ProntuarioDentista prontuario = criarProntuario(1L);
            CadastrarProntuarioDentistaRequest request = criarRequest();
            request.setPlanejamentos(List.of(criarItemPlanejamento(5L)));

            Paciente paciente = new Paciente();
            paciente.setPaciCodigo(5L);

            when(prontuarioDentistaRepository.findById(1L)).thenReturn(Optional.of(prontuario));
            when(prontuarioDentistaRepository.saveAndFlush(any())).thenReturn(prontuario);
            when(prontuarioDentistaRepository.save(any())).thenReturn(prontuario);
            when(pacienteRepository.findById(5L)).thenReturn(Optional.of(paciente));

            service.atualizarProntuario(1L, request);

            verify(pacienteRepository).findById(5L);
            assertThat(prontuario.getPlanejamentos()).hasSize(1);
            assertThat(prontuario.getPlanejamentos().get(0).getProcedimentoRealizado())
                    .isEqualTo("Exodontia");
            assertThat(prontuario.getPlanejamentos().get(0).getPaciente()).isEqualTo(paciente);
        }

        @Test
        @DisplayName("Deve atualizar com planejamentos quando pacienteId é nulo")
        void deveAtualizarComPlanejamentosSemPaciente() {
            TenantContext.setCurrentTenant(1L);

            ProntuarioDentista prontuario = criarProntuario(1L);
            CadastrarProntuarioDentistaRequest request = criarRequest();
            request.setPlanejamentos(List.of(criarItemPlanejamento(null)));

            when(prontuarioDentistaRepository.findById(1L)).thenReturn(Optional.of(prontuario));
            when(prontuarioDentistaRepository.saveAndFlush(any())).thenReturn(prontuario);
            when(prontuarioDentistaRepository.save(any())).thenReturn(prontuario);

            service.atualizarProntuario(1L, request);

            verify(pacienteRepository, never()).findById(any());
            assertThat(prontuario.getPlanejamentos()).hasSize(1);
            assertThat(prontuario.getPlanejamentos().get(0).getPaciente()).isNull();
        }

        @Test
        @DisplayName("Deve limpar planejamentos antes de recriar")
        void deveLimparPlanejamentosAntesDeRecriar() {
            ProntuarioDentista prontuario = criarProntuario(1L);

            when(prontuarioDentistaRepository.findById(1L)).thenReturn(Optional.of(prontuario));
            when(prontuarioDentistaRepository.saveAndFlush(any())).thenReturn(prontuario);
            when(prontuarioDentistaRepository.save(any())).thenReturn(prontuario);

            service.atualizarProntuario(1L, criarRequest());

            assertThat(prontuario.getPlanejamentos()).isEmpty();
            assertThat(prontuario.getDentes()).isEmpty();
        }
    }
}
