package cn.aipal.mcp.common.server;

import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;

/**
 * MCP Server 抽象基类 - 所有子模块继承
 *
 * 子类只需要：
 * 1. 继承本类
 * 2. 实现 registerTools(server) 抽象方法
 * 3. 在 main() 里调用 start()
 *
 * 例：
 *   public class FaceMcpServer extends BaseMcpServer {
 *       public static void main(String[] args) throws Exception {
 *           new FaceMcpServer("smartjavaai-face", "0.1.0").start();
 *       }
 *       public void registerTools(McpSyncServer server) {
 *           server.addTool(FaceTools.detectFace());
 *       }
 *   }
 */
@Slf4j
public abstract class BaseMcpServer {

    protected final String name;
    protected final String version;

    public BaseMcpServer(String name, String version) {
        this.name = name;
        this.version = version;
    }

    /**
     * 启动 MCP Server（阻塞主线程，直到进程被 kill）
     */
    public void start() throws Exception {
        log.info("========================================");
        log.info("  {} v{} 启动中...", name, version);
        log.info("========================================");
        log.info("日志: ~/.aipal-mcp/{}-server.log", name);
        log.info("提示: 调试可用 npx @modelcontextprotocol/inspector");

        StdioServerTransportProvider transport = new StdioServerTransportProvider();

        McpSyncServer server = McpServer.sync(transport)
                .serverInfo(name, version)
                .capabilities(McpSchema.ServerCapabilities.builder()
                        .tools(true)
                        .build())
                .build();

        // 子类注册自己的 Tools
        registerTools(server);

        log.info("========================================");
        log.info("  MCP Server 已启动，等待客户端连接...");
        log.info("========================================");

        // 注册 shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("正在关闭 MCP Server...");
            try {
                server.close();
            } catch (Exception e) {
                log.error("关闭失败", e);
            }
        }));

        // 阻塞主线程
        Thread.currentThread().join();
    }

    /**
     * 子类实现：注册自己模块的 Tools
     */
    protected abstract void registerTools(McpSyncServer server);
}
