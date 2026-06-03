package cn.aipal.mcp.common.config;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

/**
 * YAML 配置加载器
 * 子模块在自己的 application.yml 里加 ModelConfig 字段即可
 */
public class ConfigLoader {

    public static <T> T load(String resourcePath, Class<T> clazz) {
        try (InputStream in = ConfigLoader.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IllegalStateException("未找到配置文件: " + resourcePath);
            }
            Yaml yaml = new Yaml();
            Map<String, Object> map = yaml.load(in);
            // 用 Jackson 做 map → object
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.convertValue(map, clazz);
        } catch (Exception e) {
            throw new RuntimeException("加载配置失败: " + e.getMessage(), e);
        }
    }
}
