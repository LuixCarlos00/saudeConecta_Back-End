package br.com.saudeConecta.application.prontuario;

import br.com.saudeConecta.application.cache.CacheEvictionService;
import br.com.saudeConecta.application.exception.BusinessException;
import br.com.saudeConecta.application.exception.ResourceNotFoundException;
import br.com.saudeConecta.application.prontuario.dto.CadastrarProntuarioRequest;
import br.com.saudeConecta.config.tenant.TenantContext;
import br.com.saudeConecta.config.tenant.TenantHelper;
import br.com.saudeConecta.domain.consulta.Consulta;
import br.com.saudeConecta.domain.consulta.StatusConsulta;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.paciente.Paciente;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.domain.prontuario.PlanejamentoTerapeutico;
import br.com.saudeConecta.domain.prontuario.Prontuario;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.infrastructure.persistence.repository.AtendimentoRascunhoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.ConsultaRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.PacienteRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.ProfissionalRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.ProntuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProntuarioService - Testes unitários")
class ProntuarioServiceTest {

    private static final Long ORG = 1L;
    /** Usuário (login) do profissional da consulta: o dono do prontuário. */
    private static final Long USUARIO_DONO = 500L;
    private static final Long OUTRA_ORG = 2L;

    @BeforeEach
    void definirTenant() {
        TenantContext.setCurrentTenant(ORG);
        TenantContext.setCurrentUser(USUARIO_DONO);
    }

    @AfterEach
    void limparTenantContext() {
        TenantContext.clear();
    }

    @Mock private ProntuarioRepository prontuarioRepository;
    @Mock private ProfissionalRepository profissionalRepository;
    @Mock private ConsultaRepository consultaRepository;
    @Mock private AtendimentoRascunhoRepository atendimentoRascunhoRepository;
    @Mock private PacienteRepository pacienteRepository;
    @Mock private PlanejamentoTerapeuticoService planejamentoService;
    @Mock private CacheEvictionService cacheEvictionService;
    @Spy private TenantHelper tenantHelper = new TenantHelper();

    @InjectMocks
    private ProntuarioService service;

    // ========== Helpers ==========

    private Profissional criarProfissional(Long id) {
        Profissional p = new Profissional();
        p.setId(id);
        p.setNome("Dr. Silva");
        return p;
    }

    private Consulta criarConsulta(Long id) {
        Consulta c = new Consulta();
        c.setId(id);
        c.setDataHora(LocalDateTime.now());
        c.setStatus(StatusConsulta.AGENDADA);
        Profissional dono = criarProfissional(1L);
        Usuario usuario = new Usuario();
        usuario.setId(USUARIO_DONO);
        dono.setUsuario(usuario);
        c.setProfissional(dono);
        return c;
    }

    private Prontuario criarProntuario(Long id) {
        Prontuario p = new Prontuario();
        p.setProntCodigoProntuario(id);
        p.setConsulta(criarConsulta(10L));
        p.setProfissional(criarProfissional(1L));
        return p;
    }

    private CadastrarProntuarioRequest criarRequest() {
        CadastrarProntuarioRequest req = new CadastrarProntuarioRequest();
        req.setConsulta(10L);
        req.setCodigoMedico(1L);
        req.setQueixaPrincipal("Dor de cabeça");
        req.setAnamnese("Paciente relata...");
        req.setDiagnostico("Enxaqueca");
        req.setPlanejamentos(Collections.emptyList());
        return req;
    }

    private CadastrarProntuarioRequest.PlanejamentoItem criarItemComPaciente(Long pacienteId) {
        CadastrarProntuarioRequest.PlanejamentoItem item = new CadastrarProntuarioRequest.PlanejamentoItem();
        item.setProcedimentoRealizado("Consulta de rotina");
        item.setValor(BigDecimal.valueOf(250.00));
        item.setDataProcedimento("2024-06-01");
        item.setPacienteId(pacienteId);
        return item;
    }

    // ========== cadastrarProntuarioMedico ==========

    @Nested
    @DisplayName("cadastrarProntuarioMedico")
    class CadastrarProntuarioMedico {

        @Test
        @DisplayName("Finalizar apaga o rascunho da consulta depois de marcar REALIZADA")
        void finalizarApagaRascunhoDaConsulta() {
            Consulta consulta = criarConsulta(10L);
            when(profissionalRepository.findByIdAndOrganizacao_Id(1L, ORG)).thenReturn(Optional.of(criarProfissional(1L)));
            when(consultaRepository.buscarComLock(10L, ORG)).thenReturn(Optional.of(consulta));
            when(prontuarioRepository.save(any())).thenReturn(criarProntuario(100L));
            when(atendimentoRascunhoRepository.apagarPorConsulta(10L)).thenReturn(1);

            service.cadastrarProntuarioMedico(criarRequest());

            var ordem = inOrder(consultaRepository, atendimentoRascunhoRepository);
            ordem.verify(consultaRepository).save(consulta);
            ordem.verify(atendimentoRascunhoRepository).apagarPorConsulta(10L);
            assertThat(consulta.getStatus()).isEqualTo(StatusConsulta.REALIZADA);
        }

        @Test
        @DisplayName("Falha ao gravar o prontuário mantém o rascunho")
        void falhaAoGravarProntuarioMantemRascunho() {
            when(profissionalRepository.findByIdAndOrganizacao_Id(1L, ORG)).thenReturn(Optional.of(criarProfissional(1L)));
            when(consultaRepository.buscarComLock(10L, ORG)).thenReturn(Optional.of(criarConsulta(10L)));
            when(prontuarioRepository.save(any())).thenThrow(new org.springframework.dao.DataIntegrityViolationException("falha"));

            var requisicao = criarRequest();
            assertThatThrownBy(() -> service.cadastrarProntuarioMedico(requisicao))
                    .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);

            verify(atendimentoRascunhoRepository, never()).apagarPorConsulta(any());
        }

        @Test
        @DisplayName("Segundo Finalizar da mesma consulta recebe 409 PRONTUARIO_JA_FINALIZADO e não grava nada")
        void segundoFinalizarDaMesmaConsultaRecebe409() {
            when(profissionalRepository.findByIdAndOrganizacao_Id(1L, ORG)).thenReturn(Optional.of(criarProfissional(1L)));
            when(consultaRepository.buscarComLock(10L, ORG)).thenReturn(Optional.of(criarConsulta(10L)));
            when(prontuarioRepository.existsByConsulta_Id(10L)).thenReturn(true);

            var requisicao = criarRequest();
            assertThatThrownBy(() -> service.cadastrarProntuarioMedico(requisicao))
                    .isInstanceOf(br.com.saudeConecta.application.exception.BusinessException.class)
                    .satisfies(ex -> {
                        var be = (br.com.saudeConecta.application.exception.BusinessException) ex;
                        assertThat(be.getStatus()).isEqualTo(org.springframework.http.HttpStatus.CONFLICT);
                        assertThat(be.getErrorCode()).isEqualTo("PRONTUARIO_JA_FINALIZADO");
                    });

            verify(prontuarioRepository, never()).save(any(Prontuario.class));
            verify(consultaRepository, never()).save(any());
            verify(atendimentoRascunhoRepository, never()).apagarPorConsulta(any());
        }

        @Test
        @DisplayName("Deve cadastrar prontuário médico com sucesso")
        void deveCadastrarComSucesso() {
            CadastrarProntuarioRequest request = criarRequest();
            Profissional profissional = criarProfissional(1L);
            Consulta consulta = criarConsulta(10L);
            Prontuario prontuario = criarProntuario(100L);

            when(profissionalRepository.findByIdAndOrganizacao_Id(1L, ORG)).thenReturn(Optional.of(profissional));
            when(consultaRepository.buscarComLock(10L, ORG)).thenReturn(Optional.of(consulta));
            when(prontuarioRepository.save(any())).thenReturn(prontuario);
            when(consultaRepository.save(any())).thenReturn(consulta);

            Prontuario resultado = service.cadastrarProntuarioMedico(request);

            assertThat(resultado).isNotNull();
            assertThat(resultado.getProntCodigoProntuario()).isEqualTo(100L);
            verify(prontuarioRepository).save(any(Prontuario.class));
        }

        @Test
        @DisplayName("Deve lançar exceção quando profissional não encontrado")
        void deveLancarExcecaoQuandoProfissionalNaoEncontrado() {
            CadastrarProntuarioRequest request = criarRequest();
            when(profissionalRepository.findByIdAndOrganizacao_Id(1L, ORG)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.cadastrarProntuarioMedico(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Profissional nao encontrado");
        }

        @Test
        @DisplayName("Deve lançar exceção quando consulta não encontrada")
        void deveLancarExcecaoQuandoConsultaNaoEncontrada() {
            CadastrarProntuarioRequest request = criarRequest();
            when(profissionalRepository.findByIdAndOrganizacao_Id(1L, ORG)).thenReturn(Optional.of(criarProfissional(1L)));
            when(consultaRepository.buscarComLock(10L, ORG)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.cadastrarProntuarioMedico(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Consulta nao encontrada");
        }

        @Test
        @DisplayName("Deve atualizar status da consulta para REALIZADA após cadastro")
        void deveAtualizarStatusConsultaParaRealizada() {
            CadastrarProntuarioRequest request = criarRequest();
            Consulta consulta = criarConsulta(10L);
            Prontuario prontuario = criarProntuario(100L);

            when(profissionalRepository.findByIdAndOrganizacao_Id(1L, ORG)).thenReturn(Optional.of(criarProfissional(1L)));
            when(consultaRepository.buscarComLock(10L, ORG)).thenReturn(Optional.of(consulta));
            when(prontuarioRepository.save(any())).thenReturn(prontuario);
            when(consultaRepository.save(any())).thenReturn(consulta);

            service.cadastrarProntuarioMedico(request);

            assertThat(consulta.getStatus()).isEqualTo(StatusConsulta.REALIZADA);
        }

        @Test
        @DisplayName("Deve cadastrar prontuário com planejamentos quando pacienteId informado")
        void deveCadastrarComPlanejamentosComPaciente() {
            CadastrarProntuarioRequest request = criarRequest();
            request.setPlanejamentos(List.of(criarItemComPaciente(5L)));

            Profissional profissional = criarProfissional(1L);
            Consulta consulta = criarConsulta(10L);
            Prontuario prontuarioSalvo = criarProntuario(100L);
            Paciente paciente = new Paciente();
            paciente.setPaciCodigo(5L);

            when(profissionalRepository.findByIdAndOrganizacao_Id(1L, ORG)).thenReturn(Optional.of(profissional));
            when(consultaRepository.buscarComLock(10L, ORG)).thenReturn(Optional.of(consulta));
            when(prontuarioRepository.save(any())).thenReturn(prontuarioSalvo);
            when(pacienteRepository.findByPaciCodigoAndOrganizacao_IdWithEndereco(5L, ORG)).thenReturn(Optional.of(paciente));
            when(consultaRepository.save(any())).thenReturn(consulta);

            Prontuario resultado = service.cadastrarProntuarioMedico(request);

            assertThat(resultado).isNotNull();
            verify(pacienteRepository).findByPaciCodigoAndOrganizacao_IdWithEndereco(5L, ORG);
            verify(prontuarioRepository, times(2)).save(any());
        }

        @Test
        @DisplayName("Deve cadastrar prontuário com planejamentos quando pacienteId é nulo")
        void deveCadastrarComPlanejamentosSemPaciente() {
            CadastrarProntuarioRequest request = criarRequest();
            request.setPlanejamentos(List.of(criarItemComPaciente(null)));

            Profissional profissional = criarProfissional(1L);
            Consulta consulta = criarConsulta(10L);
            Prontuario prontuarioSalvo = criarProntuario(100L);

            when(profissionalRepository.findByIdAndOrganizacao_Id(1L, ORG)).thenReturn(Optional.of(profissional));
            when(consultaRepository.buscarComLock(10L, ORG)).thenReturn(Optional.of(consulta));
            when(prontuarioRepository.save(any())).thenReturn(prontuarioSalvo);
            when(consultaRepository.save(any())).thenReturn(consulta);

            Prontuario resultado = service.cadastrarProntuarioMedico(request);

            assertThat(resultado).isNotNull();
            verify(pacienteRepository, never()).findByPaciCodigoAndOrganizacao_IdWithEndereco(any(), any());
            verify(prontuarioRepository, times(2)).save(any());
        }
    }

    // ========== buscarPorId ==========

    @Nested
    @DisplayName("buscarPorId")
    class BuscarPorId {

        @Test
        @DisplayName("Deve retornar prontuário quando encontrado")
        void deveRetornarProntuario() {
            Prontuario prontuario = criarProntuario(1L);
            when(prontuarioRepository.findByProntCodigoProntuarioAndConsulta_Organizacao_Id(1L, ORG)).thenReturn(Optional.of(prontuario));

            Prontuario resultado = service.buscarPorId(1L);

            assertThat(resultado).isNotNull();
            assertThat(resultado.getProntCodigoProntuario()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Deve lançar exceção quando prontuário não encontrado")
        void deveLancarExcecaoQuandoNaoEncontrado() {
            when(prontuarioRepository.findByProntCodigoProntuarioAndConsulta_Organizacao_Id(99L, ORG)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.buscarPorId(99L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Prontuario médico nao encontrado");
        }
    }

    // ========== buscarProntuarioById ==========

    @Nested
    @DisplayName("buscarProntuarioById")
    class BuscarProntuarioById {

        @Test
        @DisplayName("Deve retornar prontuário por ID da consulta")
        void deveRetornarPorConsultaId() {
            Prontuario prontuario = criarProntuario(1L);
            when(prontuarioRepository.findByConsultaIdAndOrganizacaoIdWithFetch(10L, ORG)).thenReturn(prontuario);

            Prontuario resultado = service.buscarProntuarioById(10L);

            assertThat(resultado).isNotNull();
        }

        @Test
        @DisplayName("Deve lançar EntityNotFoundException quando prontuário não encontrado por consulta")
        void deveLancarExcecaoQuandoNaoEncontrado() {
            when(prontuarioRepository.findByConsultaIdAndOrganizacaoIdWithFetch(99L, ORG)).thenReturn(null);

            assertThatThrownBy(() -> service.buscarProntuarioById(99L))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    // ========== buscarMaisRecentePorConsulta ==========

    @Nested
    @DisplayName("buscarMaisRecentePorConsulta")
    class BuscarMaisRecente {

        @Test
        @DisplayName("Deve retornar o prontuário mais recente da consulta")
        void deveRetornarMaisRecente() {
            Prontuario prontuario = criarProntuario(1L);
            when(prontuarioRepository.findIdMaisRecentePorConsulta(10L, ORG)).thenReturn(Optional.of(1L));
            when(prontuarioRepository.findByIdWithFullFetch(1L)).thenReturn(Optional.of(prontuario));

            Prontuario resultado = service.buscarMaisRecentePorConsulta(10L);

            assertThat(resultado).isNotNull();
        }

        @Test
        @DisplayName("Deve lançar EntityNotFoundException quando não há prontuário recente")
        void deveLancarExcecaoQuandoNaoEncontrado() {
            when(prontuarioRepository.findIdMaisRecentePorConsulta(99L, ORG)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.buscarMaisRecentePorConsulta(99L))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    // ========== atualizarProntuarioMedico ==========

    @Nested
    @DisplayName("atualizarProntuarioMedico")
    class AtualizarProntuario {

        private PlanejamentoTerapeutico planejamento(Long id, String procedimento, String valor, String status) {
            PlanejamentoTerapeutico p = PlanejamentoTerapeutico.builder()
                    .id(id).procedimentoRealizado(procedimento).valor(new BigDecimal(valor))
                    .dataProcedimento(LocalDate.of(2026, 11, 1)).statusAssinatura(status).build();
            if ("ASSINADO".equals(status)) {
                p.setAssinaturaBase64("data:image/png;base64,AAAA");
                p.setDataAssinatura(LocalDateTime.of(2026, 11, 2, 10, 0));
                p.setIpOrigem("10.0.0.1");
                p.setTokenAssinatura("tok-assinado");
            }
            return p;
        }

        private CadastrarProntuarioRequest.PlanejamentoItem item(Long id, String procedimento, String valor) {
            CadastrarProntuarioRequest.PlanejamentoItem i = criarItemComPaciente(null);
            i.setId(id);
            i.setProcedimentoRealizado(procedimento);
            i.setValor(new BigDecimal(valor));
            return i;
        }

        private Prontuario prontuarioGravado(PlanejamentoTerapeutico... planejamentos) {
            Prontuario prontuario = criarProntuario(1L);
            prontuario.getPlanejamentos().addAll(List.of(planejamentos));
            when(prontuarioRepository.findByProntCodigoProntuarioAndConsulta_Organizacao_Id(1L, ORG))
                    .thenReturn(Optional.of(prontuario));
            return prontuario;
        }

        @Test
        @DisplayName("Prontuário inexistente responde 404")
        void deveLancarExcecaoQuandoNaoEncontrado() {
            when(prontuarioRepository.findByProntCodigoProntuarioAndConsulta_Organizacao_Id(99L, ORG)).thenReturn(Optional.empty());

            var requisicao = criarRequest();
            assertThatThrownBy(() -> service.atualizarProntuarioMedico(99L, requisicao))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Prontuário não encontrado");
        }

        @Test
        @DisplayName("Dono atualiza os campos clínicos")
        void deveAtualizarSemPlanejamentos() {
            Prontuario prontuario = prontuarioGravado();

            service.atualizarProntuarioMedico(1L, criarRequest());

            verify(prontuarioRepository).save(prontuario);
            assertThat(prontuario.getProntAnamnese()).isEqualTo("Paciente relata...");
            assertThat(prontuario.getProntDiagnostico()).isEqualTo("Enxaqueca");
        }

        @Test
        @DisplayName("Outro profissional, secretária ou gestor da mesma clínica recebem 404 e nada é gravado")
        void naoDonoRecebe404() {
            Prontuario prontuario = prontuarioGravado();
            String anamneseOriginal = prontuario.getProntAnamnese();
            for (Long outroUsuario : List.of(501L, 502L, 503L)) { // outro médico, secretária, gestor
                TenantContext.setCurrentUser(outroUsuario);

                var requisicao = criarRequest();
                assertThatThrownBy(() -> service.atualizarProntuarioMedico(1L, requisicao))
                        .isInstanceOf(ResourceNotFoundException.class);
            }
            verify(prontuarioRepository, never()).save(any());
            verify(cacheEvictionService, never()).evictProntuarioRecente(any(), any());
            assertThat(prontuario.getProntAnamnese()).isEqualTo(anamneseOriginal);
        }

        @Test
        @DisplayName("Sem usuário no token responde 404")
        void semUsuarioRecebe404() {
            prontuarioGravado();
            TenantContext.setCurrentUser(null);

            var requisicao = criarRequest();
            assertThatThrownBy(() -> service.atualizarProntuarioMedico(1L, requisicao))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Payload com outra consulta e outro profissional é ignorado: vínculos continuam os do prontuário")
        void payloadNaoTrocaConsultaNemProfissional() {
            Prontuario prontuario = prontuarioGravado();
            Consulta consultaOriginal = prontuario.getConsulta();
            Profissional profissionalOriginal = prontuario.getProfissional();
            CadastrarProntuarioRequest request = criarRequest();
            request.setConsulta(999L);
            request.setCodigoMedico(888L);
            request.setPlanejamentos(List.of(item(null, "Retorno", "100")));

            service.atualizarProntuarioMedico(1L, request);

            assertThat(prontuario.getConsulta()).isSameAs(consultaOriginal);
            assertThat(prontuario.getProfissional()).isSameAs(profissionalOriginal);
            assertThat(prontuario.getPlanejamentos()).singleElement().satisfies(p -> {
                assertThat(p.getConsulta()).isSameAs(consultaOriginal);
                assertThat(p.getProfissional()).isSameAs(profissionalOriginal);
                assertThat(p.getOrganizacao().getId()).isEqualTo(ORG);
            });
            verifyNoInteractions(consultaRepository, profissionalRepository);
        }

        @Test
        @DisplayName("Item novo usa o paciente da consulta; pacienteId do payload é ignorado")
        void itemNovoUsaPacienteDaConsulta() {
            Prontuario prontuario = prontuarioGravado();
            Paciente pacienteDaConsulta = new Paciente();
            pacienteDaConsulta.setPaciCodigo(5L);
            prontuario.getConsulta().setPaciente(pacienteDaConsulta);
            CadastrarProntuarioRequest request = criarRequest();
            request.setPlanejamentos(List.of(criarItemComPaciente(99L)));

            service.atualizarProntuarioMedico(1L, request);

            assertThat(prontuario.getPlanejamentos()).singleElement().satisfies(p -> {
                assertThat(p.getProcedimentoRealizado()).isEqualTo("Consulta de rotina");
                assertThat(p.getPaciente()).isSameAs(pacienteDaConsulta);
                assertThat(p.getStatusAssinatura()).isEqualTo("PENDENTE");
            });
            verifyNoInteractions(pacienteRepository);
        }

        @Test
        @DisplayName("Edição não muda a data de finalização do atendimento")
        void naoMudaDataFinalizado() {
            Prontuario prontuario = prontuarioGravado();
            java.sql.Date finalizado = java.sql.Date.valueOf("2026-10-02");
            prontuario.setProntDataFinalizado(finalizado);
            CadastrarProntuarioRequest request = criarRequest();
            request.setDataFinalizado(null);

            service.atualizarProntuarioMedico(1L, request);

            assertThat(prontuario.getProntDataFinalizado()).isEqualTo(finalizado);
        }

        @Test
        @DisplayName("Mantém o assinado intacto, atualiza o pendente por id, cria o novo e remove o pendente omitido")
        void preservaAssinadoEAtualizaPorId() {
            PlanejamentoTerapeutico assinado = planejamento(10L, "Limpeza", "150.00", "ASSINADO");
            PlanejamentoTerapeutico pendente = planejamento(11L, "Raio-X", "80.00", "PENDENTE");
            PlanejamentoTerapeutico omitido = planejamento(12L, "Clareamento", "300.00", "PENDENTE");
            Prontuario prontuario = prontuarioGravado(assinado, pendente, omitido);
            CadastrarProntuarioRequest request = criarRequest();
            request.setPlanejamentos(List.of(
                    item(10L, "Limpeza", "150"),          // assinado reenviado igual (150 == 150.00)
                    item(11L, "Raio-X panorâmico", "95"), // pendente alterado
                    item(null, "Retorno", "0")));         // novo

            service.atualizarProntuarioMedico(1L, request);

            assertThat(prontuario.getPlanejamentos()).extracting(PlanejamentoTerapeutico::getProcedimentoRealizado)
                    .containsExactlyInAnyOrder("Limpeza", "Raio-X panorâmico", "Retorno");
            assertThat(prontuario.getPlanejamentos()).contains(assinado, pendente).doesNotContain(omitido);
            assertThat(assinado.getStatusAssinatura()).isEqualTo("ASSINADO");
            assertThat(assinado.getAssinaturaBase64()).isEqualTo("data:image/png;base64,AAAA");
            assertThat(assinado.getDataAssinatura()).isEqualTo(LocalDateTime.of(2026, 11, 2, 10, 0));
            assertThat(assinado.getIpOrigem()).isEqualTo("10.0.0.1");
            assertThat(assinado.getTokenAssinatura()).isEqualTo("tok-assinado");
            assertThat(assinado.getValor()).isEqualByComparingTo("150.00");
            assertThat(pendente.getValor()).isEqualByComparingTo("95");
        }

        @Test
        @DisplayName("Assinado omitido do payload não é removido")
        void assinadoOmitidoNaoERemovido() {
            PlanejamentoTerapeutico assinado = planejamento(10L, "Limpeza", "150.00", "ASSINADO");
            Prontuario prontuario = prontuarioGravado(assinado, planejamento(11L, "Raio-X", "80.00", "PENDENTE"));
            CadastrarProntuarioRequest request = criarRequest(); // lista vazia

            service.atualizarProntuarioMedico(1L, request);

            assertThat(prontuario.getPlanejamentos()).containsExactly(assinado);
        }

        @Test
        @DisplayName("Mudar procedimento ou valor de item assinado responde 409 PLANEJAMENTO_ASSINADO e não grava")
        void alterarAssinadoRecusado() {
            for (CadastrarProntuarioRequest.PlanejamentoItem alterado : List.of(
                    item(10L, "Limpeza profunda", "150.00"), item(10L, "Limpeza", "151.00"))) {
                PlanejamentoTerapeutico assinado = planejamento(10L, "Limpeza", "150.00", "ASSINADO");
                prontuarioGravado(assinado);
                CadastrarProntuarioRequest request = criarRequest();
                request.setPlanejamentos(List.of(alterado));

                assertThatThrownBy(() -> service.atualizarProntuarioMedico(1L, request))
                        .isInstanceOfSatisfying(BusinessException.class,
                                e -> assertThat(e.getErrorCode()).isEqualTo("PLANEJAMENTO_ASSINADO"));
                assertThat(assinado.getProcedimentoRealizado()).isEqualTo("Limpeza");
                assertThat(assinado.getValor()).isEqualByComparingTo("150.00");
            }
            verify(prontuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("Item com id de outro prontuário responde 400")
        void idDeOutroProntuarioRecusado() {
            prontuarioGravado(planejamento(11L, "Raio-X", "80.00", "PENDENTE"));
            CadastrarProntuarioRequest request = criarRequest();
            request.setPlanejamentos(List.of(item(77L, "Invasor", "1")));

            assertThatThrownBy(() -> service.atualizarProntuarioMedico(1L, request))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(prontuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("Lista de planejamentos ausente (null) mantém todos os planejamentos")
        void listaNullMantemPlanejamentos() {
            PlanejamentoTerapeutico pendente = planejamento(11L, "Raio-X", "80.00", "PENDENTE");
            Prontuario prontuario = prontuarioGravado(pendente);
            CadastrarProntuarioRequest request = criarRequest();
            request.setPlanejamentos(null);

            service.atualizarProntuarioMedico(1L, request);

            assertThat(prontuario.getPlanejamentos()).containsExactly(pendente);
        }
    }

    // ========== Isolamento multi-tenant ==========

    @Nested
    @DisplayName("Isolamento entre clínicas")
    class IsolamentoEntreClinicas {

        private Prontuario prontuarioDeOutraClinica() {
            Prontuario p = criarProntuario(1L);
            Organizacao outra = new Organizacao();
            outra.setId(OUTRA_ORG);
            p.getConsulta().setOrganizacao(outra);
            return p;
        }

        @Test
        @DisplayName("buscarPorId não devolve prontuário de outra clínica (responde não encontrado)")
        void buscarPorIdOutraClinica() {
            lenient().when(prontuarioRepository.findById(1L)).thenReturn(Optional.of(prontuarioDeOutraClinica()));

            assertThatThrownBy(() -> service.buscarPorId(1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("nao encontrado");
            verify(prontuarioRepository).findByProntCodigoProntuarioAndConsulta_Organizacao_Id(1L, ORG);
        }

        @Test
        @DisplayName("buscarProntuarioById filtra pela organização do token")
        void buscarPorConsultaOutraClinica() {
            assertThatThrownBy(() -> service.buscarProntuarioById(10L))
                    .isInstanceOf(EntityNotFoundException.class);
            verify(prontuarioRepository).findByConsultaIdAndOrganizacaoIdWithFetch(10L, ORG);
        }

        @Test
        @DisplayName("buscarMaisRecentePorConsulta filtra pela organização do token")
        void maisRecenteOutraClinica() {
            when(prontuarioRepository.findIdMaisRecentePorConsulta(10L, ORG)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.buscarMaisRecentePorConsulta(10L))
                    .isInstanceOf(EntityNotFoundException.class);
            verify(prontuarioRepository, never()).findByIdWithFullFetch(any());
        }

        @Test
        @DisplayName("cadastrar recusa consulta de outra clínica e não grava nada")
        void cadastrarConsultaOutraClinica() {
            when(profissionalRepository.findByIdAndOrganizacao_Id(1L, ORG)).thenReturn(Optional.of(criarProfissional(1L)));
            lenient().when(consultaRepository.findById(10L)).thenReturn(Optional.of(criarConsulta(10L)));

            var requisicao = criarRequest();
            assertThatThrownBy(() -> service.cadastrarProntuarioMedico(requisicao))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Consulta nao encontrada");
            verify(prontuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("cadastrar recusa profissional de outra clínica")
        void cadastrarProfissionalOutraClinica() {
            lenient().when(profissionalRepository.findById(1L)).thenReturn(Optional.of(criarProfissional(1L)));

            var requisicao = criarRequest();
            assertThatThrownBy(() -> service.cadastrarProntuarioMedico(requisicao))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Profissional nao encontrado");
            verify(prontuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("atualizar recusa prontuário de outra clínica e não grava nada")
        void atualizarOutraClinica() {
            lenient().when(prontuarioRepository.findById(1L)).thenReturn(Optional.of(prontuarioDeOutraClinica()));

            var requisicao = criarRequest();
            assertThatThrownBy(() -> service.atualizarProntuarioMedico(1L, requisicao))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Prontuário não encontrado");
            verify(prontuarioRepository, never()).saveAndFlush(any());
            verify(prontuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("atualizar invalida o cache prontuario-recente com a chave do tenant")
        void atualizarInvalidaCacheDoTenant() {
            Prontuario prontuario = criarProntuario(1L);
            when(prontuarioRepository.findByProntCodigoProntuarioAndConsulta_Organizacao_Id(1L, ORG))
                    .thenReturn(Optional.of(prontuario));

            service.atualizarProntuarioMedico(1L, criarRequest());

            verify(cacheEvictionService).evictProntuarioRecente(ORG, 10L);
        }

        @Test
        @DisplayName("Sem tenant no token a operação é negada")
        void semTenantNega() {
            TenantContext.clear();

            assertThatThrownBy(() -> service.buscarMaisRecentePorConsulta(10L))
                    .isInstanceOf(br.com.saudeConecta.config.tenant.TenantNotDefinedException.class);
            verifyNoInteractions(prontuarioRepository);
        }
    }
}
