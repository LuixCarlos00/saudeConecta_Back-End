package br.com.saudeConecta.application.historico;

import br.com.saudeConecta.application.consulta.dto.HistoricoConsultaPacienteResponse;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.domain.profissional.TipoProfissional;
import br.com.saudeConecta.domain.usuario.TipoUsuarioNovo;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.infrastructure.persistence.repository.ProfissionalRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BuscarHistoricoPacientePorPerfilUseCase - histórico visível conforme o perfil do usuário")
class BuscarHistoricoPacientePorPerfilUseCaseTest {

    @Mock private BuscarHistoricoCompletoPacienteDentistaUseCase dentista;
    @Mock private BuscarHistoricoCompletoPacienteMedicoUseCase medico;
    @Mock private BuscarHistoricoCompletoPacienteAdminUseCase admin;
    @Mock private ProfissionalRepository profissionalRepository;

    @InjectMocks
    private BuscarHistoricoPacientePorPerfilUseCase useCase;

    private final List<HistoricoConsultaPacienteResponse> historico =
            List.of(HistoricoConsultaPacienteResponse.builder().consultaId(1L).build());

    private Usuario usuario(TipoUsuarioNovo tipo) {
        return Usuario.builder().id(50L).tipoUsuarioNovo(tipo).build();
    }

    private Profissional profissional(Long id, String codigoTipo) {
        Profissional p = new Profissional();
        p.setId(id);
        if (codigoTipo != null) {
            p.setTipoProfissional(TipoProfissional.builder().codigo(codigoTipo).build());
        }
        return p;
    }

    @Test
    @DisplayName("Gestor vê o histórico completo, sem consultar o profissional")
    void gestorVeTudo() {
        when(admin.executar(20L)).thenReturn(historico);

        assertThat(useCase.executar(20L, usuario(TipoUsuarioNovo.GESTOR))).contains(historico);
        verifyNoInteractions(profissionalRepository, medico, dentista);
    }

    @Test
    @DisplayName("Dentista vê só os prontuários odontológicos dele")
    void dentistaVeOsDele() {
        when(profissionalRepository.findByUsuarioIdWithRelations(50L)).thenReturn(Optional.of(profissional(8L, "DENTISTA")));
        when(dentista.executar(20L, 8L)).thenReturn(historico);

        assertThat(useCase.executar(20L, usuario(TipoUsuarioNovo.CLINICO))).contains(historico);
        verifyNoInteractions(admin, medico);
    }

    @Test
    @DisplayName("Médico (ou profissional sem tipo) usa o caso de uso médico com o id do profissional")
    void medicoOuSemTipo() {
        when(profissionalRepository.findByUsuarioIdWithRelations(50L)).thenReturn(Optional.of(profissional(3L, null)));
        when(medico.executar(20L, 3L)).thenReturn(historico);

        assertThat(useCase.executar(20L, usuario(TipoUsuarioNovo.CLINICO))).contains(historico);
        verifyNoInteractions(admin, dentista);
    }

    @Test
    @DisplayName("Clínico sem cadastro de profissional: vazio (o controller responde 404)")
    void clinicoSemProfissional() {
        when(profissionalRepository.findByUsuarioIdWithRelations(50L)).thenReturn(Optional.empty());

        assertThat(useCase.executar(20L, usuario(TipoUsuarioNovo.CLINICO))).isEmpty();
        verifyNoInteractions(admin, medico, dentista);
    }

    @Test
    @DisplayName("Caso de uso devolvendo lista nula: vazio em vez de NPE")
    void listaNula() {
        when(admin.executar(20L)).thenReturn(null);

        assertThat(useCase.executar(20L, usuario(TipoUsuarioNovo.GESTOR))).isEmpty();
    }
}
