package br.com.saudeConecta.usecase;

import br.com.saudeConecta.domain.consulta.Consulta;
import br.com.saudeConecta.domain.consulta.StatusConsulta;
import br.com.saudeConecta.domain.paciente.Paciente;
import br.com.saudeConecta.domain.profissional.Especialidade;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.domain.prontuario.ProntuarioDentista;
import br.com.saudeConecta.domain.prontuario.ProntuarioDentistaDente;
import br.com.saudeConecta.domain.prontuario.Prontuario;
import br.com.saudeConecta.infra.tenant.TenantHelper;
import br.com.saudeConecta.infrastructure.persistence.repository.ConsultaRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.ProntuarioDentistaRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.ProntuarioRepository;
import br.com.saudeConecta.presentation.dto.consulta.HistoricoConsultaPacienteResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BuscarHistoricoCompletoPaciente UseCases - Testes unitários")
class BuscarHistoricoCompletoPacienteUseCaseTest {

    // ========== Odontológico ==========

    @Nested
    @DisplayName("BuscarHistoricoCompletoPacienteUseCase (Dentista)")
    class DentistaUseCase {

        @Mock private ConsultaRepository consultaRepository;
        @Mock private ProntuarioDentistaRepository prontuarioDentistaRepository;
        @Mock private TenantHelper tenantHelper;

        @InjectMocks
        private BuscarHistoricoCompletoPacienteUseCase useCase;

        @Test
        @DisplayName("Deve retornar lista vazia quando não há consultas")
        void deveRetornarListaVaziaQuandoSemConsultas() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(1L);
            when(consultaRepository.findHistoricoCompletoPaciente(eq(10L), eq(1L), isNull()))
                    .thenReturn(Collections.emptyList());

            List<HistoricoConsultaPacienteResponse> resultado = useCase.executar(10L, null);

            assertThat(resultado).isEmpty();
        }

        @Test
        @DisplayName("Deve retornar histórico com consulta sem prontuário")
        void deveRetornarHistoricoSemProntuario() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(1L);

            Consulta consulta = criarConsulta(100L);
            when(consultaRepository.findHistoricoCompletoPaciente(eq(10L), eq(1L), isNull()))
                    .thenReturn(List.of(consulta));
            when(prontuarioDentistaRepository.findByConsultaId(100L))
                    .thenReturn(Collections.emptyList());

            List<HistoricoConsultaPacienteResponse> resultado = useCase.executar(10L, null);

            assertThat(resultado).hasSize(1);
            assertThat(resultado.get(0).getConsultaId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("Deve retornar histórico com prontuário dentista preenchido")
        void deveRetornarHistoricoComProntuario() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(1L);

            Consulta consulta = criarConsultaCompleta(100L);
            ProntuarioDentista prontuario = criarProntuarioDentista(50L);

            when(consultaRepository.findHistoricoCompletoPaciente(eq(10L), eq(1L), isNull()))
                    .thenReturn(List.of(consulta));
            when(prontuarioDentistaRepository.findByConsultaId(100L))
                    .thenReturn(List.of(prontuario));

            List<HistoricoConsultaPacienteResponse> resultado = useCase.executar(10L, null);

            assertThat(resultado).hasSize(1);
            assertThat(resultado.get(0).getProntuarioId()).isEqualTo(50L);
            verify(prontuarioDentistaRepository).findByConsultaId(100L);
        }

        @Test
        @DisplayName("Deve filtrar por profissional quando informado")
        void deveFiltrarPorProfissional() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(1L);
            when(consultaRepository.findHistoricoCompletoPaciente(eq(10L), eq(1L), eq(5L)))
                    .thenReturn(Collections.emptyList());

            useCase.executar(10L, 5L);

            verify(consultaRepository).findHistoricoCompletoPaciente(10L, 1L, 5L);
        }

        @Test
        @DisplayName("Deve mapear dentes e planejamentos quando prontuário tem dados")
        void deveMapearDentesEPlanejamentos() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(1L);

            Consulta consulta = criarConsultaCompleta(100L);
            ProntuarioDentistaDente dente = new ProntuarioDentistaDente();
            dente.setCodigo(1L);
            dente.setNumeroFdi(11);
            dente.setStatus("cariado");
            dente.setObservacao("Cárie na face vestibular");

            ProntuarioDentista prontuario = criarProntuarioDentista(50L);
            prontuario.setDentes(Set.of(dente));

            when(consultaRepository.findHistoricoCompletoPaciente(eq(10L), eq(1L), isNull()))
                    .thenReturn(List.of(consulta));
            when(prontuarioDentistaRepository.findByConsultaId(100L))
                    .thenReturn(List.of(prontuario));

            List<HistoricoConsultaPacienteResponse> resultado = useCase.executar(10L, null);

            assertThat(resultado).hasSize(1);
            assertThat(resultado.get(0).getDentes()).hasSize(1);
            assertThat(resultado.get(0).getDentes().get(0).getNumeroFdi()).isEqualTo(11);
        }
    }

    // ========== Médico ==========

    @Nested
    @DisplayName("BuscarHistoricoCompletoPacienteMedicoUseCase (Médico)")
    class MedicoUseCase {

        @Mock private ConsultaRepository consultaRepository;
        @Mock private ProntuarioRepository prontuarioRepository;
        @Mock private TenantHelper tenantHelper;

        @InjectMocks
        private BuscarHistoricoCompletoPacienteMedicoUseCase useCase;

        @Test
        @DisplayName("Deve retornar lista vazia quando não há consultas médicas")
        void deveRetornarListaVaziaQuandoSemConsultas() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(1L);
            when(consultaRepository.findHistoricoCompletoPacienteMedico(eq(10L), eq(1L), isNull()))
                    .thenReturn(Collections.emptyList());

            List<HistoricoConsultaPacienteResponse> resultado = useCase.executar(10L, null);

            assertThat(resultado).isEmpty();
        }

        @Test
        @DisplayName("Deve retornar histórico médico com consulta sem prontuário")
        void deveRetornarHistoricoSemProntuario() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(1L);

            Consulta consulta = criarConsulta(200L);
            when(consultaRepository.findHistoricoCompletoPacienteMedico(eq(10L), eq(1L), isNull()))
                    .thenReturn(List.of(consulta));
            when(prontuarioRepository.findByConsulta_Id(200L)).thenReturn(null);

            List<HistoricoConsultaPacienteResponse> resultado = useCase.executar(10L, null);

            assertThat(resultado).hasSize(1);
            assertThat(resultado.get(0).getConsultaId()).isEqualTo(200L);
        }

        @Test
        @DisplayName("Deve retornar histórico médico com prontuário preenchido")
        void deveRetornarHistoricoComProntuario() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(1L);

            Consulta consulta = criarConsultaCompleta(200L);
            Prontuario prontuario = criarProntuarioMedico(99L);

            when(consultaRepository.findHistoricoCompletoPacienteMedico(eq(10L), eq(1L), isNull()))
                    .thenReturn(List.of(consulta));
            when(prontuarioRepository.findByConsulta_Id(200L)).thenReturn(prontuario);

            List<HistoricoConsultaPacienteResponse> resultado = useCase.executar(10L, null);

            assertThat(resultado).hasSize(1);
            assertThat(resultado.get(0).getProntuarioId()).isEqualTo(99L);
        }

        @Test
        @DisplayName("Deve filtrar por profissional quando informado")
        void deveFiltrarPorProfissional() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(1L);
            when(consultaRepository.findHistoricoCompletoPacienteMedico(eq(10L), eq(1L), eq(7L)))
                    .thenReturn(Collections.emptyList());

            useCase.executar(10L, 7L);

            verify(consultaRepository).findHistoricoCompletoPacienteMedico(10L, 1L, 7L);
        }
    }

    // ========== Auxiliares ==========

    private static Consulta criarConsulta(Long id) {
        Consulta consulta = new Consulta();
        consulta.setId(id);
        consulta.setDataHora(LocalDateTime.now());
        consulta.setStatus(StatusConsulta.REALIZADA);
        return consulta;
    }

    private static Consulta criarConsultaCompleta(Long id) {
        Consulta consulta = criarConsulta(id);

        Paciente paciente = new Paciente();
        paciente.setPaciCodigo(10L);
        paciente.setPaciNome("Ana Souza");
        paciente.setPaciCpf("12345678900");
        consulta.setPaciente(paciente);

        Profissional profissional = new Profissional();
        profissional.setId(1L);
        profissional.setNome("Dr. Silva");
        profissional.setRegistroConselho("CRM-12345");
        consulta.setProfissional(profissional);

        Especialidade especialidade = new Especialidade();
        especialidade.setNome("Clínica Geral");
        consulta.setEspecialidade(especialidade);

        return consulta;
    }

    private static ProntuarioDentista criarProntuarioDentista(Long id) {
        ProntuarioDentista p = new ProntuarioDentista();
        p.setCodigo(id);
        p.setQueixaPrincipal("Dor de dente");
        p.setAnamnese("Paciente relata...");
        p.setDiagnostico("Cárie");
        p.setDentes(new HashSet<>());
        p.setPlanejamentos(new ArrayList<>());
        return p;
    }

    private static Prontuario criarProntuarioMedico(Long id) {
        Prontuario p = new Prontuario();
        p.setProntCodigoProntuario(id);
        p.setProntQueixaPricipal("Dor de cabeça");
        p.setProntAnamnese("Paciente relata...");
        p.setProntDiagnostico("Enxaqueca");
        p.setPlanejamentos(Collections.emptyList());
        return p;
    }
}
