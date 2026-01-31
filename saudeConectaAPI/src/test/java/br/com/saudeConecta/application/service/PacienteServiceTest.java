package br.com.saudeConecta.application.service;

import br.com.saudeConecta.application.port.out.paciente.PacienteOutputPort;
import br.com.saudeConecta.domain.paciente.Paciente;
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
class PacienteServiceTest {

    @Mock
    private PacienteOutputPort pacienteOutputPort;

    @InjectMocks
    private PacienteService pacienteService;

    private Paciente paciente;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        paciente = new Paciente();
        paciente.setPaciCodigo(1L);
        paciente.setPaciNome("Paciente Test");
        paciente.setPaciEmail("paciente@test.com");
        paciente.setPaciCpf("12345678901");
        
        pageable = PageRequest.of(0, 10);
    }

    @Test
    void buscarPorId_DeveRetornarPaciente_QuandoExistir() {
        // Arrange
        when(pacienteOutputPort.findById(1L)).thenReturn(Optional.of(paciente));

        // Act
        Optional<Paciente> result = pacienteService.buscarPorId(1L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(paciente, result.get());
        verify(pacienteOutputPort).findById(1L);
    }

    @Test
    void buscarPorEmail_DeveRetornarPaciente_QuandoExistir() {
        // Arrange
        when(pacienteOutputPort.findByPaciEmail("paciente@test.com")).thenReturn(Optional.of(paciente));

        // Act
        Optional<Paciente> result = pacienteService.buscarPorEmail("paciente@test.com");

        // Assert
        assertTrue(result.isPresent());
        assertEquals(paciente, result.get());
        verify(pacienteOutputPort).findByPaciEmail("paciente@test.com");
    }

    @Test
    void buscarPorCpf_DeveRetornarListaDePacientes() {
        // Arrange
        List<Paciente> pacientes = Arrays.asList(paciente);
        when(pacienteOutputPort.findByPaciCpfContainingIgnoreCase("12345678901")).thenReturn(pacientes);

        // Act
        List<Paciente> result = pacienteService.buscarPorCpf("12345678901");

        // Assert
        assertEquals(1, result.size());
        assertEquals(paciente, result.get(0));
        verify(pacienteOutputPort).findByPaciCpfContainingIgnoreCase("12345678901");
    }

    @Test
    void buscarPorRg_DeveRetornarListaDePacientes() {
        // Arrange
        List<Paciente> pacientes = Arrays.asList(paciente);
        when(pacienteOutputPort.findByPaciRgContainingIgnoreCase("RG12345")).thenReturn(pacientes);

        // Act
        List<Paciente> result = pacienteService.buscarPorRg("RG12345");

        // Assert
        assertEquals(1, result.size());
        assertEquals(paciente, result.get(0));
        verify(pacienteOutputPort).findByPaciRgContainingIgnoreCase("RG12345");
    }

    @Test
    void buscarPorTelefone_DeveRetornarListaDePacientes() {
        // Arrange
        List<Paciente> pacientes = Arrays.asList(paciente);
        when(pacienteOutputPort.findByPaciTelefoneContainingIgnoreCase("11999999999")).thenReturn(pacientes);

        // Act
        List<Paciente> result = pacienteService.buscarPorTelefone("11999999999");

        // Assert
        assertEquals(1, result.size());
        assertEquals(paciente, result.get(0));
        verify(pacienteOutputPort).findByPaciTelefoneContainingIgnoreCase("11999999999");
    }

    @Test
    void buscarPorNome_DeveRetornarListaDePacientes() {
        // Arrange
        List<Paciente> pacientes = Arrays.asList(paciente);
        when(pacienteOutputPort.findByPaciNomeContainingIgnoreCase("Test")).thenReturn(pacientes);

        // Act
        List<Paciente> result = pacienteService.buscarPorNome("Test");

        // Assert
        assertEquals(1, result.size());
        assertEquals(paciente, result.get(0));
        verify(pacienteOutputPort).findByPaciNomeContainingIgnoreCase("Test");
    }

    @Test
    void buscarTodos_DeveRetornarListaDePacientes() {
        // Arrange
        List<Paciente> pacientes = Arrays.asList(paciente);
        when(pacienteOutputPort.findAll()).thenReturn(pacientes);

        // Act
        List<Paciente> result = pacienteService.buscarTodos();

        // Assert
        assertEquals(1, result.size());
        assertEquals(paciente, result.get(0));
        verify(pacienteOutputPort).findAll();
    }

    @Test
    void buscarTodosComPaginacao_DeveRetornarPaginaDePacientes() {
        // Arrange
        List<Paciente> pacientes = Arrays.asList(paciente);
        Page<Paciente> page = new PageImpl<>(pacientes, pageable, 1);
        when(pacienteOutputPort.findAll(pageable)).thenReturn(page);

        // Act
        Page<Paciente> result = pacienteService.buscarTodos(pageable);

        // Assert
        assertEquals(1, result.getContent().size());
        assertEquals(paciente, result.getContent().get(0));
        verify(pacienteOutputPort).findAll(pageable);
    }

    @Test
    void cadastrar_DeveRetornarPacienteSalvo() {
        // Arrange
        when(pacienteOutputPort.save(paciente)).thenReturn(paciente);

        // Act
        Paciente result = pacienteService.cadastrar(paciente);

        // Assert
        assertEquals(paciente, result);
        verify(pacienteOutputPort).save(paciente);
    }

    @Test
    void deletar_DeveLancarException_QuandoIdInvalido() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> pacienteService.deletar(null)
        );
        
        assertEquals("ID inválido", exception.getMessage());
        verify(pacienteOutputPort, never()).deleteById(any());
    }

    @Test
    void deletar_DeveLancarException_QuandoRegistroNaoExistir() {
        // Arrange
        when(pacienteOutputPort.existsById(1L)).thenReturn(false);

        // Act & Assert
        Exception exception = assertThrows(
                Exception.class,
                () -> pacienteService.deletar(1L)
        );
        
        assertEquals("Registro não encontrado", exception.getMessage());
        verify(pacienteOutputPort).existsById(1L);
        verify(pacienteOutputPort, never()).deleteById(any());
    }

    @Test
    void deletar_DeveDeletarComSucesso_QuandoRegistroExistir() throws Exception {
        // Arrange
        when(pacienteOutputPort.existsById(1L)).thenReturn(true);
        doNothing().when(pacienteOutputPort).deleteById(1L);

        // Act
        assertDoesNotThrow(() -> pacienteService.deletar(1L));

        // Assert
        verify(pacienteOutputPort).existsById(1L);
        verify(pacienteOutputPort).deleteById(1L);
    }
}
