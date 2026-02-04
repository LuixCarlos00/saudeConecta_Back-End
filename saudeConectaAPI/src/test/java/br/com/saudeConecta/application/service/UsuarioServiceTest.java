package br.com.saudeConecta.application.service;

import br.com.saudeConecta.application.port.out.usuario.UsuarioOutputPort;
import br.com.saudeConecta.domain.usuario.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioOutputPort usuarioOutputPort;

    @InjectMocks
    private UsuarioService usuarioService;

    private Usuario usuario;
    private UserDetails userDetails;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setLogin("testuser");
        usuario.setSenha("password");
        usuario.setTipoUsuario((byte) 1);
        usuario.setStatus((byte) 1);
        
        userDetails = mock(UserDetails.class);
        pageable = PageRequest.of(0, 10);
    }

    @Test
    void buscarPorId_DeveRetornarUsuario_QuandoExistir() {
        // Arrange
        when(usuarioOutputPort.findById(1L)).thenReturn(Optional.of(usuario));

        // Act
        Optional<Usuario> result = usuarioService.buscarPorId(1L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(usuario, result.get());
        verify(usuarioOutputPort).findById(1L);
    }

    @Test
    void buscarPorLogin_DeveRetornarUsuario_QuandoExistir() {
        // Arrange
        when(usuarioOutputPort.findUsuarioByLogin("testuser")).thenReturn(Optional.of(usuario));

        // Act
        Optional<Usuario> result = usuarioService.buscarPorLogin("testuser");

        // Assert
        assertTrue(result.isPresent());
        assertEquals(usuario, result.get());
        verify(usuarioOutputPort).findUsuarioByLogin("testuser");
    }

    @Test
    void buscarUserDetailsPorLogin_DeveRetornarUserDetails_QuandoExistir() {
        // Arrange
        when(usuarioOutputPort.findByLogin("testuser")).thenReturn(userDetails);

        // Act
        UserDetails result = usuarioService.buscarUserDetailsPorLogin("testuser");

        // Assert
        assertEquals(userDetails, result);
        verify(usuarioOutputPort).findByLogin("testuser");
    }

    @Test
    void existePorLogin_DeveRetornarTrue_QuandoLoginExistir() {
        // Arrange
        when(usuarioOutputPort.existsByLogin("testuser")).thenReturn(true);

        // Act
        boolean result = usuarioService.existePorLogin("testuser");

        // Assert
        assertTrue(result);
        verify(usuarioOutputPort).existsByLogin("testuser");
    }

    @Test
    void existePorLogin_DeveRetornarFalse_QuandoLoginNaoExistir() {
        // Arrange
        when(usuarioOutputPort.existsByLogin("nonexistent")).thenReturn(false);

        // Act
        boolean result = usuarioService.existePorLogin("nonexistent");

        // Assert
        assertFalse(result);
        verify(usuarioOutputPort).existsByLogin("nonexistent");
    }

    @Test
    void buscarTodos_DeveRetornarListaDeUsuarios() {
        // Arrange
        List<Usuario> usuarios = Arrays.asList(usuario);
        when(usuarioOutputPort.findAll()).thenReturn(usuarios);

        // Act
        List<Usuario> result = usuarioService.buscarTodos();

        // Assert
        assertEquals(1, result.size());
        assertEquals(usuario, result.get(0));
        verify(usuarioOutputPort).findAll();
    }

    @Test
    void buscarTodosComPaginacao_DeveRetornarPaginaDeUsuarios() {
        // Arrange
        List<Usuario> usuarios = Arrays.asList(usuario);
        Page<Usuario> page = new PageImpl<>(usuarios, pageable, 1);
        when(usuarioOutputPort.findAll(pageable)).thenReturn(page);

        // Act
        Page<Usuario> result = usuarioService.buscarTodos(pageable);

        // Assert
        assertEquals(1, result.getContent().size());
        assertEquals(usuario, result.getContent().get(0));
        verify(usuarioOutputPort).findAll(pageable);
    }

    @Test
    void cadastrar_DeveRetornarUsuarioSalvo() {
        // Arrange
        when(usuarioOutputPort.save(usuario)).thenReturn(usuario);

        // Act
        Usuario result = usuarioService.cadastrar(usuario);

        // Assert
        assertEquals(usuario, result);
        verify(usuarioOutputPort).save(usuario);
    }

    @Test
    void deletar_DeveLancarException_QuandoIdInvalido() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> usuarioService.deletar(null)
        );
        
        assertEquals("ID inválido", exception.getMessage());
        verify(usuarioOutputPort, never()).deleteById(any());
    }

    @Test
    void deletar_DeveLancarException_QuandoRegistroNaoExistir() {
        // Arrange
        when(usuarioOutputPort.existsById(1L)).thenReturn(false);

        // Act & Assert
        Exception exception = assertThrows(
                Exception.class,
                () -> usuarioService.deletar(1L)
        );
        
        assertEquals("Registro não encontrado", exception.getMessage());
        verify(usuarioOutputPort).existsById(1L);
        verify(usuarioOutputPort, never()).deleteById(any());
    }

    @Test
    void deletar_DeveDeletarComSucesso_QuandoRegistroExistir() throws Exception {
        // Arrange
        when(usuarioOutputPort.existsById(1L)).thenReturn(true);
        doNothing().when(usuarioOutputPort).deleteById(1L);

        // Act
        assertDoesNotThrow(() -> usuarioService.deletar(1L));

        // Assert
        verify(usuarioOutputPort).existsById(1L);
        verify(usuarioOutputPort).deleteById(1L);
    }
}
