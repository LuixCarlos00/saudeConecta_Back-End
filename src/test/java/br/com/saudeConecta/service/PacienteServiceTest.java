package br.com.saudeConecta.service;

import br.com.saudeConecta.domain.endereco.Endereco;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.paciente.Paciente;
import br.com.saudeConecta.infra.tenant.TenantHelper;
import br.com.saudeConecta.infrastructure.persistence.repository.EnderecoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.OrganizacaoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.PacienteRepository;
import br.com.saudeConecta.presentation.dto.paciente.AtualizarPacienteRequest;
import br.com.saudeConecta.presentation.dto.paciente.CadastrarPacienteCompletoRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PacienteService - Testes de gerenciamento de pacientes")
class PacienteServiceTest {

    @Mock
    private PacienteRepository pacienteRepository;

    @Mock
    private EnderecoRepository enderecoRepository;

    @Mock
    private OrganizacaoRepository organizacaoRepository;

    @Mock
    private TenantHelper tenantHelper;

    @Mock
    private HistoricoDadosPessoaisService historicoDadosPessoaisService;

    @InjectMocks
    private PacienteService pacienteService;

    private static final Long ORG_ID = 10L;
    private static final Long PACIENTE_ID = 1L;

    private Organizacao organizacaoMock;
    private Endereco enderecoMock;
    private Paciente pacienteMock;

    @BeforeEach
    void setUp() {
        organizacaoMock = Organizacao.builder().id(ORG_ID).nome("Clinica Teste").build();

        enderecoMock = Endereco.builder()
                .endCodigo(100L)
                .endRua("Rua das Flores")
                .endNumero(42L)
                .endBairro("Centro")
                .endMunicipio("São Paulo")
                .endUF("SP")
                .endCep("01310-000")
                .build();

        pacienteMock = Paciente.builder()
                .paciCodigo(PACIENTE_ID)
                .organizacao(organizacaoMock)
                .paciNome("João Silva")
                .paciSexo("M")
                .paciCpf("12345678901")
                .paciEmail("joao@email.com")
                .paciTelefone("11999990000")
                .paciStatus("ATIVO")
                .endereco(enderecoMock)
                .build();
    }

    // ========== bloquearPacientebyOrg ==========

    @Nested
    @DisplayName("bloquearPacientebyOrg")
    class BloquearPaciente {

        @Test
        @DisplayName("Deve inativar paciente quando status = 0")
        void deveInativarPacienteQuandoStatus0() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(pacienteRepository.findByPaciCodigoAndOrganizacao_IdWithEndereco(PACIENTE_ID, ORG_ID))
                    .thenReturn(Optional.of(pacienteMock));

            pacienteService.bloquearPacientebyOrg(PACIENTE_ID, 0);

            assertThat(pacienteMock.getPaciStatus()).isEqualTo("INATIVO");
            verify(pacienteRepository).save(pacienteMock);
        }

        @Test
        @DisplayName("Deve ativar paciente quando status = 1")
        void deveAtivarPacienteQuandoStatus1() {
            pacienteMock.setPaciStatus("INATIVO");
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(pacienteRepository.findByPaciCodigoAndOrganizacao_IdWithEndereco(PACIENTE_ID, ORG_ID))
                    .thenReturn(Optional.of(pacienteMock));

            pacienteService.bloquearPacientebyOrg(PACIENTE_ID, 1);

            assertThat(pacienteMock.getPaciStatus()).isEqualTo("ATIVO");
            verify(pacienteRepository).save(pacienteMock);
        }

        @Test
        @DisplayName("Deve lançar IllegalArgumentException quando paciente não for encontrado")
        void deveLancarExcecaoQuandoPacienteNaoEncontrado() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(pacienteRepository.findByPaciCodigoAndOrganizacao_IdWithEndereco(99L, ORG_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> pacienteService.bloquearPacientebyOrg(99L, 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Paciente não encontrado");
        }
    }

    // ========== deletarPacientebyOrg ==========

    @Nested
    @DisplayName("deletarPacientebyOrg")
    class DeletarPaciente {

        @Test
        @DisplayName("Deve deletar paciente e endereço com sucesso")
        void deveDeletarPacienteEEndereco() throws Exception {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(pacienteRepository.findByPaciCodigoAndOrganizacao_IdWithEndereco(PACIENTE_ID, ORG_ID))
                    .thenReturn(Optional.of(pacienteMock));

            pacienteService.deletarPacientebyOrg(PACIENTE_ID);

            verify(pacienteRepository).deleteById(PACIENTE_ID);
            verify(enderecoRepository).deleteById(enderecoMock.getEndCodigo());
        }

        @Test
        @DisplayName("Deve deletar paciente sem endereço sem lançar exceção")
        void deveDeletarPacienteSemEndereco() throws Exception {
            pacienteMock.setEndereco(null);
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(pacienteRepository.findByPaciCodigoAndOrganizacao_IdWithEndereco(PACIENTE_ID, ORG_ID))
                    .thenReturn(Optional.of(pacienteMock));

            assertThatCode(() -> pacienteService.deletarPacientebyOrg(PACIENTE_ID))
                    .doesNotThrowAnyException();

            verify(pacienteRepository).deleteById(PACIENTE_ID);
            verify(enderecoRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando ID for nulo")
        void deveLancarExcecaoParaIdNulo() {
            assertThatThrownBy(() -> pacienteService.deletarPacientebyOrg(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ID inválido");
        }

        @Test
        @DisplayName("Deve lançar exceção quando ID for zero ou negativo")
        void deveLancarExcecaoParaIdInvalido() {
            assertThatThrownBy(() -> pacienteService.deletarPacientebyOrg(0L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ID inválido");
        }

        @Test
        @DisplayName("Deve lançar exceção quando paciente não for encontrado para exclusão")
        void deveLancarExcecaoQuandoPacienteNaoEncontradoParaExclusao() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(pacienteRepository.findByPaciCodigoAndOrganizacao_IdWithEndereco(99L, ORG_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> pacienteService.deletarPacientebyOrg(99L))
                    .isInstanceOf(Exception.class)
                    .hasMessageContaining("Registro não encontrado");
        }
    }

    // ========== buscarListaPacientesPorNomeComFiltro ==========

    @Nested
    @DisplayName("buscarListaPacientesPorNomeComFiltro")
    class BuscarPorNome {

        @Test
        @DisplayName("Deve buscar apenas ativos quando filtro for ATIVO")
        void deveBuscarApenasAtivosComFiltroAtivo() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(pacienteRepository.findByOrganizacaoIdAndNomeContainingWithFiltro(ORG_ID, "João", "ATIVO", "ATIVO"))
                    .thenReturn(List.of(pacienteMock));

            List<Paciente> resultado = pacienteService.buscarListaPacientesPorNomeComFiltro("João", "ATIVO");

            assertThat(resultado).hasSize(1);
            verify(pacienteRepository).findByOrganizacaoIdAndNomeContainingWithFiltro(ORG_ID, "João", "ATIVO", "ATIVO");
            verify(pacienteRepository, never()).findByOrganizacaoIdAndNomeContaining(any(), any());
        }

        @Test
        @DisplayName("Deve buscar todos quando filtro for ALL")
        void deveBuscarTodosComFiltroAll() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(pacienteRepository.findByOrganizacaoIdAndNomeContaining(ORG_ID, "João"))
                    .thenReturn(List.of(pacienteMock));

            List<Paciente> resultado = pacienteService.buscarListaPacientesPorNomeComFiltro("João", "ALL");

            assertThat(resultado).hasSize(1);
            verify(pacienteRepository).findByOrganizacaoIdAndNomeContaining(ORG_ID, "João");
            verify(pacienteRepository, never())
                    .findByOrganizacaoIdAndNomeContainingWithFiltro(any(), any(), any(), any());
        }
    }

    // ========== buscarTodosPacientesComFiltro ==========

    @Nested
    @DisplayName("buscarTodosPacientesComFiltro")
    class BuscarTodos {

        @Test
        @DisplayName("Deve retornar apenas ativos quando filtro for ATIVO")
        void deveRetornarApenasAtivosComFiltroAtivo() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(pacienteRepository.findByOrganizacao_IdWithFiltro(ORG_ID, "ATIVO", "ATIVO"))
                    .thenReturn(List.of(pacienteMock));

            List<Paciente> resultado = pacienteService.buscarTodosPacientesComFiltro("ATIVO");

            assertThat(resultado).hasSize(1);
            verify(pacienteRepository).findByOrganizacao_IdWithFiltro(ORG_ID, "ATIVO", "ATIVO");
        }

        @Test
        @DisplayName("Deve retornar todos quando filtro for ALL")
        void deveRetornarTodosComFiltroAll() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(pacienteRepository.findByOrganizacao_Id(ORG_ID))
                    .thenReturn(List.of(pacienteMock));

            List<Paciente> resultado = pacienteService.buscarTodosPacientesComFiltro("ALL");

            assertThat(resultado).hasSize(1);
            verify(pacienteRepository).findByOrganizacao_Id(ORG_ID);
        }
    }

    // ========== existeCpfNoTenant ==========

    @Nested
    @DisplayName("existeCpfNoTenant")
    class ExisteCpf {

        @Test
        @DisplayName("Deve retornar true quando CPF já existe no tenant")
        void deveRetornarTrueQuandoCpfExiste() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(pacienteRepository.existsByPaciCpfAndOrganizacao_Id("12345678901", ORG_ID))
                    .thenReturn(true);

            boolean resultado = pacienteService.existeCpfNoTenant("12345678901");

            assertThat(resultado).isTrue();
        }

        @Test
        @DisplayName("Deve retornar false quando CPF não existe no tenant")
        void deveRetornarFalseQuandoCpfNaoExiste() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(pacienteRepository.existsByPaciCpfAndOrganizacao_Id("99999999999", ORG_ID))
                    .thenReturn(false);

            boolean resultado = pacienteService.existeCpfNoTenant("99999999999");

            assertThat(resultado).isFalse();
        }
    }

    // ========== cadastrarPacientebyOrg ==========

    @Nested
    @DisplayName("cadastrarPacientebyOrg")
    class CadastrarPaciente {

        @Test
        @DisplayName("Deve cadastrar paciente com sucesso quando CPF não existir")
        void deveCadastrarComSucesso() {
            CadastrarPacienteCompletoRequest request = new CadastrarPacienteCompletoRequest(
                    "Maria Souza", "F", LocalDate.of(1990, 5, 20),
                    "111.222.333-44", "MG-123456", "maria@email.com", "11988880000",
                    "Brasileira", "SP", "São Paulo", "Vila Madalena",
                    "01414-000", "Rua Wisard", 150, null
            );

            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(pacienteRepository.existsByPaciCpfAndOrganizacao_Id("11122233344", ORG_ID))
                    .thenReturn(false);
            when(organizacaoRepository.findById(ORG_ID)).thenReturn(Optional.of(organizacaoMock));
            when(enderecoRepository.save(any())).thenReturn(enderecoMock);
            when(pacienteRepository.save(any())).thenReturn(pacienteMock);

            Paciente resultado = pacienteService.cadastrarPacientebyOrg(request);

            assertThat(resultado).isNotNull();
            verify(enderecoRepository).save(any());
            verify(pacienteRepository).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando CPF já cadastrado no tenant")
        void deveLancarExcecaoQuandoCpfDuplicado() {
            CadastrarPacienteCompletoRequest request = new CadastrarPacienteCompletoRequest(
                    "Maria Souza", "F", LocalDate.of(1990, 5, 20),
                    "111.222.333-44", null, "maria@email.com", "11988880000",
                    null, null, null, null, null, null, null, null
            );

            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(pacienteRepository.existsByPaciCpfAndOrganizacao_Id("11122233344", ORG_ID))
                    .thenReturn(true);

            assertThatThrownBy(() -> pacienteService.cadastrarPacientebyOrg(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("CPF já cadastrado");
        }

        @Test
        @DisplayName("Deve cadastrar quando CPF for nulo (sem validação de duplicidade)")
        void deveCadastrarQuandoCpfNulo() {
            CadastrarPacienteCompletoRequest request = new CadastrarPacienteCompletoRequest(
                    "Sem CPF", "M", null,
                    null, null, null, null,
                    null, null, null, null, null, null, null, null
            );

            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(organizacaoRepository.findById(ORG_ID)).thenReturn(Optional.of(organizacaoMock));
            when(enderecoRepository.save(any())).thenReturn(enderecoMock);
            when(pacienteRepository.save(any())).thenReturn(pacienteMock);

            Paciente resultado = pacienteService.cadastrarPacientebyOrg(request);

            assertThat(resultado).isNotNull();
            verify(pacienteRepository, never()).existsByPaciCpfAndOrganizacao_Id(any(), any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando organização não encontrada")
        void deveLancarExcecaoQuandoOrgNaoEncontrada() {
            CadastrarPacienteCompletoRequest request = new CadastrarPacienteCompletoRequest(
                    "Paciente Teste", "M", null,
                    null, null, null, null,
                    null, null, null, null, null, null, null, null
            );

            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(organizacaoRepository.findById(ORG_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> pacienteService.cadastrarPacientebyOrg(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("organizacao não encontrada");
        }
    }

    // ========== atualizarPacientebyOrg ==========

    @Nested
    @DisplayName("atualizarPacientebyOrg")
    class AtualizarPaciente {

        @Test
        @DisplayName("Deve atualizar dados do paciente com endereço existente")
        void deveAtualizarComEndereco() {
            AtualizarPacienteRequest dados = new AtualizarPacienteRequest(
                    "João Atualizado", "M", LocalDate.of(1985, 3, 10),
                    null, "MG-999999", "joao.novo@email.com", "11977770000",
                    "Brasileira", "RJ", "Rio de Janeiro", "Copacabana",
                    "22010-000", "Av. Atlântica", 500, "Apto 12"
            );

            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(tenantHelper.getCurrentUserId()).thenReturn(99L);
            when(pacienteRepository.findByPaciCodigoAndOrganizacao_IdWithEndereco(PACIENTE_ID, ORG_ID))
                    .thenReturn(Optional.of(pacienteMock));
            when(enderecoRepository.save(any())).thenReturn(enderecoMock);
            when(pacienteRepository.save(any())).thenReturn(pacienteMock);

            Paciente resultado = pacienteService.atualizarPacientebyOrg(PACIENTE_ID, dados);

            assertThat(resultado).isNotNull();
            verify(enderecoRepository).save(any());
            verify(pacienteRepository).save(any());
            verify(historicoDadosPessoaisService).registrarAlteracoesDeObjeto(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Deve atualizar paciente sem endereço (enderecoAtualizado fica nulo)")
        void deveAtualizarSemEndereco() {
            pacienteMock.setEndereco(null);

            AtualizarPacienteRequest dados = new AtualizarPacienteRequest(
                    "João Sem End", "M", null,
                    null, null, null, null,
                    null, null, null, null, null, null, null, null
            );

            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(tenantHelper.getCurrentUserId()).thenReturn(99L);
            when(pacienteRepository.findByPaciCodigoAndOrganizacao_IdWithEndereco(PACIENTE_ID, ORG_ID))
                    .thenReturn(Optional.of(pacienteMock));
            when(pacienteRepository.save(any())).thenReturn(pacienteMock);

            Paciente resultado = pacienteService.atualizarPacientebyOrg(PACIENTE_ID, dados);

            assertThat(resultado).isNotNull();
            verify(enderecoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando paciente não encontrado ao atualizar")
        void deveLancarExcecaoQuandoPacienteNaoEncontrado() {
            AtualizarPacienteRequest dados = new AtualizarPacienteRequest(
                    null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null
            );

            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(pacienteRepository.findByPaciCodigoAndOrganizacao_IdWithEndereco(99L, ORG_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> pacienteService.atualizarPacientebyOrg(99L, dados))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Paciente não encontrado");
        }

        @Test
        @DisplayName("Deve manter dados anteriores quando campos do request forem nulos")
        void deveManterDadosAnterioresParaCamposNulos() {
            AtualizarPacienteRequest dados = new AtualizarPacienteRequest(
                    null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null
            );

            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(tenantHelper.getCurrentUserId()).thenReturn(99L);
            when(pacienteRepository.findByPaciCodigoAndOrganizacao_IdWithEndereco(PACIENTE_ID, ORG_ID))
                    .thenReturn(Optional.of(pacienteMock));
            when(enderecoRepository.save(any())).thenReturn(enderecoMock);
            when(pacienteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Paciente resultado = pacienteService.atualizarPacientebyOrg(PACIENTE_ID, dados);

            assertThat(resultado.getPaciNome()).isEqualTo("João Silva");
            assertThat(resultado.getPaciEmail()).isEqualTo("joao@email.com");
        }
    }

    // ========== buscarListaPacientesPorCPFComFiltro ==========

    @Nested
    @DisplayName("buscarListaPacientesPorCPFComFiltro")
    class BuscarPorCPF {

        @Test
        @DisplayName("Deve buscar apenas ativos quando filtro for ATIVO")
        void deveBuscarApenasAtivosComFiltroAtivo() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(pacienteRepository.findByOrganizacaoIdAndCpfContainingWithFiltro(ORG_ID, "123", "ATIVO", "ATIVO"))
                    .thenReturn(List.of(pacienteMock));

            List<Paciente> resultado = pacienteService.buscarListaPacientesPorCPFComFiltro("123", "ATIVO");

            assertThat(resultado).hasSize(1);
            verify(pacienteRepository).findByOrganizacaoIdAndCpfContainingWithFiltro(ORG_ID, "123", "ATIVO", "ATIVO");
            verify(pacienteRepository, never()).findByOrganizacaoIdAndCpfContaining(any(), any());
        }

        @Test
        @DisplayName("Deve buscar todos quando filtro for ALL")
        void deveBuscarTodosComFiltroAll() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(pacienteRepository.findByOrganizacaoIdAndCpfContaining(ORG_ID, "123"))
                    .thenReturn(List.of(pacienteMock));

            List<Paciente> resultado = pacienteService.buscarListaPacientesPorCPFComFiltro("123", "ALL");

            assertThat(resultado).hasSize(1);
            verify(pacienteRepository).findByOrganizacaoIdAndCpfContaining(ORG_ID, "123");
            verify(pacienteRepository, never())
                    .findByOrganizacaoIdAndCpfContainingWithFiltro(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Deve retornar lista vazia quando não há pacientes com o CPF")
        void deveRetornarListaVaziaQuandoNaoEncontrado() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(pacienteRepository.findByOrganizacaoIdAndCpfContaining(ORG_ID, "000"))
                    .thenReturn(List.of());

            List<Paciente> resultado = pacienteService.buscarListaPacientesPorCPFComFiltro("000", "ALL");

            assertThat(resultado).isEmpty();
        }
    }

    // ========== buscarListaPacientesPorRGComFiltro ==========

    @Nested
    @DisplayName("buscarListaPacientesPorRGComFiltro")
    class BuscarPorRG {

        @Test
        @DisplayName("Deve buscar apenas ativos quando filtro for ATIVO")
        void deveBuscarApenasAtivosComFiltroAtivo() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(pacienteRepository.findByOrganizacaoIdAndRgContainingWithFiltro(ORG_ID, "MG-1", "ATIVO", "ATIVO"))
                    .thenReturn(List.of(pacienteMock));

            List<Paciente> resultado = pacienteService.buscarListaPacientesPorRGComFiltro("MG-1", "ATIVO");

            assertThat(resultado).hasSize(1);
            verify(pacienteRepository).findByOrganizacaoIdAndRgContainingWithFiltro(ORG_ID, "MG-1", "ATIVO", "ATIVO");
            verify(pacienteRepository, never()).findByOrganizacaoIdAndRgContaining(any(), any());
        }

        @Test
        @DisplayName("Deve buscar todos quando filtro for ALL")
        void deveBuscarTodosComFiltroAll() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(pacienteRepository.findByOrganizacaoIdAndRgContaining(ORG_ID, "MG-1"))
                    .thenReturn(List.of(pacienteMock));

            List<Paciente> resultado = pacienteService.buscarListaPacientesPorRGComFiltro("MG-1", "ALL");

            assertThat(resultado).hasSize(1);
            verify(pacienteRepository).findByOrganizacaoIdAndRgContaining(ORG_ID, "MG-1");
            verify(pacienteRepository, never())
                    .findByOrganizacaoIdAndRgContainingWithFiltro(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Deve retornar lista vazia quando não há pacientes com o RG")
        void deveRetornarListaVaziaQuandoNaoEncontrado() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(pacienteRepository.findByOrganizacaoIdAndRgContaining(ORG_ID, "ZZ-0"))
                    .thenReturn(List.of());

            List<Paciente> resultado = pacienteService.buscarListaPacientesPorRGComFiltro("ZZ-0", "ALL");

            assertThat(resultado).isEmpty();
        }
    }

    // ========== buscarListaPacientesPorTelefoneComFiltro ==========

    @Nested
    @DisplayName("buscarListaPacientesPorTelefoneComFiltro")
    class BuscarPorTelefone {

        @Test
        @DisplayName("Deve buscar apenas ativos quando filtro for ATIVO")
        void deveBuscarApenasAtivosComFiltroAtivo() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(pacienteRepository.findByOrganizacaoIdAndTelefoneContainingWithFiltro(ORG_ID, "119", "ATIVO", "ATIVO"))
                    .thenReturn(List.of(pacienteMock));

            List<Paciente> resultado = pacienteService.buscarListaPacientesPorTelefoneComFiltro("119", "ATIVO");

            assertThat(resultado).hasSize(1);
            verify(pacienteRepository).findByOrganizacaoIdAndTelefoneContainingWithFiltro(ORG_ID, "119", "ATIVO", "ATIVO");
            verify(pacienteRepository, never()).findByOrganizacaoIdAndTelefoneContaining(any(), any());
        }

        @Test
        @DisplayName("Deve buscar todos quando filtro for ALL")
        void deveBuscarTodosComFiltroAll() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(pacienteRepository.findByOrganizacaoIdAndTelefoneContaining(ORG_ID, "119"))
                    .thenReturn(List.of(pacienteMock));

            List<Paciente> resultado = pacienteService.buscarListaPacientesPorTelefoneComFiltro("119", "ALL");

            assertThat(resultado).hasSize(1);
            verify(pacienteRepository).findByOrganizacaoIdAndTelefoneContaining(ORG_ID, "119");
            verify(pacienteRepository, never())
                    .findByOrganizacaoIdAndTelefoneContainingWithFiltro(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Deve retornar lista vazia quando não há pacientes com o telefone")
        void deveRetornarListaVaziaQuandoNaoEncontrado() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(pacienteRepository.findByOrganizacaoIdAndTelefoneContaining(ORG_ID, "0000"))
                    .thenReturn(List.of());

            List<Paciente> resultado = pacienteService.buscarListaPacientesPorTelefoneComFiltro("0000", "ALL");

            assertThat(resultado).isEmpty();
        }
    }
}
