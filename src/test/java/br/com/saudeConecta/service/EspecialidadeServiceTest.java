package br.com.saudeConecta.service;

import br.com.saudeConecta.domain.profissional.Especialidade;
import br.com.saudeConecta.domain.profissional.TipoProfissional;
import br.com.saudeConecta.infrastructure.persistence.repository.EspecialidadeRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.TipoProfissionalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EspecialidadeService - Testes de leitura de especialidades")
class EspecialidadeServiceTest {

    @Mock
    private EspecialidadeRepository especialidadeRepository;

    @Mock
    private TipoProfissionalRepository tipoProfissionalRepository;

    @InjectMocks
    private EspecialidadeService especialidadeService;

    private TipoProfissional tipoProfissionalMedico;
    private Especialidade especialidadeMock;

    @BeforeEach
    void setUp() {
        tipoProfissionalMedico = TipoProfissional.builder()
                .id(1L)
                .codigo("MEDICO")
                .nome("Médico")
                .conselho("CRM")
                .status((byte) 1)
                .build();

        especialidadeMock = Especialidade.builder()
                .id(10L)
                .nome("Cardiologia")
                .codigo("CARDIO")
                .status((byte) 1)
                .tipoProfissional(tipoProfissionalMedico)
                .build();
    }

    // ========== listarTodas ==========

    @Nested
    @DisplayName("Listagem de todas as especialidades")
    class ListagemTodas {

        @Test
        @DisplayName("Deve retornar todas as especialidades")
        void deveListarTodas() {
            when(especialidadeRepository.findAll()).thenReturn(List.of(especialidadeMock));

            List<Especialidade> resultado = especialidadeService.listarTodas();

            assertThat(resultado).hasSize(1);
            assertThat(resultado.get(0).getNome()).isEqualTo("Cardiologia");
            verify(especialidadeRepository).findAll();
        }
    }

    // ========== carregarEspecialidades ==========

    @Nested
    @DisplayName("Carregamento de especialidades com cache")
    class CarregamentoEspecialidades {

        @Test
        @DisplayName("Deve carregar especialidades ativas com tipo profissional")
        void deveCarregarEspecialidadesAtivas() {
            when(especialidadeRepository.findAllAtivasWithTipoProfissional())
                    .thenReturn(List.of(especialidadeMock));

            List<Especialidade> resultado = especialidadeService.carregarEspecialidades();

            assertThat(resultado).hasSize(1);
            assertThat(resultado.get(0).getNome()).isEqualTo("Cardiologia");
            verify(especialidadeRepository).findAllAtivasWithTipoProfissional();
        }
    }

    // ========== buscarPorId ==========

    @Nested
    @DisplayName("Busca por ID")
    class BuscaPorId {

        @Test
        @DisplayName("Deve retornar especialidade quando ID existir")
        void deveRetornarEspecialidadeExistente() {
            when(especialidadeRepository.findById(10L)).thenReturn(Optional.of(especialidadeMock));

            Optional<Especialidade> resultado = especialidadeService.buscarPorId(10L);

            assertThat(resultado).isPresent();
            assertThat(resultado.get().getNome()).isEqualTo("Cardiologia");
        }

        @Test
        @DisplayName("Deve retornar Optional vazio quando ID não existir")
        void deveRetornarVazioQuandoIdNaoExistir() {
            when(especialidadeRepository.findById(99L)).thenReturn(Optional.empty());

            Optional<Especialidade> resultado = especialidadeService.buscarPorId(99L);

            assertThat(resultado).isEmpty();
        }
    }

    // ========== listarPorTipo ==========

    @Nested
    @DisplayName("Listagem por tipo de profissional")
    class ListagemPorTipo {

        @Test
        @DisplayName("Deve listar especialidades por código do tipo")
        void deveListarPorCodigoTipo() {
            when(especialidadeRepository.findAtivasByTipoProfissionalCodigo("MEDICO"))
                    .thenReturn(List.of(especialidadeMock));

            List<Especialidade> resultado = especialidadeService.listarPorTipo("medico");

            assertThat(resultado).hasSize(1);
            assertThat(resultado.get(0).getNome()).isEqualTo("Cardiologia");
            verify(especialidadeRepository).findAtivasByTipoProfissionalCodigo("MEDICO");
        }

        @Test
        @DisplayName("Deve listar especialidades por ID do tipo")
        void deveListarPorIdTipo() {
            when(especialidadeRepository.findByTipoProfissional_IdAndStatus(1L, (byte) 1))
                    .thenReturn(List.of(especialidadeMock));

            List<Especialidade> resultado = especialidadeService.listarPorTipoId(1L);

            assertThat(resultado).hasSize(1);
            verify(especialidadeRepository).findByTipoProfissional_IdAndStatus(1L, (byte) 1);
        }
    }

    // ========== listarMedicas / listarOdontologicas ==========

    @Nested
    @DisplayName("Listagem por especialidade específica")
    class ListagemEspecifica {

        @Test
        @DisplayName("Deve listar especialidades médicas")
        void deveListarEspecialidadesMedicas() {
            when(especialidadeRepository.findAtivasByTipoProfissionalCodigo("MEDICO"))
                    .thenReturn(List.of(especialidadeMock));

            List<Especialidade> resultado = especialidadeService.listarMedicas();

            assertThat(resultado).hasSize(1);
            verify(especialidadeRepository).findAtivasByTipoProfissionalCodigo("MEDICO");
        }

        @Test
        @DisplayName("Deve listar especialidades odontológicas")
        void deveListarEspecialidadesOdontologicas() {
            when(especialidadeRepository.findAtivasByTipoProfissionalCodigo("DENTISTA"))
                    .thenReturn(List.of(especialidadeMock));

            List<Especialidade> resultado = especialidadeService.listarOdontologicas();

            assertThat(resultado).hasSize(1);
            verify(especialidadeRepository).findAtivasByTipoProfissionalCodigo("DENTISTA");
        }
    }

    // ========== TipoProfissional ==========

    @Nested
    @DisplayName("Tipos de Profissional")
    class TiposProf {

        @Test
        @DisplayName("buscarTipoPorCodigo deve converter para maiúsculas antes de buscar")
        void deveBuscarTipoConvertendoParaMaiusculas() {
            when(tipoProfissionalRepository.findByCodigo("MEDICO"))
                    .thenReturn(Optional.of(tipoProfissionalMedico));

            Optional<TipoProfissional> resultado = especialidadeService.buscarTipoPorCodigo("medico");

            assertThat(resultado).isPresent();
            verify(tipoProfissionalRepository).findByCodigo("MEDICO");
        }

        @Test
        @DisplayName("listarTodosTiposProfissional deve retornar todos os tipos")
        void deveListarTodosOsTipos() {
            when(tipoProfissionalRepository.findAll()).thenReturn(List.of(tipoProfissionalMedico));

            List<TipoProfissional> resultado = especialidadeService.listarTodosTiposProfissional();

            assertThat(resultado).hasSize(1);
        }

        @Test
        @DisplayName("listarTiposProfissional deve retornar tipos ativos ordenados por nome")
        void deveListarTiposProfissionalAtivos() {
            when(tipoProfissionalRepository.findByStatusOrderByNomeAsc((byte) 1))
                    .thenReturn(List.of(tipoProfissionalMedico));

            List<TipoProfissional> resultado = especialidadeService.listarTiposProfissional();

            assertThat(resultado).hasSize(1);
            verify(tipoProfissionalRepository).findByStatusOrderByNomeAsc((byte) 1);
        }
    }
}
