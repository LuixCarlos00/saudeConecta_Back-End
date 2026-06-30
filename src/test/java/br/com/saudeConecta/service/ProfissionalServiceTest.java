package br.com.saudeConecta.service;

import br.com.saudeConecta.domain.endereco.Endereco;
import br.com.saudeConecta.domain.mensageria.Mensageria;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.profissional.Especialidade;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.domain.profissional.StatusProfissional;
import br.com.saudeConecta.domain.profissional.TipoProfissional;
import br.com.saudeConecta.domain.usuario.StatusUsuario;
import br.com.saudeConecta.domain.usuario.TipoUsuarioNovo;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.email.EmailNotificacaoService;
import br.com.saudeConecta.infra.tenant.TenantHelper;
import br.com.saudeConecta.infrastructure.persistence.repository.*;
import br.com.saudeConecta.presentation.dto.profissional.AtualizarClinicoRequest;
import br.com.saudeConecta.presentation.dto.profissional.CadastrarClinicoRequest;
import br.com.saudeConecta.util.EmailUnicoService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProfissionalService - Testes unitários")
class ProfissionalServiceTest {

    @Mock private ProfissionalRepository profissionalRepository;
    @Mock private TipoProfissionalRepository tipoProfissionalRepository;
    @Mock private EspecialidadeRepository especialidadeRepository;
    @Mock private OrganizacaoRepository organizacaoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private EnderecoRepository enderecoRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private TenantHelper tenantHelper;
    @Mock private EmailNotificacaoService emailNotificacaoService;
    @Mock private EmailUnicoService emailUnicoService;
    @Mock private HistoricoDadosPessoaisService historicoDadosPessoaisService;
    @Mock private LimitePlanoService limitePlanoService;
    @Mock private MensageriaRepository mensageriaRepository;

    @InjectMocks
    private ProfissionalService profissionalService;

    private static final Long ORG_ID = 1L;
    private static final Long PROF_ID = 10L;
    private static final Long USUARIO_ID = 20L;

    private Organizacao organizacao;
    private Endereco endereco;
    private Usuario usuario;
    private TipoProfissional tipoProfissional;
    private Profissional profissional;

    @BeforeEach
    void setUp() {
        organizacao = Organizacao.builder()
                .nome("Clínica Teste").cnpj("12345678000100")
                .build();
        organizacao.setId(ORG_ID);

        endereco = Endereco.builder()
                .endUF("SP").endMunicipio("São Paulo").endCep("01310-100")
                .endRua("Av. Paulista").endNumero(1000L)
                .build();

        tipoProfissional = TipoProfissional.builder()
                .codigo("MEDICO").nome("Médico").status((byte) 1)
                .build();
        tipoProfissional.setId(5L);

        usuario = Usuario.builder()
                .login("12345678900").senha("encodedPass")
                .tipoUsuario((byte) 3).tipoUsuarioNovo(TipoUsuarioNovo.PROFISSIONAL)
                .status(StatusUsuario.ATIVO).organizacao(organizacao)
                .build();
        usuario.setId(USUARIO_ID);

        profissional = Profissional.builder()
                .organizacao(organizacao).tipoProfissional(tipoProfissional)
                .nome("Dr. Teste").cpf("12345678900").email("dr@teste.com")
                .registroConselho("CRM-12345").usuario(usuario).endereco(endereco)
                .status(StatusProfissional.ATIVO)
                .build();
        profissional.setId(PROF_ID);
    }

    // ========== cadastraClinicoByOrg ==========

    @Nested
    @DisplayName("cadastraClinicoByOrg")
    class CadastraClinicoByOrg {

        private CadastrarClinicoRequest request;

        @BeforeEach
        void setUp() {
            request = new CadastrarClinicoRequest(
                    "Dr. Novo", "MASCULINO", "1985-03-15", "CRM-99999",
                    "123.456.789-00", "MG-123456", "novo@clinica.com",
                    "11999999999", "Cardiologia", "Medicina", "USP",
                    30, "MEDICO", "Brasileiro", "SP", "São Paulo",
                    "Centro", "01001-000", "Rua A", 10, null
            );
        }

        @Test
        @DisplayName("Deve cadastrar profissional com sucesso sem especialidade")
        void deveCadastrarProfissionalSemEspecialidade() {
            CadastrarClinicoRequest semEspecialidade = new CadastrarClinicoRequest(
                    "Dr. Novo", null, null, "CRM-99999",
                    "123.456.789-00", null, "novo@clinica.com",
                    null, null, null, null, null, "MEDICO",
                    null, null, null, null, null, null, null, null
            );

            doNothing().when(limitePlanoService).validarLimiteProfissional(ORG_ID);
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(usuarioRepository.existsByLogin("123.456.789-00")).thenReturn(false);
            when(emailUnicoService.emailJaExiste("novo@clinica.com")).thenReturn(false);
            when(organizacaoRepository.findById(ORG_ID)).thenReturn(Optional.of(organizacao));
            when(tipoProfissionalRepository.findByCodigo("MEDICO")).thenReturn(Optional.of(tipoProfissional));
            when(enderecoRepository.save(any(Endereco.class))).thenReturn(endereco);
            when(passwordEncoder.encode(any())).thenReturn("encodedPass");
            when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);
            when(profissionalRepository.save(any(Profissional.class))).thenReturn(profissional);

            Profissional resultado = profissionalService.cadastraClinicoByOrg(semEspecialidade);

            assertThat(resultado).isNotNull();
            assertThat(resultado.getId()).isEqualTo(PROF_ID);
            verify(profissionalRepository).save(any(Profissional.class));
            verify(emailNotificacaoService).enviarCredenciaisClinico(
                    eq("novo@clinica.com"), eq("Dr. Novo"), any(), any(),
                    eq(organizacao.getNome()), eq(ORG_ID), eq(PROF_ID));
        }

        @Test
        @DisplayName("Deve cadastrar profissional com especialidade existente")
        void deveCadastrarProfissionalComEspecialidadeExistente() {
            Especialidade especialidade = Especialidade.builder()
                    .nome("Cardiologia").tipoProfissional(tipoProfissional).status((byte) 1).build();

            doNothing().when(limitePlanoService).validarLimiteProfissional(ORG_ID);
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(usuarioRepository.existsByLogin(request.cpf())).thenReturn(false);
            when(emailUnicoService.emailJaExiste(request.email())).thenReturn(false);
            when(organizacaoRepository.findById(ORG_ID)).thenReturn(Optional.of(organizacao));
            when(tipoProfissionalRepository.findByCodigo("MEDICO")).thenReturn(Optional.of(tipoProfissional));
            when(especialidadeRepository.findByTipoProfissional_IdAndNome(5L, "Cardiologia"))
                    .thenReturn(Optional.of(especialidade));
            when(enderecoRepository.save(any())).thenReturn(endereco);
            when(passwordEncoder.encode(any())).thenReturn("encodedPass");
            when(usuarioRepository.save(any())).thenReturn(usuario);
            when(profissionalRepository.save(any())).thenReturn(profissional);

            Profissional resultado = profissionalService.cadastraClinicoByOrg(request);

            assertThat(resultado).isNotNull();
            verify(especialidadeRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve criar nova especialidade quando não existir")
        void deveCriarNovaEspecialidadeQuandoNaoExistir() {
            doNothing().when(limitePlanoService).validarLimiteProfissional(ORG_ID);
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(usuarioRepository.existsByLogin(request.cpf())).thenReturn(false);
            when(emailUnicoService.emailJaExiste(request.email())).thenReturn(false);
            when(organizacaoRepository.findById(ORG_ID)).thenReturn(Optional.of(organizacao));
            when(tipoProfissionalRepository.findByCodigo("MEDICO")).thenReturn(Optional.of(tipoProfissional));
            when(especialidadeRepository.findByTipoProfissional_IdAndNome(5L, "Cardiologia"))
                    .thenReturn(Optional.empty());
            when(especialidadeRepository.save(any())).thenReturn(Especialidade.builder().nome("Cardiologia").build());
            when(enderecoRepository.save(any())).thenReturn(endereco);
            when(passwordEncoder.encode(any())).thenReturn("encodedPass");
            when(usuarioRepository.save(any())).thenReturn(usuario);
            when(profissionalRepository.save(any())).thenReturn(profissional);

            Profissional resultado = profissionalService.cadastraClinicoByOrg(request);

            assertThat(resultado).isNotNull();
            verify(especialidadeRepository).save(any(Especialidade.class));
        }

        @Test
        @DisplayName("Deve lançar exceção quando CPF já cadastrado")
        void deveLancarExcecaoQuandoCpfJaCadastrado() {
            doNothing().when(limitePlanoService).validarLimiteProfissional(ORG_ID);
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(usuarioRepository.existsByLogin(request.cpf())).thenReturn(true);

            assertThatThrownBy(() -> profissionalService.cadastraClinicoByOrg(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("CPF ja cadastrado");
        }

        @Test
        @DisplayName("Deve lançar exceção quando email já existe")
        void deveLancarExcecaoQuandoEmailJaExiste() {
            doNothing().when(limitePlanoService).validarLimiteProfissional(ORG_ID);
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(usuarioRepository.existsByLogin(request.cpf())).thenReturn(false);
            when(emailUnicoService.emailJaExiste(request.email())).thenReturn(true);
            when(emailUnicoService.ondeEmailFoiEncontrado(request.email())).thenReturn("PROFISSIONAL");

            assertThatThrownBy(() -> profissionalService.cadastraClinicoByOrg(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Email ja cadastrado");
        }

        @Test
        @DisplayName("Deve lançar exceção quando organização não encontrada")
        void deveLancarExcecaoQuandoOrganizacaoNaoEncontrada() {
            doNothing().when(limitePlanoService).validarLimiteProfissional(ORG_ID);
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(usuarioRepository.existsByLogin(any())).thenReturn(false);
            when(emailUnicoService.emailJaExiste(any())).thenReturn(false);
            when(organizacaoRepository.findById(ORG_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> profissionalService.cadastraClinicoByOrg(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("nao encontrada");
        }

        @Test
        @DisplayName("Deve lançar exceção quando tipo profissional não encontrado")
        void deveLancarExcecaoQuandoTipoProfissionalNaoEncontrado() {
            doNothing().when(limitePlanoService).validarLimiteProfissional(ORG_ID);
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(usuarioRepository.existsByLogin(any())).thenReturn(false);
            when(emailUnicoService.emailJaExiste(any())).thenReturn(false);
            when(organizacaoRepository.findById(ORG_ID)).thenReturn(Optional.of(organizacao));
            when(tipoProfissionalRepository.findByCodigo("MEDICO")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> profissionalService.cadastraClinicoByOrg(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("nao encontrado");
        }
    }

    // ========== buscarClinicoIdByOrg ==========

    @Nested
    @DisplayName("buscarClinicoIdByOrg")
    class BuscarClinicoIdByOrg {

        @Test
        @DisplayName("Deve retornar profissional quando encontrado")
        void deveRetornarProfissionalQuandoEncontrado() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(profissionalRepository.buscarClinicoIdByOrg(PROF_ID, ORG_ID))
                    .thenReturn(Optional.of(profissional));

            Optional<Profissional> resultado = profissionalService.buscarClinicoIdByOrg(PROF_ID);

            assertThat(resultado).isPresent();
            assertThat(resultado.get().getId()).isEqualTo(PROF_ID);
        }

        @Test
        @DisplayName("Deve retornar vazio quando profissional não pertence à organização")
        void deveRetornarVazioQuandoNaoPertenceOrg() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(profissionalRepository.buscarClinicoIdByOrg(PROF_ID, ORG_ID))
                    .thenReturn(Optional.empty());

            Optional<Profissional> resultado = profissionalService.buscarClinicoIdByOrg(PROF_ID);

            assertThat(resultado).isEmpty();
        }
    }

    // ========== atualizarClinicoIdByOrg ==========

    @Nested
    @DisplayName("atualizarClinicoIdByOrg")
    class AtualizarClinicoIdByOrg {

        private AtualizarClinicoRequest request;

        @BeforeEach
        void setUp() {
            request = new AtualizarClinicoRequest(
                    "Dr. Atualizado", "12345678900", "MG-111", "CRM-11111",
                    "11888888888", "dr.atualizado@teste.com", "MASCULINO",
                    LocalDate.of(1985, 3, 15), "Medicina", "USP",
                    45, BigDecimal.valueOf(250), null, null, null
            );
        }

        @Test
        @DisplayName("Deve atualizar profissional com sucesso")
        void deveAtualizarProfissionalComSucesso() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(tenantHelper.getCurrentUserId()).thenReturn(USUARIO_ID);
            when(profissionalRepository.buscarClinicoIdByOrg(PROF_ID, ORG_ID))
                    .thenReturn(Optional.of(profissional));
            when(profissionalRepository.save(any())).thenReturn(profissional);

            Profissional resultado = profissionalService.atualizarClinicoIdByOrg(PROF_ID, request);

            assertThat(resultado).isNotNull();
            assertThat(profissional.getNome()).isEqualTo("Dr. Atualizado");
            assertThat(profissional.getEmail()).isEqualTo("dr.atualizado@teste.com");
            verify(profissionalRepository).save(profissional);
        }

        @Test
        @DisplayName("Deve lançar exceção quando profissional não encontrado")
        void deveLancarExcecaoQuandoProfissionalNaoEncontrado() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(profissionalRepository.buscarClinicoIdByOrg(PROF_ID, ORG_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> profissionalService.atualizarClinicoIdByOrg(PROF_ID, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("nao encontrado");
        }

        @Test
        @DisplayName("Deve atualizar tipo profissional quando informado")
        void deveAtualizarTipoProfissionalQuandoInformado() {
            AtualizarClinicoRequest requestComTipo = new AtualizarClinicoRequest(
                    "Dr. Atualizado", "12345678900", null, "CRM-11111",
                    null, "dr@teste.com", "MASCULINO",
                    LocalDate.of(1985, 3, 15), null, null,
                    30, null, "DENTISTA", null, null
            );
            TipoProfissional dentista = TipoProfissional.builder().codigo("DENTISTA").build();
            dentista.setId(6L);

            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(tenantHelper.getCurrentUserId()).thenReturn(USUARIO_ID);
            when(profissionalRepository.buscarClinicoIdByOrg(PROF_ID, ORG_ID))
                    .thenReturn(Optional.of(profissional));
            when(tipoProfissionalRepository.findByCodigo("DENTISTA")).thenReturn(Optional.of(dentista));
            when(profissionalRepository.save(any())).thenReturn(profissional);

            profissionalService.atualizarClinicoIdByOrg(PROF_ID, requestComTipo);

            assertThat(profissional.getTipoProfissional()).isEqualTo(dentista);
        }

        @Test
        @DisplayName("Deve lançar exceção quando tipo profissional informado não existe")
        void deveLancarExcecaoQuandoTipoProfissionalNaoExiste() {
            AtualizarClinicoRequest requestTipoInvalido = new AtualizarClinicoRequest(
                    "Dr. Atualizado", "12345678900", null, "CRM-11111",
                    null, "dr@teste.com", "MASCULINO",
                    LocalDate.of(1985, 3, 15), null, null,
                    30, null, "INVALIDO", null, null
            );

            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(profissionalRepository.buscarClinicoIdByOrg(PROF_ID, ORG_ID))
                    .thenReturn(Optional.of(profissional));
            when(tipoProfissionalRepository.findByCodigo("INVALIDO")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> profissionalService.atualizarClinicoIdByOrg(PROF_ID, requestTipoInvalido))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("nao encontrado");
        }

        @Test
        @DisplayName("Deve atualizar especialidade existente")
        void deveAtualizarEspecialidadeExistente() {
            Especialidade esp = Especialidade.builder().nome("Neurologia").tipoProfissional(tipoProfissional).build();
            AtualizarClinicoRequest requestComEsp = new AtualizarClinicoRequest(
                    "Dr. Atualizado", "12345678900", null, "CRM-11111",
                    null, "dr@teste.com", "MASCULINO",
                    LocalDate.of(1985, 3, 15), null, null,
                    30, null, null, "Neurologia", null
            );

            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(tenantHelper.getCurrentUserId()).thenReturn(USUARIO_ID);
            when(profissionalRepository.buscarClinicoIdByOrg(PROF_ID, ORG_ID))
                    .thenReturn(Optional.of(profissional));
            when(especialidadeRepository.findByTipoProfissional_IdAndNome(5L, "Neurologia"))
                    .thenReturn(Optional.of(esp));
            when(profissionalRepository.save(any())).thenReturn(profissional);

            profissionalService.atualizarClinicoIdByOrg(PROF_ID, requestComEsp);

            assertThat(profissional.getEspecialidades()).contains(esp);
            verify(especialidadeRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve criar nova especialidade quando não existir na atualização")
        void deveCriarNovaEspecialidadeQuandoNaoExistirNaAtualizacao() {
            Especialidade novaEsp = Especialidade.builder().nome("Geriatria").tipoProfissional(tipoProfissional).build();
            AtualizarClinicoRequest requestNovaEsp = new AtualizarClinicoRequest(
                    "Dr. Atualizado", "12345678900", null, "CRM-11111",
                    null, "dr@teste.com", "MASCULINO",
                    LocalDate.of(1985, 3, 15), null, null,
                    30, null, null, "Geriatria", null
            );

            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(tenantHelper.getCurrentUserId()).thenReturn(USUARIO_ID);
            when(profissionalRepository.buscarClinicoIdByOrg(PROF_ID, ORG_ID))
                    .thenReturn(Optional.of(profissional));
            when(especialidadeRepository.findByTipoProfissional_IdAndNome(5L, "Geriatria"))
                    .thenReturn(Optional.empty());
            when(especialidadeRepository.save(any())).thenReturn(novaEsp);
            when(profissionalRepository.save(any())).thenReturn(profissional);

            profissionalService.atualizarClinicoIdByOrg(PROF_ID, requestNovaEsp);

            verify(especialidadeRepository).save(any(Especialidade.class));
        }
    }

    // ========== deletarClinicoIdByOrg ==========

    @Nested
    @DisplayName("deletarClinicoIdByOrg")
    class DeletarClinicoIdByOrg {

        @Test
        @DisplayName("Deve deletar profissional sem mensageria com sucesso")
        void deveDeletarProfissionalSemMensageriaComSucesso() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(profissionalRepository.buscarClinicoIdByOrg(PROF_ID, ORG_ID))
                    .thenReturn(Optional.of(profissional));
            when(mensageriaRepository.findByDestinatarioEntidadeId(PROF_ID))
                    .thenReturn(Collections.emptyList());

            profissionalService.deletarClinicoIdByOrg(PROF_ID);

            verify(mensageriaRepository, never()).deleteAll(any());
            verify(profissionalRepository).delete(profissional);
            verify(usuarioRepository).delete(usuario);
        }

        @Test
        @DisplayName("Deve deletar mensageria antes de deletar profissional")
        void deveDeletarMensageriaAntesDoProfissional() {
            List<Mensageria> mensagens = List.of(mock(Mensageria.class));
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(profissionalRepository.buscarClinicoIdByOrg(PROF_ID, ORG_ID))
                    .thenReturn(Optional.of(profissional));
            when(mensageriaRepository.findByDestinatarioEntidadeId(PROF_ID)).thenReturn(mensagens);

            profissionalService.deletarClinicoIdByOrg(PROF_ID);

            verify(mensageriaRepository).deleteAll(mensagens);
            verify(profissionalRepository).delete(profissional);
            verify(usuarioRepository).delete(usuario);
        }

        @Test
        @DisplayName("Deve lançar exceção quando profissional não encontrado")
        void deveLancarExcecaoQuandoProfissionalNaoEncontrado() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(profissionalRepository.buscarClinicoIdByOrg(PROF_ID, ORG_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> profissionalService.deletarClinicoIdByOrg(PROF_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("nao encontrado");
        }

        @Test
        @DisplayName("Deve lançar exceção quando profissional não tem usuário associado")
        void deveLancarExcecaoQuandoSemUsuarioAssociado() {
            Profissional profSemUsuario = Profissional.builder()
                    .nome("Sem Usuário").usuario(null).status(StatusProfissional.ATIVO).build();
            profSemUsuario.setId(PROF_ID);

            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(profissionalRepository.buscarClinicoIdByOrg(PROF_ID, ORG_ID))
                    .thenReturn(Optional.of(profSemUsuario));

            assertThatThrownBy(() -> profissionalService.deletarClinicoIdByOrg(PROF_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("usuario associado");
        }

        @Test
        @DisplayName("Deve propagar exceção de relacionamento ao deletar")
        void devePropararExcecaoDeRelacionamento() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(profissionalRepository.buscarClinicoIdByOrg(PROF_ID, ORG_ID))
                    .thenReturn(Optional.of(profissional));
            when(mensageriaRepository.findByDestinatarioEntidadeId(PROF_ID))
                    .thenReturn(Collections.emptyList());
            doThrow(new RuntimeException("FK constraint"))
                    .when(profissionalRepository).delete(profissional);

            assertThatThrownBy(() -> profissionalService.deletarClinicoIdByOrg(PROF_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("relacionamentos existentes");
        }
    }

    // ========== contarAtivosPorOrganizacao ==========

    @Nested
    @DisplayName("contarAtivosPorOrganizacao")
    class ContarAtivosPorOrganizacao {

        @Test
        @DisplayName("Deve retornar contagem de profissionais ativos")
        void deveRetornarContagemAtivos() {
            when(profissionalRepository.countAtivosByOrganizacaoId(ORG_ID)).thenReturn(8L);

            Long resultado = profissionalService.contarAtivosPorOrganizacao(ORG_ID);

            assertThat(resultado).isEqualTo(8L);
        }

        @Test
        @DisplayName("Deve retornar zero quando não houver ativos")
        void deveRetornarZeroQuandoSemAtivos() {
            when(profissionalRepository.countAtivosByOrganizacaoId(ORG_ID)).thenReturn(0L);

            Long resultado = profissionalService.contarAtivosPorOrganizacao(ORG_ID);

            assertThat(resultado).isZero();
        }
    }

    // ========== getEstatisticasMedicosAtivosByOrg ==========

    @Nested
    @DisplayName("getEstatisticasMedicosAtivosByOrg")
    class GetEstatisticasMedicosAtivosByOrg {

        @Test
        @DisplayName("Deve retornar contagem global de profissionais ativos")
        void deveRetornarContagemGlobal() {
            when(profissionalRepository.countTodosAtivos()).thenReturn(50L);

            Long resultado = profissionalService.getEstatisticasMedicosAtivosByOrg();

            assertThat(resultado).isEqualTo(50L);
        }
    }

    // ========== buscarPorOrganizacao ==========

    @Nested
    @DisplayName("buscarPorOrganizacao")
    class BuscarPorOrganizacao {

        @Test
        @DisplayName("Deve retornar lista de profissionais ativos da organização")
        void deveRetornarProfissionaisAtivos() {
            when(profissionalRepository.findByOrganizacaoIdAndStatusWithRelations(ORG_ID, StatusProfissional.ATIVO))
                    .thenReturn(List.of(profissional));

            List<Profissional> resultado = profissionalService.buscarPorOrganizacao(ORG_ID);

            assertThat(resultado).hasSize(1);
            verify(profissionalRepository).findByOrganizacaoIdAndStatusWithRelations(ORG_ID, StatusProfissional.ATIVO);
        }

        @Test
        @DisplayName("Deve retornar lista vazia quando organização não tiver profissionais")
        void deveRetornarListaVazia() {
            when(profissionalRepository.findByOrganizacaoIdAndStatusWithRelations(ORG_ID, StatusProfissional.ATIVO))
                    .thenReturn(Collections.emptyList());

            List<Profissional> resultado = profissionalService.buscarPorOrganizacao(ORG_ID);

            assertThat(resultado).isEmpty();
        }
    }

    // ========== buscarPorNomeComFiltro ==========

    @Nested
    @DisplayName("buscarPorNomeComFiltro")
    class BuscarPorNomeComFiltro {

        @Test
        @DisplayName("Deve usar consulta com filtro ATIVO")
        void deveUsarConsultaComFiltroAtivo() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(profissionalRepository.findByOrganizacaoIdAndNomeContainingWithFiltro(
                    ORG_ID, "Dr", "ATIVO", StatusProfissional.ATIVO))
                    .thenReturn(List.of(profissional));

            List<Profissional> resultado = profissionalService.buscarPorNomeComFiltro("Dr", "ATIVO");

            assertThat(resultado).hasSize(1);
            verify(profissionalRepository).findByOrganizacaoIdAndNomeContainingWithFiltro(
                    ORG_ID, "Dr", "ATIVO", StatusProfissional.ATIVO);
        }

        @Test
        @DisplayName("Deve usar consulta original com filtro ALL")
        void deveUsarConsultaOriginalComFiltroAll() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(profissionalRepository.findByOrganizacaoIdAndNomeContaining(ORG_ID, "Dr"))
                    .thenReturn(List.of(profissional));

            List<Profissional> resultado = profissionalService.buscarPorNomeComFiltro("Dr", "ALL");

            assertThat(resultado).hasSize(1);
            verify(profissionalRepository).findByOrganizacaoIdAndNomeContaining(ORG_ID, "Dr");
        }
    }

    // ========== buscarPorCRMComFiltro ==========

    @Nested
    @DisplayName("buscarPorCRMComFiltro")
    class BuscarPorCRMComFiltro {

        @Test
        @DisplayName("Deve usar consulta com filtro ATIVO")
        void deveUsarConsultaComFiltroAtivo() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(profissionalRepository.findByOrganizacaoIdAndCrmContainingWithFiltro(
                    ORG_ID, "CRM-123", "ATIVO", StatusProfissional.ATIVO))
                    .thenReturn(List.of(profissional));

            List<Profissional> resultado = profissionalService.buscarPorCRMComFiltro("CRM-123", "ATIVO");

            assertThat(resultado).hasSize(1);
        }

        @Test
        @DisplayName("Deve usar consulta original com filtro ALL")
        void deveUsarConsultaOriginalComFiltroAll() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(profissionalRepository.findByOrganizacaoIdAndCrmContaining(ORG_ID, "CRM-123"))
                    .thenReturn(Collections.emptyList());

            List<Profissional> resultado = profissionalService.buscarPorCRMComFiltro("CRM-123", "ALL");

            assertThat(resultado).isEmpty();
        }
    }

    // ========== buscarPorCidadeComFiltro ==========

    @Nested
    @DisplayName("buscarPorCidadeComFiltro")
    class BuscarPorCidadeComFiltro {

        @Test
        @DisplayName("Deve usar consulta com filtro ATIVO")
        void deveUsarConsultaComFiltroAtivo() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(profissionalRepository.findByOrganizacaoIdAndCidadeContainingWithFiltro(
                    ORG_ID, "São Paulo", "ATIVO", StatusProfissional.ATIVO))
                    .thenReturn(List.of(profissional));

            List<Profissional> resultado = profissionalService.buscarPorCidadeComFiltro("São Paulo", "ATIVO");

            assertThat(resultado).hasSize(1);
        }

        @Test
        @DisplayName("Deve usar consulta original com filtro ALL")
        void deveUsarConsultaOriginalComFiltroAll() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(profissionalRepository.findByOrganizacaoIdAndCidadeContaining(ORG_ID, "São Paulo"))
                    .thenReturn(Collections.emptyList());

            List<Profissional> resultado = profissionalService.buscarPorCidadeComFiltro("São Paulo", "ALL");

            assertThat(resultado).isEmpty();
        }
    }

    // ========== buscarPorEspecialidadeComFiltro ==========

    @Nested
    @DisplayName("buscarPorEspecialidadeComFiltro")
    class BuscarPorEspecialidadeComFiltro {

        @Test
        @DisplayName("Deve usar consulta com filtro ATIVO")
        void deveUsarConsultaComFiltroAtivo() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(profissionalRepository.findByOrganizacaoIdAndEspecialidadeContainingWithFiltro(
                    ORG_ID, "Cardiologia", "ATIVO", StatusProfissional.ATIVO))
                    .thenReturn(List.of(profissional));

            List<Profissional> resultado = profissionalService.buscarPorEspecialidadeComFiltro("Cardiologia", "ATIVO");

            assertThat(resultado).hasSize(1);
        }

        @Test
        @DisplayName("Deve usar consulta original com filtro ALL")
        void deveUsarConsultaOriginalComFiltroAll() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(profissionalRepository.findByOrganizacaoIdAndEspecialidadeContaining(ORG_ID, "Cardiologia"))
                    .thenReturn(Collections.emptyList());

            List<Profissional> resultado = profissionalService.buscarPorEspecialidadeComFiltro("Cardiologia", "ALL");

            assertThat(resultado).isEmpty();
        }
    }

    // ========== buscarTodosComFiltro ==========

    @Nested
    @DisplayName("buscarTodosComFiltro")
    class BuscarTodosComFiltro {

        @Test
        @DisplayName("Deve buscar com filtro ATIVO")
        void deveBuscarComFiltroAtivo() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(profissionalRepository.findByOrganizacao_IdWithUsuarioWithFiltro(
                    ORG_ID, "ATIVO", StatusProfissional.ATIVO))
                    .thenReturn(List.of(profissional));

            List<Profissional> resultado = profissionalService.buscarTodosComFiltro("ATIVO");

            assertThat(resultado).hasSize(1);
            verify(profissionalRepository).findByOrganizacao_IdWithUsuarioWithFiltro(ORG_ID, "ATIVO", StatusProfissional.ATIVO);
        }

        @Test
        @DisplayName("Deve buscar todos com filtro ALL")
        void deveBuscarTodosComFiltroAll() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(profissionalRepository.findByOrganizacao_IdWithUsuario(ORG_ID))
                    .thenReturn(List.of(profissional));

            List<Profissional> resultado = profissionalService.buscarTodosComFiltro("ALL");

            assertThat(resultado).hasSize(1);
            verify(profissionalRepository).findByOrganizacao_IdWithUsuario(ORG_ID);
        }

        @Test
        @DisplayName("Deve retornar lista vazia quando não houver profissionais")
        void deveRetornarListaVazia() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(profissionalRepository.findByOrganizacao_IdWithUsuario(ORG_ID))
                    .thenReturn(Collections.emptyList());

            List<Profissional> resultado = profissionalService.buscarTodosComFiltro("ALL");

            assertThat(resultado).isEmpty();
        }
    }
}
