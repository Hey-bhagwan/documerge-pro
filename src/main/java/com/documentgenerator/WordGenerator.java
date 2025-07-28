package com.documentgenerator;

import org.apache.poi.xwpf.usermodel.*;
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WordGenerator {
    
    // Original method for backward compatibility
    public static void generateDocument(String templatePath, String outputPath, 
                                      Map<String, String> data) throws Exception {
        generateDocument(templatePath, outputPath, data, null);
    }
    
    // Enhanced method with user-specified placeholder support
    public static void generateDocument(String templatePath, String outputPath, 
                                      Map<String, String> data, 
                                      List<String> userSpecifiedPlaceholders) throws Exception {
        try (FileInputStream fis = new FileInputStream(templatePath);
             XWPFDocument document = new XWPFDocument(fis)) {
            
            // Get all placeholders from document
            Set<String> detectedPlaceholders = detectPlaceholders(document);
            System.out.println("Detected placeholders in template: " + detectedPlaceholders);
            
            // Determine which placeholders to use
            Set<String> activePlaceholders = determineActivePlaceholders(
                detectedPlaceholders, userSpecifiedPlaceholders, data);
            System.out.println("Processing placeholders: " + activePlaceholders);
            
            // Replace placeholders with data
            replacePlaceholders(document, data, activePlaceholders);
            
            // Save the modified document
            try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                document.write(fos);
                System.out.println("Document generated: " + outputPath);
            }
            
        } catch (IOException e) {
            System.err.println("Error generating Word document: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Automatically detect all placeholders in the document
     */
    private static Set<String> detectPlaceholders(XWPFDocument document) {
        Set<String> placeholders = new HashSet<>();
        Pattern pattern = Pattern.compile("<<([^<>]+)>>");
        
        // Check paragraphs
        for (XWPFParagraph paragraph : document.getParagraphs()) {
            String text = paragraph.getText();
            if (text != null) {
                Matcher matcher = pattern.matcher(text);
                while (matcher.find()) {
                    placeholders.add(matcher.group(1).trim());
                }
            }
        }
        
        // Check tables
        for (XWPFTable table : document.getTables()) {
            for (XWPFTableRow row : table.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    String text = cell.getText();
                    if (text != null) {
                        Matcher matcher = pattern.matcher(text);
                        while (matcher.find()) {
                            placeholders.add(matcher.group(1).trim());
                        }
                    }
                }
            }
        }
        
        // Check headers
        for (XWPFHeader header : document.getHeaderList()) {
            for (XWPFParagraph paragraph : header.getParagraphs()) {
                String text = paragraph.getText();
                if (text != null) {
                    Matcher matcher = pattern.matcher(text);
                    while (matcher.find()) {
                        placeholders.add(matcher.group(1).trim());
                    }
                }
            }
        }
        
        // Check footers
        for (XWPFFooter footer : document.getFooterList()) {
            for (XWPFParagraph paragraph : footer.getParagraphs()) {
                String text = paragraph.getText();
                if (text != null) {
                    Matcher matcher = pattern.matcher(text);
                    while (matcher.find()) {
                        placeholders.add(matcher.group(1).trim());
                    }
                }
            }
        }
        
        return placeholders;
    }
    
    /**
     * Determine which placeholders should be actively processed
     */
    private static Set<String> determineActivePlaceholders(Set<String> detected, 
                                                          List<String> userSpecified, 
                                                          Map<String, String> data) {
        Set<String> active = new HashSet<>();
        
        if (userSpecified != null && !userSpecified.isEmpty()) {
            // User specified placeholders - validate they exist in template
            for (String userPH : userSpecified) {
                if (detected.contains(userPH)) {
                    active.add(userPH);
                    System.out.println("✓ User-specified placeholder found: " + userPH);
                } else {
                    System.out.println("⚠ User-specified placeholder NOT found in template: " + userPH);
                }
            }
            
            // Also include any auto-detected placeholders that have data available
            for (String detectedPH : detected) {
                if (data.containsKey(detectedPH) && !active.contains(detectedPH)) {
                    active.add(detectedPH);
                    System.out.println("✓ Auto-detected placeholder with data: " + detectedPH);
                }
            }
        } else {
            // No user specification - use all detected placeholders that have data
            for (String detectedPH : detected) {
                if (data.containsKey(detectedPH)) {
                    active.add(detectedPH);
                    System.out.println("✓ Auto-detected placeholder: " + detectedPH);
                } else {
                    System.out.println("⚠ Placeholder found but no data available: " + detectedPH);
                }
            }
        }
        
        return active;
    }
    
    /**
     * Replace placeholders in the entire document
     */
    private static void replacePlaceholders(XWPFDocument document, Map<String, String> data, 
                                          Set<String> activePlaceholders) {
        // Replace in paragraphs
        for (XWPFParagraph paragraph : document.getParagraphs()) {
            replacePlaceholdersInParagraph(paragraph, data, activePlaceholders);
        }
        
        // Replace in tables
        for (XWPFTable table : document.getTables()) {
            for (XWPFTableRow row : table.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    for (XWPFParagraph paragraph : cell.getParagraphs()) {
                        replacePlaceholdersInParagraph(paragraph, data, activePlaceholders);
                    }
                }
            }
        }
        
        // Replace in headers
        for (XWPFHeader header : document.getHeaderList()) {
            for (XWPFParagraph paragraph : header.getParagraphs()) {
                replacePlaceholdersInParagraph(paragraph, data, activePlaceholders);
            }
        }
        
        // Replace in footers
        for (XWPFFooter footer : document.getFooterList()) {
            for (XWPFParagraph paragraph : footer.getParagraphs()) {
                replacePlaceholdersInParagraph(paragraph, data, activePlaceholders);
            }
        }
    }
    
    /**
     * Enhanced placeholder replacement in paragraphs
     */
    private static void replacePlaceholdersInParagraph(XWPFParagraph paragraph, 
                                                     Map<String, String> data,
                                                     Set<String> activePlaceholders) {
        // Get the complete paragraph text
        String fullText = paragraph.getText();
        if (fullText == null || fullText.isEmpty()) {
            return;
        }
        
        String modifiedText = fullText;
        boolean textChanged = false;
        
        // Replace only active placeholders
        for (String placeholder : activePlaceholders) {
            String placeholderPattern = "<<" + placeholder + ">>";
            String replacement = data.getOrDefault(placeholder, "");
            
            if (modifiedText.contains(placeholderPattern)) {
                modifiedText = modifiedText.replace(placeholderPattern, replacement);
                textChanged = true;
            }
        }
        
        // If text was changed, update the paragraph
        if (textChanged) {
            // Clear existing runs
            for (int i = paragraph.getRuns().size() - 1; i >= 0; i--) {
                paragraph.removeRun(i);
            }
            
            // Create a new run with the modified text
            XWPFRun newRun = paragraph.createRun();
            newRun.setText(modifiedText);
        }
    }
    
    /**
     * Utility method to get available placeholders from a template
     */
    public static Set<String> getAvailablePlaceholders(String templatePath) throws Exception {
        try (FileInputStream fis = new FileInputStream(templatePath);
             XWPFDocument document = new XWPFDocument(fis)) {
            return detectPlaceholders(document);
        }
    }
    
    /**
     * Utility method to validate if specified placeholders exist in template
     */
    public static Map<String, Boolean> validatePlaceholders(String templatePath, 
                                                           List<String> placeholders) throws Exception {
        Set<String> available = getAvailablePlaceholders(templatePath);
        Map<String, Boolean> validation = new HashMap<>();
        
        for (String placeholder : placeholders) {
            validation.put(placeholder, available.contains(placeholder));
        }
        
        return validation;
    }
}
