package br.com.saudeConecta.application.service;

import br.com.saudeConecta.application.port.out.administrador.AdministradorOutputPort;
import br.com.saudeConecta.domain.administrador.Administrador;
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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdministradorServiceTest {

    @Mock
    private AdministradorOutputPort administradorOutputPort;

    @InjectMocks
    private AdministradorService administradorService;

    private Administrador administrador;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        administrador = new Administrador();
        administrador.setAdmCodigo(1L);
        administrador.setAdmNome("Admin Test");
        administrador.setAdmEmail("admin@test.com");
        
        pageable = PageRequest.of(0, 10);
    }

    @Test
    void buscarPorId_DeveRetornarAdministrador_QuandoExistir() {
        // Arrange
        when(administradorOutputPort.findById(1L)).thenReturn(Optional.of(administrador));

        // Act
        Optional<Administrador> result = administradorService.buscarPorId(1L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(administrador, result.get());
        verify(administradorOutputPort).findById(1L);
    }

    @Test
    void buscarPorId_DeveRetornarVazio_QuandoNaoExistir() {
        // Arrange
        when(administradorOutputPort.findById(1L)).thenReturn(Optional.empty());

        // Act
        Optional<Administrador> result = administradorService.buscarPorId(1L);

        // Assert
        assertFalse(result.isPresent());
        verify(administradorOutputPort).findById(1L);
    }

    @Test
    void buscarPorIdUsuario_DeveRetornarAdministrador_QuandoExistir() {
        // Arrange
        when(administradorOutputPort.findByAdmUsuario_Id(1L)).thenReturn(Optional.of(administrador));

        // Act
        Optional<Administrador> result = administradorService.buscarPorIdUsuario(1L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(administrador, result.get());
        verify(administradorOutputPort).findByAdmUsuario_Id(1L);
    }

    @Test
    void buscarTodos_DeveRetornarListaDeAdministradores() {
        // Arrange
        List<Administrador> administradores = Arrays.asList(administrador);
        when(administradorOutputPort.findAll()).thenReturn(administradores);

        // Act
        List<Administrador> result = administradorService.buscarTodos();

        // Assert
        assertEquals(1, result.size());
        assertEquals(administrador, result.get(0));
        verify(administradorOutputPort).findAll();
    }

    @Test
    void buscarTodosComPaginacao_DeveRetornarPaginaDeAdministradores() {
        // Arrange
        List<Administrador> administradores = Arrays.asList(administrador);
        Page<Administrador> page = new PageImpl<>(administradores, pageable, 1);
        when(administradorOutputPort.findAll(pageable)).thenReturn(page);

        // Act
        Page<Administrador> result = administradorService.buscarTodos(pageable);

        // Assert
        assertEquals(1, result.getContent().size());
        assertEquals(administrador, result.getContent().get(0));
        verify(administradorOutputPort).findAll(pageable);
    }

    @Test
    void cadastrar_DeveRetornarAdministradorSalvo() {
        // Arrange
        when(administradorOutputPort.save(administrador)).thenReturn(administrador);

        // Act
        Administrador result = administradorService.cadastrar(administrador);

        // Assert
        assertEquals(administrador, result);
        verify(administradorOutputPort).save(administrador);
    }

    @Test
    void deletar_DeveLancarException_QuandoIdInvalido() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> administradorService.deletar(null)
        );
        
        assertEquals("ID inválido", exception.getMessage());
        verify(administradorOutputPort, never()).deleteById(any());
    }

    @Test
    void deletar_DeveLancarException_QuandoRegistroNaoExistir() {
        // Arrange
        when(administradorOutputPort.existsById(1L)).thenReturn(false);

        // Act & Assert
        Exception exception = assertThrows(
                Exception.class,
                () -> administradorService.deletar(1L)
        );
        
        assertEquals("Registro não encontrado", exception.getMessage());
        verify(administradorOutputPort).existsById(1L);
        verify(administradorOutputPort, never()).deleteById(any());
    }

    @Test
    void deletar_DeveDeletarComSucesso_QuandoRegistroExistir() throws Exception {
        // Arrange
        when(administradorOutputPort.existsById(1L)).thenReturn(true);
        doNothing().when(administradorOutputPort).deleteById(1L);

        // Act
        assertDoesNotThrow(() -> administradorService.deletar(1L));

        // Assert
        verify(administradorOutputPort).existsById(1L);
        verify(administradorOutputPort).deleteById(1L);
    }
}
