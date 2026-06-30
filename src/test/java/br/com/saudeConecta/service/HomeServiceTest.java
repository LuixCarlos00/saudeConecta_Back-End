package br.com.saudeConecta.service;

import br.com.saudeConecta.domain.admin.AdminOrganizacao;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.domain.secretaria.Secretaria;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.email.EmailNotificacaoService;
import br.com.saudeConecta.infrastructure.persistence.repository.AdminOrganizacaoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.ProfissionalRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.SecretariaRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("HomeService - Testes de recuperação de senha")
class HomeServiceTest {

    @Mock
    private ProfissionalRepository profissionalRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private SecretariaRepository secretariaRepository;

    @Mock
    private AdminOrganizacaoRepository adminOrganizacaoRepository;

    @Mock
    private EmailNotificacaoService emailNotificacaoService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private HomeService homeService;

    private Usuario usuarioMock;

    @BeforeEach
    void setUp() {
        usuarioMock = Usuario.builder()
                .id(1L)
                .login("usuario.teste")
                .senha("senhaHash")
                .build();
    }

    // ========== recuperarSenhaPorEmail — via Profissional ==========

    @Test
    @DisplayName("Deve recuperar senha quando email pertence a um Profissional")
    void deveRecuperarSenhaViaProfissional() {
        Profissional profissional = Profissional.builder()
                .nome("Dr. Teste")
                .usuario(usuarioMock)
                .build();

        when(profissionalRepository.findByEmail("dr.teste@email.com"))
                .thenReturn(Optional.of(profissional));
        when(passwordEncoder.encode(anyString())).thenReturn("novaSenhaHash");

        homeService.recuperarSenhaPorEmail("dr.teste@email.com");

        verify(usuarioRepository).save(usuarioMock);
        verify(emailNotificacaoService).enviarRecuperacaoSenha(
                eq("dr.teste@email.com"), anyString(), anyString(), anyString(), any());
    }

    // ========== recuperarSenhaPorEmail — via Secretaria ==========

    @Test
    @DisplayName("Deve recuperar senha quando email pertence a uma Secretária")
    void deveRecuperarSenhaViaSecretaria() {
        Secretaria secretaria = Secretaria.builder()
                .nome("Sec. Teste")
                .usuario(usuarioMock)
                .build();

        when(profissionalRepository.findByEmail("sec@email.com")).thenReturn(Optional.empty());
        when(secretariaRepository.findByEmail("sec@email.com")).thenReturn(Optional.of(secretaria));
        when(passwordEncoder.encode(anyString())).thenReturn("novaSenhaHash");

        homeService.recuperarSenhaPorEmail("sec@email.com");

        verify(usuarioRepository).save(usuarioMock);
        verify(emailNotificacaoService).enviarRecuperacaoSenha(
                eq("sec@email.com"), anyString(), anyString(), anyString(), any());
    }

    // ========== recuperarSenhaPorEmail — via AdminOrganizacao ==========

    @Test
    @DisplayName("Deve recuperar senha quando email pertence a um AdminOrganizacao")
    void deveRecuperarSenhaViaAdminOrg() {
        AdminOrganizacao admin = AdminOrganizacao.builder()
                .nome("Admin Teste")
                .usuario(usuarioMock)
                .build();

        when(profissionalRepository.findByEmail("admin@email.com")).thenReturn(Optional.empty());
        when(secretariaRepository.findByEmail("admin@email.com")).thenReturn(Optional.empty());
        when(adminOrganizacaoRepository.findByEmail("admin@email.com")).thenReturn(Optional.of(admin));
        when(passwordEncoder.encode(anyString())).thenReturn("novaSenhaHash");

        homeService.recuperarSenhaPorEmail("admin@email.com");

        verify(usuarioRepository).save(usuarioMock);
        verify(emailNotificacaoService).enviarRecuperacaoSenha(
                eq("admin@email.com"), anyString(), anyString(), anyString(), any());
    }

    // ========== recuperarSenhaPorEmail — via Usuario (SUPER_ADMIN) ==========

    @Test
    @DisplayName("Deve recuperar senha quando email pertence a usuário sem organização (SUPER_ADMIN)")
    void deveRecuperarSenhaViaSuperAdmin() {
        when(profissionalRepository.findByEmail("super@email.com")).thenReturn(Optional.empty());
        when(secretariaRepository.findByEmail("super@email.com")).thenReturn(Optional.empty());
        when(adminOrganizacaoRepository.findByEmail("super@email.com")).thenReturn(Optional.empty());
        when(usuarioRepository.findByEmailAndOrganizacaoIsNull("super@email.com"))
                .thenReturn(Optional.of(usuarioMock));
        when(passwordEncoder.encode(anyString())).thenReturn("novaSenhaHash");

        homeService.recuperarSenhaPorEmail("super@email.com");

        verify(usuarioRepository).save(usuarioMock);
        verify(emailNotificacaoService).enviarRecuperacaoSenha(
                eq("super@email.com"), anyString(), anyString(), anyString(), any());
    }

    // ========== recuperarSenhaPorEmail — email não encontrado ==========

    @Test
    @DisplayName("Deve lançar EmailNaoEncontradoException quando email não existe no sistema")
    void deveLancarExcecaoQuandoEmailNaoEncontrado() {
        when(profissionalRepository.findByEmail("inexistente@email.com")).thenReturn(Optional.empty());
        when(secretariaRepository.findByEmail("inexistente@email.com")).thenReturn(Optional.empty());
        when(adminOrganizacaoRepository.findByEmail("inexistente@email.com")).thenReturn(Optional.empty());
        when(usuarioRepository.findByEmailAndOrganizacaoIsNull("inexistente@email.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> homeService.recuperarSenhaPorEmail("inexistente@email.com"))
                .isInstanceOf(HomeService.EmailNaoEncontradoException.class)
                .hasMessageContaining("Email nao encontrado no sistema");

        verify(usuarioRepository, never()).save(any());
        verify(emailNotificacaoService, never()).enviarRecuperacaoSenha(any(), any(), any(), any(), any());
    }

    // ========== Ordem de busca ==========

    @Test
    @DisplayName("Deve priorizar busca em Profissional antes das demais entidades")
    void devePriorizarBuscaEmProfissional() {
        Profissional profissional = Profissional.builder()
                .nome("Dr. Prioritário")
                .usuario(usuarioMock)
                .build();

        when(profissionalRepository.findByEmail("teste@email.com"))
                .thenReturn(Optional.of(profissional));
        when(passwordEncoder.encode(anyString())).thenReturn("hash");

        homeService.recuperarSenhaPorEmail("teste@email.com");

        verify(secretariaRepository, never()).findByEmail(any());
        verify(adminOrganizacaoRepository, never()).findByEmail(any());
        verify(usuarioRepository, never()).findByEmailAndOrganizacaoIsNull(any());
    }

    // ========== Senha gerada ==========

    @Test
    @DisplayName("Deve codificar a nova senha antes de salvar no banco")
    void deveCodificarNovaSenhaAntesDeSalvar() {
        Profissional profissional = Profissional.builder()
                .nome("Dr. Encode")
                .usuario(usuarioMock)
                .build();

        when(profissionalRepository.findByEmail("encode@email.com"))
                .thenReturn(Optional.of(profissional));
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hashedPassword");

        homeService.recuperarSenhaPorEmail("encode@email.com");

        verify(passwordEncoder).encode(anyString());
        verify(usuarioRepository).save(argThat(u -> "$2a$10$hashedPassword".equals(u.getSenha())));
    }
}
