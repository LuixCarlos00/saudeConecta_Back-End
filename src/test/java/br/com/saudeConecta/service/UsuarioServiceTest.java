package br.com.saudeConecta.service;

import br.com.saudeConecta.domain.admin.AdminOrganizacao;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.domain.profissional.StatusProfissional;
import br.com.saudeConecta.domain.secretaria.Secretaria;
import br.com.saudeConecta.domain.secretaria.StatusSecretaria;
import br.com.saudeConecta.domain.usuario.StatusUsuario;
import br.com.saudeConecta.domain.usuario.TipoUsuarioNovo;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.infra.tenant.TenantContext;
import br.com.saudeConecta.infrastructure.persistence.repository.*;
import br.com.saudeConecta.presentation.dto.usuario.BloquearUsuarioRequest;
import br.com.saudeConecta.presentation.dto.usuario.TodosUsuariosAgrupadosResponse;
import br.com.saudeConecta.presentation.dto.usuario.UsuarioPerfilCompletoResponse;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
                .tipoUsuario((byte) 3).tipoUsuarioNovo(TipoUsuarioNovo.PROFISSIONAL)
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

        @Test
        @DisplayName("Deve trocar senha com sucesso")
        void deveTrocarSenhaComSucesso() {
            when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
            when(passwordEncoder.encode("novaSenha123")).thenReturn("encodedNovaSenha");

            usuarioService.trocarSenharUsuariobyOrg(USUARIO_ID, "novaSenha123");

            assertThat(usuario.getSenha()).isEqualTo("encodedNovaSenha");
            verify(usuarioRepository).save(usuario);
        }

        @Test
        @DisplayName("Deve lançar exceção quando usuário não for encontrado")
        void deveLancarExcecaoQuandoUsuarioNaoEncontrado() {
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
        @DisplayName("Deve lançar exceção ao tentar bloquear Super Admin")
        void deveLancarExcecaoAoBloquearSuperAdmin() {
            TenantContext.setCurrentTenant(ORG_ID);
            // SuperAdmin precisa pertencer à mesma org para passar o filter e chegar na validação
            Usuario superAdmin = Usuario.builder()
                    .login("superadmin").tipoUsuario((byte) 0)
                    .tipoUsuarioNovo(TipoUsuarioNovo.SUPER_ADMIN)
                    .status(StatusUsuario.ATIVO)
                    .organizacao(organizacao)
                    .build();
            superAdmin.setId(USUARIO_ID);

            BloquearUsuarioRequest request = new BloquearUsuarioRequest(PERFIL_ID, USUARIO_ID, 0);

            when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(superAdmin));

            assertThatThrownBy(() -> usuarioService.bloquearUsuariobyOrg(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Super Admin");
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
        }

        @Test
        @DisplayName("Deve bloquear todos os usuários da organização em cascata")
        void deveBloquearTodosUsuariosEmCascata() {
            // SUPER_ADMIN: TenantContext sem tenant definido (null)
            TenantContext.clear();

            Usuario adminOrgUsuario = Usuario.builder()
                    .login("cnpj").tipoUsuario((byte) 1)
                    .tipoUsuarioNovo(TipoUsuarioNovo.ADMIN_ORG)
                    .status(StatusUsuario.ATIVO).organizacao(organizacao)
                    .build();
            adminOrgUsuario.setId(USUARIO_ID);

            Usuario outroUsuario = Usuario.builder()
                    .login("outro").tipoUsuario((byte) 3)
                    .tipoUsuarioNovo(TipoUsuarioNovo.PROFISSIONAL)
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
                    .login("sem-tipo").tipoUsuario((byte) 3)
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
                    .login("sec").tipoUsuario((byte) 2)
                    .tipoUsuarioNovo(TipoUsuarioNovo.RECEPCIONISTA)
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
