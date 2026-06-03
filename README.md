# AIPal MCP Server

> 把 86 个 AI 模型封装为 MCP Tools，让 Claude / Cursor / Cherry Studio 等 AI 助手能"看图识字、识别人脸、检测物体"。

[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://openjdk.java.net/)
[![MCP](https://img.shields.io/badge/MCP-0.10.0-blue.svg)](https://modelcontextprotocol.io)
[![License](https://img.shields.io/badge/License-MulanPSL2-blue.svg)](https://license.coscl.org.cn/MulanPSL2)
[![GitHub stars](https://img.shields.io/github/stars/SLACKOFFXIXI/aipal-mcp?style=social)](https://github.com/SLACKOFFXIXI/aipal-mcp)

[English](#) | [简体中文](./README.zh-CN.md)

## ✨ 这是什么？

**AIPal MCP** 是一组 [Model Context Protocol](https://modelcontextprotocol.io) Server，把 [SmartJavaAI](https://gitee.com/dengwenjie/SmartJavaAI) 项目的 **86 个 AI 模型** 变成 AI 助手能直接调用的"工具"。

装上之后，你可以在 Claude Desktop / Cursor 里说：

> "OCR 这张图片"
> "检测这张图里的人脸"
> "识别这张车牌"
> "找出这些图片里的重复照片"

AI 助手就会自动调用对应的工具完成任务。

## 🎯 已实现的 Tools

### 📝 OCR 工具集（4 个）
- **`ocr_extract_text`** — 通用文字提取（中英文、数字、标点）
- **`ocr_extract_with_positions`** — 文字+坐标（适合版面分析）
- **`ocr_detect_id_card`** — 身份证识别（结构化字段）
- **`ocr_detect_plate`** — 车牌识别（带正则校验，不误报）

### 👤 人脸工具集（开发中，8 个规划中）
人脸检测、识别、1:1 比对、1:N 搜索、活体检测、口罩检测、年龄/性别估计、属性识别

### 🎯 目标检测工具集（开发中，4 个规划中）
通用物体检测（YOLOv5/v8/v11）、实例分割、行人检测、姿态估计

### 🌐 多语种翻译（4 个）
中英日韩、200+ 语言互译

*（更多模块在持续开发中，详见 [Models 支持](#models-支持)）*

## 🚀 5 分钟快速开始

### 前置要求
- **JDK 17+**（OpenJDK / Oracle JDK / Corretto 任一）
- **已下载 SmartJavaAI 模型文件**（从 [百度网盘](https://pan.baidu.com/s/1dlZxWEMULnaietMDUJh38g?pwd=1234) 获取）

### Step 1: 下载

从 [Releases](https://github.com/SLACKOFFXIXI/aipal-mcp/releases) 页面下载最新的 `mcp-ocr.jar`：

```bash
wget https://github.com/SLACKOFFXIXI/aipal-mcp/releases/download/v0.1.0/mcp-ocr.jar
```

### Step 2: 配置模型路径

设置环境变量 `SMARTJAVAAI_MODEL_PATH` 指向你下载的模型目录：

```bash
# Mac/Linux
export SMARTJAVAAI_MODEL_PATH=/Users/yourname/Downloads/SmartJavaAIModels/SmartJavaAI

# Windows
set SMARTJAVAAI_MODEL_PATH=C:\Users\yourname\Downloads\SmartJavaAIModels\SmartJavaAI
```

### Step 3: 配置 Claude Desktop

编辑 Claude Desktop 配置文件：

**Mac**: `~/Library/Application Support/Claude/claude_desktop_config.json`
**Windows**: `%APPDATA%\Claude\claude_desktop_config.json`

```json
{
  "mcpServers": {
    "aipal-ocr": {
      "command": "java",
      "args": [
        "-Dai.djl.default_engine=OnnxRuntime",
        "-jar",
        "/绝对路径/mcp-ocr.jar"
      ]
    }
  }
}
```

重启 Claude Desktop，在对话里说：

> "帮我 OCR 这张图片"（附上图片）

Claude 就会自动调用 `ocr_extract_text` 工具完成识别 🎉

### Step 4: 调试（可选）

用 [MCP Inspector](https://github.com/modelcontextprotocol/inspector) 测试：

```bash
npx @modelcontextprotocol/inspector java -jar mcp-ocr.jar
```

浏览器打开 http://localhost:6274 即可可视化测试所有 Tools。

## 📦 可用模块

| 模块 | Tool 数 | 状态 |
|------|---------|------|
| [mcp-ocr](./mcp-ocr) | 4 | ✅ v0.1.0 |
| [mcp-face](./mcp-face) | 8 | 🚧 规划中 |
| [mcp-detection](./mcp-detection) | 4 | 🚧 规划中 |
| [mcp-classification](./mcp-classification) | 3 | 🚧 规划中 |
| [mcp-pose](./mcp-pose) | 2 | 🚧 规划中 |
| [mcp-translation](./mcp-translation) | 3 | 🚧 规划中 |

**总计划**：24+ 个 Tool 覆盖 SmartJavaAI 全部 86 个模型。

## 🎨 Models 支持

| 模型类别 | 数量 | 对应模块 |
|----------|------|----------|
| OCR（文字识别） | 17 | mcp-ocr |
| 人脸识别 | 26 | mcp-face |
| 目标检测 | 16 | mcp-detection |
| 人类动作识别 | 9 | mcp-face（跌倒检测）|
| 姿态估计 | 4 | mcp-pose |
| 实例分割 | 4 | mcp-detection |
| 机器翻译 | 4 | mcp-translation |
| 图像分类 | 2 | mcp-classification |
| 行人检测 | 1 | mcp-detection |
| 语义分割 | 1 | mcp-detection |
| OBB 旋转框 | 1 | mcp-detection |
| CLIP（文搜图） | 1 | mcp-classification |

## 🛠 架构

```
┌──────────────────────────────────┐
│   Claude / Cursor / AI 客户端     │
│   (stdio JSON-RPC 通信)            │
└──────────────┬───────────────────┘
               │
       ┌───────▼────────┐
       │  mcp-ocr.jar   │  ← Spring Boot 风格 fat jar
       │                │
       │  OcrMcpServer  │  ← BaseMcpServer.start()
       │       │        │
       │  OcrTools.all() │  ← 4 个 Tool 注册
       │       │        │
       │  OcrService     │  ← 反射 SmartJavaAI
       └───────┬────────┘
               │
       ┌───────▼────────┐
       │  SmartJavaAI   │
       │  DJL + ONNX    │
       │  PaddleOCR     │
       └────────────────┘
```

**关键设计**：
- **每个模块独立 jar**：用户按需下载，不会被不需要的模型拖累
- **反射调用 SmartJavaAI**：避免父项目 toolchain 冲突
- **可独立运行**：自带 JRE 启动脚本（参见 `scripts/`）

## 🛣 路线图

- [x] **v0.1.0**（当前）— mcp-ocr 模块（4 个 OCR Tools）
- [ ] **v0.2.0** — mcp-face（人脸检测 + 识别 + 比对）
- [ ] **v0.3.0** — mcp-detection（YOLO 系列）
- [ ] **v0.4.0** — mcp-classification + mcp-pose + mcp-translation
- [ ] **v0.5.0** — npm 包分发（一键安装）
- [ ] **v0.6.0** — SaaS 云托管版（不用装 Java）
- [ ] **v1.0.0** — 完整 24+ Tool + 商业化

## 🤝 贡献

欢迎贡献！请：
1. Fork 这个仓库
2. 创建 feature 分支 (`git checkout -b feature/AmazingFeature`)
3. 提交改动 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

## 📜 许可证

基于 [SmartJavaAI](https://gitee.com/dengwenjie/SmartJavaAI) 的 MulanPSL-2 许可证。

## 🙏 致谢

- [SmartJavaAI](https://gitee.com/dengwenjie/SmartJavaAI) - 提供底层 AI 模型
- [Model Context Protocol](https://modelcontextprotocol.io) - MCP 协议
- [Anthropic](https://www.anthropic.com) - Claude AI

## 📮 联系

- **作者**: SLACKOFFXIXI
- **项目主页**: https://github.com/SLACKOFFXIXI/aipal-mcp
- **问题反馈**: [Issues](https://github.com/SLACKOFFXIXI/aipal-mcp/issues)

---

⭐ 如果这个项目对你有帮助，请在 GitHub 上点 Star！这对我们非常重要。
