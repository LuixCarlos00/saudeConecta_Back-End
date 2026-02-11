-- ==========================================
-- SCRIPT PARA POPULAR BANCO MULTI-TENANT
-- Execute no MySQL: source insert-dados-multi-tenant.sql
-- Senha dos usuarios: 123456 (BCrypt hash)
-- ==========================================
-- ESTRUTURA:
-- - 2 organizações (Clínica e Consultório)
-- - Usuários com organizacao_id
-- - Profissionais (Médicos e Dentistas)
-- - Pacientes
-- - Consultas v2
-- ==========================================

USE office;

-- ==========================================
-- TABELA: organizacao (2 organizações)
-- ==========================================
INSERT INTO organizacao (nome, razao_social, cnpj, tipo, telefone, email, status, created_at) VALUES
('Clínica Saúde Total', 'Clínica Saúde Total LTDA', '12.345.678/0001-90', 'CLINICA', '(11) 3333-4444', 'contato@saudetotal.com.br', 'ATIVA', NOW()),
('Consultório Dr. Silva', 'Carlos Silva Consultório Médico ME', '98.765.432/0001-10', 'CONSULTORIO', '(11) 5555-6666', 'contato@drsilva.com.br', 'ATIVA', NOW());

-- ==========================================
-- TABELA: endereco (10 endereços)
-- ==========================================
INSERT INTO endereco (EndNacionalidade, EndUF, EndMunicipio, EndBairro, EndCep, EndRua, EndNumero, EndComplemento) VALUES
('Brasileira', 'SP', 'São Paulo', 'Centro', '01001-000', 'Rua da Consolação', 100, 'Sala 101'),
('Brasileira', 'SP', 'São Paulo', 'Jardins', '01402-000', 'Av. Paulista', 200, 'Conj 202'),
('Brasileira', 'SP', 'São Paulo', 'Moema', '04077-000', 'Av. Ibirapuera', 300, ''),
('Brasileira', 'SP', 'São Paulo', 'Pinheiros', '05422-000', 'Rua dos Pinheiros', 400, 'Apto 42'),
('Brasileira', 'SP', 'São Paulo', 'Vila Mariana', '04110-000', 'Rua Domingos de Morais', 500, ''),
('Brasileira', 'SP', 'São Paulo', 'Itaim Bibi', '04538-000', 'Rua Joaquim Floriano', 600, 'Bloco A'),
('Brasileira', 'SP', 'São Paulo', 'Brooklin', '04571-000', 'Av. Engenheiro Luís Carlos', 700, ''),
('Brasileira', 'SP', 'São Paulo', 'Santana', '02012-000', 'Rua Voluntários da Pátria', 800, 'Casa'),
('Brasileira', 'SP', 'São Paulo', 'Tatuapé', '03310-000', 'Rua Serra de Bragança', 900, ''),
('Brasileira', 'SP', 'São Paulo', 'Perdizes', '05005-000', 'Rua Cardoso de Almeida', 1000, 'Apto 101');

-- ==========================================
-- TABELA: usuarios (10 usuários com multi-tenant)
-- Org 1: 1 SuperAdmin, 1 Admin, 2 Profissionais, 1 Recepcionista
-- Org 2: 1 Admin, 2 Profissionais, 1 Recepcionista
-- + 1 SuperAdmin sem org (acesso global)
-- ==========================================
INSERT INTO usuarios (organizacao_id, login, senha, TipoUsuario, tipo_usuario_novo, status) VALUES
-- Super Admin Global (sem organização)
(NULL, 'luiz', '$2y$10$hubYaqDdzJx/4apNtiCoE.jAah1wlFLGJ9PoST1lsbS8u7WwnwGYK', 1, 'SUPER_ADMIN', 'ATIVO'),
-- Organização 1 - Clínica Saúde Total
(1, 'adm', '$2y$10$hubYaqDdzJx/4apNtiCoE.jAah1wlFLGJ9PoST1lsbS8u7WwnwGYK', 1, 'ADMIN_ORG', 'ATIVO'),
(1, 'med', '$2y$10$hubYaqDdzJx/4apNtiCoE.jAah1wlFLGJ9PoST1lsbS8u7WwnwGYK', 3, 'PROFISSIONAL', 'ATIVO'),
(1, 'dra.maria@saudetotal.com.br', '$2y$10$hubYaqDdzJx/4apNtiCoE.jAah1wlFLGJ9PoST1lsbS8u7WwnwGYK', 3, 'PROFISSIONAL', 'ATIVO'),
(1, 'recepcao@saudetotal.com.br', '$2y$10$hubYaqDdzJx/4apNtiCoE.jAah1wlFLGJ9PoST1lsbS8u7WwnwGYK', 2, 'RECEPCIONISTA', 'ATIVO'),
-- Organização 2 - Consultório Dr. Silva
(2, 'admin@drsilva.com.br', '$2y$10$hubYaqDdzJx/4apNtiCoE.jAah1wlFLGJ9PoST1lsbS8u7WwnwGYK', 1, 'ADMIN_ORG', 'ATIVO'),
(2, 'dr.pedro@drsilva.com.br', '$2y$10$hubYaqDdzJx/4apNtiCoE.jAah1wlFLGJ9PoST1lsbS8u7WwnwGYK', 3, 'PROFISSIONAL', 'ATIVO'),
(2, 'dra.ana@drsilva.com.br', '$2y$10$hubYaqDdzJx/4apNtiCoE.jAah1wlFLGJ9PoST1lsbS8u7WwnwGYK', 3, 'PROFISSIONAL', 'ATIVO'),
(2, 'atendimento@drsilva.com.br', '$2y$10$hubYaqDdzJx/4apNtiCoE.jAah1wlFLGJ9PoST1lsbS8u7WwnwGYK', 2, 'RECEPCIONISTA', 'ATIVO');

-- ==========================================
-- TABELA: admin_organizacao
-- ==========================================
INSERT INTO admin_organizacao (organizacao_id, usuario_id, nome, cargo, email, is_owner, status, created_at) VALUES
(1, 2, 'João Administrador', 'Diretor Administrativo', 'admin@saudetotal.com.br', 1, 'ATIVO', NOW()),
(2, 6, 'Maria Gestora', 'Gerente', 'admin@drsilva.com.br', 1, 'ATIVO', NOW());

-- ==========================================
-- TABELA: profissional
-- Org 1: 2 médicos (Cardiologia, Dermatologia)
-- Org 2: 1 médico + 1 dentista
-- ==========================================
INSERT INTO profissional (organizacao_id, tipo_profissional_id, nome, sexo, data_nascimento, registro_conselho, cpf, email, telefone, formacao, instituicao, tempo_consulta_minutos, usuario_id, endereco_id, status, created_at, updated_at) VALUES
-- Org 1 - Clínica Saúde Total
(1, 1, 'Dr. Carlos Alberto Silva', 'MASCULINO', '1975-03-15', '123456', '11122233344', 'dr.carlos@saudetotal.com.br', '(11) 99999-0001', 'Medicina - Cardiologia', 'USP', 30, 3, 1, 'ATIVO', NOW(), NOW()),
(1, 1, 'Dra. Maria Fernanda Costa', 'FEMININO', '1980-07-22', '234567', '22233344455', 'dra.maria@saudetotal.com.br', '(11) 99999-0002', 'Medicina - Dermatologia', 'UNICAMP', 20, 4, 2, 'ATIVO', NOW(), NOW()),
-- Org 2 - Consultório Dr. Silva
(2, 1, 'Dr. Pedro Santos', 'MASCULINO', '1978-11-08', '345678', '33344455566', 'dr.pedro@drsilva.com.br', '(11) 99999-0003', 'Medicina - Ortopedia', 'UFRJ', 40, 7, 3, 'ATIVO', NOW(), NOW()),
(2, 2, 'Dra. Ana Paula Oliveira', 'FEMININO', '1982-05-30', '456789', '44455566677', 'dra.ana@drsilva.com.br', '(11) 99999-0004', 'Odontologia - Ortodontia', 'FOUSP', 45, 8, 4, 'ATIVO', NOW(), NOW());

-- ==========================================
-- TABELA: profissional_especialidade (N:N)
-- Especialidades Médicas: 1-Cardiologia, 2-Dermatologia, 3-Ortopedia, 4-Pediatria, 5-Ginecologia, 6-Neurologia, 7-Psiquiatria, 8-Oftalmologia, 9-Urologia, 10-Gastroenterologia
-- Especialidades Odonto: 11-Ortodontia, 12-Endodontia, 13-Periodontia, 14-Implantodontia, 15-Odontopediatria, 16-Cirurgia Buco, 17-Prótese, 18-Clínico Geral
-- ==========================================
INSERT INTO profissional_especialidade (profissional_id, especialidade_id, principal) VALUES
-- Dr. Carlos (Cardiologista) - especialidade 1 (Cardiologia)
(1, 1, 1),
-- Dra. Maria (Dermatologista) - especialidade 2 (Dermatologia)
(2, 2, 1),
-- Dr. Pedro (Ortopedista) - especialidade 3 (Ortopedia)
(3, 3, 1),
-- Dra. Ana (Dentista/Ortodontista) - especialidade 11 (Ortodontia)
(4, 11, 1);

-- ==========================================
-- TABELA: paciente (10 pacientes)
-- Pacientes 1-5: Organização 1 (Clínica Saúde Total)
-- Pacientes 6-10: Organização 2 (Consultório Dr. Silva)
-- ==========================================
INSERT INTO paciente (organizacao_id, PaciNome, PaciSexo, PaciDataNacimento, PaciCpf, PaciRg, PaciEmail, PaciTelefone, Endereco, PaciStatus) VALUES
-- Org 1 - Clínica Saúde Total
(1, 'José da Silva', 'Masculino', '1985-05-10', '123.456.789-00', '12.345.678-X', 'jose.silva@email.com', '(11) 98888-0001', 5, 'ATIVO'),
(1, 'Maria Santos', 'Feminino', '1990-08-15', '234.567.890-11', '23.456.789-1', 'maria.santos@email.com', '(11) 98888-0002', 6, 'ATIVO'),
(1, 'Pedro Oliveira', 'Masculino', '1978-03-22', '345.678.901-22', '34.567.890-2', 'pedro.oliveira@email.com', '(11) 98888-0003', 7, 'ATIVO'),
(1, 'Ana Costa', 'Feminino', '1995-11-30', '456.789.012-33', '45.678.901-3', 'ana.costa@email.com', '(11) 98888-0004', 8, 'ATIVO'),
(1, 'Carlos Ferreira', 'Masculino', '1982-07-18', '567.890.123-44', '56.789.012-4', 'carlos.ferreira@email.com', '(11) 98888-0005', 9, 'ATIVO'),
-- Org 2 - Consultório Dr. Silva
(2, 'Juliana Lima', 'Feminino', '1988-01-25', '678.901.234-55', '67.890.123-5', 'juliana.lima@email.com', '(11) 98888-0006', 10, 'ATIVO'),
(2, 'Roberto Almeida', 'Masculino', '1970-09-12', '789.012.345-66', '78.901.234-6', 'roberto.almeida@email.com', '(11) 98888-0007', 5, 'ATIVO'),
(2, 'Fernanda Rocha', 'Feminino', '1992-04-08', '890.123.456-77', '89.012.345-7', 'fernanda.rocha@email.com', '(11) 98888-0008', 6, 'ATIVO'),
(2, 'Marcos Souza', 'Masculino', '1975-12-03', '901.234.567-88', '90.123.456-8', 'marcos.souza@email.com', '(11) 98888-0009', 7, 'ATIVO'),
(2, 'Camila Pereira', 'Feminino', '1998-06-20', '012.345.678-99', '01.234.567-9', 'camila.pereira@email.com', '(11) 98888-0010', 8, 'ATIVO');

-- ==========================================
-- TABELA: consulta (15 consultas)
-- Distribuídas entre as 2 organizações
-- ==========================================
INSERT INTO consulta (organizacao_id, profissional_id, paciente_id, especialidade_id, data_hora, duracao_minutos, observacoes, forma_pagamento_id, valor, status, criado_por, created_at, updated_at) VALUES
-- Org 1 - Clínica Saúde Total (prof 1=Cardio, prof 2=Dermato - pacientes 1-5)
-- Especialidades: 1=Cardiologia, 2=Dermatologia
(1, 1, 1, 1, '2026-02-05 08:00:00', 30, 'Consulta de rotina cardiológica', 1, 250.00, 'AGENDADA', 5, NOW(), NOW()),
(1, 1, 2, 1, '2026-02-05 08:30:00', 30, 'Avaliação de pressão arterial', 2, 250.00, 'AGENDADA', 5, NOW(), NOW()),
(1, 1, 3, 1, '2026-02-05 09:00:00', 30, 'Retorno - arritmia', 1, 200.00, 'CONFIRMADA', 5, NOW(), NOW()),
(1, 2, 4, 2, '2026-02-05 09:00:00', 20, 'Avaliação dermatológica', 3, 180.00, 'AGENDADA', 5, NOW(), NOW()),
(1, 2, 5, 2, '2026-02-05 09:20:00', 20, 'Procedimento estético', 1, 350.00, 'CONFIRMADA', 5, NOW(), NOW()),
(1, 1, 1, 1, '2026-02-06 08:00:00', 30, 'Retorno cardiologia', 1, 200.00, 'AGENDADA', 2, NOW(), NOW()),
(1, 2, 2, 2, '2026-02-06 09:00:00', 20, 'Manchas na pele', 2, 180.00, 'AGENDADA', 2, NOW(), NOW()),
-- Consultas passadas (realizadas/canceladas)
(1, 1, 3, 1, '2026-02-01 08:00:00', 30, 'Check-up cardíaco anual', 1, 250.00, 'REALIZADA', 5, '2026-01-25 10:00:00', NOW()),
(1, 2, 4, 2, '2026-02-01 09:00:00', 20, 'Acne facial', 3, 180.00, 'REALIZADA', 5, '2026-01-25 11:00:00', NOW()),
(1, 1, 5, 1, '2026-02-02 08:00:00', 30, 'Dor no peito - URGENTE', 1, 300.00, 'CANCELADA', 5, '2026-01-26 09:00:00', NOW()),
-- Org 2 - Consultório Dr. Silva (prof 3=Ortopedia, prof 4=Ortodontia - pacientes 6-10)
-- Especialidades: 3=Ortopedia, 11=Ortodontia
(2, 3, 6, 3, '2026-02-05 10:00:00', 40, 'Dor no joelho', 1, 200.00, 'AGENDADA', 9, NOW(), NOW()),
(2, 3, 7, 3, '2026-02-05 10:40:00', 40, 'Lesão esportiva', 2, 200.00, 'CONFIRMADA', 9, NOW(), NOW()),
(2, 4, 8, 11, '2026-02-05 14:00:00', 45, 'Limpeza dental', 1, 150.00, 'AGENDADA', 9, NOW(), NOW()),
(2, 4, 9, 11, '2026-02-05 14:45:00', 45, 'Avaliação ortodôntica', 3, 200.00, 'AGENDADA', 9, NOW(), NOW()),
(2, 3, 10, 3, '2026-02-06 10:00:00', 40, 'Retorno ortopedia', 1, 180.00, 'AGENDADA', 9, NOW(), NOW());

-- ==========================================
-- TABELA: consulta_historico
-- ==========================================
INSERT INTO consulta_historico (consulta_id, status_anterior, status_novo, observacao, alterado_por, created_at) VALUES
-- Histórico das consultas confirmadas
(3, 'AGENDADA', 'CONFIRMADA', 'Paciente confirmou presença', 5, NOW()),
(5, 'AGENDADA', 'CONFIRMADA', 'Confirmação por telefone', 5, NOW()),
(12, 'AGENDADA', 'CONFIRMADA', 'Paciente confirmou via WhatsApp', 9, NOW()),
-- Histórico das consultas realizadas
(8, 'AGENDADA', 'CONFIRMADA', 'Paciente confirmou', 5, '2026-01-30 10:00:00'),
(8, 'CONFIRMADA', 'EM_ANDAMENTO', 'Consulta iniciada', 3, '2026-02-01 08:00:00'),
(8, 'EM_ANDAMENTO', 'REALIZADA', 'Consulta finalizada - paciente orientado', 3, '2026-02-01 08:30:00'),
(9, 'AGENDADA', 'CONFIRMADA', 'Confirmação recebida', 5, '2026-01-30 11:00:00'),
(9, 'CONFIRMADA', 'REALIZADA', 'Tratamento iniciado', 4, '2026-02-01 09:20:00'),
-- Histórico da consulta cancelada
(10, 'AGENDADA', 'CANCELADA', 'Paciente solicitou cancelamento - viagem', 5, '2026-02-01 07:00:00');

-- ==========================================
-- ATUALIZAR a consulta cancelada com motivo
-- ==========================================
UPDATE consulta SET cancelado_por = 'PACIENTE', motivo_cancelamento = 'Viagem de emergência' WHERE id = 10;

-- ==========================================
-- FIM DO SCRIPT
-- ==========================================
SELECT '========================================' AS '';
SELECT 'DADOS MULTI-TENANT INSERIDOS COM SUCESSO!' AS Resultado;
SELECT '========================================' AS '';
SELECT 'Organizações: 2' AS Tabela;
SELECT 'Endereços: 10' AS Tabela;
SELECT 'Usuários: 9 (1 SuperAdmin + 4 Org1 + 4 Org2)' AS Tabela;
SELECT 'Admin Organizações: 2' AS Tabela;
SELECT 'Profissionais: 4 (2 médicos + 1 ortopedista + 1 dentista)' AS Tabela;
SELECT 'Pacientes: 10' AS Tabela;
SELECT 'Consultas v2: 15' AS Tabela;
SELECT 'Histórico Consultas: 9' AS Tabela;
SELECT '========================================' AS '';
SELECT 'CREDENCIAIS DE TESTE:' AS '';
SELECT 'Super Admin: superadmin@saudeconecta.com / 123' AS '';
SELECT 'Admin adm: admin@saudetotal.com.br / 123' AS '';
SELECT 'Admin Org2: admin@drsilva.com.br / 123' AS '';
SELECT '========================================' AS '';
