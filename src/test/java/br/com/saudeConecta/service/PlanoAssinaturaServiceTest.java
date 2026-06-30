package br.com.saudeConecta.service;

import br.com.saudeConecta.domain.planos.PlanoAssinatura;
import br.com.saudeConecta.domain.planos.TipoPlano;
import br.com.saudeConecta.infra.exceptions.BusinessException;
import br.com.saudeConecta.infrastructure.persistence.repository.PlanoAssinaturaRepository;
import br.com.saudeConecta.presentation.dto.planos.PlanoAssinaturaRequest;
import br.com.saudeConecta.presentation.dto.planos.PlanoAssinaturaResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlanoAssinaturaService - Testes unitários")
class PlanoAssinaturaServiceTest {

    @Mock private PlanoAssinaturaRepository planoAssinaturaRepository;

    @InjectMocks
    private PlanoAssinaturaService service;

    // ========== Helpers ==========

    private PlanoAssinatura criarPlano(Long id) {
        PlanoAssinatura p = new PlanoAssinatura();
        p.setId(id);
        p.setNome("Plano Starter");
        p.setTipo(TipoPlano.STARTER);
        p.setValorMensal(BigDecimal.valueOf(99.90));
        p.setLimiteAdminOrg(1);
        p.setLimiteProfissional(3);
        p.setLimiteSecretaria(2);
        p.setValorAdicionalAdmin(BigDecimal.valueOf(50));
        p.setValorAdicionalProfissional(BigDecimal.valueOf(30));
        p.setValorAdicionalSecretaria(BigDecimal.valueOf(20));
        p.setAtivo(true);
        return p;
    }

    private PlanoAssinaturaRequest criarRequest() {
        return new PlanoAssinaturaRequest(
                "Plano Starter",
                "Plano básico",
                TipoPlano.STARTER,
                BigDecimal.valueOf(99.90),
                1,
                3,
                2,
                BigDecimal.valueOf(50),
                BigDecimal.valueOf(30),
                BigDecimal.valueOf(20)
        );
    }

    // ========== listarPlanosAtivos ==========

    @Nested
    @DisplayName("listarPlanosAtivos")
    class ListarPlanosAtivos {

        @Test
        @DisplayName("Deve retornar lista de planos ativos")
        void deveRetornarPlanosAtivos() {
            when(planoAssinaturaRepository.findByAtivoTrue())
                    .thenReturn(List.of(criarPlano(1L), criarPlano(2L)));

            List<PlanoAssinaturaResponse> resultado = service.listarPlanosAtivos();

            assertThat(resultado).hasSize(2);
        }

        @Test
        @DisplayName("Deve retornar lista vazia quando não há planos ativos")
        void deveRetornarListaVaziaQuandoSemPlanos() {
            when(planoAssinaturaRepository.findByAtivoTrue()).thenReturn(List.of());

            List<PlanoAssinaturaResponse> resultado = service.listarPlanosAtivos();

            assertThat(resultado).isEmpty();
        }
    }

    // ========== buscarPorId ==========

    @Nested
    @DisplayName("buscarPorId")
    class BuscarPorId {

        @Test
        @DisplayName("Deve retornar plano quando encontrado e ativo")
        void deveRetornarPlanoAtivo() {
            when(planoAssinaturaRepository.findByIdAndAtivoTrue(1L))
                    .thenReturn(Optional.of(criarPlano(1L)));

            PlanoAssinaturaResponse response = service.buscarPorId(1L);

            assertThat(response).isNotNull();
            assertThat(response.nome()).isEqualTo("Plano Starter");
        }

        @Test
        @DisplayName("Deve lançar BusinessException quando plano não encontrado")
        void deveLancarExcecaoQuandoNaoEncontrado() {
            when(planoAssinaturaRepository.findByIdAndAtivoTrue(99L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.buscarPorId(99L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Plano nao encontrado");
        }
    }

    // ========== buscarPorTipo ==========

    @Nested
    @DisplayName("buscarPorTipo")
    class BuscarPorTipo {

        @Test
        @DisplayName("Deve retornar plano pelo tipo")
        void deveRetornarPlanoPorTipo() {
            when(planoAssinaturaRepository.findByTipo(TipoPlano.STARTER))
                    .thenReturn(Optional.of(criarPlano(1L)));

            PlanoAssinaturaResponse response = service.buscarPorTipo(TipoPlano.STARTER);

            assertThat(response).isNotNull();
            assertThat(response.tipo()).isEqualTo("STARTER");
        }

        @Test
        @DisplayName("Deve lançar BusinessException quando tipo não encontrado")
        void deveLancarExcecaoQuandoTipoNaoEncontrado() {
            when(planoAssinaturaRepository.findByTipo(TipoPlano.BUSINESS))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.buscarPorTipo(TipoPlano.BUSINESS))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Plano nao encontrado para o tipo");
        }
    }

    // ========== atualizarPlano ==========

    @Nested
    @DisplayName("atualizarPlano")
    class AtualizarPlano {

        @Test
        @DisplayName("Deve atualizar plano com dados válidos")
        void deveAtualizarPlano() {
            PlanoAssinatura plano = criarPlano(1L);
            when(planoAssinaturaRepository.findById(1L)).thenReturn(Optional.of(plano));
            when(planoAssinaturaRepository.save(any())).thenReturn(plano);

            PlanoAssinaturaResponse response = service.atualizarPlano(1L, criarRequest());

            assertThat(response).isNotNull();
            assertThat(plano.getNome()).isEqualTo("Plano Starter");
            verify(planoAssinaturaRepository).save(plano);
        }

        @Test
        @DisplayName("Deve lançar BusinessException quando plano não encontrado ao atualizar")
        void deveLancarExcecaoQuandoNaoEncontrado() {
            when(planoAssinaturaRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.atualizarPlano(99L, criarRequest()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Plano nao encontrado");
        }
    }

    // ========== desativarPlano ==========

    @Nested
    @DisplayName("desativarPlano")
    class DesativarPlano {

        @Test
        @DisplayName("Deve desativar plano existente")
        void deveDesativarPlano() {
            PlanoAssinatura plano = criarPlano(1L);
            when(planoAssinaturaRepository.findById(1L)).thenReturn(Optional.of(plano));

            service.desativarPlano(1L);

            assertThat(plano.getAtivo()).isFalse();
            verify(planoAssinaturaRepository).save(plano);
        }

        @Test
        @DisplayName("Deve lançar BusinessException quando plano não encontrado ao desativar")
        void deveLancarExcecaoQuandoNaoEncontrado() {
            when(planoAssinaturaRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.desativarPlano(99L))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ========== buscarEntidadePorId ==========

    @Nested
    @DisplayName("buscarEntidadePorId")
    class BuscarEntidadePorId {

        @Test
        @DisplayName("Deve retornar entidade PlanoAssinatura")
        void deveRetornarEntidade() {
            PlanoAssinatura plano = criarPlano(1L);
            when(planoAssinaturaRepository.findByIdAndAtivoTrue(1L))
                    .thenReturn(Optional.of(plano));

            PlanoAssinatura resultado = service.buscarEntidadePorId(1L);

            assertThat(resultado).isNotNull();
            assertThat(resultado.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Deve lançar BusinessException quando entidade não encontrada")
        void deveLancarExcecaoQuandoNaoEncontrada() {
            when(planoAssinaturaRepository.findByIdAndAtivoTrue(99L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.buscarEntidadePorId(99L))
                    .isInstanceOf(BusinessException.class);
        }
    }
}
