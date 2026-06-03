package cn.aipal.mcp.ocr.config;

import cn.aipal.mcp.common.config.ModelConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * OCR 模块配置 - 继承公共 ModelConfig
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OcrConfig extends ModelConfig {
    /** 检测模型名（PP-OCRv5_server）*/
    private String detModelName = "ch_PP-OCRv5_server_det";
    /** 识别模型名（PP-OCRv5_server）*/
    private String recModelName = "ch_PP-OCRv5_server_rec";
    /** 是否使用方向分类（识别旋转图片）*/
    private boolean useDirection = true;
    /** 单张图最大尺寸（字节）*/
    private long maxImageSize = 10 * 1024 * 1024;
}
