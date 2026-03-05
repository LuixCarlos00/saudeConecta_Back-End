-- ==============================================================================
-- SCRIPT COMPLETO PARA POPULAR BANCO MULTI-TENANT
-- Execute no MySQL: source insert-dados-multi-tenant.sql
-- Senha de todos os usuários: 123456 (BCrypt hash)
-- ==============================================================================
-- ESTRUTURA:
--   2 Organizações (Clínica Saúde Total + Odonto Smile)
--   1 Super Admin global (sem organização)
--   Cada clínica com: Admin, Médico(s), Dentista(s), Secretária
--   12 Pacientes (6 por organização)
--   30 Consultas com DATAS DINÂMICAS (relativas a CURDATE())
--   10 Prontuários médicos + 6 Prontuários odontológicos
--   Planos de assinatura e cobranças
-- ==============================================================================

USE office;

-- ==============================================================================
-- 1. ENDEREÇOS (19 registros)
--    1-2   → Organizações
--    3-7   → Profissionais
--    8-19  → Pacientes
-- ==============================================================================
INSERT INTO endereco (EndNacionalidade, EndUF, EndMunicipio, EndBairro, EndCep, EndRua, EndNumero, EndComplemento) VALUES
('Brasileira', 'SP', 'São Paulo', 'Centro',        '01001-000', 'Rua da Consolação',            100, 'Sala 101'),
('Brasileira', 'RJ', 'Rio de Janeiro', 'Copacabana','22041-080', 'Av. Nossa Sra. de Copacabana', 500, 'Sala 301'),
('Brasileira', 'SP', 'São Paulo', 'Jardins',        '01402-000', 'Av. Paulista',                 200, 'Conj 202'),
('Brasileira', 'SP', 'São Paulo', 'Moema',          '04077-000', 'Av. Ibirapuera',               300, ''),
('Brasileira', 'SP', 'São Paulo', 'Pinheiros',      '05422-000', 'Rua dos Pinheiros',            400, 'Apto 42'),
('Brasileira', 'RJ', 'Rio de Janeiro', 'Botafogo',  '22250-040', 'Rua Voluntários da Pátria',    180, 'Bloco B'),
('Brasileira', 'RJ', 'Rio de Janeiro', 'Leblon',    '22430-010', 'Av. Ataulfo de Paiva',         270, ''),
('Brasileira', 'SP', 'São Paulo', 'Vila Mariana',   '04110-000', 'Rua Domingos de Morais',       500, ''),
('Brasileira', 'SP', 'São Paulo', 'Itaim Bibi',     '04538-000', 'Rua Joaquim Floriano',         600, 'Bloco A'),
('Brasileira', 'SP', 'São Paulo', 'Brooklin',       '04571-000', 'Av. Eng. Luís Carlos Berrini', 700, ''),
('Brasileira', 'SP', 'São Paulo', 'Santana',        '02012-000', 'Rua Voluntários da Pátria',    800, 'Casa'),
('Brasileira', 'SP', 'São Paulo', 'Tatuapé',        '03310-000', 'Rua Serra de Bragança',        900, ''),
('Brasileira', 'SP', 'São Paulo', 'Perdizes',       '05005-000', 'Rua Cardoso de Almeida',      1000, 'Apto 101'),
('Brasileira', 'RJ', 'Rio de Janeiro', 'Tijuca',    '20520-050', 'Rua Conde de Bonfim',          350, ''),
('Brasileira', 'RJ', 'Rio de Janeiro', 'Flamengo',  '22210-030', 'Rua do Catete',                210, 'Apto 502'),
('Brasileira', 'RJ', 'Rio de Janeiro', 'Ipanema',   '22420-020', 'Rua Visconde de Pirajá',       450, ''),
('Brasileira', 'RJ', 'Rio de Janeiro', 'Barra',     '22640-100', 'Av. das Américas',            1200, 'Bloco 3'),
('Brasileira', 'RJ', 'Rio de Janeiro', 'Méier',     '20735-090', 'Rua Dias da Cruz',             320, 'Casa 2'),
('Brasileira', 'RJ', 'Rio de Janeiro', 'Lapa',      '20230-070', 'Rua do Lavradio',               88, '');

-- ==============================================================================
-- 2. ORGANIZAÇÕES (2 registros)
--    Org 1: Clínica Saúde Total (SP) — tipo MISTA (médico + dentista)
--    Org 2: Odonto Smile (RJ)        — tipo MISTA (médico + dentista)
-- ==============================================================================
INSERT INTO organizacao (nome, razao_social, cnpj, tipo, telefone, email, endereco_id, status, created_at, updated_at) VALUES
('Clínica Saúde Total', 'Clínica Saúde Total LTDA',     '12.345.678/0001-90', 'MISTA', '(11) 3333-4444', 'contato@saudetotal.com.br',  1, 'ATIVO', NOW(), NOW()),
('Odonto Smile',        'Odonto Smile Clínica Ltda ME', '98.765.432/0001-10', 'MISTA', '(21) 5555-6666', 'contato@odontosmile.com.br', 2, 'ATIVO', NOW(), NOW());

-- ==============================================================================
-- 3. USUÁRIOS (10 registros)
--    ID 1:  Super Admin global (sem organização)
--    ID 2:  Admin Org 1
--    ID 3:  Médico Org 1 (Dr. Carlos — Cardiologia)
--    ID 4:  Médica Org 1 (Dra. Maria — Dermatologia)
--    ID 5:  Dentista Org 1 (Dr. Ricardo — Ortodontia)
--    ID 6:  Secretária Org 1
--    ID 7:  Admin Org 2
--    ID 8:  Médico Org 2 (Dr. Pedro — Ortopedia)
--    ID 9:  Dentista Org 2 (Dra. Ana — Endodontia)
--    ID 10: Secretária Org 2
-- ==============================================================================
INSERT INTO usuarios (organizacao_id, login, senha, TipoUsuario, tipo_usuario_novo, status) VALUES
(1,    'adm',                           '$2y$10$hubYaqDdzJx/4apNtiCoE.jAah1wlFLGJ9PoST1lsbS8u7WwnwGYK', 1, 'ADMIN_ORG',    'ATIVO'),
(1,    'med',                           '$2y$10$hubYaqDdzJx/4apNtiCoE.jAah1wlFLGJ9PoST1lsbS8u7WwnwGYK', 3, 'PROFISSIONAL', 'ATIVO'),
(1,    'dra.maria@saudetotal.com.br',   '$2y$10$hubYaqDdzJx/4apNtiCoE.jAah1wlFLGJ9PoST1lsbS8u7WwnwGYK', 3, 'PROFISSIONAL', 'ATIVO'),
(1,    'med2',  '$2y$10$hubYaqDdzJx/4apNtiCoE.jAah1wlFLGJ9PoST1lsbS8u7WwnwGYK', 3, 'PROFISSIONAL', 'ATIVO'),
(1,    'recepcao@saudetotal.com.br',    '$2y$10$hubYaqDdzJx/4apNtiCoE.jAah1wlFLGJ9PoST1lsbS8u7WwnwGYK', 2, 'RECEPCIONISTA','ATIVO'),
(2,    'admin@odontosmile.com.br',      '$2y$10$hubYaqDdzJx/4apNtiCoE.jAah1wlFLGJ9PoST1lsbS8u7WwnwGYK', 1, 'ADMIN_ORG',    'ATIVO'),
(2,    'dr.pedro@odontosmile.com.br',   '$2y$10$hubYaqDdzJx/4apNtiCoE.jAah1wlFLGJ9PoST1lsbS8u7WwnwGYK', 3, 'PROFISSIONAL', 'ATIVO'),
(2,    'dra.ana@odontosmile.com.br',    '$2y$10$hubYaqDdzJx/4apNtiCoE.jAah1wlFLGJ9PoST1lsbS8u7WwnwGYK', 3, 'PROFISSIONAL', 'ATIVO'),
(2,    'atendimento@odontosmile.com.br','$2y$10$hubYaqDdzJx/4apNtiCoE.jAah1wlFLGJ9PoST1lsbS8u7WwnwGYK', 2, 'RECEPCIONISTA','ATIVO');

-- ==============================================================================
-- 4. ADMIN ORGANIZAÇÃO (2 registros)
-- ==============================================================================
INSERT INTO admin_organizacao (organizacao_id, usuario_id, nome, cargo, email, is_owner, status, created_at) VALUES
(1, 2, 'João Administrador', 'Diretor Administrativo', 'admin@saudetotal.com.br',  1, 'ATIVO', NOW()),
(2, 7, 'Maria Gestora',      'Gerente Clínico',        'admin@odontosmile.com.br', 1, 'ATIVO', NOW());

-- ==============================================================================
-- 5. PLANOS DE ASSINATURA
--    Já inseridos pela migration. IDs: 1=STARTER, 2=PROFISSIONAL, 3=BUSINESS
-- ==============================================================================

-- ==============================================================================
-- 6. ASSINATURA TENANT (2 registros)
--    Org 1 → Plano PROFISSIONAL (id=2)
--    Org 2 → Plano STARTER      (id=1)
-- ==============================================================================
INSERT INTO assinatura_tenant (organizacao_id, plano_assinatura_id, status, data_inicio, data_vencimento, data_proxima_cobranca, valor_mensal, criado_em, atualizado_em) VALUES
(1, 2, 'ATIVA',
    DATE_SUB(CURDATE(), INTERVAL 60 DAY),
    DATE_ADD(CURDATE(), INTERVAL 305 DAY),
    DATE_ADD(CURDATE(), INTERVAL 30 DAY),
    449.00, DATE_SUB(NOW(), INTERVAL 60 DAY), NOW()),
(2, 1, 'ATIVA',
    DATE_SUB(CURDATE(), INTERVAL 30 DAY),
    DATE_ADD(CURDATE(), INTERVAL 335 DAY),
    DATE_ADD(CURDATE(), INTERVAL 30 DAY),
    249.00, DATE_SUB(NOW(), INTERVAL 30 DAY), NOW());

-- ==============================================================================
-- 7. COBRANÇA TENANT (3 registros)
-- ==============================================================================
INSERT INTO cobranca_tenant (assinatura_tenant_id, organizacao_id, valor_total, status, pix_copia_cola, pix_qrcode_base64, txid, data_vencimento_pix, data_pagamento, criada_em) VALUES
(1, 1, 449.00, 'PAGO',
    '00020126580014br.gov.bcb.pix0136a1b2c3d4-e5f6-7890-abcd-ef123456789052040000530398654054490.005802BR5925CLINICA SAUDE TOTAL LTDA6009SAO PAULO62070503***63041D3E',
    'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==',
    'TXN-ORG1-COB001',
    DATE_SUB(CURDATE(), INTERVAL 58 DAY),
    DATE_SUB(NOW(), INTERVAL 57 DAY),
    DATE_SUB(NOW(), INTERVAL 60 DAY)),
(1, 1, 449.00, 'PAGO',
    '00020126580014br.gov.bcb.pix0136a1b2c3d4-e5f6-7890-abcd-ef123456789052040000530398654054490.005802BR5925CLINICA SAUDE TOTAL LTDA6009SAO PAULO62070503***63041D3E',
    'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==',
    'TXN-ORG1-COB002',
    DATE_SUB(CURDATE(), INTERVAL 28 DAY),
    DATE_SUB(NOW(), INTERVAL 27 DAY),
    DATE_SUB(NOW(), INTERVAL 30 DAY)),
(2, 2, 249.00, 'PAGO',
    '00020126580014br.gov.bcb.pix0136b2c3d4e5-f6g7-8901-bcde-fg234567890152040000530398654052490.005802BR5925ODONTO SMILE LTDA ME6009RIO DE JANEIRO62070503***6304A1B2',
    'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==',
    'TXN-ORG2-COB001',
    DATE_SUB(CURDATE(), INTERVAL 28 DAY),
    DATE_SUB(NOW(), INTERVAL 27 DAY),
    DATE_SUB(NOW(), INTERVAL 30 DAY));

-- ==============================================================================
-- 8. PROFISSIONAIS (5 registros)
--    Org 1: Dr. Carlos (Médico), Dra. Maria (Médica), Dr. Ricardo (Dentista)
--    Org 2: Dr. Pedro (Médico), Dra. Ana (Dentista)
-- ==============================================================================
INSERT INTO profissional (organizacao_id, tipo_profissional_id, nome, sexo, data_nascimento, registro_conselho, cpf, email, telefone, formacao, instituicao, tempo_consulta_minutos, valor_consulta, usuario_id, endereco_id, status, created_at, updated_at) VALUES
(1, 1, 'Dr. Carlos Alberto Silva',   'MASCULINO', '1975-03-15', 'CRM-123456', '11122233344', 'dr.carlos@saudetotal.com.br',  '(11) 99999-0001', 'Medicina — Cardiologia',   'USP',     30, 250.00, 3, 3, 'ATIVO', NOW(), NOW()),
(1, 1, 'Dra. Maria Fernanda Costa',  'FEMININO',  '1980-07-22', 'CRM-234567', '22233344455', 'dra.maria@saudetotal.com.br',  '(11) 99999-0002', 'Medicina — Dermatologia',  'UNICAMP', 20, 180.00, 4, 4, 'ATIVO', NOW(), NOW()),
(1, 2, 'Dr. Ricardo Mendes',         'MASCULINO', '1983-09-10', 'CRO-345678', '33344455566', 'dr.ricardo@saudetotal.com.br', '(11) 99999-0003', 'Odontologia — Ortodontia', 'FOUSP',   45, 200.00, 5, 5, 'ATIVO', NOW(), NOW()),
(2, 1, 'Dr. Pedro Santos',           'MASCULINO', '1978-11-08', 'CRM-456789', '55566677788', 'dr.pedro@odontosmile.com.br',  '(21) 99999-0004', 'Medicina — Ortopedia',     'UFRJ',   40, 200.00, 8, 6, 'ATIVO', NOW(), NOW()),
(2, 2, 'Dra. Ana Paula Oliveira',    'FEMININO',  '1982-05-30', 'CRO-567890', '66677788899', 'dra.ana@odontosmile.com.br',   '(21) 99999-0005', 'Odontologia — Endodontia', 'UERJ',   45, 180.00, 9, 7, 'ATIVO', NOW(), NOW());

-- ==============================================================================
-- 9. PROFISSIONAL <-> ESPECIALIDADE (N:N)
--    Especialidades médicas (tipo_prof=1):  1=Cardiologia, 2=Dermatologia, 3=Ortopedia ...
--    Especialidades odontológicas (tipo_prof=2): 11=Ortodontia, 12=Endodontia ...
-- ==============================================================================
INSERT INTO profissional_especialidade (profissional_id, especialidade_id, principal) VALUES
(1, 1, 1),   -- Dr. Carlos  -> Cardiologia (esp id=1)
(2, 2, 1),   -- Dra. Maria  -> Dermatologia (esp id=2)
(3, 11, 1),  -- Dr. Ricardo -> Ortodontia (esp id=11)
(4, 3, 1),   -- Dr. Pedro   -> Ortopedia (esp id=3)
(5, 12, 1);  -- Dra. Ana    -> Endodontia (esp id=12)

-- ==============================================================================
-- 10. SECRETÁRIAS (2 registros)
-- ==============================================================================
INSERT INTO secretaria (organizacao_id, usuario_id, nome, cpf, email, telefone, status) VALUES
(1, 6,  'Patrícia Recepcionista', '77788899900', 'recepcao@saudetotal.com.br',     '(11) 99999-0010', 'ATIVO'),
(2, 10, 'Carla Atendimento',      '88899900011', 'atendimento@odontosmile.com.br', '(21) 99999-0011', 'ATIVO');

-- ==============================================================================
-- 11. PACIENTES (12 registros — 6 por organização)
-- ==============================================================================
INSERT INTO paciente (organizacao_id, PaciNome, PaciSexo, PaciDataNacimento, PaciCpf, PaciRg, PaciEmail, PaciTelefone, Endereco, PaciStatus) VALUES
(1, 'José da Silva',      'Masculino', '1985-05-10', '123.456.789-00', '12.345.678-X', 'jose.silva@email.com',      '(11) 98888-0001', 8,  'ATIVO'),
(1, 'Maria Santos',       'Feminino',  '1990-08-15', '234.567.890-11', '23.456.789-1', 'maria.santos@email.com',    '(11) 98888-0002', 9,  'ATIVO'),
(1, 'Pedro Oliveira',     'Masculino', '1978-03-22', '345.678.901-22', '34.567.890-2', 'pedro.oliveira@email.com',  '(11) 98888-0003', 10, 'ATIVO'),
(1, 'Ana Costa',          'Feminino',  '1995-11-30', '456.789.012-33', '45.678.901-3', 'ana.costa@email.com',       '(11) 98888-0004', 11, 'ATIVO'),
(1, 'Carlos Ferreira',    'Masculino', '1982-07-18', '567.890.123-44', '56.789.012-4', 'carlos.ferreira@email.com', '(11) 98888-0005', 12, 'ATIVO'),
(1, 'Beatriz Almeida',    'Feminino',  '1993-02-28', '678.901.234-55', '67.890.123-5', 'beatriz.almeida@email.com', '(11) 98888-0006', 13, 'ATIVO'),
(2, 'Juliana Lima',       'Feminino',  '1988-01-25', '789.012.345-66', '78.901.234-6', 'juliana.lima@email.com',    '(21) 98888-0007', 14, 'ATIVO'),
(2, 'Roberto Almeida',    'Masculino', '1970-09-12', '890.123.456-77', '89.012.345-7', 'roberto.almeida@email.com', '(21) 98888-0008', 15, 'ATIVO'),
(2, 'Fernanda Rocha',     'Feminino',  '1992-04-08', '901.234.567-88', '90.123.456-8', 'fernanda.rocha@email.com',  '(21) 98888-0009', 16, 'ATIVO'),
(2, 'Marcos Souza',       'Masculino', '1975-12-03', '012.345.678-99', '01.234.567-9', 'marcos.souza@email.com',    '(21) 98888-0010', 17, 'ATIVO'),
(2, 'Camila Pereira',     'Feminino',  '1998-06-20', '111.222.333-44', '11.222.333-0', 'camila.pereira@email.com',  '(21) 98888-0011', 18, 'ATIVO'),
(2, 'Lucas Mendonça',     'Masculino', '1987-10-14', '222.333.444-55', '22.333.444-1', 'lucas.mendonca@email.com',  '(21) 98888-0012', 19, 'ATIVO');

-- ==============================================================================
-- 12. CONSULTAS (30 registros) — DATAS DINÂMICAS relativas a CURDATE()
--
-- LEGENDA PROFISSIONAIS:
--   Prof 1 = Dr. Carlos   (Médico/Cardio,     org1, esp_id=1)
--   Prof 2 = Dra. Maria   (Médica/Dermato,    org1, esp_id=2)
--   Prof 3 = Dr. Ricardo  (Dentista/Ortodont,  org1, esp_id=11)
--   Prof 4 = Dr. Pedro    (Médico/Ortopedia,   org2, esp_id=3)
--   Prof 5 = Dra. Ana     (Dentista/Endodont,  org2, esp_id=12)
--
-- STATUS: REALIZADA (passado), CANCELADA (passado), AGENDADA (futuro)
-- forma_pagamento_id: 1=Particular, 2=Convênio, 3=Crédito, 4=Débito, 5=PIX, 6=Dinheiro
-- ==============================================================================
INSERT INTO consulta (organizacao_id, profissional_id, paciente_id, especialidade_id, data_hora, duracao_minutos, observacoes, forma_pagamento_id, valor, status, criado_por, created_at, updated_at) VALUES

-- ======= ORG 1 — CLÍNICA SAÚDE TOTAL =======

-- Prof 1 (Dr. Carlos — Cardiologia) — REALIZADAS
(1, 1, 1, 1, DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 28 DAY), INTERVAL 8 HOUR),  30, 'Check-up cardíaco anual',           1, 250.00, 'REALIZADA', 6, DATE_SUB(NOW(), INTERVAL 35 DAY), NOW()),
(1, 1, 2, 1, DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 21 DAY), INTERVAL 9 HOUR),  30, 'Avaliação de pressão arterial',     5, 250.00, 'REALIZADA', 6, DATE_SUB(NOW(), INTERVAL 28 DAY), NOW()),
(1, 1, 3, 1, DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 14 DAY), INTERVAL 10 HOUR), 30, 'Retorno — arritmia cardíaca',       1, 200.00, 'REALIZADA', 6, DATE_SUB(NOW(), INTERVAL 21 DAY), NOW()),
(1, 1, 4, 1, DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 7 DAY),  INTERVAL 8 HOUR),  30, 'Eletrocardiograma + avaliação',     2, 300.00, 'REALIZADA', 6, DATE_SUB(NOW(), INTERVAL 14 DAY), NOW()),
-- Prof 1 — CANCELADA
(1, 1, 5, 1, DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 10 DAY), INTERVAL 11 HOUR), 30, 'Dor no peito — URGENTE',            1, 300.00, 'CANCELADA', 6, DATE_SUB(NOW(), INTERVAL 17 DAY), NOW()),
-- Prof 1 — AGENDADAS (futuro)
(1, 1, 1, 1, DATE_ADD(DATE_ADD(CURDATE(), INTERVAL 1 DAY),  INTERVAL 8 HOUR),  30, 'Retorno cardiologia',               1, 200.00, 'AGENDADA',  6, NOW(), NOW()),
(1, 1, 6, 1, DATE_ADD(DATE_ADD(CURDATE(), INTERVAL 3 DAY),  INTERVAL 9 HOUR),  30, 'Primeira consulta — check-up',      5, 250.00, 'AGENDADA',  6, NOW(), NOW()),
(1, 1, 2, 1, DATE_ADD(DATE_ADD(CURDATE(), INTERVAL 7 DAY),  INTERVAL 10 HOUR), 30, 'Acompanhamento hipertensão',        1, 200.00, 'AGENDADA',  6, NOW(), NOW()),

-- Prof 2 (Dra. Maria — Dermatologia) — REALIZADAS
(1, 2, 3, 2, DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 25 DAY), INTERVAL 14 HOUR), 20, 'Avaliação dermatológica geral',     3, 180.00, 'REALIZADA', 6, DATE_SUB(NOW(), INTERVAL 30 DAY), NOW()),
(1, 2, 4, 2, DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 18 DAY), INTERVAL 15 HOUR), 20, 'Tratamento acne facial',            1, 180.00, 'REALIZADA', 6, DATE_SUB(NOW(), INTERVAL 25 DAY), NOW()),
(1, 2, 5, 2, DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 5 DAY),  INTERVAL 14 HOUR), 20, 'Procedimento estético — peeling',   5, 350.00, 'REALIZADA', 6, DATE_SUB(NOW(), INTERVAL 10 DAY), NOW()),
-- Prof 2 — AGENDADAS (futuro)
(1, 2, 6, 2, DATE_ADD(DATE_ADD(CURDATE(), INTERVAL 2 DAY),  INTERVAL 14 HOUR), 20, 'Manchas na pele — avaliação',       2, 180.00, 'AGENDADA',  6, NOW(), NOW()),
(1, 2, 1, 2, DATE_ADD(DATE_ADD(CURDATE(), INTERVAL 8 DAY),  INTERVAL 15 HOUR), 20, 'Retorno dermatologia',              1, 150.00, 'AGENDADA',  6, NOW(), NOW()),

-- Prof 3 (Dr. Ricardo — Ortodontia/Dentista) — REALIZADAS
(1, 3, 2, 11, DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 20 DAY), INTERVAL 9 HOUR),  45, 'Avaliação ortodôntica inicial',    1, 200.00, 'REALIZADA', 6, DATE_SUB(NOW(), INTERVAL 27 DAY), NOW()),
(1, 3, 4, 11, DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 12 DAY), INTERVAL 10 HOUR), 45, 'Manutenção aparelho ortodôntico',  5, 150.00, 'REALIZADA', 6, DATE_SUB(NOW(), INTERVAL 19 DAY), NOW()),
(1, 3, 6, 11, DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 3 DAY),  INTERVAL 11 HOUR), 45, 'Limpeza dental + profilaxia',      1, 180.00, 'REALIZADA', 6, DATE_SUB(NOW(), INTERVAL 10 DAY), NOW()),
-- Prof 3 — CANCELADA
(1, 3, 5, 11, DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 8 DAY),  INTERVAL 14 HOUR), 45, 'Extração de siso — cancelada',     3, 250.00, 'CANCELADA', 6, DATE_SUB(NOW(), INTERVAL 15 DAY), NOW()),
-- Prof 3 — AGENDADAS (futuro)
(1, 3, 3, 11, DATE_ADD(DATE_ADD(CURDATE(), INTERVAL 4 DAY),  INTERVAL 9 HOUR),  45, 'Avaliação para aparelho',          1, 200.00, 'AGENDADA',  6, NOW(), NOW()),
(1, 3, 5, 11, DATE_ADD(DATE_ADD(CURDATE(), INTERVAL 10 DAY), INTERVAL 10 HOUR), 45, 'Reagendamento extração de siso',   5, 250.00, 'AGENDADA',  6, NOW(), NOW()),

-- ======= ORG 2 — ODONTO SMILE =======

-- Prof 4 (Dr. Pedro — Ortopedia) — REALIZADAS
(2, 4, 7,  3, DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 30 DAY), INTERVAL 10 HOUR), 40, 'Dor no joelho — avaliação inicial', 1, 200.00, 'REALIZADA', 10, DATE_SUB(NOW(), INTERVAL 37 DAY), NOW()),
(2, 4, 8,  3, DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 15 DAY), INTERVAL 11 HOUR), 40, 'Lesão esportiva — tornozelo',       2, 200.00, 'REALIZADA', 10, DATE_SUB(NOW(), INTERVAL 22 DAY), NOW()),
(2, 4, 9,  3, DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 6 DAY),  INTERVAL 10 HOUR), 40, 'Retorno — ressonância magnética',   5, 250.00, 'REALIZADA', 10, DATE_SUB(NOW(), INTERVAL 13 DAY), NOW()),
-- Prof 4 — AGENDADAS (futuro)
(2, 4, 10, 3, DATE_ADD(DATE_ADD(CURDATE(), INTERVAL 2 DAY),  INTERVAL 10 HOUR), 40, 'Dor lombar crônica',                1, 200.00, 'AGENDADA',  10, NOW(), NOW()),
(2, 4, 7,  3, DATE_ADD(DATE_ADD(CURDATE(), INTERVAL 9 DAY),  INTERVAL 11 HOUR), 40, 'Retorno joelho — pós-fisioterapia', 1, 180.00, 'AGENDADA',  10, NOW(), NOW()),

-- Prof 5 (Dra. Ana — Endodontia/Dentista) — REALIZADAS
(2, 5, 10, 12, DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 22 DAY), INTERVAL 14 HOUR), 45, 'Tratamento de canal — molar',      1, 350.00, 'REALIZADA', 10, DATE_SUB(NOW(), INTERVAL 29 DAY), NOW()),
(2, 5, 11, 12, DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 11 DAY), INTERVAL 15 HOUR), 45, 'Restauração após canal',           5, 280.00, 'REALIZADA', 10, DATE_SUB(NOW(), INTERVAL 18 DAY), NOW()),
(2, 5, 12, 12, DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 4 DAY),  INTERVAL 14 HOUR), 45, 'Avaliação endodôntica',            1, 180.00, 'REALIZADA', 10, DATE_SUB(NOW(), INTERVAL 11 DAY), NOW()),
-- Prof 5 — CANCELADA
(2, 5, 8,  12, DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 9 DAY),  INTERVAL 16 HOUR), 45, 'Retratamento canal — desmarcado',  3, 400.00, 'CANCELADA', 10, DATE_SUB(NOW(), INTERVAL 16 DAY), NOW()),
-- Prof 5 — AGENDADAS (futuro)
(2, 5, 9,  12, DATE_ADD(DATE_ADD(CURDATE(), INTERVAL 5 DAY),  INTERVAL 14 HOUR), 45, 'Limpeza + avaliação cárie',        1, 180.00, 'AGENDADA',  10, NOW(), NOW()),
(2, 5, 11, 12, DATE_ADD(DATE_ADD(CURDATE(), INTERVAL 12 DAY), INTERVAL 15 HOUR), 45, 'Retorno restauração',              5, 150.00, 'AGENDADA',  10, NOW(), NOW());

-- ==============================================================================
-- 13. CONSULTA HISTÓRICO
-- ==============================================================================
INSERT INTO consulta_historico (consulta_id, status_anterior, status_novo, observacao, alterado_por, created_at) VALUES
-- Org 1 — Realizadas Dr. Carlos
(1,  'AGENDADA', 'REALIZADA', 'Consulta finalizada — paciente orientado sobre dieta', 3, DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 28 DAY), INTERVAL 9 HOUR)),
(2,  'AGENDADA', 'REALIZADA', 'Pressão estável — manter medicação',                   3, DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 21 DAY), INTERVAL 10 HOUR)),
(3,  'AGENDADA', 'REALIZADA', 'ECG normal — retorno em 30 dias',                      3, DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 14 DAY), INTERVAL 11 HOUR)),
(4,  'AGENDADA', 'REALIZADA', 'ECG realizado — resultados normais',                   3, DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 7 DAY),  INTERVAL 9 HOUR)),
(5,  'AGENDADA', 'CANCELADA', 'Paciente solicitou cancelamento — viagem',             6, DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 11 DAY), INTERVAL 8 HOUR)),
-- Org 1 — Realizadas Dra. Maria
(9,  'AGENDADA', 'REALIZADA', 'Pele saudável — orientações gerais',                   4, DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 25 DAY), INTERVAL 15 HOUR)),
(10, 'AGENDADA', 'REALIZADA', 'Tratamento com retinóide iniciado',                    4, DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 18 DAY), INTERVAL 16 HOUR)),
(11, 'AGENDADA', 'REALIZADA', 'Peeling aplicado com sucesso',                         4, DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 5 DAY),  INTERVAL 15 HOUR)),
-- Org 1 — Realizadas Dr. Ricardo (Dentista)
(14, 'AGENDADA', 'REALIZADA', 'Moldagem realizada — aparelho em 15 dias',             5, DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 20 DAY), INTERVAL 10 HOUR)),
(15, 'AGENDADA', 'REALIZADA', 'Ajuste de fio — evolução positiva',                    5, DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 12 DAY), INTERVAL 11 HOUR)),
(16, 'AGENDADA', 'REALIZADA', 'Limpeza completa — sem cáries',                        5, DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 3 DAY),  INTERVAL 12 HOUR)),
(17, 'AGENDADA', 'CANCELADA', 'Paciente com medo — reagendou',                        6, DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 9 DAY),  INTERVAL 10 HOUR)),
-- Org 2 — Realizadas Dr. Pedro
(20, 'AGENDADA', 'REALIZADA', 'Raio-X solicitado — possível menisco',                 8, DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 30 DAY), INTERVAL 11 HOUR)),
(21, 'AGENDADA', 'REALIZADA', 'Imobilização e fisioterapia prescrita',                 8, DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 15 DAY), INTERVAL 12 HOUR)),
(22, 'AGENDADA', 'REALIZADA', 'Ressonância analisada — sem cirurgia',                 8, DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 6 DAY),  INTERVAL 11 HOUR)),
-- Org 2 — Realizadas Dra. Ana (Dentista)
(25, 'AGENDADA', 'REALIZADA', 'Canal realizado com sucesso — 3 canais',               9, DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 22 DAY), INTERVAL 15 HOUR)),
(26, 'AGENDADA', 'REALIZADA', 'Restauração em resina — ótimo resultado',              9, DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 11 DAY), INTERVAL 16 HOUR)),
(27, 'AGENDADA', 'REALIZADA', 'Sem necessidade de canal — apenas restauração',        9, DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 4 DAY),  INTERVAL 15 HOUR)),
(28, 'AGENDADA', 'CANCELADA', 'Paciente desmarcou por conflito de agenda',            10, DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 10 DAY), INTERVAL 12 HOUR));

-- ==============================================================================
-- 14. ATUALIZAR consultas canceladas com motivo
-- ==============================================================================
UPDATE consulta SET cancelado_por = 'PACIENTE', motivo_cancelamento = 'Viagem de emergência'  WHERE id = 5;
UPDATE consulta SET cancelado_por = 'PACIENTE', motivo_cancelamento = 'Medo do procedimento'  WHERE id = 17;
UPDATE consulta SET cancelado_por = 'PACIENTE', motivo_cancelamento = 'Conflito de agenda'    WHERE id = 28;

-- ==============================================================================
-- 15. PRONTUÁRIO MÉDICO (10 registros)
--     Consultas REALIZADAS dos MÉDICOS (prof 1, 2, 4)
-- ==============================================================================
INSERT INTO prontuario (
    prontPeso, prontAltura, prontTemperatura, prontSexo, prontSaturacao,
    prontHemoglobina, prontPressao, prontFrequenciaRespiratoria,
    prontFrequenciaArterialSistolica, prontFrequenciaArterialDiastolica,
    prontObservacao, prontCondulta, prontAnamnese, prontQueixaPricipal,
    prontDiagnostico, prontModeloPrescricao, prontTituloPrescricao,
    prontDataPrescricao, prontPrescricao, prontDataFinalizado,
    prontCodigoMedico, consulta, prontTempoDuracao
) VALUES
-- Consulta 1 — Dr. Carlos / José (Cardiologia) — -28 dias
('78.5', '1.75', '36.5', 'Masculino', '98', '14.2', '130/85', '18', '130', '85',
 'Paciente refere cansaço aos esforços', 'Solicitar ecocardiograma', 'HAS há 5 anos, uso de Losartana 50mg',
 'Cansaço aos esforços moderados', 'Hipertensão arterial sistêmica controlada',
 'Modelo padrão cardiologia', 'Prescrição Cardiológica',
 DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 28 DAY), '%Y-%m-%d'),
 'Losartana 50mg — 1x/dia manhã; AAS 100mg — 1x/dia',
 DATE_SUB(CURDATE(), INTERVAL 28 DAY), 1, 1, '30 min'),

-- Consulta 2 — Dr. Carlos / Maria Santos (Cardiologia) — -21 dias
('62.0', '1.63', '36.2', 'Feminino', '99', '13.5', '120/78', '16', '120', '78',
 'Pressão arterial estável', 'Manter medicação atual', 'Hipertensa, uso de Enalapril 10mg',
 'Acompanhamento de rotina', 'HAS controlada — manter tratamento',
 NULL, NULL, NULL, NULL,
 DATE_SUB(CURDATE(), INTERVAL 21 DAY), 1, 2, '25 min'),

-- Consulta 3 — Dr. Carlos / Pedro (Cardiologia) — -14 dias
('85.0', '1.80', '36.8', 'Masculino', '97', '15.0', '140/90', '20', '140', '90',
 'Arritmia detectada em ECG prévio', 'Holter 24h solicitado', 'Palpitações esporádicas há 2 meses',
 'Palpitações e arritmia', 'Arritmia sinusal — investigação complementar',
 'Modelo padrão cardiologia', 'Prescrição — Investigação',
 DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 14 DAY), '%Y-%m-%d'),
 'Propranolol 40mg — 2x/dia; Solicitar Holter 24h',
 DATE_SUB(CURDATE(), INTERVAL 14 DAY), 1, 3, '30 min'),

-- Consulta 4 — Dr. Carlos / Ana Costa (Cardiologia) — -7 dias
('58.0', '1.60', '36.3', 'Feminino', '99', '12.8', '110/70', '15', '110', '70',
 'ECG sem alterações significativas', 'Alta — retorno anual', 'Sem queixas cardiovasculares',
 'Check-up preventivo', 'Coração saudável — sem alterações',
 NULL, NULL, NULL, NULL,
 DATE_SUB(CURDATE(), INTERVAL 7 DAY), 1, 4, '30 min'),

-- Consulta 9 — Dra. Maria / Pedro (Dermatologia) — -25 dias
('85.0', '1.80', '36.5', 'Masculino', '98', '15.0', '125/80', '17', '125', '80',
 'Lesões eritematosas em dorso', 'Biópsia agendada', 'Manchas vermelhas há 3 meses',
 'Manchas avermelhadas no dorso', 'Dermatite seborreica',
 'Modelo dermatologia', 'Prescrição Dermatológica',
 DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 25 DAY), '%Y-%m-%d'),
 'Cetoconazol creme 2% — aplicar 2x/dia por 15 dias',
 DATE_SUB(CURDATE(), INTERVAL 25 DAY), 2, 9, '20 min'),

-- Consulta 10 — Dra. Maria / Ana Costa (Dermatologia) — -18 dias
('58.0', '1.60', '36.4', 'Feminino', '99', '12.8', '110/70', '16', '110', '70',
 'Acne grau II em face', 'Iniciar tratamento tópico', 'Acne desde adolescência, piora recente',
 'Acne facial persistente', 'Acne vulgar grau II',
 'Modelo dermatologia', 'Prescrição Acne',
 DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 18 DAY), '%Y-%m-%d'),
 'Adapaleno gel 0.1% — aplicar à noite; Peróxido de benzoíla 5% — manhã',
 DATE_SUB(CURDATE(), INTERVAL 18 DAY), 2, 10, '20 min'),

-- Consulta 11 — Dra. Maria / Carlos Ferreira (Dermatologia) — -5 dias
('80.0', '1.78', '36.6', 'Masculino', '98', '14.5', '128/82', '17', '128', '82',
 'Peeling químico realizado sem intercorrências', 'Retorno em 15 dias para avaliação', 'Manchas solares em face',
 'Manchas solares — peeling', 'Melanose solar',
 NULL, NULL, NULL, NULL,
 DATE_SUB(CURDATE(), INTERVAL 5 DAY), 2, 11, '20 min'),

-- Consulta 20 — Dr. Pedro / Juliana (Ortopedia) — -30 dias
('65.0', '1.65', '36.4', 'Feminino', '99', '13.0', '118/76', '16', '118', '76',
 'Dor no joelho D há 2 semanas', 'Raio-X solicitado', 'Dor ao subir escadas e agachar',
 'Dor no joelho direito', 'Condromalácia patelar grau II',
 'Modelo ortopedia', 'Prescrição Ortopédica',
 DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 30 DAY), '%Y-%m-%d'),
 'Ibuprofeno 600mg — 8/8h por 5 dias; Fisioterapia 2x/semana',
 DATE_SUB(CURDATE(), INTERVAL 30 DAY), 4, 20, '40 min'),

-- Consulta 21 — Dr. Pedro / Roberto (Ortopedia) — -15 dias
('90.0', '1.82', '36.7', 'Masculino', '97', '15.5', '135/88', '18', '135', '88',
 'Entorse de tornozelo E grau II', 'Imobilização com tala', 'Torção durante futebol há 3 dias',
 'Lesão esportiva — tornozelo', 'Entorse de tornozelo grau II',
 'Modelo ortopedia', 'Prescrição — Imobilização',
 DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 15 DAY), '%Y-%m-%d'),
 'Nimesulida 100mg — 12/12h por 7 dias; Gelo local 3x/dia; Imobilização por 15 dias',
 DATE_SUB(CURDATE(), INTERVAL 15 DAY), 4, 21, '40 min'),

-- Consulta 22 — Dr. Pedro / Fernanda (Ortopedia) — -6 dias
('60.0', '1.62', '36.3', 'Feminino', '99', '12.5', '115/72', '15', '115', '72',
 'RM sem lesão ligamentar — menisco íntegro', 'Fisioterapia e fortalecimento', 'Retorno para avaliar RM de joelho',
 'Retorno — resultado de ressonância', 'Sem lesão estrutural — fortalecer musculatura',
 NULL, NULL, NULL, NULL,
 DATE_SUB(CURDATE(), INTERVAL 6 DAY), 4, 22, '35 min');

-- ==============================================================================
-- 16. PRONTUÁRIO DENTISTA (6 registros)
--     Consultas REALIZADAS dos DENTISTAS (prof 3, 5)
-- ==============================================================================
INSERT INTO prontuario_dentista (
    prontdent_queixa_principal, prontdent_anamnese, prontdent_observacao,
    prontdent_higiene_bucal, prontdent_condicao_gengival,
    prontdent_diagnostico, prontdent_plano_tratamento,
    prontdent_titulo_prescricao, prontdent_data_prescricao, prontdent_prescricao,
    prontdent_procedimentos, prontdent_orientacoes,
    prontdent_pressao_arterial, prontdent_peso, prontdent_altura,
    prontdent_data_finalizado, prontdent_tempo_duracao,
    prontdent_codigo_medico, prontdent_consulta
) VALUES
-- Consulta 14 — Dr. Ricardo / Maria Santos (Ortodontia) — -20 dias
('Dentes desalinhados', 'Sem alergias. Última visita ao dentista há 1 ano.', 'Paciente cooperativa, boa abertura bucal',
 'Regular', 'Saudável — sem sangramento',
 'Maloclusão Classe II — indicação de aparelho fixo', 'Instalação de aparelho ortodôntico fixo metálico',
 'Prescrição Pós-Moldagem', DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 20 DAY), '%Y-%m-%d'),
 'Paracetamol 750mg se dor; Bochecho com Clorexidina 0,12% — 2x/dia por 7 dias',
 'Moldagem superior e inferior para confecção de aparelho', 'Evitar alimentos duros por 48h após instalação do aparelho',
 '120/78', '62.0', '1.63',
 DATE_SUB(CURDATE(), INTERVAL 20 DAY), '45 min', 3, 14),

-- Consulta 15 — Dr. Ricardo / Ana Costa (Ortodontia) — -12 dias
('Manutenção do aparelho', 'Uso de aparelho há 6 meses. Sem queixas.', 'Boa evolução — dentes alinhando',
 'Boa', 'Normal',
 'Evolução satisfatória do tratamento ortodôntico', 'Troca de fio — NiTi 0.016 para 0.018',
 NULL, NULL, NULL,
 'Troca de fio ortodôntico e ajuste de elásticos', 'Manter uso de elásticos intermaxilares conforme orientado',
 '110/70', '58.0', '1.60',
 DATE_SUB(CURDATE(), INTERVAL 12 DAY), '40 min', 3, 15),

-- Consulta 16 — Dr. Ricardo / Beatriz (Limpeza) — -3 dias
('Limpeza de rotina', 'Paciente saudável, sem queixas.', 'Pouco tártaro supra-gengival',
 'Boa', 'Leve gengivite marginal',
 'Gengivite marginal leve — boa higiene geral', 'Profilaxia + orientação de escovação',
 NULL, NULL, NULL,
 'Raspagem supra-gengival + profilaxia com pasta profilática + polimento', 'Usar fio dental diariamente. Escovar 3x ao dia com técnica de Bass.',
 '115/75', '55.0', '1.58',
 DATE_SUB(CURDATE(), INTERVAL 3 DAY), '45 min', 3, 16),

-- Consulta 25 — Dra. Ana / Marcos Souza (Endodontia) — -22 dias
('Dor forte no dente 36', 'Diabético tipo 2 — uso de Metformina.', 'Teste de vitalidade negativo — necrose pulpar',
 'Regular', 'Inflamação gengival localizada no 36',
 'Necrose pulpar no dente 36 — indicação de tratamento endodôntico', 'Tratamento de canal em 2 sessões',
 'Prescrição Pós-Canal', DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 22 DAY), '%Y-%m-%d'),
 'Amoxicilina 500mg — 8/8h por 7 dias; Ibuprofeno 600mg — 8/8h por 3 dias',
 'Abertura coronária, instrumentação e medicação intracanal (Calen)', 'Evitar mastigar do lado esquerdo. Retornar em 7 dias para obturação.',
 '130/85', '82.0', '1.76',
 DATE_SUB(CURDATE(), INTERVAL 22 DAY), '50 min', 5, 25),

-- Consulta 26 — Dra. Ana / Camila (Restauração pós-canal) — -11 dias
('Retorno para restauração pós-canal', 'Sem alergias. Canal realizado há 2 semanas.', 'Dente assintomático — pronto para restauração',
 'Boa', 'Normal',
 'Dente 46 desvitalizado — restauração definitiva', 'Restauração em resina composta',
 NULL, NULL, NULL,
 'Restauração direta em resina composta (A2) — face oclusal e mesial', 'Evitar mastigar alimentos muito duros nas primeiras 24h.',
 '110/70', '56.0', '1.60',
 DATE_SUB(CURDATE(), INTERVAL 11 DAY), '45 min', 5, 26),

-- Consulta 27 — Dra. Ana / Lucas (Avaliação endodôntica) — -4 dias
('Sensibilidade ao frio no dente 15', 'Saudável, sem medicações.', 'Teste de vitalidade positivo — polpa vital',
 'Boa', 'Saudável',
 'Cárie profunda no 15 — sem comprometimento pulpar', 'Restauração profunda com proteção pulpar indireta',
 'Prescrição Pós-Restauração', DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '%Y-%m-%d'),
 'Paracetamol 750mg — se dor',
 'Remoção de cárie + proteção pulpar com hidróxido de cálcio + restauração em resina', 'Sensibilidade pode persistir por até 2 semanas. Se piorar, retornar.',
 '118/76', '75.0', '1.78',
 DATE_SUB(CURDATE(), INTERVAL 4 DAY), '45 min', 5, 27);

-- ==============================================================================
-- 17. ODONTOGRAMA (dentes) — vinculados aos prontuários dentistas
--     Prontuário dentista IDs: 1-3 (org1), 4-6 (org2)
-- ==============================================================================
INSERT INTO prontuario_dentista_dente (prontdentd_prontuario, prontdentd_numero_fdi, prontdentd_status, prontdentd_observacao) VALUES
-- Prontuário 1 (Consulta 14 — Maria Santos — Ortodontia)
(1, 11, 'sadio', NULL),
(1, 12, 'sadio', NULL),
(1, 21, 'sadio', NULL),
(1, 22, 'sadio', 'Leve apinhamento'),
(1, 31, 'sadio', NULL),
(1, 41, 'sadio', NULL),
-- Prontuário 4 (Consulta 25 — Marcos Souza — Canal)
(4, 36, 'canal',    'Necrose pulpar — tratamento endodôntico realizado'),
(4, 35, 'obturado', 'Restauração antiga em amálgama'),
(4, 37, 'cariado',  'Cárie oclusal pequena — monitorar'),
(4, 46, 'sadio',    NULL),
-- Prontuário 5 (Consulta 26 — Camila — Restauração)
(5, 46, 'obturado', 'Restauração em resina A2 — pós canal'),
(5, 45, 'sadio',    NULL),
(5, 47, 'sadio',    NULL),
-- Prontuário 6 (Consulta 27 — Lucas — Cárie profunda)
(6, 15, 'obturado', 'Restauração profunda em resina com proteção pulpar'),
(6, 14, 'sadio',    NULL),
(6, 16, 'sadio',    NULL);

-- ==============================================================================
-- FIM DO SCRIPT
-- ==============================================================================
SELECT '===============================================' AS '';
SELECT 'DADOS MULTI-TENANT INSERIDOS COM SUCESSO!' AS Resultado;
SELECT '===============================================' AS '';
SELECT 'Organizações: 2 (Clínica Saúde Total + Odonto Smile)' AS Tabela;
SELECT 'Endereços: 19' AS Tabela;
SELECT 'Usuários: 10 (1 SuperAdmin + 5 Org1 + 4 Org2)' AS Tabela;
SELECT 'Admin Organizações: 2' AS Tabela;
SELECT 'Secretárias: 2' AS Tabela;
SELECT 'Profissionais: 5 (2 médicos + 1 dentista Org1 | 1 médico + 1 dentista Org2)' AS Tabela;
SELECT 'Pacientes: 12 (6 Org1 + 6 Org2)' AS Tabela;
SELECT 'Consultas: 30 (19 Org1 + 11 Org2) — DATAS DINÂMICAS' AS Tabela;
SELECT 'Histórico Consultas: 19' AS Tabela;
SELECT 'Prontuários Médicos: 10' AS Tabela;
SELECT 'Prontuários Dentista: 6' AS Tabela;
SELECT 'Odontograma (dentes): 16' AS Tabela;
SELECT '===============================================' AS '';
SELECT 'PLANOS DE ASSINATURA:' AS '';
SELECT '  Org 1: Plano PROFISSIONAL (R$ 449/mês)' AS Detalhes;
SELECT '  Org 2: Plano STARTER (R$ 249/mês)' AS Detalhes;
SELECT '  Cobranças pagas: 3' AS Detalhes;
SELECT '===============================================' AS '';
SELECT 'CREDENCIAIS DE TESTE:' AS '';
SELECT '  Super Admin:  superadmin / 123456' AS '';
SELECT '  Admin Org1:   adm / 123456' AS '';
SELECT '  Admin Org2:   admin@odontosmile.com.br / 123456' AS '';
SELECT '  Médico Org1:  med / 123456' AS '';
SELECT '  Médico Org2:  dr.pedro@odontosmile.com.br / 123456' AS '';
SELECT '  Dentista Org1: dr.ricardo@saudetotal.com.br / 123456' AS '';
SELECT '  Dentista Org2: dra.ana@odontosmile.com.br / 123456' AS '';
SELECT '===============================================' AS '';
