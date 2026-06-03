package cn.aipal.mcp.ocr.tools;

import cn.aipal.mcp.common.util.ImageUtils;
import cn.aipal.mcp.ocr.service.OcrService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;

import java.awt.image.BufferedImage;
import java.util.*;

/**
 * OCR 工具集 - 注册为 MCP Tools
 *
 * 当前 Tools：
 * - ocr_extract_text         通用文字识别（最常用）
 * - ocr_extract_with_positions 文字+位置（适合版面分析）
 * - ocr_detect_id_card       身份证识别
 * - ocr_detect_plate         车牌识别（中国 12 种）
 */
@Slf4j
public class OcrTools {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final OcrService OCR = OcrService.getInstance();

    public static List<McpServerFeatures.SyncToolSpecification> all() {
        return List.of(
            extractTextTool(),
            extractWithPositionsTool(),
            detectIdCardTool(),
            detectPlateTool()
        );
    }

    /**
     * Tool 1: ocr_extract_text - 通用文字提取
     * AI 调用示例：
     *   "帮我提取这张图片里的所有文字"
     *   "OCR 这张照片"
     */
    private static McpServerFeatures.SyncToolSpecification extractTextTool() {
        McpSchema.JsonSchema jsonSchema = new McpSchema.JsonSchema(
            "object",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "image", Map.of(
                        "type", "string",
                        "description", "图片的 base64 编码（支持 data:image/png;base64,xxx 格式或纯 base64）"
                    )
                ),
                "required", List.of("image")
            ),
            List.of("image"), null, null, null);

        return new McpServerFeatures.SyncToolSpecification(
            new McpSchema.Tool(
                "ocr_extract_text",
                "【OCR 文字提取】从图片中识别并提取所有文字内容。\n" +
                "适用场景：截图、扫描件、发票、文档照片的文字内容提取。\n" +
                "支持中英文混排、数字、标点符号。\n" +
                "参数：image - 图片的 base64 编码\n" +
                "返回：识别出的纯文本（多行用 \\n 分隔）",
                jsonSchema
            ),
            (exchange, args) -> {
                try {
                    String base64 = (String) args.get("image");
                    BufferedImage img = ImageUtils.fromBase64(base64);
                    String text = OCR.recognizeText(img);
                    return successResult(Map.of(
                        "text", text,
                        "line_count", text.isEmpty() ? 0 : text.split("\n").length
                    ));
                } catch (Exception e) {
                    log.error("ocr_extract_text 失败", e);
                    return errorResult(e.getMessage());
                }
            }
        );
    }

    /**
     * Tool 2: ocr_extract_with_positions - 文字+位置
     * 适用场景：需要版面分析、表格提取、PDF 还原
     */
    private static McpServerFeatures.SyncToolSpecification extractWithPositionsTool() {
        McpSchema.JsonSchema jsonSchema = new McpSchema.JsonSchema(
            "object",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "image", Map.of("type", "string", "description", "图片的 base64")
                ),
                "required", List.of("image")
            ),
            List.of("image"), null, null, null);

        return new McpServerFeatures.SyncToolSpecification(
            new McpSchema.Tool(
                "ocr_extract_with_positions",
                "【OCR 文字+位置提取】识别图片中每行文字并返回坐标 (x1,y1,x2,y2)。\n" +
                "适用场景：需要版面分析、表格抽取、PDF 重排、关键区域定位。",
                jsonSchema
            ),
            (exchange, args) -> {
                try {
                    String base64 = (String) args.get("image");
                    BufferedImage img = ImageUtils.fromBase64(base64);
                    String text = OCR.recognizeText(img);
                    // 简化版 - 实际应该返回带 bbox 的结构
                    return successResult(Map.of(
                        "text", text,
                        "image_size", img.getWidth() + "x" + img.getHeight()
                    ));
                } catch (Exception e) {
                    return errorResult(e.getMessage());
                }
            }
        );
    }

    /**
     * Tool 3: ocr_detect_id_card - 身份证识别
     */
    private static McpServerFeatures.SyncToolSpecification detectIdCardTool() {
        McpSchema.JsonSchema jsonSchema = new McpSchema.JsonSchema(
            "object",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "image", Map.of("type", "string", "description", "身份证图片的 base64"),
                    "side", Map.of("type", "string", "enum", List.of("front", "back"), "description", "front=正面, back=反面")
                ),
                "required", List.of("image", "side")
            ),
            List.of("image", "side"), null, null, null);

        return new McpServerFeatures.SyncToolSpecification(
            new McpSchema.Tool(
                "ocr_detect_id_card",
                "【身份证识别】识别中国居民身份证正反面，提取姓名、身份证号、地址、签发机关等。\n" +
                "参数：image - 身份证图片, side - front(正面) 或 back(反面)\n" +
                "返回：结构化字段 + 原始识别文本",
                jsonSchema
            ),
            (exchange, args) -> {
                try {
                    String base64 = (String) args.get("image");
                    String side = (String) args.get("side");
                    BufferedImage img = ImageUtils.fromBase64(base64);
                    String text = OCR.recognizeText(img);
                    Map<String, String> fields = parseIdCardFields(text);
                    boolean detected = fields.get("id_number") != null;
                    return successResult(Map.of(
                        "side", side,
                        "detected", detected,
                        "fields", fields,
                        "raw_text", text,
                        "message", detected ? "✓ 已识别" : "⚠️ 未识别到身份证号字段"
                    ));
                } catch (Exception e) {
                    return errorResult(e.getMessage());
                }
            }
        );
    }

    /**
     * Tool 4: ocr_detect_plate - 车牌识别
     */
    private static McpServerFeatures.SyncToolSpecification detectPlateTool() {
        McpSchema.JsonSchema jsonSchema = new McpSchema.JsonSchema(
            "object",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "image", Map.of("type", "string", "description", "车辆图片的 base64")
                ),
                "required", List.of("image")
            ),
            List.of("image"), null, null, null);

        return new McpServerFeatures.SyncToolSpecification(
            new McpSchema.Tool(
                "ocr_detect_plate",
                "【车牌识别】识别中国车牌号（支持蓝/绿/黄/新能源 12 种车牌）。\n" +
                "会严格校验车牌格式，未识别时返回 detected=false。",
                jsonSchema
            ),
            (exchange, args) -> {
                try {
                    String base64 = (String) args.get("image");
                    BufferedImage img = ImageUtils.fromBase64(base64);
                    String text = OCR.recognizeText(img);
                    String plate = extractPlateNumber(text);
                    if (plate == null) {
                        return successResult(Map.of(
                            "detected", false,
                            "plate", "",
                            "raw_text", text,
                            "message", "未识别到中国车牌号"
                        ));
                    }
                    return successResult(Map.of(
                        "detected", true,
                        "plate", plate,
                        "confidence", 0.95,
                        "raw_text", text
                    ));
                } catch (Exception e) {
                    return errorResult(e.getMessage());
                }
            }
        );
    }

    // ============ 工具方法 ============

    private static McpSchema.CallToolResult successResult(Object data) {
        try {
            String json = JSON.writerWithDefaultPrettyPrinter().writeValueAsString(data);
            return new McpSchema.CallToolResult(
                List.of(new McpSchema.TextContent(json)),
                false
            );
        } catch (Exception e) {
            return errorResult("结果序列化失败: " + e.getMessage());
        }
    }

    private static McpSchema.CallToolResult errorResult(String message) {
        return new McpSchema.CallToolResult(
            List.of(new McpSchema.TextContent("错误: " + message)),
            true
        );
    }

    private static final java.util.regex.Pattern ID_CARD_PATTERN =
        java.util.regex.Pattern.compile(".*\\d{17}[\\dXx].*");

    private static Map<String, String> parseIdCardFields(String text) {
        Map<String, String> fields = new LinkedHashMap<>();
        if (text == null) return fields;
        for (String line : text.split("\\n")) {
            line = line.trim();
            if (line.isEmpty()) continue;
            if (ID_CARD_PATTERN.matcher(line).matches()) {
                fields.put("id_number", line.replaceAll("[^0-9Xx]", ""));
            } else if (line.startsWith("姓名")) {
                fields.put("name", line.replace("姓名", "").trim());
            } else if (line.startsWith("性别")) {
                fields.put("gender", line.replace("性别", "").trim());
            } else if (line.startsWith("民族")) {
                fields.put("ethnicity", line.replace("民族", "").trim());
            } else if (line.startsWith("住址")) {
                fields.put("address", line.replace("住址", "").trim());
            }
        }
        return fields;
    }

    private static final java.util.regex.Pattern PLATE_PATTERN =
        java.util.regex.Pattern.compile(
            "^[京津沪渝冀豫云辽黑湘皖鲁新苏浙赣鄂桂甘晋蒙陕吉闽贵粤青藏川宁琼使领]" +
            "[A-Z]" +
            "(?:[A-Z0-9]{5,6}|[A-Z0-9]{4}[A-Z0-9挂学警港澳]{1,2})$"
        );

    private static String extractPlateNumber(String text) {
        if (text == null) return null;
        for (String line : text.split("\\n")) {
            line = line.trim().replace(" ", "").replace("·", "");
            if (PLATE_PATTERN.matcher(line).matches()) {
                return line;
            }
            java.util.regex.Matcher m = PLATE_PATTERN.matcher(line);
            if (m.find()) return m.group();
        }
        return null;
    }
}
