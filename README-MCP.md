# MySQL MCP Server Setup

## 🚀 Configuração do Servidor MCP para MySQL

Este servidor MCP permite que você interaja com seu banco de dados MySQL local através do protocolo MCP.

### 📋 Pré-requisitos

1. **Python 3.8+**
2. **MySQL Server** instalado e rodando localmente
3. **Banco de dados `saudeconecta` criado**

### 🔧 Instalação

1. **Instale as dependências Python:**
```bash
pip install -r requirements-mcp.txt
```

2. **Configure as credenciais do banco de dados:**
   - Edite o arquivo `.env-mcp`
   - Substitua `sua_senha_aqui` pela sua senha do MySQL

3. **Crie o banco de dados (se ainda não existir):**
```sql
CREATE DATABASE saudeconecta;
```

### 🚀 Iniciar o Servidor MCP

```bash
python mysql-mcp-server.py
```

### 📋 Funcionalidades Disponíveis

#### 🛠️ **Tools (Ferramentas)**

1. **`execute_sql` - Executar queries SQL
2. **`get_tables` - Listar todas as tabelas
3. **describe_table` - Verificar estrutura de uma tabela
4. `connect_database` - Conectar ao banco
5. `disconnect_database` - Desconectar do banco

#### 📁 **Resources (Recursos)**

- Lista automática de todas as tabelas do banco como recursos MCP
- Cada tabela pode ser acessada via `mysql://database/{nome_tabela}`

### 💡 Exemplo de Uso

#### Conectar ao banco:
```json
{
  "method": "tools/call",
  "params": {
    "name": "connect_database",
    "arguments": {}
  }
}
```

#### Listar tabelas:
```json
{
  "method": "tools/call",
  "params": {
    "name": "get_tables",
    "arguments": {}
  }
}
```

#### Executar query:
```json
{
  "method": "tools/call",
  "params": {
    "name": "execute_sql",
    "arguments": {
      "query": "SELECT * FROM administrador LIMIT 5;"
    }
  }
}
```

#### Verificar estrutura da tabela:
```json
{
  "method": "tools/call",
  "params": {
    "name": "describe_table",
    "arguments": {
      "table_name": "administrador"
    }
  }
}
```

### 🔧 Configuração do Cliente MCP

Para conectar este servidor MCP ao seu cliente MCP, você precisará:

1. **Arquivo de configuração do cliente MCP** (ex: `mcp-config.json`):

```json
{
  "mcpServers": {
    "mysql": {
      "command": "python",
      "args": ["mysql-mcp-server.py"],
      "cwd": "C:\\Users\\luixc\\OneDrive\\Documentos\\Saas\\saudeConecta_Back-End",
      "env": {
        "DB_HOST": "localhost",
        "DB_PORT": "3306",
        "DB_USER": "root",
        "DB_PASSWORD": "sua_senha_aqui",
        "DB_DATABASE": "saudeconecta"
      }
    }
  }
}
```

### 🛡️ Segurança

- **Nunca exponha suas credenciais do banco de dados em logs ou commits**
- **Use senhas fortes e específicas para o ambiente MCP**
- **Limite o acesso do servidor MCP apenas às operações necessárias**
- **Considere usar SSL/TLS para conexões de produção**

### 🚨 Solução de Problemas

#### Erro de conexão:
- Verifique se o MySQL está rodando
- Confirme as credenciais no arquivo `.env-mcp`
- Verifique se o banco `saudeconecta` existe

#### Erro de permissão:
- Verifique se o usuário MySQL tem permissões no banco de dados
- Confirme se as tabelas existem antes de fazer queries

### 📞 Mais Informações

- Documentação oficial do MCP: https://modelcontextprotocol.io/
- MySQL Connector Python: https://dev.mysql.com/doc/connector-python/en/
- Exemplos de configuração MCP: https://github.com/modelcontextprotocol/servers
