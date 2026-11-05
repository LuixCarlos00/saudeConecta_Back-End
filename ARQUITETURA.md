# Arquitetura do saudeConecta API

Spring Boot 3.0.5, Java 17, MySQL, Liquibase. É uma API em **camadas clássicas**
(controller, service, repository JPA). Não é hexagonal: não existem ports/adapters.

```
HTTP -> Controller (presentation) -> Service -> Repository JPA (infrastructure) -> MySQL
              |                          |
            DTOs                    entidades (domain)
```

## Pacotes

```
br.com.saudeConecta/
├── presentation/   controller/ (REST) e dto/ (request/response)
├── service/        regras de negócio; mercadopago/ tem a integração com o MP
├── domain/         entidades JPA e enums, um pacote por assunto
├── infrastructure/ persistence/repository: interfaces Spring Data JPA
├── infra/          transversais (ver convenção abaixo)
├── usecase/        casos de uso que agregam histórico do paciente
├── email/          envio de e-mail, retry com backoff e registro na mensageria
├── config/         CacheConfig, FusoHorarioConfig, AsyncConfig, RestTemplateConfig
└── util/           MascaraLog, InputSanitizer, EmailUnicoService...
```

### Convenção `infra` x `infrastructure`

Existem dois pacotes de nome parecido por razão histórica. Mover um deles tem risco alto
(muitos imports), então vale a convenção:

- `infra/` = o que atravessa a aplicação: segurança (`configuracoesseguranca`), tenant,
  logging (`CorrelationIdFilter`, MDC), rate limit e exceções (`exceptions`).
- `infrastructure/` = persistência (`persistence/repository`).

Código novo segue essa divisão.

## Entidades principais (`domain/`)

- **Organização e pessoas**: `Organizacao`, `Usuario` (login/senha/perfil), `Profissional`
  (médico ou dentista, por tipo), `AdminOrganizacao` (gestor), `Secretaria`, `Paciente`, `Endereco`.
- **Atendimento**: `Consulta`, `ConfiguracoesConsulta`, `Prontuario` e `ProntuarioDentista`,
  `PlanejamentoTerapeutico`, `TermoAutorizacao`, `FormaPagamento` (cadastro de formas de
  pagamento da consulta).
- **Assinatura e cobrança**: `PlanoAssinatura`, `AssinaturaTenant`, `CobrancaTenant`
  (usa o enum `FormaPagamentoCobranca`), `MpWebhookEvento`.
- **Comunicação e suporte**: `Mensageria`, `ChamadoSuporte`.
- **Auditoria**: `AuditoriaAcessoMaster`, `HistoricoDadosPessoais`.

Banco versionado por Liquibase em `src/main/resources/db/changelog` (arquivo mestre
`db.changelog-master.xml`).

## Multi-tenant

Cada clínica é um tenant (`Organizacao`). Não há filtro automático do Hibernate: o isolamento
é feito de forma explícita.

- `TenantFilter` lê a organização do JWT e a coloca no `TenantContext`.
- `TenantHelper` expõe `getCurrentTenantId()`, `validateTenant(id)`, `isSuperAdmin()`.
- Toda consulta de dados da clínica filtra pela organização do token direto na query
  (ex.: `findByIdAndOrganizacao_Id`). Registro de outra clínica responde como inexistente.
- `organizacaoId` vindo na URL e diferente do token responde 403.
- Cache (Caffeine, `CacheConfig`): a chave inclui o tenant (`org-<id>-...`).
  `CacheEvictionService` invalida por tenant.

## Segurança

- JWT (`TokenService`); `FiltroAcesso` autentica a cada requisição e revalida usuário
  (apagado/bloqueado = 401) e assinatura da clínica (suspensa/cancelada = 403, exceto
  rotas de pagamento). ROOT não depende de assinatura.
- Roles: `ROLE_SUPER_ADMIN` (ROOT), `ROLE_ADMIN` (gestor), `ROLE_RECEPCIONISTA` (secretária),
  `ROLE_PROFISSIONAL`. Autorização por `@PreAuthorize` nos controllers
  (`@EnableMethodSecurity` ativo).
- Senha master do ROOT é intencional e cada uso é auditado em `AuditoriaAcessoMaster`.
- Links públicos (termo, planejamento) usam token de uso único com expiração.

## Cobrança (Mercado Pago)

- `PlanoAssinatura` (STARTER/PROFISSIONAL/BUSINESS), `AssinaturaTenant` por clínica,
  `CobrancaTenant` por fatura.
- Webhook em `/publico/webhooks/mercadopago`, **idempotente** via `MpWebhookEvento`.
- `CobrancaScheduler` (fuso America/Sao_Paulo): links de ativação de trial (08h), **régua de
  carência** (09h: +5 dias INADIMPLENTE, +15 SUSPENSA, +30 suspensa CANCELADA),
  **reconciliação** com o MP (a cada 4h) e **reprocessamento** de webhooks com erro (a cada 15 min).

## Erros

`GlobalExceptionHandler` converte exceções em `ErrorResponse` (`status`, `error`, `message`,
`path`...). `IllegalStateException` (regra de negócio violada) vira 409; `BusinessException` traz
o próprio status; `ResourceNotFoundException` vira 404. Controllers não devem capturar essas
exceções para montar resposta própria.

## Logs

- `CorrelationIdFilter` gera o `requestId`; `TenantFilter`/`FiltroAcesso` põem `tenantId` e
  `userId` no MDC. `@Scheduled` ganha `requestId` próprio (`ScheduledMdcAspect`) e
  tarefas assíncronas copiam o MDC (`MdcTaskDecorator`).
- Dados sensíveis são mascarados com `MascaraLog` (CPF, e-mail, token). Nunca logar senha.
- Toda requisição termina com `http fim <método> <rota> status= duracaoMs=` (`CorrelationIdFilter`).
- Mensagens seguem `<evento> <etapa> chave=valor`, com regra por nível: **[docs/LOGGING.md](docs/LOGGING.md)**.

## Fuso horário

A JVM roda em `America/Sao_Paulo` (`FusoHorarioConfig`, definido também no Dockerfile e no
`main`), e os `@Scheduled` usam a mesma zona.

## Convenções

- Classes `PascalCase`, métodos/variáveis `camelCase`, constantes `UPPER_CASE`.
- Controller só orquestra HTTP; regra de negócio fica no service; acesso a dados no repository.
- Testes: JUnit 5 + Mockito; `./mvnw test -Djacoco.skip=true`. CI em `.github/workflows/ci.yml`.
