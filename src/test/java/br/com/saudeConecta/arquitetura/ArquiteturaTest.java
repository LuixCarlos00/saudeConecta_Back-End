package br.com.saudeConecta.arquitetura;

import br.com.saudeConecta.infrastructure.persistence.listener.AssinaturaTenantCacheListener;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Regras de dependência entre camadas (PLANO-ARQUITETURA-SAUDECONECTA §3.1, regras 1 a 5).
 *
 * <p>Desde a fase B5 os pacotes seguem a arquitetura-alvo, então cada camada é casada só
 * pelo nome-alvo ({@code application}, {@code config}, {@code presentation},
 * {@code infrastructure}).
 *
 * <p>As regras 1 a 3 são congeladas ({@link FreezingArchRule}): as violações que já existiam
 * ficam em {@code src/test/resources/archunit_store} e não quebram o build, mas qualquer
 * violação nova quebra. Violação corrigida sai sozinha do store, que só encolhe.
 * As regras 4 e 5 já passam e não são congeladas.
 */
@DisplayName("Arquitetura - regras de dependência entre camadas")
class ArquiteturaTest {

    private static final String RAIZ = "br.com.saudeConecta";

    private static final String APLICACAO = "..application..";
    private static final String TRANSVERSAL = "..config..";
    private static final String APRESENTACAO = "..presentation..";
    private static final String INFRAESTRUTURA = "..infrastructure..";

    private static JavaClasses classes;

    @BeforeAll
    static void importarClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(RAIZ);
    }

    @Test
    @DisplayName("Regra 1: domain não depende de application, presentation, config nem infrastructure (exceto o listener JPA)")
    void dominioNaoDependeDeCamadasDeFora() {
        ArchRule regra = noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat(
                        resideInAnyPackage(APLICACAO, TRANSVERSAL, APRESENTACAO, INFRAESTRUTURA)
                                // única exceção: o @EntityListeners de AssinaturaTenant; outras classes do pacote não
                                .and(not(equivalentTo(AssinaturaTenantCacheListener.class))))
                .because("a entidade é o centro: quem conhece DTO, service ou configuração é a camada de fora");

        FreezingArchRule.freeze(regra).check(classes);
    }

    @Test
    @DisplayName("Regra 2: presentation não depende de infrastructure")
    void apresentacaoNaoTocaRepository() {
        ArchRule regra = noClasses().that().resideInAPackage(APRESENTACAO)
                .should().dependOnClassesThat().resideInAPackage(INFRAESTRUTURA)
                .because("controller só fala HTTP e chama a aplicação; repository é detalhe de persistência");

        FreezingArchRule.freeze(regra).check(classes);
    }

    @Test
    @DisplayName("Regra 3: application não depende de presentation")
    void aplicacaoNaoDependeDaApresentacao() {
        ArchRule regra = noClasses().that().resideInAPackage(APLICACAO)
                .should().dependOnClassesThat().resideInAPackage(APRESENTACAO)
                .because("o caso de uso não pode depender do adaptador web que o chama");

        FreezingArchRule.freeze(regra).check(classes);
    }

    @Test
    @DisplayName("Regra 4: todo @RestController fica em presentation.controller")
    void controllersFicamEmPresentationController() {
        ArchRule regra = classes().that().areAnnotatedWith(RestController.class)
                .should().resideInAPackage("..presentation.controller..")
                .because("a entrada HTTP tem um lugar só");

        regra.check(classes);
    }

    @Test
    @DisplayName("Regra 5: não existem mais os pacotes antigos infra, usecase e util")
    void pacotesAntigosNaoVoltam() {
        // "..infra.." casa só o segmento exato "infra"; "infrastructure" não é atingido.
        ArchRule regra = noClasses().should().resideInAnyPackage("..infra..", "..usecase..", "..util..")
                .because("a fase B5 levou esse código para config, application e infrastructure");

        regra.check(classes);
    }
}
