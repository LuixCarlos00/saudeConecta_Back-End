package br.com.saudeConecta.application.prontuario;

import br.com.saudeConecta.application.exception.BusinessException;
import br.com.saudeConecta.domain.prontuario.PlanejamentoTerapeutico;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Aplica, na edição do prontuário (médico e dentista), a lista de planejamentos enviada pelo front
 * sobre a lista gravada, sem apagar e recriar: recriar perdia a assinatura do paciente
 * ({@code assinaturaBase64}, {@code dataAssinatura}, {@code ipOrigem}, token do link).
 *
 * <ul>
 *   <li>item com {@code id}: atualiza o planejamento existente (que precisa ser deste prontuário);</li>
 *   <li>item sem {@code id}: cria um planejamento novo, PENDENTE;</li>
 *   <li>planejamento gravado que não veio: é removido, <b>exceto o ASSINADO</b>, que nunca é removido;</li>
 *   <li>planejamento ASSINADO não é alterado: mudar procedimento ou valor recebe 409
 *       {@code PLANEJAMENTO_ASSINADO}; os demais campos dele são ignorados.</li>
 * </ul>
 */
final class EdicaoPlanejamentos {

    /** Código do 409 quando a edição tenta mudar um planejamento já assinado pelo paciente. */
    static final String CODE_PLANEJAMENTO_ASSINADO = "PLANEJAMENTO_ASSINADO";

    private EdicaoPlanejamentos() {
    }

    /**
     * Item da edição, já convertido do DTO do médico ou do dentista.
     *
     * @param id                    id do planejamento existente (null = novo)
     * @param dataProcedimento      data do procedimento
     * @param procedimentoRealizado descrição do procedimento
     * @param valor                 valor do procedimento
     */
    record Item(Long id, LocalDate dataProcedimento, String procedimentoRealizado, BigDecimal valor) {
    }

    /**
     * Sincroniza a lista gravada com a lista da edição.
     *
     * @param gravados     coleção gerenciada do prontuário (alterada no lugar; orphanRemoval apaga os removidos)
     * @param itens        itens da edição; null = o front não mandou a lista, nada muda
     * @param criarNovo    monta um planejamento novo (com consulta, profissional e tenant do prontuário)
     * @param atualizar    aplica data, procedimento e valor num planejamento não assinado
     * @param prontuarioId id do prontuário, para a mensagem de erro
     * @throws IllegalArgumentException item com id que não é deste prontuário (400)
     * @throws BusinessException        409 {@code PLANEJAMENTO_ASSINADO}
     */
    static void sincronizar(List<PlanejamentoTerapeutico> gravados, List<Item> itens,
                            Function<Item, PlanejamentoTerapeutico> criarNovo,
                            BiConsumer<PlanejamentoTerapeutico, Item> atualizar,
                            Long prontuarioId) {
        if (itens == null) {
            return;
        }
        Set<Long> mantidos = new HashSet<>();
        for (Item item : itens) {
            if (item.id() == null) {
                continue;
            }
            PlanejamentoTerapeutico gravado = gravados.stream()
                    .filter(p -> item.id().equals(p.getId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Planejamento " + item.id() + " nao pertence ao prontuario " + prontuarioId));
            if (gravado.isAssinado()) {
                recusarSeAlterado(gravado, item);
            } else {
                atualizar.accept(gravado, item);
            }
            mantidos.add(gravado.getId());
        }

        // Só depois de validar tudo: um 409 no meio não deixa a coleção pela metade.
        gravados.removeIf(p -> !p.isAssinado() && !mantidos.contains(p.getId()));
        itens.stream().filter(i -> i.id() == null).map(criarNovo).forEach(gravados::add);
    }

    private static void recusarSeAlterado(PlanejamentoTerapeutico assinado, Item item) {
        boolean mudouProcedimento = !Objects.equals(assinado.getProcedimentoRealizado(), item.procedimentoRealizado());
        boolean mudouValor = assinado.getValor() == null
                ? item.valor() != null
                : item.valor() == null || assinado.getValor().compareTo(item.valor()) != 0;
        if (mudouProcedimento || mudouValor) {
            throw new BusinessException(
                    "O planejamento " + assinado.getId() + " já foi assinado pelo paciente e não pode ser alterado.",
                    HttpStatus.CONFLICT, CODE_PLANEJAMENTO_ASSINADO);
        }
    }
}
