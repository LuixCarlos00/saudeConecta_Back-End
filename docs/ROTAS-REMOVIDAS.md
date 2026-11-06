# Rotas removidas (fase B2 do plano de arquitetura)

> **Data:** 06/11/2026 · **Branch:** `Assinatura-mercado-pago` · **Origem:** item B-R26 do `PLANO-ARQUITETURA-SAUDECONECTA.md`.
> **Decisão do Luiz (05/11):** apagar já as rotas sem cliente, sem esperar o log de produção, exceto as que as telas novas usam.

## Como cada rota foi conferida

Antes de apagar, cada rota passou por três checagens:

1. **Front:** nenhuma URL montada em `saudeConecta-frontend/src` casa verbo + caminho. Todo HTTP do front passa por `services/api/*`, `core/services/auth.service.ts` e `core/services/configuracao-grafico-dashboard.service.ts`, e foi lido o working tree de 06/11, inclusive as telas `editar-prontuario-*`.
2. **Rotas protegidas:** a rota não está nos três contratos (`CONTRATO-ADMIN-ASSINATURAS.md`, `CONTRATO-RASCUNHO-ATENDIMENTO.md`, `CONTRATO-DASHBOARD-PLATAFORMA.md`), não é do webhook do MP, de `/publico/**` nem de `/Home/**`, e não aparece na lista liberada do `FiltroAcesso` nem no `SecurityConfigurations`.
3. **Cascata:** o código que morreu junto foi achado pelo bytecode. A comparação do constant pool (`javap -v`) antes e depois listou cada método de service e de repository que ficou sem nenhuma chamada, e a análise foi repetida até não sobrar nada. Os `@Scheduled` chamam service por código Java, então entram nessa contagem e nenhum método usado por job saiu.

Nenhum changeset do Liquibase nem índice foi alterado. Os comentários dos changesets `041` e `045`, que citam métodos de repository removidos, ficaram como estão, porque changeset aplicado não se edita.

## Rotas removidas (58)

| Verbo | Caminho | Controller | O que saiu em cascata |
|---|---|---|---|
| PUT | `/consultas/concluirConsultabyOrg/{id}` | `ConsultaController` | `ConsultaService.concluirConsultabyOrg` |
| GET | `/consultas/profissional/{profissionalId}` | `ConsultaController` | `ConsultaService.buscarPorProfissional`, `ConsultaRepository.findByOrganizacao_IdAndProfissional_IdWithRelations` |
| GET | `/consultas/profissional/{profissionalId}/especialidade/intervalo` | `ConsultaController` | `ConsultaService.pesquisarMedicoEspecialidadeEmIntervaloDeDatas`, `ConsultaRepository.findByOrganizacaoIdAndProfissionalIdAndEspecialidadeNomeContainingAndDataHoraBetweenWithRelations` e `...AndStatusOptionalAndDataHoraBetweenWithRelations` |
| GET | `/consultas/profissional/{profissionalId}/especialidade` | `ConsultaController` | `ConsultaService.pesquisarPorProfissionalEEspecialidade`, `ConsultaRepository.findByOrganizacaoIdAndProfissionalIdAndEspecialidadeNomeContainingWithRelations` e `...AndStatusOptionalWithRelations` |
| GET | `/consultas/especialidade/intervalo` | `ConsultaController` | `ConsultaService.pesquisarEspecialidadeEmIntervaloDeDatas`, `ConsultaRepository.findByOrganizacaoIdAndEspecialidadeNomeContainingAndDataHoraBetweenWithRelations` e `...AndStatusOptionalAndDataHoraBetweenWithRelations` |
| GET | `/consultas/especialidade/{especialidade}` | `ConsultaController` | `ConsultaService.pesquisarPorEspecialidade` |
| GET | `/consultas/organizacao/{organizacaoId}/especialidade/{especialidade}` | `ConsultaController` | `ConsultaService.pesquisarPorEspecialidadeEOrganizacao`; com a rota anterior, `ConsultaRepository.findByOrganizacaoIdAndEspecialidadeNomeContainingWithRelations` e `...AndStatusOptionalWithRelations` |
| GET | `/consultas/paginado` | `ConsultaController` | `ConsultaService.buscarTodas`, `ConsultaRepository.findByOrganizacao_Id(Long, Pageable)` |
| GET | `/consultas/profissional/{profissionalId}/agenda` | `ConsultaController` | `ConsultaService.buscarAgendaDia`, `ConsultaRepository.findAgendaDia` |
| GET | `/consultas/periodo` | `ConsultaController` | `ConsultaService.buscarPorPeriodo` |
| GET | `/consultas/paciente/{pacienteId}` | `ConsultaController` | `ConsultaService.buscarPorPaciente`, `ConsultaRepository.findByOrganizacao_IdAndPaciente_PaciCodigoWithRelations` |
| GET | `/consultas/count/hoje` | `ConsultaController` | `ConsultaService.contarAgendadasHoje`, `ConsultaRepository.countAgendadasHoje` |
| GET | `/consultas/estatisticas/organizacao/{organizacaoId}/consultas-hoje` | `ConsultaController` | `ConsultaService.getEstatisticaConsultasHojeByAdmiOrg`, `ConsultaRepository.countByOrganizacao_IdAndDataHoraBetween` |
| GET | `/consultas/estatisticas/organizacao/{organizacaoId}/consultas-atendidas-hoje` | `ConsultaController` | `ConsultaService.getEstatisticaConsultasAtendidasByAdmiOrg` |
| GET | `/consultas/estatisticas/organizacao/{organizacaoId}/consultas-agendadas-hoje` | `ConsultaController` | `ConsultaService.getEstatisticasConsultaAgendadasHojeByOrd`; com a rota anterior, `ConsultaRepository.countByOrganizacao_IdAndStatusAndDataHoraBetween` |
| GET | `/consultas/estatisticas/organizacao/{organizacaoId}/consultas-semana` | `ConsultaController` | `ConsultaService.getEstatisticasSemanaPorOrganizacao`, `ConsultaRepository.countConsultasSemanaByOrganizacao` |
| GET | `/consultas/estatisticas/profissional/{usuarioId}/consultas-hoje` | `ConsultaController` | `ConsultaService.contarConsultasHojePorUsuarioEOrg`, `ConsultaRepository.countConsultasHojePorUsuarioEOrg` |
| GET | `/consultas/estatisticas/profissional/{usuarioId}/consultas-realizadas-hoje` | `ConsultaController` | `ConsultaService.contarConsultasRealizadasHojePorUsuarioEOrg`, `ConsultaRepository.countConsultasRealizadasHojePorUsuarioEOrg` |
| GET | `/consultas/estatisticas/profissional/{usuarioId}/consultas-agendadas-hoje` | `ConsultaController` | `ConsultaService.contarConsultasAgendadasHojePorUsuarioEOrg`, `ConsultaRepository.countConsultasAgendadasHojePorUsuarioEOrg` |
| GET | `/consultas/estatisticas/consultas-hoje` | `ConsultaController` | `ConsultaService.contarTodasConsultasHoje`, `ConsultaRepository.countByDataHoraBetween` |
| GET | `/consultas/estatisticas/consultas-realizadas-hoje` | `ConsultaController` | `ConsultaService.contarTodasConsultasRealizadasHoje` |
| GET | `/consultas/estatisticas/consultas-agendadas-hoje` | `ConsultaController` | `ConsultaService.contarTodasConsultasAgendadasHoje`; com a rota anterior, `ConsultaRepository.countByStatusAndDataHoraBetween` |
| GET | `/consultas/estatisticas/consultas-semana` | `ConsultaController` | `ConsultaService.contarConsultasSemana`, `ConsultaRepository.countConsultasPorMedicoEIntervalo` |
| GET | `/consultas/estatisticas/consultas-semana-global` | `ConsultaController` | `ConsultaService.contarConsultasSemanaGlobal`, `ConsultaRepository.countConsultasSemanaGlobal` |
| GET | `/api/v2/organizacoes/minha` | `OrganizacaoController` (arquivo removido) | `OrganizacaoService` inteiro (arquivo removido), `OrganizacaoResponse`, `CriarOrganizacaoRequest`, `AtualizarOrganizacaoRequest`, `OrganizacaoRepository.findByStatus` e `countByStatus` |
| GET | `/api/v2/organizacoes` | `OrganizacaoController` | idem |
| GET | `/api/v2/organizacoes/ativas` | `OrganizacaoController` | idem |
| GET | `/api/v2/organizacoes/{id}` | `OrganizacaoController` | idem |
| POST | `/api/v2/organizacoes` | `OrganizacaoController` | idem |
| PUT | `/api/v2/organizacoes/{id}` | `OrganizacaoController` | idem |
| PUT | `/api/v2/organizacoes/{id}/ativar` | `OrganizacaoController` | idem |
| PUT | `/api/v2/organizacoes/{id}/inativar` | `OrganizacaoController` | idem |
| GET | `/api/v2/organizacoes/estatisticas/total` | `OrganizacaoController` | idem |
| GET | `/api/v2/tipos-profissional` | `TipoProfissionalController` (arquivo removido) | `EspecialidadeService.listarTiposProfissional`, `TipoProfissionalRepository.findByStatusOrderByNomeAsc`, `TipoProfissionalResponse` |
| GET | `/api/v2/tipos-profissional/{codigo}` | `TipoProfissionalController` | `EspecialidadeService.buscarTipoPorCodigo` |
| GET | `/especialidades/{id}` | `EspecialidadeController` | `EspecialidadeService.buscarPorId` |
| GET | `/especialidades/medicas` | `EspecialidadeController` | `EspecialidadeService.listarMedicas` |
| GET | `/especialidades/odontologicas` | `EspecialidadeController` | `EspecialidadeService.listarOdontologicas`; com a rota anterior, `EspecialidadeService.listarPorTipo` e `EspecialidadeRepository.findAtivasByTipoProfissionalCodigo` |
| GET | `/especialidades/tipo/{codigo}` | `EspecialidadeController` | idem à anterior (`listarPorTipo`) |
| GET | `/especialidades/tipo-id/{tipoId}` | `EspecialidadeController` | `EspecialidadeService.listarPorTipoId`, `EspecialidadeRepository.findByTipoProfissional_IdAndStatus` |
| GET | `/configuracoes-consulta/organizacao/{organizacaoId}` | `ConfiguracoesConsultaController` | nada (o service continua usado por `/atual` e pelo agendamento) |
| POST | `/configuracoes-consulta/organizacao/{organizacaoId}` | `ConfiguracoesConsultaController` | nada (`criarConfiguracao` continua usado internamente) |
| PUT | `/configuracoes-consulta/{id}` | `ConfiguracoesConsultaController` | `ConfiguracoesConsultaService.atualizarConfiguracao`, `ConfiguracoesConsultaRepository.findByIdAndOrganizacaoId` |
| GET | `/configuracoes-consulta/deve-pular-confirmado` | `ConfiguracoesConsultaController` | nada (`devePularParaConfirmado` continua usado pelo agendamento) |
| GET | `/prontuario-dentista/{id}` | `ProntuarioDentistaController` | `ProntuarioDentistaService.buscarPorId` |
| GET | `/prontuario-dentista/consulta/{consultaId}` | `ProntuarioDentistaController` | `ProntuarioDentistaService.listarPorConsulta`, `ProntuarioDentistaRepository.findByConsultaIdAndOrganizacaoId` |
| GET | `/prontuario-dentista/profissional/{profissionalId}` | `ProntuarioDentistaController` | `ProntuarioDentistaService.listarPorProfissional`, `ProntuarioDentistaRepository.findByProfissionalId` |
| GET | `/prontuario-dentista/paciente/{pacienteId}` | `ProntuarioDentistaController` | `ProntuarioDentistaService.listarPorPaciente`, `ProntuarioDentistaRepository.findByPacienteId` |
| GET | `/prontuario/paciente/{pacienteId}` | `ProntuarioController` | `ProntuarioService.buscarPorPaciente`, `ProntuarioRepository.findByPacienteEOrganizacaoComFetch` |
| GET | `/prontuario/profissional/{profissionalId}` | `ProntuarioController` | `ProntuarioService.buscarPorProfissional`, `ProntuarioRepository.findByProfissionalEOrganizacaoComFetch` |
| POST | `/prontuario/cadastrarProntuarioMedicoComPlanejamentos` | `ProntuarioController` | `CadastrarProntuarioComPlanejamentosRequest` (o `cadastrarProntuarioMedico` continua vivo) |
| GET | `/relatorios/pacientes/{pacienteId}` | `RelatorioController` | `RelatorioPacienteService.buscarRelatoriosDoPaciente` |
| GET | `/relatorios/tipos-documento` | `RelatorioController` | o record `RelatorioController.TipoDocumentoResponse` |
| GET | `/mensageria/{id}` | `MensageriaController` | `MensageriaService.buscarPorId` |
| GET | `/mensageria/falhas-pendentes` | `MensageriaController` | `MensageriaService.listarFalhasPendentesNotificacao`, `MensageriaRepository.findByAdminNotificadoFalseAndStatus` e `findByOrganizacao_IdAndAdminNotificadoFalseAndStatus` |
| GET | `/planejamento-terapeutico/prontuario/{prontuarioId}` | `PlanejamentoTerapeuticoController` | `PlanejamentoTerapeuticoService.listarPorProntuario` |
| POST | `/planejamento-terapeutico/profissional/{profissionalId}` | `PlanejamentoTerapeuticoController` | `PlanejamentoTerapeuticoService.adicionar` e `parseData`, mais os 4 repositories que só ele usava no service (`ProntuarioDentista`, `Consulta`, `Profissional`, `Paciente`) |
| DELETE | `/procedimentos-padrao/{id}` | `ProcedimentoPadraoController` | `ProcedimentoPadraoService.desativar` (o front usa `PATCH /{id}/toggle`) |

**Total: 58 rotas** (24 de consulta, 9 de organização, 2 de tipo de profissional, 5 de especialidade, 4 de configuração de consulta, 4 de prontuário odontológico, 3 de prontuário médico, 2 de relatório, 2 de mensageria, 2 de planejamento terapêutico e 1 de procedimento padrão).

**Testes que saíram junto: 121.** São os que só exercitavam o que foi removido: `OrganizacaoControllerIntegrationTest` (18), `OrganizacaoServiceTest` (16), boa parte de `ConsultaServiceTest`, `EspecialidadeServiceTest` e `EspecialidadeControllerIntegrationTest`, e os testes de isolamento por tenant dos métodos removidos em `MensageriaServiceTest`, `ProntuarioServiceTest`, `ProntuarioDentistaServiceTest`, `PlanejamentoTerapeuticoServiceTest`, `ConfiguracoesConsultaServiceTest`, `ConsultaControllerIntegrationTest`, `IsolamentoTenantRepositoryTest` e o passo 3 do `ConsultaFluxoE2ETest`.

Três testes foram ajustados, sem perder cobertura de rota viva:
- Isolamento de `ProcedimentoPadraoService`: continua cobrindo `toggleAtivo` e `atualizar`.
- `ProntuarioServiceTest.semTenantNega`: passou a usar `buscarMaisRecentePorConsulta`.
- `PagamentoAssinaturaFluxoE2ETest`: o bloqueio `PLANO_SEM_ACESSO` passou a ser conferido em `GET /prontuario/buscarProntuarioById/1` e `GET /prontuario/consulta/1/recente`, que são rotas vivas.

## Mantidas por dúvida ou por regra

| Verbo | Caminho | Controller | Por que ficou |
|---|---|---|---|
| GET | `/planos/{id}` | `PlanoAssinaturaController` | Estava na lista B-R26, mas toda rota de `/planos` fica por decisão: as telas novas do SUPER_ADMIN usam esse recurso. O `plano-api.service.ts` do front ainda tem `buscarPorId`. |
| GET | `/planos/tipo/{tipo}` | `PlanoAssinaturaController` | Mesmo motivo; o front tem `buscarPorTipo`. |
| GET | `/cobrancas/pendentes` | `CobrancaController` | B-D2: o par `minhas-cobrancas`/`pendente-atual` está liberado no `FiltroAcesso` para clínica suspensa. Parece funcionalidade planejada (a clínica ver a própria fatura), então as três ficam juntas até a decisão do Luiz. |
| GET | `/cobrancas/minhas-cobrancas` | `CobrancaController` | Liberada no `FiltroAcesso` (B-D2). |
| GET | `/cobrancas/pendente-atual` | `CobrancaController` | Liberada no `FiltroAcesso` (B-D2). |
| POST | `/ia/resumir-historico` | `IAController` | B-D3: sem cliente no front, mas o handoff §7 trata o resumo de IA do back como vivo, e a decisão de manter a regra no back ou no front ainda está em aberto. |
| todas | `/assinaturas/**` | `AssinaturaTenantController` | B-D1: usadas pelo `CONTRATO-ADMIN-ASSINATURAS.md` e pelas telas do SUPER_ADMIN em construção. |
| GET, PATCH | `/cobrancas/organizacao/{id}`, `/cobrancas/{id}/confirmar-pagamento` | `CobrancaController` | Mesmo motivo: o contrato de assinaturas as usa. |

## Achados fora do escopo (não removidos)

O bytecode também mostrou métodos que **já estavam sem chamador antes da B2**: não morreram com as rotas e não estão no plano. Ficaram para uma próxima limpeza, depois de conferidos um a um:
- `ConsultaService.parseStringToDate`, `EspecialidadeService.listarTodas`, `ProntuarioService.buscarPorId`, `MensageriaService.resolverTipoUsuario`;
- `AssinaturaTenantService.ativar`, `cancelar` e `buscarEntidadePorId`;
- `EmailNotificacaoService.enviarCredenciaisPaciente`;
- `ProntuarioRepository.deleteByConsultaId`, `ProntuarioDentistaRepository.deleteByConsultaId`, `SecretariaRepository.findByOrganizacao_Id` e `TermoAutorizacaoRepository.findByConsultaId`;
- `TipoDocumentoRelatorio.getRotulo`, que perdeu o único leitor com a rota `tipos-documento`. Ficou porque é dado do enum de domínio e não custa nada.
