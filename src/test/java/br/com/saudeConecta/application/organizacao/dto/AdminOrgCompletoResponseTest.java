package br.com.saudeConecta.application.organizacao.dto;

import br.com.saudeConecta.domain.admin.AdminOrganizacao;
import br.com.saudeConecta.domain.admin.AdminOrganizacao.StatusAdmin;
import br.com.saudeConecta.domain.endereco.Endereco;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.organizacao.TipoOrganizacao;
import br.com.saudeConecta.domain.usuario.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AdminOrgCompletoResponse.fromEntity - admin, organização e endereço opcionais")
class AdminOrgCompletoResponseTest {

    private static Usuario usuario(String login) {
        return Usuario.builder().login(login).senha("x").build();
    }

    @Test
    @DisplayName("Admin sem organização: campos da organização, do endereço e CPF ficam nulos")
    void semOrganizacao() {
        AdminOrganizacao admin = AdminOrganizacao.builder().id(1L).nome("Ana").cargo("Dona").email("a@a.com")
                .isOwner(true).status(StatusAdmin.ATIVO).usuario(usuario("111")).build();

        AdminOrgCompletoResponse r = AdminOrgCompletoResponse.fromEntity(admin);

        assertThat(r.id()).isEqualTo(1L);
        assertThat(r.nome()).isEqualTo("Ana");
        assertThat(r.cargo()).isEqualTo("Dona");
        assertThat(r.email()).isEqualTo("a@a.com");
        assertThat(r.isOwner()).isTrue();
        assertThat(r.status()).isEqualTo("ATIVO");
        assertThat(r.organizacaoId()).isNull();
        assertThat(r.nomeClinica()).isNull();
        assertThat(r.cnpj()).isNull();
        // org nula => cnpj nulo, então o CPF sai do login do usuário
        assertThat(r.cpf()).isEqualTo("111");
        assertThat(r.tipoClinica()).isNull();
        assertThat(r.enderecoId()).isNull();
        assertThat(r.cep()).isNull();
    }

    @Test
    @DisplayName("Admin sem status mantém status nulo")
    void semStatus() {
        AdminOrganizacao admin = AdminOrganizacao.builder().id(1L).build();

        AdminOrgCompletoResponse r = AdminOrgCompletoResponse.fromEntity(admin);

        assertThat(r.status()).isNull();
        assertThat(r.cpf()).isNull();
    }

    @Test
    @DisplayName("Organização com CNPJ: CPF fica nulo e todos os dados são mapeados")
    void organizacaoComCnpjEEndereco() {
        Endereco end = Endereco.builder().endCodigo(5L).endCep("01001000").endUF("SP").endMunicipio("Sao Paulo")
                .endBairro("Se").endRua("Praca").endNumero(10L).endComplemento("sala 1").build();
        Organizacao org = Organizacao.builder().id(2L).nome("Clinica X").razaoSocial("X LTDA").cnpj("123")
                .tipo(TipoOrganizacao.CLINICA_MEDICA).email("c@c.com").telefone("1100").endereco(end).build();
        AdminOrganizacao admin = AdminOrganizacao.builder().id(1L).organizacao(org).usuario(usuario("111")).build();

        AdminOrgCompletoResponse r = AdminOrgCompletoResponse.fromEntity(admin);

        assertThat(r.organizacaoId()).isEqualTo(2L);
        assertThat(r.nomeClinica()).isEqualTo("Clinica X");
        assertThat(r.razaoSocial()).isEqualTo("X LTDA");
        assertThat(r.cnpj()).isEqualTo("123");
        assertThat(r.cpf()).isNull();
        assertThat(r.tipoClinica()).isEqualTo("CLINICA_MEDICA");
        assertThat(r.emailClinica()).isEqualTo("c@c.com");
        assertThat(r.telefoneClinica()).isEqualTo("1100");
        assertThat(r.enderecoId()).isEqualTo(5L);
        assertThat(r.cep()).isEqualTo("01001000");
        assertThat(r.uf()).isEqualTo("SP");
        assertThat(r.municipio()).isEqualTo("Sao Paulo");
        assertThat(r.bairro()).isEqualTo("Se");
        assertThat(r.rua()).isEqualTo("Praca");
        assertThat(r.numero()).isEqualTo(10L);
        assertThat(r.complemento()).isEqualTo("sala 1");
    }

    @Test
    @DisplayName("CNPJ em branco com usuário usa o login como CPF; sem usuário o CPF é nulo; tipo e endereço nulos")
    void cnpjEmBranco() {
        Organizacao org = Organizacao.builder().id(2L).cnpj("  ").build();

        AdminOrgCompletoResponse comUsuario = AdminOrgCompletoResponse.fromEntity(
                AdminOrganizacao.builder().id(1L).organizacao(org).usuario(usuario("222")).build());
        AdminOrgCompletoResponse semUsuario = AdminOrgCompletoResponse.fromEntity(
                AdminOrganizacao.builder().id(1L).organizacao(org).build());

        assertThat(comUsuario.cnpj()).isEqualTo("  ");
        assertThat(comUsuario.cpf()).isEqualTo("222");
        assertThat(comUsuario.tipoClinica()).isNull();
        assertThat(comUsuario.enderecoId()).isNull();
        assertThat(semUsuario.cpf()).isNull();
    }

    @Test
    @DisplayName("CNPJ nulo com usuário usa o login como CPF")
    void cnpjNulo() {
        Organizacao org = Organizacao.builder().id(2L).build();

        AdminOrgCompletoResponse r = AdminOrgCompletoResponse.fromEntity(
                AdminOrganizacao.builder().id(1L).organizacao(org).usuario(usuario("333")).build());

        assertThat(r.cpf()).isEqualTo("333");
    }
}
