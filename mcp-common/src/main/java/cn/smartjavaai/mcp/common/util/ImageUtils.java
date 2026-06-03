package cn.aipal.mcp.common.util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Base64;

/**
 * 图像工具 - 统一的 base64/File/BufferedImage 互转
 */
public class ImageUtils {

    /**
     * base64 → BufferedImage
     * 支持 "data:image/png;base64,xxx" 或纯 base64
     */
    public static BufferedImage fromBase64(String base64) {
        if (base64 == null || base64.isEmpty()) {
            throw new IllegalArgumentException("base64 字符串为空");
        }
        if (base64.contains(",")) {
            base64 = base64.substring(base64.indexOf(",") + 1);
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            return ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (Exception e) {
            throw new RuntimeException("base64 解码失败: " + e.getMessage(), e);
        }
    }

    /**
     * File → BufferedImage
     */
    public static BufferedImage fromFile(String path) {
        try {
            return ImageIO.read(new File(path));
        } catch (Exception e) {
            throw new RuntimeException("图片读取失败: " + path, e);
        }
    }

    /**
     * BufferedImage → base64 (PNG 格式)
     */
    public static String toBase64(BufferedImage img) {
        try {
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            ImageIO.write(img, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("图片转 base64 失败", e);
        }
    }
}
