package com.ginkgooai.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationTextMarkup;
import org.apache.pdfbox.text.TextPosition;
import org.apache.pdfbox.text.PDFTextStripper;


import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.HashMap;
import java.util.Set;

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
            System.out.println("Total pages: " + document.getNumberOfPages());
            
            for (int pageNum = 0; pageNum < Math.min(2, document.getNumberOfPages()); pageNum++) {
                System.out.println("\n--- PAGE " + (pageNum + 1) + " ANALYSIS ---");
                
                PDFStructureAnalyzer analyzer = new PDFStructureAnalyzer();
                analyzer.analyzePage(document, pageNum);
            }
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
                System.out.println("No text found on this page");
                return;
            }
            
            // Group text by lines (similar Y coordinates)
            Map<Integer, List<TextElement>> lineGroups = new TreeMap<>();
            for (TextElement element : textElements) {
                int lineY = (int) Math.round(element.y / 5) * 5; // Group by 5-point intervals
                lineGroups.computeIfAbsent(lineY, k -> new ArrayList<>()).add(element);
            }
            
            System.out.println("Found " + lineGroups.size() + " text lines");
            
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
                    System.out.printf("Line %2d (Y=%3.0f, X=%3.0f-%3.0f): %s%n", 
                                    lineCount, (float)entry.getKey(), minX, maxX, text);
                                    
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
                    
                    System.out.printf("    → Question: '%s' (ends at X=%.0f)%n", beforeColon, questionEndX);
                    System.out.printf("    → Answer: '%s' (starts at X=%.0f)%n", afterColon, answerStartX);
                    System.out.printf("    → Gap: %.0f points%n", answerStartX - questionEndX);
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
            
            System.out.println("\nX position frequency (showing top positions):");
            xPositions.entrySet().stream()
                    .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                    .limit(10)
                    .forEach(entry -> System.out.printf("X=%d: %d chars%n", entry.getKey(), entry.getValue()));
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
     * 在PDF中高亮JSON数据中指定的答案
     * @param inputFile 输入的PDF文件
     * @param outputFile 输出的PDF文件
     * @param jsonData 包含问答对的JSON字符串
     * @throws IOException
     */
    public static void highlightAnswers(File inputFile, File outputFile, String jsonData) throws IOException {
        try (PDDocument document = PDDocument.load(inputFile)) {
            JSONArray qaArray = JSON.parseArray(jsonData);

            for (int i = 0; i < qaArray.size(); i++) {
                JSONObject qa = qaArray.getJSONObject(i);
                String question = qa.getString("question");
                String answer = qa.getString("answer");

                // 在PDF中查找并高亮答案
                findAndHighlight(document, question, answer);
            }

            document.save(outputFile);
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
        System.out.println("=== Searching for Question: '" + question + "' with Answer: '" + answerToHighlight + "' ===");
        
        for (int pageNum = 0; pageNum < document.getNumberOfPages(); pageNum++) {
            System.out.println("Processing page " + (pageNum + 1));
            
            // Use coordinate-based matching for left-right layout
            CoordinateBasedMatcher matcher = new CoordinateBasedMatcher();
            matcher.processPage(document, pageNum, question.trim(), answerToHighlight.trim());
        }
    }

    /**
     * Coordinate-based matcher for left-right layout PDFs
     */
    private static class CoordinateBasedMatcher extends PDFTextStripper {
        private PDDocument document;
        private int pageNum;
        private String targetQuestion;
        private String targetAnswer;
        private List<TextElement> allTextElements = new ArrayList<>();
        
        public CoordinateBasedMatcher() throws IOException {
            super();
        }
        
        public void processPage(PDDocument doc, int pageNumber, String question, String answer) throws IOException {
            this.document = doc;
            this.pageNum = pageNumber;
            this.targetQuestion = question;
            this.targetAnswer = answer;
            this.allTextElements.clear();
            
            // Set page range
            this.setStartPage(pageNumber + 1);
            this.setEndPage(pageNumber + 1);
            
            // Extract text with positions
            this.getText(doc);
            
            // Find and highlight using coordinate-based matching
            findAndHighlightByCoordinates();
        }
        
        @Override
        protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
            super.writeString(text, textPositions);
            
            // Store all text elements with their positions
            for (int i = 0; i < text.length() && i < textPositions.size(); i++) {
                char ch = text.charAt(i);
                TextPosition pos = textPositions.get(i);
                
                // Debug first few characters to understand coordinate system
                if (allTextElements.size() < 5) {
                    System.out.println("    → Text '" + ch + "' at: X=" + pos.getXDirAdj() + 
                                     ", Y=" + pos.getYDirAdj() + 
                                     ", W=" + pos.getWidthDirAdj() + 
                                     ", H=" + pos.getHeightDir());
                }
                
                allTextElements.add(new TextElement(ch, pos.getXDirAdj(), pos.getYDirAdj(), 
                                                 pos.getWidthDirAdj(), pos.getHeightDir(), pos));
            }
        }
        
        private void findAndHighlightByCoordinates() throws IOException {
            if (allTextElements.isEmpty()) {
                System.out.println("No text found on page " + (pageNum + 1));
                return;
            }
            
            // Group text elements by lines (similar Y coordinates)
            Map<Integer, List<TextElement>> lineGroups = groupByLines();
            
            System.out.println("Found " + lineGroups.size() + " text lines on page " + (pageNum + 1));
            
            // Search each line for question matches
            for (Map.Entry<Integer, List<TextElement>> entry : lineGroups.entrySet()) {
                List<TextElement> lineElements = entry.getValue();
                // Sort by X coordinate
                lineElements.sort((a, b) -> Float.compare(a.x, b.x));
                
                // Build line text
                String lineText = buildLineText(lineElements);
                
                if (lineText.trim().length() > 0) {
                    System.out.println("Line (Y=" + entry.getKey() + "): " + lineText);
                    
                    // Check if this line contains our question
                    if (containsQuestion(lineText, targetQuestion)) {
                        System.out.println("  → Found question in this line! Highlighting entire line...");
                        
                        // Highlight the entire line
                        List<TextPosition> linePositions = highlightEntireLine(lineElements, lineText);
                        
                        if (!linePositions.isEmpty()) {
                            highlightTextPositions(document, pageNum, linePositions);
                            System.out.println("  → Successfully highlighted entire line with " + linePositions.size() + " positions");
                            return; // Found and highlighted, stop searching
                        }
                    }
                }
            }
            
            System.out.println("No matching question found on page " + (pageNum + 1));
        }
        
        private Map<Integer, List<TextElement>> groupByLines() {
            Map<Integer, List<TextElement>> lineGroups = new TreeMap<>();
            
            // Sort all elements by Y coordinate first (descending, since PDF Y goes from bottom to top)
            allTextElements.sort((a, b) -> Float.compare(b.y, a.y));
            
            for (TextElement element : allTextElements) {
                // Use precise Y coordinate for grouping
                int lineY = Math.round(element.y);
                
                // Find existing line within very tight tolerance
                Integer matchingLineY = null;
                for (Integer existingY : lineGroups.keySet()) {
                    if (Math.abs(existingY - lineY) <= 1) { // Only 1-point tolerance
                        matchingLineY = existingY;
                        break;
                    }
                }
                
                if (matchingLineY != null) {
                    lineGroups.get(matchingLineY).add(element);
                } else {
                    List<TextElement> newLine = new ArrayList<>();
                    newLine.add(element);
                    lineGroups.put(lineY, newLine);
                }
            }
            
            // Debug output with improved validation
            System.out.println("Line grouping results:");
            int lineCount = 0;
            for (Map.Entry<Integer, List<TextElement>> entry : lineGroups.entrySet()) {
                lineCount++;
                List<TextElement> elements = entry.getValue();
                elements.sort((a, b) -> Float.compare(a.x, b.x)); // Sort by X within each line
                String lineText = buildLineText(elements);
                
                // Check Y coordinate variance within this "line"
                float minY = elements.stream().map(e -> e.y).min(Float::compare).orElse(0f);
                float maxY = elements.stream().map(e -> e.y).max(Float::compare).orElse(0f);
                float yVariance = maxY - minY;
                
                System.out.println("  Line " + lineCount + " Y=" + entry.getKey() + 
                                 " (variance: " + String.format("%.1f", yVariance) + 
                                 ", " + elements.size() + " chars): " + lineText.trim());
                
                // Warning if Y variance is too high (indicates wrong grouping)
                if (yVariance > 2) {
                    System.out.println("    ⚠️  Warning: High Y variance in this line - possibly incorrect grouping!");
                }
            }
            
            return lineGroups;
        }
        
        private String buildLineText(List<TextElement> elements) {
            StringBuilder sb = new StringBuilder();
            for (TextElement element : elements) {
                sb.append(element.character);
            }
            return sb.toString();
        }
        
        private boolean containsQuestion(String lineText, String question) {
            if (lineText == null || question == null) {
                return false;
            }
            
            String lowerLineText = lineText.toLowerCase().trim();
            String lowerQuestion = question.toLowerCase().trim();
            
            // Try exact match first
            if (lowerLineText.contains(lowerQuestion)) {
                return true;
            }
            
            // Try normalized match (remove extra spaces)
            String normalizedLine = lowerLineText.replaceAll("\\s+", " ");
            String normalizedQuestion = lowerQuestion.replaceAll("\\s+", " ");
            
            return normalizedLine.contains(normalizedQuestion);
        }
        
        /**
         * Highlight only the text that contains the question, not the entire line
         */
        private List<TextPosition> highlightEntireLine(List<TextElement> lineElements, String lineText) {
            List<TextPosition> highlightPositions = new ArrayList<>();
            
            System.out.println("    → Analyzing line for highlighting: '" + lineText.trim() + "'");
            
            // First, let's see exactly what Y coordinates we have
            Set<Float> uniqueYs = lineElements.stream().map(e -> e.y).collect(java.util.stream.Collectors.toSet());
            System.out.println("    → Unique Y coordinates in this 'line': " + uniqueYs);
            
            if (uniqueYs.size() > 1) {
                System.out.println("    → ⚠️  Multiple Y coordinates detected! This is definitely multiple lines.");
                
                // Find which Y coordinate contains our question
                Map<Float, List<TextElement>> byY = lineElements.stream()
                    .collect(java.util.stream.Collectors.groupingBy(e -> e.y));
                
                for (Map.Entry<Float, List<TextElement>> entry : byY.entrySet()) {
                    List<TextElement> elementsAtY = entry.getValue();
                    elementsAtY.sort((a, b) -> Float.compare(a.x, b.x));
                    String textAtY = buildLineText(elementsAtY);
                    
                    System.out.println("    → Y=" + entry.getKey() + ": '" + textAtY.trim() + "'");
                    
                    // Check if this specific Y line contains our question
                    if (containsQuestion(textAtY, targetQuestion)) {
                        System.out.println("    → ✓ Found question at Y=" + entry.getKey() + ", highlighting only this line");
                        
                        // Add only elements from this Y coordinate
                        for (TextElement element : elementsAtY) {
                            if (!Character.isWhitespace(element.character)) {
                                highlightPositions.add(element.position);
                            }
                        }
                        break; // Found the right line, stop looking
                    }
                }
            } else {
                System.out.println("    → Single Y coordinate detected, highlighting entire line");
                
                // Single Y coordinate, safe to highlight all
                for (TextElement element : lineElements) {
                    if (!Character.isWhitespace(element.character)) {
                        highlightPositions.add(element.position);
                    }
                }
            }
            
            System.out.println("    → Selected " + highlightPositions.size() + " characters for highlighting");
            
            // Show Y range of selected positions
            if (!highlightPositions.isEmpty()) {
                float minY = highlightPositions.stream().map(pos -> pos.getYDirAdj()).min(Float::compare).orElse(0f);
                float maxY = highlightPositions.stream().map(pos -> pos.getYDirAdj()).max(Float::compare).orElse(0f);
                System.out.println("    → Highlight Y range: " + String.format("%.2f", minY) + " to " + String.format("%.2f", maxY));
            }
            
            return highlightPositions;
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
}
