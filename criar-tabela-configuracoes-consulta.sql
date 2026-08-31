-- Execute este SQL manualmente no banco de dados 'office'
-- para criar a tabela configuracoes_consulta

USE office;

CREATE TABLE IF NOT EXISTS configuracoes_consulta (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organizacao_id BIGINT NOT NULL,
    pular_para_agendado BOOLEAN NOT NULL DEFAULT FALSE,
    descricao VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_organizacao_fluxo UNIQUE (organizacao_id),
    CONSTRAINT fk_config_fluxo_organizacao FOREIGN KEY (organizacao_id) REFERENCES organizacao(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_config_fluxo_organizacao ON configuracoes_consulta(organizacao_id);