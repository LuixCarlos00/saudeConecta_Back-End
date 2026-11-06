package br.com.saudeConecta.application.historico;

import br.com.saudeConecta.config.tenant.TenantAccessDeniedException;
import br.com.saudeConecta.domain.historicodadospessoais.EntidadeTipo;
import br.com.saudeConecta.domain.historicodadospessoais.HistoricoDadosPessoais;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.usuario.Usuario;
import org.mockito.ArgumentCaptor;
import java.math.BigDecimal;
import java.util.Optional;
import br.com.saudeConecta.config.tenant.TenantHelper;
import br.com.saudeConecta.infrastructure.persistence.repository.HistoricoDadosPessoaisRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.OrganizacaoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("HistoricoDadosPessoaisService - acesso exclusivo do SUPER_ADMIN")
class HistoricoDadosPessoaisServiceTest {

    @Mock private HistoricoDadosPessoaisRepository historicoRepository;
    @Mock private OrganizacaoRepository organizacaoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private TenantHelper tenantHelper;

    @InjectMocks
    private HistoricoDadosPessoaisService service;

    @Test
    @DisplayName("Usuário sem tenant e sem papel SUPER_ADMIN não lê o histórico global")
    void negaSemTenantNaoRoot() {
        when(tenantHelper.isSuperAdmin()).thenReturn(false);

        assertThatThrownBy(() -> service.buscarHistoricoRecentes())
                .isInstanceOf(TenantAccessDeniedException.class);
        assertThatThrownBy(() -> service.buscarHistoricoByUsuario(1L))
                .isInstanceOf(TenantAccessDeniedException.class);
        assertThatThrownBy(() -> service.buscarHistoricoByEntidade("PROFISSIONAL", 1L))
                .isInstanceOf(TenantAccessDeniedException.class);
        verifyNoInteractions(historicoRepository);
    }

    @Test
    @DisplayName("SUPER_ADMIN lê o histórico global")
    void permiteSuperAdmin() {
        when(tenantHelper.isSuperAdmin()).thenReturn(true);
        when(historicoRepository.findTop10ByOrderByCriadoEmDesc()).thenReturn(List.of());

        service.buscarHistoricoRecentes();

        verify(historicoRepository).findTop10ByOrderByCriadoEmDesc();
        verify(historicoRepository, never()).findByUsuario_IdOrderByCriadoEmDesc(any());
    }

    /** Entidade simples para exercitar a comparação por reflection. */
    static class Alvo {
        Long id;
        String nome;
        String telefone;
        BigDecimal valor;
        String usuario;

        Alvo(Long id, String nome, String telefone, BigDecimal valor, String usuario) {
            this.id = id;
            this.nome = nome;
            this.telefone = telefone;
            this.valor = valor;
            this.usuario = usuario;
        }
    }

    @Test
    @DisplayName("registrarAlteracoesDeObjeto ignora objeto nulo sem tocar no repositório")
    void registrarIgnoraNulo() {
        service.registrarAlteracoesDeObjeto(EntidadeTipo.PACIENTE, 1L, 2L, null, new Alvo(1L, "a", null, null, null));
        service.registrarAlteracoesDeObjeto(EntidadeTipo.PACIENTE, 1L, 2L, new Alvo(1L, "a", null, null, null), null);

        verifyNoInteractions(historicoRepository, organizacaoRepository, usuarioRepository);
    }

    @Test
    @DisplayName("registrarAlteracoesDeObjeto rejeita objetos de tipos diferentes")
    void registrarTiposDiferentes() {
        assertThatThrownBy(() -> service.registrarAlteracoesDeObjeto(
                EntidadeTipo.PACIENTE, 1L, 2L, new Alvo(1L, "a", null, null, null), "outro"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Os objetos 'antes' e 'depois' devem ser do mesmo tipo");
    }

    @Test
    @DisplayName("registrarAlteracoesDeObjeto falha quando a organização do contexto não existe")
    void registrarOrganizacaoInexistente() {
        when(tenantHelper.getCurrentTenantId()).thenReturn(10L);
        when(organizacaoRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrarAlteracoesDeObjeto(
                EntidadeTipo.PACIENTE, 1L, 2L, new Alvo(1L, "a", null, null, null), new Alvo(1L, "b", null, null, null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Organização não encontrada no contexto");
    }

    @Test
    @DisplayName("registrarAlteracoesDeObjeto falha quando o usuário não existe")
    void registrarUsuarioInexistente() {
        when(tenantHelper.getCurrentTenantId()).thenReturn(10L);
        when(organizacaoRepository.findById(10L)).thenReturn(Optional.of(new Organizacao()));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrarAlteracoesDeObjeto(
                EntidadeTipo.PACIENTE, 1L, 2L, new Alvo(1L, "a", null, null, null), new Alvo(1L, "b", null, null, null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Usuário não encontrado: 2");
    }

    @Test
    @DisplayName("registrarAlteracoesDeObjeto persiste só os campos alterados, ignorando BigDecimal igual e campos ignorados")
    void registrarPersisteSomenteAlterados() {
        Organizacao org = new Organizacao();
        Usuario usuario = new Usuario();
        when(tenantHelper.getCurrentTenantId()).thenReturn(10L);
        when(organizacaoRepository.findById(10L)).thenReturn(Optional.of(org));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario));

        Alvo antes = new Alvo(1L, "Ana", null, new BigDecimal("250.00"), "u1");
        Alvo depois = new Alvo(9L, "Bia", "9999", new BigDecimal("250"), "u2");

        service.registrarAlteracoesDeObjeto(EntidadeTipo.PACIENTE, 1L, 2L, antes, depois);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<HistoricoDadosPessoais>> captor = ArgumentCaptor.forClass(List.class);
        verify(historicoRepository).saveAll(captor.capture());
        List<HistoricoDadosPessoais> registros = captor.getValue();
        assertThat(registros).extracting(HistoricoDadosPessoais::getCampo)
                .containsExactlyInAnyOrder("nome", "telefone");
        HistoricoDadosPessoais nome = registros.stream().filter(r -> "nome".equals(r.getCampo())).findFirst().orElseThrow();
        assertThat(nome.getValorAnterior()).isEqualTo("Ana");
        assertThat(nome.getValorNovo()).isEqualTo("Bia");
        assertThat(nome.getEntidade()).isEqualTo(EntidadeTipo.PACIENTE);
        assertThat(nome.getIdEntidade()).isEqualTo(1L);
        assertThat(nome.getOrganizacao()).isSameAs(org);
        assertThat(nome.getUsuario()).isSameAs(usuario);
        HistoricoDadosPessoais telefone = registros.stream().filter(r -> "telefone".equals(r.getCampo())).findFirst().orElseThrow();
        assertThat(telefone.getValorAnterior()).isNull();
        assertThat(telefone.getValorNovo()).isEqualTo("9999");
    }

    @Test
    @DisplayName("registrarAlteracoesDeObjeto não salva nada quando não há alteração")
    void registrarSemAlteracao() {
        when(tenantHelper.getCurrentTenantId()).thenReturn(10L);
        when(organizacaoRepository.findById(10L)).thenReturn(Optional.of(new Organizacao()));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(new Usuario()));

        service.registrarAlteracoesDeObjeto(EntidadeTipo.PACIENTE, 1L, 2L,
                new Alvo(1L, "Ana", null, new BigDecimal("1.0"), null),
                new Alvo(1L, "Ana", null, new BigDecimal("1.00"), null));

        verify(historicoRepository, never()).saveAll(any());
    }
}
