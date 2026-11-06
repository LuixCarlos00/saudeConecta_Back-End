package br.com.saudeConecta.application.relatorio;

import br.com.saudeConecta.application.relatorio.dto.ConsultaRelatorioResponse;
import br.com.saudeConecta.application.relatorio.dto.DocumentoRelatorioResponse;
import br.com.saudeConecta.application.relatorio.dto.PacienteAtendidoResponse;
import br.com.saudeConecta.config.tenant.TenantHelper;
import br.com.saudeConecta.domain.consulta.Consulta;
import br.com.saudeConecta.domain.consulta.StatusConsulta;
import br.com.saudeConecta.domain.pagamento.FormaPagamento;
import br.com.saudeConecta.domain.paciente.Paciente;
import br.com.saudeConecta.domain.profissional.Especialidade;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.domain.profissional.TipoProfissional;
import br.com.saudeConecta.domain.prontuario.PlanejamentoTerapeutico;
import br.com.saudeConecta.domain.prontuario.Prontuario;
import br.com.saudeConecta.domain.prontuario.ProntuarioDentista;
import br.com.saudeConecta.domain.prontuario.TermoAutorizacao;
import br.com.saudeConecta.domain.relatorio.TipoDocumentoRelatorio;
import br.com.saudeConecta.infrastructure.persistence.repository.ProfissionalRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.RelatorioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RelatorioPacienteService - pacientes atendidos e documentos derivados")
class RelatorioPacienteServiceTest {

    private static final Long ORG = 1L;

    @Mock private RelatorioRepository relatorioRepository;
    @Mock private ProfissionalRepository profissionalRepository;
    @Mock private TenantHelper tenantHelper;

    @InjectMocks
    private RelatorioPacienteService service;

    private final Profissional dentista = Profissional.builder().id(10L).nome("Dr. Dente")
            .tipoProfissional(TipoProfissional.builder().codigo("DENTISTA").build()).build();
    private final Profissional medico = Profissional.builder().id(11L).nome("Dra. Med").build();

    @BeforeEach
    void setUp() {
        lenient().when(tenantHelper.getCurrentTenantId()).thenReturn(ORG);
    }

    private static Paciente paciente(long id, String nome) {
        return Paciente.builder().paciCodigo(id).paciNome(nome).paciCpf("cpf" + id).paciTelefone("tel" + id)
                .paciEmail(nome + "@x.com").paciSexo("F").build();
    }

    private void vazioExceto(List<Consulta> consultas) {
        when(relatorioRepository.buscarConsultasParaRelatorio(any(), any(), any(), any(), any(), any()))
                .thenReturn(consultas);
        when(relatorioRepository.buscarProntuariosMedicos(ORG, null, null)).thenReturn(List.of());
        when(relatorioRepository.buscarProntuariosDentista(ORG, null, null)).thenReturn(List.of());
        when(relatorioRepository.buscarPlanejamentos(ORG, null, null)).thenReturn(List.of());
        when(relatorioRepository.buscarTermosAutorizacao(ORG, null, null)).thenReturn(List.of());
    }

    @Test
    @DisplayName("Sem consultas retorna lista vazia sem buscar documentos")
    void semConsultas() {
        when(relatorioRepository.buscarConsultasParaRelatorio(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());

        assertThat(service.buscarPacientesAtendidos(null, null, null, null, null)).isEmpty();

        verify(relatorioRepository).buscarConsultasParaRelatorio(eq(ORG), eq(null), eq(null), eq(null), eq(null), eq(null));
        verifyNoMoreInteractions(relatorioRepository);
    }

    @Test
    @DisplayName("Termo em branco vira nulo e datas viram intervalo [inicio, fim+1)")
    void normalizaFiltros() {
        when(relatorioRepository.buscarConsultasParaRelatorio(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());

        service.buscarPacientesAtendidos(3L, 4L, "   ", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31));
        service.buscarPacientesAtendidos(3L, 4L, "  ana ", null, null);

        verify(relatorioRepository).buscarConsultasParaRelatorio(ORG, 3L, 4L, null,
                LocalDate.of(2025, 1, 1).atStartOfDay(), LocalDate.of(2025, 2, 1).atStartOfDay());
        verify(relatorioRepository).buscarConsultasParaRelatorio(ORG, 3L, 4L, "ana", null, null);
    }

    @Test
    @DisplayName("Consulta sem paciente é descartada")
    void consultaSemPaciente() {
        vazioExceto(List.of(Consulta.builder().id(1L).dataHora(LocalDateTime.of(2025, 1, 1, 8, 0)).build()));

        assertThat(service.buscarPacientesAtendidos(null, null, null, null, null)).isEmpty();
    }

    @Test
    @DisplayName("Prontuário de dentista gera registro, prescrição e exames com títulos e datas de fallback")
    void documentosDeProntuarioDentista() {
        LocalDateTime dh = LocalDateTime.of(2025, 3, 1, 9, 30);
        Consulta c1 = Consulta.builder().id(1L).dataHora(dh).paciente(paciente(1L, "Ana")).profissional(dentista).build();
        Consulta c2 = Consulta.builder().id(2L).dataHora(dh.plusDays(1)).paciente(paciente(1L, "Ana")).profissional(dentista).build();
        Consulta c3 = Consulta.builder().id(3L).dataHora(dh.plusDays(2)).paciente(paciente(1L, "Ana")).profissional(dentista).build();
        vazioExceto(List.of(c3, c2, c1));

        ProntuarioDentista semConsulta = ProntuarioDentista.builder().codigo(90L).diagnostico("x").build();
        // Consulta 1: sem data finalizada (usa a data da consulta), prescrição/exame com títulos próprios
        ProntuarioDentista p1 = ProntuarioDentista.builder().codigo(91L).consulta(c1)
                .diagnostico("  Carie <b>profunda</b>  ").prescricao("Amoxicilina").tituloPrescricao("Receita A")
                .solicitacaoExameTexto("Raio X").tituloExame("Exame B").build();
        // Consulta 2: data finalizada, prescrição e exame sem título (usa o padrão), diagnóstico vazio
        ProntuarioDentista p2 = ProntuarioDentista.builder().codigo(92L).consulta(c2)
                .dataFinalizado(LocalDate.of(2025, 4, 5)).diagnostico("  ").prescricao("Dipirona")
                .tituloPrescricao(" ").solicitacaoExameTexto("Hemograma").tituloExame("").build();
        // Consulta 3: só o registro
        ProntuarioDentista p3 = ProntuarioDentista.builder().codigo(93L).consulta(c3).diagnostico("Gengivite").build();
        when(relatorioRepository.buscarProntuariosDentista(ORG, null, null)).thenReturn(List.of(semConsulta, p1, p2, p3));

        List<PacienteAtendidoResponse> r = service.buscarPacientesAtendidos(null, null, null, null, null);

        assertThat(r).hasSize(1);
        PacienteAtendidoResponse ana = r.get(0);
        assertThat(ana.totalConsultas()).isEqualTo(3);
        assertThat(ana.totalDocumentos()).isEqualTo(7); // consulta 1: 3 docs, consulta 2: 3 docs, consulta 3: 1 doc
        ConsultaRelatorioResponse r1 = ana.consultas().stream().filter(x -> x.id() == 1L).findFirst().orElseThrow();
        assertThat(r1.diagnostico()).isEqualTo("Carie <b>profunda</b>");
        assertThat(r1.documentos()).extracting(DocumentoRelatorioResponse::tipo, DocumentoRelatorioResponse::titulo,
                        DocumentoRelatorioResponse::descricao, DocumentoRelatorioResponse::emitidoEm, DocumentoRelatorioResponse::id)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(TipoDocumentoRelatorio.REGISTRO_CONSULTA, "Registro da consulta",
                                "Carie profunda", dh, 91L),
                        org.assertj.core.groups.Tuple.tuple(TipoDocumentoRelatorio.PRESCRICAO, "Receita A",
                                "Amoxicilina", dh, 91L),
                        org.assertj.core.groups.Tuple.tuple(TipoDocumentoRelatorio.EXAMES, "Exame B",
                                "Raio X", dh, 91L));
        ConsultaRelatorioResponse r2 = ana.consultas().stream().filter(x -> x.id() == 2L).findFirst().orElseThrow();
        assertThat(r2.diagnostico()).isNull();
        assertThat(r2.documentos()).extracting(DocumentoRelatorioResponse::tipo, DocumentoRelatorioResponse::titulo,
                        DocumentoRelatorioResponse::descricao, DocumentoRelatorioResponse::emitidoEm)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(TipoDocumentoRelatorio.REGISTRO_CONSULTA, "Registro da consulta",
                                TipoDocumentoRelatorio.REGISTRO_CONSULTA.getDescricaoPadrao(), LocalDate.of(2025, 4, 5).atStartOfDay()),
                        org.assertj.core.groups.Tuple.tuple(TipoDocumentoRelatorio.PRESCRICAO, "Receituario",
                                "Dipirona", LocalDate.of(2025, 4, 5).atStartOfDay()),
                        org.assertj.core.groups.Tuple.tuple(TipoDocumentoRelatorio.EXAMES, "Solicitacao de exames",
                                "Hemograma", LocalDate.of(2025, 4, 5).atStartOfDay()));
        ConsultaRelatorioResponse r3 = ana.consultas().stream().filter(x -> x.id() == 3L).findFirst().orElseThrow();
        assertThat(r3.documentos()).hasSize(1);
        assertThat(r3.tipoProfissionalNome()).isEqualTo("DENTISTA");
    }

    @Test
    @DisplayName("Cenário completo: prontuário médico, planejamentos, termos, pagamento, agrupamento e ordenação")
    void cenarioCompleto() {
        LocalDateTime base = LocalDateTime.of(2025, 5, 10, 10, 0);
        Paciente ana = paciente(1L, "Ana");
        ana.setPaciDataNacimento(java.sql.Date.valueOf("1990-02-03"));
        Paciente bia = paciente(2L, "Bia");
        Consulta c1 = Consulta.builder().id(1L).dataHora(base).paciente(ana).profissional(medico)
                .status(StatusConsulta.PAGO).valor(new BigDecimal("150.00"))
                .formaPagamento(FormaPagamento.builder().nome("PIX").build())
                .especialidade(Especialidade.builder().nome("Clinica").build())
                .updatedAt(base.plusHours(1)).build();
        Consulta c2 = Consulta.builder().id(2L).dataHora(base.plusDays(3)).paciente(bia).status(StatusConsulta.PAGO).build();
        Consulta c3 = Consulta.builder().id(3L).dataHora(base.minusDays(5)).paciente(ana).status(StatusConsulta.AGENDADA).build();
        vazioExceto(List.of(c1, c2, c3));

        Prontuario pm = Prontuario.builder().prontCodigoProntuario(50L).consulta(c1)
                .prontDiagnostico("Gripe").prontPrescricao("Repouso").prontTituloPrescricao("Receita")
                .prontSolicitacaoExameTexto("Hemograma").prontDataFinalizado(java.sql.Date.valueOf("2025-05-10")).build();
        Prontuario pmSemConsulta = Prontuario.builder().prontCodigoProntuario(51L).build();
        when(relatorioRepository.buscarProntuariosMedicos(ORG, null, null)).thenReturn(List.of(pmSemConsulta, pm));

        PlanejamentoTerapeutico pl1 = PlanejamentoTerapeutico.builder().id(60L).consulta(c1)
                .procedimentoRealizado("Limpeza").statusAssinatura("ASSINADO").createdAt(base).build();
        PlanejamentoTerapeutico pl2 = PlanejamentoTerapeutico.builder().id(61L).consulta(c1)
                .dataProcedimento(LocalDate.of(2025, 1, 1)).build();
        PlanejamentoTerapeutico pl3 = PlanejamentoTerapeutico.builder().id(62L).consulta(c3)
                .procedimentoRealizado("Canal").dataProcedimento(LocalDate.of(2025, 2, 2)).build();
        PlanejamentoTerapeutico plSemConsulta = PlanejamentoTerapeutico.builder().id(63L).build();
        when(relatorioRepository.buscarPlanejamentos(ORG, null, null)).thenReturn(List.of(pl1, plSemConsulta, pl2, pl3));

        TermoAutorizacao assinado = TermoAutorizacao.builder().id(70L).consulta(c1).status("ASSINADO")
                .dataAssinatura(base.plusMinutes(5)).createdAt(base).build();
        TermoAutorizacao pendente = TermoAutorizacao.builder().id(71L).consulta(c3).status("PENDENTE")
                .createdAt(base.minusDays(6)).build();
        TermoAutorizacao termoSemConsulta = TermoAutorizacao.builder().id(72L).build();
        when(relatorioRepository.buscarTermosAutorizacao(ORG, null, null)).thenReturn(List.of(assinado, pendente, termoSemConsulta));

        List<PacienteAtendidoResponse> r = service.buscarPacientesAtendidos(null, null, null, null, null);

        // Bia atendida por último vem primeiro
        assertThat(r).extracting(PacienteAtendidoResponse::nome).containsExactly("Bia", "Ana");
        PacienteAtendidoResponse biaR = r.get(0);
        assertThat(biaR.ultimoAtendimento()).isEqualTo(base.plusDays(3));
        assertThat(biaR.profissionalId()).isNull();
        assertThat(biaR.profissionalNome()).isNull();
        assertThat(biaR.dataNascimento()).isNull();
        assertThat(biaR.totalDocumentos()).isEqualTo(1);
        DocumentoRelatorioResponse comprovanteBia = biaR.consultas().get(0).documentos().get(0);
        assertThat(comprovanteBia.tipo()).isEqualTo(TipoDocumentoRelatorio.COMPROVANTE_PAGAMENTO);
        assertThat(comprovanteBia.descricao()).isEqualTo("Nao informado");
        assertThat(comprovanteBia.emitidoEm()).isEqualTo(base.plusDays(3));
        assertThat(biaR.consultas().get(0).status()).isEqualTo("PAGO");
        assertThat(biaR.consultas().get(0).especialidadeNome()).isNull();
        assertThat(biaR.consultas().get(0).tipoProfissionalNome()).isNull();

        PacienteAtendidoResponse anaR = r.get(1);
        assertThat(anaR.dataNascimento()).isEqualTo(LocalDate.of(1990, 2, 3));
        assertThat(anaR.profissionalId()).isEqualTo(11L);
        assertThat(anaR.profissionalNome()).isEqualTo("Dra. Med");
        assertThat(anaR.ultimoAtendimento()).isEqualTo(base);
        assertThat(anaR.totalConsultas()).isEqualTo(2);

        ConsultaRelatorioResponse a1 = anaR.consultas().get(0);
        assertThat(a1.id()).isEqualTo(1L);
        assertThat(a1.especialidadeNome()).isEqualTo("Clinica");
        assertThat(a1.profissionalNome()).isEqualTo("Dra. Med");
        assertThat(a1.tipoProfissionalNome()).isNull();
        assertThat(a1.diagnostico()).isEqualTo("Gripe");
        assertThat(a1.documentos()).extracting(DocumentoRelatorioResponse::tipo, DocumentoRelatorioResponse::titulo,
                        DocumentoRelatorioResponse::descricao, DocumentoRelatorioResponse::assinado)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(TipoDocumentoRelatorio.REGISTRO_CONSULTA, "Registro da consulta", "Gripe", false),
                        org.assertj.core.groups.Tuple.tuple(TipoDocumentoRelatorio.PRESCRICAO, "Receita", "Repouso", false),
                        org.assertj.core.groups.Tuple.tuple(TipoDocumentoRelatorio.EXAMES, "Solicitacao de exames", "Hemograma", false),
                        org.assertj.core.groups.Tuple.tuple(TipoDocumentoRelatorio.PLANEJAMENTO, "Planejamento terapeutico",
                                "2 procedimentos planejados", true),
                        org.assertj.core.groups.Tuple.tuple(TipoDocumentoRelatorio.QUESTIONARIO_SAUDE, "Questionario de saude",
                                "Respondido e assinado pelo paciente", true),
                        org.assertj.core.groups.Tuple.tuple(TipoDocumentoRelatorio.COMPROVANTE_PAGAMENTO, "Comprovante de pagamento",
                                "PIX - R$ 150.00", false));
        DocumentoRelatorioResponse comprovante = a1.documentos().get(5);
        assertThat(comprovante.emitidoEm()).isEqualTo(base.plusHours(1));
        assertThat(a1.documentos().get(0).emitidoEm()).isEqualTo(LocalDate.of(2025, 5, 10).atStartOfDay());
        assertThat(a1.documentos().get(3).emitidoEm()).isEqualTo(base);
        assertThat(a1.documentos().get(3).id()).isEqualTo(60L);
        assertThat(a1.documentos().get(4).emitidoEm()).isEqualTo(base.plusMinutes(5));

        ConsultaRelatorioResponse a3 = anaR.consultas().get(1);
        assertThat(a3.id()).isEqualTo(3L);
        assertThat(a3.documentos()).extracting(DocumentoRelatorioResponse::tipo, DocumentoRelatorioResponse::descricao,
                        DocumentoRelatorioResponse::emitidoEm, DocumentoRelatorioResponse::assinado)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(TipoDocumentoRelatorio.PLANEJAMENTO, "Canal",
                                LocalDate.of(2025, 2, 2).atStartOfDay(), false),
                        org.assertj.core.groups.Tuple.tuple(TipoDocumentoRelatorio.QUESTIONARIO_SAUDE,
                                "Aguardando assinatura do paciente", base.minusDays(6), false));
        assertThat(anaR.totalDocumentos()).isEqualTo(8);
    }

    @Test
    @DisplayName("Resolve o profissional vinculado ao usuário")
    void profissionalDoUsuario() {
        when(profissionalRepository.findByUsuario_Id(5L)).thenReturn(Optional.of(medico));

        assertThat(service.buscarProfissionalDoUsuario(5L)).contains(11L);
    }
}
