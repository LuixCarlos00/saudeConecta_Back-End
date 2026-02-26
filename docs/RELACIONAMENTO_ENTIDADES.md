# Mapa de Relacionamento das Entidades — Saúde Conecta

> Documento gerado em 26/02/2026  
> Baseado no pacote `br.com.saudeConecta.domain`

---

## Visão Geral

O sistema **Saúde Conecta** é uma plataforma multi-tenant de gestão clínica (médica e odontológica). Cada organização (clínica) possui seus próprios pacientes, profissionais, consultas e prontuários, isolados por `organizacao_id`.

### Diagrama de Relacionamento (texto)

```
                           ┌──────────────┐
                           │  Organizacao  │
                           │  (multi-tenant│
                           │   central)    │
                           └──────┬────────┘
          ┌───────────┬───────────┼───────────┬──────────────┬──────────────┐
          │           │           │           │              │              │
          ▼           ▼           ▼           ▼              ▼              ▼
     ┌─────────┐ ┌─────────┐ ┌────────┐ ┌──────────┐ ┌───────────┐ ┌────────────┐
     │ Usuario │ │Paciente │ │Profiss.│ │FormaPgto │ │Mensageria │ │ProcedPadrao│
     └────┬────┘ └────┬────┘ └───┬────┘ └────┬─────┘ └───────────┘ └────────────┘
          │           │          │            │
          │           │    ┌─────┴─────┐      │
          │           │    │           │      │
          │           │    ▼           ▼      │
          │           │ ┌──────┐ ┌────────┐   │
          │           │ │TipoP.│ │Especia.│   │
          │           │ └──────┘ └────────┘   │
          │           │                       │
          │           ▼                       │
          │     ┌──────────┐                  │
          ├────►│ Consulta │◄─────────────────┘
          │     └────┬─────┘
          │          │
          │    ┌─────┴──────────────────────┐
          │    │                            │
          │    ▼                            ▼
          │ ┌────────────┐          ┌───────────────┐
          │ │ Prontuario │          │ProntuarioDent.│
          │ │  (médico)  │          │ (odontológico)│
          │ └────────────┘          └──────┬────────┘
          │                          ┌─────┴──────┐
          │                          │            │
          │                          ▼            ▼
          │                   ┌──────────┐ ┌───────────┐
          │                   │  Dentes  │ │Planejam.  │
          │                   │(odontogr)│ │Terapêutico│
          │                   └──────────┘ └───────────┘
          │
          ├───► AdminOrganizacao
          ├───► ConsultaHistorico
          ├───► HistoricoDadosPessoais
          ├───► ConfiguracaoCardDashboard
          ├───► ConfiguracaoGraficoDashboard
          └───► TermoAutorizacao
```

---

## 1. Organizacao (Entidade Central — Multi-Tenant)

| Campo           | Tipo               | Descrição                          |
|-----------------|--------------------|------------------------------------|
| `id`            | Long (PK)          | Identificador único                |
| `nome`          | String             | Nome fantasia da clínica           |
| `razaoSocial`   | String             | Razão social                       |
| `cnpj`          | String (unique)    | CNPJ da organização                |
| `tipo`          | TipoOrganizacao    | CLINICA, HOSPITAL, etc.            |
| `email`         | String             | E-mail de contato                  |
| `telefone`      | String             | Telefone de contato                |
| `logoUrl`       | String             | URL do logo                        |
| `endereco`      | Endereco (ManyToOne) | Endereço da organização          |
| `status`        | StatusOrganizacao  | ATIVO / INATIVO                    |

**É referenciada por:** Quase todas as entidades do sistema (tenant isolation).

---

## 2. Usuario

| Campo             | Tipo               | Descrição                               |
|-------------------|--------------------|-----------------------------------------|
| `id`              | Long (PK)          | Identificador único                     |
| `organizacao`     | Organizacao (ManyToOne) | Organização à qual pertence        |
| `login`           | String             | Login de acesso                         |
| `senha`           | String             | Senha criptografada                     |
| `tipoUsuario`     | Byte               | Tipo legado (numérico)                  |
| `tipoUsuarioNovo` | TipoUsuarioNovo    | SUPER_ADMIN / ADMIN_ORG / PROFISSIONAL  |
| `status`          | StatusUsuario      | ATIVO / INATIVO                         |

**Implementa:** `UserDetails` (Spring Security), `TenantAware`

### Relacionamentos:
- **Organizacao** → ManyToOne (`organizacao_id`)

---

## 3. AdminOrganizacao

| Campo         | Tipo                | Descrição                                |
|---------------|---------------------|------------------------------------------|
| `id`          | Long (PK)           | Identificador único                      |
| `organizacao` | Organizacao (ManyToOne) | Organização administrada             |
| `usuario`     | Usuario (ManyToOne) | Usuário administrador                    |
| `nome`        | String              | Nome do administrador                    |
| `cargo`       | String              | Cargo na organização                     |
| `email`       | String              | E-mail de contato                        |
| `isOwner`     | Boolean             | Se é o proprietário da organização       |
| `permissoes`  | String (JSON)       | Permissões em formato JSON               |
| `status`      | StatusAdmin         | ATIVO / INATIVO                          |

**Unique Constraint:** `(organizacao_id, usuario_id)`

### Relacionamentos:
- **Organizacao** → ManyToOne (`organizacao_id`)
- **Usuario** → ManyToOne (`usuario_id`)

---

## 4. Endereco

| Campo             | Tipo       | Descrição             |
|-------------------|------------|-----------------------|
| `endCodigo`       | Long (PK)  | Identificador único   |
| `endNacionalidade` | String    | Nacionalidade         |
| `endUF`           | String     | Estado (UF)           |
| `endMunicipio`    | String     | Cidade/Município      |
| `endBairro`       | String     | Bairro                |
| `endCep`          | String     | CEP                   |
| `endRua`          | String     | Logradouro            |
| `endNumero`       | Long       | Número                |
| `endComplemento`  | String     | Complemento           |

**É referenciado por:** `Paciente`, `Profissional`, `Organizacao`

---

## 5. Paciente

| Campo              | Tipo                    | Descrição                       |
|--------------------|-------------------------|---------------------------------|
| `paciCodigo`       | Long (PK)               | Identificador único             |
| `organizacao`      | Organizacao (ManyToOne)  | Organização à qual pertence     |
| `paciNome`         | String                  | Nome completo                   |
| `paciSexo`         | String                  | Sexo                            |
| `paciDataNacimento`| Date                    | Data de nascimento              |
| `paciCpf`          | String                  | CPF                             |
| `paciRg`           | String                  | RG                              |
| `paciEmail`        | String                  | E-mail                          |
| `paciTelefone`     | String                  | Telefone                        |
| `endereco`         | Endereco (ManyToOne)    | Endereço do paciente            |
| `paciStatus`       | String                  | Status (ativo/inativo)          |

### Relacionamentos:
- **Organizacao** → ManyToOne (`organizacao_id`)
- **Endereco** → ManyToOne (`Endereco`)

---

## 6. TipoProfissional

| Campo      | Tipo       | Descrição                          |
|------------|------------|------------------------------------|
| `id`       | Long (PK)  | Identificador único                |
| `codigo`   | String (unique) | MEDICO / DENTISTA             |
| `nome`     | String     | Nome legível (ex: "Médico")        |
| `conselho` | String     | Conselho (CRM, CRO)               |
| `status`   | Byte       | 1 = ativo, 0 = inativo            |

**É referenciado por:** `Profissional`, `Especialidade`

---

## 7. Especialidade

| Campo             | Tipo                      | Descrição                             |
|-------------------|---------------------------|---------------------------------------|
| `id`              | Long (PK)                 | Identificador único                   |
| `tipoProfissional`| TipoProfissional (ManyToOne) | Tipo (MEDICO ou DENTISTA)          |
| `nome`            | String                    | Nome da especialidade                 |
| `codigo`          | String                    | Código opcional                       |
| `status`          | Byte                      | 1 = ativo                             |
| `profissionais`   | Set\<Profissional\> (ManyToMany) | Profissionais desta especialidade |

**Unique Constraint:** `(tipo_profissional_id, nome)`

### Relacionamentos:
- **TipoProfissional** → ManyToOne (`tipo_profissional_id`)
- **Profissional** → ManyToMany (inverso, `mappedBy = "especialidades"`)

---

## 8. Profissional

| Campo                | Tipo                         | Descrição                          |
|----------------------|------------------------------|------------------------------------|
| `id`                 | Long (PK)                    | Identificador único                |
| `organizacao`        | Organizacao (ManyToOne)      | Organização à qual pertence        |
| `tipoProfissional`   | TipoProfissional (ManyToOne) | MEDICO ou DENTISTA                 |
| `nome`               | String                       | Nome completo                      |
| `sexo`               | Sexo (enum)                  | Sexo                               |
| `dataNascimento`     | LocalDate                    | Data de nascimento                 |
| `registroConselho`   | String                       | Nº CRM/CRO                        |
| `cpf`                | String (unique)              | CPF                                |
| `rg`                 | String                       | RG                                 |
| `email`              | String                       | E-mail                             |
| `telefone`           | String                       | Telefone                           |
| `formacao`           | String                       | Formação acadêmica                 |
| `instituicao`        | String                       | Instituição de formação            |
| `tempoConsultaMinutos`| Integer                     | Duração padrão da consulta (min)   |
| `valorConsulta`      | BigDecimal                   | Valor padrão da consulta           |
| `usuario`            | Usuario (ManyToOne)          | Usuário de login vinculado         |
| `endereco`           | Endereco (ManyToOne)         | Endereço do profissional           |
| `status`             | StatusProfissional           | ATIVO / INATIVO                    |
| `especialidades`     | Set\<Especialidade\> (ManyToMany) | Especialidades do profissional |

### Relacionamentos:
- **Organizacao** → ManyToOne (`organizacao_id`)
- **TipoProfissional** → ManyToOne (`tipo_profissional_id`)
- **Usuario** → ManyToOne (`usuario_id`)
- **Endereco** → ManyToOne (`endereco_id`)
- **Especialidade** → ManyToMany (tabela `profissional_especialidade`)

---

## 9. FormaPagamento

| Campo         | Tipo                    | Descrição                          |
|---------------|-------------------------|------------------------------------|
| `id`          | Long (PK)               | Identificador único                |
| `organizacao` | Organizacao (ManyToOne)  | Organização (null = global)        |
| `nome`        | String                  | Nome (Particular, Convênio, etc.)  |
| `codigo`      | String                  | Código opcional                    |
| `status`      | Byte                    | 1 = ativo                          |

### Relacionamentos:
- **Organizacao** → ManyToOne (`organizacao_id`) — se null, é forma de pagamento global

---

## 10. Consulta

| Campo                | Tipo                        | Descrição                            |
|----------------------|-----------------------------|--------------------------------------|
| `id`                 | Long (PK)                   | Identificador único                  |
| `organizacao`        | Organizacao (ManyToOne)     | Organização                          |
| `profissional`       | Profissional (ManyToOne)    | Profissional responsável             |
| `paciente`           | Paciente (ManyToOne)        | Paciente da consulta                 |
| `especialidade`      | Especialidade (ManyToOne)   | Especialidade da consulta            |
| `dataHora`           | LocalDateTime               | Data/hora de início                  |
| `duracaoMinutos`     | Integer (default 30)        | Duração em minutos                   |
| `observacoes`        | String (TEXT)               | Observações                          |
| `formaPagamento`     | FormaPagamento (ManyToOne)  | Forma de pagamento                   |
| `valor`              | BigDecimal                  | Valor cobrado                        |
| `status`             | StatusConsulta              | AGENDADA / REALIZADA / CANCELADA     |
| `canceladoPor`       | CanceladoPor                | PACIENTE / PROFISSIONAL / ADMIN      |
| `motivoCancelamento` | String (TEXT)               | Motivo do cancelamento               |
| `criadoPor`          | Usuario (ManyToOne)         | Usuário que criou a consulta         |

### Relacionamentos:
- **Organizacao** → ManyToOne (`organizacao_id`)
- **Profissional** → ManyToOne (`profissional_id`)
- **Paciente** → ManyToOne (`paciente_id`)
- **Especialidade** → ManyToOne (`especialidade_id`)
- **FormaPagamento** → ManyToOne (`forma_pagamento_id`)
- **Usuario** → ManyToOne (`criado_por`)

**Campo derivado:** `getDataHoraFim()` = `dataHora + duracaoMinutos`

---

## 11. ConsultaHistorico

| Campo            | Tipo                    | Descrição                             |
|------------------|-------------------------|---------------------------------------|
| `id`             | Long (PK)               | Identificador único                   |
| `consulta`       | Consulta (ManyToOne)    | Consulta rastreada                    |
| `statusAnterior` | StatusConsulta           | Status antes da alteração             |
| `statusNovo`     | StatusConsulta           | Status novo                           |
| `observacao`     | String (TEXT)            | Observação sobre a mudança            |
| `alteradoPor`    | Usuario (ManyToOne)     | Quem alterou                          |

### Relacionamentos:
- **Consulta** → ManyToOne (`consulta_id`)
- **Usuario** → ManyToOne (`alterado_por`)

---

## 12. Prontuario (Médico)

| Campo                               | Tipo                     | Descrição                        |
|--------------------------------------|--------------------------|----------------------------------|
| `prontCodigoProntuario`             | Long (PK)                | Identificador único              |
| `prontPeso`                          | String                   | Peso                             |
| `prontAltura`                        | String                   | Altura                           |
| `prontTemperatura`                   | String                   | Temperatura                      |
| `prontSexo`                          | String                   | Sexo                             |
| `prontSaturacao`                     | String                   | Saturação O₂                     |
| `prontHemoglobina`                   | String                   | Hemoglobina                      |
| `prontPressao`                       | String                   | Pressão arterial                 |
| `prontFrequenciaRespiratoria`        | String                   | Frequência respiratória          |
| `prontFrequenciaArterialSistolica`   | String                   | PA sistólica                     |
| `prontFrequenciaArterialDiastolica`  | String                   | PA diastólica                    |
| `prontObservacao`                    | String                   | Observação                       |
| `prontCondulta`                      | String                   | Conduta médica                   |
| `prontAnamnese`                      | String                   | Anamnese                         |
| `prontQueixaPricipal`                | String                   | Queixa principal                 |
| `prontDiagnostico`                   | String                   | Diagnóstico                      |
| `prontModeloPrescricao`              | String                   | Modelo de prescrição             |
| `prontTituloPrescricao`              | String                   | Título da prescrição             |
| `prontDataPrescricao`                | String                   | Data da prescrição               |
| `prontPrescricao`                    | String                   | Texto da prescrição              |
| `prontModeloExame`                   | String                   | Modelo de exame                  |
| `prontTituloExame`                   | String                   | Título do exame                  |
| `prontDataExame`                     | String                   | Data do exame                    |
| `prontExame`                         | String                   | Texto do exame                   |
| `prontTempoDuracao`                  | String                   | Tempo de duração da consulta     |
| `prontDataFinalizado`                | Date                     | Data de finalização              |
| `profissional`                       | Profissional (ManyToOne) | Profissional responsável         |
| `consulta`                           | Consulta (ManyToOne)     | Consulta vinculada               |

### Relacionamentos:
- **Profissional** → ManyToOne (`prontCodigoMedico`)
- **Consulta** → ManyToOne (`consulta`)

---

## 13. ProntuarioDentista (Odontológico)

| Campo               | Tipo                     | Descrição                          |
|----------------------|--------------------------|------------------------------------|
| `codigo`             | Long (PK)                | Identificador único                |
| **Anamnese**         |                          |                                    |
| `queixaPrincipal`    | String (TEXT)            | Queixa principal                   |
| `anamnese`           | String (TEXT)            | Histórico odontológico             |
| `observacao`         | String (TEXT)            | Observações                        |
| **Exame Clínico**    |                          |                                    |
| `higieneBucal`       | String                   | Higiene bucal                      |
| `condicaoGengival`   | String                   | Condição gengival                  |
| `oclusal`            | String (TEXT)            | Avaliação oclusal                  |
| `atm`                | String (TEXT)            | Articulação temporomandibular      |
| **Diagnóstico**      |                          |                                    |
| `diagnostico`        | String (TEXT)            | Diagnóstico                        |
| `planoTratamento`    | String (TEXT)            | Plano de tratamento                |
| **Prescrição**       |                          |                                    |
| `tituloPrescricao`   | String                   | Título da prescrição               |
| `dataPrescricao`     | String                   | Data da prescrição                 |
| `prescricao`         | String (TEXT)            | Texto da prescrição                |
| **Procedimentos**    |                          |                                    |
| `tituloExame`        | String                   | Título do exame                    |
| `dataExame`          | String                   | Data do exame                      |
| `procedimentos`      | String (TEXT)            | Procedimentos realizados           |
| `orientacoes`        | String (TEXT)            | Orientações ao paciente            |
| **Identificação**    |                          |                                    |
| `responsavel`        | String                   | Nome do responsável (se menor)     |
| `inicioTratamento`   | LocalDate                | Data início do tratamento          |
| `terminoTratamento`  | LocalDate                | Data término do tratamento         |
| `interrupcao`        | String                   | Motivo de interrupção              |
| **Sinais Vitais**    |                          |                                    |
| `pressaoArterial`    | String                   | Pressão arterial                   |
| `pulso`              | String                   | Pulso                              |
| `altura`             | String                   | Altura                             |
| `temperatura`        | String                   | Temperatura                        |
| `peso`               | String                   | Peso                               |
| `edema`              | String                   | Edema                              |
| `facies`             | String                   | Fácies                             |
| `linfonodos`         | String                   | Linfonodos                         |
| `labios`             | String                   | Lábios                             |
| `mucosas`            | String                   | Mucosas                            |
| `soalhoBucal`        | String                   | Soalho bucal                       |
| `palato`             | String                   | Palato                             |
| `orofaringe`         | String                   | Orofaringe                         |
| **Exame Intrabucal** |                          |                                    |
| `lingua`             | String                   | Língua                             |
| `gengiva`            | String                   | Gengiva                            |
| `habitosNocivos`     | String                   | Hábitos nocivos                    |
| `portadorAparelho`   | String                   | Se é portador de aparelho          |
| `oclusao`            | String                   | Oclusão                            |
| `exameOutros`        | String (TEXT)            | Outros exames                      |
| **Controle**         |                          |                                    |
| `dataFinalizado`     | LocalDate                | Data de finalização                |
| `tempoDuracao`       | String                   | Duração da consulta                |

### Relacionamentos:
- **Profissional** → ManyToOne (`prontdent_codigo_medico`)
- **Consulta** → ManyToOne (`prontdent_consulta`)
- **ProntuarioDentistaDente** → OneToMany (`mappedBy = "prontuarioDentista"`, cascade ALL)
- **PlanejamentoTerapeutico** → OneToMany (`mappedBy = "prontuarioDentista"`, cascade ALL)

---

## 14. ProntuarioDentistaDente (Odontograma)

| Campo                | Tipo                           | Descrição                                               |
|----------------------|--------------------------------|---------------------------------------------------------|
| `codigo`             | Long (PK)                      | Identificador único                                     |
| `prontuarioDentista` | ProntuarioDentista (ManyToOne) | Prontuário pai                                          |
| `numeroFdi`          | Integer                        | Número FDI do dente (11–48)                             |
| `status`             | String                         | sadio/cariado/obturado/ausente/protese/canal/fratura/implante |
| `observacao`         | String (TEXT)                  | Observação sobre o dente                                |

**Unique Constraint:** `(prontdentd_prontuario, prontdentd_numero_fdi)`

### Relacionamentos:
- **ProntuarioDentista** → ManyToOne (`prontdentd_prontuario`)

---

## 15. PlanejamentoTerapeutico

| Campo                | Tipo                           | Descrição                            |
|----------------------|--------------------------------|--------------------------------------|
| `id`                 | Long (PK)                      | Identificador único                  |
| `prontuarioDentista` | ProntuarioDentista (ManyToOne) | Prontuário odontológico pai          |
| `consulta`           | Consulta (ManyToOne)           | Consulta vinculada                   |
| `paciente`           | Paciente (ManyToOne)           | Paciente                             |
| `profissional`       | Profissional (ManyToOne)       | Profissional responsável             |
| `organizacao`        | Organizacao (ManyToOne)        | Organização                          |
| `dataProcedimento`   | LocalDate                      | Data do procedimento                 |
| `procedimentoRealizado` | String                      | Descrição do procedimento            |
| `valor`              | BigDecimal                     | Valor do procedimento                |
| `tokenAssinatura`    | String                         | Token para link de assinatura pública|
| `assinaturaBase64`   | String (TEXT)                  | Assinatura digital do paciente       |
| `dataAssinatura`     | LocalDateTime                  | Data/hora da assinatura              |
| `statusAssinatura`   | String                         | PENDENTE / ASSINADO                  |

### Relacionamentos:
- **ProntuarioDentista** → ManyToOne (`prontuario_dentista_id`)
- **Consulta** → ManyToOne (`consulta_id`)
- **Paciente** → ManyToOne (`paciente_id`)
- **Profissional** → ManyToOne (`profissional_id`)
- **Organizacao** → ManyToOne (`organizacao_id`)

---

## 16. ProcedimentoPadrao

| Campo              | Tipo                     | Descrição                            |
|--------------------|--------------------------|--------------------------------------|
| `id`               | Long (PK)                | Identificador único                  |
| `profissional`     | Profissional (ManyToOne) | Profissional dono do procedimento    |
| `organizacao`      | Organizacao (ManyToOne)  | Organização                          |
| `nomeProcedimento` | String                   | Nome do procedimento                 |
| `valorPadrao`      | BigDecimal               | Valor padrão                         |
| `ativo`            | Boolean                  | Se está ativo                        |

### Relacionamentos:
- **Profissional** → ManyToOne (`profissional_id`)
- **Organizacao** → ManyToOne (`organizacao_id`)

---

## 17. TermoAutorizacao (Questionário de Saúde)

| Campo                   | Tipo                    | Descrição                             |
|-------------------------|-------------------------|---------------------------------------|
| `id`                    | Long (PK)               | Identificador único                   |
| `consulta`              | Consulta (ManyToOne)    | Consulta vinculada                    |
| `paciente`              | Paciente (ManyToOne)    | Paciente                              |
| `organizacao`           | Organizacao (ManyToOne) | Organização                           |
| `token`                 | String (unique)         | Token para link público               |
| `dataExpiracao`         | LocalDateTime           | Data de expiração do link             |
| `dataAssinatura`        | LocalDateTime           | Data/hora da assinatura               |
| `assinaturaBase64`      | String (TEXT)           | Assinatura digital                    |
| `ipOrigem`              | String                  | IP do paciente ao assinar             |
| `respostasQuestionario` | String (TEXT)           | Respostas do questionário (JSON)      |
| `status`                | String                  | PENDENTE / ASSINADO                   |

### Relacionamentos:
- **Consulta** → ManyToOne (`consulta_id`)
- **Paciente** → ManyToOne (`paciente_id`)
- **Organizacao** → ManyToOne (`organizacao_id`)

---

## 18. Mensageria

| Campo                       | Tipo                     | Descrição                          |
|-----------------------------|--------------------------|------------------------------------|
| `id`                        | Long (PK)                | Identificador único                |
| `organizacao`               | Organizacao (ManyToOne)  | Organização                        |
| `destinatarioProfissional`  | Profissional (ManyToOne) | Profissional destinatário          |
| `destinatarioEmail`         | String                   | E-mail do destinatário             |
| `destinatarioNome`          | String                   | Nome do destinatário               |
| `assunto`                   | String                   | Assunto da mensagem                |
| `corpoMensagem`             | String (TEXT)            | Corpo completo (template)          |
| `tipoMensagem`              | TipoMensagem             | Tipo (enum)                        |
| `status`                    | StatusMensagem           | PENDENTE / ENVIADA / ERRO          |
| `erroDetalhe`               | String (TEXT)            | Detalhes do erro (se falhou)       |
| `tentativas`                | Integer                  | Nº de tentativas de envio          |
| `adminNotificado`           | Boolean                  | Se o admin foi notificado do erro  |

### Relacionamentos:
- **Organizacao** → ManyToOne (`organizacao_id`)
- **Profissional** → ManyToOne (`destinatario_profissional_id`)

---

## 19. HistoricoDadosPessoais (Auditoria)

| Campo          | Tipo                    | Descrição                              |
|----------------|-------------------------|----------------------------------------|
| `id`           | Long (PK)               | Identificador único                    |
| `organizacao`  | Organizacao (ManyToOne) | Organização                            |
| `usuario`      | Usuario (ManyToOne)     | Usuário que fez a alteração            |
| `entidade`     | EntidadeTipo            | PACIENTE / PROFISSIONAL / ADMIN        |
| `idEntidade`   | Long                    | ID do registro alterado                |
| `campo`        | String                  | Nome do campo alterado                 |
| `valorAnterior`| String (TEXT)           | Valor antes da alteração               |
| `valorNovo`    | String (TEXT)           | Valor após a alteração                 |

### Relacionamentos:
- **Organizacao** → ManyToOne (`organizacao_id`)
- **Usuario** → ManyToOne (`usuario_id`)

---

## 20. ConfiguracaoCardDashboard

| Campo          | Tipo                    | Descrição                              |
|----------------|-------------------------|----------------------------------------|
| `id`           | Long (PK)               | Identificador único                    |
| `organizacao`  | Organizacao (ManyToOne) | Organização                            |
| `usuario`      | Usuario (ManyToOne)     | Usuário dono da configuração           |
| `tipoCard`     | TipoCardDashboard       | Tipo do card (enum)                    |
| `ativo`        | Boolean                 | Se o card está visível                 |
| `ordemExibicao`| Integer                 | Posição na tela                        |

**Unique Constraint:** `(usuario_id, tipo_card)`

### Relacionamentos:
- **Organizacao** → ManyToOne (`organizacao_id`)
- **Usuario** → ManyToOne (`usuario_id`)

---

## 21. ConfiguracaoGraficoDashboard

| Campo          | Tipo                       | Descrição                            |
|----------------|----------------------------|--------------------------------------|
| `id`           | Long (PK)                  | Identificador único                  |
| `organizacao`  | Organizacao (ManyToOne)    | Organização                          |
| `usuario`      | Usuario (ManyToOne)        | Usuário dono da configuração         |
| `tipoGrafico`  | TipoGraficoDashboard       | Tipo do gráfico (enum)               |
| `ativo`        | Boolean                    | Se o gráfico está visível            |
| `ordemExibicao`| Integer                    | Posição na tela                      |

**Unique Constraint:** `(usuario_id, tipo_grafico)`

### Relacionamentos:
- **Organizacao** → ManyToOne (`organizacao_id`)
- **Usuario** → ManyToOne (`usuario_id`)

---

## 22. CodigoVerificacao

| Campo                       | Tipo           | Descrição                     |
|-----------------------------|----------------|-------------------------------|
| `codVerificacaoCodigoID`    | Long (PK)      | Identificador único           |
| `codVerificacaoCodigo`      | String (6 chars)| Código de verificação         |
| `codVerificacaoTempo`       | LocalDateTime  | Data/hora de expiração        |

> Entidade independente, sem relacionamentos com outras tabelas.

---

## Enums do Sistema

| Enum                     | Valores                                           | Usado em                    |
|--------------------------|---------------------------------------------------|-----------------------------|
| `StatusConsulta`         | AGENDADA, REALIZADA, CANCELADA                    | Consulta                    |
| `CanceladoPor`           | PACIENTE, PROFISSIONAL, ADMIN                     | Consulta                    |
| `TipoOrganizacao`        | (definido na entidade)                             | Organizacao                 |
| `StatusOrganizacao`      | ATIVO, INATIVO                                    | Organizacao                 |
| `StatusProfissional`     | ATIVO, INATIVO                                    | Profissional                |
| `Sexo`                   | MASCULINO, FEMININO                               | Profissional                |
| `StatusUsuario`          | ATIVO, INATIVO                                    | Usuario                     |
| `TipoUsuarioNovo`        | SUPER_ADMIN, ADMIN_ORG, PROFISSIONAL              | Usuario                     |
| `TipoMensagem`           | (definido na entidade)                             | Mensageria                  |
| `StatusMensagem`         | PENDENTE, ENVIADA, ERRO                           | Mensageria                  |
| `EntidadeTipo`           | PACIENTE, PROFISSIONAL, ADMIN                     | HistoricoDadosPessoais      |
| `TipoCardDashboard`      | (definido na entidade)                             | ConfiguracaoCardDashboard   |
| `TipoGraficoDashboard`   | (definido na entidade)                             | ConfiguracaoGraficoDashboard|

---

## Tabelas Associativas (Join Tables)

| Tabela                        | Colunas                                  | Relacionamento                          |
|-------------------------------|------------------------------------------|-----------------------------------------|
| `profissional_especialidade`  | `profissional_id`, `especialidade_id`    | Profissional ↔ Especialidade (ManyToMany)|

---

## Padrão Multi-Tenant

As seguintes entidades implementam `TenantAware` e são filtradas por `organizacao_id`:

- `Usuario`
- `Paciente`
- `Profissional`
- `Consulta`
- `Mensageria`
- `HistoricoDadosPessoais`

---

## Fluxo Principal de Dados

```
Organizacao
  └─► Usuario (login/autenticação)
  └─► AdminOrganizacao (gestão)
  └─► Paciente (cadastro)
  │     └─► Endereco
  └─► Profissional (cadastro)
  │     └─► TipoProfissional (MEDICO/DENTISTA)
  │     └─► Especialidade (ManyToMany)
  │     └─► Endereco
  │     └─► Usuario (vínculo login)
  └─► FormaPagamento
  └─► Consulta (agendamento)
  │     └─► Paciente
  │     └─► Profissional
  │     └─► Especialidade
  │     └─► FormaPagamento
  │     └─► ConsultaHistorico (auditoria de status)
  │     │
  │     ├─► Prontuario (médico — após finalização)
  │     │
  │     ├─► ProntuarioDentista (odontológico — após finalização)
  │     │     └─► ProntuarioDentistaDente (odontograma)
  │     │     └─► PlanejamentoTerapeutico (procedimentos planejados)
  │     │           └─► Assinatura digital do paciente (via token público)
  │     │
  │     └─► TermoAutorizacao (questionário de saúde — via token público)
  │
  └─► ProcedimentoPadrao (catálogo de procedimentos por profissional)
  └─► Mensageria (e-mails enviados)
  └─► HistoricoDadosPessoais (auditoria de alterações)
  └─► ConfiguracaoCardDashboard (personalização do dashboard)
  └─► ConfiguracaoGraficoDashboard (personalização do dashboard)
```
