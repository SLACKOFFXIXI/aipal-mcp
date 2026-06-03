package cn.aipal.mcp.ocr.service;

import cn.aipal.mcp.common.util.ImageUtils;
import lombok.extern.slf4j.Slf4j;

import java.awt.image.BufferedImage;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * OCR 推理服务 - 反射调用 SmartJavaAI 的 OcrModelFactory
 *
 * 设计原则：
 * - 通过反射调用 SmartJavaAI API（不依赖父项目 toolchain）
 * - 懒加载 + 缓存模型实例
 * - 失败信息详细，便于调试
 */
@Slf4j
public class OcrService {

    private static final OcrService INSTANCE = new OcrService();
    public static OcrService getInstance() { return INSTANCE; }

    private final Map<String, Object> modelCache = new ConcurrentHashMap<>();
    private volatile boolean available = false;
    private volatile boolean checked = false;

    public OcrService() {
        try {
            Class.forName("cn.smartjavaai.ocr.factory.OcrModelFactory");
            available = true;
            log.info("✅ SmartJavaAI OCR 已就绪");
        } catch (ClassNotFoundException e) {
            log.warn("⚠️ SmartJavaAI OCR 依赖未找到（classpath 中缺 ocr/common jar）");
        } finally {
            checked = true;
        }
    }

    public boolean isAvailable() { return available; }

    /**
     * 通用 OCR 识别
     * @return 识别出的所有文字（按行拼接）
     */
    public String recognizeText(BufferedImage image) {
        if (!available) return "[OCR 未启用] SmartJavaAI 依赖未配置";
        long start = System.currentTimeMillis();
        try {
            Object detModel = getOrLoadModel("common_det", this::loadDetModel);
            Object recModel = getOrLoadModel("common_rec", this::loadRecModel);

            Method detect = detModel.getClass().getMethod("detect", BufferedImage.class);
            List<Object> dets = (List<Object>) detect.invoke(detModel, image);
            log.info("det 检出 {} 个区域 ({}ms)", dets.size(), System.currentTimeMillis() - start);

            if (dets.isEmpty()) return "";

            // 用 rec 模型一次性识别
            ai.djl.modality.cv.Image djImage = toDjImage(image);
            Class<?> ocrRecOptionsCls = Class.forName("cn.smartjavaai.ocr.config.OcrRecOptions");
            Method recognize = recModel.getClass().getMethod("recognize",
                Class.forName("ai.djl.modality.cv.Image"),
                java.util.List.class, ocrRecOptionsCls);
            Object ocrInfo = recognize.invoke(recModel, djImage, dets, null);

            // 解析 OcrInfo
            Method getFullText = ocrInfo.getClass().getMethod("getFullText");
            String text = (String) getFullText.invoke(ocrInfo);
            log.info("OCR 完成: {}ms", System.currentTimeMillis() - start);
            return text != null ? text : "";
        } catch (Exception e) {
            log.error("OCR 失败", e);
            return "[OCR 失败] " + e.getMessage();
        }
    }

    private Object loadDetModel() {
        try {
            Class<?> factoryCls = Class.forName("cn.smartjavaai.ocr.factory.OcrModelFactory");
            Object factory = factoryCls.getMethod("getInstance").invoke(null);
            Class<?> cfgCls = Class.forName("cn.smartjavaai.ocr.config.OcrDetModelConfig");
            Object config = cfgCls.newInstance();
            Class<?> enumCls = Class.forName("cn.smartjavaai.ocr.enums.CommonDetModelEnum");
            cfgCls.getMethod("setModelEnum", enumCls).invoke(config,
                Enum.valueOf((Class<Enum>) enumCls, "PP_OCR_V5_SERVER_DET_MODEL"));
            String basePath = System.getenv().getOrDefault("SMARTJAVAAI_MODEL_PATH", "./models/");
            cfgCls.getMethod("setDetModelPath", String.class).invoke(config,
                Paths.get(basePath, "ch_PP-OCRv5_server_det").toString());
            return factoryCls.getMethod("getDetModel", cfgCls).invoke(factory, config);
        } catch (Exception e) {
            throw new RuntimeException("det 模型加载失败: " + e.getMessage(), e);
        }
    }

    private Object loadRecModel() {
        try {
            Class<?> factoryCls = Class.forName("cn.smartjavaai.ocr.factory.OcrModelFactory");
            Object factory = factoryCls.getMethod("getInstance").invoke(null);
            Class<?> cfgCls = Class.forName("cn.smartjavaai.ocr.config.OcrRecModelConfig");
            Object config = cfgCls.newInstance();
            Class<?> enumCls = Class.forName("cn.smartjavaai.ocr.enums.CommonRecModelEnum");
            cfgCls.getMethod("setRecModelEnum", enumCls).invoke(config,
                Enum.valueOf((Class<Enum>) enumCls, "PP_OCR_V5_SERVER_REC_MODEL"));
            String basePath = System.getenv().getOrDefault("SMARTJAVAAI_MODEL_PATH", "./models/");
            cfgCls.getMethod("setRecModelPath", String.class).invoke(config,
                Paths.get(basePath, "ch_PP-OCRv5_server_rec").toString());
            return factoryCls.getMethod("getRecModel", cfgCls).invoke(factory, config);
        } catch (Exception e) {
            throw new RuntimeException("rec 模型加载失败: " + e.getMessage(), e);
        }
    }

    private Object getOrLoadModel(String key, Supplier<Object> loader) {
        return modelCache.computeIfAbsent(key, k -> {
            log.info("首次加载模型: {}", k);
            return loader.get();
        });
    }

    /**
     * BufferedImage → DJL Image
     */
    private ai.djl.modality.cv.Image toDjImage(BufferedImage img) throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(img, "png", baos);
        Class<?> factoryCls = Class.forName("ai.djl.modality.cv.ImageFactory");
        Object factory = factoryCls.getMethod("getInstance").invoke(null);
        Method fromInputStream = factoryCls.getMethod("fromInputStream", java.io.InputStream.class);
        return (ai.djl.modality.cv.Image) fromInputStream.invoke(factory,
            new java.io.ByteArrayInputStream(baos.toByteArray()));
    }
}
