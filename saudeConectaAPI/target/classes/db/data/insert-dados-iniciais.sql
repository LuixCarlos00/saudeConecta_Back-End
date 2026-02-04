-- ==========================================
-- SCRIPT PARA POPULAR BANCO DE DADOS OFFICE
-- Execute no MySQL: source insert-dados-iniciais.sql
-- Senha dos usuarios: 123456 (BCrypt hash)
-- ==========================================
-- REGRAS:
-- - 5 usuarios (para medicos, administradores, secretarias)
-- - 5 medicos, 5 administradores, 5 secretarias
-- - 15 pacientes
-- - 15 consultas (medicos e pacientes podem repetir)
-- - 15 prontuarios
-- ==========================================

USE office;

-- ==========================================
-- 5 USUARIOS (TipoUsuario: 1=Admin, 2=Medico, 3=Secretaria)
-- ==========================================
INSERT INTO usuarios (login, senha, TipoUsuario, status) VALUES
('admin@saudeconecta.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqBuBjZxJmGHV0F7.VlQJvJGJzHGO', 1, 1),
('medico1@saudeconecta.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqBuBjZxJmGHV0F7.VlQJvJGJzHGO', 2, 1),
('medico2@saudeconecta.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqBuBjZxJmGHV0F7.VlQJvJGJzHGO', 2, 1),
('medico3@saudeconecta.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqBuBjZxJmGHV0F7.VlQJvJGJzHGO', 2, 1),
('secretaria1@saudeconecta.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqBuBjZxJmGHV0F7.VlQJvJGJzHGO', 3, 1);

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
-- 5 MEDICOS
-- ==========================================
INSERT INTO medico (MedNome, MedSexo, MedDataNacimento, MedCrm, MedCpf, MedRg, MedEmail, MedTelefone, MedEspecialidade, MedFormacoes, MedEmpresa, MedGraduacao, Usuario, Endereco, MedTempoDeConsulta) VALUES
('Dr. Carlos Alberto Silva', 'Masculino', '1975-03-15', 'SP-123456', '111.222.333-44', '12.345.678-9', 'medico1@saudeconecta.com', '(11) 99999-0001', 'Cardiologia', 'USP', 'Hospital Sao Paulo', 'Medicina', 2, 1, '30'),
('Dra. Maria Fernanda Costa', 'Feminino', '1980-07-22', 'SP-234567', '222.333.444-55', '23.456.789-0', 'medico2@saudeconecta.com', '(11) 99999-0002', 'Dermatologia', 'UNICAMP', 'Clinica Derma', 'Medicina', 3, 2, '20'),
('Dr. Joao Pedro Santos', 'Masculino', '1978-11-08', 'RJ-345678', '333.444.555-66', '34.567.890-1', 'medico3@saudeconecta.com', '(21) 99999-0003', 'Ortopedia', 'UFRJ', 'Hospital Ortoped', 'Medicina', 4, 3, '40'),
('Dra. Ana Paula Oliveira', 'Feminino', '1982-05-30', 'MG-456789', '444.555.666-77', '45.678.901-2', 'medico4@saudeconecta.com', '(31) 99999-0004', 'Pediatria', 'UFMG', 'Hospital Infantil', 'Medicina', NULL, 4, '25'),
('Dr. Roberto Mendes', 'Masculino', '1970-09-12', 'RS-567890', '555.666.777-88', '56.789.012-3', 'medico5@saudeconecta.com', '(51) 99999-0005', 'Neurologia', 'UFRGS', 'Clinica Neuro', 'Medicina', NULL, 5, '45');

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
-- 5 ADMINISTRADORES
-- ==========================================
INSERT INTO administrador (AdmNome, AdmStatus, AdmDataCriacao, AdmEmail, AdmCodigoAtorizacao, AdmUsuario) VALUES
('Administrador Master', 1, '2024-01-01', 'admin@saudeconecta.com', 'ADM001', 1),
('Ana Gestora', 1, '2024-01-15', 'ana.gestora@saudeconecta.com', 'ADM002', NULL),
('Carlos Gestor', 1, '2024-02-01', 'carlos.gestor@saudeconecta.com', 'ADM003', NULL),
('Maria Coordenadora', 1, '2024-02-15', 'maria.coord@saudeconecta.com', 'ADM004', NULL),
('Joao Supervisor', 1, '2024-03-01', 'joao.super@saudeconecta.com', 'ADM005', NULL);

-- ==========================================
-- 5 SECRETARIAS
-- ==========================================
INSERT INTO secretaria (SecreNome, SecreStatus, SecreDataCriacao, SecreEmail, SecreCodigoAtorizacao, SecreUsuario) VALUES
('Carla Secretaria', 1, '2024-01-10', 'secretaria1@saudeconecta.com', 'SEC001', 5),
('Mariana Atendimento', 1, '2024-01-20', 'mariana.atend@saudeconecta.com', 'SEC002', NULL),
('Priscila Recepcao', 1, '2024-02-05', 'priscila.recepcao@saudeconecta.com', 'SEC003', NULL),
('Debora Agendamento', 1, '2024-02-18', 'debora.agenda@saudeconecta.com', 'SEC004', NULL),
('Renata Cadastro', 1, '2024-03-01', 'renata.cadastro@saudeconecta.com', 'SEC005', NULL);

-- ==========================================
-- 15 CONSULTAS (medicos 1-5 e pacientes 1-15 podem repetir)
-- ==========================================
INSERT INTO consulta (ConMedico, ConPaciente, ConDia_semana, ConHorario, ConData, ConObservacoes, ConDataCriacao, ConFormaPagamento, ConStatus, ConAdm) VALUES
(1, 1, 'Segunda-feira', '08:00', '2024-12-02', 'Consulta de rotina cardiologica', '2024-11-20', 1, 'REALIZADA', 1),
(2, 2, 'Segunda-feira', '09:00', '2024-12-02', 'Avaliacao dermatologica', '2024-11-20', 2, 'REALIZADA', 1),
(3, 3, 'Terca-feira', '10:00', '2024-12-03', 'Dor no joelho', '2024-11-21', 1, 'REALIZADA', 2),
(4, 4, 'Terca-feira', '11:00', '2024-12-03', 'Consulta pediatrica', '2024-11-21', 3, 'REALIZADA', 2),
(5, 5, 'Quarta-feira', '08:30', '2024-12-04', 'Avaliacao neurologica', '2024-11-22', 1, 'CANCELADA', 3),
(1, 6, 'Quarta-feira', '09:30', '2024-12-04', 'Retorno cardiologia', '2024-11-22', 2, 'REALIZADA', 3),
(2, 7, 'Quinta-feira', '10:30', '2024-12-05', 'Manchas na pele', '2024-11-23', 1, 'REALIZADA', 4),
(3, 8, 'Quinta-feira', '11:30', '2024-12-05', 'Fratura no braco', '2024-11-23', 3, 'REALIZADA', 4),
(4, 9, 'Sexta-feira', '08:00', '2024-12-06', 'Vacinacao infantil', '2024-11-24', 1, 'AGENDADA', 5),
(5, 10, 'Sexta-feira', '09:00', '2024-12-06', 'Cefaleia cronica', '2024-11-24', 2, 'REALIZADA', 5),
(1, 11, 'Segunda-feira', '10:00', '2024-12-09', 'Check-up cardiaco', '2024-11-25', 1, 'REALIZADA', 1),
(2, 12, 'Segunda-feira', '11:00', '2024-12-09', 'Alergia cutanea', '2024-11-25', 3, 'REALIZADA', 1),
(3, 13, 'Terca-feira', '08:30', '2024-12-10', 'Lesao esportiva', '2024-11-26', 1, 'FALTOU', 2),
(4, 14, 'Terca-feira', '09:30', '2024-12-10', 'Febre persistente', '2024-11-26', 2, 'REALIZADA', 2),
(5, 15, 'Quarta-feira', '10:30', '2024-12-11', 'Enxaqueca', '2024-11-26', 1, 'REALIZADA', 3);

-- ==========================================
-- 15 PRONTUARIOS
-- ==========================================
INSERT INTO prontuario (prontPeso, prontAltura, prontTemperatura, prontDataNacimento, prontSexo, prontSaturacao, prontHemoglobina, prontPressao, prontFrequenciaRespiratoria, prontFrequenciaArterialSistolica, prontFrequenciaArterialDiastolica, prontObservacao, prontCondulta, prontAnamnese, prontQueixaPricipal, prontDiagnostico, prontModeloPrescricao, prontTituloPrescricao, prontDataPrescricao, prontPrescricao, prontDataFinalizado, prontCodigoMedico, consulta, prontModeloExame, prontTituloExame, prontDataExame, prontExame, prontTempoDuracao) VALUES
('75.5', '1.75', '36.5', '1985-05-10', 'Masculino', '98%', '14.5', '120/80', '18', '120', '80', 'Paciente em bom estado geral', 'Retorno em 30 dias', 'Dores no peito ha 2 semanas', 'Dor toracica', 'Angina estavel', 'Modelo cardiologia', 'Prescricao Cardiologica', '2024-12-02', 'Losartana 50mg - 1x ao dia', '2024-12-02', 1, 1, 'Modelo exame cardiaco', 'Eletrocardiograma', '2024-12-02', 'ECG normal', '30'),
('62.0', '1.65', '36.8', '1990-08-15', 'Feminino', '99%', '13.2', '110/70', '16', '110', '70', 'Manchas na pele', 'Retorno em 15 dias', 'Manchas nos bracos ha 1 mes', 'Lesoes cutaneas', 'Dermatite de contato', 'Modelo dermatologico', 'Prescricao Dermatologica', '2024-12-02', 'Hidrocortisona creme 1%', '2024-12-02', 2, 2, 'Modelo exame pele', 'Exame Dermatologico', '2024-12-02', 'Lesoes eritematosas', '20'),
('85.0', '1.80', '36.6', '1978-03-22', 'Masculino', '97%', '15.0', '130/85', '17', '130', '85', 'Dor no joelho direito', 'Retorno em 45 dias', 'Dor no joelho ha 3 meses', 'Dor articular', 'Lesao meniscal', 'Modelo ortopedico', 'Prescricao Ortopedica', '2024-12-03', 'Ibuprofeno 600mg - 8/8h', '2024-12-03', 3, 3, 'Modelo ortopedico', 'Ressonancia Joelho', '2024-12-03', 'Lesao parcial menisco', '40'),
('28.0', '1.20', '37.2', '2018-11-30', 'Feminino', '98%', '12.0', '90/60', '22', '90', '60', 'Crianca com febre', 'Retorno se febre persistir', 'Febre e coriza ha 2 dias', 'Febre e sintomas gripais', 'IVAS - Infeccao viral', 'Modelo pediatrico', 'Prescricao Pediatrica', '2024-12-03', 'Paracetamol gotas', '2024-12-03', 4, 4, 'Modelo pediatrico', 'Hemograma Completo', '2024-12-03', 'Leucocitose leve', '25'),
('78.0', '1.72', '36.4', '1982-07-18', 'Masculino', '96%', '14.8', '140/90', '19', '140', '90', 'Cefaleia frequente', 'Retorno em 30 dias', 'Dores de cabeca ha 6 meses', 'Cefaleia cronica', 'Enxaqueca sem aura', 'Modelo neurologico', 'Prescricao Neurologica', '2024-12-04', 'Sumatriptano 50mg', '2024-12-04', 5, 5, 'Modelo neurologico', 'Tomografia Cranio', '2024-12-04', 'TC cranio sem alteracoes', '45'),
('58.0', '1.62', '36.7', '1988-01-25', 'Feminino', '99%', '12.8', '115/75', '16', '115', '75', 'Consulta de rotina', 'Retorno anual', 'Exame preventivo anual', 'Consulta preventiva', 'Sem alteracoes', 'Modelo cardiologia', 'Prescricao Cardiologica', '2024-12-04', 'Acido folico 5mg', '2024-12-04', 1, 6, 'Modelo cardiaco', 'Check-up', '2024-12-04', 'Exames normais', '30'),
('82.0', '1.78', '36.5', '1970-09-12', 'Masculino', '97%', '15.2', '135/88', '18', '135', '88', 'Queixas urinarias', 'Retorno em 30 dias', 'Dificuldade para urinar', 'Disuria e polaciuria', 'Hiperplasia prostatica', 'Modelo dermatologico', 'Prescricao Dermatologica', '2024-12-05', 'Tansulosina 0.4mg', '2024-12-05', 2, 7, 'Modelo dermatologico', 'Exame pele', '2024-12-05', 'Sem alteracoes', '35'),
('55.0', '1.60', '36.6', '1992-04-08', 'Feminino', '99%', '13.0', '110/70', '15', '110', '70', 'Queixa visual', 'Retorno em 6 meses', 'Dificuldade para enxergar', 'Baixa acuidade visual', 'Miopia leve', 'Modelo ortopedico', 'Prescricao Ortopedica', '2024-12-05', 'Fisioterapia', '2024-12-05', 3, 8, 'Modelo ortopedico', 'Raio-X', '2024-12-05', 'Sem fraturas', '20'),
('88.0', '1.75', '36.5', '1975-12-03', 'Masculino', '98%', '14.5', '125/82', '17', '125', '82', 'Paciente com ansiedade', 'Retorno em 30 dias', 'Ansiedade e insonia', 'Ansiedade e disturbio sono', 'TAG', 'Modelo pediatrico', 'Prescricao Pediatrica', '2024-12-06', 'Vitaminas', '2024-12-06', 4, 9, 'Modelo pediatrico', 'Hemograma', '2024-12-06', 'Normal', '50'),
('65.0', '1.68', '36.8', '1998-06-20', 'Feminino', '98%', '12.5', '108/68', '16', '108', '68', 'Ganho de peso', 'Retorno em 60 dias', 'Ganho de peso e cansaco', 'Fadiga e ganho ponderal', 'Hipotireoidismo', 'Modelo neurologico', 'Prescricao Neurologica', '2024-12-06', 'Levotiroxina 50mcg', '2024-12-06', 5, 10, 'Modelo neurologico', 'Eletroencefalograma', '2024-12-06', 'Normal', '30'),
('72.0', '1.70', '36.6', '1980-02-14', 'Masculino', '97%', '14.0', '122/78', '17', '122', '78', 'Dor abdominal', 'Retorno em 15 dias', 'Dor epigastrica ha 1 mes', 'Dor epigastrica', 'Gastrite', 'Modelo cardiologia', 'Prescricao Cardiologica', '2024-12-09', 'Omeprazol 20mg', '2024-12-09', 1, 11, 'Modelo cardiaco', 'ECG', '2024-12-09', 'Normal', '35'),
('60.0', '1.58', '36.7', '1993-10-28', 'Feminino', '98%', '13.5', '118/72', '16', '118', '72', 'Tosse persistente', 'Retorno em 30 dias', 'Tosse ha 3 semanas', 'Tosse cronica', 'Bronquite', 'Modelo dermatologico', 'Prescricao Dermatologica', '2024-12-09', 'Ambroxol xarope', '2024-12-09', 2, 12, 'Modelo dermatologico', 'Exame pele', '2024-12-09', 'Normal', '40'),
('80.0', '1.76', '36.5', '1977-05-05', 'Masculino', '96%', '14.2', '128/82', '18', '128', '82', 'Dor nas articulacoes', 'Retorno em 45 dias', 'Dores articulares ha 2 meses', 'Poliartralgia', 'Artrite reumatoide', 'Modelo ortopedico', 'Prescricao Ortopedica', '2024-12-10', 'Metotrexato 15mg', '2024-12-10', 3, 13, 'Modelo ortopedico', 'Raio-X', '2024-12-10', 'Desgaste articular', '30'),
('68.0', '1.64', '36.8', '1986-08-17', 'Feminino', '97%', '11.8', '115/70', '17', '115', '70', 'Fadiga intensa', 'Retorno em 30 dias', 'Cansaco e fraqueza', 'Fadiga cronica', 'Anemia ferropriva', 'Modelo pediatrico', 'Prescricao Pediatrica', '2024-12-10', 'Sulfato ferroso 300mg', '2024-12-10', 4, 14, 'Modelo pediatrico', 'Hemograma', '2024-12-10', 'Hb 10.5 Ferritina baixa', '45'),
('76.0', '1.74', '37.0', '1972-11-22', 'Masculino', '95%', '13.8', '130/85', '19', '130', '85', 'Febre recorrente', 'Retorno em 7 dias', 'Febre ha 5 dias', 'Febre de origem indeterminada', 'Infeccao bacteriana', 'Modelo neurologico', 'Prescricao Neurologica', '2024-12-11', 'Amoxicilina 500mg', '2024-12-11', 5, 15, 'Modelo neurologico', 'Hemograma e PCR', '2024-12-11', 'Leucocitose e PCR elevado', '35');

-- ==========================================
-- FIM DO SCRIPT
-- ==========================================
SELECT 'Dados inseridos com sucesso!' AS Resultado;
SELECT 'Usuarios: 5' AS Tabela;
SELECT 'Enderecos: 15' AS Tabela;
SELECT 'Medicos: 5' AS Tabela;
SELECT 'Pacientes: 15' AS Tabela;
SELECT 'Administradores: 5' AS Tabela;
SELECT 'Secretarias: 5' AS Tabela;
SELECT 'Consultas: 15' AS Tabela;
SELECT 'Prontuarios: 15' AS Tabela;
