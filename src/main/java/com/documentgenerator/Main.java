package com.documentgenerator;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("=== DocuMerge Pro ===");
        
        // Get file paths from user
        System.out.print("Enter Excel file path: ");
        String excelPath = scanner.nextLine();
        
        System.out.print("Enter Word template path: ");
        String templatePath = scanner.nextLine();
        
        System.out.print("Enter output directory: ");
        String outputDir = scanner.nextLine();
        
        // Process documents
        try {
            DocumentProcessor.processDocuments(excelPath, templatePath, outputDir);
        } catch (Exception e) {
            System.err.println("Error processing documents: " + e.getMessage());
            e.printStackTrace();
        }
        
        scanner.close();
    }
}

