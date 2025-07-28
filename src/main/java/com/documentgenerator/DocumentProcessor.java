package com.documentgenerator;

import java.io.File;
import java.util.List;
import java.util.Map;

public class DocumentProcessor {
    
    public static void processDocuments(String excelPath, String templatePath, 
                                      String outputDir) throws Exception{
        // Read data from Excel
        List<Map<String, String>> dataList = ExcelReader.readExcelData(excelPath);
        
        // Create output directory if it doesn't exist
        File outputDirectory = new File(outputDir);
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }
        
        // Generate a document for each row of data
        for (int i = 0; i < dataList.size(); i++) {
            Map<String, String> rowData = dataList.get(i);
            String outputFileName = outputDir + "/document_" + (i + 1) + "_" + 
                                  rowData.get("Name").replaceAll("\\s+", "_") + ".docx";
            
            WordGenerator.generateDocument(templatePath, outputFileName, rowData);
        }
        
        System.out.println("Processing complete! Generated " + dataList.size() + " documents.");
    }
}
