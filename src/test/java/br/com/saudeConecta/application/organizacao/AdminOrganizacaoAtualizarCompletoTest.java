package br.com.saudeConecta.application.organizacao;

import br.com.saudeConecta.application.cache.CacheEvictionService;
import br.com.saudeConecta.application.organizacao.dto.AtualizarAdminOrgCompletoRequest;
import br.com.saudeConecta.domain.admin.AdminOrganizacao;
import br.com.saudeConecta.domain.endereco.Endereco;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.organizacao.TipoOrganizacao;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.infrastructure.persistence.repository.AdminOrganizacaoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.EnderecoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.OrganizacaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Caracteriza as regras campo a campo de atualizarAdminOrgCompleto (SUPER_ADMIN):
 * quais campos ignoram nulo/em branco e quais aceitam texto vazio.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdminOrganizacaoService.atualizarAdminOrgCompleto - regras por campo")
class AdminOrganizacaoAtualizarCompletoTest {

    private static final Long ADMIN_ID = 10L;

    @Mock private AdminOrganizacaoRepository adminOrganizacaoRepository;
    @Mock private OrganizacaoRepository organizacaoRepository;
    @Mock private EnderecoRepository enderecoRepository;
    @Mock private CacheEvictionService cacheEvictionService;

    @InjectMocks
    private AdminOrganizacaoService service;

    private Endereco endereco;
    private Organizacao organizacao;
    private AdminOrganizacao admin;

    @BeforeEach
    void setUp() {
        endereco = Endereco.builder().endCep("111").endUF("SP").endMunicipio("Sao Paulo").endBairro("Centro")
                .endRua("Rua A").endNumero(1L).endComplemento("c").build();
        organizacao = Organizacao.builder().nome("Clinica").razaoSocial("Razao").cnpj("123")
                .tipo(TipoOrganizacao.CLINICA).email("c@c.com").telefone("1100").endereco(endereco).build();
        organizacao.setId(1L);
        Usuario usuario = Usuario.builder().login("login").senha("x").build();
        usuario.setId(20L);
        admin = AdminOrganizacao.builder().organizacao(organizacao).usuario(usuario).nome("Admin")
                .cargo("Cargo").email("a@a.com").build();
        admin.setId(ADMIN_ID);
        when(adminOrganizacaoRepository.findByIdWithOrgAndEndereco(ADMIN_ID)).thenReturn(Optional.of(admin));
        lenient().when(adminOrganizacaoRepository.save(any())).thenReturn(admin);
    }

    private static AtualizarAdminOrgCompletoRequest request(String nome, String cargo, String email,
                                                            String nomeClinica, String razao, String cnpj,
                                                            String tipo, String emailClinica, String telefone,
                                                            String cep, String uf, String municipio, String bairro,
                                                            String rua, Long numero, String complemento) {
        return new AtualizarAdminOrgCompletoRequest(nome, cargo, email, nomeClinica, razao, cnpj, tipo,
                emailClinica, telefone, cep, uf, municipio, bairro, rua, numero, complemento);
    }

    @Test
    @DisplayName("Todos os campos informados são aplicados e o CNPJ é limpo")
    void aplicaTodosOsCampos() {
        service.atualizarAdminOrgCompleto(ADMIN_ID, request("Novo", "Gerente", "n@n.com", "Nova Clinica", "Nova Razao",
                "12.345.678/0001-00", "HOSPITAL", "nc@c.com", "1199", "222", "RJ", "Rio", "Copa", "Rua B", 2L, "apto"));

        assertThat(admin.getNome()).isEqualTo("Novo");
        assertThat(admin.getCargo()).isEqualTo("Gerente");
        assertThat(admin.getEmail()).isEqualTo("n@n.com");
        assertThat(organizacao.getNome()).isEqualTo("Nova Clinica");
        assertThat(organizacao.getRazaoSocial()).isEqualTo("Nova Razao");
        assertThat(organizacao.getCnpj()).isEqualTo("12345678000100");
        assertThat(organizacao.getTipo()).isEqualTo(TipoOrganizacao.HOSPITAL);
        assertThat(organizacao.getEmail()).isEqualTo("nc@c.com");
        assertThat(organizacao.getTelefone()).isEqualTo("1199");
        assertThat(endereco.getEndCep()).isEqualTo("222");
        assertThat(endereco.getEndUF()).isEqualTo("RJ");
        assertThat(endereco.getEndMunicipio()).isEqualTo("Rio");
        assertThat(endereco.getEndBairro()).isEqualTo("Copa");
        assertThat(endereco.getEndRua()).isEqualTo("Rua B");
        assertThat(endereco.getEndNumero()).isEqualTo(2L);
        assertThat(endereco.getEndComplemento()).isEqualTo("apto");
        verify(organizacaoRepository).save(organizacao);
        verify(enderecoRepository).save(endereco);
        verify(cacheEvictionService).evictPerfilEUsuariosAgrupados(20L, 1L);
    }

    @Test
    @DisplayName("Textos em branco são ignorados onde há checagem de branco; vazio é aceito onde só há checagem de nulo")
    void brancoVersusNulo() {
        service.atualizarAdminOrgCompleto(ADMIN_ID, request("  ", "", " ", "  ", "", "  ", " ", "", "",
                "", "", "", "", "", null, ""));

        // Ignoram branco
        assertThat(admin.getNome()).isEqualTo("Admin");
        assertThat(admin.getCargo()).isEqualTo("Cargo");
        assertThat(admin.getEmail()).isEqualTo("a@a.com");
        assertThat(organizacao.getNome()).isEqualTo("Clinica");
        assertThat(organizacao.getCnpj()).isEqualTo("123");
        assertThat(organizacao.getTipo()).isEqualTo(TipoOrganizacao.CLINICA);
        // Aceitam vazio (só nulo é ignorado)
        assertThat(organizacao.getRazaoSocial()).isEmpty();
        assertThat(organizacao.getEmail()).isEmpty();
        assertThat(organizacao.getTelefone()).isEmpty();
        assertThat(endereco.getEndCep()).isEmpty();
        assertThat(endereco.getEndUF()).isEmpty();
        assertThat(endereco.getEndMunicipio()).isEmpty();
        assertThat(endereco.getEndBairro()).isEmpty();
        assertThat(endereco.getEndRua()).isEmpty();
        assertThat(endereco.getEndComplemento()).isEmpty();
        assertThat(endereco.getEndNumero()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Organização sem endereço não chama o repositório de endereço")
    void semEndereco() {
        organizacao.setEndereco(null);

        service.atualizarAdminOrgCompleto(ADMIN_ID, request(null, null, null, "Outra", null, null, null, null, null,
                "999", null, null, null, null, null, null));

        assertThat(organizacao.getNome()).isEqualTo("Outra");
        verify(organizacaoRepository).save(organizacao);
        verify(enderecoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Admin sem organização só atualiza os dados do admin e evict usa organização nula")
    void semOrganizacao() {
        admin.setOrganizacao(null);

        service.atualizarAdminOrgCompleto(ADMIN_ID, request("Novo", null, null, "Outra", null, null, null, null, null,
                null, null, null, null, null, null, null));

        assertThat(admin.getNome()).isEqualTo("Novo");
        verify(organizacaoRepository, never()).save(any());
        verify(enderecoRepository, never()).save(any());
        verify(cacheEvictionService).evictPerfilEUsuariosAgrupados(20L, null);
    }

    @Test
    @DisplayName("Tipo de clínica inválido lança IllegalArgumentException")
    void tipoInvalido() {
        assertThatThrownBy(() -> service.atualizarAdminOrgCompleto(ADMIN_ID,
                request(null, null, null, null, null, null, "INEXISTENTE", null, null,
                        null, null, null, null, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
