# Massa de demonstração (seed-demo.sql)

> ⚠️ **O script APAGA todos os dados de negócio do banco em que rodar** (usuários, clínicas, pacientes,
> agenda, prontuários, assinaturas, cobranças...) e recria a demo. Nunca rode em produção.

## Como rodar

1. Suba a API uma vez apontando para o banco, para o Liquibase criar o schema (até o changeset 066).
2. Rode o script no banco escolhido (não há `USE` fixo):
   ```bash
   mysql -u root -p office < /home/luiz.carlos/Documentos/GitHub/saudeConectaAPI/docs/demo/seed-demo.sql
   ```
3. Pode rodar quantas vezes quiser: cada execução apaga e recria tudo, sempre com as mesmas contagens.
   Leva uns 2 s. Depois reinicie a API, ou espere uns 5 min, para o cache (Caffeine) não mostrar dados antigos.

As datas são relativas ao momento da importação, no horário de Brasília. Cada profissional tem cerca de
34 dias de agenda para trás e 34 para frente, só em dias úteis. Fim de semana e feriado não entram, e os
feriados não são tratados.

**Para gerar outra massa aleatória:** `python3 docs/demo/gerar_seed_demo.py --seed 123`. Precisa só de stdlib + `bcrypt`.
A seed padrão (`20261105`) gera sempre o mesmo arquivo, e os logins abaixo valem para ela.

## O que o script apaga e o que preserva

- **Preserva:** `DATABASECHANGELOG*`, `tipo_profissional`, `especialidade`, `plano_assinatura` e as formas de pagamento globais (`forma_pagamento.organizacao_id IS NULL`). Esses dados vêm dos changesets, e o script os busca por código, nunca por id.
- **Apaga** todas as outras tabelas do schema. O ROOT é recriado com o login do changeset 031.
  Detalhes no cabeçalho do `.sql`.
- Tabela que **não** é do Liquibase continua intacta. Se o seu banco tiver uma, ela não é apagada.

## Tenants

| Tenant | Plano | Gestores / Secretárias / Clínicos | Observação |
|---|---|---|---|
| Clínica Vida Plena (SP) | BUSINESS | 2 / 4 / 8 (4 médicos, 4 dentistas) | ATIVA, paga mensalmente |
| Centro Médico Bem Viver (Campinas) | PROFISSIONAL | 2 / 2 / 4 | ATIVA. Fluxo acelerado: consulta já nasce CONFIRMADA |
| Consultório Saúde & Sorriso (Curitiba) | STARTER | 1 / 1 / 2 | ATIVA |
| Clínica Integrada Horizonte (BH) | STARTER **personalizado** | 2 / 3 / 5 | Limites de 2 gestores, 5 clínicos e 3 secretárias: 1 gestor, 3 clínicos e 2 secretárias extras, R$ 234 a mais. Mensalidade de R$ 383 |
| Policlínica Mar Azul (Recife) | PROFISSIONAL **personalizado + cortesia** | 3 / 3 / 6 | Limites de 3 gestores e 10 clínicos. Secretária fica no limite do plano (4). Cortesia: não há cobrança e a régua não atua |

O modelo não tem um "contrato à parte" por pessoa. A coisa mais próxima é a personalização do plano:
`assinatura_tenant.limite_*_custom`, `qtd_*_extra` e `valor_adicional_perfis`. Os valores são os mesmos que
`AssinaturaTenantService.customizarPlano` calcularia.

As assinaturas pagas têm uma cobrança `PAGO` por mês, desde `data_inicio`, com forma `MANUAL`. Elas não
têm nenhum id do Mercado Pago, e o vencimento cai sempre no futuro. Por isso a reconciliação, o webhook e
a régua de cobrança não mexem nelas.

## Logins (login = senha)

- **ROOT:** a senha dele também é a *senha master* da aplicação. Qualquer login com a senha `14776815664`
  entra como aquele usuário, e o acesso fica auditado.
- **Gestor dono:** entra com o **CNPJ** da clínica. É o que acontece no cadastro de tenant Pessoa Jurídica.
  Os outros gestores, as secretárias e os clínicos entram com o próprio CPF.
- **E-mails:** todos usam `@example.com`, um domínio reservado. Nenhum e-mail real recebe nada.

| Tenant | Perfil | Login = senha | Nome |
|---|---|---|---|
| Plataforma | ROOT (SUPER_ADMIN) | `14776815664` | Super Administrador |
| Clínica Vida Plena | GESTOR (dono, login=CNPJ) | `28617486000120` | Bianca Nascimento Batista |
| Clínica Vida Plena | GESTOR | `32194752641` | Gustavo Machado Oliveira |
| Clínica Vida Plena | ASSISTENTE (secretária) | `46457663508` | Otávio Barbosa Teixeira |
| Clínica Vida Plena | ASSISTENTE (secretária) | `06087325590` | Aline Ribeiro Dias |
| Clínica Vida Plena | ASSISTENTE (secretária) | `62494434289` | Sofia Soares Machado |
| Clínica Vida Plena | ASSISTENTE (secretária) | `07599229264` | Lívia Santos Ramos |
| Clínica Vida Plena | CLINICO (médico - CARDIO) | `33388124779` | Dra. Maria Santana Santos |
| Clínica Vida Plena | CLINICO (médico - DERMA) | `78398330716` | Dra. Letícia Ferreira Carvalho |
| Clínica Vida Plena | CLINICO (médico - MED_CLINICA_MEDICA) | `29085870330` | Dra. Helena Cardoso Machado |
| Clínica Vida Plena | CLINICO (médico - GINE) | `35678745271` | Dra. Bruna Costa Machado |
| Clínica Vida Plena | CLINICO (dentista - ORTODONT) | `20264370414` | Dr. Thiago Pinto Rodrigues |
| Clínica Vida Plena | CLINICO (dentista - ENDO) | `23292331676` | Dra. Carolina Lima Moreira |
| Clínica Vida Plena | CLINICO (dentista - IMPL) | `77432614253` | Dr. Arthur Marques Cardoso |
| Clínica Vida Plena | CLINICO (dentista - GERAL) | `82392606323` | Dr. Felipe Marques Nascimento |
| Centro Médico Bem Viver | GESTOR (dono, login=CNPJ) | `48889029000100` | Tatiane Soares Alves |
| Centro Médico Bem Viver | GESTOR | `48586295388` | Caio Batista Rocha |
| Centro Médico Bem Viver | ASSISTENTE (secretária) | `50386667365` | Fernanda Moura Pinto |
| Centro Médico Bem Viver | ASSISTENTE (secretária) | `30328509248` | Viviane Pinto Nunes |
| Centro Médico Bem Viver | CLINICO (médico - PED) | `16967386107` | Dr. Lorenzo Silva Oliveira |
| Centro Médico Bem Viver | CLINICO (médico - ORTO) | `05578730450` | Dra. Raquel Mendes Andrade |
| Centro Médico Bem Viver | CLINICO (médico - MED_ENDOCRINOLOGIA) | `17201099825` | Dra. Simone Lima Soares |
| Centro Médico Bem Viver | CLINICO (dentista - PERIO) | `43462797751` | Dra. Priscila Soares Ferreira |
| Consultório Saúde & Sorriso | GESTOR (dono, login=CNPJ) | `05109027000130` | Caio Gomes Ramos |
| Consultório Saúde & Sorriso | ASSISTENTE (secretária) | `65496862884` | Bruna Carvalho Nunes |
| Consultório Saúde & Sorriso | CLINICO (médico - MED_CLINICA_MEDICA) | `38927667972` | Dra. Ana Souza Martins |
| Consultório Saúde & Sorriso | CLINICO (dentista - GERAL) | `86977163496` | Dra. Cecília Andrade Almeida |
| Clínica Integrada Horizonte | GESTOR (dono, login=CNPJ) | `88783590000117` | Amanda Araújo Ribeiro |
| Clínica Integrada Horizonte | GESTOR | `05591687997` | André Moura Oliveira |
| Clínica Integrada Horizonte | ASSISTENTE (secretária) | `31784952079` | Aline Souza Teixeira |
| Clínica Integrada Horizonte | ASSISTENTE (secretária) | `93919873548` | Simone Martins Lopes |
| Clínica Integrada Horizonte | ASSISTENTE (secretária) | `08187851287` | Sofia Alves Santos |
| Clínica Integrada Horizonte | CLINICO (médico - MED_OTORRINOLARINGOLOGIA) | `92107619956` | Dra. Patrícia Dias Teixeira |
| Clínica Integrada Horizonte | CLINICO (médico - DERMA) | `56592940816` | Dr. Bruno Gonçalves Martins |
| Clínica Integrada Horizonte | CLINICO (dentista - ORTODONT) | `07955372557` | Dr. Rodrigo Rodrigues Nunes |
| Clínica Integrada Horizonte | CLINICO (dentista - PROT) | `32094147932` | Dr. José Nunes Freitas |
| Clínica Integrada Horizonte | CLINICO (dentista - ENDO) | `02669996540` | Dr. Sérgio Machado Cardoso |
| Policlínica Mar Azul | GESTOR (dono, login=CNPJ) | `08747359000139` | Fernanda Machado Barbosa |
| Policlínica Mar Azul | GESTOR | `12363607538` | Sofia Alves Carvalho |
| Policlínica Mar Azul | GESTOR | `82021704009` | José Oliveira Moreira |
| Policlínica Mar Azul | ASSISTENTE (secretária) | `62706397519` | Otávio Dias Santos |
| Policlínica Mar Azul | ASSISTENTE (secretária) | `76462870168` | Priscila Souza Gonçalves |
| Policlínica Mar Azul | ASSISTENTE (secretária) | `02401323600` | Valentina Pereira Ramos |
| Policlínica Mar Azul | CLINICO (médico - CARDIO) | `14045703306` | Dr. Leonardo Carvalho Pinto |
| Policlínica Mar Azul | CLINICO (médico - PED) | `52266701924` | Dra. Laura Araújo Lopes |
| Policlínica Mar Azul | CLINICO (médico - ORTO) | `18106748405` | Dra. Carolina Vieira Machado |
| Policlínica Mar Azul | CLINICO (médico - MED_CLINICA_MEDICA) | `09523085760` | Dr. Caio Souza Batista |
| Policlínica Mar Azul | CLINICO (dentista - IMPL) | `71152907336` | Dra. Larissa Gonçalves Cardoso |
| Policlínica Mar Azul | CLINICO (dentista - GERAL) | `51446956989` | Dr. Diego Silva Marques |

## Limitações conhecidas (encontradas na verificação, são da aplicação e não da massa)

- `GET /prontuario/paciente/{id}` (histórico do médico) e `GET /prontuario-dentista/consulta/{id}`
  respondem **500**. O erro é `LazyInitializationException`: a entidade é serializada com `open-in-view=false`.
  As outras rotas do prontuário, como `/consulta/{id}/recente`, questionário, planejamento e
  `/prontuario-dentista/paciente/{id}`, respondem 200.
- `GET /planejamento-terapeutico/prontuario/{id}` procura o id primeiro como prontuário de dentista. Por isso
  a massa numera `prontuario_dentista` a partir de 100001, para não colidir com os ids de `prontuario`.
- `paciente.PaciSexo` é gravado como `1`/`2`, que é o que a tela de cadastro envia. A tela de edição
  espera "Masculino"/"Feminino".
