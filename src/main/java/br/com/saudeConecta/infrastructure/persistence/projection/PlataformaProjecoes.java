package br.com.saudeConecta.infrastructure.persistence.projection;

import br.com.saudeConecta.domain.planos.StatusAssinatura;
import br.com.saudeConecta.domain.planos.TipoPlano;
import br.com.saudeConecta.domain.usuario.TipoUsuarioNovo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Projeções das agregações do dashboard da plataforma. Cada interface casa com os aliases
 * ({@code AS ...}) da query JPQL correspondente; o banco devolve só o agrupado, nunca entidades.
 */
public final class PlataformaProjecoes {

    private PlataformaProjecoes() {
    }

    /** Quantidade de assinaturas vigentes em um status. */
    public interface ContagemPorStatus {
        StatusAssinatura getStatus();

        Long getQuantidade();
    }

    /** Quantidade de assinaturas por plano base. */
    public interface ContagemPorPlano {
        TipoPlano getPlano();

        Long getQuantidade();
    }

    /** Quantidade e soma mensal das assinaturas pagantes por plano base. */
    public interface MrrPorPlano {
        TipoPlano getPlano();

        Long getQuantidade();

        BigDecimal getMrr();
    }

    /** Contagem agrupada por mês (ano e mês separados, via YEAR()/MONTH()). */
    public interface ContagemMensal {
        Integer getAno();

        Integer getMes();

        Long getQuantidade();
    }

    /** Soma de valores agrupada por mês. */
    public interface ValorMensal {
        Integer getAno();

        Integer getMes();

        BigDecimal getValor();
    }

    /** Clínica com assinatura em situação que pede atenção. */
    public interface ClinicaAtencao {
        Long getOrganizacaoId();

        String getNome();

        TipoPlano getPlano();

        StatusAssinatura getStatus();

        LocalDate getDataVencimento();

        BigDecimal getValorMensal();
    }

    /** Evento do Mercado Pago que exige tratamento manual (sem payload). */
    public interface EventoPendente {
        Long getId();

        LocalDateTime getRecebidoEm();
    }

    /** Usuários ativos por perfil. */
    public interface ContagemPorPerfil {
        TipoUsuarioNovo getPerfil();

        Long getQuantidade();
    }

    /** Consultas do mês por clínica. */
    public interface ConsultasPorClinica {
        Long getOrganizacaoId();

        String getNome();

        TipoPlano getPlano();

        StatusAssinatura getStatus();

        Long getConsultasMes();
    }

    /** Profissionais ativos por clínica. */
    public interface ProfissionaisPorClinica {
        Long getOrganizacaoId();

        Long getQuantidade();
    }
}
