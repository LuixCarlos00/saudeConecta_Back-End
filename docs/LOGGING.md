# Padrão de logs — SaúdeConecta API

Objetivo: olhando só o log, saber **o que** aconteceu, **com quais ids**, **onde parou** e **por quê**.

## 1. O que já vem de graça em toda linha

```
2026-11-05 14:02:11.482  INFO --- [nio-8080-exec-3] [a1b2c3d4 t=3 u=27] ConsultaAgendamentoService.cadastrarConsultaByOrg:69 : consulta.agendar inicio profissionalId=12 ...
                                                     │        │    │     │                                                     └─ mensagem (seção 2)
                                                     │        │    │     └─ Classe.metodo:linha (pattern)
                                                     │        │    └─ userId   (MDC, FiltroAcesso)
                                                     │        └─ tenantId (clínica) (MDC, TenantFilter)
                                                     └─ requestId (MDC, CorrelationIdFilter; devolvido no header X-Request-Id)
```

- Jobs agendados recebem `requestId=job-xxxxxxxx` (`ScheduledMdcAspect`).
- Toda requisição termina com uma linha `http fim <MÉTODO> <rota> status=<n> duracaoMs=<n>`
  (`CorrelationIdFilter`). A rota é o padrão do Spring (`/consultas/{id}`), nunca a URI crua.
- **Não** repita na mensagem nada disso: nem tenant, nem usuário, nem o nome da classe ou do método.

**Nome do logger depois da B6 (06/11):** a fase B6 tirou regra de controllers e do `ConsultaService`, então
algumas linhas continuam com o **mesmo evento** mas saem com **outra classe** no `Classe.metodo:linha`.
Quem filtra log pelo nome da classe precisa atualizar o filtro:

| Evento | Antes | Agora |
|---|---|---|
| `auth.login *`, `auth.senhaMaster *`, `auth.senha.recuperar ignorado` | `HomeController` | `AutenticacaoService` |
| `consulta.historico negado` | `ConsultaController` | `BuscarHistoricoPacientePorPerfilUseCase` |
| `cep.buscar viacep`, `cep.buscar negado`, `cep.buscar falha` | `CepController` | `ViaCepClient` (`cep.buscar inicio/ok` seguem no `CepController`) |
| `consulta.agendar/atualizar/status/excluir/disponibilidade/horariosOcupados/formaPagamento *`, `consulta.historico registrado` | `ConsultaService` | `ConsultaAgendamentoService` (os `negado` de status inválido e consulta não encontrada seguem no `ConsultaController`) |
| `consulta.buscar *`, `consulta.pesquisar *`, `consulta.periodo *` | `ConsultaService` | `ConsultaBuscaService` |
| `consulta.dashboard *`, `consulta.estatisticas *`, `consulta.duracoes *` | `ConsultaService` | `ConsultaEstatisticaService` |
| `consulta.estatisticas negado`, `consulta.saldoFinanceiro negado` | `ConsultaController` | `ConsultaEstatisticaController` |

**Para investigar:** pegue o `X-Request-Id` da resposta (ou do erro no front) e faça `grep <requestId>`.
A história da requisição aparece em ordem; a última linha antes de `http fim` (ou a ausência dele) é onde parou.

## 2. Molde da mensagem

```
<evento> <etapa> chave=valor chave=valor ...
```

| Parte | Regra | Exemplos |
|---|---|---|
| `evento` | `area.acao`, minúsculo, o mesmo em todas as linhas da operação | `consulta.agendar`, `assinatura.cancelar`, `mp.webhook`, `usuario.bloquear`, `cobranca.regua` |
| `etapa` | uma de: `inicio`, `ok`, `negado`, `falha`, ou um passo intermediário curto (`conflito.verificar`) | |
| dados | sempre `chave=valor`, chaves em camelCase, ids com o nome do campo | `consultaId=823 pacienteId=115 status=AGENDADA` |
| motivo | em `negado`/`falha`, `motivo=` com código curto em MAIÚSCULAS | `motivo=HORARIO_OCUPADO`, `motivo=OUTRA_CLINICA` |

Placeholders do SLF4J (`{}`) sempre; nunca concatenar string. Sem `===`, `---`, `[TAG]` ou frases soltas
("Iniciando busca...").

## 3. Quando usar cada nível

| Nível | Quando | Regras |
|---|---|---|
| **ERROR** | Falhou e **alguém precisa agir**: 500, pagamento não processado, e-mail perdido depois das tentativas, job que abortou | A exceção é o **último argumento** (`log.error("... ", a, b, e)`), sem `e.getMessage()` repetido. Logar **uma vez**, onde o erro é tratado — nunca logar e relançar. |
| **WARN** | Anormal, mas tratado: acesso negado a dado de outra clínica, webhook rejeitado, nova tentativa agendada, entrada inválida que chegou do cliente | Sempre com `motivo=` e os ids. Stack trace só se a causa for inesperada. Operação normal **não** é WARN. |
| **INFO** | Marcos de negócio: `inicio` e `ok` de operações que **alteram dados**, resumo de job (`processados=12 falhas=1 duracaoMs=830`) | Uma linha de entrada com os ids que chegaram e uma de saída com o resultado. Leituras simples (GET) não precisam de INFO: o `http fim` já cobre. Ligado em produção. |
| **DEBUG** | Passos intermediários, contagens de query, decisões internas | Desligado em produção. Pode ter mais detalhe, mas a mesma regra de dados sensíveis. |

Controllers não logam "recebi a requisição": o `http fim` e os logs do service já contam a história.

## 4. Dados sensíveis (LGPD)

Nunca em log, em nenhum nível: senha, hash, JWT, conteúdo clínico (anamnese, diagnóstico, prescrição...).
Com máscara obrigatória (`config/logging/MascaraLog`):

| Dado | Como |
|---|---|
| e-mail | `MascaraLog.email(email)` |
| CPF / login | `MascaraLog.cpf(cpf)` |
| token de link público, preapproval etc. | `MascaraLog.token(token)` |
| CEP | `MascaraLog.cep(cep)` |
| texto vindo do cliente (header, parâmetro; risco de CR/LF) | `MascaraLog.seguro(valor)` |

Prefira sempre o **id** da entidade ao dado pessoal.

## 5. Exemplos

**Sucesso**
```java
log.info("consulta.agendar inicio profissionalId={} pacienteId={} dataHora={} duracaoMin={}",
        req.profissionalId(), req.pacienteId(), req.dataHora(), req.duracaoMinutos());
// ...
log.info("consulta.agendar ok consultaId={} status={}", salva.getId(), salva.getStatus());
```

**Regra de negócio barrou**
```java
log.warn("consulta.agendar negado motivo=HORARIO_OCUPADO profissionalId={} conflitoConsultaId={}",
        profissionalId, conflito.getId());
throw new ConflitoHorarioException(...);
```

**Falha de verdade**
```java
} catch (RestClientException e) {
    log.error("mp.webhook.reprocessar falha eventoId={} tipo={} dataId={} tentativa={}",
            evento.getId(), evento.getTipo(), evento.getDataId(), tentativas, e);
}
```

**Job**
```java
log.info("cobranca.regua inicio vencidas={}", vencidas.size());
// ...
log.info("cobranca.regua ok processadas={} falhas={} duracaoMs={}", ok, falhas, duracao);
```

## 6. Anti-padrões (barrados pelo `PadraoLogTest`)

| Não | Sim |
|---|---|
| `log.error("Erro: {}", e.getMessage(), e)` | `log.error("x.y falha id={}", id, e)` |
| `log.warn("Erro ao extrair token: {}", e.getMessage())` | `log.warn("auth.token negado motivo=TOKEN_INVALIDO causa={}", e.getClass().getSimpleName())` |
| `log.info("=== Requisição: GET /x ===")` | nada (o `http fim` cobre) |
| `log.warn("Cancelando assinatura ID: {}", id)` | `log.info("assinatura.cancelar inicio assinaturaId={}", id)` |
| `log.info("... email={}", email)` | `log.info("... email={}", MascaraLog.email(email))` |
