# Integração Mercado Pago — Assinaturas recorrentes

Este documento descreve a configuração necessária para o fluxo de assinatura
automática por cartão que substituiu o Pix estático.

## 1. Visão geral do fluxo

```
Cadastro público (landing)  →  Org + Admin + AssinaturaTenant(TRIAL, 30 dias)   [sem cartão]
                                            │
       7 dias antes do fim do trial  →  POST /preapproval {status:"pending"}
                                            │  → init_point (checkout hospedado)
                                            │  → e-mail com o link de ativação
                                            ▼
                         Cliente cadastra o cartão no site do Mercado Pago
                                            │
        webhook subscription_preapproval (authorized)  → assinatura ATIVA
        webhook subscription_authorized_payment        → CobrancaTenant PAGO / RECUSADO
                                            │
                       O Mercado Pago cobra sozinho todo mês
```

Cancelamento: `PUT /preapproval/{id} {"status":"cancelled"}`, disparado pelo próprio
cliente em **Minha Assinatura** ou pelo SUPER_ADMIN.

Telas do SUPER_ADMIN (rotas, permissão e erros em `CONTRATO-ADMIN-ASSINATURAS.md`, na pasta acima
do repositório):

- **Suspender** (`PATCH /assinaturas/{id}/suspender`): com a linha travada, grava `SUSPENSA` e pausa o
  preapproval no MP na mesma transação (como a régua). Falha do MP não desfaz a suspensão local.
  Assinatura `CANCELADA` não é suspensa (`409 ASSINATURA_CANCELADA`).
- **Cancelar** (`PATCH /assinaturas/{id}/cancelar`): com a linha travada, cancela no MP e depois local
  (`canceladoPor = SUPER_ADMIN`). Se o MP falhar, nada muda (502).
- **Associar plano** (`POST /assinaturas`): sem assinatura ativa cria `TRIAL`; com assinatura ativa troca
  o plano (propaga o valor ao MP, `502 MP_VALOR_NAO_ATUALIZADO`). Clínica com assinatura `SUSPENSA`
  vigente recebe `409 ASSINATURA_SUSPENSA`: para liberar, cortesia ou cancelar antes.
- **Resolver pendência** (`PATCH /assinaturas/pendencias-manuais/{id}/resolver`): só registra quem,
  quando e a observação (changeset 068) e tira a pendência da lista e da contagem do dashboard. **Não
  altera nada no MP**: o estorno ou cancelamento é feito no painel do MP. A reentrega da mesma
  notificação não reabre a pendência resolvida.

## 2. Criação da aplicação (ação manual)

1. Criar conta vendedor em <https://www.mercadopago.com.br>.
2. Em **Suas integrações** → **Criar aplicação**, escolher o produto
   *Plans and Subscriptions (Assinaturas)*.
3. Copiar `Access Token` e `Public Key` — comece pelas **credenciais de teste**.
4. Criar 2 usuários de teste (vendedor e comprador) em
   <https://www.mercadopago.com.br/developers/pt/docs/your-integrations/test/accounts>
   e usar os cartões de teste da documentação.
5. Em **Webhooks → Configurar notificações**, cadastrar a URL e marcar os tópicos
   `subscription_preapproval` e `subscription_authorized_payment`.
   Guardar o **secret** gerado — ele valida a assinatura HMAC das notificações.
   - Desenvolvimento: `ngrok http 8080` e usar a URL pública gerada.
   - Produção: `https://saudeconectaback-end-production.up.railway.app/publico/webhooks/mercadopago`

## 3. Variáveis de ambiente

Nunca versione credenciais. Os arquivos `application*.properties` apenas leem
as variáveis abaixo:

| Variável | Descrição |
|---|---|
| `MP_ACCESS_TOKEN` | Access Token da aplicação |
| `MP_PUBLIC_KEY` | Public Key da aplicação |
| `MP_WEBHOOK_SECRET` | Secret da assinatura HMAC dos webhooks |
| `MP_BACK_URL` | URL **pública** de retorno do checkout |

Sem `MP_ACCESS_TOKEN` ou `MP_BACK_URL` a API responde `503` nas rotas que dependem
do Mercado Pago — o restante do sistema continua funcionando normalmente.

### `MP_BACK_URL` não aceita localhost

Validado contra a API real: `http://localhost:4200/...` e `https://localhost:4200/...`
são recusados com `400 Invalid value for back_url, must be a valid URL`. É preciso
uma URL pública. Em desenvolvimento, use o ngrok:

```bash
ngrok http 4200
export MP_BACK_URL=https://<subdominio>.ngrok-free.app/assinatura/sucesso
```

Como o frontend usa `useHash: true` nas rotas, o caminho real é
`https://.../#/assinatura/sucesso`. O Mercado Pago aceita a URL com fragmento,
mas preserve o `#` apenas se o teste confirmar o redirecionamento correto —
alternativamente aponte para a raiz e trate o redirecionamento no frontend.

## 4. Assinatura sem plano associado — cuidado com o `payer_email`

A integração usa `/preapproval` **sem plano associado**, o que permite valor
customizado por tenant. A documentação oficial alerta:

> *"For subscriptions without an associated plan, we validate during the payment
> process that the entered email matches the payer's email. If the email addresses
> don't match, the payment will be rejected."*

Ou seja: o `payer_email` enviado (o e-mail do administrador owner da organização)
precisa ser o e-mail da conta Mercado Pago de quem paga. A tela de ativação e o
e-mail de ativação exibem esse aviso.

Se isso gerar suporte demais, a coluna `assinatura_tenant.mp_payer_email` já existe
e pode passar a ser editável pelo cliente.

## 5. Validação de assinatura do webhook

Header `x-signature`: `ts=1704908010,v1=618c8534...`

Manifest assinado com HMAC-SHA256 usando `MP_WEBHOOK_SECRET`:

```
id:{data.id};request-id:{x-request-id};ts:{ts};
```

A comparação com `v1` é *time-safe* (`MessageDigest.isEqual`) e timestamps com mais
de 5 minutos de diferença são rejeitados (mitigação de replay). Requisições sem
assinatura válida recebem `401`.

Implementação: `application/assinatura/MercadoPagoSignatureValidator`.

## 6. Idempotência

Toda notificação é gravada em `mp_webhook_evento` com `UNIQUE (tipo, data_id)`
antes de qualquer efeito colateral. O Mercado Pago reenvia a mesma notificação
até receber um `2xx`, então eventos repetidos são descartados.

O endpoint responde `200` imediatamente e processa em `@Async` — o Mercado Pago
exige resposta em até 22 segundos.

## 7. Mapeamento de status

| Evento Mercado Pago | Efeito local |
|---|---|
| `subscription_preapproval` → `authorized` | `AssinaturaTenant.status = ATIVA`, renova vencimento |
| `subscription_preapproval` → `cancelled` | `status = CANCELADA`, `canceladoPor = MERCADO_PAGO` |
| `subscription_preapproval` → `paused` | `status = SUSPENSA` |
| `subscription_authorized_payment` → `approved` | `CobrancaTenant` PAGO + `renovarAposPagamento` |
| `subscription_authorized_payment` → `rejected` | `CobrancaTenant` RECUSADO + `status = INADIMPLENTE` |

### Troca de preapproval (link novo) em duas fases

Gerar um link novo quando já existe um preapproval (A) que não serve mais:

1. Com a linha da assinatura travada (`SELECT ... FOR UPDATE`), cria o preapproval
   novo (B) no MP, grava `mp_preapproval_id = B` e guarda A em
   `mp_preapproval_substituido_id`. Commit. Se o POST de B falhar, nada muda e A
   segue intacto.
   Se der erro depois de B criado no MP, B é cancelado (compensação), mas só depois de reler
   a assinatura numa transação nova e confirmar que B **não** ficou vinculado (o erro pode vir
   da confirmação de um commit que gravou). Se nem a releitura funcionar, B não é cancelado e
   o log ERROR pede conferência manual.
2. Depois do commit, cancela A no MP e limpa `mp_preapproval_substituido_id`. Se o MP
   falhar, a coluna fica preenchida e a reconciliação (4 h) tenta de novo.

**A não é cancelado às cegas.** Antes de cancelar, A é consultado no MP:

| A no MP | O que acontece |
|---|---|
| `cancelled` ou não encontrado | Só limpa a coluna |
| `authorized`, mas não estava com cartão (`authorized`/`paused`) no momento da troca — ex.: o cliente terminou o checkout do link abandonado | **Pendência manual** `PREAPPROVAL_SUBSTITUIDO_AUTORIZADO`, log WARN, A não é cancelado |
| Fatura aprovada no MP sem cobrança `PAGO` local | **Pendência manual** `PREAPPROVAL_SUBSTITUIDO_AUTORIZADO` |
| Outros casos (inclusive `authorized` já conhecido na troca: cortesia, troca de cartão) | Cancela normalmente |

Na nova tentativa da reconciliação o status na troca é desconhecido, então um `authorized`
vai para pendência. A pendência é gravada uma vez por preapproval em `mp_webhook_evento`
(`tipo = pendencia_preapproval_substituido`, `data_id` = A) e aparece em
`/assinaturas/pendencias-manuais`. A coluna de substituído continua preenchida, então os
webhooks de A seguem ignorados. Para resolver, o SUPER_ADMIN estorna/cancela A no painel do
MP (a reconciliação vê `cancelled` e limpa a coluna) ou religa manualmente.

Enquanto houver um substituído em pendência, uma troca nova que precisaria da coluna (link
novo, inclusive sem preapproval vinculado, cortesia, troca de plano com link pendente) é
recusada com `409` e `code = PREAPPROVAL_SUBSTITUIDO_EM_ANALISE`: no caso do link abandonado,
gerar outro levaria o cliente a pagar duas vezes.

O webhook de A (inclusive `cancelled`) é sempre ignorado: ou a assinatura está
vinculada a B (`OUTRO_PREAPPROVAL_VINCULADO`), ou A está na coluna de substituído
(`PREAPPROVAL_SUBSTITUIDO`), ou não há preapproval vinculado e A está cancelado
(`CANCELADO_SEM_VINCULO`).

### `FOR UPDATE` segurado durante chamadas HTTP

Algumas operações seguram o lock da linha de `assinatura_tenant` (`SELECT ... FOR UPDATE`)
enquanto chamam o MP, de propósito: é o que serializa a troca de link, a troca de plano, a
cortesia, a régua e a reconciliação sobre a mesma assinatura.

| Operação | Chamadas ao MP com a linha travada |
|---|---|
| Link novo (fase 1) | `GET` + `PUT` do substituído anterior (só se ainda pendente), `POST /preapproval` |
| Troca de plano / plano personalizado | `GET /preapproval/{id}` e `PUT` do valor; na falha, mais um `GET` de releitura |
| Cortesia | `GET` do `next_payment_date` (ao remover); `GET` + `PUT` do substituído anterior (só se pendente) |
| Régua (suspender/cancelar) e reconciliação | `PUT` de pausa/cancelamento; `GET /preapproval/{id}` e, se o valor divergir, `PUT` do valor |

Cada chamada é limitada por `HTTP_CONNECT_TIMEOUT` (5 s) e `HTTP_READ_TIMEOUT` (15 s); no pior
caso a linha fica travada por algumas dezenas de segundos. Enquanto isso, quem precisa da
mesma linha (webhook da mesma assinatura, outra troca) espera até o
`innodb_lock_wait_timeout` do MySQL (50 s por padrão) e, se estourar, falha: o webhook é
reprocessado com backoff e as telas recebem erro e podem tentar de novo. Outras assinaturas
não são afetadas. Cada transação trava uma única linha de `assinatura_tenant`, como a régua,
então não há ordem de locks que possa formar deadlock entre elas.

**Timeout menor só nessas chamadas: avaliado e não feito.** Os timeouts vêm do
`SimpleClientHttpRequestFactory` do único `RestTemplate` da aplicação (`RestTemplateConfig`),
compartilhado com o webhook, o CEP e o restante do `MercadoPagoClient`. O Spring 6.0 não tem
timeout por requisição no `RestTemplate`; baixar para ~8 s só em criar/buscar preapproval e
atualizar valor exigiria um segundo `RestTemplate` dentro do `MercadoPagoClient`, e o
`buscarPreapproval` também é usado fora do lock (webhook). Se o tempo de lock virar problema,
o caminho é esse segundo cliente, ou baixar `HTTP_READ_TIMEOUT` para toda a aplicação.

### Troca de plano e plano personalizado

`valor_mensal` = preço do plano + extras × valor adicional de cada perfil. Na troca de
plano os limites personalizados continuam e os extras são recalculados contra o plano novo.
O valor novo vale **a partir do próximo ciclo**, sem cobrança proporcional. Com a linha
travada, o banco é gravado e então o valor vai ao MP, conforme o preapproval:

| Preapproval no MP | O que acontece |
|---|---|
| `authorized` ou `paused` | `PUT /preapproval/{id}` com o valor novo. Se o PUT falhar ou estourar o timeout, o preapproval é relido: se `auto_recurring.transaction_amount` já é o valor novo, a troca segue (o MP aplicou e a resposta se perdeu); se não, a troca é desfeita com `502` e `code = MP_VALOR_NAO_ATUALIZADO`. Se nem a releitura responder, também `502` (log `motivo=VALOR_INCERTO`) e a reconciliação acerta o MP pelo banco |
| `pending` | O link é abandonado (o checkout mostraria o valor antigo): desvinculado e cancelado depois do commit. O próximo "Pagar" gera outro com o valor novo |
| nenhum ou `cancelled` | Só o banco |

### Dia âncora do ciclo (`dia_cobranca`)

O dia âncora é fixado no primeiro pagamento aprovado e preserva o dia nos meses curtos.
Quando um preapproval novo é vinculado (link novo), `dia_cobranca` volta a `NULL`: o MP
cobra o preapproval novo a partir do dia em que o cartão foi cadastrado, então o próximo
pagamento aprovado define a âncora nova (ex.: âncora 5, link novo pago no dia 22 → âncora 22).

O `next_payment_date` do MP **não** é usado para recalcular o vencimento depois de cada
fatura paga: exigiria um GET extra dentro da transação do webhook, o campo vem com fuso
do MP (a conversão está pendente) e, durante as novas tentativas de uma fatura recusada,
ele aponta para a próxima tentativa e não para o próximo ciclo. Ele só é usado ao remover
a cortesia de quem já tem cartão autorizado.

## 8. Rotinas automáticas (`CobrancaScheduler`)

| Cron | Ação |
|---|---|
| `0 0 8 * * *` | Trials vencendo em 7 dias → cria preapproval pendente e envia o link por e-mail |
| `0 0 9 * * *` | Régua de carência: +5d → INADIMPLENTE, +15d → SUSPENSA (pausa no MP), +30d → CANCELADA |
| `0 0 */4 * * *` | Reconciliação com o Mercado Pago (rede de segurança para webhooks perdidos), correção do valor do preapproval (`authorized`/`paused`) que diverge de `valor_mensal` (o banco vale; log WARN `motivo=VALOR_DIVERGENTE`) e nova tentativa de cancelar preapprovals substituídos (`mp_preapproval_substituido_id` preenchido) |

Assinaturas com `cortesia = true` são ignoradas por todas as rotinas (régua, links,
reconciliação e recuperação de faturas), com uma exceção: a reconciliação busca as cortesias
que ainda têm `mp_preapproval_id` (dadas antes de a cortesia desvincular o preapproval, que
seguiriam cobrando), desvincula o preapproval e o cancela no MP depois do commit, com a mesma
regra da troca de preapproval (`authorized` ou com fatura aprovada sem registro vira pendência
manual em vez de ser cancelado). Log INFO `assinatura.cortesia.limpeza`. Não precisa de script.

**Dar cortesia** leva a assinatura para `ATIVA` (inclusive vinda de `SUSPENSA` ou
`INADIMPLENTE`) e para a cobrança: o preapproval é desvinculado e cancelado no MP depois
do commit (mesmo mecanismo da troca de link). Uma fatura aprovada que ainda chegue para
uma cortesia é registrada como `PAGO`, não renova a assinatura e aparece em
`/assinaturas/pendencias-manuais` (`PAGAMENTO_EM_CORTESIA`). **Remover a cortesia** segue
a decisão fechada: `TRIAL` + 7 dias e link novo.

## 9. Validação manual

```bash
# Criação de preapproval (equivalente ao que o backend faz)
curl -X POST 'https://api.mercadopago.com/preapproval' \
  -H "Authorization: Bearer $MP_ACCESS_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "reason": "Saúde Conecta — Plano Profissional",
    "external_reference": "ASSINATURA-42",
    "payer_email": "test_user_123@testuser.com",
    "auto_recurring": {
      "frequency": 1,
      "frequency_type": "months",
      "transaction_amount": 279.00,
      "currency_id": "BRL"
    },
    "back_url": "https://app.saudeconecta.com.br/#/assinatura/sucesso",
    "status": "pending"
  }'
```

Roteiro com os cartões de teste:

Erros já mapeados nessa chamada:

| Erro | Causa |
|---|---|
| `Invalid value for back_url, must be a valid URL` | `back_url` apontando para localhost |
| `Payer and collector cannot be the same user` | `payer_email` igual à conta vendedor |
| `User bad request` | `payer_email` não corresponde a uma conta Mercado Pago existente |

| Cenário | Resultado esperado |
|---|---|
| Cartão aprovado | Assinatura vira `ATIVA` |
| Recusado por saldo insuficiente | Assinatura `INADIMPLENTE`, cobrança `RECUSADO` |
| Recusado por dados inválidos | Cobrança `RECUSADO` com `motivo_recusa` preenchido |
| Cancelamento pelo cliente | `CANCELADA` no Saúde Conecta e no Mercado Pago |

## 10. Migração do Pix

- `PixServiceEstatico` e a dependência ZXing foram removidos.
- As colunas `pix_copia_cola`, `pix_qrcode_base64` e `data_vencimento_pix` foram
  removidas (changelog `059`).
- A coluna `txid` foi mantida (nullable) para preservar o histórico financeiro das
  cobranças Pix já liquidadas; elas aparecem com `forma_pagamento = 'PIX'`.
- A chave Pix (CPF) que estava versionada foi removida dos `application*.properties`.
  **Ela continua no histórico do git** — considere trocá-la.
