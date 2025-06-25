package com.ginkgooai.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationTextMarkup;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Slf4j
public class PDFHighlighter {

//    // 假设这是您提供的JSON数据
//    private static final String JSON_DATA = "[ " +
//            "  { \"question\": \"APPLICANT NAME:\", \"answer\": \"david wang\" }, " +
//            "  { \"question\": \"UNIQUE APPLICATION NUMBER:\", \"answer\": \"1212-0001-5341-5655/00\" }, " +
//            "  { \"question\": \"PASSPORT NUMBER:\", \"answer\": \"E12345678\" } " +
//            "]";
//
//    public static void main(String[] args) {
//        // Check if command line arguments are provided
//        String inputPath = "a.pdf";
//        String outputPath = "b.pdf";
//
//        if (args.length >= 2) {
//            inputPath = args[0];
//            outputPath = args[1];
//        } else if (args.length == 1) {
//            inputPath = args[0];
//            outputPath = inputPath.replace(".pdf", "_highlighted.pdf");
//        }
//
//        try {
//            File inputFile;
//            File outputFile = new File("G:\\ginkoo\\be-core-common\\src\\main\\resources\\b.pdf");
//
//            // First try to load from resources directory
//            inputFile = getResourceFile(inputPath);
//
//            // If not found in resources, try current directory
//            if (inputFile == null || !inputFile.exists()) {
//                inputFile = new File(inputPath);
//            }
//
//            // Check if input file exists
//            if (!inputFile.exists()) {
//                System.err.println("Error: Input PDF file not found: " + inputFile.getAbsolutePath());
//                System.out.println("Searched in:");
//                System.out.println("1. Resources directory: src/main/resources/");
//                System.out.println("2. Current directory: " + System.getProperty("user.dir"));
//                System.out.println("Please ensure the PDF file exists in one of these locations.");
//                System.out.println("Usage: java PDFHighlighter <input.pdf> [output.pdf]");
//                return;
//            }
//
//            // Check if input file is readable
//            if (!inputFile.canRead()) {
//                System.err.println("Error: Cannot read input PDF file: " + inputFile.getAbsolutePath());
//                System.out.println("Please check file permissions.");
//                return;
//            }
//
//            // Create output directory if it doesn't exist
//            File outputDir = outputFile.getParentFile();
//            if (outputDir != null && !outputDir.exists()) {
//                boolean created = outputDir.mkdirs();
//                if (!created) {
//                    System.err.println("Error: Cannot create output directory: " + outputDir.getAbsolutePath());
//                    return;
//                }
//            }
//
//            System.out.println("Processing PDF file: " + inputFile.getAbsolutePath());
//            System.out.println("Output will be saved to: " + outputFile.getAbsolutePath());
//
//            // First, analyze the PDF structure
//            System.out.println("\n=== ANALYZING PDF STRUCTURE ===");
//            analyzePDFStructure(inputFile);
//
//            System.out.println("\n=== STARTING HIGHLIGHTING PROCESS ===");
//            highlightAnswers(inputFile, outputFile, JSON_DATA);
//            System.out.println("PDF highlighting completed successfully!");
//
//        } catch (IOException e) {
//            System.err.println("Error processing PDF: " + e.getMessage());
//            e.printStackTrace();
//        } catch (Exception e) {
//            System.err.println("Unexpected error: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }

    /**
     * Analyze PDF structure to understand layout
     */
    public static void analyzePDFStructure(File inputFile) throws IOException {
        try (PDDocument document = PDDocument.load(inputFile)) {
            log.info("Total pages: {}", document.getNumberOfPages());

            for (int pageNum = 0; pageNum < Math.min(2, document.getNumberOfPages()); pageNum++) {
                log.info("\n--- PAGE {} ANALYSIS ---", pageNum + 1);

                PDFStructureAnalyzer analyzer = new PDFStructureAnalyzer();
                analyzer.analyzePage(document, pageNum);
            }
        }
    }

    /**
     * Find and highlight answers in PDF based on coordinate-based question-answer matching
     * @param document PDF document object
     * @param question Question text to match
     * @param answerToHighlight Answer text to highlight
     * @throws IOException
     */
    private static void findAndHighlight(PDDocument document, String question, String answerToHighlight) throws IOException {
        log.info("=== Searching for Question: '{}' with Answer: '{}' ===", question, answerToHighlight);

        for (int pageNum = 0; pageNum < document.getNumberOfPages(); pageNum++) {
            log.info("Processing page {}", pageNum + 1);

            try {
                // Create a new matcher instance for each page to avoid state pollution
                CoordinateBasedMatcher matcher = new CoordinateBasedMatcher();
                matcher.processPage(document, pageNum, question.trim(), answerToHighlight.trim());
            } catch (Exception e) {
                log.error("Error processing page {}: {}", pageNum + 1, e.getMessage(), e);
                // Continue processing other pages even if one fails
            }
        }
    }

    /**
     * 根据TextPosition列表添加高亮注释
     * @param document PDF文档
     * @param pageNum 页码 (0-based)
     * @param positions 要高亮的文本位置列表
     * @throws IOException
     */
    private static void highlightTextPositions(PDDocument document, int pageNum, List<TextPosition> positions) throws IOException {
        if (positions.isEmpty()) return;

        PDPage page = document.getPage(pageNum);
        List<PDAnnotation> annotations = page.getAnnotations();
        PDRectangle pageBox = page.getCropBox();
        float pageHeight = pageBox.getHeight();

        // Debug page info
        System.out.println("    → Page height: " + pageHeight + ", Page Y range: 0 to " + pageHeight);
        System.out.println("    → Creating highlight for " + positions.size() + " text positions");

        // Group consecutive positions to avoid large rectangles
        List<List<TextPosition>> groups = groupConsecutivePositions(positions);
        System.out.println("    → Grouped into " + groups.size() + " consecutive segments");

        for (int groupIndex = 0; groupIndex < groups.size(); groupIndex++) {
            List<TextPosition> group = groups.get(groupIndex);
            System.out.println("    → Processing group " + (groupIndex + 1) + " with " + group.size() + " positions");

            // Find the bounding box for this group
            float minX = Float.MAX_VALUE;
            float maxX = Float.MIN_VALUE;
            float minY = Float.MAX_VALUE;
            float maxY = Float.MIN_VALUE;

            for (TextPosition pos : group) {
                float x = pos.getXDirAdj();
                float y = pos.getYDirAdj();
                float width = pos.getWidth();
                float height = pos.getHeight();

                minX = Math.min(minX, x);
                maxX = Math.max(maxX, x + width);

                // Fine-tune Y coordinate conversion to better align with text
                // Adjust the offset slightly to match text position better
                float yTop = pageHeight - y + 6;     // Balance the upward adjustment
                float yBottom = pageHeight - y - height + 6;  // Keep consistent offset

                minY = Math.min(minY, yBottom);
                maxY = Math.max(maxY, yTop);
            }

            System.out.println("    → Group " + (groupIndex + 1) + " bounds: X=" +
                    String.format("%.1f", minX) + "-" + String.format("%.1f", maxX) +
                    ", Y=" + String.format("%.1f", minY) + "-" + String.format("%.1f", maxY));

            // Create highlight rectangle for this group only if it's reasonable size
            float width = maxX - minX;
            float height = maxY - minY;

            if (width > 0 && height > 0 && width < 500) { // Reasonable width limit
                // Fine-tune highlight position with adjustable margins
                float leftMargin = 1;
                float rightMargin = 1;
                float topMargin = 1;    // Reduce top margin
                float bottomMargin = -1; // Small negative bottom margin

                PDRectangle highlightRect = new PDRectangle();
                highlightRect.setLowerLeftX(minX - leftMargin);
                highlightRect.setLowerLeftY(minY - bottomMargin);
                highlightRect.setUpperRightX(maxX + rightMargin);
                highlightRect.setUpperRightY(maxY + topMargin);

                // 创建高亮注释
                PDAnnotationTextMarkup highlight = new PDAnnotationTextMarkup(PDAnnotationTextMarkup.SUB_TYPE_HIGHLIGHT);
                highlight.setRectangle(highlightRect);

                // 设置高亮颜色 (黄色)
                float[] color = {1, 1, 0};
                highlight.setColor(new PDColor(color, PDDeviceRGB.INSTANCE));

                // 设置四边形坐标
                float[] quads = new float[8];
                quads[0] = minX - leftMargin;        // 左下角 X
                quads[1] = minY - bottomMargin;      // 左下角 Y
                quads[2] = maxX + rightMargin;       // 右下角 X
                quads[3] = minY - bottomMargin;      // 右下角 Y
                quads[4] = minX - leftMargin;        // 左上角 X
                quads[5] = maxY + topMargin;         // 左上角 Y
                quads[6] = maxX + rightMargin;       // 右上角 X
                quads[7] = maxY + topMargin;         // 右上角 Y
                highlight.setQuadPoints(quads);

                annotations.add(highlight);

                System.out.println("    → Added highlight group " + (groupIndex + 1) + ": " +
                        String.format("(%.1f,%.1f,%.1f,%.1f)",
                                highlightRect.getLowerLeftX(), highlightRect.getLowerLeftY(),
                                highlightRect.getUpperRightX(), highlightRect.getUpperRightY()));
            } else {
                System.out.println("    → Skipped group " + (groupIndex + 1) + " - unreasonable size: " +
                        String.format("%.1fx%.1f", width, height));
            }
        }
    }

    /**
     * 在PDF中高亮JSON数据中指定的答案
     * @param inputFile 输入的PDF文件
     * @param outputFile 输出的PDF文件
     * @param jsonData 包含问答对的JSON字符串
     * @throws IOException
     */
    public static void highlightAnswers(File inputFile, File outputFile, String jsonData) throws IOException {
        try (PDDocument document = PDDocument.load(inputFile)) {
            JSONArray qaArray = JSON.parseArray(jsonData);

            log.info("\n🔍 === PDF HIGHLIGHTING START ===");
            log.info("Input PDF: {}", inputFile.getName());
            log.info("Output PDF: {}", outputFile.getName());
            log.info("JSON Data: {}", jsonData);
            log.info("Total Q&A pairs to process: {}", qaArray.size());

            for (int i = 0; i < qaArray.size(); i++) {
                JSONObject qa = qaArray.getJSONObject(i);
                String question = qa.getString("question");
                String answer = qa.getString("answer");

                log.info("\n📋 Processing Q&A pair {}/{}", i + 1, qaArray.size());
                log.info("Question: \"{}\"", question);
                log.info("Answer: \"{}\"", answer);

                // 在PDF中查找并高亮答案
                findAndHighlight(document, question, answer);
            }

            document.save(outputFile);
            log.info("\n✅ === PDF HIGHLIGHTING COMPLETED ===");
            log.info("Result saved to: {}", outputFile.getAbsolutePath());
        }
    }

    /**
     * Group consecutive text positions to avoid creating overly large highlight rectangles
     */
    private static List<List<TextPosition>> groupConsecutivePositions(List<TextPosition> positions) {
        List<List<TextPosition>> groups = new ArrayList<>();
        if (positions.isEmpty()) return groups;

        List<TextPosition> currentGroup = new ArrayList<>();
        currentGroup.add(positions.get(0));

        for (int i = 1; i < positions.size(); i++) {
            TextPosition current = positions.get(i);
            TextPosition previous = positions.get(i - 1);

            // Check if positions are close enough to be in the same group
            float xGap = current.getXDirAdj() - (previous.getXDirAdj() + previous.getWidth());
            float yDiff = Math.abs(current.getYDirAdj() - previous.getYDirAdj());

            // If gap is too large (more than 20 points) or Y difference is too large, start new group
            if (xGap > 20 || yDiff > 2) {
                groups.add(currentGroup);
                currentGroup = new ArrayList<>();
            }

            currentGroup.add(current);
        }

        if (!currentGroup.isEmpty()) {
            groups.add(currentGroup);
        }

        return groups;
    }

    /**
     * Get file from resources directory
     * @param fileName the name of the file in resources
     * @return File object pointing to the resource file, or null if not found
     */
    private static File getResourceFile(String fileName) {
        try {
            // Method 1: Try to get resource from classpath
            ClassLoader classLoader = PDFHighlighter.class.getClassLoader();
            java.net.URL resource = classLoader.getResource(fileName);
            if (resource != null) {
                return new File(resource.toURI());
            }

            // Method 2: Try relative path to resources directory
            File resourcesDir = new File("src/main/resources");
            if (resourcesDir.exists()) {
                File resourceFile = new File(resourcesDir, fileName);
                if (resourceFile.exists()) {
                    return resourceFile;
                }
            }

        } catch (Exception e) {
            System.out.println("Note: Could not load from resources via classpath, trying file system path...");
        }

        return null;
    }

    /**
     * Represents a separator line in the PDF
     */
    private static class SeparatorLine {
        float y;
        int gap;

        SeparatorLine(float y, int gap) {
            this.y = y;
            this.gap = gap;
        }
    }

    /**
     * Represents a question-answer region between separator lines
     */
    private static class QARegion {
        float topY;
        float bottomY;
        List<TextElement> elements;

        QARegion(float topY, float bottomY, List<TextElement> elements) {
            this.topY = topY;
            this.bottomY = bottomY;
            this.elements = elements;
        }
    }

    /**
     * PDF structure analyzer to understand text layout
     */
    private static class PDFStructureAnalyzer extends PDFTextStripper {
        private int pageNum;
        private List<TextElement> textElements = new ArrayList<>();

        public PDFStructureAnalyzer() throws IOException {
            super();
        }

        public void analyzePage(PDDocument document, int pageNumber) throws IOException {
            this.pageNum = pageNumber;
            this.textElements.clear();

            this.setStartPage(pageNumber + 1);
            this.setEndPage(pageNumber + 1);

            // Extract text with positions
            this.getText(document);

            // Analyze the layout
            analyzeLayout();
        }

        @Override
        protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
            super.writeString(text, textPositions);

            for (int i = 0; i < text.length() && i < textPositions.size(); i++) {
                char ch = text.charAt(i);
                TextPosition pos = textPositions.get(i);

                if (!Character.isWhitespace(ch)) {
                    textElements.add(new TextElement(ch, pos.getXDirAdj(), pos.getYDirAdj(), pos.getWidthDirAdj(), pos.getHeightDir(), pos));
                }
            }
        }

        private void analyzeLayout() {
            if (textElements.isEmpty()) {
                log.warn("No text found on this page");
                return;
            }

            // Group text by lines (similar Y coordinates)
            Map<Integer, List<TextElement>> lineGroups = new TreeMap<>();
            for (TextElement element : textElements) {
                int lineY = (int) Math.round(element.y / 5) * 5; // Group by 5-point intervals
                lineGroups.computeIfAbsent(lineY, k -> new ArrayList<>()).add(element);
            }

            log.info("Found {} text lines", lineGroups.size());

            // Analyze each line
            int lineCount = 0;
            for (Map.Entry<Integer, List<TextElement>> entry : lineGroups.entrySet()) {
                lineCount++;
                if (lineCount > 20) break; // Limit output

                List<TextElement> line = entry.getValue();
                line.sort((a, b) -> Float.compare(a.x, b.x)); // Sort by X position

                StringBuilder lineText = new StringBuilder();
                float minX = Float.MAX_VALUE, maxX = Float.MIN_VALUE;

                for (TextElement element : line) {
                    lineText.append(element.character);
                    minX = Math.min(minX, element.x);
                    maxX = Math.max(maxX, element.x);
                }

                String text = lineText.toString().trim();
                if (text.length() > 0) {
                    log.info("Line {} (Y={}, X={}-{}): {}",
                            lineCount, String.format("%.0f", (float) entry.getKey()),
                            String.format("%.0f", minX), String.format("%.0f", maxX), text);

                    // Check if this might be a question-answer pair
                    if (text.contains(":")) {
                        analyzeQuestionAnswerLine(text, line);
                    }
                }
            }

            // Analyze column structure
            analyzeColumnStructure();
        }

        private void analyzeQuestionAnswerLine(String text, List<TextElement> line) {
            int colonIndex = text.indexOf(":");
            if (colonIndex > 0) {
                String beforeColon = text.substring(0, colonIndex + 1).trim();
                String afterColon = text.substring(colonIndex + 1).trim();

                if (beforeColon.length() > 0 && afterColon.length() > 0) {
                    // Find X positions for question and answer
                    float questionEndX = 0;
                    float answerStartX = Float.MAX_VALUE;

                    int charIndex = 0;
                    for (TextElement element : line) {
                        if (charIndex <= colonIndex) {
                            questionEndX = Math.max(questionEndX, element.x + element.width);
                        } else if (charIndex > colonIndex && !Character.isWhitespace(element.character)) {
                            answerStartX = Math.min(answerStartX, element.x);
                            break;
                        }
                        charIndex++;
                    }

                    log.info("    → Question: '{}' (ends at X={})", beforeColon, String.format("%.0f", questionEndX));
                    log.info("    → Answer: '{}' (starts at X={})", afterColon, String.format("%.0f", answerStartX));
                    log.info("    → Gap: {} points", String.format("%.0f", answerStartX - questionEndX));
                }
            }
        }

        private void analyzeColumnStructure() {
            // Find typical X positions to understand column structure
            Map<Integer, Integer> xPositions = new HashMap<>();
            for (TextElement element : textElements) {
                int roundedX = (int) Math.round(element.x / 10) * 10; // Round to 10-point intervals
                xPositions.put(roundedX, xPositions.getOrDefault(roundedX, 0) + 1);
            }

            log.info("\nX position frequency (showing top positions):");
            xPositions.entrySet().stream()
                    .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                    .limit(10)
                    .forEach(entry -> log.info("X={}: {} chars", entry.getKey(), entry.getValue()));
        }


    }

    /**
     * Enhanced text element with position information
     */
    private static class TextElement {
        char character;
        float x, y, width, height;
        TextPosition position; // Keep original position for highlighting

        TextElement(char ch, float x, float y, float width, float height, TextPosition pos) {
            this.character = ch;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.position = pos;
        }
    }

    /**
     * Helper method to create a sample PDF for testing
     * @param outputFile The file where the sample PDF will be created
     * @throws IOException
     */
    public static void createSamplePDF(File outputFile) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            // Note: This method requires additional PDFBox dependencies for content stream manipulation
            // For a complete implementation, you would need to add text content using PDPageContentStream
            System.out.println("Sample PDF creation method available.");
            System.out.println("To create a test PDF, you can:");
            System.out.println("1. Use any existing PDF file");
            System.out.println("2. Create a simple PDF using online tools");
            System.out.println("3. Use other libraries like iText to generate test content");

            document.save(outputFile);
        }
    }

    /**
     * Enhanced coordinate-based matcher that detects separator lines and handles column layout
     */
    private static class CoordinateBasedMatcher extends PDFTextStripper {
        private PDDocument document;
        private int pageNum;
        private String targetQuestion;
        private String targetAnswer;
        private List<TextElement> allTextElements = new ArrayList<>();
        private List<SeparatorLine> separatorLines = new ArrayList<>();
        private List<QARegion> qaRegions = new ArrayList<>();

        public CoordinateBasedMatcher() throws IOException {
            super();
            setSuppressDuplicateOverlappingText(false);
            setSortByPosition(true);
            setAddMoreFormatting(false);
        }

        public void processPage(PDDocument doc, int pageNumber, String question, String answer) throws IOException {
            this.document = doc;
            this.pageNum = pageNumber;
            this.targetQuestion = question.trim();
            this.targetAnswer = answer.trim();
            this.allTextElements.clear();
            this.separatorLines.clear();
            this.qaRegions.clear();

            log.info("\n=== Processing Page {} ===", pageNumber + 1);
            log.info("Target Question: \"{}\"", targetQuestion);
            log.info("Target Answer: \"{}\"", targetAnswer);

            // Set page range for text extraction  
            this.setStartPage(pageNumber + 1);
            this.setEndPage(pageNumber + 1);

            log.info("🔍 === STARTING TEXT EXTRACTION ===");

            // Extract all text elements
            try {
                String extractedText = this.getText(doc);
                log.info("📄 Raw extracted text length: {}", extractedText.length());
                log.info("📄 Raw extracted text: \"{}\"",
                        extractedText.length() > 200 ? extractedText.substring(0, 200) + "..." : extractedText);
            } catch (Exception e) {
                log.error("❌ Error during text extraction: {}", e.getMessage());
                e.printStackTrace();
                throw e; // Re-throw to be caught by the caller
            }

            log.info("📊 Total text elements collected: {}", allTextElements.size());

            if (allTextElements.isEmpty()) {
                log.warn("❌ === NO TEXT ELEMENTS FOUND ON PAGE {} ===", pageNumber + 1);
                log.warn("This could indicate:");
                log.warn("  1. PDF contains only images/scanned content on this page");
                log.warn("  2. Text extraction failed for this page");
                log.warn("  3. PDF page is encrypted or protected");
                log.warn("  4. Page {} might not contain text content", pageNumber + 1);

                // Try alternative text extraction approach for debugging
                try {
                    log.info("Attempting alternative text extraction...");
                    PDFTextStripper debugStripper = new PDFTextStripper();
                    debugStripper.setStartPage(pageNumber + 1);
                    debugStripper.setEndPage(pageNumber + 1);
                    String debugText = debugStripper.getText(document);
                    log.info("Alternative extraction result - Text length: {}", debugText.length());
                    if (debugText.length() > 0) {
                        log.info("Alternative extraction - First 200 chars: \"{}\"",
                                debugText.length() > 200 ? debugText.substring(0, 200) + "..." : debugText);
                    }
                } catch (Exception e) {
                    log.error("Alternative text extraction also failed: {}", e.getMessage());
                }
                return;
            }

            // Show detailed Y coordinate analysis
            analyzeYCoordinates();

            // Detect separator lines in the PDF
            detectSeparatorLines();

            // Create Q&A regions based on separator lines
            createQARegions();

            // Try to find and highlight the question-answer pair
            findAndHighlightByRegions();
        }

        /**
         * Analyze and display all Y coordinates found in the PDF
         */
        private void analyzeYCoordinates() {
            log.info("\n📍 === Y COORDINATE ANALYSIS ===");

            // Group by Y coordinates
            Map<Float, List<TextElement>> yGroups = new TreeMap<>(Collections.reverseOrder());
            for (TextElement element : allTextElements) {
                yGroups.computeIfAbsent(element.y, k -> new ArrayList<>()).add(element);
            }

            log.info("Found {} unique Y coordinates:", yGroups.size());

            int lineIndex = 1;
            for (Map.Entry<Float, List<TextElement>> entry : yGroups.entrySet()) {
                float y = entry.getKey();
                List<TextElement> elements = entry.getValue();

                // Sort elements by X coordinate
                elements.sort((a, b) -> Float.compare(a.x, b.x));

                // Build text for this Y coordinate
                StringBuilder lineText = new StringBuilder();
                float lastX = -1;
                for (TextElement element : elements) {
                    if (lastX > 0 && element.x - lastX > 5) {
                        lineText.append(" ");
                    }
                    lineText.append(element.character);
                    lastX = element.x + element.width;
                }

                log.info("  Line {} Y={} ({} chars): \"{}\"",
                        lineIndex, String.format("%.2f", y), elements.size(), lineText.toString().trim());
                lineIndex++;
            }
        }

        @Override
        protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
            log.debug("🔤 writeString called with text: \"{}\" ({} chars)", text, text.length());
            log.debug("   TextPositions count: {}", textPositions.size());

            // Store all text elements with their positions for later analysis
            int characterCount = 0;
            for (TextPosition position : textPositions) {
                String textString = position.getUnicode();
                if (textString != null && !textString.trim().isEmpty()) {

                    // Debug first few positions
                    if (characterCount < 5) {
                        log.debug("   Position {}: '{}' at X={}, Y={}, W={}, H={}",
                                characterCount, textString,
                                String.format("%.2f", position.getXDirAdj()),
                                String.format("%.2f", position.getYDirAdj()),
                                String.format("%.2f", position.getWidthDirAdj()),
                                String.format("%.2f", position.getHeightDir()));
                    }

                    for (char ch : textString.toCharArray()) {
                        if (!Character.isWhitespace(ch)) {
                            allTextElements.add(new TextElement(
                                    ch,
                                    position.getXDirAdj(),
                                    position.getYDirAdj(),
                                    position.getWidthDirAdj(),
                                    position.getHeightDir(),
                                    position
                            ));
                            characterCount++;
                        }
                    }
                }
            }

            log.debug("   Added {} non-whitespace characters", characterCount);
            log.debug("   Total elements so far: {}", allTextElements.size());
        }

        /**
         * Detect horizontal separator lines by analyzing Y coordinate gaps
         */
        private void detectSeparatorLines() {
            if (allTextElements.isEmpty()) {
                log.warn("No text elements found for separator detection");
                return;
            }

            // Group elements by Y coordinate to find text lines
            Map<Integer, List<TextElement>> yGroups = new TreeMap<>();
            for (TextElement element : allTextElements) {
                int roundedY = Math.round(element.y / 3) * 3; // Group by 3-point intervals
                yGroups.computeIfAbsent(roundedY, k -> new ArrayList<>()).add(element);
            }

            // Convert to sorted list of Y coordinates
            List<Integer> yCoordinates = new ArrayList<>(yGroups.keySet());
            yCoordinates.sort(Collections.reverseOrder()); // PDF Y goes from bottom to top

            log.info("Found {} text lines at Y coordinates: {}",
                    yCoordinates.size(), yCoordinates.subList(0, Math.min(10, yCoordinates.size())));

            // Find significant gaps between text lines (these indicate separator lines)
            for (int i = 0; i < yCoordinates.size() - 1; i++) {
                int currentY = yCoordinates.get(i);
                int nextY = yCoordinates.get(i + 1);

                // Calculate gap (remember Y coordinates are in reverse order)
                int gap = currentY - nextY;

                // If gap is significant (more than 15 points), there's likely a separator line
                if (gap > 15) {
                    float separatorY = (currentY + nextY) / 2.0f;
                    separatorLines.add(new SeparatorLine(separatorY, gap));
                    log.info("Detected separator line at Y={} (gap: {} points)",
                            String.format("%.1f", separatorY), gap);
                }
            }

            log.info("Total separator lines detected: {}", separatorLines.size());
        }

        /**
         * Create Q&A regions based on separator lines
         */
        private void createQARegions() {
            if (separatorLines.isEmpty()) {
                // No separator lines found, treat entire page as one region
                log.info("No separator lines found, using entire page as one region");
                qaRegions.add(new QARegion(Float.MAX_VALUE, Float.MIN_VALUE, allTextElements));
                return;
            }

            // Sort separator lines by Y coordinate (descending)
            separatorLines.sort((a, b) -> Float.compare(b.y, a.y));

            // Create regions between separator lines
            List<Float> boundaries = new ArrayList<>();
            boundaries.add(Float.MAX_VALUE); // Top boundary

            for (SeparatorLine line : separatorLines) {
                boundaries.add(line.y);
            }

            boundaries.add(Float.MIN_VALUE); // Bottom boundary

            // Create Q&A regions
            for (int i = 0; i < boundaries.size() - 1; i++) {
                float topY = boundaries.get(i);
                float bottomY = boundaries.get(i + 1);

                List<TextElement> regionElements = new ArrayList<>();
                for (TextElement element : allTextElements) {
                    if (element.y <= topY && element.y > bottomY) {
                        regionElements.add(element);
                    }
                }

                if (!regionElements.isEmpty()) {
                    QARegion region = new QARegion(topY, bottomY, regionElements);
                    qaRegions.add(region);

                    log.info("Created Q&A region {}: Y={} to {} ({} characters)",
                            qaRegions.size(),
                            String.format("%.1f", topY),
                            String.format("%.1f", bottomY),
                            regionElements.size());
                }
            }

            log.info("Total Q&A regions created: {}", qaRegions.size());
        }

        /**
         * Find and highlight question-answer pairs within regions
         */
        private void findAndHighlightByRegions() throws IOException {
            log.info("\n🔍 === STARTING REGION-BASED MATCHING ===");
            log.info("Total characters extracted: {}", allTextElements.size());
            log.info("Total Q&A regions: {}", qaRegions.size());

            if (qaRegions.isEmpty()) {
                log.warn("❌ No Q&A regions found for matching");
                return;
            }

            boolean foundMatch = false;

            // Try to find matches in each Q&A region - don't break on first match, check all regions
            for (int i = 0; i < qaRegions.size(); i++) {
                QARegion region = qaRegions.get(i);
                log.info("\n📍 === Analyzing Q&A Region {}/{} ===", i + 1, qaRegions.size());
                log.info("Region Y range: {} to {}", String.format("%.1f", region.topY), String.format("%.1f", region.bottomY));
                log.info("Region characters: {}", region.elements.size());

                // Show a sample of text in this region
                String regionText = buildColumnText(region.elements);
                System.out.println("Region text sample: \"" +
                        (regionText.length() > 100 ? regionText.substring(0, 100) + "..." : regionText) + "\"");

                if (tryRegionBasedMatching(region)) {
                    foundMatch = true;
                    log.info("✅ Found match in region {}", i + 1);
                    // Continue to check other regions for more matches - don't break!
                } else {
                    log.info("❌ No match found in region {}", i + 1);
                }
            }

            if (!foundMatch) {
                log.warn("\n❌ === NO MATCHING FOUND ===");
                log.warn("No matching question-answer pair found on page {}", pageNum + 1);
                log.warn("Target Question: \"{}\"", targetQuestion);
                log.warn("Target Answer: \"{}\"", targetAnswer);

                // Show all text found on the page for debugging
                System.out.println("\n📄 === ALL TEXT ON PAGE ===");
                String allText = buildColumnText(allTextElements);
                System.out.println("Full page text: \"" + allText + "\"");
            }
        }

        /**
         * NEW APPROACH: Line-by-line analysis with 1-6 line combinations for Q&A matching
         */
        private boolean tryRegionBasedMatching(QARegion region) throws IOException {
            List<TextElement> elements = region.elements;
            if (elements.isEmpty()) {
                System.out.println("    ❌ Region is empty, skipping");
                return false;
            }

            System.out.println("    🔧 === LINE-BY-LINE REGION ANALYSIS ===");
            System.out.println("    Region Y range: " + String.format("%.1f", region.topY) + " to " + String.format("%.1f", region.bottomY));
            System.out.println("    Total characters in region: " + elements.size());

            // Step 1: Detect column boundary
            float columnBoundary = detectColumnBoundaryInRegion(elements);

            // Step 2: Group elements by lines (Y coordinates)
            Map<Integer, List<TextElement>> lineGroups = new TreeMap<>(Collections.reverseOrder());
            for (TextElement element : elements) {
                int lineKey = Math.round(element.y / 3) * 3; // Group by 3-point intervals
                lineGroups.computeIfAbsent(lineKey, k -> new ArrayList<>()).add(element);
            }

            System.out.println("    📄 === FOUND " + lineGroups.size() + " LINES IN REGION ===");

            // Step 3: Analyze each line and split into question/answer parts
            List<LineQA> lineQAs = new ArrayList<>();
            int lineIndex = 1;

            for (Map.Entry<Integer, List<TextElement>> entry : lineGroups.entrySet()) {
                List<TextElement> lineElements = entry.getValue();
                lineElements.sort((a, b) -> Float.compare(a.x, b.x)); // Sort by X within line

                // Split this line into left (question) and right (answer) parts
                List<TextElement> questionPart = new ArrayList<>();
                List<TextElement> answerPart = new ArrayList<>();

                for (TextElement element : lineElements) {
                    if (element.x < columnBoundary) {
                        questionPart.add(element);
                    } else {
                        answerPart.add(element);
                    }
                }

                String questionText = buildLineText(questionPart).trim();
                String answerText = buildLineText(answerPart).trim();

                LineQA lineQA = new LineQA(entry.getKey(), questionText, answerText, questionPart, answerPart);
                lineQAs.add(lineQA);

                System.out.println("    Line " + lineIndex + " (Y=" + entry.getKey() + "):");
                System.out.println("      Question part: \"" + questionText + "\"");
                System.out.println("      Answer part: \"" + answerText + "\"");

                lineIndex++;
            }

            // Step 4: Try combinations of 1-6 consecutive lines for matching
            System.out.println("    🎯 === TRYING LINE COMBINATIONS (1-6 lines) ===");

            boolean foundAnyMatch = false;
            Set<Integer> alreadyHighlightedLines = new HashSet<>();
            
            for (int startLine = 0; startLine < lineQAs.size(); startLine++) {
                // Skip if this line was already part of a highlighted match
                if (alreadyHighlightedLines.contains(startLine)) {
                    continue;
                }
                
                for (int lineCount = 1; lineCount <= Math.min(6, lineQAs.size() - startLine); lineCount++) {
                    if (tryLineCombination(lineQAs, startLine, lineCount)) {
                        foundAnyMatch = true;
                        // Mark these lines as highlighted to avoid overlap
                        for (int i = startLine; i < startLine + lineCount; i++) {
                            alreadyHighlightedLines.add(i);
                        }
                        // Don't break - but skip checking overlapping combinations for this starting line
                        break;
                    }
                }
            }

            if (!foundAnyMatch) {
                System.out.println("    ❌ === NO MATCH FOUND IN ANY LINE COMBINATION ===");
            }
            return foundAnyMatch;
        }

        /**
         * Try matching with a specific combination of consecutive lines
         */
        private boolean tryLineCombination(List<LineQA> lineQAs, int startIndex, int lineCount) throws IOException {
            System.out.println("      🔍 Testing lines " + (startIndex + 1) + "-" + (startIndex + lineCount) +
                    " (" + lineCount + " lines):");

            // Combine question parts from consecutive lines
            StringBuilder combinedQuestion = new StringBuilder();
            StringBuilder combinedAnswer = new StringBuilder();
            List<TextElement> allQuestionElements = new ArrayList<>();
            List<TextElement> allAnswerElements = new ArrayList<>();

            for (int i = startIndex; i < startIndex + lineCount; i++) {
                LineQA lineQA = lineQAs.get(i);

                if (lineQA.questionText.length() > 0) {
                    if (combinedQuestion.length() > 0) combinedQuestion.append(" ");
                    combinedQuestion.append(lineQA.questionText);
                    allQuestionElements.addAll(lineQA.questionElements);
                }

                if (lineQA.answerText.length() > 0) {
                    if (combinedAnswer.length() > 0) combinedAnswer.append(" ");
                    combinedAnswer.append(lineQA.answerText);
                    allAnswerElements.addAll(lineQA.answerElements);
                }
            }

            String finalQuestionText = combinedQuestion.toString().trim();
            String finalAnswerText = combinedAnswer.toString().trim();

            System.out.println("        Combined Question: \"" + finalQuestionText + "\"");
            System.out.println("        Combined Answer: \"" + finalAnswerText + "\"");
            System.out.println("        Target Question: \"" + targetQuestion + "\"");
            System.out.println("        Target Answer: \"" + targetAnswer + "\"");

            // Perform matching
            boolean questionMatch = performDetailedQuestionMatch(finalQuestionText, targetQuestion);
            boolean answerMatch = performDetailedAnswerMatch(finalAnswerText, targetAnswer);

            System.out.println("        Question Match: " + (questionMatch ? "✅ YES" : "❌ NO"));
            System.out.println("        Answer Match: " + (answerMatch ? "✅ YES" : "❌ NO"));

            if (questionMatch && answerMatch) {
                System.out.println("        🎉 === PERFECT MATCH FOUND ===");
                System.out.println("        Highlighting " + allQuestionElements.size() + " question characters and " +
                        allAnswerElements.size() + " answer characters");

                // Highlight both question and answer
                highlightElements(allQuestionElements);
                highlightElements(allAnswerElements);
                return true;
            } else if (questionMatch) {
                System.out.println("        ⚠️ === PARTIAL MATCH (Question Only) ===");
                System.out.println("        Question matched but answer didn't. Checking if we should still highlight...");

                // Only highlight question if answer text is too different or empty
                if (finalAnswerText.trim().isEmpty()) {
                    System.out.println("        Answer text is empty, highlighting question only");
                    highlightElements(allQuestionElements);
                    return true;
                } else {
                    System.out.println("        Answer text exists but doesn't match. Skipping to find better match.");
                    System.out.println("        Answer text was: \"" + finalAnswerText + "\"");
                    return false; // Don't highlight partial matches when answer exists but doesn't match
                }
            }

            return false;
        }

        /**
         * Helper class to store line-based question-answer data
         */
        private static class LineQA {
            int yCoordinate;
            String questionText;
            String answerText;
            List<TextElement> questionElements;
            List<TextElement> answerElements;

            LineQA(int y, String q, String a, List<TextElement> qElements, List<TextElement> aElements) {
                this.yCoordinate = y;
                this.questionText = q;
                this.answerText = a;
                this.questionElements = new ArrayList<>(qElements);
                this.answerElements = new ArrayList<>(aElements);
            }
        }

        /**
         * Build text from elements in a single line
         */
        private String buildLineText(List<TextElement> elements) {
            if (elements.isEmpty()) return "";

            StringBuilder text = new StringBuilder();
            float lastX = -1;

            for (TextElement element : elements) {
                // Add space if there's a significant gap (word separation)
                if (lastX > 0 && element.x - lastX > 5) {
                    text.append(" ");
                }
                text.append(element.character);
                lastX = element.x + element.width;
            }

            return text.toString();
        }

        /**
         * Build complete text from column elements, properly handling multi-line content
         */
        private String buildCompleteColumnText(List<TextElement> elements) {
            if (elements.isEmpty()) return "";

            // Group elements by Y coordinate (lines) first
            Map<Integer, List<TextElement>> lineGroups = new TreeMap<>(Collections.reverseOrder());
            for (TextElement element : elements) {
                int lineKey = Math.round(element.y / 3) * 3; // Group by 3-point intervals
                lineGroups.computeIfAbsent(lineKey, k -> new ArrayList<>()).add(element);
            }

            StringBuilder completeText = new StringBuilder();

            // Process each line and join them
            for (Map.Entry<Integer, List<TextElement>> entry : lineGroups.entrySet()) {
                List<TextElement> lineElements = entry.getValue();
                lineElements.sort((a, b) -> Float.compare(a.x, b.x)); // Sort by X within line

                StringBuilder lineText = new StringBuilder();
                float lastX = -1;

                for (TextElement element : lineElements) {
                    // Add space if there's a significant gap (word separation)
                    if (lastX > 0 && element.x - lastX > 5) {
                        lineText.append(" ");
                    }
                    lineText.append(element.character);
                    lastX = element.x + element.width;
                }

                String cleanLineText = lineText.toString().trim();
                if (cleanLineText.length() > 0) {
                    if (completeText.length() > 0) {
                        completeText.append(" "); // Add space between lines
                    }
                    completeText.append(cleanLineText);
                }
            }

            return completeText.toString();
        }

        /**
         * Perform detailed question matching with comprehensive analysis
         */
        private boolean performDetailedQuestionMatch(String extractedText, String targetQuestion) {
            System.out.println("      🔍 === DETAILED QUESTION ANALYSIS ===");

            String normalizedExtracted = normalizeText(extractedText);
            String normalizedTarget = normalizeText(targetQuestion);

            System.out.println("      Normalized extracted: \"" + normalizedExtracted + "\"");
            System.out.println("      Normalized target: \"" + normalizedTarget + "\"");

            // Strategy 1: Direct substring match
            if (normalizedExtracted.contains(normalizedTarget)) {
                System.out.println("      ✅ Direct substring match found!");
                return true;
            }

            if (normalizedTarget.contains(normalizedExtracted)) {
                System.out.println("      ✅ Target contains extracted text!");
                return true;
            }

            // Strategy 2: Word-based matching
            String[] targetWords = normalizedTarget.split("\\s+");
            String[] extractedWords = normalizedExtracted.split("\\s+");

            if (targetWords.length == 0) {
                System.out.println("      ❌ No target words to match");
                return false;
            }

            int matchedWords = 0;
            System.out.println("      📝 Word-by-word analysis:");

            for (String targetWord : targetWords) {
                if (targetWord.length() <= 2) continue; // Skip very short words

                boolean wordFound = false;
                for (String extractedWord : extractedWords) {
                    if (extractedWord.contains(targetWord) || targetWord.contains(extractedWord)) {
                        wordFound = true;
                        matchedWords++;
                        break;
                    }
                }
                System.out.println("        \"" + targetWord + "\" -> " + (wordFound ? "✅" : "❌"));
            }

            double matchRatio = (double) matchedWords / targetWords.length;
            System.out.println("      📊 Match ratio: " + String.format("%.2f", matchRatio) + " (threshold: 0.7)");

            return matchRatio >= 0.7; // Lower threshold for better matching
        }

        /**
         * Perform detailed answer matching
         */
        private boolean performDetailedAnswerMatch(String extractedText, String targetAnswer) {
            System.out.println("      🔍 === DETAILED ANSWER ANALYSIS ===");

            String normalizedExtracted = normalizeText(extractedText);
            String normalizedTarget = normalizeText(targetAnswer);

            System.out.println("      Normalized extracted: \"" + normalizedExtracted + "\"");
            System.out.println("      Normalized target: \"" + normalizedTarget + "\"");

            // Strategy 1: Direct matching
            if (normalizedExtracted.contains(normalizedTarget) ||
                    normalizedTarget.contains(normalizedExtracted) ||
                    normalizedExtracted.equals(normalizedTarget)) {
                System.out.println("      ✅ Direct match found!");
                return true;
            }

            // Strategy 2: Word-based matching for answers (similar to questions)
            String[] targetWords = normalizedTarget.split("\\s+");
            String[] extractedWords = normalizedExtracted.split("\\s+");

            if (targetWords.length == 0) {
                System.out.println("      ❌ No target words to match");
                return false;
            }

            int matchedWords = 0;
            System.out.println("      📝 Answer word-by-word analysis:");

            for (String targetWord : targetWords) {
                if (targetWord.length() <= 1) continue; // Skip very short words

                boolean wordFound = false;
                for (String extractedWord : extractedWords) {
                    if (extractedWord.contains(targetWord) || targetWord.contains(extractedWord)) {
                        wordFound = true;
                        matchedWords++;
                        break;
                    }
                }
                System.out.println("        \"" + targetWord + "\" -> " + (wordFound ? "✅" : "❌"));
            }

            double matchRatio = (double) matchedWords / targetWords.length;
            System.out.println("      📊 Answer match ratio: " + String.format("%.2f", matchRatio) + " (threshold: 0.6)");

            boolean match = matchRatio >= 0.6; // More lenient threshold for answers
            System.out.println("      Result: " + (match ? "✅ MATCHED" : "❌ NO MATCH"));
            return match;
        }

        /**
         * Detect column boundary within a specific region
         */
        private float detectColumnBoundaryInRegion(List<TextElement> elements) {
            if (elements.size() < 4) {
                System.out.println("  Not enough elements for column detection, using default boundary");
                return 300; // Default boundary for small regions
            }

            // Collect X coordinates and remove duplicates
            Set<Float> uniqueXCoords = new TreeSet<>();
            for (TextElement element : elements) {
                uniqueXCoords.add(element.x);
            }

            List<Float> xCoords = new ArrayList<>(uniqueXCoords);
            System.out.println("  Found " + xCoords.size() + " unique X coordinates: " +
                    xCoords.subList(0, Math.min(10, xCoords.size())));

            // Look for the largest gap in X coordinates  
            float maxGap = 0;
            float boundary = -1;

            for (int i = 0; i < xCoords.size() - 1; i++) {
                float gap = xCoords.get(i + 1) - xCoords.get(i);
                if (gap > maxGap && gap > 20) { // Reduced threshold for better detection
                    maxGap = gap;
                    boundary = xCoords.get(i) + gap / 2;
                }
            }

            if (boundary > 0) {
                System.out.println("  Detected column boundary at X=" + String.format("%.1f", boundary) +
                        " (gap: " + String.format("%.1f", maxGap) + ")");
                return boundary;
            }

            // Improved fallback: use page center if no clear gap found
            float minX = xCoords.get(0);
            float maxX = xCoords.get(xCoords.size() - 1);
            float centerBoundary = minX + (maxX - minX) * 0.5f; // Use 50% of page width

            System.out.println("  No clear column gap found. Using center boundary: " + String.format("%.1f", centerBoundary));
            System.out.println("  X range: " + String.format("%.1f", minX) + " to " + String.format("%.1f", maxX));
            return centerBoundary;
        }

        /**
         * Build text from column elements, preserving word spacing
         */
        private String buildColumnText(List<TextElement> elements) {
            if (elements.isEmpty()) return "";

            // Sort by Y (descending) then by X (ascending) to read naturally
            elements.sort((a, b) -> {
                int yCompare = Float.compare(b.y, a.y); // Descending Y
                if (yCompare != 0) return yCompare;
                return Float.compare(a.x, b.x); // Ascending X
            });

            StringBuilder text = new StringBuilder();
            float lastY = Float.MAX_VALUE;
            float lastX = -1;

            for (TextElement element : elements) {
                // Add line break if Y coordinate changed significantly
                if (Math.abs(element.y - lastY) > 5) {
                    if (text.length() > 0) {
                        text.append(" ");
                    }
                    lastX = -1; // Reset X tracking for new line
                }
                // Add space if there's a significant X gap (word separation)
                else if (lastX > 0 && element.x - lastX > 5) {
                    text.append(" ");
                }

                text.append(element.character);
                lastY = element.y;
                lastX = element.x + element.width;
            }

            return text.toString();
        }

        /**
         * Highlight a list of text elements
         */
        private void highlightElements(List<TextElement> elements) throws IOException {
            List<TextPosition> positions = new ArrayList<>();
            for (TextElement element : elements) {
                if (!Character.isWhitespace(element.character)) {
                    positions.add(element.position);
                }
            }
            if (!positions.isEmpty()) {
                highlightTextPositions(document, pageNum, positions);
                System.out.println("    Highlighted " + positions.size() + " character positions");
            }
        }

        /**
         * Check if text contains the target question with flexible matching
         */
        private boolean containsQuestion(String text, String question) {
            if (text == null || question == null) return false;

            String normalizedText = normalizeText(text);
            String normalizedQuestion = normalizeText(question);

            // Direct match
            if (normalizedText.contains(normalizedQuestion)) {
                return true;
            }

            // Word-based partial matching (at least 80% of words match)
            String[] questionWords = normalizedQuestion.split("\\s+");
            String[] textWords = normalizedText.split("\\s+");

            if (questionWords.length == 0) return false;

            int matchCount = 0;
            for (String qWord : questionWords) {
                if (qWord.length() > 2) { // Skip very short words
                    for (String tWord : textWords) {
                        if (tWord.contains(qWord) || qWord.contains(tWord)) {
                            matchCount++;
                            break;
                        }
                    }
                }
            }

            double matchRatio = (double) matchCount / questionWords.length;
            return matchRatio >= 0.8;
        }

        /**
         * Check if text contains the target answer
         */
        private boolean containsAnswer(String text, String answer) {
            if (text == null || answer == null) return false;

            String normalizedText = normalizeText(text);
            String normalizedAnswer = normalizeText(answer);

            return normalizedText.contains(normalizedAnswer) ||
                    normalizedAnswer.contains(normalizedText);
        }

        /**
         * Normalize text for matching
         */
        private String normalizeText(String text) {
            return text.toLowerCase()
                    .trim()
                    .replaceAll("\\s+", " ")
                    .replaceAll("[^a-zA-Z0-9\\s]", "");
        }
    }
}