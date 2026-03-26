# Plano de Melhoria de Performance — SaudeConecta

**Data:** Março/2026  
**Escopo:** Backend (Spring Boot) + Frontend (Angular)  
**Objetivo:** Eliminar gargalos identificados nos principais services e controllers do sistema

---

## Sumário Executivo

A análise identificou **7 gargalos** distribuídos entre `ConsultaService`, `PacienteService` e camadas relacionadas. Os problemas variam de queries sem paginação que retornam volumes ilimitados de dados, a múltiplas queries sequenciais desnecessárias no caminho crítico de agendamento.

Os itens já implementados de cache (`especialidades`, `planos`, `procedimentos-padrao`, `profissionais-org`) permanecem válidos e não são alterados por este plano.

---

## Diagnóstico Completo de Gargalos

### 🔴 Crítico

| ID | Local | Problema | Risco |
|----|-------|----------|-------|
| G1 | `ConsultaService.buscarTodas()` | Retorna **todas as consultas** da org em `List<Consulta>` com JOINs, sem paginação | Timeout / OOM com volume alto |
| G2 | `ConsultaService.buscarComFiltrosDinamicos()` | `findAll(Specification)` sem `Pageable` — volume ilimitado | Timeout / memória |
| G3 | `ConsultaService.cadastrarConsultaByOrg()` | 6 queries sequenciais ao banco antes de fazer o INSERT | Latência desnecessária |

### 🟡 Moderado

| ID | Local | Problema | Risco |
|----|-------|----------|-------|
| G4 | `PacienteService` — buscas por nome/cpf/rg/telefone | 4 métodos retornam `List<Paciente>` completa sem paginação | Lentidão com muitos pacientes |
| G5 | `PacienteService.atualizarPacientebyOrg()` | Reconstrói entidade com `builder` — perde dirty checking do Hibernate, emite UPDATE com todos os campos | UPDATE desnecessariamente pesado |
| G6 | `ConsultaService.getUsuarioAtual()` | Busca o `Usuario` no banco **em cada operação de escrita** (cadastrar, cancelar, concluir, atualizar) | Query extra em todo write |

### 🟢 Baixo

| ID | Local | Problema | Risco |
|----|-------|----------|-------|
| G7 | `ConsultaService.registrarHistorico()` | Executado **síncronamente** na mesma transação das operações de escrita | Adiciona latência ao caminho crítico |

---

## Plano de Atividades

---

### ATIVIDADE 1 — Paginação em `buscarTodas()` de Consultas

**Gargalo:** G1  
**Impacto:** Alto  
**Esforço:** Baixo

#### Backend

**Arquivo:** `ConsultaService.java`

Remover o método `buscarTodas()` que retorna `List<Consulta>` (sem paginação) e garantir que o método paginado seja o único caminho utilizado.

```java
// REMOVER — retorna tudo sem limite
public List<Consulta> buscarTodas() {
    Long orgId = tenantHelper.getCurrentTenantId();
    return consultaRepository.findByOrganizacao_IdWithRelations(orgId);
}

// MANTER e padronizar uso — já existe, com Pageable
public Page<Consulta> buscarTodas(Pageable pageable) {
    Long orgId = tenantHelper.getCurrentTenantId();
    return consultaRepository.findByOrganizacao_Id(orgId, pageable);
}
```

**Arquivo:** `ConsultaController.java`

Verificar e corrigir todos os endpoints que chamam `buscarTodas()` sem pageable para usar a versão paginada, passando `Pageable` via `@PageableDefault`.

```java
// ANTES
@GetMapping
public ResponseEntity<List<Consulta>> listar() {
    return ResponseEntity.ok(consultaService.buscarTodas());
}

// DEPOIS
@GetMapping
public ResponseEntity<Page<Consulta>> listar(
        @PageableDefault(size = 20, sort = "dataHora", direction = Sort.Direction.DESC) Pageable pageable) {
    return ResponseEntity.ok(consultaService.buscarTodas(pageable));
}
```

#### Frontend

**Arquivos afetados:** Componentes que listam consultas (agenda, listagem geral)

Adaptar as chamadas para consumir a estrutura paginada `Page<T>` do Spring:

```typescript
// Interface para resposta paginada
interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;     // página atual (0-based)
  size: number;
}

// Serviço — adicionar parâmetros de paginação
listarConsultas(page: number = 0, size: number = 20): Observable<PageResponse<Consulta>> {
  return this.http.get<PageResponse<Consulta>>(
    `${this.apiUrl}/consultas?page=${page}&size=${size}&sort=dataHora,desc`
  );
}
```

Nos componentes, implementar controle de página (botões Anterior/Próximo ou paginador do Angular Material):

```typescript
// Componente
paginaAtual = 0;
totalPaginas = 0;

carregarConsultas(): void {
  this.consultaApi.listarConsultas(this.paginaAtual).subscribe(resp => {
    this.consultas = resp.content;
    this.totalPaginas = resp.totalPages;
  });
}

proximaPagina(): void {
  if (this.paginaAtual < this.totalPaginas - 1) {
    this.paginaAtual++;
    this.carregarConsultas();
  }
}
```

---

### ATIVIDADE 2 — Paginação em `buscarComFiltrosDinamicos()` (Specification)

**Gargalo:** G2  
**Impacto:** Alto  
**Esforço:** Baixo

#### Backend

**Arquivo:** `ConsultaService.java`

```java
// ANTES — sem limite de resultado
public List<Consulta> buscarComFiltrosDinamicos(...) {
    return consultaRepository.findAll(
        ConsultaSpecification.buscarComFiltros(orgId, profissionalId, especialidade, dataInicio, dataFim, statusList)
    );
}

// DEPOIS — com paginação obrigatória
public Page<Consulta> buscarComFiltrosDinamicos(
        Long profissionalId,
        String especialidade,
        LocalDateTime dataInicio,
        LocalDateTime dataFim,
        List<StatusConsulta> statusList,
        Pageable pageable) {
    Long orgId = tenantHelper.getCurrentTenantId();
    return consultaRepository.findAll(
        ConsultaSpecification.buscarComFiltros(orgId, profissionalId, especialidade, dataInicio, dataFim, statusList),
        pageable
    );
}
```

**Arquivo:** `ConsultaController.java`

```java
// DEPOIS
@GetMapping("/buscar")
public ResponseEntity<Page<Consulta>> buscar(
        @RequestParam(required = false) Long profissionalId,
        @RequestParam(required = false) String especialidade,
        @RequestParam(required = false) LocalDateTime dataInicio,
        @RequestParam(required = false) LocalDateTime dataFim,
        @RequestParam(required = false) List<StatusConsulta> status,
        @PageableDefault(size = 20, sort = "dataHora", direction = Sort.Direction.DESC) Pageable pageable) {
    return ResponseEntity.ok(
        consultaService.buscarComFiltrosDinamicos(profissionalId, especialidade, dataInicio, dataFim, status, pageable)
    );
}
```

#### Frontend

Mesmo padrão da Atividade 1: adaptar chamada para consumir `PageResponse<Consulta>` e implementar paginação no componente de busca/filtro.

---

### ATIVIDADE 3 — Reduzir queries no cadastro de consulta

**Gargalo:** G3  
**Impacto:** Médio  
**Esforço:** Baixo

#### Backend

**Arquivo:** `ConsultaService.java` — método `cadastrarConsultaByOrg()`

Substituir buscas de `Organizacao` e `Usuario` (que só servem para criar referência de FK) por `EntityManager.getReference()`, eliminando 2 SELECT desnecessários:

```java
// ANTES — faz SELECT para montar referência de FK
Organizacao organizacao = organizacaoRepository.findById(orgId)
    .orElseThrow(() -> new IllegalStateException("Organização não encontrada"));

Usuario criadoPor = null;
Long userId = TenantContext.getCurrentUser();
if (userId != null) {
    criadoPor = usuarioRepository.findById(userId).orElse(null);
}

// DEPOIS — proxy JPA, sem SELECT ao banco
Organizacao organizacao = entityManager.getReference(Organizacao.class, orgId);

Usuario criadoPor = null;
Long userId = TenantContext.getCurrentUser();
if (userId != null) {
    criadoPor = entityManager.getReference(Usuario.class, userId);
}
```

Injetar `EntityManager` no service:

```java
private final EntityManager entityManager;
```

**Resultado:** De 6 queries sequenciais, passa para 4 (mantém as buscas de `Profissional`, `Paciente`, `Especialidade` e `FormaPagamento` que são realmente necessárias para validação/dados).

---

### ATIVIDADE 4 — Paginação nas buscas de Pacientes

**Gargalo:** G4  
**Impacto:** Médio  
**Esforço:** Médio

#### Backend

**Arquivo:** `PacienteService.java`

Adicionar suporte a `Pageable` nos 4 métodos de busca e em `buscarTodosPacientesComFiltro`:

```java
// ANTES
public List<Paciente> buscarListaPacientesPorNomeComFiltro(String pesquisa, String filtro) { ... }

// DEPOIS
public Page<Paciente> buscarListaPacientesPorNomeComFiltro(String pesquisa, String filtro, Pageable pageable) {
    Long orgId = tenantHelper.getCurrentTenantId();
    if ("ATIVO".equals(filtro)) {
        return pacienteRepository.findByOrganizacaoIdAndNomeContainingWithFiltro(orgId, pesquisa, filtro, "ATIVO", pageable);
    }
    return pacienteRepository.findByOrganizacaoIdAndNomeContaining(orgId, pesquisa, pageable);
}
```

**Arquivo:** `PacienteRepository.java`

Adicionar versões dos métodos que aceitam `Pageable`:

```java
Page<Paciente> findByOrganizacaoIdAndNomeContaining(Long orgId, String nome, Pageable pageable);

@Query("SELECT p FROM Paciente p WHERE p.organizacao.id = :orgId AND LOWER(p.paciNome) LIKE LOWER(CONCAT('%', :nome, '%')) AND p.paciStatus = :status")
Page<Paciente> findByOrganizacaoIdAndNomeContainingWithFiltro(
    @Param("orgId") Long orgId,
    @Param("nome") String nome,
    @Param("filtro") String filtro,
    @Param("status") String status,
    Pageable pageable);
```

**Arquivo:** `PacienteApiController.java`

```java
@GetMapping("/buscar-por-nome")
public ResponseEntity<Page<Paciente>> buscarPorNome(
        @RequestParam String pesquisa,
        @RequestParam(defaultValue = "ALL") String filtro,
        @PageableDefault(size = 20, sort = "paciNome") Pageable pageable) {
    return ResponseEntity.ok(pacienteService.buscarListaPacientesPorNomeComFiltro(pesquisa, filtro, pageable));
}
```

#### Frontend

**Arquivos afetados:** Componentes de busca de pacientes

```typescript
buscarPacientesPorNome(nome: string, filtro: string = 'ALL', page: number = 0): Observable<PageResponse<Paciente>> {
  return this.http.get<PageResponse<Paciente>>(
    `${this.apiUrl}/pacientes/buscar-por-nome?pesquisa=${nome}&filtro=${filtro}&page=${page}&size=20`
  );
}
```

Implementar feedback visual de paginação e total de resultados nos componentes de listagem de pacientes.

---

### ATIVIDADE 5 — Corrigir `atualizarPacientebyOrg()` para usar dirty checking

**Gargalo:** G5  
**Impacto:** Médio  
**Esforço:** Baixo

#### Backend

**Arquivo:** `PacienteService.java`

O problema: ao usar `Paciente.builder()` para reconstruir a entidade e depois chamar `save()`, o Hibernate não sabe quais campos mudaram e emite `UPDATE` com **todos os campos**.

A correção é modificar diretamente o objeto gerenciado (managed entity) e deixar o dirty checking do Hibernate detectar apenas os campos alterados:

```java
// ANTES — reconstrói objeto inteiro
@Transactional
public Paciente atualizarPacientebyOrg(Long id, AtualizarPacienteRequest dados) {
    Paciente antes = buscarrPacientebyOrg(id).orElseThrow(...);
    Paciente snapshot = SnapshotUtil.copiarSnapshot(antes);

    // Reconstrói com builder — perde dirty checking
    Paciente pacienteAtualizado = Paciente.builder()
        .paciCodigo(antes.getPaciCodigo())
        ...
        .build();
    return pacienteRepository.save(pacienteAtualizado);
}

// DEPOIS — modifica o managed entity
@Transactional
public Paciente atualizarPacientebyOrg(Long id, AtualizarPacienteRequest dados) {
    Long orgId = tenantHelper.getCurrentTenantId();
    // findById dentro de @Transactional retorna managed entity
    Paciente paciente = pacienteRepository.findByPaciCodigoAndOrganizacao_IdWithEndereco(id, orgId)
        .orElseThrow(() -> new IllegalArgumentException("Paciente não encontrado"));

    Paciente snapshot = SnapshotUtil.copiarSnapshot(paciente);

    // Atualiza apenas campos fornecidos — dirty checking detecta somente eles
    if (dados.nome() != null)          paciente.setPaciNome(dados.nome());
    if (dados.sexo() != null)          paciente.setPaciSexo(dados.sexo());
    if (dados.dataNascimento() != null) paciente.setPaciDataNacimento(Date.valueOf(dados.dataNascimento()));
    if (dados.cpf() != null)           paciente.setPaciCpf(dados.cpf());
    if (dados.rg() != null)            paciente.setPaciRg(dados.rg());
    if (dados.email() != null)         paciente.setPaciEmail(dados.email());
    if (dados.telefone() != null)      paciente.setPaciTelefone(dados.telefone());

    // Atualiza endereço se existir
    if (dados.uf() != null && paciente.getEndereco() != null) {
        Endereco end = paciente.getEndereco();
        if (dados.uf() != null)          end.setEndUF(dados.uf());
        if (dados.municipio() != null)   end.setEndMunicipio(dados.municipio());
        if (dados.bairro() != null)      end.setEndBairro(dados.bairro());
        if (dados.cep() != null)         end.setEndCep(dados.cep());
        if (dados.rua() != null)         end.setEndRua(dados.rua());
        if (dados.numero() != null)      end.setEndNumero(dados.numero().longValue());
        if (dados.complemento() != null) end.setEndComplemento(dados.complemento());
        if (dados.nacionalidade() != null) end.setEndNacionalidade(dados.nacionalidade());
    }

    // Flush automático ao fim da @Transactional — sem save() explícito necessário
    // mas mantemos para clareza
    Paciente resultado = pacienteRepository.save(paciente);

    historicoDadosPessoaisService.registrarAlteracoesDeObjeto(
        EntidadeTipo.PACIENTE, resultado.getPaciCodigo(),
        tenantHelper.getCurrentUserId(), snapshot, resultado
    );

    return resultado;
}
```

**Resultado:** Hibernate emite `UPDATE` apenas com os campos que de fato mudaram.

---

### ATIVIDADE 6 — Eliminar query de `getUsuarioAtual()` nas escritas

**Gargalo:** G6  
**Impacto:** Médio  
**Esforço:** Baixo

#### Backend

**Arquivo:** `ConsultaService.java` — método `getUsuarioAtual()`

```java
// ANTES — SELECT ao banco em cada operação de escrita
private Usuario getUsuarioAtual() {
    Long userId = TenantContext.getCurrentUser();
    if (userId != null) {
        return usuarioRepository.findById(userId).orElse(null);
    }
    return null;
}

// DEPOIS — proxy JPA, sem SELECT
private Usuario getUsuarioAtual() {
    Long userId = TenantContext.getCurrentUser();
    if (userId != null) {
        return entityManager.getReference(Usuario.class, userId);
    }
    return null;
}
```

> **Pré-requisito:** Atividade 3 já injeta `EntityManager` no service.

---

### ATIVIDADE 7 — Mover `registrarHistorico()` para execução assíncrona

**Gargalo:** G7  
**Impacto:** Baixo  
**Esforço:** Baixo

#### Backend

**Arquivo:** `ConsultaService.java`

```java
// ANTES — síncrono, bloqueia a transação principal
private void registrarHistorico(Consulta consulta, StatusConsulta statusAnterior,
                                 StatusConsulta statusNovo, String observacao, Usuario alteradoPor) {
    ConsultaHistorico historico = ConsultaHistorico.builder()...build();
    historicoRepository.save(historico);
}

// DEPOIS — extrair para um service separado com @Async
```

Criar `ConsultaHistoricoService.java`:

```java
@Service
@RequiredArgsConstructor
public class ConsultaHistoricoService {

    private final ConsultaHistoricoRepository historicoRepository;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(Consulta consulta, StatusConsulta statusAnterior,
                          StatusConsulta statusNovo, String observacao, Long usuarioId) {
        ConsultaHistorico historico = ConsultaHistorico.builder()
            .consulta(consulta)
            .statusAnterior(statusAnterior)
            .statusNovo(statusNovo)
            .observacao(observacao)
            .build();
        historicoRepository.save(historico);
    }
}
```

Habilitar `@Async` na aplicação (se ainda não estiver):

```java
@SpringBootApplication
@EnableCaching
@EnableAsync   // adicionar
public class SaudeConectaApplication { ... }
```

---

## Resumo Executivo de Atividades

| # | Atividade | Gargalo | Impacto | Esforço | Backend | Frontend |
|---|-----------|---------|---------|---------|---------|---------|
| 1 | Paginação em `buscarTodas()` | G1 | 🔴 Alto | Baixo | ✅ | ✅ |
| 2 | Paginação em `buscarComFiltrosDinamicos()` | G2 | 🔴 Alto | Baixo | ✅ | ✅ |
| 3 | Reduzir queries no cadastro de consulta | G3 | 🔴 Alto | Baixo | ✅ | ❌ |
| 4 | Paginação nas buscas de pacientes | G4 | 🟡 Médio | Médio | ✅ | ✅ |
| 5 | Corrigir update de paciente (dirty checking) | G5 | 🟡 Médio | Baixo | ✅ | ❌ |
| 6 | Eliminar query de `getUsuarioAtual()` | G6 | 🟡 Médio | Baixo | ✅ | ❌ |
| 7 | `registrarHistorico()` assíncrono | G7 | 🟢 Baixo | Baixo | ✅ | ❌ |

**Legenda:** ✅ Requer mudança | ❌ Sem impacto nesta camada

---

## Ordem de Implementação Sugerida

```
Semana 1 (Backend puro — sem quebra de contrato):
  → Atividade 3: EntityManager no cadastro de consulta
  → Atividade 5: Dirty checking no update de paciente
  → Atividade 6: getUsuarioAtual() via proxy
  → Atividade 7: Histórico assíncrono

Semana 2 (Backend + Frontend — requer coordenação):
  → Atividade 1: Paginação em buscarTodas()
  → Atividade 2: Paginação em buscarComFiltrosDinamicos()
  → Atividade 4: Paginação nas buscas de pacientes
```

> **Observação:** Atividades 3, 5, 6 e 7 não alteram a assinatura dos endpoints e podem ser feitas sem nenhuma mudança no frontend, tornando-as candidatas ideais para iniciar.

---

## O que já está implementado (não alterar)

Os seguintes caches já foram aplicados e estão corretos:

| Cache | Service | TTL |
|-------|---------|-----|
| `especialidades` | `EspecialidadeService` | 5 min |
| `planos` | `PlanoAssinaturaService` | 5 min |
| `procedimentos-padrao` | `ProcedimentoPadraoService` | 5 min |
| `profissionais-org` | `ProfissionalService` | 5 min |

Os services `ProntuarioService`, `ProntuarioDentistaService`, `SaldoFinanceiroService`, `SecretariaService`, `TermoAutorizacaoService`, `HomeService` e `HistoricoDadosPessoaisService` **não necessitam de cache** — seus dados são dinâmicos ou o volume de acesso não justifica.
