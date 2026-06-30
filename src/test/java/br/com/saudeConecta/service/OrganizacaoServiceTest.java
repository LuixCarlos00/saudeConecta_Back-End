package br.com.saudeConecta.service;

import br.com.saudeConecta.domain.endereco.Endereco;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.organizacao.StatusOrganizacao;
import br.com.saudeConecta.domain.organizacao.TipoOrganizacao;
import br.com.saudeConecta.infra.tenant.TenantHelper;
import br.com.saudeConecta.infrastructure.persistence.repository.EnderecoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.OrganizacaoRepository;
import br.com.saudeConecta.presentation.dto.organizacao.AtualizarOrganizacaoRequest;
import br.com.saudeConecta.presentation.dto.organizacao.CriarOrganizacaoRequest;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrganizacaoService - Testes unitários")
class OrganizacaoServiceTest {

    @Mock private OrganizacaoRepository organizacaoRepository;
    @Mock private EnderecoRepository enderecoRepository;
    @Mock private TenantHelper tenantHelper;

    @InjectMocks
    private OrganizacaoService organizacaoService;

    private static final Long ORG_ID = 1L;

    private Organizacao organizacao;

    @BeforeEach
    void setUp() {
        organizacao = Organizacao.builder()
                .nome("Clínica Teste").razaoSocial("Clínica Teste LTDA")
                .cnpj("12345678000100").tipo(TipoOrganizacao.CLINICA)
                .email("clinica@teste.com").telefone("11999999999")
                .status(StatusOrganizacao.ATIVO)
                .build();
        organizacao.setId(ORG_ID);
    }

    // ========== buscarAtual ==========

    @Nested
    @DisplayName("buscarAtual")
    class BuscarAtual {

        @Test
        @DisplayName("Deve retornar organização atual quando tenant estiver presente")
        void deveRetornarOrgAtualQuandoTenantPresente() {
            when(tenantHelper.hasTenant()).thenReturn(true);
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(organizacaoRepository.findById(ORG_ID)).thenReturn(Optional.of(organizacao));

            Optional<Organizacao> resultado = organizacaoService.buscarAtual();

            assertThat(resultado).isPresent();
            assertThat(resultado.get().getId()).isEqualTo(ORG_ID);
        }

        @Test
        @DisplayName("Deve retornar vazio quando não houver tenant (Super Admin)")
        void deveRetornarVazioQuandoSemTenant() {
            when(tenantHelper.hasTenant()).thenReturn(false);

            Optional<Organizacao> resultado = organizacaoService.buscarAtual();

            assertThat(resultado).isEmpty();
            verify(organizacaoRepository, never()).findById(any());
        }
    }

    // ========== listarTodas ==========

    @Nested
    @DisplayName("listarTodas e listarAtivas")
    class Listagem {

        @Test
        @DisplayName("Deve listar todas as organizações")
        void deveListarTodasOrganizacoes() {
            when(organizacaoRepository.findAll()).thenReturn(List.of(organizacao));

            List<Organizacao> resultado = organizacaoService.listarTodas();

            assertThat(resultado).hasSize(1);
        }

        @Test
        @DisplayName("Deve listar apenas organizações ativas")
        void deveListarApenasAtivas() {
            when(organizacaoRepository.findByStatus(StatusOrganizacao.ATIVO)).thenReturn(List.of(organizacao));

            List<Organizacao> resultado = organizacaoService.listarAtivas();

            assertThat(resultado).hasSize(1);
            verify(organizacaoRepository).findByStatus(StatusOrganizacao.ATIVO);
        }
    }

    // ========== buscarPorId e buscarPorCnpj ==========

    @Nested
    @DisplayName("buscarPorId e buscarPorCnpj")
    class BuscasPorId {

        @Test
        @DisplayName("Deve retornar organização pelo ID")
        void deveRetornarOrganizacaoPorId() {
            when(organizacaoRepository.findById(ORG_ID)).thenReturn(Optional.of(organizacao));

            Optional<Organizacao> resultado = organizacaoService.buscarPorId(ORG_ID);

            assertThat(resultado).isPresent();
        }

        @Test
        @DisplayName("Deve retornar organização pelo CNPJ")
        void deveRetornarOrganizacaoPorCnpj() {
            when(organizacaoRepository.findByCnpj("12345678000100")).thenReturn(Optional.of(organizacao));

            Optional<Organizacao> resultado = organizacaoService.buscarPorCnpj("12345678000100");

            assertThat(resultado).isPresent();
            assertThat(resultado.get().getCnpj()).isEqualTo("12345678000100");
        }
    }

    // ========== criar ==========

    @Nested
    @DisplayName("criar")
    class Criar {

        private CriarOrganizacaoRequest request;

        @BeforeEach
        void setUp() {
            request = new CriarOrganizacaoRequest(
                    "Nova Clínica", "Nova Razao Social LTDA", "98765432000100",
                    TipoOrganizacao.CLINICA, "nova@clinica.com", "11888888888",
                    null, null
            );
        }

        @Test
        @DisplayName("Deve criar organização com sucesso sem endereço")
        void deveCriarOrgComSucesso() {
            when(organizacaoRepository.existsByCnpj("98765432000100")).thenReturn(false);
            when(organizacaoRepository.save(any(Organizacao.class))).thenReturn(organizacao);

            Organizacao resultado = organizacaoService.criar(request);

            assertThat(resultado).isNotNull();
            verify(organizacaoRepository).save(any(Organizacao.class));
        }

        @Test
        @DisplayName("Deve criar organização com endereço existente")
        void deveCriarOrgComEndereco() {
            CriarOrganizacaoRequest requestComEndereco = new CriarOrganizacaoRequest(
                    "Nova Clínica", "Nova Razao", "98765432000100",
                    TipoOrganizacao.CLINICA, "nova@clinica.com", "11888888888",
                    null, 5L
            );
            Endereco endereco = Endereco.builder().endCep("01310-100").build();

            when(organizacaoRepository.existsByCnpj("98765432000100")).thenReturn(false);
            when(enderecoRepository.findById(5L)).thenReturn(Optional.of(endereco));
            when(organizacaoRepository.save(any())).thenReturn(organizacao);

            Organizacao resultado = organizacaoService.criar(requestComEndereco);

            assertThat(resultado).isNotNull();
            verify(enderecoRepository).findById(5L);
        }

        @Test
        @DisplayName("Deve lançar exceção quando CNPJ já estiver cadastrado")
        void deveLancarExcecaoQuandoCnpjJaCadastrado() {
            when(organizacaoRepository.existsByCnpj("98765432000100")).thenReturn(true);

            assertThatThrownBy(() -> organizacaoService.criar(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("CNPJ já cadastrado");
        }

        @Test
        @DisplayName("Deve lançar exceção quando endereço não for encontrado")
        void deveLancarExcecaoQuandoEnderecoNaoEncontrado() {
            CriarOrganizacaoRequest requestComEndereco = new CriarOrganizacaoRequest(
                    "Nova Clínica", "Nova Razao", "98765432000100",
                    TipoOrganizacao.CLINICA, "nova@clinica.com", "11888888888",
                    null, 999L
            );

            when(organizacaoRepository.existsByCnpj("98765432000100")).thenReturn(false);
            when(enderecoRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> organizacaoService.criar(requestComEndereco))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Endereço não encontrado");
        }
    }

    // ========== atualizar ==========

    @Nested
    @DisplayName("atualizar")
    class Atualizar {

        private AtualizarOrganizacaoRequest request;

        @BeforeEach
        void setUp() {
            request = new AtualizarOrganizacaoRequest(
                    "Nome Atualizado", null, null, "novo@email.com", null, null, null
            );
        }

        @Test
        @DisplayName("Deve atualizar organização com sucesso")
        void deveAtualizarOrgComSucesso() {
            when(organizacaoRepository.findById(ORG_ID)).thenReturn(Optional.of(organizacao));
            when(organizacaoRepository.save(any())).thenReturn(organizacao);

            Organizacao resultado = organizacaoService.atualizar(ORG_ID, request);

            assertThat(resultado).isNotNull();
            assertThat(organizacao.getNome()).isEqualTo("Nome Atualizado");
            assertThat(organizacao.getEmail()).isEqualTo("novo@email.com");
        }

        @Test
        @DisplayName("Deve lançar exceção quando organização não existir")
        void deveLancarExcecaoQuandoOrgNaoExistir() {
            when(organizacaoRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> organizacaoService.atualizar(999L, request))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Organização não encontrada");
        }

        @Test
        @DisplayName("Deve lançar exceção quando novo CNPJ já estiver cadastrado em outra org")
        void deveLancarExcecaoQuandoNovoCnpjJaExistir() {
            AtualizarOrganizacaoRequest requestComCnpj = new AtualizarOrganizacaoRequest(
                    null, null, "99999999000199", null, null, null, null
            );

            when(organizacaoRepository.findById(ORG_ID)).thenReturn(Optional.of(organizacao));
            when(organizacaoRepository.existsByCnpj("99999999000199")).thenReturn(true);

            assertThatThrownBy(() -> organizacaoService.atualizar(ORG_ID, requestComCnpj))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("CNPJ já cadastrado");
        }
    }

    // ========== ativar e inativar ==========

    @Nested
    @DisplayName("ativar e inativar")
    class AtivarInativar {

        @Test
        @DisplayName("Deve ativar organização com sucesso")
        void deveAtivarOrgComSucesso() {
            organizacao.setStatus(StatusOrganizacao.INATIVO);
            when(organizacaoRepository.findById(ORG_ID)).thenReturn(Optional.of(organizacao));

            organizacaoService.ativar(ORG_ID);

            assertThat(organizacao.getStatus()).isEqualTo(StatusOrganizacao.ATIVO);
            verify(organizacaoRepository).save(organizacao);
        }

        @Test
        @DisplayName("Deve inativar organização com sucesso")
        void deveInativarOrgComSucesso() {
            when(organizacaoRepository.findById(ORG_ID)).thenReturn(Optional.of(organizacao));

            organizacaoService.inativar(ORG_ID);

            assertThat(organizacao.getStatus()).isEqualTo(StatusOrganizacao.INATIVO);
            verify(organizacaoRepository).save(organizacao);
        }

        @Test
        @DisplayName("Deve lançar exceção ao tentar ativar org inexistente")
        void deveLancarExcecaoQuandoOrgInexistente() {
            when(organizacaoRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> organizacaoService.ativar(999L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Organização não encontrada");
        }
    }

    // ========== contarAtivas ==========

    @Nested
    @DisplayName("contarAtivas")
    class ContarAtivas {

        @Test
        @DisplayName("Deve retornar contagem de organizações ativas")
        void deveRetornarContagemAtivas() {
            when(organizacaoRepository.countByStatus(StatusOrganizacao.ATIVO)).thenReturn(5L);

            Long resultado = organizacaoService.contarAtivas();

            assertThat(resultado).isEqualTo(5L);
        }
    }
}
