package br.com.saudeConecta.application.auth;

import br.com.saudeConecta.application.exception.BusinessException;
import br.com.saudeConecta.application.notificacao.DadosEmailRecuperacaoSenha;
import br.com.saudeConecta.application.notificacao.EmailNotificacaoService;
import br.com.saudeConecta.domain.admin.AdminOrganizacao;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.domain.secretaria.Secretaria;
import br.com.saudeConecta.domain.usuario.TokenRecuperacaoSenha;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.infrastructure.persistence.repository.AdminOrganizacaoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.ProfissionalRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.SecretariaRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.TokenRecuperacaoSenhaRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecuperacaoSenhaService - link de redefinição com token de uso único")
class RecuperacaoSenhaServiceTest {

    private static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");
    private static final Clock RELOGIO = Clock.fixed(Instant.parse("2026-11-17T15:00:00Z"), ZONA);
    private static final LocalDateTime AGORA = LocalDateTime.now(RELOGIO);
    private static final String EMAIL = "luiz@email.com";
    private static final String URL_FRONT = "https://app.docclin.com.br/";

    @Mock private ProfissionalRepository profissionalRepository;
    @Mock private SecretariaRepository secretariaRepository;
    @Mock private AdminOrganizacaoRepository adminOrganizacaoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private TokenRecuperacaoSenhaRepository tokenRepository;
    @Mock private EmailNotificacaoService emailNotificacaoService;
    @Mock private PasswordEncoder passwordEncoder;

    private RecuperacaoSenhaService service;
    private Usuario admin;
    private Usuario root;

    @BeforeEach
    void setUp() {
        service = new RecuperacaoSenhaService(profissionalRepository, secretariaRepository,
                adminOrganizacaoRepository, usuarioRepository, tokenRepository, emailNotificacaoService,
                passwordEncoder, RELOGIO, URL_FRONT, 30);

        Organizacao organizacao = new Organizacao();
        organizacao.setId(4L);
        admin = Usuario.builder().id(17L).login("11122233344").senha("hashAntigo").organizacao(organizacao).build();
        root = Usuario.builder().id(1L).login("root").senha("hashRoot").build();
    }

    private void semContasNasTabelasDePerfil() {
        when(profissionalRepository.findAllByEmail(EMAIL)).thenReturn(List.of());
        when(secretariaRepository.findAllByEmail(EMAIL)).thenReturn(List.of());
        when(adminOrganizacaoRepository.findAllByEmail(EMAIL)).thenReturn(List.of());
    }

    @Nested
    @DisplayName("solicitar")
    class Solicitar {

        @Test
        @DisplayName("Admin: grava só o hash do token, com validade de 30 min, e manda o link com o token em claro")
        void adminRecebeLink() {
            when(profissionalRepository.findAllByEmail(EMAIL)).thenReturn(List.of());
            when(secretariaRepository.findAllByEmail(EMAIL)).thenReturn(List.of());
            when(adminOrganizacaoRepository.findAllByEmail(EMAIL))
                    .thenReturn(List.of(AdminOrganizacao.builder().nome("Ana").usuario(admin).build()));
            when(usuarioRepository.findByEmailAndOrganizacaoIsNull(EMAIL)).thenReturn(Optional.empty());

            int enviados = service.solicitar("  " + EMAIL + " ");

            assertThat(enviados).isEqualTo(1);
            ArgumentCaptor<TokenRecuperacaoSenha> tokenCaptor = ArgumentCaptor.forClass(TokenRecuperacaoSenha.class);
            verify(tokenRepository).save(tokenCaptor.capture());
            ArgumentCaptor<DadosEmailRecuperacaoSenha> emailCaptor = ArgumentCaptor.forClass(DadosEmailRecuperacaoSenha.class);
            verify(emailNotificacaoService).enviarLinkRecuperacaoSenha(emailCaptor.capture());

            TokenRecuperacaoSenha salvo = tokenCaptor.getValue();
            DadosEmailRecuperacaoSenha enviado = emailCaptor.getValue();
            String token = enviado.link().substring(enviado.link().indexOf("token=") + "token=".length());

            assertThat(enviado.link()).startsWith("https://app.docclin.com.br/#/redefinir-senha?token=");
            assertThat(token).hasSizeGreaterThanOrEqualTo(43).matches("[A-Za-z0-9_-]+");
            assertThat(salvo.getTokenHash()).isEqualTo(RecuperacaoSenhaService.hash(token)).isNotEqualTo(token);
            assertThat(salvo.getUsuario()).isSameAs(admin);
            assertThat(salvo.getExpiraEm()).isEqualTo(AGORA.plusMinutes(30));
            assertThat(salvo.getUsadoEm()).isNull();
            assertThat(enviado.login()).isEqualTo("11122233344");
            assertThat(enviado.nome()).isEqualTo("Ana");
            assertThat(enviado.organizacaoId()).isEqualTo(4L);
            assertThat(admin.getSenha()).isEqualTo("hashAntigo");
        }

        @Test
        @DisplayName("Pedido novo invalida os links pendentes do mesmo usuário antes de gerar outro")
        void invalidaPendentes() {
            semContasNasTabelasDePerfil();
            when(usuarioRepository.findByEmailAndOrganizacaoIsNull(EMAIL)).thenReturn(Optional.of(root));

            service.solicitar(EMAIL);

            verify(tokenRepository).invalidarPendentes(1L, AGORA);
        }

        @Test
        @DisplayName("Mesmo e-mail no ROOT e num admin: cada conta recebe o próprio link")
        void emailRepetidoEmDuasContas() {
            when(profissionalRepository.findAllByEmail(EMAIL)).thenReturn(List.of());
            when(secretariaRepository.findAllByEmail(EMAIL)).thenReturn(List.of());
            when(adminOrganizacaoRepository.findAllByEmail(EMAIL))
                    .thenReturn(List.of(AdminOrganizacao.builder().nome("Ana").usuario(admin).build()));
            when(usuarioRepository.findByEmailAndOrganizacaoIsNull(EMAIL)).thenReturn(Optional.of(root));

            assertThat(service.solicitar(EMAIL)).isEqualTo(2);

            ArgumentCaptor<DadosEmailRecuperacaoSenha> emailCaptor = ArgumentCaptor.forClass(DadosEmailRecuperacaoSenha.class);
            verify(emailNotificacaoService, times(2)).enviarLinkRecuperacaoSenha(emailCaptor.capture());
            assertThat(emailCaptor.getAllValues()).extracting(DadosEmailRecuperacaoSenha::login)
                    .containsExactly("11122233344", "root");
            assertThat(emailCaptor.getAllValues()).extracting(DadosEmailRecuperacaoSenha::link).doesNotHaveDuplicates();
        }

        @Test
        @DisplayName("Mesmo usuário achado em duas tabelas (dados legados): um link só")
        void mesmoUsuarioEmDuasTabelas() {
            when(profissionalRepository.findAllByEmail(EMAIL))
                    .thenReturn(List.of(Profissional.builder().nome("Dr. Ana").usuario(admin).build()));
            when(secretariaRepository.findAllByEmail(EMAIL))
                    .thenReturn(List.of(Secretaria.builder().nome("Ana").usuario(admin).build()));
            when(adminOrganizacaoRepository.findAllByEmail(EMAIL)).thenReturn(List.of());
            when(usuarioRepository.findByEmailAndOrganizacaoIsNull(EMAIL)).thenReturn(Optional.empty());

            assertThat(service.solicitar(EMAIL)).isEqualTo(1);
            verify(emailNotificacaoService, times(1)).enviarLinkRecuperacaoSenha(any());
        }

        @Test
        @DisplayName("Registro de perfil sem usuário vinculado é ignorado, sem erro")
        void perfilSemUsuario() {
            when(profissionalRepository.findAllByEmail(EMAIL))
                    .thenReturn(List.of(Profissional.builder().nome("Órfão").build()));
            when(secretariaRepository.findAllByEmail(EMAIL)).thenReturn(List.of());
            when(adminOrganizacaoRepository.findAllByEmail(EMAIL)).thenReturn(List.of());
            when(usuarioRepository.findByEmailAndOrganizacaoIsNull(EMAIL)).thenReturn(Optional.empty());

            assertThat(service.solicitar(EMAIL)).isZero();
            verifyNoInteractions(tokenRepository, emailNotificacaoService);
        }

        @Test
        @DisplayName("E-mail não cadastrado: não lança, não grava token e não manda e-mail")
        void emailNaoCadastrado() {
            semContasNasTabelasDePerfil();
            when(usuarioRepository.findByEmailAndOrganizacaoIsNull(EMAIL)).thenReturn(Optional.empty());

            assertThat(service.solicitar(EMAIL)).isZero();
            verifyNoInteractions(tokenRepository, emailNotificacaoService);
        }

        @Test
        @DisplayName("ROOT sem nome cadastrado: saudação com o nome padrão")
        void rootNomePadrao() {
            semContasNasTabelasDePerfil();
            when(usuarioRepository.findByEmailAndOrganizacaoIsNull(EMAIL)).thenReturn(Optional.of(root));

            service.solicitar(EMAIL);

            ArgumentCaptor<DadosEmailRecuperacaoSenha> emailCaptor = ArgumentCaptor.forClass(DadosEmailRecuperacaoSenha.class);
            verify(emailNotificacaoService).enviarLinkRecuperacaoSenha(emailCaptor.capture());
            assertThat(emailCaptor.getValue().nome()).isEqualTo("Usuário");
            assertThat(emailCaptor.getValue().organizacaoId()).isNull();
        }
    }

    @Nested
    @DisplayName("validar")
    class Validar {

        @Test
        @DisplayName("Token pendente e no prazo: não lança")
        void valido() {
            when(tokenRepository.findByTokenHash(RecuperacaoSenhaService.hash("tok")))
                    .thenReturn(Optional.of(new TokenRecuperacaoSenha(admin, "h", AGORA.minusMinutes(5), AGORA.plusMinutes(25))));

            service.validar("tok");
        }

        @Test
        @DisplayName("Token inexistente: 400 TOKEN_RECUPERACAO_INVALIDO")
        void inexistente() {
            when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

            assertTokenInvalido(() -> service.validar("tok"));
        }

        @Test
        @DisplayName("Token vencido exatamente agora: inválido (o prazo é exclusivo)")
        void vencidoNoLimite() {
            when(tokenRepository.findByTokenHash(anyString()))
                    .thenReturn(Optional.of(new TokenRecuperacaoSenha(admin, "h", AGORA.minusMinutes(30), AGORA)));

            assertTokenInvalido(() -> service.validar("tok"));
        }

        @Test
        @DisplayName("Token já usado ou substituído: inválido")
        void usado() {
            TokenRecuperacaoSenha usado = new TokenRecuperacaoSenha(admin, "h", AGORA.minusMinutes(5), AGORA.plusMinutes(25));
            usado.marcarUsado(AGORA.minusMinutes(1));
            when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(usado));

            assertTokenInvalido(() -> service.validar("tok"));
        }
    }

    @Nested
    @DisplayName("redefinir")
    class Redefinir {

        @Test
        @DisplayName("Link válido: troca a senha (hash), consome o token e invalida os outros pendentes")
        void redefineSenha() {
            TokenRecuperacaoSenha registro = new TokenRecuperacaoSenha(admin, "h", AGORA.minusMinutes(5), AGORA.plusMinutes(25));
            when(tokenRepository.findByTokenHashParaUso(RecuperacaoSenhaService.hash("tok"))).thenReturn(Optional.of(registro));
            when(passwordEncoder.encode("novaSenha123")).thenReturn("hashNovo");

            service.redefinir("tok", "novaSenha123");

            assertThat(admin.getSenha()).isEqualTo("hashNovo");
            assertThat(registro.getUsadoEm()).isEqualTo(AGORA);
            verify(usuarioRepository).save(admin);
            verify(tokenRepository).saveAndFlush(registro);
            verify(tokenRepository).invalidarPendentes(17L, AGORA);
        }

        @Test
        @DisplayName("Link já usado: 400 e a senha não muda (reuso do mesmo link)")
        void linkJaUsado() {
            TokenRecuperacaoSenha registro = new TokenRecuperacaoSenha(admin, "h", AGORA.minusMinutes(5), AGORA.plusMinutes(25));
            registro.marcarUsado(AGORA.minusMinutes(1));
            when(tokenRepository.findByTokenHashParaUso(anyString())).thenReturn(Optional.of(registro));

            assertTokenInvalido(() -> service.redefinir("tok", "novaSenha123"));

            assertThat(admin.getSenha()).isEqualTo("hashAntigo");
            verify(passwordEncoder, never()).encode(anyString());
            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("Link expirado: 400 e a senha não muda")
        void linkExpirado() {
            when(tokenRepository.findByTokenHashParaUso(anyString()))
                    .thenReturn(Optional.of(new TokenRecuperacaoSenha(admin, "h", AGORA.minusMinutes(40), AGORA.minusMinutes(10))));

            assertTokenInvalido(() -> service.redefinir("tok", "novaSenha123"));

            verify(usuarioRepository, never()).save(any());
            verify(tokenRepository, never()).invalidarPendentes(eq(17L), any());
        }
    }

    @Test
    @DisplayName("hash: SHA-256 em hex, 64 caracteres e determinístico")
    void hashDeterministico() {
        assertThat(RecuperacaoSenhaService.hash("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad")
                .hasSize(64);
    }

    private static void assertTokenInvalido(org.assertj.core.api.ThrowableAssert.ThrowingCallable chamada) {
        assertThatThrownBy(chamada)
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Link inválido ou expirado")
                .extracting("errorCode").isEqualTo(RecuperacaoSenhaService.CODIGO_TOKEN_INVALIDO);
    }
}
