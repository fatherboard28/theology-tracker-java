package com.theology.tracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.List;
import com.lowagie.text.ListItem;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.theology.tracker.model.Paper;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;

@Service
public class PaperExportService {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final float BODY_SIZE = 11f;

    // ── PDF ──────────────────────────────────────────────────────────────────

    public byte[] toPdf(Paper paper) throws Exception {
        var baos = new ByteArrayOutputStream();
        var doc = new Document(PageSize.A4, 72, 72, 72, 90);
        PdfWriter.getInstance(doc, baos);
        doc.open();

        var titlePara = new Paragraph(paper.getTitle(), new Font(Font.HELVETICA, 20, Font.BOLD));
        titlePara.setSpacingAfter(4f);
        doc.add(titlePara);

        if (paper.getAuthor() != null && !paper.getAuthor().isBlank()) {
            doc.add(new Paragraph(paper.getAuthor(), new Font(Font.HELVETICA, 12, Font.ITALIC)));
        }

        if (paper.getThesis() != null && !paper.getThesis().isBlank()) {
            var thPara = new Paragraph(paper.getThesis(), new Font(Font.HELVETICA, BODY_SIZE, Font.ITALIC));
            thPara.setIndentationLeft(30f);
            thPara.setIndentationRight(30f);
            thPara.setSpacingBefore(6f);
            thPara.setSpacingAfter(12f);
            doc.add(thPara);
        }

        doc.add(Chunk.NEWLINE);
        renderNodeToPdf(doc, MAPPER.readTree(paper.getBody()));

        var footnotes = MAPPER.readTree(paper.getFootnotes());
        if (footnotes.isArray() && !footnotes.isEmpty()) {
            doc.add(Chunk.NEWLINE);
            doc.add(new Paragraph("Footnotes", new Font(Font.HELVETICA, 13, Font.BOLD)));
            int i = 1;
            for (var fn : footnotes) {
                var p = new Paragraph((i++) + ". " + fn.asText(), new Font(Font.HELVETICA, 10));
                p.setSpacingAfter(3f);
                doc.add(p);
            }
        }

        var bibliography = MAPPER.readTree(paper.getBibliography());
        if (bibliography.isArray() && !bibliography.isEmpty()) {
            doc.add(Chunk.NEWLINE);
            doc.add(new Paragraph("Bibliography", new Font(Font.HELVETICA, 13, Font.BOLD)));
            for (var bib : bibliography) {
                var p = new Paragraph(bib.asText(), new Font(Font.HELVETICA, BODY_SIZE));
                p.setIndentationLeft(30f);
                p.setFirstLineIndent(-30f);
                p.setSpacingAfter(3f);
                doc.add(p);
            }
        }

        doc.close();
        return baos.toByteArray();
    }

    private void renderNodeToPdf(Document doc, JsonNode node) throws Exception {
        if (!node.has("type")) return;
        var content = node.get("content");

        switch (node.path("type").asText()) {
            case "doc" -> {
                if (content != null) for (var c : content) renderNodeToPdf(doc, c);
            }
            case "paragraph" -> {
                var p = new Paragraph();
                p.setFont(new Font(Font.HELVETICA, BODY_SIZE));
                p.setSpacingAfter(6f);
                if (content != null) for (var c : content) p.add(pdfChunk(c, BODY_SIZE));
                if (p.isEmpty()) p.add(Chunk.NEWLINE);
                doc.add(p);
            }
            case "heading" -> {
                int lvl = node.path("attrs").path("level").asInt(1);
                float sz = lvl == 1 ? 18f : lvl == 2 ? 15f : 13f;
                var p = new Paragraph();
                p.setFont(new Font(Font.HELVETICA, sz, Font.BOLD));
                p.setSpacingBefore(10f);
                p.setSpacingAfter(4f);
                if (content != null) for (var c : content) p.add(pdfChunk(c, sz));
                doc.add(p);
            }
            case "bulletList" -> {
                var list = new List(false, 20f);
                list.setListSymbol("• ");
                list.setIndentationLeft(20f);
                if (content != null) for (var item : content) addPdfListItem(item, list);
                doc.add(list);
            }
            case "orderedList" -> {
                var list = new List(true, 20f);
                list.setIndentationLeft(20f);
                if (content != null) for (var item : content) addPdfListItem(item, list);
                doc.add(list);
            }
            case "blockquote" -> {
                if (content != null) {
                    for (var c : content) {
                        if ("paragraph".equals(c.path("type").asText())) {
                            var p = new Paragraph();
                            p.setFont(new Font(Font.HELVETICA, BODY_SIZE, Font.ITALIC, new Color(100, 100, 100)));
                            p.setIndentationLeft(30f);
                            p.setIndentationRight(30f);
                            p.setSpacingAfter(4f);
                            if (c.has("content")) for (var t : c.get("content")) p.add(pdfChunk(t, BODY_SIZE));
                            doc.add(p);
                        }
                    }
                }
            }
            case "horizontalRule" -> {
                var hr = new Paragraph(
                    "──────────────────────────────────────────────────",
                    new Font(Font.HELVETICA, 6, Font.NORMAL, new Color(180, 180, 180)));
                hr.setSpacingBefore(4f);
                hr.setSpacingAfter(4f);
                doc.add(hr);
            }
        }
    }

    private void addPdfListItem(JsonNode itemNode, List list) {
        var li = new ListItem();
        li.setFont(new Font(Font.HELVETICA, BODY_SIZE));
        li.setSpacingAfter(3f);
        if (itemNode.has("content")) {
            for (var c : itemNode.get("content")) {
                if ("paragraph".equals(c.path("type").asText()) && c.has("content")) {
                    for (var t : c.get("content")) li.add(pdfChunk(t, BODY_SIZE));
                }
            }
        }
        list.add(li);
    }

    private Chunk pdfChunk(JsonNode node, float baseSize) {
        if ("hardBreak".equals(node.path("type").asText())) return Chunk.NEWLINE;
        if (!"text".equals(node.path("type").asText())) return new Chunk("");

        var text = node.path("text").asText("");
        boolean bold = false, italic = false, strike = false, code = false;
        if (node.has("marks")) {
            for (var mark : node.get("marks")) {
                switch (mark.path("type").asText()) {
                    case "bold"   -> bold = true;
                    case "italic" -> italic = true;
                    case "strike" -> strike = true;
                    case "code"   -> code = true;
                }
            }
        }
        Font font;
        if (code) {
            font = new Font(Font.COURIER, 10f);
        } else {
            int style = (bold ? Font.BOLD : 0) | (italic ? Font.ITALIC : 0) | (strike ? Font.STRIKETHRU : 0);
            font = new Font(Font.HELVETICA, baseSize, style == 0 ? Font.NORMAL : style);
        }
        return new Chunk(text, font);
    }

    // ── DOCX ─────────────────────────────────────────────────────────────────

    public byte[] toDocx(Paper paper) throws Exception {
        var word = new XWPFDocument();
        var baos = new ByteArrayOutputStream();

        docxRun(word.createParagraph(), paper.getTitle(), 20, true, false);

        if (paper.getAuthor() != null && !paper.getAuthor().isBlank()) {
            docxRun(word.createParagraph(), paper.getAuthor(), 12, false, true);
        }

        if (paper.getThesis() != null && !paper.getThesis().isBlank()) {
            var thPara = word.createParagraph();
            thPara.setIndentationLeft(720);
            thPara.setIndentationRight(720);
            thPara.setSpacingBefore(100);
            thPara.setSpacingAfter(200);
            var run = thPara.createRun();
            run.setText(paper.getThesis());
            run.setItalic(true);
            run.setFontSize(11);
            run.setFontFamily("Calibri");
        }

        word.createParagraph();
        renderNodeToDocx(word, MAPPER.readTree(paper.getBody()));

        var footnotes = MAPPER.readTree(paper.getFootnotes());
        if (footnotes.isArray() && !footnotes.isEmpty()) {
            word.createParagraph();
            docxRun(word.createParagraph(), "Footnotes", 13, true, false);
            int i = 1;
            for (var fn : footnotes) {
                var p = word.createParagraph();
                p.setSpacingAfter(60);
                var run = p.createRun();
                run.setText((i++) + ". " + fn.asText());
                run.setFontSize(10);
                run.setFontFamily("Calibri");
            }
        }

        var bibliography = MAPPER.readTree(paper.getBibliography());
        if (bibliography.isArray() && !bibliography.isEmpty()) {
            word.createParagraph();
            docxRun(word.createParagraph(), "Bibliography", 13, true, false);
            for (var bib : bibliography) {
                var p = word.createParagraph();
                p.setIndentationLeft(360);
                p.setIndentationHanging(360);
                p.setSpacingAfter(60);
                var run = p.createRun();
                run.setText(bib.asText());
                run.setFontSize(11);
                run.setFontFamily("Calibri");
            }
        }

        word.write(baos);
        return baos.toByteArray();
    }

    private void renderNodeToDocx(XWPFDocument word, JsonNode node) {
        if (!node.has("type")) return;
        switch (node.path("type").asText()) {
            case "doc" -> {
                if (node.has("content")) for (var c : node.get("content")) renderNodeToDocx(word, c);
            }
            case "paragraph" -> {
                var p = word.createParagraph();
                p.setSpacingAfter(120);
                addInlineRuns(p, node, 11, false, false);
            }
            case "heading" -> {
                int lvl = node.path("attrs").path("level").asInt(1);
                int sz = lvl == 1 ? 20 : lvl == 2 ? 16 : 14;
                var p = word.createParagraph();
                p.setSpacingBefore(200);
                p.setSpacingAfter(80);
                addInlineRuns(p, node, sz, true, false);
            }
            case "bulletList" -> {
                if (node.has("content"))
                    for (var item : node.get("content")) addDocxListItem(word, item, "•", 0);
            }
            case "orderedList" -> {
                if (node.has("content")) {
                    int i = 1;
                    for (var item : node.get("content")) addDocxListItem(word, item, (i++) + ".", 0);
                }
            }
            case "blockquote" -> {
                if (node.has("content")) {
                    for (var c : node.get("content")) {
                        if ("paragraph".equals(c.path("type").asText())) {
                            var p = word.createParagraph();
                            p.setIndentationLeft(720);
                            p.setIndentationRight(720);
                            p.setSpacingAfter(80);
                            addInlineRuns(p, c, 11, false, true);
                        }
                    }
                }
            }
            case "horizontalRule" -> {
                var p = word.createParagraph();
                var run = p.createRun();
                run.setText("────────────────────────────────────────────────────────");
                run.setColor("AAAAAA");
                run.setFontSize(8);
                run.setFontFamily("Calibri");
            }
        }
    }

    private void addDocxListItem(XWPFDocument word, JsonNode itemNode, String prefix, int depth) {
        if (!itemNode.has("content")) return;
        boolean first = true;
        for (var c : itemNode.get("content")) {
            var nodeType = c.path("type").asText();
            if ("paragraph".equals(nodeType)) {
                var p = word.createParagraph();
                p.setIndentationLeft((depth + 1) * 360);
                p.setSpacingAfter(60);
                if (first) {
                    var prefixRun = p.createRun();
                    prefixRun.setText(prefix + " ");
                    prefixRun.setFontSize(11);
                    prefixRun.setFontFamily("Calibri");
                    first = false;
                }
                addInlineRuns(p, c, 11, false, false);
            } else if ("bulletList".equals(nodeType) && c.has("content")) {
                for (var sub : c.get("content")) addDocxListItem(word, sub, "•", depth + 1);
            } else if ("orderedList".equals(nodeType) && c.has("content")) {
                int i = 1;
                for (var sub : c.get("content")) addDocxListItem(word, sub, (i++) + ".", depth + 1);
            }
        }
    }

    private void addInlineRuns(XWPFParagraph para, JsonNode node, int fontSize, boolean defaultBold, boolean defaultItalic) {
        if (!node.has("content")) return;
        for (var child : node.get("content")) {
            if ("hardBreak".equals(child.path("type").asText())) {
                para.createRun().addBreak();
            } else if ("text".equals(child.path("type").asText())) {
                boolean bold = defaultBold, italic = defaultItalic, strike = false, code = false;
                if (child.has("marks")) {
                    for (var mark : child.get("marks")) {
                        switch (mark.path("type").asText()) {
                            case "bold"   -> bold = true;
                            case "italic" -> italic = true;
                            case "strike" -> strike = true;
                            case "code"   -> code = true;
                        }
                    }
                }
                var run = para.createRun();
                run.setText(child.path("text").asText(""));
                run.setBold(bold);
                run.setItalic(italic);
                run.setStrikeThrough(strike);
                run.setFontSize(code ? 10 : fontSize);
                run.setFontFamily(code ? "Courier New" : "Calibri");
            }
        }
    }

    private void docxRun(XWPFParagraph para, String text, int fontSize, boolean bold, boolean italic) {
        var run = para.createRun();
        run.setText(text);
        run.setBold(bold);
        run.setItalic(italic);
        run.setFontSize(fontSize);
        run.setFontFamily("Calibri");
    }
}
