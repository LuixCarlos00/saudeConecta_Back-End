 # SaúdeConecta - Backend API

Sistema de gestão de clínica médica desenvolvido com Spring Boot.

## Descrição

O SaúdeConecta é uma API REST para gerenciamento de clínicas médicas, permitindo:

- **Gestão de Usuários**: Autenticação JWT com diferentes níveis de acesso (Admin, Secretária, Médico, Paciente)
- **Agendamento de Consultas**: CRUD completo de consultas médicas
- **Cadastro de Profissionais**: Médicos, secretárias e administradores
- **Cadastro de Pacientes**: Informações completas com prontuário
- **Notificações**: Envio de e-mails (Gmail/SMTP) com registro na mensageria
- **Assinaturas**: Planos por clínica cobrados via Mercado Pago (régua de cobrança, webhook e reconciliação)
- **Verificação 2FA**: Código de verificação por email

## Tecnologias

| Tecnologia | Versão | Descrição |
|------------|--------|-----------|
| Java | 17 | Linguagem principal |
| Spring Boot | 3.0.5 | Framework |
| Spring Security | - | Autenticação/Autorização |
| Spring Data JPA | - | Persistência |
| MySQL | 8.x | Banco de dados |
| JWT (Auth0) | 4.2.1 | Tokens de autenticação |
| Lombok | 1.18.30 | Redução de boilerplate |
| Liquibase | - | Migrações de banco |
| Caffeine | - | Cache em memória |
| Thymeleaf | 3.0.5 | Templates de email |

## Pré-requisitos

- JDK 17+
- Maven 3.8+
- MySQL 8.x
- Conta Gmail (para envio de emails)
- Conta Mercado Pago (opcional em dev; sem as chaves as rotas de assinatura respondem 503)

## Instalação

### 1. Clone o repositório

```bash
git clone https://github.com/LuixCarlos00/saudeConecta_Back-End.git
cd saudeConecta_Back-End/saudeConectaAPI
```

### 2. Configure as variáveis de ambiente

O perfil de produção (`application-prod.properties`) lê tudo de variáveis de ambiente.
O arquivo `.env.example` traz o modelo:

| Variável | Obrigatória | Descrição |
|----------|-------------|-----------|
| `MYSQLHOST`, `MYSQLPORT`, `MYSQLDATABASE`, `MYSQLUSER`, `MYSQLPASSWORD` | Sim | Conexão com o MySQL |
| `JWT_SECRET` | Sim | Segredo do JWT (`openssl rand -base64 64`) |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | Sim | Gmail e App Password |
| `MP_ACCESS_TOKEN`, `MP_PUBLIC_KEY`, `MP_WEBHOOK_SECRET`, `MP_BACK_URL` | Sim | Mercado Pago (`MP_BACK_URL` deve ser pública) |
| `CORS_ALLOWED_ORIGINS` | Não | Origens liberadas no CORS, separadas por vírgula |
| `HTTP_CONNECT_TIMEOUT`, `HTTP_READ_TIMEOUT` | Não | Timeouts das chamadas HTTP externas (padrão `5s` e `15s`) |
| `SERVER_PORT` | Não | Porta (padrão `8080`) |

Em desenvolvimento (`application.properties`) os mesmos nomes valem, com defaults locais.

### 3. Crie o banco de dados

```sql
CREATE DATABASE saudeConecta;
```

### 4. Execute a aplicação

```bash
./mvnw spring-boot:run
```

A API estará disponível em `http://localhost:8080`

## Estrutura do Projeto

```
src/main/java/br/com/saudeConecta/
├── SaudeConectaApplication.java
├── domain/            # Entidades JPA e enums, por assunto
├── application/       # Casos de uso por área (<area>/XxxService, <area>/dto, exception/)
├── presentation/      # Controllers REST por área (controller/<area>) e erro/ (handler HTTP)
├── infrastructure/    # Persistência (repository, listener...), Mercado Pago e e-mail
└── config/            # Transversais: security, tenant, logging, web, cache, async
```

Veja o `ARQUITETURA.md` para camadas, multi-tenant, segurança e cobrança.

## Endpoints Principais

Os controllers ficam em `presentation/controller/<area>`. Os prefixos principais são:
`/Home` (login e senha), `/usuario`, `/administrador`, `/secretaria`, `/profissionais`,
`/pacientes`, `/consultas`, `/prontuario`, `/prontuario-dentista`, `/planejamento-terapeutico`,
`/relatorios`, `/mensageria`, `/chamados-suporte`, `/planos`, `/assinaturas`, `/cobrancas`,
`/publico/**` (cadastro, questionário e webhook do Mercado Pago, sem JWT).

## Autenticação

A API utiliza JWT (JSON Web Token) para autenticação.

### Obtendo o Token

```bash
curl -X POST http://localhost:8080/Home/login \
  -H "Content-Type: application/json" \
  -d '{"login": "usuario", "senha": "senha123"}'
```

### Usando o Token

```bash
curl -X GET http://localhost:8080/profissionais \
  -H "Authorization: Bearer SEU_TOKEN_JWT"
```

## Níveis de Acesso

| Role | Permissões |
|------|------------|
| `ROLE_SUPER_ADMIN` (ROOT) | Administra todas as clínicas (planos, estatísticas globais, auditoria) |
| `ROLE_ADMIN` | Gestor da clínica |
| `ROLE_RECEPCIONISTA` | Secretária: agenda, pacientes, consultas |
| `ROLE_PROFISSIONAL` | Médico/dentista: agenda e prontuários |

## Testes

```bash
./mvnw test -Djacoco.skip=true
```

A CI (`.github/workflows/ci.yml`) roda esse mesmo comando em push e pull request.

## Contribuição

1. Fork o projeto
2. Crie uma branch (`git checkout -b feature/nova-funcionalidade`)
3. Commit suas mudanças (`git commit -m 'feat: adiciona nova funcionalidade'`)
4. Push para a branch (`git push origin feature/nova-funcionalidade`)
5. Abra um Pull Request

## Licença

Este projeto está sob a licença MIT.

## Contato

- **Desenvolvedor**: Luix Carlos
- **GitHub**: [@LuixCarlos00](https://github.com/LuixCarlos00)
