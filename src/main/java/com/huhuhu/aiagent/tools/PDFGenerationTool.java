package com.huhuhu.aiagent.tools;

import cn.hutool.core.io.FileUtil;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.huhuhu.aiagent.constant.FileConstant;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.IOException;

/**
 * PDF 生成工具
 */
public class PDFGenerationTool {

    @Tool(description = "Generate a PDF file with given content", returnDirect = false)
    public String generatePDF(
            @ToolParam(description = "Name of the file to save the generated PDF") String fileName,
            @ToolParam(description = "Content to be included in the PDF") String content) {
        String fileDir = FileConstant.FILE_SAVE_DIR + "/pdf";
        String filePath = fileDir + "/" + fileName;
        try {
            // 创建目录
            FileUtil.mkdir(fileDir);
            // 创建 PdfWriter 和 PdfDocument 对象
            try (PdfWriter writer = new PdfWriter(filePath);
                 PdfDocument pdf = new PdfDocument(writer);
                 Document document = new Document(pdf)) {
                // 使用 classpath 中的中文字体
                PdfFont font = createChineseFont();
                document.setFont(font);

                // 按行分割内容，保留格式
                String[] lines = content.split("\n");
                for (String line : lines) {
                    if (line.trim().isEmpty()) {
                        document.add(new Paragraph(" "));
                    } else {
                        document.add(new Paragraph(line));
                    }
                }
            }
            return "PDF generated successfully to: " + filePath;
        } catch (IOException e) {
            return "Error generating PDF: " + e.getMessage();
        }
    }

    /**
     * 创建支持中文的字体（从 classpath 加载）
     */
    private PdfFont createChineseFont() throws IOException {
        // 从 classpath 加载字体文件，使用相对路径
        return PdfFontFactory.createFont("/fonts/simsun.ttc,0");
    }
}