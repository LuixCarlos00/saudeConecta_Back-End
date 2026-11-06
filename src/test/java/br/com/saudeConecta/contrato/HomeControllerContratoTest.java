package br.com.saudeConecta.contrato;

import br.com.saudeConecta.application.usuario.HomeService;
import br.com.saudeConecta.domain.admin.AdminOrganizacao;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.planos.AssinaturaTenant;
import br.com.saudeConecta.domain.planos.StatusAssinatura;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.domain.profissional.TipoProfissional;
import br.com.saudeConecta.domain.secretaria.Secretaria;
import br.com.saudeConecta.domain.usuario.AuditoriaAcessoMaster;
import br.com.saudeConecta.domain.usuario.StatusUsuario;
import br.com.saudeConecta.domain.usuario.TipoUsuarioNovo;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.infrastructure.persistence.repository.AdminOrganizacaoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.AssinaturaTenantRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.AuditoriaAcessoMasterRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.ProfissionalRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.SecretariaRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.UsuarioRepository;
import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contrato HTTP de {@code /Home} (login e "esqueci minha senha"): status e JSON de cada caminho.
 *
 * <p>Rede de segurança da fase B6, escrita contra o código anterior à extração da regra de login
 * para {@code application/auth}. O {@code AuthenticationManager}, o {@code TokenService} e o
 * {@code PasswordEncoder} são os reais; só o acesso a dados é simulado. Assim o teste continua
 * válido qualquer que seja a classe que guarda a regra.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Contrato HTTP - /Home (login, senha master, plano e recuperação de senha)")
class HomeControllerContratoTest {

    private static final Long ORG_ID = 10L;
    private static final String SENHA_MASTER = "senha-master-root";
    private static final String SENHA_GESTOR = "senha-do-gestor";
    private static final String MENSAGEM_RECUPERACAO =
            "Se o e-mail estiver cadastrado, voce recebera as instrucoes para recuperar a senha.";

    @Autowired private MockMvc mockMvc;
    @Autowired private PasswordEncoder passwordEncoder;

    @MockBean private UsuarioRepository usuarioRepository;
    @MockBean private AssinaturaTenantRepository assinaturaTenantRepository;
    @MockBean private AdminOrganizacaoRepository adminOrganizacaoRepository;
    @MockBean private ProfissionalRepository profissionalRepository;
    @MockBean private SecretariaRepository secretariaRepository;
    @MockBean private AuditoriaAcessoMasterRepository auditoriaAcessoMasterRepository;
    @MockBean private HomeService homeService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Usuario root;
    private Usuario gestor;

    @BeforeEach
    void setUp() {
        Organizacao organizacao = new Organizacao();
        organizacao.setId(ORG_ID);

        root = Usuario.builder().id(1L).login("root").senha(passwordEncoder.encode(SENHA_MASTER))
                .tipoUsuarioNovo(TipoUsuarioNovo.ROOT).status(StatusUsuario.ATIVO).build();
        gestor = Usuario.builder().id(42L).login("11122233344").senha(passwordEncoder.encode(SENHA_GESTOR))
                .tipoUsuarioNovo(TipoUsuarioNovo.GESTOR).status(StatusUsuario.ATIVO)
                .organizacao(organizacao).build();

        when(usuarioRepository.findByTipoUsuarioNovoAndOrganizacaoIsNull(TipoUsuarioNovo.ROOT)).thenReturn(root);
        when(usuarioRepository.findByLogin("root")).thenReturn(root);
        when(usuarioRepository.findByLogin("11122233344")).thenReturn(gestor);
        when(usuarioRepository.findUsuarioByLogin("11122233344")).thenReturn(gestor);
        when(adminOrganizacaoRepository.findByUsuario_Id(42L))
                .thenReturn(Optional.of(AdminOrganizacao.builder().nome("Ana Gestora").build()));
        assinaturaCom(StatusAssinatura.ATIVA);
    }

    private void assinaturaCom(StatusAssinatura status) {
        when(assinaturaTenantRepository.findAssinaturaVigenteByOrganizacaoId(ORG_ID))
                .thenReturn(Optional.of(AssinaturaTenant.builder().status(status).build()));
    }

    private ResultActions login(String login, String senha, String perfil) throws Exception {
        Map<String, String> corpo = perfil == null
                ? Map.of("login", login, "senha", senha)
                : Map.of("login", login, "senha", senha, "perfil", perfil);
        return mockMvc.perform(post("/Home/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(corpo)));
    }

    private DecodedJWT tokenDaResposta(ResultActions resposta) throws Exception {
        String json = resposta.andReturn().getResponse().getContentAsString();
        return JWT.decode(objectMapper.readTree(json).get("token").asText());
    }

    @Nested
    @DisplayName("POST /Home/login - fluxo normal")
    class LoginNormal {

        @Test
        @DisplayName("Gestor com plano ATIVA: 200 com token (sub, id, organização, nome e perfil) e sem auditoria master")
        void gestorComPlanoAtivo() throws Exception {
            ResultActions resposta = login("11122233344", SENHA_GESTOR, "GESTOR")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value(not(emptyOrNullString())));

            DecodedJWT jwt = tokenDaResposta(resposta);
            assertThat(jwt.getSubject()).isEqualTo("11122233344");
            assertThat(jwt.getClaim("id").asLong()).isEqualTo(42L);
            assertThat(jwt.getClaim("organizacaoId").asLong()).isEqualTo(ORG_ID);
            assertThat(jwt.getClaim("nome").asString()).isEqualTo("Ana Gestora");
            assertThat(jwt.getClaim("perfil").asString()).isEqualTo("GESTOR");
            assertThat(jwt.getClaim("tipoProfissional").isNull()).isTrue();
            verify(auditoriaAcessoMasterRepository, never()).save(any());
        }

        @Test
        @DisplayName("Clínico médico: token leva o nome do profissional e tipoProfissional=MEDICO")
        void clinicoMedico() throws Exception {
            Usuario medico = Usuario.builder().id(50L).login("medico").senha(passwordEncoder.encode("s"))
                    .tipoUsuarioNovo(TipoUsuarioNovo.CLINICO).status(StatusUsuario.ATIVO)
                    .organizacao(gestor.getOrganizacao()).build();
            Profissional profissional = new Profissional();
            profissional.setNome("Dr. Bruno");
            TipoProfissional tipo = new TipoProfissional();
            tipo.setCodigo("MEDICO");
            profissional.setTipoProfissional(tipo);
            when(usuarioRepository.findByLogin("medico")).thenReturn(medico);
            when(profissionalRepository.findByUsuario_Id(50L)).thenReturn(Optional.of(profissional));
            when(profissionalRepository.findByUsuarioIdWithRelations(50L)).thenReturn(Optional.of(profissional));

            DecodedJWT jwt = tokenDaResposta(login("medico", "s", null).andExpect(status().isOk()));

            assertThat(jwt.getClaim("nome").asString()).isEqualTo("Dr. Bruno");
            assertThat(jwt.getClaim("perfil").asString()).isEqualTo("CLINICO");
            assertThat(jwt.getClaim("tipoProfissional").asString()).isEqualTo("MEDICO");
        }

        @Test
        @DisplayName("Assistente: token leva o nome da secretária")
        void assistente() throws Exception {
            Usuario assistente = Usuario.builder().id(60L).login("recepcao").senha(passwordEncoder.encode("s"))
                    .tipoUsuarioNovo(TipoUsuarioNovo.ASSISTENTE).status(StatusUsuario.ATIVO)
                    .organizacao(gestor.getOrganizacao()).build();
            Secretaria secretaria = new Secretaria();
            secretaria.setNome("Carla");
            when(usuarioRepository.findByLogin("recepcao")).thenReturn(assistente);
            when(secretariaRepository.findByUsuario_Id(60L)).thenReturn(Optional.of(secretaria));

            DecodedJWT jwt = tokenDaResposta(login("recepcao", "s", null).andExpect(status().isOk()));

            assertThat(jwt.getClaim("nome").asString()).isEqualTo("Carla");
            assertThat(jwt.getClaim("perfil").asString()).isEqualTo("ASSISTENTE");
        }

        @Test
        @DisplayName("Senha errada: 401 NAO_AUTENTICADO 'Credenciais invalidas'")
        void senhaErrada() throws Exception {
            login("11122233344", "errada", null)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("NAO_AUTENTICADO"))
                    .andExpect(jsonPath("$.message").value("Credenciais invalidas"));
        }

        @Test
        @DisplayName("Usuário bloqueado (INATIVO) com a senha certa: 401 'Usuario bloqueado ou inativo'")
        void usuarioBloqueado() throws Exception {
            gestor.setStatus(StatusUsuario.INATIVO);

            login("11122233344", SENHA_GESTOR, null)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("NAO_AUTENTICADO"))
                    .andExpect(jsonPath("$.message").value("Usuario bloqueado ou inativo"))
                    .andExpect(jsonPath("$.error").value("Usuario bloqueado ou inativo"));
        }

        @Test
        @DisplayName("Perfil selecionado diferente do tipo do usuário: 403 com mensagem")
        void perfilDivergente() throws Exception {
            login("11122233344", SENHA_GESTOR, "CLINICO")
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message")
                            .value("O tipo selecionado não corresponde ao tipo do usuário no sistema"));
        }

        @Test
        @DisplayName("Perfil inexistente: 400 'Tipo inválido'")
        void perfilInvalido() throws Exception {
            login("11122233344", SENHA_GESTOR, "PIRATA")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Tipo inválido"));
        }
    }

    @Nested
    @DisplayName("POST /Home/login - situação do plano da clínica")
    class PlanoDaClinica {

        @Test
        @DisplayName("SUSPENSA: 200 com token (o FiltroAcesso limita o token às rotas de pagamento)")
        void suspensaRecebeToken() throws Exception {
            assinaturaCom(StatusAssinatura.SUSPENSA);

            login("11122233344", SENHA_GESTOR, null)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value(not(emptyOrNullString())));
        }

        @Test
        @DisplayName("INADIMPLENTE ainda entra: 200 com token")
        void inadimplenteEntra() throws Exception {
            assinaturaCom(StatusAssinatura.INADIMPLENTE);

            login("11122233344", SENHA_GESTOR, null).andExpect(status().isOk());
        }

        @Test
        @DisplayName("CANCELADA: 403 sem token, com o status na mensagem")
        void canceladaSemLogin() throws Exception {
            assinaturaCom(StatusAssinatura.CANCELADA);

            login("11122233344", SENHA_GESTOR, null)
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.token").doesNotExist())
                    .andExpect(jsonPath("$.message").value(
                            "O plano da sua organizacao esta cancelada. Entre em contato com o administrador."));
        }

        @Test
        @DisplayName("Sem assinatura vigente: 403 'não possui um plano ativo'")
        void semAssinatura() throws Exception {
            when(assinaturaTenantRepository.findAssinaturaVigenteByOrganizacaoId(ORG_ID)).thenReturn(Optional.empty());

            login("11122233344", SENHA_GESTOR, null)
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value(
                            "Sua organizacao nao possui um plano ativo. Entre em contato com o administrador."));
        }

        @Test
        @DisplayName("ROOT não passa pela checagem de plano")
        void rootIgnoraPlano() throws Exception {
            DecodedJWT jwt = tokenDaResposta(login("root", SENHA_MASTER, null).andExpect(status().isOk()));

            assertThat(jwt.getSubject()).isEqualTo("root");
            verify(assinaturaTenantRepository, never()).findAssinaturaVigenteByOrganizacaoId(any());
            verify(auditoriaAcessoMasterRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("POST /Home/login - senha master do ROOT")
    class SenhaMaster {

        @Test
        @DisplayName("Login do alvo + senha master: 200 com token do alvo e auditoria gravada (ator, alvo, IP)")
        void senhaMasterAuditada() throws Exception {
            DecodedJWT jwt = tokenDaResposta(login("11122233344", SENHA_MASTER, null).andExpect(status().isOk()));

            assertThat(jwt.getSubject()).isEqualTo("11122233344");
            assertThat(jwt.getClaim("organizacaoId").asLong()).isEqualTo(ORG_ID);
            ArgumentCaptor<AuditoriaAcessoMaster> captor = ArgumentCaptor.forClass(AuditoriaAcessoMaster.class);
            verify(auditoriaAcessoMasterRepository).save(captor.capture());
            AuditoriaAcessoMaster auditoria = captor.getValue();
            assertThat(auditoria.getAtorUsuarioId()).isEqualTo(1L);
            assertThat(auditoria.getAtorLogin()).isEqualTo("root");
            assertThat(auditoria.getAlvoUsuarioId()).isEqualTo(42L);
            assertThat(auditoria.getAlvoLogin()).isEqualTo("11122233344");
            assertThat(auditoria.getAlvoOrganizacaoId()).isEqualTo(ORG_ID);
            assertThat(auditoria.getIp()).isEqualTo("127.0.0.1");
            assertThat(auditoria.getDataHora()).isNotNull();
            // a senha master não passa pelo AuthenticationManager (que buscaria pelo findByLogin)
            verify(usuarioRepository, never()).findByLogin(anyString());
        }

        @Test
        @DisplayName("Senha master em alvo bloqueado (INATIVO): mesmo 401 do login normal, sem auditoria e sem token")
        void senhaMasterAlvoBloqueado() throws Exception {
            gestor.setStatus(StatusUsuario.INATIVO);

            login("11122233344", SENHA_MASTER, null)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("NAO_AUTENTICADO"))
                    .andExpect(jsonPath("$.message").value("Usuario bloqueado ou inativo"))
                    .andExpect(jsonPath("$.error").value("Usuario bloqueado ou inativo"))
                    .andExpect(jsonPath("$.token").doesNotExist());
            verify(auditoriaAcessoMasterRepository, never()).save(any());
        }

        @Test
        @DisplayName("Senha master também respeita o plano: alvo de clínica CANCELADA recebe 403")
        void senhaMasterRespeitaPlano() throws Exception {
            assinaturaCom(StatusAssinatura.CANCELADA);

            login("11122233344", SENHA_MASTER, null).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Se a auditoria falhar, o acesso master é negado e cai no fluxo normal (401)")
        void auditoriaFalhaNegaAcesso() throws Exception {
            when(auditoriaAcessoMasterRepository.save(any())).thenThrow(new RuntimeException("banco fora"));

            login("11122233344", SENHA_MASTER, null)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Credenciais invalidas"));
        }

        @Test
        @DisplayName("Login inexistente com senha master: cai no fluxo normal (401) sem auditoria")
        void loginInexistente() throws Exception {
            login("ninguem", SENHA_MASTER, null).andExpect(status().isUnauthorized());

            verify(auditoriaAcessoMasterRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("POST /Home/esqueciMinhaSenha - resposta neutra")
    class EsqueciMinhaSenha {

        @Test
        @DisplayName("E-mail cadastrado: 200 com a mensagem genérica")
        void emailCadastrado() throws Exception {
            mockMvc.perform(post("/Home/esqueciMinhaSenha")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"a@b.com\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(MENSAGEM_RECUPERACAO));
            verify(homeService).recuperarSenhaPorEmail("a@b.com");
        }

        @Test
        @DisplayName("E-mail não cadastrado: resposta idêntica (200 e mesma mensagem)")
        void emailNaoCadastrado() throws Exception {
            doThrow(new HomeService.EmailNaoEncontradoException("Email nao encontrado no sistema"))
                    .when(homeService).recuperarSenhaPorEmail("x@y.com");

            mockMvc.perform(post("/Home/esqueciMinhaSenha")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"x@y.com\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(MENSAGEM_RECUPERACAO));
        }

        @Test
        @DisplayName("Sem e-mail: 400 'Informe o e-mail'")
        void semEmail() throws Exception {
            mockMvc.perform(post("/Home/esqueciMinhaSenha")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Informe o e-mail"));
        }
    }
}
