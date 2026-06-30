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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EspecialidadeService - Testes de CRUD de especialidades")
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
                .tipoProfissional(tipoProfissionalMedico)
                .nome("Cardiologia")
                .codigo("CARDIO")
                .status((byte) 1)
                .build();
    }

    // ========== listarMedicas / listarOdontologicas ==========

    @Nested
    @DisplayName("Listagem por tipo profissional")
    class ListagemPorTipo {

        @Test
        @DisplayName("listarMedicas deve delegar com código MEDICO")
        void deveListarMedicasComCodigoMedico() {
            when(especialidadeRepository.findAtivasByTipoProfissionalCodigo("MEDICO"))
                    .thenReturn(List.of(especialidadeMock));

            List<Especialidade> resultado = especialidadeService.listarMedicas();

            assertThat(resultado).hasSize(1);
            assertThat(resultado.get(0).getNome()).isEqualTo("Cardiologia");
            verify(especialidadeRepository).findAtivasByTipoProfissionalCodigo("MEDICO");
        }

        @Test
        @DisplayName("listarOdontologicas deve delegar com código DENTISTA")
        void deveListarOdontologicasComCodigoDentista() {
            Especialidade odonto = Especialidade.builder()
                    .id(20L).nome("Ortodontia").status((byte) 1).build();

            when(especialidadeRepository.findAtivasByTipoProfissionalCodigo("DENTISTA"))
                    .thenReturn(List.of(odonto));

            List<Especialidade> resultado = especialidadeService.listarOdontologicas();

            assertThat(resultado).hasSize(1);
            assertThat(resultado.get(0).getNome()).isEqualTo("Ortodontia");
        }

        @Test
        @DisplayName("listarPorTipo deve converter código para maiúsculas")
        void deveConverterCodigoParaMaiusculas() {
            when(especialidadeRepository.findAtivasByTipoProfissionalCodigo("MEDICO"))
                    .thenReturn(List.of(especialidadeMock));

            especialidadeService.listarPorTipo("medico");

            verify(especialidadeRepository).findAtivasByTipoProfissionalCodigo("MEDICO");
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

    // ========== criar ==========

    @Nested
    @DisplayName("Criação de especialidade")
    class Criacao {

        @Test
        @DisplayName("Deve criar especialidade com dados válidos")
        void deveCriarEspecialidadeComDadosValidos() {
            when(tipoProfissionalRepository.findById(1L)).thenReturn(Optional.of(tipoProfissionalMedico));
            when(especialidadeRepository.existsByTipoProfissional_IdAndNome(1L, "Neurologia")).thenReturn(false);
            when(especialidadeRepository.save(any(Especialidade.class))).thenAnswer(inv -> inv.getArgument(0));

            Especialidade resultado = especialidadeService.criar(1L, "Neurologia", "NEURO");

            assertThat(resultado.getNome()).isEqualTo("Neurologia");
            assertThat(resultado.getCodigo()).isEqualTo("NEURO");
            assertThat(resultado.getStatus()).isEqualTo((byte) 1);
            assertThat(resultado.getTipoProfissional()).isEqualTo(tipoProfissionalMedico);
            verify(especialidadeRepository).save(any(Especialidade.class));
        }

        @Test
        @DisplayName("Deve lançar exceção quando tipo profissional não for encontrado")
        void deveLancarExcecaoQuandoTipoProfissionalNaoEncontrado() {
            when(tipoProfissionalRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> especialidadeService.criar(99L, "Neurologia", "NEURO"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Tipo profissional não encontrado");

            verify(especialidadeRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção ao criar especialidade com nome duplicado para o mesmo tipo")
        void deveLancarExcecaoParaNomeDuplicado() {
            when(tipoProfissionalRepository.findById(1L)).thenReturn(Optional.of(tipoProfissionalMedico));
            when(especialidadeRepository.existsByTipoProfissional_IdAndNome(1L, "Cardiologia")).thenReturn(true);

            assertThatThrownBy(() -> especialidadeService.criar(1L, "Cardiologia", "CARDIO"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Já existe uma especialidade");

            verify(especialidadeRepository, never()).save(any());
        }
    }

    // ========== atualizar ==========

    @Nested
    @DisplayName("Atualização de especialidade")
    class Atualizacao {

        @Test
        @DisplayName("Deve atualizar nome e código com dados válidos")
        void deveAtualizarComDadosValidos() {
            when(especialidadeRepository.findById(10L)).thenReturn(Optional.of(especialidadeMock));
            when(especialidadeRepository.findByTipoProfissional_IdAndNome(1L, "Hematologia"))
                    .thenReturn(Optional.empty());
            when(especialidadeRepository.save(any(Especialidade.class))).thenAnswer(inv -> inv.getArgument(0));

            Especialidade resultado = especialidadeService.atualizar(10L, "Hematologia", "HEMA", null);

            assertThat(resultado.getNome()).isEqualTo("Hematologia");
            assertThat(resultado.getCodigo()).isEqualTo("HEMA");
        }

        @Test
        @DisplayName("Deve atualizar status quando fornecido")
        void deveAtualizarStatus() {
            when(especialidadeRepository.findById(10L)).thenReturn(Optional.of(especialidadeMock));
            when(especialidadeRepository.findByTipoProfissional_IdAndNome(anyLong(), anyString()))
                    .thenReturn(Optional.empty());
            when(especialidadeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Especialidade resultado = especialidadeService.atualizar(10L, "Cardiologia", "CARDIO", (byte) 0);

            assertThat(resultado.getStatus()).isEqualTo((byte) 0);
        }

        @Test
        @DisplayName("Deve lançar exceção ao atualizar especialidade inexistente")
        void deveLancarExcecaoAoAtualizarInexistente() {
            when(especialidadeRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> especialidadeService.atualizar(99L, "Teste", "TST", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Especialidade não encontrada");
        }

        @Test
        @DisplayName("Deve lançar exceção ao renomear para nome já usado por outra especialidade do mesmo tipo")
        void deveLancarExcecaoParaNomeDuplicadoEmOutraEspecialidade() {
            Especialidade outra = Especialidade.builder()
                    .id(99L).nome("Neurologia").tipoProfissional(tipoProfissionalMedico).build();

            when(especialidadeRepository.findById(10L)).thenReturn(Optional.of(especialidadeMock));
            when(especialidadeRepository.findByTipoProfissional_IdAndNome(1L, "Neurologia"))
                    .thenReturn(Optional.of(outra));

            assertThatThrownBy(() -> especialidadeService.atualizar(10L, "Neurologia", "NEURO", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Já existe uma especialidade");
        }

        @Test
        @DisplayName("Deve permitir manter o mesmo nome da própria especialidade na atualização")
        void devePermitirManterMesmoNomeProprio() {
            when(especialidadeRepository.findById(10L)).thenReturn(Optional.of(especialidadeMock));
            when(especialidadeRepository.findByTipoProfissional_IdAndNome(1L, "Cardiologia"))
                    .thenReturn(Optional.of(especialidadeMock));
            when(especialidadeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Especialidade resultado = especialidadeService.atualizar(10L, "Cardiologia", "CARDIO_NOVO", null);

            assertThat(resultado.getCodigo()).isEqualTo("CARDIO_NOVO");
        }
    }

    // ========== deletar (soft delete) ==========

    @Nested
    @DisplayName("Exclusão (soft delete)")
    class Exclusao {

        @Test
        @DisplayName("Deve realizar soft delete definindo status = 0")
        void deveFazerSoftDelete() {
            when(especialidadeRepository.findById(10L)).thenReturn(Optional.of(especialidadeMock));
            when(especialidadeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            especialidadeService.deletar(10L);

            assertThat(especialidadeMock.getStatus()).isEqualTo((byte) 0);
            verify(especialidadeRepository).save(especialidadeMock);
        }

        @Test
        @DisplayName("Deve lançar exceção ao tentar soft delete de especialidade inexistente")
        void deveLancarExcecaoSoftDeleteInexistente() {
            when(especialidadeRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> especialidadeService.deletar(99L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Especialidade não encontrada");

            verify(especialidadeRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve realizar exclusão permanente quando especialidade existir")
        void deveDeletarPermanentementeQuandoExistir() {
            when(especialidadeRepository.existsById(10L)).thenReturn(true);

            especialidadeService.deletarPermanente(10L);

            verify(especialidadeRepository).deleteById(10L);
        }

        @Test
        @DisplayName("Deve lançar exceção ao tentar exclusão permanente de especialidade inexistente")
        void deveLancarExcecaoDeletarPermanenteInexistente() {
            when(especialidadeRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> especialidadeService.deletarPermanente(99L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Especialidade não encontrada");

            verify(especialidadeRepository, never()).deleteById(any());
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

    // ========== listarTodas / carregarEspecialidades / listarPorTipoId ==========

    @Nested
    @DisplayName("Listagem geral de especialidades")
    class ListagemGeral {

        @Test
        @DisplayName("listarTodas deve retornar todas as especialidades")
        void deveListarTodas() {
            when(especialidadeRepository.findAll()).thenReturn(List.of(especialidadeMock));

            List<Especialidade> resultado = especialidadeService.listarTodas();

            assertThat(resultado).hasSize(1);
            verify(especialidadeRepository).findAll();
        }

        @Test
        @DisplayName("carregarEspecialidades deve retornar ativas com tipo profissional")
        void deveCarregarEspecialidades() {
            when(especialidadeRepository.findAllAtivasWithTipoProfissional())
                    .thenReturn(List.of(especialidadeMock));

            List<Especialidade> resultado = especialidadeService.carregarEspecialidades();

            assertThat(resultado).hasSize(1);
            verify(especialidadeRepository).findAllAtivasWithTipoProfissional();
        }

        @Test
        @DisplayName("listarPorTipoId deve retornar especialidades ativas do tipo")
        void deveListarPorTipoId() {
            when(especialidadeRepository.findByTipoProfissional_IdAndStatus(1L, (byte) 1))
                    .thenReturn(List.of(especialidadeMock));

            List<Especialidade> resultado = especialidadeService.listarPorTipoId(1L);

            assertThat(resultado).hasSize(1);
            verify(especialidadeRepository).findByTipoProfissional_IdAndStatus(1L, (byte) 1);
        }
    }
}
