-- ==========================================
-- SCRIPT PARA POPULAR BANCO DE DADOS OFFICE
-- Execute no MySQL: source insert-dados-iniciais.sql
-- Senha dos usuarios: 123456 (BCrypt hash)
-- ==========================================

USE office;

-- ==========================================
-- 15 USUARIOS
-- ==========================================
INSERT INTO usuarios (login, senha, TipoUsuario, status) VALUES
('admin@saudeconecta.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqBuBjZxJmGHV0F7.VlQJvJGJzHGO', 1, 1),
('medico1@saudeconecta.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqBuBjZxJmGHV0F7.VlQJvJGJzHGO', 2, 1),
('medico2@saudeconecta.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqBuBjZxJmGHV0F7.VlQJvJGJzHGO', 2, 1),
('medico3@saudeconecta.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqBuBjZxJmGHV0F7.VlQJvJGJzHGO', 2, 1),
('medico4@saudeconecta.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqBuBjZxJmGHV0F7.VlQJvJGJzHGO', 2, 1),
('medico5@saudeconecta.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqBuBjZxJmGHV0F7.VlQJvJGJzHGO', 2, 1),
('medico6@saudeconecta.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqBuBjZxJmGHV0F7.VlQJvJGJzHGO', 2, 1),
('medico7@saudeconecta.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqBuBjZxJmGHV0F7.VlQJvJGJzHGO', 2, 1),
('medico8@saudeconecta.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqBuBjZxJmGHV0F7.VlQJvJGJzHGO', 2, 1),
('medico9@saudeconecta.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqBuBjZxJmGHV0F7.VlQJvJGJzHGO', 2, 1),
('medico10@saudeconecta.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqBuBjZxJmGHV0F7.VlQJvJGJzHGO', 2, 1),
('medico11@saudeconecta.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqBuBjZxJmGHV0F7.VlQJvJGJzHGO', 2, 1),
('medico12@saudeconecta.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqBuBjZxJmGHV0F7.VlQJvJGJzHGO', 2, 1),
('medico13@saudeconecta.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqBuBjZxJmGHV0F7.VlQJvJGJzHGO', 2, 1),
('medico14@saudeconecta.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqBuBjZxJmGHV0F7.VlQJvJGJzHGO', 2, 1);

-- ==========================================
-- 15 ENDERECOS
-- ==========================================
INSERT INTO endereco (EndNacionalidade, EndUF, EndMunicipio, EndBairro, EndCep, EndRua, EndNumero, EndComplemento) VALUES
('Brasileira', 'SP', 'São Paulo', 'Centro', '01001-000', 'Rua da Consolação', 100, 'Apto 101'),
('Brasileira', 'SP', 'São Paulo', 'Jardins', '01402-000', 'Av. Paulista', 200, 'Sala 202'),
('Brasileira', 'RJ', 'Rio de Janeiro', 'Copacabana', '22041-080', 'Av. Atlântica', 300, 'Cobertura'),
('Brasileira', 'MG', 'Belo Horizonte', 'Savassi', '30130-000', 'Rua Pernambuco', 400, ''),
('Brasileira', 'RS', 'Porto Alegre', 'Moinhos de Vento', '90510-000', 'Rua Padre Chagas', 500, 'Casa'),
('Brasileira', 'PR', 'Curitiba', 'Batel', '80420-090', 'Av. do Batel', 600, 'Loja 1'),
('Brasileira', 'SC', 'Florianópolis', 'Centro', '88010-000', 'Rua Felipe Schmidt', 700, ''),
('Brasileira', 'BA', 'Salvador', 'Barra', '40140-130', 'Av. Oceânica', 800, 'Apto 301'),
('Brasileira', 'PE', 'Recife', 'Boa Viagem', '51020-010', 'Av. Boa Viagem', 900, ''),
('Brasileira', 'CE', 'Fortaleza', 'Meireles', '60165-121', 'Av. Beira Mar', 1000, 'Bloco A'),
('Brasileira', 'GO', 'Goiânia', 'Setor Bueno', '74215-010', 'Rua T-37', 1100, ''),
('Brasileira', 'DF', 'Brasília', 'Asa Sul', '70390-010', 'SQS 308 Bloco A', 1200, 'Apto 201'),
('Brasileira', 'AM', 'Manaus', 'Adrianópolis', '69057-060', 'Av. Mário Ypiranga', 1300, ''),
('Brasileira', 'PA', 'Belém', 'Nazaré', '66035-170', 'Av. Nazaré', 1400, 'Casa 2'),
('Brasileira', 'ES', 'Vitória', 'Praia do Canto', '29055-500', 'Rua Aleixo Netto', 1500, '');

-- ==========================================
-- 15 MEDICOS
-- ==========================================
INSERT INTO medico (MedNome, MedSexo, MedDataNacimento, MedCrm, MedCpf, MedRg, MedEmail, MedTelefone, MedEspecialidade, MedFormacoes, MedEmpresa, MedGraduacao, Usuario, Endereco, MedTempoDeConsulta) VALUES
('Dr. Carlos Alberto Silva', 'Masculino', '1975-03-15', 'SP-123456', '111.222.333-44', '12.345.678-9', 'medico1@saudeconecta.com', '(11) 99999-0001', 'Cardiologia', 'USP', 'Hospital São Paulo', 'Medicina', 2, 1, '30'),
('Dra. Maria Fernanda Costa', 'Feminino', '1980-07-22', 'SP-234567', '222.333.444-55', '23.456.789-0', 'medico2@saudeconecta.com', '(11) 99999-0002', 'Dermatologia', 'UNICAMP', 'Clínica Derma', 'Medicina', 3, 2, '20'),
('Dr. João Pedro Santos', 'Masculino', '1978-11-08', 'RJ-345678', '333.444.555-66', '34.567.890-1', 'medico3@saudeconecta.com', '(21) 99999-0003', 'Ortopedia', 'UFRJ', 'Hospital Ortoped', 'Medicina', 4, 3, '40'),
('Dra. Ana Paula Oliveira', 'Feminino', '1982-05-30', 'MG-456789', '444.555.666-77', '45.678.901-2', 'medico4@saudeconecta.com', '(31) 99999-0004', 'Pediatria', 'UFMG', 'Hospital Infantil', 'Medicina', 5, 4, '25'),
('Dr. Roberto Mendes', 'Masculino', '1970-09-12', 'RS-567890', '555.666.777-88', '56.789.012-3', 'medico5@saudeconecta.com', '(51) 99999-0005', 'Neurologia', 'UFRGS', 'Clínica Neuro', 'Medicina', 6, 5, '45'),
('Dra. Juliana Ferreira', 'Feminino', '1985-01-25', 'PR-678901', '666.777.888-99', '67.890.123-4', 'medico6@saudeconecta.com', '(41) 99999-0006', 'Ginecologia', 'UFPR', 'Clínica da Mulher', 'Medicina', 7, 6, '30'),
('Dr. Fernando Lima', 'Masculino', '1973-06-18', 'SC-789012', '777.888.999-00', '78.901.234-5', 'medico7@saudeconecta.com', '(48) 99999-0007', 'Urologia', 'UFSC', 'Hospital Urológico', 'Medicina', 8, 7, '35'),
('Dra. Patrícia Rocha', 'Feminino', '1988-12-03', 'BA-890123', '888.999.000-11', '89.012.345-6', 'medico8@saudeconecta.com', '(71) 99999-0008', 'Oftalmologia', 'UFBA', 'Clínica Visão', 'Medicina', 9, 8, '20'),
('Dr. Marcos Almeida', 'Masculino', '1976-04-27', 'PE-901234', '999.000.111-22', '90.123.456-7', 'medico9@saudeconecta.com', '(81) 99999-0009', 'Psiquiatria', 'UFPE', 'Centro Saúde Mental', 'Medicina', 10, 9, '50'),
('Dra. Camila Souza', 'Feminino', '1990-08-14', 'CE-012345', '000.111.222-33', '01.234.567-8', 'medico10@saudeconecta.com', '(85) 99999-0010', 'Endocrinologia', 'UFC', 'Hospital Endócrino', 'Medicina', 11, 10, '30'),
('Dr. Ricardo Pereira', 'Masculino', '1979-02-19', 'GO-112233', '112.233.445-56', '11.223.344-5', 'medico11@saudeconecta.com', '(62) 99999-0011', 'Gastroenterologia', 'UFG', 'Clínica Gastro', 'Medicina', 12, 11, '35'),
('Dra. Beatriz Martins', 'Feminino', '1983-10-05', 'DF-223344', '223.344.556-67', '22.334.455-6', 'medico12@saudeconecta.com', '(61) 99999-0012', 'Pneumologia', 'UnB', 'Hospital Pulmonar', 'Medicina', 13, 12, '40'),
('Dr. André Barbosa', 'Masculino', '1977-07-11', 'AM-334455', '334.455.667-78', '33.445.566-7', 'medico13@saudeconecta.com', '(92) 99999-0013', 'Reumatologia', 'UFAM', 'Clínica Reuma', 'Medicina', 14, 13, '30'),
('Dra. Luciana Dias', 'Feminino', '1986-03-28', 'PA-445566', '445.566.778-89', '44.556.677-8', 'medico14@saudeconecta.com', '(91) 99999-0014', 'Oncologia', 'UFPA', 'Hospital Oncológico', 'Medicina', 15, 14, '45'),
('Dr. Paulo Henrique', 'Masculino', '1971-12-09', 'ES-556677', '556.677.889-90', '55.667.788-9', 'medico15@email.com', '(27) 99999-0015', 'Infectologia', 'UFES', 'Hospital Tropical', 'Medicina', NULL, 15, '35');

-- ==========================================
-- 15 PACIENTES
-- ==========================================
INSERT INTO paciente (PaciNome, PaciSexo, PaciDataNacimento, PaciCpf, PaciRg, PaciEmail, PaciTelefone, Endereco, PaciStatus) VALUES
('José da Silva', 'Masculino', '1985-05-10', '123.456.789-00', '12.345.678-X', 'jose.silva@email.com', '(11) 98888-0001', 1, 'ATIVO'),
('Maria Santos', 'Feminino', '1990-08-15', '234.567.890-11', '23.456.789-1', 'maria.santos@email.com', '(11) 98888-0002', 2, 'ATIVO'),
('Pedro Oliveira', 'Masculino', '1978-03-22', '345.678.901-22', '34.567.890-2', 'pedro.oliveira@email.com', '(21) 98888-0003', 3, 'ATIVO'),
('Ana Costa', 'Feminino', '1995-11-30', '456.789.012-33', '45.678.901-3', 'ana.costa@email.com', '(31) 98888-0004', 4, 'ATIVO'),
('Carlos Ferreira', 'Masculino', '1982-07-18', '567.890.123-44', '56.789.012-4', 'carlos.ferreira@email.com', '(51) 98888-0005', 5, 'ATIVO'),
('Juliana Lima', 'Feminino', '1988-01-25', '678.901.234-55', '67.890.123-5', 'juliana.lima@email.com', '(41) 98888-0006', 6, 'ATIVO'),
('Roberto Almeida', 'Masculino', '1970-09-12', '789.012.345-66', '78.901.234-6', 'roberto.almeida@email.com', '(48) 98888-0007', 7, 'ATIVO'),
('Fernanda Rocha', 'Feminino', '1992-04-08', '890.123.456-77', '89.012.345-7', 'fernanda.rocha@email.com', '(71) 98888-0008', 8, 'ATIVO'),
('Marcos Souza', 'Masculino', '1975-12-03', '901.234.567-88', '90.123.456-8', 'marcos.souza@email.com', '(81) 98888-0009', 9, 'ATIVO'),
('Camila Pereira', 'Feminino', '1998-06-20', '012.345.678-99', '01.234.567-9', 'camila.pereira@email.com', '(85) 98888-0010', 10, 'ATIVO'),
('Ricardo Martins', 'Masculino', '1980-02-14', '111.222.333-00', '11.222.333-0', 'ricardo.martins@email.com', '(62) 98888-0011', 11, 'ATIVO'),
('Beatriz Gomes', 'Feminino', '1993-10-28', '222.333.444-11', '22.333.444-1', 'beatriz.gomes@email.com', '(61) 98888-0012', 12, 'ATIVO'),
('André Barbosa', 'Masculino', '1977-05-05', '333.444.555-22', '33.444.555-2', 'andre.barbosa@email.com', '(92) 98888-0013', 13, 'ATIVO'),
('Luciana Dias', 'Feminino', '1986-08-17', '444.555.666-33', '44.555.666-3', 'luciana.dias@email.com', '(91) 98888-0014', 14, 'ATIVO'),
('Paulo Henrique', 'Masculino', '1972-11-22', '555.666.777-44', '55.666.777-4', 'paulo.henrique@email.com', '(27) 98888-0015', 15, 'ATIVO');

-- ==========================================
-- 15 ADMINISTRADORES
-- ==========================================
INSERT INTO administrador (AdmNome, AdmStatus, AdmDataCriacao, AdmEmail, AdmCodigoAtorizacao, AdmUsuario) VALUES
('Administrador Master', 1, '2024-01-01', 'admin@saudeconecta.com', 'ADM001', 1),
('Ana Secretária', 1, '2024-01-15', 'ana.secretaria@saudeconecta.com', 'ADM002', NULL),
('Carlos Gestor', 1, '2024-02-01', 'carlos.gestor@saudeconecta.com', 'ADM003', NULL),
('Maria Coordenadora', 1, '2024-02-15', 'maria.coord@saudeconecta.com', 'ADM004', NULL),
('João Supervisor', 1, '2024-03-01', 'joao.super@saudeconecta.com', 'ADM005', NULL),
('Fernanda Atendente', 1, '2024-03-15', 'fernanda.atend@saudeconecta.com', 'ADM006', NULL),
('Roberto Financeiro', 1, '2024-04-01', 'roberto.fin@saudeconecta.com', 'ADM007', NULL),
('Patrícia RH', 1, '2024-04-15', 'patricia.rh@saudeconecta.com', 'ADM008', NULL),
('Marcos TI', 1, '2024-05-01', 'marcos.ti@saudeconecta.com', 'ADM009', NULL),
('Camila Recepção', 1, '2024-05-15', 'camila.recepcao@saudeconecta.com', 'ADM010', NULL),
('Ricardo Operacional', 1, '2024-06-01', 'ricardo.op@saudeconecta.com', 'ADM011', NULL),
('Beatriz Qualidade', 1, '2024-06-15', 'beatriz.qual@saudeconecta.com', 'ADM012', NULL),
('André Logística', 1, '2024-07-01', 'andre.log@saudeconecta.com', 'ADM013', NULL),
('Luciana Compras', 1, '2024-07-15', 'luciana.compras@saudeconecta.com', 'ADM014', NULL),
('Paulo Diretor', 1, '2024-08-01', 'paulo.diretor@saudeconecta.com', 'ADM015', NULL);

-- ==========================================
-- 15 CONSULTAS
-- ==========================================
INSERT INTO consulta (ConMedico, ConPaciente, ConDia_semana, ConHorario, ConData, ConObservacoes, ConDataCriacao, ConFormaPagamento, ConStatus, ConAdm) VALUES
(1, 1, 'Segunda-feira', '08:00', '2025-01-06', 'Consulta de rotina cardiológica', '2024-12-20', 1, 1, 1),
(2, 2, 'Segunda-feira', '09:00', '2025-01-06', 'Avaliação dermatológica', '2024-12-20', 2, 1, 1),
(3, 3, 'Terça-feira', '10:00', '2025-01-07', 'Dor no joelho direito', '2024-12-21', 1, 1, 2),
(4, 4, 'Terça-feira', '11:00', '2025-01-07', 'Consulta pediátrica de acompanhamento', '2024-12-21', 3, 1, 2),
(5, 5, 'Quarta-feira', '08:30', '2025-01-08', 'Avaliação neurológica', '2024-12-22', 1, 1, 3),
(6, 6, 'Quarta-feira', '09:30', '2025-01-08', 'Exame ginecológico de rotina', '2024-12-22', 2, 1, 3),
(7, 7, 'Quinta-feira', '10:30', '2025-01-09', 'Consulta urológica', '2024-12-23', 1, 1, 4),
(8, 8, 'Quinta-feira', '11:30', '2025-01-09', 'Exame oftalmológico', '2024-12-23', 3, 1, 4),
(9, 9, 'Sexta-feira', '08:00', '2025-01-10', 'Acompanhamento psiquiátrico', '2024-12-24', 1, 1, 5),
(10, 10, 'Sexta-feira', '09:00', '2025-01-10', 'Avaliação endocrinológica', '2024-12-24', 2, 1, 5),
(11, 11, 'Segunda-feira', '10:00', '2025-01-13', 'Consulta gastroenterológica', '2024-12-25', 1, 2, 6),
(12, 12, 'Segunda-feira', '11:00', '2025-01-13', 'Avaliação pulmonar', '2024-12-25', 3, 2, 6),
(13, 13, 'Terça-feira', '08:30', '2025-01-14', 'Consulta reumatológica', '2024-12-26', 1, 2, 7),
(14, 14, 'Terça-feira', '09:30', '2025-01-14', 'Acompanhamento oncológico', '2024-12-26', 2, 2, 7),
(15, 15, 'Quarta-feira', '10:30', '2025-01-15', 'Avaliação infectológica', '2024-12-27', 1, 2, 8);

-- ==========================================
-- 15 CONSULTA STATUS (Histórico)
-- ==========================================
INSERT INTO consultastatus (ConSttMedico, ConSttPaciente, ConSttDia_semana, ConSttHorario, ConSttData, ConSttObservacao, ConSttDataCriacao, ConSttFormaPagamento, ConSttStatus, ConSttAdm) VALUES
(1, 1, 'Segunda-feira', '08:00', '2024-12-02', 'Consulta realizada com sucesso', '2024-11-20', 1, 3, 1),
(2, 2, 'Segunda-feira', '09:00', '2024-12-02', 'Paciente não compareceu', '2024-11-20', 2, 4, 1),
(3, 3, 'Terça-feira', '10:00', '2024-12-03', 'Consulta realizada', '2024-11-21', 1, 3, 2),
(4, 4, 'Terça-feira', '11:00', '2024-12-03', 'Consulta realizada', '2024-11-21', 3, 3, 2),
(5, 5, 'Quarta-feira', '08:30', '2024-12-04', 'Consulta cancelada pelo paciente', '2024-11-22', 1, 5, 3),
(6, 6, 'Quarta-feira', '09:30', '2024-12-04', 'Consulta realizada', '2024-11-22', 2, 3, 3),
(7, 7, 'Quinta-feira', '10:30', '2024-12-05', 'Consulta realizada', '2024-11-23', 1, 3, 4),
(8, 8, 'Quinta-feira', '11:30', '2024-12-05', 'Consulta realizada', '2024-11-23', 3, 3, 4),
(9, 9, 'Sexta-feira', '08:00', '2024-12-06', 'Consulta remarcada', '2024-11-24', 1, 2, 5),
(10, 10, 'Sexta-feira', '09:00', '2024-12-06', 'Consulta realizada', '2024-11-24', 2, 3, 5),
(11, 11, 'Segunda-feira', '10:00', '2024-12-09', 'Consulta realizada', '2024-11-25', 1, 3, 6),
(12, 12, 'Segunda-feira', '11:00', '2024-12-09', 'Consulta realizada', '2024-11-25', 3, 3, 6),
(13, 13, 'Terça-feira', '08:30', '2024-12-10', 'Paciente não compareceu', '2024-11-26', 1, 4, 7),
(14, 14, 'Terça-feira', '09:30', '2024-12-10', 'Consulta realizada', '2024-11-26', 2, 3, 7),
(15, 15, 'Quarta-feira', '10:30', '2024-12-11', 'Consulta realizada', '2024-11-26', 1, 3, 8);

-- ==========================================
-- FIM DO SCRIPT
-- ==========================================
SELECT 'Dados inseridos com sucesso!' AS Resultado;
SELECT 'Usuarios: 15' AS Tabela;
SELECT 'Enderecos: 15' AS Tabela;
SELECT 'Medicos: 15' AS Tabela;
SELECT 'Pacientes: 15' AS Tabela;
SELECT 'Administradores: 15' AS Tabela;
SELECT 'Consultas: 15' AS Tabela;
SELECT 'ConsultaStatus: 15' AS Tabela;
