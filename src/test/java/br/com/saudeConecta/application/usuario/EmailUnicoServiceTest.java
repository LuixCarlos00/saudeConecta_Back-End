package br.com.saudeConecta.application.usuario;

import br.com.saudeConecta.domain.admin.AdminOrganizacao;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.infrastructure.persistence.repository.AdminOrganizacaoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.ProfissionalRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.SecretariaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailUnicoService - unicidade de e-mail na atualização (4.5d)")
class EmailUnicoServiceTest {

    private static final Long USUARIO_ID = 20L;

    @Mock private AdminOrganizacaoRepository adminOrganizacaoRepository;
    @Mock private ProfissionalRepository profissionalRepository;
    @Mock private SecretariaRepository secretariaRepository;

    @InjectMocks
    private EmailUnicoService emailUnicoService;

    private static Usuario usuario(Long id) {
        Usuario u = new Usuario();
        u.setId(id);
        return u;
    }

    @Test
    @DisplayName("E-mail de outro usuário (em qualquer tabela) é rejeitado")
    void rejeitaEmailDeOutroUsuario() {
        AdminOrganizacao outroAdmin = AdminOrganizacao.builder().usuario(usuario(99L)).build();
        when(adminOrganizacaoRepository.findByEmail("gestor@outra.com")).thenReturn(Optional.of(outroAdmin));

        assertThatThrownBy(() -> emailUnicoService.validarEmailLivreParaAtualizacao(
                "gestor@outra.com", "meu@email.com", USUARIO_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Email");
    }

    @Test
    @DisplayName("O próprio registro não conta como duplicado")
    void aceitaEmailDoProprioUsuario() {
        Profissional proprio = Profissional.builder().usuario(usuario(USUARIO_ID)).build();
        when(adminOrganizacaoRepository.findByEmail("novo@email.com")).thenReturn(Optional.empty());
        when(profissionalRepository.findByEmail("novo@email.com")).thenReturn(Optional.of(proprio));
        when(secretariaRepository.findByEmail("novo@email.com")).thenReturn(Optional.empty());

        assertThatCode(() -> emailUnicoService.validarEmailLivreParaAtualizacao(
                "novo@email.com", "antigo@email.com", USUARIO_ID)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Registro existente sem usuário vinculado conta como outro dono (sem NPE)")
    void registroSemUsuarioContaComoOutro() {
        Profissional semUsuario = Profissional.builder().usuario(null).build();
        when(adminOrganizacaoRepository.findByEmail("x@x.com")).thenReturn(Optional.empty());
        when(profissionalRepository.findByEmail("x@x.com")).thenReturn(Optional.of(semUsuario));

        assertThatThrownBy(() -> emailUnicoService.validarEmailLivreParaAtualizacao("x@x.com", null, USUARIO_ID))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("E-mail inalterado (ignorando caixa), nulo ou vazio não consulta o banco")
    void semTrocaNaoConsulta() {
        emailUnicoService.validarEmailLivreParaAtualizacao("Meu@Email.com", "meu@email.com", USUARIO_ID);
        emailUnicoService.validarEmailLivreParaAtualizacao(null, "meu@email.com", USUARIO_ID);
        emailUnicoService.validarEmailLivreParaAtualizacao(" ", "meu@email.com", USUARIO_ID);

        verify(adminOrganizacaoRepository, never()).findByEmail(anyString());
        verifyNoInteractions(profissionalRepository, secretariaRepository);
    }
}
