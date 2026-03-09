package br.com.saudeConecta.infrastructure.persistence.specification;

import br.com.saudeConecta.domain.consulta.Consulta;
import br.com.saudeConecta.domain.consulta.StatusConsulta;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Specification para construir queries dinâmicas de Consulta
 * Permite filtrar por: organização, médico, especialidade, período, status
 */
public class ConsultaSpecification {

    /**
     * Cria uma Specification dinâmica baseada nos filtros fornecidos
     * 
     * @param organizacaoId ID da organização (obrigatório)
     * @param profissionalId ID do profissional/médico (opcional)
     * @param especialidade Nome da especialidade (opcional)
     * @param dataInicio Data/hora inicial (opcional)
     * @param dataFim Data/hora final (opcional)
     * @param statusList Lista de status (opcional)
     * @return Specification configurada com os filtros
     */
    public static Specification<Consulta> buscarComFiltros(
            Long organizacaoId,
            Long profissionalId,
            String especialidade,
            LocalDateTime dataInicio,
            LocalDateTime dataFim,
            List<StatusConsulta> statusList) {

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // JOIN FETCH para evitar N+1 queries (apenas em queries de seleção)
            if (query.getResultType().equals(Consulta.class)) {
                root.fetch("profissional", JoinType.LEFT).fetch("tipoProfissional", JoinType.LEFT);
                root.fetch("paciente", JoinType.LEFT);
                root.fetch("especialidade", JoinType.LEFT);
                root.fetch("formaPagamento", JoinType.LEFT);
                query.distinct(true);
            }

            // Filtro obrigatório: organização
            predicates.add(criteriaBuilder.equal(root.get("organizacao").get("id"), organizacaoId));

            // Filtro opcional: profissional/médico
            if (profissionalId != null) {
                predicates.add(criteriaBuilder.equal(
                    root.get("profissional").get("id"), 
                    profissionalId
                ));
            }

            // Filtro opcional: especialidade
            if (especialidade != null && !especialidade.trim().isEmpty()) {
                Join<Object, Object> especialidadeJoin = root.join("especialidade", JoinType.LEFT);
                predicates.add(criteriaBuilder.equal(
                    criteriaBuilder.upper(especialidadeJoin.get("nome")), 
                    especialidade.toUpperCase()
                ));
            }

            // Filtro opcional: período (data início e fim)
            if (dataInicio != null && dataFim != null) {
                predicates.add(criteriaBuilder.between(
                    root.get("dataHora"), 
                    dataInicio, 
                    dataFim
                ));
            }

            // Filtro opcional: lista de status
            if (statusList != null && !statusList.isEmpty()) {
                predicates.add(root.get("status").in(statusList));
            }

            // Ordenação por data/hora
            query.orderBy(criteriaBuilder.asc(root.get("dataHora")));

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
