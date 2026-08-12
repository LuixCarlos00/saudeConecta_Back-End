package br.com.saudeConecta.presentation.controller;

import br.com.saudeConecta.domain.relatorio.TipoDocumentoRelatorio;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.presentation.dto.relatorio.PacienteAtendidoResponse;
import br.com.saudeConecta.service.RelatorioPacienteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * Endpoints da tela de relatorios dos pacientes.
 *
 * Um usuario clinico so visualiza os pacientes que atendeu. Gestores e
 * assistentes visualizam toda a organizacao e podem filtrar por profissional.
 */
@RestController
@RequestMapping("/relatorios")
@RequiredArgsConstructor
@Slf4j
public class RelatorioController {

    private final RelatorioPacienteService relatorioPacienteService;

    /**
     * Lista os pacientes atendidos com as consultas e documentos disponiveis.
     *
     * @param profissionalId filtro opcional de profissional, ignorado para usuarios clinicos
     * @param termo          busca opcional por nome, cpf ou telefone
     * @param dataInicio     inicio opcional do periodo
     * @param dataFim        fim opcional do periodo
     * @param usuarioLogado  usuario autenticado
     * @return pacientes com seus relatorios
     */
    @GetMapping("/pacientes")
    public ResponseEntity<List<PacienteAtendidoResponse>> listarPacientesAtendidos(
            @RequestParam(required = false) Long profissionalId,
            @RequestParam(required = false) String termo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @AuthenticationPrincipal Usuario usuarioLogado) {

        Long profissionalFiltro = resolverProfissionalFiltro(profissionalId, usuarioLogado);
        log.debug("- Listando relatorios de pacientes - usuario={}, profissionalFiltro={}",
                usuarioLogado.getId(), profissionalFiltro);

        return ResponseEntity.ok(relatorioPacienteService.buscarPacientesAtendidos(
                profissionalFiltro, null, termo, dataInicio, dataFim));
    }

    /**
     * Busca os relatorios de um paciente especifico.
     *
     * @param pacienteId    paciente desejado
     * @param usuarioLogado usuario autenticado
     * @return relatorios do paciente ou 404 quando nao houver atendimento visivel
     */
    @GetMapping("/pacientes/{pacienteId}")
    public ResponseEntity<PacienteAtendidoResponse> buscarRelatoriosDoPaciente(
            @PathVariable Long pacienteId,
            @AuthenticationPrincipal Usuario usuarioLogado) {

        Long profissionalFiltro = resolverProfissionalFiltro(null, usuarioLogado);
        log.debug("- Buscando relatorios do paciente {} - usuario={}", pacienteId, usuarioLogado.getId());

        return relatorioPacienteService.buscarRelatoriosDoPaciente(pacienteId, profissionalFiltro)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Lista os tipos de documento suportados, para montagem dos filtros na interface.
     *
     * @return tipos de documento com rotulo e descricao
     */
    @GetMapping("/tipos-documento")
    public ResponseEntity<List<TipoDocumentoResponse>> listarTiposDocumento() {
        List<TipoDocumentoResponse> tipos = Arrays.stream(TipoDocumentoRelatorio.values())
                .map(tipo -> new TipoDocumentoResponse(tipo.name(), tipo.getRotulo(), tipo.getDescricaoPadrao()))
                .toList();
        return ResponseEntity.ok(tipos);
    }

    /**
     * Define qual profissional deve ser usado como filtro.
     *
     * Usuario clinico e sempre restrito aos proprios atendimentos; os demais
     * perfis usam o filtro informado na requisicao.
     *
     * @param profissionalIdInformado filtro enviado pela interface
     * @param usuarioLogado           usuario autenticado
     * @return id do profissional a filtrar ou null para toda a organizacao
     */
    private Long resolverProfissionalFiltro(Long profissionalIdInformado, Usuario usuarioLogado) {
        if (!usuarioLogado.isClinico()) {
            return profissionalIdInformado;
        }

        return relatorioPacienteService.buscarProfissionalDoUsuario(usuarioLogado.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Usuario clinico sem profissional vinculado: " + usuarioLogado.getId()));
    }

    /**
     * Tipo de documento exposto para a interface.
     *
     * @param codigo    nome do enum
     * @param rotulo    texto curto para exibicao
     * @param descricao descricao padrao do tipo
     */
    public record TipoDocumentoResponse(String codigo, String rotulo, String descricao) {
    }
}
