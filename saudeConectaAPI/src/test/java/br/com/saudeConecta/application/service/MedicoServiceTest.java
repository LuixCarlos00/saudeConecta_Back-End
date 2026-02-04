package br.com.saudeConecta.application.service;

import br.com.saudeConecta.application.port.out.medico.MedicoOutputPort;
import br.com.saudeConecta.domain.medico.Medico;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MedicoServiceTest {

    @Mock
    private MedicoOutputPort medicoOutputPort;

    @InjectMocks
    private MedicoService medicoService;

    private Medico medico;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        medico = new Medico();
        medico.setMedCodigo(1L);
        medico.setMedNome("Dr. Test");
        medico.setMedEmail("medico@test.com");
        medico.setMedCrm("CRM12345");
        
        pageable = PageRequest.of(0, 10);
    }

    @Test
    void buscarPorId_DeveRetornarMedico_QuandoExistir() {
        // Arrange
        when(medicoOutputPort.findById(1L)).thenReturn(Optional.of(medico));

        // Act
        Optional<Medico> result = medicoService.buscarPorId(1L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(medico, result.get());
        verify(medicoOutputPort).findById(1L);
    }

    @Test
    void buscarPorIdUsuario_DeveRetornarMedico_QuandoExistir() {
        // Arrange
        when(medicoOutputPort.buscarMedicoPorIdUsuario(1L)).thenReturn(Optional.of(medico));

        // Act
        Optional<Medico> result = medicoService.buscarPorIdUsuario(1L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(medico, result.get());
        verify(medicoOutputPort).buscarMedicoPorIdUsuario(1L);
    }

    @Test
    void buscarPorEmail_DeveRetornarMedico_QuandoExistir() {
        // Arrange
        when(medicoOutputPort.findByMedEmail("medico@test.com")).thenReturn(Optional.of(medico));

        // Act
        Optional<Medico> result = medicoService.buscarPorEmail("medico@test.com");

        // Assert
        assertTrue(result.isPresent());
        assertEquals(medico, result.get());
        verify(medicoOutputPort).findByMedEmail("medico@test.com");
    }

    @Test
    void buscarPorCrm_DeveRetornarListaDeMedicos() {
        // Arrange
        List<Medico> medicos = Arrays.asList(medico);
        when(medicoOutputPort.findByMedCrmContainingIgnoreCase("CRM12345")).thenReturn(medicos);

        // Act
        List<Medico> result = medicoService.buscarPorCrm("CRM12345");

        // Assert
        assertEquals(1, result.size());
        assertEquals(medico, result.get(0));
        verify(medicoOutputPort).findByMedCrmContainingIgnoreCase("CRM12345");
    }

    @Test
    void buscarPorNome_DeveRetornarListaDeMedicos() {
        // Arrange
        List<Medico> medicos = Arrays.asList(medico);
        when(medicoOutputPort.findByMedNomeContainingIgnoreCase("Test")).thenReturn(medicos);

        // Act
        List<Medico> result = medicoService.buscarPorNome("Test");

        // Assert
        assertEquals(1, result.size());
        assertEquals(medico, result.get(0));
        verify(medicoOutputPort).findByMedNomeContainingIgnoreCase("Test");
    }

    @Test
    void buscarPorEspecialidade_DeveRetornarListaDeMedicos() {
        // Arrange
        List<Medico> medicos = Arrays.asList(medico);
        when(medicoOutputPort.findByMedEspecialidadeContainingIgnoreCase("Cardiologia")).thenReturn(medicos);

        // Act
        List<Medico> result = medicoService.buscarPorEspecialidade("Cardiologia");

        // Assert
        assertEquals(1, result.size());
        assertEquals(medico, result.get(0));
        verify(medicoOutputPort).findByMedEspecialidadeContainingIgnoreCase("Cardiologia");
    }

    @Test
    void contarMedicosAtivos_DeveRetornarContagem() {
        // Arrange
        when(medicoOutputPort.contarMedicosAtivos()).thenReturn(10L);

        // Act
        Long result = medicoService.contarMedicosAtivos();

        // Assert
        assertEquals(10L, result);
        verify(medicoOutputPort).contarMedicosAtivos();
    }

    @Test
    void cadastrar_DeveRetornarMedicoSalvo() {
        // Arrange
        when(medicoOutputPort.save(medico)).thenReturn(medico);

        // Act
        Medico result = medicoService.cadastrar(medico);

        // Assert
        assertEquals(medico, result);
        verify(medicoOutputPort).save(medico);
    }

    @Test
    void deletar_DeveLancarException_QuandoIdInvalido() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> medicoService.deletar(null)
        );
        
        assertEquals("ID inválido", exception.getMessage());
        verify(medicoOutputPort, never()).deleteById(any());
    }

    @Test
    void deletar_DeveLancarException_QuandoRegistroNaoExistir() {
        // Arrange
        when(medicoOutputPort.existsById(1L)).thenReturn(false);

        // Act & Assert
        Exception exception = assertThrows(
                Exception.class,
                () -> medicoService.deletar(1L)
        );
        
        assertEquals("Registro não encontrado", exception.getMessage());
        verify(medicoOutputPort).existsById(1L);
        verify(medicoOutputPort, never()).deleteById(any());
    }

    @Test
    void deletar_DeveDeletarComSucesso_QuandoRegistroExistir() throws Exception {
        // Arrange
        when(medicoOutputPort.existsById(1L)).thenReturn(true);
        doNothing().when(medicoOutputPort).deleteById(1L);

        // Act
        assertDoesNotThrow(() -> medicoService.deletar(1L));

        // Assert
        verify(medicoOutputPort).existsById(1L);
        verify(medicoOutputPort).deleteById(1L);
    }
}
