package br.com.saudeConecta.presentation;

import br.com.saudeConecta.domain.usuario.AuditoriaAcessoMaster;
import br.com.saudeConecta.domain.usuario.TipoUsuarioNovo;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.infra.configuracoesseguranca.TokenService;
import br.com.saudeConecta.infrastructure.persistence.repository.*;
import br.com.saudeConecta.presentation.controller.HomeController;
import br.com.saudeConecta.presentation.dto.usuario.DadosLoginUsuario;
import br.com.saudeConecta.service.HomeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("HomeController - senha master auditada e recuperação de senha sem enumeração")
class HomeControllerTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private TokenService tokenService;
    @Mock private HomeService homeService;
    @Mock private ProfissionalRepository profissionalRepository;
    @Mock private AdminOrganizacaoRepository adminOrganizacaoRepository;
    @Mock private SecretariaRepository secretariaRepository;
    @Mock private AssinaturaTenantRepository assinaturaTenantRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuditoriaAcessoMasterRepository auditoriaAcessoMasterRepository;

    @InjectMocks
    private HomeController controller;

    private static final String MENSAGEM_GENERICA =
            "Se o e-mail estiver cadastrado, voce recebera as instrucoes para recuperar a senha.";

    @Test
    @DisplayName("Login com senha master grava auditoria (ROOT, alvo, IP, quando)")
    void senhaMasterGravaAuditoria() {
        Usuario root = Usuario.builder().id(1L).login("root").senha("hash-root")
                .tipoUsuarioNovo(TipoUsuarioNovo.ROOT).build();
        Usuario alvo = Usuario.builder().id(42L).login("11122233344")
                .tipoUsuarioNovo(TipoUsuarioNovo.ROOT).build();
        when(usuarioRepository.findByTipoUsuarioNovoAndOrganizacaoIsNull(TipoUsuarioNovo.ROOT)).thenReturn(root);
        when(passwordEncoder.matches("senha-master", "hash-root")).thenReturn(true);
        when(usuarioRepository.findUsuarioByLogin("11122233344")).thenReturn(alvo);
        when(tokenService.gerarToken(any(Usuario.class), any(), any(), any(), any())).thenReturn("jwt");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.9");

        ResponseEntity<?> resposta = controller.autenticar(
                new DadosLoginUsuario("11122233344", "senha-master", null), request);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        ArgumentCaptor<AuditoriaAcessoMaster> captor = ArgumentCaptor.forClass(AuditoriaAcessoMaster.class);
        verify(auditoriaAcessoMasterRepository).save(captor.capture());
        AuditoriaAcessoMaster auditoria = captor.getValue();
        assertThat(auditoria.getAtorUsuarioId()).isEqualTo(1L);
        assertThat(auditoria.getAtorLogin()).isEqualTo("root");
        assertThat(auditoria.getAlvoUsuarioId()).isEqualTo(42L);
        assertThat(auditoria.getAlvoLogin()).isEqualTo("11122233344");
        assertThat(auditoria.getIp()).isEqualTo("203.0.113.9");
        assertThat(auditoria.getDataHora()).isNotNull();
        verifyNoInteractions(authenticationManager);
    }

    @Test
    @DisplayName("Se a auditoria falhar, o acesso master é negado (segue o fluxo normal de senha)")
    void auditoriaFalhaNegaAcessoMaster() {
        Usuario root = Usuario.builder().id(1L).login("root").senha("hash-root")
                .tipoUsuarioNovo(TipoUsuarioNovo.ROOT).build();
        Usuario alvo = Usuario.builder().id(42L).login("alvo").tipoUsuarioNovo(TipoUsuarioNovo.ROOT).build();
        when(usuarioRepository.findByTipoUsuarioNovoAndOrganizacaoIsNull(TipoUsuarioNovo.ROOT)).thenReturn(root);
        when(passwordEncoder.matches("senha-master", "hash-root")).thenReturn(true);
        when(usuarioRepository.findUsuarioByLogin("alvo")).thenReturn(alvo);
        when(auditoriaAcessoMasterRepository.save(any())).thenThrow(new RuntimeException("banco fora"));
        when(authenticationManager.authenticate(any()))
                .thenThrow(new org.springframework.security.authentication.BadCredentialsException("x"));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> controller.autenticar(
                        new DadosLoginUsuario("alvo", "senha-master", null), new MockHttpServletRequest()))
                .isInstanceOf(org.springframework.security.authentication.BadCredentialsException.class);
        verify(tokenService, never()).gerarToken(any(Usuario.class), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Login normal não grava auditoria de senha master")
    void loginNormalNaoAudita() {
        Usuario root = Usuario.builder().id(1L).login("root").senha("hash-root")
                .tipoUsuarioNovo(TipoUsuarioNovo.ROOT).build();
        when(usuarioRepository.findByTipoUsuarioNovoAndOrganizacaoIsNull(TipoUsuarioNovo.ROOT)).thenReturn(root);
        when(passwordEncoder.matches("minha-senha", "hash-root")).thenReturn(false);
        Usuario usuario = Usuario.builder().id(7L).login("u").tipoUsuarioNovo(TipoUsuarioNovo.ROOT).build();
        when(authenticationManager.authenticate(any()))
                .thenReturn(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(usuario, null));
        when(tokenService.gerarToken(any(Usuario.class), any(), any(), any(), any())).thenReturn("jwt");

        controller.autenticar(new DadosLoginUsuario("u", "minha-senha", null), new MockHttpServletRequest());

        verifyNoInteractions(auditoriaAcessoMasterRepository);
    }

    @Test
    @DisplayName("Esqueci senha: e-mail cadastrado responde 200 com a mensagem genérica")
    void esqueciSenhaEmailCadastrado() {
        ResponseEntity<?> resposta = controller.esqueciMinhaSenha(Map.of("email", "a@b.com"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).isEqualTo(Map.of("message", MENSAGEM_GENERICA));
        verify(homeService).recuperarSenhaPorEmail("a@b.com");
    }

    @Test
    @DisplayName("Esqueci senha: e-mail NÃO cadastrado responde exatamente igual (sem enumeração)")
    void esqueciSenhaEmailNaoCadastradoRespondeIgual() {
        doThrow(new HomeService.EmailNaoEncontradoException("Email nao encontrado no sistema"))
                .when(homeService).recuperarSenhaPorEmail("x@y.com");

        ResponseEntity<?> resposta = controller.esqueciMinhaSenha(Map.of("email", "x@y.com"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).isEqualTo(Map.of("message", MENSAGEM_GENERICA));
    }
}
