package cn.aipal.mcp.common.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * Tool 元数据 - 用于动态注册
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ToolInfo {
    /** 工具名（英文，下划线分隔，如 detect_face）*/
    private String name;
    /** 中文显示名（如"人脸检测"）*/
    private String displayName;
    /** 工具描述（会展示给 AI 客户端）*/
    private String description;
    /** 分类：ocr / face / detection / classification / pose / translation*/
    private String category;
    /** 所需模型文件路径（相对 basePath）*/
    private String modelPath;
    /** 标签（用于搜索和过滤）*/
    private String[] tags;
}
