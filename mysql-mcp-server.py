#!/usr/bin/env python3
"""
MCP Server for MySQL Database Connection
"""

import asyncio
import json
import logging
from typing import Any, Dict, List

import mysql.connector
from mcp.server import Server
from mcp.server.models import InitializationOptions
from mcp.server.lowlevel import NotificationOptions
from mcp.server.stdio import stdio_server
from mcp.types import Resource, Tool, TextContent

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("mysql-mcp-server")


class MySQLMCPServer:
    def __init__(self, db_config: Dict[str, str]):
        self.db_config = db_config
        self.connection = None

    async def connect(self) -> bool:
        try:
            self.connection = mysql.connector.connect(
                host=self.db_config.get("host", "localhost"),
                port=int(self.db_config.get("port", "3306")),
                user=self.db_config.get("user", "root"),
                password=self.db_config.get("password", ""),
                database=self.db_config.get("database", "office"),
            )
            logger.info("Connected to MySQL")
            return True
        except Exception as e:
            logger.error(f"MySQL connection failed: {e}")
            return False

    async def disconnect(self):
        if self.connection and self.connection.is_connected():
            self.connection.close()
            logger.info("Disconnected from MySQL")

    async def execute_query(self, query: str) -> List[Dict[str, Any]]:
        if not self.connection or not self.connection.is_connected():
            if not await self.connect():
                raise Exception("Database connection failed")

        cursor = self.connection.cursor(dictionary=True)
        cursor.execute(query)

        if query.strip().upper().startswith(("SELECT", "SHOW", "DESCRIBE", "EXPLAIN")):
            results = cursor.fetchall()
        else:
            self.connection.commit()
            results = [{"affected_rows": cursor.rowcount}]

        cursor.close()
        return results

    async def get_tables(self) -> List[str]:
        rows = await self.execute_query("SHOW TABLES")
        return [list(r.values())[0] for r in rows]

    async def get_table_structure(self, table_name: str) -> List[Dict[str, Any]]:
        return await self.execute_query(f"DESCRIBE {table_name}")


mysql_server = None


async def main():
    global mysql_server

    db_config = {
        "host": "localhost",
        "port": "3306",
        "user": "root",
        "password": "root",
        "database": "office",
    }

    mysql_server = MySQLMCPServer(db_config)
    server = Server("mysql-mcp-server", "1.0.0")

    # ---------------- RESOURCES ----------------
    @server.list_resources()
    async def list_resources() -> List[Resource]:
        tables = await mysql_server.get_tables()
        return [
            Resource(
                uri=f"mysql://database/{t}",
                name=f"Table: {t}",
                description=f"MySQL table {t}",
                mimeType="text/plain",
            )
            for t in tables
        ]

    # ---------------- TOOLS ----------------
    @server.list_tools()
    async def list_tools() -> List[Tool]:
        return [
            Tool(
                name="execute_sql",
                description="Execute SQL query",
                inputSchema={
                    "type": "object",
                    "properties": {"query": {"type": "string"}},
                    "required": ["query"],
                },
            ),
            Tool(
                name="get_tables",
                description="List all tables",
                inputSchema={"type": "object", "properties": {}},
            ),
            Tool(
                name="describe_table",
                description="Describe a table",
                inputSchema={
                    "type": "object",
                    "properties": {"table_name": {"type": "string"}},
                    "required": ["table_name"],
                },
            ),
            Tool(
                name="connect_database",
                description="Connect to database",
                inputSchema={"type": "object", "properties": {}},
            ),
            Tool(
                name="disconnect_database",
                description="Disconnect database",
                inputSchema={"type": "object", "properties": {}},
            ),
        ]

    # ---------------- TOOL EXECUTION ----------------
    @server.call_tool()
    async def call_tool(name: str, arguments: Dict[str, Any]):
        try:
            if name == "execute_sql":
                results = await mysql_server.execute_query(arguments["query"])
                return [TextContent(type="text", text=json.dumps(results, indent=2, default=str))]

            if name == "get_tables":
                tables = await mysql_server.get_tables()
                return [TextContent(type="text", text=json.dumps(tables, indent=2))]

            if name == "describe_table":
                structure = await mysql_server.get_table_structure(arguments["table_name"])
                return [TextContent(type="text", text=json.dumps(structure, indent=2, default=str))]

            if name == "connect_database":
                ok = await mysql_server.connect()
                return [TextContent(type="text", text="Connected" if ok else "Connection failed")]

            if name == "disconnect_database":
                await mysql_server.disconnect()
                return [TextContent(type="text", text="Disconnected")]

            return [TextContent(type="text", text=f"Unknown tool {name}")]

        except Exception as e:
            logger.exception("Tool error")
            return [TextContent(type="text", text=f"Error: {str(e)}")]

    # ---------------- SERVER START ----------------
    async with stdio_server() as (read_stream, write_stream):
        await server.run(
            read_stream,
            write_stream,
            InitializationOptions(
                server_name="mysql-mcp-server",
                server_version="1.0.0",
                capabilities=server.get_capabilities(
                    notification_options=NotificationOptions(
                        resources_changed=False,
                        tools_changed=False,
                    ),
                    experimental_capabilities={},
                ),
            ),
        )


if __name__ == "__main__":
    asyncio.run(main())
