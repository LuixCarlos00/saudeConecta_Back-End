package br.com.saudeConecta.application.especialidade;

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

}
