package br.com.saudeConecta.application.auth;

import br.com.saudeConecta.application.usuario.HomeService;
import br.com.saudeConecta.application.usuario.dto.DadosLoginUsuario;
import br.com.saudeConecta.config.security.TokenService;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.planos.AssinaturaTenant;
import br.com.saudeConecta.domain.planos.StatusAssinatura;
import br.com.saudeConecta.domain.usuario.AuditoriaAcessoMaster;
import br.com.saudeConecta.domain.usuario.TipoUsuarioNovo;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.infrastructure.persistence.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AutenticacaoService - senha master auditada, perfil, plano e recuperação de senha sem enumeração")
class AutenticacaoServiceTest {

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
    private AutenticacaoService service;

    private Usuario root() {
        return Usuario.builder().id(1L).login("root").senha("hash-root").tipoUsuarioNovo(TipoUsuarioNovo.ROOT).build();
    }

    private Usuario gestorDaClinica(Long orgId) {
        Organizacao org = new Organizacao();
        org.setId(orgId);
        return Usuario.builder().id(7L).login("gestor").tipoUsuarioNovo(TipoUsuarioNovo.GESTOR).organizacao(org).build();
    }

    /** Login normal: a senha não é a master e o AuthenticationManager devolve o usuário. */
    private void loginNormalDe(Usuario usuario) {
        when(usuarioRepository.findByTipoUsuarioNovoAndOrganizacaoIsNull(TipoUsuarioNovo.ROOT)).thenReturn(root());
        when(passwordEncoder.matches(anyString(), eq("hash-root"))).thenReturn(false);
        when(authenticationManager.authenticate(any()))
                .thenReturn(new UsernamePasswordAuthenticationToken(usuario, null));
    }

    @Test
    @DisplayName("Login com senha master grava auditoria (ROOT, alvo, IP, quando) e não passa pelo AuthenticationManager")
    void senhaMasterGravaAuditoria() {
        Usuario alvo = Usuario.builder().id(42L).login("11122233344").tipoUsuarioNovo(TipoUsuarioNovo.ROOT)
                .status(br.com.saudeConecta.domain.usuario.StatusUsuario.ATIVO).build();
        when(usuarioRepository.findByTipoUsuarioNovoAndOrganizacaoIsNull(TipoUsuarioNovo.ROOT)).thenReturn(root());
        when(passwordEncoder.matches("senha-master", "hash-root")).thenReturn(true);
        when(usuarioRepository.findUsuarioByLogin("11122233344")).thenReturn(alvo);
        when(tokenService.gerarToken(any(Usuario.class), any(), any(), any(), any())).thenReturn("jwt");

        ResultadoLogin resultado = service.autenticar(new DadosLoginUsuario("11122233344", "senha-master", null), "203.0.113.9");

        assertThat(resultado.isAutenticado()).isTrue();
        assertThat(resultado.token()).isEqualTo("jwt");
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
        Usuario alvo = Usuario.builder().id(42L).login("alvo").tipoUsuarioNovo(TipoUsuarioNovo.ROOT)
                .status(br.com.saudeConecta.domain.usuario.StatusUsuario.ATIVO).build();
        when(usuarioRepository.findByTipoUsuarioNovoAndOrganizacaoIsNull(TipoUsuarioNovo.ROOT)).thenReturn(root());
        when(passwordEncoder.matches("senha-master", "hash-root")).thenReturn(true);
        when(usuarioRepository.findUsuarioByLogin("alvo")).thenReturn(alvo);
        when(auditoriaAcessoMasterRepository.save(any())).thenThrow(new RuntimeException("banco fora"));
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("x"));

        var dados = new DadosLoginUsuario("alvo", "senha-master", null);
        assertThatThrownBy(() -> service.autenticar(dados, "ip"))
                .isInstanceOf(BadCredentialsException.class);
        verify(tokenService, never()).gerarToken(any(Usuario.class), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Senha master com o login do próprio ROOT não é impersonação: segue o fluxo normal")
    void senhaMasterNoProprioRoot() {
        when(usuarioRepository.findByTipoUsuarioNovoAndOrganizacaoIsNull(TipoUsuarioNovo.ROOT)).thenReturn(root());
        when(passwordEncoder.matches("senha-master", "hash-root")).thenReturn(true);
        when(authenticationManager.authenticate(any())).thenReturn(new UsernamePasswordAuthenticationToken(root(), null));
        when(tokenService.gerarToken(any(Usuario.class), any(), any(), any(), any())).thenReturn("jwt");

        assertThat(service.autenticar(new DadosLoginUsuario("root", "senha-master", null), "ip").isAutenticado()).isTrue();
        verify(usuarioRepository, never()).findUsuarioByLogin(any());
        verifyNoInteractions(auditoriaAcessoMasterRepository, assinaturaTenantRepository);
    }

    @Test
    @DisplayName("Sem ROOT cadastrado: segue o fluxo normal sem auditoria")
    void semRoot() {
        Usuario usuario = Usuario.builder().id(7L).login("u").tipoUsuarioNovo(TipoUsuarioNovo.ROOT).build();
        when(authenticationManager.authenticate(any())).thenReturn(new UsernamePasswordAuthenticationToken(usuario, null));
        when(tokenService.gerarToken(any(Usuario.class), any(), any(), any(), any())).thenReturn("jwt");

        assertThat(service.autenticar(new DadosLoginUsuario("u", "s", null), "ip").isAutenticado()).isTrue();
        verifyNoInteractions(auditoriaAcessoMasterRepository, passwordEncoder);
    }

    @Test
    @DisplayName("CPF com máscara no login normal chega ao AuthenticationManager só com dígitos")
    void loginNormalComCpfMascarado() {
        loginNormalDe(Usuario.builder().id(7L).login("10740759000").tipoUsuarioNovo(TipoUsuarioNovo.ROOT).build());
        when(tokenService.gerarToken(any(Usuario.class), any(), any(), any(), any())).thenReturn("jwt");

        service.autenticar(new DadosLoginUsuario(" 107.407.590-00 ", "minha-senha", null), "ip");

        var captor = org.mockito.ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(captor.capture());
        assertThat(captor.getValue().getPrincipal()).isEqualTo("10740759000");
    }

    @Test
    @DisplayName("CNPJ com máscara na senha master procura o alvo só com dígitos")
    void senhaMasterComCnpjMascarado() {
        Usuario root = Usuario.builder().id(1L).login("root").senha("hash").tipoUsuarioNovo(TipoUsuarioNovo.ROOT).build();
        when(usuarioRepository.findByTipoUsuarioNovoAndOrganizacaoIsNull(TipoUsuarioNovo.ROOT)).thenReturn(root);
        when(passwordEncoder.matches("senha-master", "hash")).thenReturn(true);

        when(authenticationManager.authenticate(any()))
                .thenThrow(new org.springframework.security.authentication.BadCredentialsException("x"));

        // O alvo não existe neste teste: cai no fluxo normal e recusa; importa o login procurado
        org.assertj.core.api.Assertions.catchThrowable(() ->
                service.autenticar(new DadosLoginUsuario("12.345.678/0001-90", "senha-master", null), "ip"));

        verify(usuarioRepository).findUsuarioByLogin("12345678000190");
    }

    @Test
    @DisplayName("Login normal não grava auditoria de senha master")
    void loginNormalNaoAudita() {
        loginNormalDe(Usuario.builder().id(7L).login("u").tipoUsuarioNovo(TipoUsuarioNovo.ROOT).build());
        when(tokenService.gerarToken(any(Usuario.class), any(), any(), any(), any())).thenReturn("jwt");

        service.autenticar(new DadosLoginUsuario("u", "minha-senha", null), "ip");

        verifyNoInteractions(auditoriaAcessoMasterRepository);
    }

    @Test
    @DisplayName("Perfil divergente: recusa TIPO_DIVERGENTE sem gerar token")
    void perfilDivergente() {
        loginNormalDe(gestorDaClinica(10L));

        ResultadoLogin resultado = service.autenticar(new DadosLoginUsuario("gestor", "s", "clinico"), "ip");

        assertThat(resultado.recusa()).isEqualTo(ResultadoLogin.Recusa.TIPO_DIVERGENTE);
        assertThat(resultado.mensagem()).isEqualTo("O tipo selecionado não corresponde ao tipo do usuário no sistema");
        verifyNoInteractions(tokenService);
    }

    @Test
    @DisplayName("Perfil inexistente: recusa TIPO_INVALIDO")
    void perfilInvalido() {
        loginNormalDe(gestorDaClinica(10L));

        ResultadoLogin resultado = service.autenticar(new DadosLoginUsuario("gestor", "s", "pirata"), "ip");

        assertThat(resultado.recusa()).isEqualTo(ResultadoLogin.Recusa.TIPO_INVALIDO);
        assertThat(resultado.mensagem()).isEqualTo("Tipo inválido");
    }

    @Test
    @DisplayName("Perfil em branco é ignorado")
    void perfilEmBranco() {
        loginNormalDe(gestorDaClinica(10L));
        when(assinaturaTenantRepository.findAssinaturaVigenteByOrganizacaoId(10L))
                .thenReturn(Optional.of(AssinaturaTenant.builder().status(StatusAssinatura.TRIAL).build()));
        when(tokenService.gerarToken(any(Usuario.class), any(), any(), any(), any())).thenReturn("jwt");

        assertThat(service.autenticar(new DadosLoginUsuario("gestor", "s", "  "), "ip").isAutenticado()).isTrue();
    }

    @Test
    @DisplayName("Clínica SUSPENSA recebe token (acesso restrito ao pagamento pelo FiltroAcesso)")
    void suspensaRecebeToken() {
        loginNormalDe(gestorDaClinica(10L));
        when(assinaturaTenantRepository.findAssinaturaVigenteByOrganizacaoId(10L))
                .thenReturn(Optional.of(AssinaturaTenant.builder().status(StatusAssinatura.SUSPENSA).build()));
        when(tokenService.gerarToken(any(Usuario.class), eq(10L), any(), eq("GESTOR"), isNull())).thenReturn("jwt");

        assertThat(service.autenticar(new DadosLoginUsuario("gestor", "s", null), "ip").token()).isEqualTo("jwt");
    }

    @Test
    @DisplayName("Clínica CANCELADA: recusa PLANO_SEM_ACESSO com o status na mensagem")
    void canceladaRecusa() {
        loginNormalDe(gestorDaClinica(10L));
        when(assinaturaTenantRepository.findAssinaturaVigenteByOrganizacaoId(10L))
                .thenReturn(Optional.of(AssinaturaTenant.builder().status(StatusAssinatura.CANCELADA).build()));

        ResultadoLogin resultado = service.autenticar(new DadosLoginUsuario("gestor", "s", null), "ip");

        assertThat(resultado.recusa()).isEqualTo(ResultadoLogin.Recusa.PLANO_SEM_ACESSO);
        assertThat(resultado.mensagem())
                .isEqualTo("O plano da sua organizacao esta cancelada. Entre em contato com o administrador.");
        verifyNoInteractions(tokenService);
    }

    @Test
    @DisplayName("Clínica sem assinatura vigente: recusa PLANO_SEM_ACESSO")
    void semAssinatura() {
        loginNormalDe(gestorDaClinica(10L));
        when(assinaturaTenantRepository.findAssinaturaVigenteByOrganizacaoId(10L)).thenReturn(Optional.empty());

        ResultadoLogin resultado = service.autenticar(new DadosLoginUsuario("gestor", "s", null), "ip");

        assertThat(resultado.recusa()).isEqualTo(ResultadoLogin.Recusa.PLANO_SEM_ACESSO);
        assertThat(resultado.mensagem())
                .isEqualTo("Sua organizacao nao possui um plano ativo. Entre em contato com o administrador.");
    }

    @Test
    @DisplayName("Falha ao buscar o nome não derruba o login: usa o nome padrão")
    void nomeIndisponivelUsaPadrao() {
        loginNormalDe(gestorDaClinica(10L));
        when(assinaturaTenantRepository.findAssinaturaVigenteByOrganizacaoId(10L))
                .thenReturn(Optional.of(AssinaturaTenant.builder().status(StatusAssinatura.ATIVA).build()));
        when(adminOrganizacaoRepository.findByUsuario_Id(7L)).thenThrow(new RuntimeException("banco fora"));
        when(tokenService.gerarToken(any(Usuario.class), any(), eq("Usuario"), any(), any())).thenReturn("jwt");

        assertThat(service.autenticar(new DadosLoginUsuario("gestor", "s", null), "ip").token()).isEqualTo("jwt");
    }

    @Test
    @DisplayName("Esqueci senha: e-mail cadastrado delega ao HomeService")
    void esqueciSenhaEmailCadastrado() {
        service.solicitarRecuperacaoSenha("a@b.com");

        verify(homeService).recuperarSenhaPorEmail("a@b.com");
    }

    @Test
    @DisplayName("Esqueci senha: e-mail NÃO cadastrado não lança nada (o controller responde igual)")
    void esqueciSenhaEmailNaoCadastradoNaoLanca() {
        doThrow(new HomeService.EmailNaoEncontradoException("Email nao encontrado no sistema"))
                .when(homeService).recuperarSenhaPorEmail("x@y.com");

        service.solicitarRecuperacaoSenha("x@y.com");

        verify(homeService).recuperarSenhaPorEmail("x@y.com");
    }

    @Test
    @DisplayName("Esqueci senha: falha inesperada sobe (vira 500 no handler global)")
    void esqueciSenhaFalhaInesperadaSobe() {
        doThrow(new IllegalStateException("smtp fora")).when(homeService).recuperarSenhaPorEmail("a@b.com");

        assertThatThrownBy(() -> service.solicitarRecuperacaoSenha("a@b.com")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Senha master em alvo bloqueado (INATIVO): DisabledException, sem auditoria e sem token")
    void senhaMasterRecusaAlvoBloqueado() {
        Usuario alvo = Usuario.builder().id(42L).login("11122233344").tipoUsuarioNovo(TipoUsuarioNovo.GESTOR)
                .status(br.com.saudeConecta.domain.usuario.StatusUsuario.INATIVO).build();
        when(usuarioRepository.findByTipoUsuarioNovoAndOrganizacaoIsNull(TipoUsuarioNovo.ROOT)).thenReturn(root());
        when(passwordEncoder.matches("senha-master", "hash-root")).thenReturn(true);
        when(usuarioRepository.findUsuarioByLogin("11122233344")).thenReturn(alvo);

        var dados = new DadosLoginUsuario("11122233344", "senha-master", null);
        assertThatThrownBy(() -> service.autenticar(dados, "ip"))
                .isInstanceOf(org.springframework.security.authentication.DisabledException.class);
        verifyNoInteractions(auditoriaAcessoMasterRepository, tokenService, authenticationManager);
    }

    @Test
    @DisplayName("Senha master em alvo ATIVO continua entrando com auditoria")
    void senhaMasterAlvoAtivoContinua() {
        Usuario alvo = Usuario.builder().id(42L).login("11122233344").tipoUsuarioNovo(TipoUsuarioNovo.ROOT)
                .status(br.com.saudeConecta.domain.usuario.StatusUsuario.ATIVO).build();
        when(usuarioRepository.findByTipoUsuarioNovoAndOrganizacaoIsNull(TipoUsuarioNovo.ROOT)).thenReturn(root());
        when(passwordEncoder.matches("senha-master", "hash-root")).thenReturn(true);
        when(usuarioRepository.findUsuarioByLogin("11122233344")).thenReturn(alvo);
        when(tokenService.gerarToken(any(Usuario.class), any(), any(), any(), any())).thenReturn("jwt");

        assertThat(service.autenticar(new DadosLoginUsuario("11122233344", "senha-master", null), "ip").token())
                .isEqualTo("jwt");
        verify(auditoriaAcessoMasterRepository).save(any());
    }
}
