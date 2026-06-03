package cn.aipal.mcp.common.config;

import lombok.Data;

/**
 * 模型配置基类
 * 子模块可继承扩展（如 OcrConfig 加 pHash 阈值等）
 */
@Data
public class ModelConfig {
    /** 模型根目录（默认 ./models/，可通过环境变量 SMARTJAVAAI_MODEL_PATH 覆盖）*/
    private String basePath = "./models/";
    /** 是否使用 GPU（false=CPU）*/
    private boolean useGpu = false;
    /** GPU 设备 ID（仅 useGpu=true 时生效）*/
    private int gpuId = 0;
    /** 推理超时（毫秒）*/
    private long timeout = 60_000;
}
