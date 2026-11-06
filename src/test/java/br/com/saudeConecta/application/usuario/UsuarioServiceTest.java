package br.com.saudeConecta.application.usuario;

import br.com.saudeConecta.application.assinatura.LimitePlanoService;
import br.com.saudeConecta.application.cache.CacheEvictionService;
import br.com.saudeConecta.application.exception.LimitePlanoException;
import br.com.saudeConecta.config.tenant.TenantAccessDeniedException;
import br.com.saudeConecta.config.tenant.TenantContext;
import br.com.saudeConecta.config.tenant.TenantHelper;
import br.com.saudeConecta.config.tenant.TenantNotDefinedException;
import br.com.saudeConecta.domain.admin.AdminOrganizacao;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.domain.profissional.StatusProfissional;
import br.com.saudeConecta.domain.secretaria.Secretaria;
import br.com.saudeConecta.domain.secretaria.StatusSecretaria;
import br.com.saudeConecta.domain.usuario.MotivoBloqueio;
import br.com.saudeConecta.domain.usuario.StatusUsuario;
import br.com.saudeConecta.domain.usuario.TipoUsuarioNovo;
import br.com.saudeConecta.domain.usuario.Usuario;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import br.com.saudeConecta.application.usuario.dto.BloquearUsuarioRequest;
import br.com.saudeConecta.application.usuario.dto.TodosUsuariosAgrupadosResponse;
import br.com.saudeConecta.application.usuario.dto.UsuarioPerfilCompletoResponse;
import br.com.saudeConecta.infrastructure.persistence.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UsuarioService - Testes unitários")
class UsuarioServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private ProfissionalRepository profissionalRepository;
    @Mock private AdminOrganizacaoRepository adminOrganizacaoRepository;
    @Mock private PacienteRepository pacienteRepository;
    @Mock private SecretariaRepository secretariaRepository;
    @Mock private CacheEvictionService cacheEvictionService;
    @Mock private LimitePlanoService limitePlanoService;
    @Spy private TenantHelper tenantHelper = new TenantHelper();

    @InjectMocks
    private UsuarioService usuarioService;

    private static final Long ORG_ID = 1L;
    private static final Long USUARIO_ID = 10L;
    private static final Long PERFIL_ID = 20L;

    private Organizacao organizacao;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        organizacao = Organizacao.builder().nome("Clínica Teste").build();
        organizacao.setId(ORG_ID);

        usuario = Usuario.builder()
                .login("12345678900").senha("encodedPass")
                .tipoUsuarioNovo(TipoUsuarioNovo.CLINICO)
                .status(StatusUsuario.ATIVO).organizacao(organizacao)
                .build();
        usuario.setId(USUARIO_ID);
    }

    // ========== buscarPorId ==========

    @Nested
    @DisplayName("buscarPorId")
    class BuscarPorId {

        @Test
        @DisplayName("Deve retornar usuário quando ID existir")
        void deveRetornarUsuarioQuandoIdExistir() {
            when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));

            Optional<Usuario> resultado = usuarioService.buscarPorId(USUARIO_ID);

            assertThat(resultado).isPresent();
            assertThat(resultado.get().getId()).isEqualTo(USUARIO_ID);
        }

        @Test
        @DisplayName("Deve retornar vazio quando ID não existir")
        void deveRetornarVazioQuandoNaoExistir() {
            when(usuarioRepository.findById(999L)).thenReturn(Optional.empty());

            Optional<Usuario> resultado = usuarioService.buscarPorId(999L);

            assertThat(resultado).isEmpty();
        }
    }

    // ========== trocarSenharUsuariobyOrg ==========

    @Nested
    @DisplayName("trocarSenharUsuariobyOrg")
    class TrocarSenha {

        private static final Long GESTOR_ID = 30L;
        private static final Long OUTRA_ORG_ID = 2L;

        private Usuario criarUsuario(Long id, TipoUsuarioNovo tipo, Organizacao org) {
            Usuario u = Usuario.builder()
                    .login("login" + id).senha("encodedPass")
                    .tipoUsuarioNovo(tipo)
                    .status(StatusUsuario.ATIVO).organizacao(org)
                    .build();
            u.setId(id);
            return u;
        }

        /** Simula o que o TenantFilter faz a partir do JWT do usuário logado. */
        private void logarComo(Long usuarioId, Long organizacaoId) {
            TenantContext.setCurrentUser(usuarioId);
            TenantContext.setCurrentTenant(organizacaoId);
        }

        @AfterEach
        void limparTenantContext() {
            TenantContext.clear();
        }

        @Test
        @DisplayName("Usuário comum troca a própria senha")
        void deveTrocarPropriaSenha() {
            logarComo(USUARIO_ID, ORG_ID);
            when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
            when(passwordEncoder.encode("novaSenha123")).thenReturn("encodedNovaSenha");

            usuarioService.trocarSenharUsuariobyOrg(USUARIO_ID, "novaSenha123");

            assertThat(usuario.getSenha()).isEqualTo("encodedNovaSenha");
            verify(usuarioRepository).save(usuario);
            verify(cacheEvictionService).evictPerfilUsuario(USUARIO_ID);
        }

        @Test
        @DisplayName("Gestor troca a senha de um usuário da própria organização")
        void gestorDeveTrocarSenhaDeUsuarioDaMesmaOrganizacao() {
            Usuario gestor = criarUsuario(GESTOR_ID, TipoUsuarioNovo.GESTOR, organizacao);
            logarComo(GESTOR_ID, ORG_ID);
            when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
            when(usuarioRepository.findById(GESTOR_ID)).thenReturn(Optional.of(gestor));
            when(passwordEncoder.encode("novaSenha123")).thenReturn("encodedNovaSenha");

            usuarioService.trocarSenharUsuariobyOrg(USUARIO_ID, "novaSenha123");

            assertThat(usuario.getSenha()).isEqualTo("encodedNovaSenha");
            verify(usuarioRepository).save(usuario);
        }

        @Test
        @DisplayName("Gestor NÃO troca a senha de usuário de outra organização")
        void gestorNaoDeveTrocarSenhaDeOutraOrganizacao() {
            Organizacao outraOrg = Organizacao.builder().nome("Outra Clínica").build();
            outraOrg.setId(OUTRA_ORG_ID);
            Usuario gestorOutraOrg = criarUsuario(GESTOR_ID, TipoUsuarioNovo.GESTOR, outraOrg);
            logarComo(GESTOR_ID, OUTRA_ORG_ID);
            when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
            when(usuarioRepository.findById(GESTOR_ID)).thenReturn(Optional.of(gestorOutraOrg));

            assertThatThrownBy(() -> usuarioService.trocarSenharUsuariobyOrg(USUARIO_ID, "novaSenha123"))
                    .isInstanceOf(TenantAccessDeniedException.class);
            verify(usuarioRepository, never()).save(any());
            assertThat(usuario.getSenha()).isEqualTo("encodedPass");
        }

        @Test
        @DisplayName("Clínico NÃO troca a senha de outro usuário, mesmo da mesma organização")
        void clinicoNaoDeveTrocarSenhaDeOutroUsuario() {
            Usuario outroClinico = criarUsuario(40L, TipoUsuarioNovo.CLINICO, organizacao);
            logarComo(40L, ORG_ID);
            when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
            when(usuarioRepository.findById(40L)).thenReturn(Optional.of(outroClinico));

            assertThatThrownBy(() -> usuarioService.trocarSenharUsuariobyOrg(USUARIO_ID, "novaSenha123"))
                    .isInstanceOf(TenantAccessDeniedException.class);
            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("Assistente NÃO troca a senha de outro usuário, mesmo da mesma organização")
        void assistenteNaoDeveTrocarSenhaDeOutroUsuario() {
            Usuario assistente = criarUsuario(50L, TipoUsuarioNovo.ASSISTENTE, organizacao);
            logarComo(50L, ORG_ID);
            when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
            when(usuarioRepository.findById(50L)).thenReturn(Optional.of(assistente));

            assertThatThrownBy(() -> usuarioService.trocarSenharUsuariobyOrg(USUARIO_ID, "novaSenha123"))
                    .isInstanceOf(TenantAccessDeniedException.class);
            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("Gestor NÃO troca a senha do ROOT, mesmo que esteja na mesma organização")
        void gestorNaoDeveTrocarSenhaDoRoot() {
            Usuario root = criarUsuario(1L, TipoUsuarioNovo.ROOT, organizacao);
            logarComo(GESTOR_ID, ORG_ID);
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(root));

            assertThatThrownBy(() -> usuarioService.trocarSenharUsuariobyOrg(1L, "novaSenha123"))
                    .isInstanceOf(TenantAccessDeniedException.class);
            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("ROOT troca a própria senha")
        void rootDeveTrocarPropriaSenha() {
            Usuario root = criarUsuario(1L, TipoUsuarioNovo.ROOT, null);
            TenantContext.setCurrentUser(1L);
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(root));
            when(passwordEncoder.encode("novaSenha123")).thenReturn("encodedNovaSenha");

            usuarioService.trocarSenharUsuariobyOrg(1L, "novaSenha123");

            assertThat(root.getSenha()).isEqualTo("encodedNovaSenha");
            verify(usuarioRepository).save(root);
        }

        @Test
        @DisplayName("ROOT logado direto NÃO troca a senha de outro usuário (usa a senha master como o admin)")
        void rootNaoDeveTrocarSenhaDeOutroUsuarioDiretamente() {
            Usuario root = criarUsuario(1L, TipoUsuarioNovo.ROOT, null);
            TenantContext.setCurrentUser(1L);
            when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(root));

            assertThatThrownBy(() -> usuarioService.trocarSenharUsuariobyOrg(USUARIO_ID, "novaSenha123"))
                    .isInstanceOf(TenantAccessDeniedException.class);
            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("Gestor com token sem organização NÃO troca a senha de outro usuário")
        void gestorSemOrganizacaoNoTokenNaoDeveTrocarSenha() {
            Usuario gestor = criarUsuario(GESTOR_ID, TipoUsuarioNovo.GESTOR, organizacao);
            TenantContext.setCurrentUser(GESTOR_ID);
            when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
            when(usuarioRepository.findById(GESTOR_ID)).thenReturn(Optional.of(gestor));

            assertThatThrownBy(() -> usuarioService.trocarSenharUsuariobyOrg(USUARIO_ID, "novaSenha123"))
                    .isInstanceOf(TenantAccessDeniedException.class);
            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("Gestor NÃO troca a senha de usuário sem organização")
        void gestorNaoDeveTrocarSenhaDeUsuarioSemOrganizacao() {
            Usuario semOrg = criarUsuario(60L, TipoUsuarioNovo.CLINICO, null);
            Usuario gestor = criarUsuario(GESTOR_ID, TipoUsuarioNovo.GESTOR, organizacao);
            logarComo(GESTOR_ID, ORG_ID);
            when(usuarioRepository.findById(60L)).thenReturn(Optional.of(semOrg));
            when(usuarioRepository.findById(GESTOR_ID)).thenReturn(Optional.of(gestor));

            assertThatThrownBy(() -> usuarioService.trocarSenharUsuariobyOrg(60L, "novaSenha123"))
                    .isInstanceOf(TenantAccessDeniedException.class);
            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("Nega a troca quando não há usuário logado no contexto")
        void deveNegarSemUsuarioLogado() {
            when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));

            assertThatThrownBy(() -> usuarioService.trocarSenharUsuariobyOrg(USUARIO_ID, "novaSenha123"))
                    .isInstanceOf(TenantAccessDeniedException.class);
            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("Nega a troca quando o usuário logado não existe mais no banco")
        void deveNegarQuandoUsuarioLogadoNaoExiste() {
            logarComo(GESTOR_ID, ORG_ID);
            when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
            when(usuarioRepository.findById(GESTOR_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> usuarioService.trocarSenharUsuariobyOrg(USUARIO_ID, "novaSenha123"))
                    .isInstanceOf(TenantAccessDeniedException.class);
            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando usuário não for encontrado")
        void deveLancarExcecaoQuandoUsuarioNaoEncontrado() {
            logarComo(GESTOR_ID, ORG_ID);
            when(usuarioRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> usuarioService.trocarSenharUsuariobyOrg(999L, "novaSenha"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("usuario nao encontrado");
        }
    }

    // ========== bloquearUsuariobyOrg - bloqueio individual ==========

    @Nested
    @DisplayName("bloquearUsuariobyOrg - bloqueio individual")
    class BloquearUsuarioBloqueioIndividual {

        @AfterEach
        void limparTenantContext() {
            TenantContext.clear();
        }

        @Test
        @DisplayName("Deve bloquear (INATIVO) profissional com sucesso como Admin Org")
        void deveBloquerProfissionalComoAdminOrg() {
            TenantContext.setCurrentTenant(ORG_ID);
            BloquearUsuarioRequest request = new BloquearUsuarioRequest(PERFIL_ID, USUARIO_ID, 0);

            when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
            when(profissionalRepository.findByIdAndOrganizacao_Id(PERFIL_ID, ORG_ID))
                    .thenReturn(Optional.of(mock(Profissional.class)));

            usuarioService.bloquearUsuariobyOrg(request);

            assertThat(usuario.getStatus()).isEqualTo(StatusUsuario.INATIVO);
            verify(usuarioRepository).save(usuario);
        }

        @Test
        @DisplayName("Deve desbloquear (ATIVO) usuário com sucesso")
        void deveDesbloquearUsuarioComSucesso() {
            TenantContext.setCurrentTenant(ORG_ID);
            usuario.setStatus(StatusUsuario.INATIVO);
            BloquearUsuarioRequest request = new BloquearUsuarioRequest(PERFIL_ID, USUARIO_ID, 1);

            when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
            when(profissionalRepository.findByIdAndOrganizacao_Id(PERFIL_ID, ORG_ID))
                    .thenReturn(Optional.of(mock(Profissional.class)));

            usuarioService.bloquearUsuariobyOrg(request);

            assertThat(usuario.getStatus()).isEqualTo(StatusUsuario.ATIVO);
        }

        @Test
        @DisplayName("4.4 - Reativar usuário valida o limite do plano; estourado, não reativa")
        void reativarDeveValidarLimiteDoPlano() {
            TenantContext.setCurrentTenant(ORG_ID);
            usuario.setStatus(StatusUsuario.INATIVO);
            usuario.setMotivoBloqueio(MotivoBloqueio.INDIVIDUAL);
            BloquearUsuarioRequest request = new BloquearUsuarioRequest(PERFIL_ID, USUARIO_ID, 1);

            when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
            doThrow(new LimitePlanoException("PROFISSIONAL", 3, 3L))
                    .when(limitePlanoService).validarLimiteProfissional(ORG_ID);

            assertThatThrownBy(() -> usuarioService.bloquearUsuariobyOrg(request))
                    .isInstanceOf(LimitePlanoException.class);

            assertThat(usuario.getStatus()).isEqualTo(StatusUsuario.INATIVO);
            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("4.4 - Reativação valida o limite do tipo certo (secretária e gestor)")
        void reativarValidaLimiteConformeTipo() {
            TenantContext.setCurrentTenant(ORG_ID);
            usuario.setStatus(StatusUsuario.INATIVO);
            usuario.setTipoUsuarioNovo(TipoUsuarioNovo.ASSISTENTE);
            when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));

            usuarioService.bloquearUsuariobyOrg(new BloquearUsuarioRequest(PERFIL_ID, USUARIO_ID, 1));

            verify(limitePlanoService).validarLimiteSecretaria(ORG_ID);
            assertThat(usuario.getStatus()).isEqualTo(StatusUsuario.ATIVO);
            assertThat(usuario.getMotivoBloqueio()).isNull();
        }

        @Test
        @DisplayName("Usuário já ativo recebendo status ATIVO não consome limite")
        void ativoParaAtivoNaoValidaLimite() {
            TenantContext.setCurrentTenant(ORG_ID);
            when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));

            usuarioService.bloquearUsuariobyOrg(new BloquearUsuarioRequest(PERFIL_ID, USUARIO_ID, 1));

            verifyNoInteractions(limitePlanoService);
        }

        @Test
        @DisplayName("4.4 - Bloqueio individual marca o motivo INDIVIDUAL")
        void bloqueioIndividualMarcaMotivo() {
            TenantContext.setCurrentTenant(ORG_ID);
            when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));

            usuarioService.bloquearUsuariobyOrg(new BloquearUsuarioRequest(PERFIL_ID, USUARIO_ID, 0));

            assertThat(usuario.getMotivoBloqueio()).isEqualTo(MotivoBloqueio.INDIVIDUAL);
        }

        @Test
        @DisplayName("Deve lançar exceção ao tentar bloquear Super Admin")
        void deveLancarExcecaoAoBloquearSuperAdmin() {
            TenantContext.setCurrentTenant(ORG_ID);
            // SuperAdmin precisa pertencer à mesma org para passar o filter e chegar na validação
            Usuario superAdmin = Usuario.builder()
                    .login("superadmin")
                    .tipoUsuarioNovo(TipoUsuarioNovo.ROOT)
                    .status(StatusUsuario.ATIVO)
                    .organizacao(organizacao)
                    .build();
            superAdmin.setId(USUARIO_ID);

            BloquearUsuarioRequest request = new BloquearUsuarioRequest(PERFIL_ID, USUARIO_ID, 0);

            when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(superAdmin));

            assertThatThrownBy(() -> usuarioService.bloquearUsuariobyOrg(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Root");
        }

        @Test
        @DisplayName("Deve lançar exceção quando usuário não for encontrado (Admin Org)")
        void deveLancarExcecaoQuandoUsuarioNaoEncontradoAdminOrg() {
            TenantContext.setCurrentTenant(ORG_ID);
            BloquearUsuarioRequest request = new BloquearUsuarioRequest(PERFIL_ID, 999L, 0);

            when(usuarioRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> usuarioService.bloquearUsuariobyOrg(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("nao encontrado");
        }
    }

    // ========== bloquearUsuariobyOrg - bloqueio em cascata (SUPER_ADMIN) ==========

    @Nested
    @DisplayName("bloquearUsuariobyOrg - cascata SUPER_ADMIN")
    class BloquearUsuarioCascata {

        @AfterEach
        void limparTenantContext() {
            TenantContext.clear();
            SecurityContextHolder.clearContext();
        }

        @Test
        @DisplayName("Sem tenant e sem ROLE_SUPER_ADMIN deve negar (403) em vez de agir como super admin")
        void deveNegarSemTenantESemPapelSuperAdmin() {
            TenantContext.clear();
            SecurityContextHolder.clearContext();
            BloquearUsuarioRequest request = new BloquearUsuarioRequest(PERFIL_ID, USUARIO_ID, 0);

            assertThatThrownBy(() -> usuarioService.bloquearUsuariobyOrg(request))
                    .isInstanceOf(TenantNotDefinedException.class);
            verifyNoInteractions(usuarioRepository);
        }

        @Test
        @DisplayName("Deve bloquear todos os usuários da organização em cascata")
        void deveBloquearTodosUsuariosEmCascata() {
            // SUPER_ADMIN: sem tenant no contexto, mas com o papel explícito
            TenantContext.clear();
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                    "root", "n/a", List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))));

            Usuario adminOrgUsuario = Usuario.builder()
                    .login("cnpj")
                    .tipoUsuarioNovo(TipoUsuarioNovo.GESTOR)
                    .status(StatusUsuario.ATIVO).organizacao(organizacao)
                    .build();
            adminOrgUsuario.setId(USUARIO_ID);

            Usuario outroUsuario = Usuario.builder()
                    .login("outro")
                    .tipoUsuarioNovo(TipoUsuarioNovo.CLINICO)
                    .status(StatusUsuario.ATIVO).organizacao(organizacao)
                    .build();
            outroUsuario.setId(99L);

            BloquearUsuarioRequest request = new BloquearUsuarioRequest(PERFIL_ID, USUARIO_ID, 0);

            when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(adminOrgUsuario));
            when(adminOrganizacaoRepository.findById(PERFIL_ID)).thenReturn(Optional.of(mock(AdminOrganizacao.class)));
            when(usuarioRepository.findByOrganizacao_Id(ORG_ID)).thenReturn(List.of(outroUsuario));

            usuarioService.bloquearUsuariobyOrg(request);

            assertThat(adminOrgUsuario.getStatus()).isEqualTo(StatusUsuario.INATIVO);
            assertThat(outroUsuario.getStatus()).isEqualTo(StatusUsuario.INATIVO);
            verify(usuarioRepository, atLeast(2)).save(any(Usuario.class));
        }
    }

    @Nested
    @DisplayName("4.4 - cascata preserva bloqueios individuais")
    class CascataPreservaBloqueioIndividual {

        private Usuario adminOrgUsuario;

        @BeforeEach
        void comoSuperAdmin() {
            TenantContext.clear();
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                    "root", "n/a", List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))));
            adminOrgUsuario = Usuario.builder().login("gestor").tipoUsuarioNovo(TipoUsuarioNovo.GESTOR)
                    .status(StatusUsuario.ATIVO).organizacao(organizacao).build();
            adminOrgUsuario.setId(USUARIO_ID);
            when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(adminOrgUsuario));
            when(adminOrganizacaoRepository.findById(PERFIL_ID)).thenReturn(Optional.empty());
        }

        @AfterEach
        void limpar() {
            SecurityContextHolder.clearContext();
            TenantContext.clear();
        }

        private Usuario usuarioDaOrg(long id, StatusUsuario status, MotivoBloqueio motivo) {
            Usuario u = Usuario.builder().login("u" + id).tipoUsuarioNovo(TipoUsuarioNovo.CLINICO)
                    .status(status).motivoBloqueio(motivo).organizacao(organizacao).build();
            u.setId(id);
            return u;
        }

        @Test
        @DisplayName("Bloqueio em cascata marca CASCATA só em quem estava ativo")
        void bloqueioCascataNaoSobrescreveIndividual() {
            Usuario ativo = usuarioDaOrg(50L, StatusUsuario.ATIVO, null);
            Usuario jaBloqueado = usuarioDaOrg(51L, StatusUsuario.INATIVO, MotivoBloqueio.INDIVIDUAL);
            when(usuarioRepository.findByOrganizacao_Id(ORG_ID)).thenReturn(List.of(ativo, jaBloqueado));

            usuarioService.bloquearUsuariobyOrg(new BloquearUsuarioRequest(PERFIL_ID, USUARIO_ID, 0));

            assertThat(ativo.getStatus()).isEqualTo(StatusUsuario.INATIVO);
            assertThat(ativo.getMotivoBloqueio()).isEqualTo(MotivoBloqueio.CASCATA);
            assertThat(jaBloqueado.getMotivoBloqueio()).isEqualTo(MotivoBloqueio.INDIVIDUAL);
        }

        @Test
        @DisplayName("Desbloqueio em cascata reativa só quem foi bloqueado pela cascata")
        void desbloqueioCascataSoReativaCascata() {
            adminOrgUsuario.setStatus(StatusUsuario.INATIVO);
            adminOrgUsuario.setMotivoBloqueio(MotivoBloqueio.CASCATA);
            Usuario daCascata = usuarioDaOrg(50L, StatusUsuario.INATIVO, MotivoBloqueio.CASCATA);
            Usuario individual = usuarioDaOrg(51L, StatusUsuario.INATIVO, MotivoBloqueio.INDIVIDUAL);
            Usuario legado = usuarioDaOrg(52L, StatusUsuario.INATIVO, null);
            when(usuarioRepository.findByOrganizacao_Id(ORG_ID)).thenReturn(List.of(daCascata, individual, legado));

            usuarioService.bloquearUsuariobyOrg(new BloquearUsuarioRequest(PERFIL_ID, USUARIO_ID, 1));

            assertThat(adminOrgUsuario.getStatus()).isEqualTo(StatusUsuario.ATIVO);
            assertThat(daCascata.getStatus()).isEqualTo(StatusUsuario.ATIVO);
            assertThat(daCascata.getMotivoBloqueio()).isNull();
            assertThat(individual.getStatus()).isEqualTo(StatusUsuario.INATIVO);
            assertThat(legado.getStatus()).isEqualTo(StatusUsuario.INATIVO);
        }
    }

    // ========== buscarTodosAgrupados ==========

    @Nested
    @DisplayName("buscarTodosAgrupados")
    class BuscarTodosAgrupados {

        @Test
        @DisplayName("Deve retornar usuários agrupados por organização")
        void deveRetornarUsuariosAgrupadosPorOrg() {
            when(pacienteRepository.findByOrganizacao_Id(ORG_ID)).thenReturn(Collections.emptyList());
            when(profissionalRepository.findByOrganizacao_IdParaAgrupamento(ORG_ID)).thenReturn(Collections.emptyList());
            when(secretariaRepository.findByOrganizacao_IdWithUsuario(ORG_ID)).thenReturn(Collections.emptyList());
            when(adminOrganizacaoRepository.findByOrganizacao_IdWithUsuario(ORG_ID)).thenReturn(Collections.emptyList());

            TodosUsuariosAgrupadosResponse resultado = usuarioService.buscarTodosAgrupados(ORG_ID);

            assertThat(resultado).isNotNull();
            assertThat(resultado.paciente()).isEmpty();
            assertThat(resultado.clinico()).isEmpty();
            assertThat(resultado.secretaria()).isEmpty();
            assertThat(resultado.administrador()).isEmpty();
        }
    }

    // ========== buscarPerfilCompleto ==========

    @Nested
    @DisplayName("buscarPerfilCompleto")
    class BuscarPerfilCompleto {

        @Test
        @DisplayName("Deve retornar vazio quando usuário não existir")
        void deveRetornarVazioQuandoUsuarioNaoExistir() {
            when(usuarioRepository.findByIdWithOrganizacao(999L)).thenReturn(Optional.empty());

            Optional<UsuarioPerfilCompletoResponse> resultado = usuarioService.buscarPerfilCompleto(999L);

            assertThat(resultado).isEmpty();
        }

        @Test
        @DisplayName("Deve retornar vazio quando tipoUsuarioNovo for nulo")
        void deveRetornarVazioQuandoTipoNulo() {
            Usuario usuarioSemTipo = Usuario.builder()
                    .login("sem-tipo")
                    .tipoUsuarioNovo(null).status(StatusUsuario.ATIVO)
                    .build();
            usuarioSemTipo.setId(USUARIO_ID);

            when(usuarioRepository.findByIdWithOrganizacao(USUARIO_ID)).thenReturn(Optional.of(usuarioSemTipo));

            Optional<UsuarioPerfilCompletoResponse> resultado = usuarioService.buscarPerfilCompleto(USUARIO_ID);

            assertThat(resultado).isEmpty();
        }

        @Test
        @DisplayName("Deve retornar perfil de profissional corretamente")
        void deveRetornarPerfilProfissional() {
            when(usuarioRepository.findByIdWithOrganizacao(USUARIO_ID)).thenReturn(Optional.of(usuario));
            when(profissionalRepository.findByUsuarioIdWithRelations(USUARIO_ID)).thenReturn(Optional.empty());

            Optional<UsuarioPerfilCompletoResponse> resultado = usuarioService.buscarPerfilCompleto(USUARIO_ID);

            assertThat(resultado).isPresent();
        }

        @Test
        @DisplayName("Deve retornar perfil de secretaria corretamente")
        void deveRetornarPerfilSecretaria() {
            usuario = Usuario.builder()
                    .login("sec")
                    .tipoUsuarioNovo(TipoUsuarioNovo.ASSISTENTE)
                    .status(StatusUsuario.ATIVO).organizacao(organizacao)
                    .build();
            usuario.setId(USUARIO_ID);

            when(usuarioRepository.findByIdWithOrganizacao(USUARIO_ID)).thenReturn(Optional.of(usuario));
            when(secretariaRepository.findByUsuario_Id(USUARIO_ID)).thenReturn(Optional.empty());

            Optional<UsuarioPerfilCompletoResponse> resultado = usuarioService.buscarPerfilCompleto(USUARIO_ID);

            assertThat(resultado).isPresent();
        }
    }
}
