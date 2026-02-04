//package br.com.saudeConecta.presentation.controller;
//
//import br.com.saudeConecta.application.service.AdministradorService;
//import br.com.saudeConecta.domain.administrador.Administrador;
//import br.com.saudeConecta.domain.usuario.Usuario;
//import br.com.saudeConecta.infrastructure.persistence.repository.UsuarioRepository;
//import br.com.saudeConecta.presentation.dto.administrador.CadastrarAdministradorRequest;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.http.MediaType;
//import org.springframework.test.web.servlet.MockMvc;
//
//import java.util.Optional;
//
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//@WebMvcTest(AdministradorController.class)
//class AdministradorControllerTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    @MockBean
//    private AdministradorService administradorService;
//
//    @MockBean
//    private UsuarioRepository usuarioRepository;
//
//    private Administrador administrador;
//    private Usuario usuario;
//    private CadastrarAdministradorRequest request;
//
//    @BeforeEach
//    void setUp() {
//        administrador = new Administrador();
//        administrador.setAdmCodigo(1L);
//        administrador.setAdmNome("Admin Test");
//        administrador.setAdmEmail("admin@test.com");
//        administrador.setAdmStatus((byte) 1);
//
//        usuario = new Usuario();
//        usuarioagora.setUsuCodigo(1L);
//        usuario.setLogin("admin");
//
//        request = new CadastrarAdministradorRequest(
//                "Admin Test",
//                (byte) 1,
//                null,
//                "admin@test.com",
//                null,
//                1L
//        );
//    }
//
//    @Test
//    void buscarPorId_DeveRetornarAdministrador_QuandoExistir() throws Exception {
//        // Arrange
//        when(administradorService.buscarPorId(1L)).thenReturn(Optional.of(administrador));
//
//        // Act & Assert
//        mockMvc.perform(get("/administrador/buscarId/{id}", 1L))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.admCodigo").value(1L))
//                .andExpect(jsonPath("$.admNome").value("Admin Test"))
//                .andExpect(jsonPath("$.admEmail").value("admin@test.com"));
//
//        verify(administradorService).buscarPorId(1L);
//    }
//
//    @Test
//    void buscarPorId_DeveRetornarNotFound_QuandoNaoExistir() throws Exception {
//        // Arrange
//        when(administradorService.buscarPorId(1L)).thenReturn(Optional.empty());
//
//        // Act & Assert
//        mockMvc.perform(get("/administrador/buscarId/{id}", 1L))
//                .andExpect(status().isNotFound());
//
//        verify(administradorService).buscarPorId(1L);
//    }
//
//    @Test
//    void buscarPorIdUsuario_DeveRetornarAdministrador_QuandoExistir() throws Exception {
//        // Arrange
//        when(administradorService.buscarPorIdUsuario(1L)).thenReturn(Optional.of(administrador));
//
//        // Act & Assert
//        mockMvc.perform(get("/administrador/buscarIdDeUsusario/{id}", 1L))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.admCodigo").value(1L))
//                .andExpect(jsonPath("$.admNome").value("Admin Test"));
//
//        verify(administradorService).buscarPorIdUsuario(1L);
//    }
//
//    @Test
//    void buscarPorIdUsuario_DeveRetornarNotFound_QuandoNaoExistir() throws Exception {
//        // Arrange
//        when(administradorService.buscarPorIdUsuario(1L)).thenReturn(Optional.empty());
//
//        // Act & Assert
//        mockMvc.perform(get("/administrador/buscarIdDeUsusario/{id}", 1L))
//                .andExpect(status().isNotFound());
//
//        verify(administradorService).buscarPorIdUsuario(1L);
//    }
//
//    @Test
//    void buscarPorEmail_DeveRetornarNotImplemented() throws Exception {
//        // Act & Assert
//        mockMvc.perform(get("/administrador/buscarPorEmail/{email}", "test@test.com"))
//                .andExpect(status().isNotImplemented());
//    }
//
//    @Test
//    void verificarCodigoRecuperacao_DeveRetornarNotImplemented() throws Exception {
//        // Act & Assert
//        mockMvc.perform(get("/administrador/InserirCodigo/{codigo}", "123456"))
//                .andExpect(status().isNotImplemented());
//    }
//
//    @Test
//    void cadastrarAdministrador_DeveRetornarCreated_QuandoDadosValidos() throws Exception {
//        // Arrange
//        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
//        when(administradorService.cadastrar(any(Administrador.class))).thenReturn(administrador);
//
//        // Act & Assert
//        mockMvc.perform(post("/administrador/post")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isCreated())
//                .andExpect(jsonPath("$.admCodigo").value(1L))
//                .andExpect(jsonPath("$.admNome").value("Admin Test"))
//                .andExpect(jsonPath("$.admEmail").value("admin@test.com"));
//
//        verify(usuarioRepository).findById(1L);
//        verify(administradorService).cadastrar(any(Administrador.class));
//    }
//
//    @Test
//    void cadastrarAdministrador_DeveRetornarNotFound_QuandoUsuarioNaoExistir() throws Exception {
//        // Arrange
//        when(usuarioRepository.findById(1L)).thenReturn(Optional.empty());
//
//        // Act & Assert
//        mockMvc.perform(post("/administrador/post")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isNotFound());
//
//        verify(usuarioRepository).findById(1L);
//        verify(administradorService, never()).cadastrar(any());
//    }
//
//    @Test
//    void buscarPorPaginas_DeveRetornarPaginaDeAdministradores() throws Exception {
//        // Act & Assert
//        mockMvc.perform(get("/administrador/pacientepagina")
//                        .param("page", "0")
//                        .param("size", "12"))
//                .andExpect(status().isOk());
//    }
//
//    @Test
//    void buscarTodos_DeveRetornarListaDeAdministradores() throws Exception {
//        // Act & Assert
//        mockMvc.perform(get("/administrador/listatodospaciente"))
//                .andExpect(status().isOk());
//    }
//
//    @Test
//    void deleteAdministradorById_DeveRetornarNoContent_QuandoSucesso() throws Exception {
//        // Arrange
//        doNothing().when(administradorService).deletar(1L);
//
//        // Act & Assert
//        mockMvc.perform(delete("/administrador/{id}", 1L))
//                .andExpect(status().isNoContent());
//
//        verify(administradorService).deletar(1L);
//    }
//
//    @Test
//    void deleteAdministradorById_DeveRetornarInternalServerError_QuandoErro() throws Exception {
//        // Arrange
//        doThrow(new RuntimeException("Erro")).when(administradorService).deletar(1L);
//
//        // Act & Assert
//        mockMvc.perform(delete("/administrador/{id}", 1L))
//                .andExpect(status().isInternalServerError());
//
//        verify(administradorService).deletar(1L);
//    }
//}
