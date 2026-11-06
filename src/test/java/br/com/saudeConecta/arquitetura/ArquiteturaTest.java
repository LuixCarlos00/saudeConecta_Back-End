package br.com.saudeConecta.arquitetura;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Regras de dependência entre camadas (PLANO-ARQUITETURA-SAUDECONECTA §3.1, regras 1 a 4).
 *
 * <p>Os pacotes de hoje ainda não seguem a arquitetura-alvo; isso só acontece na fase B5.
 * Por isso cada camada lista o nome-alvo e os nomes atuais equivalentes:
 * {@code application} = {@code service}, {@code usecase} e {@code email}, e
 * {@code config} = {@code config} e {@code infra}. Quando a B5 mover os pacotes, os nomes
 * antigos deixam de casar e as regras continuam valendo pelo nome-alvo.
 *
 * <p>As regras 1 a 3 são congeladas ({@link FreezingArchRule}): as violações que já existiam
 * ficam em {@code src/test/resources/archunit_store} e não quebram o build, mas qualquer
 * violação nova quebra. Violação corrigida sai sozinha do store, que só encolhe.
 * A regra 4 já passa hoje e não é congelada. A regra 5 do plano entra depois da B5.
 */
@DisplayName("Arquitetura - regras de dependência entre camadas")
class ArquiteturaTest {

    private static final String RAIZ = "br.com.saudeConecta";

    private static final String[] APLICACAO = {"..application..", "..service..", "..usecase..", "..email.."};
    private static final String[] TRANSVERSAL = {"..config..", "..infra.."};
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
    @DisplayName("Regra 1: domain não depende de application, presentation nem config")
    void dominioNaoDependeDeCamadasDeFora() {
        ArchRule regra = noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(juntar(APLICACAO, TRANSVERSAL, APRESENTACAO))
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
        ArchRule regra = noClasses().that().resideInAnyPackage(APLICACAO)
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

    private static String[] juntar(String[] a, String[] b, String c) {
        String[] todos = new String[a.length + b.length + 1];
        System.arraycopy(a, 0, todos, 0, a.length);
        System.arraycopy(b, 0, todos, a.length, b.length);
        todos[todos.length - 1] = c;
        return todos;
    }
}
