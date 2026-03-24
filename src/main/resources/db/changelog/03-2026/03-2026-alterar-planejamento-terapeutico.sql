-- Liquibase changeset para alterar coluna de planejamento terapêutico
-- Tornar a coluna genérica para suportar prontuario médico e dentista

-- Renomear coluna prontuario_dentista_id para prontuario_id_antigo
-- ALTER TABLE tb_planejamento_terapeutico CHANGE COLUMN prontuario_dentista_id prontuario_id_antigo BIGINT;

-- Adicionar nova coluna prontuario_id genérica
-- ALTER TABLE tb_planejamento_terapeutico ADD COLUMN prontuario_id BIGINT;

-- Migrar dados existentes (se houver)
-- UPDATE tb_planejamento_terapeutico SET prontuario_id = prontuario_id_antigo WHERE prontuario_id_antigo IS NOT NULL;

-- Remover coluna antiga
-- ALTER TABLE tb_planejamento_terapeutico DROP COLUMN prontuario_id_antigo;

-- Adicionar constraint para a nova coluna (opcional, para permitir ambos os tipos)
-- ALTER TABLE tb_planejamento_terapeutico ADD CONSTRAINT fk_planejamento_prontuario 
-- FOREIGN KEY (prontuario_id) REFERENCES prontuario(prontCodigoProntuario);
