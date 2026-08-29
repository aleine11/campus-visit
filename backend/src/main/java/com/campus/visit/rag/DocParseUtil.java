package com.campus.visit.rag;

import com.campus.visit.common.BusinessException;
import com.campus.visit.common.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * 文档解析工具（对标 architecture.md 8.2：DocumentParser 提取文本）
 *
 * 支持三种格式（设计文档约定）：PDF / TXT / DOCX
 *   pdf  → PDFBox 3.0（Loader.loadPDF + PDFTextStripper）
 *   txt  → 直接按 UTF-8 读字符串
 *   docx → POI 5.2（XWPFDocument + XWPFWordExtractor）
 *   其他 → 40030 文档类型不支持
 */
@Slf4j
public final class DocParseUtil {

    private DocParseUtil() {
    }

    /** 从字节流解析纯文本（调用方负责先校验扩展名） */
    public static String parse(String fileName, byte[] bytes) {
        String ext = extension(fileName);
        try {
            String text = switch (ext) {
                case "pdf" -> parsePdf(bytes);
                case "txt" -> new String(bytes, StandardCharsets.UTF_8);
                case "docx" -> parseDocx(bytes);
                default -> throw new BusinessException(ResultCode.DOC_TYPE_NOT_SUPPORT);
            };
            // 去掉首尾空白 + 全角空格规整
            text = text == null ? "" : text.strip();
            if (text.isEmpty()) {
                throw new BusinessException(ResultCode.DOC_PARSE_FAILED, "文档解析后没有可用文本（可能是扫描件/空文档）");
            }
            return text;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("文档解析失败: {}, 原因: {}", fileName, e.getMessage());
            throw new BusinessException(ResultCode.DOC_PARSE_FAILED, "文档解析失败: " + e.getMessage());
        }
    }

    /** 从文件路径解析（重新解析场景用） */
    public static String parseFile(String filePath, String fileName) {
        try (InputStream in = new java.io.FileInputStream(filePath)) {
            return parse(fileName, in.readAllBytes());
        } catch (IOException e) {
            throw new BusinessException(ResultCode.DOC_PARSE_FAILED, "读取文件失败: " + e.getMessage());
        }
    }

    /** 提取小写扩展名（不含点） */
    public static String extension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }

    /* ==================== 各格式解析实现 ==================== */

    /** PDF：PDFBox 3.0 API（Loader 替代了旧版 PDDocument.load） */
    private static String parsePdf(byte[] bytes) throws IOException {
        try (PDDocument document = Loader.loadPDF(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    /** DOCX：POI XWPF（.doc 老格式不支持，设计文档约定只收 docx） */
    private static String parseDocx(byte[] bytes) throws IOException {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes));
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }
}
