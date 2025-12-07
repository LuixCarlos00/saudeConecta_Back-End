 # SaúdeConecta - Backend API

Sistema de gestão de clínica médica desenvolvido com Spring Boot.

## Descrição

O SaúdeConecta é uma API REST para gerenciamento de clínicas médicas, permitindo:

- **Gestão de Usuários**: Autenticação JWT com diferentes níveis de acesso (Admin, Secretária, Médico, Paciente)
- **Agendamento de Consultas**: CRUD completo de consultas médicas
- **Cadastro de Profissionais**: Médicos, secretárias e administradores
- **Cadastro de Pacientes**: Informações completas com prontuário
- **Notificações**: Envio de emails e SMS via Twilio
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
| Twilio | 8.17.0 | Envio de SMS |
| Thymeleaf | 3.0.5 | Templates de email |

## Pré-requisitos

- JDK 17+
- Maven 3.8+
- MySQL 8.x
- Conta Gmail (para envio de emails)
- Conta Twilio (para envio de SMS - opcional)

## Instalação

### 1. Clone o repositório

```bash
git clone https://github.com/LuixCarlos00/saudeConecta_Back-End.git
cd saudeConecta_Back-End/saudeConectaAPI
```

### 2. Configure as variáveis de ambiente

Copie o arquivo de exemplo e configure suas credenciais:

```bash
cp .env.example .env
```

Edite o arquivo `.env` com suas configurações:

```properties
# Banco de dados
DB_URL=jdbc:mysql://localhost:3306/saudeConecta
DB_USERNAME=seu_usuario
DB_PASSWORD=sua_senha

# JWT Secret (gere uma chave forte)
JWT_SECRET=sua_chave_secreta_minimo_32_caracteres

# Email
MAIL_USERNAME=seu_email@gmail.com
MAIL_PASSWORD=sua_app_password

# Twilio (opcional)
TWILIO_ACCOUNT_SID=seu_account_sid
TWILIO_AUTH_TOKEN=seu_auth_token
TWILIO_PHONE_NUMBER=seu_numero
```

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
├── SaudeConectaApplication.java    # Ponto de entrada
├── email/                          # Serviços de email
│   ├── EnviarEmail/
│   └── EnviarService/
├── endpoints/                      # Módulos da API
│   ├── administrador/              # CRUD Administrador
│   ├── codigoVerificacao/          # Verificação 2FA
│   ├── consulta/                   # CRUD Consultas
│   ├── consultaStatus/             # Status de consultas
│   ├── endereco/                   # CRUD Endereços
│   ├── medico/                     # CRUD Médicos
│   ├── paciente/                   # CRUD Pacientes
│   ├── prontuario/                 # CRUD Prontuários
│   ├── secretaria/                 # CRUD Secretárias
│   └── usuario/                    # Autenticação
├── infra/
│   ├── configuracoesseguranca/     # Configurações de segurança
│   └── exceptions/                 # Tratamento de erros
└── util/                           # Utilitários
```

## Endpoints Principais

### Autenticação

| Método | Endpoint | Descrição | Auth |
|--------|----------|-----------|------|
| POST | `/Home/login` | Login do usuário | Não |
| POST | `/Home/cadastralogin` | Cadastro de usuário | Não |
| PUT | `/Home/trocaDeSenha/{id}` | Troca de senha | Não |

### Médicos

| Método | Endpoint | Descrição | Auth |
|--------|----------|-----------|------|
| GET | `/medico/listatodosmedicos` | Lista todos médicos | Sim |
| GET | `/medico/buscarId/{id}` | Busca médico por ID | Sim |
| POST | `/medico/post` | Cadastra médico | Sim |
| DELETE | `/medico/{id}` | Remove médico | Sim |

### Pacientes

| Método | Endpoint | Descrição | Auth |
|--------|----------|-----------|------|
| GET | `/paciente/listatodospacientes` | Lista todos pacientes | Sim |
| GET | `/paciente/buscarId/{id}` | Busca paciente por ID | Sim |
| POST | `/paciente/post` | Cadastra paciente | Sim |

### Consultas

| Método | Endpoint | Descrição | Auth |
|--------|----------|-----------|------|
| GET | `/consulta/listatodasConsulta` | Lista todas consultas | Sim |
| POST | `/consulta/post` | Agenda consulta | Sim |
| PUT | `/consulta/editar/{id}` | Edita consulta | Sim |
| DELETE | `/consulta/{id}` | Cancela consulta | Sim |

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
curl -X GET http://localhost:8080/medico/listatodosmedicos \
  -H "Authorization: Bearer SEU_TOKEN_JWT"
```

## Níveis de Acesso

| Tipo | Código | Permissões |
|------|--------|------------|
| Admin | 1 | Acesso total |
| Secretária | 2 | Gestão de consultas e pacientes |
| Médico | 3 | Visualização de agenda e prontuários |
| Paciente | 4 | Visualização própria |

## Testes

```bash
./mvnw test
```

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
