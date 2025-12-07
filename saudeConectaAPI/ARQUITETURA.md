# Arquitetura Horizontal - SaudeConecta API

## Visão Geral

O projeto foi refatorado para utilizar uma **arquitetura horizontal** (também conhecida como Clean Architecture ou Arquitetura em Camadas), com **Use Cases** para cada operação de negócio.

## Estrutura de Pastas

```
br.com.saudeConecta/
├── domain/                              # Camada de Domínio
│   ├── medico/
│   │   └── Medico.java                  # Entidade
│   ├── paciente/
│   │   └── Paciente.java
│   ├── usuario/
│   │   └── Usuario.java
│   ├── endereco/
│   │   └── Endereco.java
│   ├── consulta/
│   └── administrador/
│
├── application/                         # Camada de Aplicação (Use Cases)
│   └── usecase/
│       ├── medico/
│       │   ├── BuscarMedicoPorIdUseCase.java           # Interface
│       │   ├── BuscarMedicoPorEmailUseCase.java
│       │   ├── BuscarMedicoPorNomeUseCase.java
│       │   ├── BuscarMedicoPorCrmUseCase.java
│       │   ├── BuscarMedicoPorCidadeUseCase.java
│       │   ├── BuscarMedicoPorEspecialidadeUseCase.java
│       │   ├── BuscarTodosMedicosUseCase.java
│       │   ├── CadastrarMedicoUseCase.java
│       │   ├── AtualizarMedicoUseCase.java
│       │   ├── DeletarMedicoUseCase.java
│       │   └── impl/                                    # Implementações
│       │       ├── BuscarMedicoPorIdUseCaseImpl.java
│       │       ├── BuscarMedicoPorEmailUseCaseImpl.java
│       │       └── ...
│       ├── paciente/
│       │   ├── BuscarPacientePorIdUseCase.java
│       │   ├── BuscarTodosPacientesUseCase.java
│       │   ├── CadastrarPacienteUseCase.java
│       │   ├── DeletarPacienteUseCase.java
│       │   └── impl/
│       │       └── ...
│       ├── consulta/
│       └── usuario/
│
├── infrastructure/                      # Camada de Infraestrutura
│   ├── persistence/
│   │   └── repository/
│   │       ├── MedicoRepository.java
│   │       ├── PacienteRepository.java
│   │       ├── UsuarioRepository.java
│   │       └── EnderecoRepository.java
│   ├── security/                        # Configurações de segurança
│   └── config/                          # Outras configurações
│
├── presentation/                        # Camada de Apresentação
│   ├── controller/
│   │   ├── MedicoController.java
│   │   ├── PacienteController.java
│   │   └── ...
│   └── dto/
│       ├── medico/
│       │   ├── MedicoResponse.java
│       │   ├── CadastrarMedicoRequest.java
│       │   ├── AtualizarDadosMedicoRequest.java
│       │   └── AtualizarEnderecoMedicoRequest.java
│       └── paciente/
│           ├── PacienteResponse.java
│           └── CadastrarPacienteRequest.java
│
└── SaudeConectaApplication.java         # Classe principal
```

## Padrão Use Case

Cada operação de negócio é representada por um **Use Case** com:

### 1. Interface (Contrato)
```java
public interface BuscarMedicoPorIdUseCase {
    Optional<Medico> executar(Long id);
}
```

### 2. Implementação
```java
@Slf4j
@Service
@RequiredArgsConstructor
public class BuscarMedicoPorIdUseCaseImpl implements BuscarMedicoPorIdUseCase {

    private final MedicoRepository medicoRepository;

    @Override
    public Optional<Medico> executar(Long id) {
        log.debug("Buscando médico por ID de usuário: {}", id);
        return medicoRepository.findByUsuario_Id(id);
    }
}
```

### 3. Uso no Controller
```java
@RestController
@RequiredArgsConstructor
public class MedicoController {

    private final BuscarMedicoPorIdUseCase buscarMedicoPorIdUseCase;

    @GetMapping("/buscarId/{id}")
    public ResponseEntity<MedicoResponse> buscarMedicoPorId(@PathVariable Long id) {
        return buscarMedicoPorIdUseCase.executar(id)
            .map(m -> ResponseEntity.ok(new MedicoResponse(m)))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
```

## Benefícios

1. **Separação de Responsabilidades**: Cada camada tem uma função específica
2. **Testabilidade**: Use Cases podem ser testados isoladamente
3. **Manutenibilidade**: Mudanças em uma camada não afetam outras
4. **Flexibilidade**: Fácil trocar implementações (ex: banco de dados)
5. **Clareza**: Cada Use Case representa uma ação de negócio clara

## Fluxo de Dados

```
Request → Controller → UseCase (Interface) → UseCaseImpl → Repository → Database
                                                    ↓
Response ← Controller ← DTO ← Entity ←─────────────┘
```

## Migração

### Estrutura Antiga (Vertical)
```
endpoinst/
├── medico/
│   ├── DTO/
│   ├── Entity/
│   ├── Repository/
│   ├── Resource/
│   └── Service/
```

### Estrutura Nova (Horizontal)
```
domain/medico/           → Entidades
application/usecase/     → Lógica de negócio
infrastructure/          → Repositórios
presentation/            → Controllers e DTOs
```

## Próximos Passos

Para completar a migração:

1. ✅ Médico - Completo
2. ✅ Paciente - Completo
3. ⏳ Consulta - Pendente
4. ⏳ Usuario - Pendente
5. ⏳ Administrador - Pendente
6. ⏳ Outros módulos - Pendente
7. ⏳ Remover estrutura antiga

## Convenções de Nomenclatura

- **Use Cases**: `[Ação][Entidade]UseCase` (ex: `BuscarMedicoPorIdUseCase`)
- **Implementações**: `[UseCase]Impl` (ex: `BuscarMedicoPorIdUseCaseImpl`)
- **DTOs Request**: `[Ação][Entidade]Request` (ex: `CadastrarMedicoRequest`)
- **DTOs Response**: `[Entidade]Response` (ex: `MedicoResponse`)
- **Controllers**: `[Entidade]Controller` (ex: `MedicoController`)
