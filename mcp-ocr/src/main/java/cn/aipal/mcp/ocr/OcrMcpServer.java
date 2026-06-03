package cn.aipal.mcp.ocr;

import cn.aipal.mcp.common.server.BaseMcpServer;
import cn.aipal.mcp.ocr.tools.OcrTools;
import io.modelcontextprotocol.server.McpSyncServer;

/**
 * AIPal MCP OCR Server 入口
 *
 * 用法：
 *   1. java -jar mcp-ocr.jar
 *   2. Claude Desktop 配置:
 *      { "mcpServers": { "aipal-ocr": {
 *          "command": "java", "args": ["-jar", "/path/to/mcp-ocr.jar"]
 *      }}}
 */
public class OcrMcpServer extends BaseMcpServer {

    public OcrMcpServer() {
        super("aipal-ocr", "0.1.0");
    }

    public static void main(String[] args) throws Exception {
        new OcrMcpServer().start();
    }

    @Override
    protected void registerTools(McpSyncServer server) {
        for (var tool : OcrTools.all()) {
            server.addTool(tool);
        }
    }
}
