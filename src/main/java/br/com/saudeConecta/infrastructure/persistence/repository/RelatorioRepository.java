package br.com.saudeConecta.infrastructure.persistence.repository;

import br.com.saudeConecta.domain.consulta.Consulta;
import br.com.saudeConecta.domain.prontuario.PlanejamentoTerapeutico;
import br.com.saudeConecta.domain.prontuario.Prontuario;
import br.com.saudeConecta.domain.prontuario.ProntuarioDentista;
import br.com.saudeConecta.domain.prontuario.TermoAutorizacao;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Consultas de leitura usadas pela tela de relatorios dos pacientes.
 *
 * Todas as buscas sao filtradas por organizacao e aceitam filtro opcional de
 * profissional e de periodo, permitindo carregar os dados da tela com um numero
 * fixo de queries em vez de uma consulta por paciente.
 */
public interface RelatorioRepository extends Repository<Consulta, Long> {

    /**
     * Busca as consultas que compoem os relatorios, com os relacionamentos necessarios.
     *
     * @param organizacaoId  organizacao do tenant atual
     * @param profissionalId profissional a filtrar; nulo traz todos
     * @param pacienteId     paciente a filtrar; nulo traz todos
     * @param termo          texto para busca em nome, cpf ou telefone; nulo traz todos
     * @param inicio         inicio do periodo; nulo ignora o limite inferior
     * @param fim            fim do periodo; nulo ignora o limite superior
     * @return consultas ordenadas do atendimento mais recente para o mais antigo
     */
    @Query("""
        SELECT c FROM Consulta c
        JOIN FETCH c.paciente p
        JOIN FETCH c.profissional prof
        LEFT JOIN FETCH prof.tipoProfissional
        LEFT JOIN FETCH c.especialidade
        LEFT JOIN FETCH c.formaPagamento
        WHERE c.organizacao.id = :organizacaoId
          AND (:profissionalId IS NULL OR prof.id = :profissionalId)
          AND (:pacienteId IS NULL OR p.paciCodigo = :pacienteId)
          AND (:termo IS NULL OR LOWER(p.paciNome) LIKE LOWER(CONCAT('%', :termo, '%'))
                              OR p.paciCpf LIKE CONCAT('%', :termo, '%')
                              OR p.paciTelefone LIKE CONCAT('%', :termo, '%'))
          AND (:inicio IS NULL OR c.dataHora >= :inicio)
          AND (:fim IS NULL OR c.dataHora <= :fim)
        ORDER BY c.dataHora DESC
    """)
    List<Consulta> buscarConsultasParaRelatorio(
            @Param("organizacaoId") Long organizacaoId,
            @Param("profissionalId") Long profissionalId,
            @Param("pacienteId") Long pacienteId,
            @Param("termo") String termo,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim);

    /**
     * Busca os prontuarios medicos das consultas da organizacao.
     *
     * @param organizacaoId  organizacao do tenant atual
     * @param profissionalId profissional a filtrar; nulo traz todos
     * @param pacienteId     paciente a filtrar; nulo traz todos
     * @return prontuarios medicos com a consulta carregada
     */
    @Query("""
        SELECT pr FROM Prontuario pr
        JOIN FETCH pr.consulta c
        WHERE c.organizacao.id = :organizacaoId
          AND (:profissionalId IS NULL OR c.profissional.id = :profissionalId)
          AND (:pacienteId IS NULL OR c.paciente.paciCodigo = :pacienteId)
    """)
    List<Prontuario> buscarProntuariosMedicos(
            @Param("organizacaoId") Long organizacaoId,
            @Param("profissionalId") Long profissionalId,
            @Param("pacienteId") Long pacienteId);

    /**
     * Busca os prontuarios odontologicos das consultas da organizacao.
     *
     * @param organizacaoId  organizacao do tenant atual
     * @param profissionalId profissional a filtrar; nulo traz todos
     * @param pacienteId     paciente a filtrar; nulo traz todos
     * @return prontuarios odontologicos com a consulta carregada
     */
    @Query("""
        SELECT pd FROM ProntuarioDentista pd
        JOIN FETCH pd.consulta c
        WHERE c.organizacao.id = :organizacaoId
          AND (:profissionalId IS NULL OR c.profissional.id = :profissionalId)
          AND (:pacienteId IS NULL OR c.paciente.paciCodigo = :pacienteId)
    """)
    List<ProntuarioDentista> buscarProntuariosDentista(
            @Param("organizacaoId") Long organizacaoId,
            @Param("profissionalId") Long profissionalId,
            @Param("pacienteId") Long pacienteId);

    /**
     * Busca os planejamentos terapeuticos vinculados a consultas da organizacao.
     *
     * @param organizacaoId  organizacao do tenant atual
     * @param profissionalId profissional a filtrar; nulo traz todos
     * @param pacienteId     paciente a filtrar; nulo traz todos
     * @return planejamentos ordenados por data do procedimento
     */
    @Query("""
        SELECT pl FROM PlanejamentoTerapeutico pl
        JOIN pl.consulta c
        WHERE c.organizacao.id = :organizacaoId
          AND (:profissionalId IS NULL OR c.profissional.id = :profissionalId)
          AND (:pacienteId IS NULL OR c.paciente.paciCodigo = :pacienteId)
        ORDER BY pl.dataProcedimento
    """)
    List<PlanejamentoTerapeutico> buscarPlanejamentos(
            @Param("organizacaoId") Long organizacaoId,
            @Param("profissionalId") Long profissionalId,
            @Param("pacienteId") Long pacienteId);

    /**
     * Busca os termos de autorizacao e questionarios de saude da organizacao.
     *
     * @param organizacaoId  organizacao do tenant atual
     * @param profissionalId profissional a filtrar; nulo traz todos
     * @param pacienteId     paciente a filtrar; nulo traz todos
     * @return termos com a consulta carregada
     */
    @Query("""
        SELECT t FROM TermoAutorizacao t
        JOIN FETCH t.consulta c
        WHERE c.organizacao.id = :organizacaoId
          AND (:profissionalId IS NULL OR c.profissional.id = :profissionalId)
          AND (:pacienteId IS NULL OR c.paciente.paciCodigo = :pacienteId)
    """)
    List<TermoAutorizacao> buscarTermosAutorizacao(
            @Param("organizacaoId") Long organizacaoId,
            @Param("profissionalId") Long profissionalId,
            @Param("pacienteId") Long pacienteId);
}
