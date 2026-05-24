package com.theology.tracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.List;
import com.lowagie.text.ListItem;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import com.theology.tracker.model.Paper;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTInd;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSpacing;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;

@Service
public class PaperExportService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // ── PDF constants (Turabian: Times Roman 12pt, 1″ margins, double-spaced) ──
    private static final int    PDF_FONT  = Font.TIMES_ROMAN;
    private static final float  BODY_PT   = 12f;
    private static final float  HALF_IN   = 36f;  // 0.5″ = 36pt
    private static final float  ONE_IN    = 72f;  // 1″   = 72pt

    // ── DOCX constants (twips: 1″ = 1440, 0.5″ = 720) ─────────────────────────
    private static final int       TWIPS_1IN     = 1440;
    private static final int       TWIPS_HALF    = 720;
    private static final BigInteger LINE_DOUBLE  = BigInteger.valueOf(480); // double-spaced
    private static final BigInteger LINE_SINGLE  = BigInteger.valueOf(240); // single-spaced
    private static final BigInteger ZERO         = BigInteger.ZERO;

    // ── PDF export ──────────────────────────────────────────────────────────────

    public byte[] toPdf(Paper paper) throws Exception {
        var baos   = new ByteArrayOutputStream();
        var doc    = new Document(PageSize.LETTER, ONE_IN, ONE_IN, ONE_IN, ONE_IN);
        var writer = PdfWriter.getInstance(doc, baos);
        writer.setPageEvent(new TurabianPageEvent());
        doc.open();

        // Title — centered, bold
        if (paper.getTitle() != null && !paper.getTitle().isBlank()) {
            var p = pdfPara(paper.getTitle(), BODY_PT, Font.BOLD, Element.ALIGN_CENTER, false);
            doc.add(p);
        }

        // Author — centered, regular
        if (paper.getAuthor() != null && !paper.getAuthor().isBlank()) {
            var p = pdfPara(paper.getAuthor(), BODY_PT, Font.NORMAL, Element.ALIGN_CENTER, false);
            doc.add(p);
        }

        // Thesis — indented block, italic
        if (paper.getThesis() != null && !paper.getThesis().isBlank()) {
            var p = pdfPara(paper.getThesis(), BODY_PT, Font.ITALIC, Element.ALIGN_LEFT, false);
            p.setIndentationLeft(HALF_IN);
            p.setIndentationRight(HALF_IN);
            doc.add(p);
        }

        // Body
        if (paper.getBody() != null && !paper.getBody().isBlank()) {
            renderNodeToPdf(doc, MAPPER.readTree(paper.getBody()));
        }

        // Notes (single-spaced, first-line indent)
        var footnotes = MAPPER.readTree(paper.getFootnotes() != null ? paper.getFootnotes() : "[]");
        if (footnotes.isArray() && !footnotes.isEmpty()) {
            doc.add(pdfBlankLine());
            doc.add(pdfSectionHeading("Notes"));
            int i = 1;
            for (var fn : footnotes) {
                doc.add(pdfNotePara((i++) + ". " + fn.asText()));
            }
        }

        // Bibliography (single-spaced, hanging indent, blank line between entries)
        var bibliography = MAPPER.readTree(paper.getBibliography() != null ? paper.getBibliography() : "[]");
        if (bibliography.isArray() && !bibliography.isEmpty()) {
            doc.add(pdfBlankLine());
            doc.add(pdfSectionHeading("Bibliography"));
            for (var bib : bibliography) {
                doc.add(pdfBibPara(bib.asText()));
            }
        }

        doc.close();
        return baos.toByteArray();
    }

    /** Double-spaced paragraph; firstLineIndent = HALF_IN when indent=true. */
    private Paragraph pdfPara(String text, float size, int style, int alignment, boolean indent) {
        var p = new Paragraph(text, new Font(PDF_FONT, size, style));
        p.setLeading(0, 2.0f);
        p.setAlignment(alignment);
        p.setFirstLineIndent(indent ? HALF_IN : 0);
        return p;
    }

    /** Blank double-spaced line used as section separator. */
    private Paragraph pdfBlankLine() {
        var p = new Paragraph(" ", new Font(PDF_FONT, BODY_PT));
        p.setLeading(0, 2.0f);
        return p;
    }

    /** Section heading: centered, bold, double-spaced. */
    private Paragraph pdfSectionHeading(String text) {
        var p = new Paragraph(text, new Font(PDF_FONT, BODY_PT, Font.BOLD));
        p.setLeading(0, 2.0f);
        p.setAlignment(Element.ALIGN_CENTER);
        p.setFirstLineIndent(0);
        return p;
    }

    /** Note paragraph: single-spaced, first-line indent. */
    private Paragraph pdfNotePara(String text) {
        var p = new Paragraph(text, new Font(PDF_FONT, BODY_PT));
        p.setLeading(0, 1.2f);
        p.setFirstLineIndent(HALF_IN);
        return p;
    }

    /** Bibliography entry: single-spaced, hanging indent, blank line after. */
    private Paragraph pdfBibPara(String text) {
        var p = new Paragraph(text, new Font(PDF_FONT, BODY_PT));
        p.setLeading(0, 1.2f);
        p.setIndentationLeft(HALF_IN);
        p.setFirstLineIndent(-HALF_IN);
        p.setSpacingAfter(BODY_PT);  // one blank line between entries
        return p;
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
                p.setFont(new Font(PDF_FONT, BODY_PT));
                p.setLeading(0, 2.0f);
                p.setFirstLineIndent(HALF_IN);
                if (content != null) for (var c : content) p.add(pdfChunk(c));
                if (p.isEmpty()) p.add(new Chunk(" "));
                doc.add(p);
            }
            case "heading" -> {
                int lvl   = node.path("attrs").path("level").asInt(1);
                // H1: centered bold  |  H2: centered regular  |  H3: flush-left bold
                int style = (lvl == 2) ? Font.NORMAL : Font.BOLD;
                int align = (lvl <= 2) ? Element.ALIGN_CENTER : Element.ALIGN_LEFT;
                var p = new Paragraph();
                p.setFont(new Font(PDF_FONT, BODY_PT, style));
                p.setLeading(0, 2.0f);
                p.setFirstLineIndent(0);
                p.setAlignment(align);
                p.setSpacingBefore(BODY_PT * 2);  // one double-spaced blank line before heading
                if (content != null) for (var c : content) p.add(pdfChunk(c));
                doc.add(p);
            }
            case "bulletList" -> {
                var list = new List(false, 20f);
                list.setListSymbol("• ");
                list.setIndentationLeft(HALF_IN);
                if (content != null) for (var item : content) addPdfListItem(item, list);
                doc.add(list);
            }
            case "orderedList" -> {
                var list = new List(true, 20f);
                list.setIndentationLeft(HALF_IN);
                if (content != null) for (var item : content) addPdfListItem(item, list);
                doc.add(list);
            }
            case "blockquote" -> {
                // Block quotation: single-spaced, indented ½″ each side
                if (content != null) {
                    for (var c : content) {
                        if ("paragraph".equals(c.path("type").asText())) {
                            var p = new Paragraph();
                            p.setFont(new Font(PDF_FONT, BODY_PT));
                            p.setLeading(0, 1.2f);
                            p.setIndentationLeft(HALF_IN);
                            p.setIndentationRight(HALF_IN);
                            p.setFirstLineIndent(0);
                            p.setSpacingBefore(BODY_PT * 2);
                            p.setSpacingAfter(BODY_PT * 2);
                            if (c.has("content")) for (var t : c.get("content")) p.add(pdfChunk(t));
                            doc.add(p);
                        }
                    }
                }
            }
            case "horizontalRule" -> doc.add(pdfBlankLine());
        }
    }

    private void addPdfListItem(JsonNode itemNode, List list) {
        var li = new ListItem();
        li.setFont(new Font(PDF_FONT, BODY_PT));
        li.setLeading(0, 2.0f);
        if (itemNode.has("content")) {
            for (var c : itemNode.get("content")) {
                if ("paragraph".equals(c.path("type").asText()) && c.has("content")) {
                    for (var t : c.get("content")) li.add(pdfChunk(t));
                }
            }
        }
        list.add(li);
    }

    private Chunk pdfChunk(JsonNode node) {
        if ("hardBreak".equals(node.path("type").asText())) return Chunk.NEWLINE;
        if (!"text".equals(node.path("type").asText())) return new Chunk("");

        var text = node.path("text").asText("");
        boolean bold = false, italic = false, strike = false, code = false;
        if (node.has("marks")) {
            for (var mark : node.get("marks")) {
                switch (mark.path("type").asText()) {
                    case "bold"   -> bold   = true;
                    case "italic" -> italic = true;
                    case "strike" -> strike = true;
                    case "code"   -> code   = true;
                }
            }
        }
        Font font;
        if (code) {
            font = new Font(Font.COURIER, 10f);
        } else {
            int style = (bold   ? Font.BOLD      : 0)
                      | (italic ? Font.ITALIC     : 0)
                      | (strike ? Font.STRIKETHRU : 0);
            font = new Font(PDF_FONT, BODY_PT, style == 0 ? Font.NORMAL : style);
        }
        return new Chunk(text, font);
    }

    /** Top-right page numbers (Turabian §A.1.4). */
    private static class TurabianPageEvent extends PdfPageEventHelper {
        private final Font numFont = new Font(Font.TIMES_ROMAN, 12f);

        @Override
        public void onEndPage(PdfWriter writer, Document doc) {
            ColumnText.showTextAligned(
                    writer.getDirectContent(),
                    Element.ALIGN_RIGHT,
                    new Phrase(String.valueOf(writer.getPageNumber()), numFont),
                    doc.right() + 36f,  // 0.5″ from right edge of page
                    doc.top()  + 36f,   // 0.5″ above text area (in top margin)
                    0);
        }
    }

    // ── DOCX export ─────────────────────────────────────────────────────────────

    public byte[] toDocx(Paper paper) throws Exception {
        var word = new XWPFDocument();
        setupDocxPageLayout(word);
        var baos = new ByteArrayOutputStream();

        // Title
        if (paper.getTitle() != null && !paper.getTitle().isBlank()) {
            var p = word.createParagraph();
            p.setAlignment(ParagraphAlignment.CENTER);
            setDoubleSpacing(p);
            tnrRun(p, paper.getTitle(), true, false);
        }

        // Author
        if (paper.getAuthor() != null && !paper.getAuthor().isBlank()) {
            var p = word.createParagraph();
            p.setAlignment(ParagraphAlignment.CENTER);
            setDoubleSpacing(p);
            tnrRun(p, paper.getAuthor(), false, false);
        }

        // Thesis
        if (paper.getThesis() != null && !paper.getThesis().isBlank()) {
            var p = word.createParagraph();
            setDoubleSpacing(p);
            setInd(p, TWIPS_HALF, 0, 0);
            tnrRun(p, paper.getThesis(), false, true);
        }

        // Blank spacer before body
        setDoubleSpacing(word.createParagraph());

        // Body
        if (paper.getBody() != null && !paper.getBody().isBlank()) {
            renderNodeToDocx(word, MAPPER.readTree(paper.getBody()));
        }

        // Notes
        var footnotes = MAPPER.readTree(paper.getFootnotes() != null ? paper.getFootnotes() : "[]");
        if (footnotes.isArray() && !footnotes.isEmpty()) {
            setDoubleSpacing(word.createParagraph());
            docxSectionHeading(word, "Notes");
            int i = 1;
            for (var fn : footnotes) {
                var p = word.createParagraph();
                setSingleSpacing(p);
                setInd(p, 0, TWIPS_HALF, 0);
                tnrRun(p, (i++) + ". " + fn.asText(), false, false);
            }
        }

        // Bibliography
        var bibliography = MAPPER.readTree(paper.getBibliography() != null ? paper.getBibliography() : "[]");
        if (bibliography.isArray() && !bibliography.isEmpty()) {
            setDoubleSpacing(word.createParagraph());
            docxSectionHeading(word, "Bibliography");
            for (var bib : bibliography) {
                var p = word.createParagraph();
                setSingleSpacing(p);
                setInd(p, TWIPS_HALF, 0, TWIPS_HALF);  // left=0.5″, hanging=0.5″
                p.setSpacingAfter(240);                 // blank line between entries
                tnrRun(p, bib.asText(), false, false);
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
                setDoubleSpacing(p);
                setInd(p, 0, TWIPS_HALF, 0);
                addInlineRuns(p, node, false, false);
            }
            case "heading" -> {
                int lvl = node.path("attrs").path("level").asInt(1);
                var p = word.createParagraph();
                setDoubleSpacing(p);
                setSpacingBefore(p, 480);  // one double-spaced blank line before heading
                boolean bold = (lvl != 2);
                if (lvl <= 2) p.setAlignment(ParagraphAlignment.CENTER);
                else          p.setAlignment(ParagraphAlignment.LEFT);
                addInlineRuns(p, node, bold, false);
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
                            setSingleSpacing(p);
                            setInd(p, TWIPS_HALF, 0, 0);
                            p.setIndentationRight(TWIPS_HALF);
                            p.setSpacingBefore(240);
                            p.setSpacingAfter(240);
                            addInlineRuns(p, c, false, false);
                        }
                    }
                }
            }
            case "horizontalRule" -> setDoubleSpacing(word.createParagraph());
        }
    }

    private void addDocxListItem(XWPFDocument word, JsonNode itemNode, String prefix, int depth) {
        if (!itemNode.has("content")) return;
        boolean first = true;
        for (var c : itemNode.get("content")) {
            var nodeType = c.path("type").asText();
            if ("paragraph".equals(nodeType)) {
                var p = word.createParagraph();
                setDoubleSpacing(p);
                setInd(p, (depth + 1) * TWIPS_HALF, 0, 0);
                if (first) { tnrRun(p, prefix + " ", false, false); first = false; }
                addInlineRuns(p, c, false, false);
            } else if ("bulletList".equals(nodeType) && c.has("content")) {
                for (var sub : c.get("content")) addDocxListItem(word, sub, "•", depth + 1);
            } else if ("orderedList".equals(nodeType) && c.has("content")) {
                int i = 1;
                for (var sub : c.get("content")) addDocxListItem(word, sub, (i++) + ".", depth + 1);
            }
        }
    }

    private void addInlineRuns(XWPFParagraph para, JsonNode node, boolean defaultBold, boolean defaultItalic) {
        if (!node.has("content")) return;
        for (var child : node.get("content")) {
            if ("hardBreak".equals(child.path("type").asText())) {
                para.createRun().addBreak();
            } else if ("text".equals(child.path("type").asText())) {
                boolean bold = defaultBold, italic = defaultItalic, strike = false, code = false;
                if (child.has("marks")) {
                    for (var mark : child.get("marks")) {
                        switch (mark.path("type").asText()) {
                            case "bold"   -> bold   = true;
                            case "italic" -> italic  = true;
                            case "strike" -> strike = true;
                            case "code"   -> code   = true;
                        }
                    }
                }
                var run = para.createRun();
                run.setText(child.path("text").asText(""));
                run.setBold(bold);
                run.setItalic(italic);
                run.setStrikeThrough(strike);
                run.setFontSize(code ? 10 : 12);
                run.setFontFamily(code ? "Courier New" : "Times New Roman");
            }
        }
    }

    // ── DOCX layout helpers ───────────────────────────────────────────────────

    private void setupDocxPageLayout(XWPFDocument word) {
        var body   = word.getDocument().getBody();
        var sectPr = body.isSetSectPr() ? body.getSectPr() : body.addNewSectPr();
        // US Letter: 8.5″ × 11″
        var pgSz   = sectPr.isSetPgSz() ? sectPr.getPgSz() : sectPr.addNewPgSz();
        pgSz.setW(BigInteger.valueOf(12240));
        pgSz.setH(BigInteger.valueOf(15840));
        // 1″ margins all sides
        var pgMar  = sectPr.isSetPgMar() ? sectPr.getPgMar() : sectPr.addNewPgMar();
        pgMar.setTop(BigInteger.valueOf(TWIPS_1IN));
        pgMar.setBottom(BigInteger.valueOf(TWIPS_1IN));
        pgMar.setLeft(BigInteger.valueOf(TWIPS_1IN));
        pgMar.setRight(BigInteger.valueOf(TWIPS_1IN));
        pgMar.setHeader(BigInteger.valueOf(TWIPS_HALF));
    }

    private CTPPr getPPr(XWPFParagraph para) {
        return para.getCTP().isSetPPr() ? para.getCTP().getPPr() : para.getCTP().addNewPPr();
    }

    private CTSpacing getSpacing(CTPPr ppr) {
        return ppr.isSetSpacing() ? ppr.getSpacing() : ppr.addNewSpacing();
    }

    private void setDoubleSpacing(XWPFParagraph para) {
        var sp = getSpacing(getPPr(para));
        sp.setLine(LINE_DOUBLE);
        sp.setLineRule(STLineSpacingRule.AUTO);
        sp.setAfter(ZERO);
        sp.setBefore(ZERO);
    }

    private void setSingleSpacing(XWPFParagraph para) {
        var sp = getSpacing(getPPr(para));
        sp.setLine(LINE_SINGLE);
        sp.setLineRule(STLineSpacingRule.AUTO);
        sp.setAfter(ZERO);
        sp.setBefore(ZERO);
    }

    /** Sets spacing-before without touching line or after values. */
    private void setSpacingBefore(XWPFParagraph para, int twips) {
        getSpacing(getPPr(para)).setBefore(BigInteger.valueOf(twips));
    }

    /**
     * leftTwips   – all-lines left indent (0 = skip)
     * firstLine   – first-line-only indent (0 = skip; mutually exclusive with hanging)
     * hangingTwips – hanging indent (0 = skip; pair with leftTwips for bibliography)
     */
    private void setInd(XWPFParagraph para, int leftTwips, int firstLine, int hangingTwips) {
        var ppr = getPPr(para);
        CTInd ind = ppr.isSetInd() ? ppr.getInd() : ppr.addNewInd();
        if (leftTwips    > 0) ind.setLeft(BigInteger.valueOf(leftTwips));
        if (firstLine    > 0) ind.setFirstLine(BigInteger.valueOf(firstLine));
        if (hangingTwips > 0) ind.setHanging(BigInteger.valueOf(hangingTwips));
    }

    private void docxSectionHeading(XWPFDocument word, String text) {
        var p = word.createParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);
        setDoubleSpacing(p);
        tnrRun(p, text, true, false);
    }

    private void tnrRun(XWPFParagraph para, String text, boolean bold, boolean italic) {
        var run = para.createRun();
        run.setText(text);
        run.setBold(bold);
        run.setItalic(italic);
        run.setFontSize(12);
        run.setFontFamily("Times New Roman");
    }
}
