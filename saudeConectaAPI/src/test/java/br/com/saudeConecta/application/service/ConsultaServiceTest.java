package br.com.saudeConecta.application.service;

import br.com.saudeConecta.application.port.out.consulta.ConsultaOutputPort;
import br.com.saudeConecta.domain.consulta.Consulta;
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

import java.sql.Date;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsultaServiceTest {

    @Mock
    private ConsultaOutputPort consultaOutputPort;

    @InjectMocks
    private ConsultaService consultaService;

    private Consulta consulta;
    private Pageable pageable;
    private Date data;

    @BeforeEach
    void setUp() {
        consulta = new Consulta();
        consulta.setConCodigoConsulta(1L);
        consulta.setConHorario("10:00");
        consulta.setConData("2024-01-01");
        consulta.setConStatus("AGENDADA");
        
        pageable = PageRequest.of(0, 10);
        data = Date.valueOf("2024-01-01");
    }

    @Test
    void buscarPorId_DeveRetornarConsulta_QuandoExistir() {
        // Arrange
        when(consultaOutputPort.findById(1L)).thenReturn(Optional.of(consulta));

        // Act
        Optional<Consulta> result = consultaService.buscarPorId(1L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(consulta, result.get());
        verify(consultaOutputPort).findById(1L);
    }

    @Test
    void buscarPorHorarioDataEMedico_DeveRetornarConsulta_QuandoExistir() {
        // Arrange
        when(consultaOutputPort.findByConHorarioAndConDataAndConMedico_MedCodigo("10:00", data, 1L))
                .thenReturn(Optional.of(consulta));

        // Act
        Optional<Consulta> result = consultaService.buscarPorHorarioDataEMedico("10:00", data, 1L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(consulta, result.get());
        verify(consultaOutputPort).findByConHorarioAndConDataAndConMedico_MedCodigo("10:00", data, 1L);
    }

    @Test
    void existePorHorarioDataEMedico_DeveRetornarTrue_QuandoExistir() {
        // Arrange
        when(consultaOutputPort.existsByConHorarioAndConDataAndConMedico_MedCodigo("10:00", "2024-01-01", 1L))
                .thenReturn(true);

        // Act
        boolean result = consultaService.existePorHorarioDataEMedico("10:00", "2024-01-01", 1L);

        // Assert
        assertTrue(result);
        verify(consultaOutputPort).existsByConHorarioAndConDataAndConMedico_MedCodigo("10:00", "2024-01-01", 1L);
    }

    @Test
    void buscarPorMedico_DeveRetornarListaDeConsultas() {
        // Arrange
        List<Consulta> consultas = Arrays.asList(consulta);
        when(consultaOutputPort.findByConMedico_MedCodigo(1L)).thenReturn(consultas);

        // Act
        List<Consulta> result = consultaService.buscarPorMedico(1L);

        // Assert
        assertEquals(1, result.size());
        assertEquals(consulta, result.get(0));
        verify(consultaOutputPort).findByConMedico_MedCodigo(1L);
    }

    @Test
    void buscarPorMedicoEData_DeveRetornarListaDeConsultas() {
        // Arrange
        List<Consulta> consultas = Arrays.asList(consulta);
        when(consultaOutputPort.findByConMedico_MedCodigoAndConData(1L, "2024-01-01")).thenReturn(consultas);

        // Act
        List<Consulta> result = consultaService.buscarPorMedicoEData(1L, "2024-01-01");

        // Assert
        assertEquals(1, result.size());
        assertEquals(consulta, result.get(0));
        verify(consultaOutputPort).findByConMedico_MedCodigoAndConData(1L, "2024-01-01");
    }

    @Test
    void buscarEmIntervaloDatas_DeveRetornarListaDeConsultas() {
        // Arrange
        List<Consulta> consultas = Arrays.asList(consulta);
        when(consultaOutputPort.buscarConsultasEmIntervaloDeDatas("2024-01-01", "2024-01-31")).thenReturn(consultas);

        // Act
        List<Consulta> result = consultaService.buscarEmIntervaloDatas("2024-01-01", "2024-01-31");

        // Assert
        assertEquals(1, result.size());
        assertEquals(consulta, result.get(0));
        verify(consultaOutputPort).buscarConsultasEmIntervaloDeDatas("2024-01-01", "2024-01-31");
    }

    @Test
    void buscarPorEspecialidade_DeveRetornarListaDeConsultas() {
        // Arrange
        List<Consulta> consultas = Arrays.asList(consulta);
        when(consultaOutputPort.buscarConsultasPorEspecialidade("Cardiologia")).thenReturn(consultas);

        // Act
        List<Consulta> result = consultaService.buscarPorEspecialidade("Cardiologia");

        // Assert
        assertEquals(1, result.size());
        assertEquals(consulta, result.get(0));
        verify(consultaOutputPort).buscarConsultasPorEspecialidade("Cardiologia");
    }

    @Test
    void buscarPorPaciente_DeveRetornarListaDeConsultas() {
        // Arrange
        List<Consulta> consultas = Arrays.asList(consulta);
        when(consultaOutputPort.findByConPaciente_PaciCodigo(1L)).thenReturn(consultas);

        // Act
        List<Consulta> result = consultaService.buscarPorPaciente(1L);

        // Assert
        assertEquals(1, result.size());
        assertEquals(consulta, result.get(0));
        verify(consultaOutputPort).findByConPaciente_PaciCodigo(1L);
    }

    @Test
    void contarPorData_DeveRetornarContagem() {
        // Arrange
        when(consultaOutputPort.contarConsultasPorData("2024-01-01")).thenReturn(5L);

        // Act
        Long result = consultaService.contarPorData("2024-01-01");

        // Assert
        assertEquals(5L, result);
        verify(consultaOutputPort).contarConsultasPorData("2024-01-01");
    }

    @Test
    void contarRealizadasPorData_DeveRetornarContagem() {
        // Arrange
        when(consultaOutputPort.contarConsultasRealizadasPorData("2024-01-01")).thenReturn(3L);

        // Act
        Long result = consultaService.contarRealizadasPorData("2024-01-01");

        // Assert
        assertEquals(3L, result);
        verify(consultaOutputPort).contarConsultasRealizadasPorData("2024-01-01");
    }

    @Test
    void contarAgendadasPorData_DeveRetornarContagem() {
        // Arrange
        when(consultaOutputPort.contarConsultasAgendadasPorData("2024-01-01")).thenReturn(2L);

        // Act
        Long result = consultaService.contarAgendadasPorData("2024-01-01");

        // Assert
        assertEquals(2L, result);
        verify(consultaOutputPort).contarConsultasAgendadasPorData("2024-01-01");
    }

    @Test
    void buscarTodos_DeveRetornarListaDeConsultas() {
        // Arrange
        List<Consulta> consultas = Arrays.asList(consulta);
        when(consultaOutputPort.findAll()).thenReturn(consultas);

        // Act
        List<Consulta> result = consultaService.buscarTodos();

        // Assert
        assertEquals(1, result.size());
        assertEquals(consulta, result.get(0));
        verify(consultaOutputPort).findAll();
    }

    @Test
    void buscarTodosComPaginacao_DeveRetornarPaginaDeConsultas() {
        // Arrange
        List<Consulta> consultas = Arrays.asList(consulta);
        Page<Consulta> page = new PageImpl<>(consultas, pageable, 1);
        when(consultaOutputPort.findAll(pageable)).thenReturn(page);

        // Act
        Page<Consulta> result = consultaService.buscarTodos(pageable);

        // Assert
        assertEquals(1, result.getContent().size());
        assertEquals(consulta, result.getContent().get(0));
        verify(consultaOutputPort).findAll(pageable);
    }

    @Test
    void cadastrar_DeveRetornarConsultaSalva() {
        // Arrange
        when(consultaOutputPort.save(consulta)).thenReturn(consulta);

        // Act
        Consulta result = consultaService.cadastrar(consulta);

        // Assert
        assertEquals(consulta, result);
        verify(consultaOutputPort).save(consulta);
    }

    @Test
    void deletar_DeveLancarException_QuandoIdInvalido() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> consultaService.deletar(null)
        );
        
        assertEquals("ID inválido", exception.getMessage());
        verify(consultaOutputPort, never()).deleteById(any());
    }

    @Test
    void deletar_DeveLancarException_QuandoRegistroNaoExistir() {
        // Arrange
        when(consultaOutputPort.existsById(1L)).thenReturn(false);

        // Act & Assert
        Exception exception = assertThrows(
                Exception.class,
                () -> consultaService.deletar(1L)
        );
        
        assertEquals("Registro não encontrado", exception.getMessage());
        verify(consultaOutputPort).existsById(1L);
        verify(consultaOutputPort, never()).deleteById(any());
    }

    @Test
    void deletar_DeveDeletarComSucesso_QuandoRegistroExistir() throws Exception {
        // Arrange
        when(consultaOutputPort.existsById(1L)).thenReturn(true);
        doNothing().when(consultaOutputPort).deleteById(1L);

        // Act
        assertDoesNotThrow(() -> consultaService.deletar(1L));

        // Assert
        verify(consultaOutputPort).existsById(1L);
        verify(consultaOutputPort).deleteById(1L);
    }
}
