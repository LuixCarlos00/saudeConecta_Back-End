package br.com.saudeConecta.domain.planos;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AssinaturaTenant - renovação de ciclo")
class AssinaturaTenantTest {

    private AssinaturaTenant assinaturaVencendoEm(LocalDate vencimento) {
        return AssinaturaTenant.builder()
                .status(StatusAssinatura.ATIVA)
                .dataVencimento(vencimento)
                .build();
    }

    @Test
    @DisplayName("Vencimento não desliza no fim do mês: 31/jan → 28/fev → 31/mar")
    void vencimentoNaoDeslizaNoFimDoMes() {
        AssinaturaTenant assinatura = assinaturaVencendoEm(LocalDate.of(2027, 1, 31));

        assinatura.renovarCiclo(LocalDate.of(2027, 1, 31));
        assertThat(assinatura.getDataVencimento()).isEqualTo(LocalDate.of(2027, 2, 28));
        assertThat(assinatura.getDiaCobranca()).isEqualTo(31);

        assinatura.renovarCiclo(LocalDate.of(2027, 2, 28));
        assertThat(assinatura.getDataVencimento()).isEqualTo(LocalDate.of(2027, 3, 31));
        assertThat(assinatura.getDataProximaCobranca()).isEqualTo(LocalDate.of(2027, 3, 31));
    }

    @Test
    @DisplayName("Pagamento atrasado mantém o dia âncora do ciclo")
    void pagamentoAtrasadoMantemDiaAncora() {
        AssinaturaTenant assinatura = assinaturaVencendoEm(LocalDate.of(2027, 4, 5));
        assinatura.setDiaCobranca(5);

        assinatura.renovarCiclo(LocalDate.of(2027, 4, 9));

        assertThat(assinatura.getDataVencimento()).isEqualTo(LocalDate.of(2027, 5, 5));
    }

    @Test
    @DisplayName("Pagamento antecipado não gera vencimento no dia seguinte")
    void pagamentoAntecipadoAvancaUmCicloInteiro() {
        AssinaturaTenant assinatura = assinaturaVencendoEm(LocalDate.of(2027, 4, 5));
        assinatura.setDiaCobranca(5);

        assinatura.renovarCiclo(LocalDate.of(2027, 4, 4));

        assertThat(assinatura.getDataVencimento()).isEqualTo(LocalDate.of(2027, 5, 5));
    }

    @Test
    @DisplayName("Primeira fatura paga durante o trial ancora no dia do pagamento sem encurtar o trial")
    void primeiraFaturaDuranteTrial() {
        AssinaturaTenant assinatura = assinaturaVencendoEm(LocalDate.of(2027, 3, 10));
        assinatura.setStatus(StatusAssinatura.TRIAL);

        assinatura.renovarCiclo(LocalDate.of(2027, 3, 3));

        assertThat(assinatura.getDiaCobranca()).isEqualTo(3);
        assertThat(assinatura.getDataVencimento()).isEqualTo(LocalDate.of(2027, 4, 3));
    }

    @Test
    @DisplayName("Sem vencimento anterior, renova a partir da data do pagamento")
    void semVencimentoAnterior() {
        AssinaturaTenant assinatura = assinaturaVencendoEm(null);

        assinatura.renovarCiclo(LocalDate.of(2027, 6, 15));

        assertThat(assinatura.getDataVencimento()).isEqualTo(LocalDate.of(2027, 7, 15));
    }

    @Test
    @DisplayName("A1 - Âncora no dia 5; link novo (preapproval trocado) pago no dia 22: a âncora vira 22")
    void preapprovalNovoRedefineDiaAncora() {
        AssinaturaTenant assinatura = assinaturaVencendoEm(LocalDate.of(2027, 4, 5));
        assinatura.setStatus(StatusAssinatura.SUSPENSA);
        assinatura.setDiaCobranca(5);
        assinatura.setMpPreapprovalId("pa-antigo");
        assinatura.setMpStatus("paused");

        assinatura.vincularNovoPreapproval("pa-novo", "pending", "https://mp/checkout", "p@x.com");
        assertThat(assinatura.getDiaCobranca()).isNull();
        assertThat(assinatura.getMpPreapprovalSubstituidoId()).isEqualTo("pa-antigo");

        assinatura.renovarCiclo(LocalDate.of(2027, 4, 22));

        assertThat(assinatura.getDiaCobranca()).isEqualTo(22);
        assertThat(assinatura.getDataVencimento()).isEqualTo(LocalDate.of(2027, 5, 22));

        assinatura.renovarCiclo(LocalDate.of(2027, 5, 22));
        assertThat(assinatura.getDataVencimento()).isEqualTo(LocalDate.of(2027, 6, 22));
    }

    @Test
    @DisplayName("A1 - Desvincular guarda o atual como substituído e limpa o vínculo; sem preapproval não faz nada")
    void desvincularGuardaSubstituido() {
        AssinaturaTenant assinatura = assinaturaVencendoEm(LocalDate.of(2027, 4, 5));
        assinatura.desvincularPreapproval();
        assertThat(assinatura.getMpPreapprovalSubstituidoId()).isNull();

        assinatura.setMpPreapprovalId("pa-1");
        assinatura.setMpStatus("pending");
        assinatura.setMpInitPoint("https://mp/checkout");
        assinatura.desvincularPreapproval();

        assertThat(assinatura.getMpPreapprovalSubstituidoId()).isEqualTo("pa-1");
        assertThat(assinatura.temPreapproval()).isFalse();
        assertThat(assinatura.getMpStatus()).isNull();
        assertThat(assinatura.getMpInitPoint()).isNull();
    }
}
