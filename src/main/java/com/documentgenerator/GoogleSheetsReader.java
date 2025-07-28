package com.documentgenerator;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.http.HttpCredentialsAdapter;

import java.io.*;
import java.security.GeneralSecurityException;
import java.util.*;

public class GoogleSheetsReader {
    private static final String APPLICATION_NAME = "DocuMerge Pro";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS_READONLY);
    private static final String SERVICE_ACCOUNT_KEY_PATH = "/service-account-key.json";

    private static Sheets getSheetsService() throws IOException, GeneralSecurityException {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        
        // Try to load service account credentials from multiple locations
        InputStream in = null;
        
        // First try classpath (for development/testing)
        in = GoogleSheetsReader.class.getResourceAsStream(SERVICE_ACCOUNT_KEY_PATH);
        
        // If not found in classpath, try external file in project root
        if (in == null) {
            File externalKey = new File("service-account-key.json");
            if (externalKey.exists()) {
                try {
                    in = new FileInputStream(externalKey);
                    System.out.println("✓ Using service account key from project root");
                } catch (FileNotFoundException e) {
                    // Continue to next option
                }
            }
        }
        
        // Try environment variable path
        if (in == null) {
            String envKeyPath = System.getenv("GOOGLE_SERVICE_ACCOUNT_PATH");
            if (envKeyPath != null) {
                File envKey = new File(envKeyPath);
                if (envKey.exists()) {
                    try {
                        in = new FileInputStream(envKey);
                        System.out.println("✓ Using service account key from environment variable");
                    } catch (FileNotFoundException e) {
                        // Continue to next option
                    }
                }
            }
        }
        
        // Try GitHub Actions secret (for CI/CD)
        if (in == null) {
            String secretContent = System.getenv("GOOGLE_SERVICE_ACCOUNT_KEY");
            if (secretContent != null && !secretContent.isEmpty()) {
                try {
                    in = new ByteArrayInputStream(secretContent.getBytes(StandardCharsets.UTF_8));
                    System.out.println("✓ Using service account key from GitHub Actions secret");
                } catch (Exception e) {
                    // Continue to next option
                }
            }
        }
        
        // If still not found, provide helpful error message
        if (in == null) {
            System.out.println("⚠ Google Sheets service account key not found. Google Sheets functionality disabled.");
            System.out.println("To enable Google Sheets integration:");
            System.out.println("  1. Place 'service-account-key.json' in project root, or");
            System.out.println("  2. Set GOOGLE_SERVICE_ACCOUNT_PATH environment variable, or");
            System.out.println("  3. Set GOOGLE_SERVICE_ACCOUNT_KEY environment variable with JSON content");
            throw new FileNotFoundException("Service account key not found. Check console for setup instructions.");
        }
        
        try {
            GoogleCredentials credentials = GoogleCredentials.fromStream(in)
                    .createScoped(SCOPES);
            
            return new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, 
                    new HttpCredentialsAdapter(credentials))
                    .setApplicationName(APPLICATION_NAME)
                    .build();
                    
        } catch (IOException e) {
            System.err.println("✗ Error loading Google Sheets credentials: " + e.getMessage());
            throw new IOException("Failed to load Google Sheets credentials", e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // Ignore close errors
                }
            }
        }
    }
    

    public static String extractSpreadsheetId(String url) {
        if (url.contains("/spreadsheets/d/")) {
            String[] parts = url.split("/spreadsheets/d/")[1].split("/");
            return parts[0];
        }
        throw new IllegalArgumentException("Invalid Google Sheets URL format");
    }

    public static List<Map<String, String>> readGoogleSheetsData(String spreadsheetUrl, String sheetName) throws Exception {
        Sheets service = getSheetsService();
        String spreadsheetId = extractSpreadsheetId(spreadsheetUrl);
        String range = sheetName != null ? sheetName : "Sheet1";

        List<Map<String, String>> dataList = new ArrayList<>();

        try {
            ValueRange response = service.spreadsheets().values()
                    .get(spreadsheetId, range)
                    .execute();
            List<List<Object>> values = response.getValues();

            if (values == null || values.isEmpty()) {
                System.out.println("No data found in the sheet.");
                return dataList;
            }

            List<Object> headerRow = values.get(0);
            List<String> headers = new ArrayList<>();
            for (Object header : headerRow) {
                headers.add(header.toString().trim());
            }

            for (int i = 1; i < values.size(); i++) {
                List<Object> row = values.get(i);
                Map<String, String> rowData = new HashMap<>();

                for (int j = 0; j < headers.size(); j++) {
                    String value = "";
                    if (j < row.size() && row.get(j) != null) {
                        value = row.get(j).toString();
                    }
                    rowData.put(headers.get(j), value);
                }
                dataList.add(rowData);
            }

        } catch (Exception e) {
            System.err.println("Error reading Google Sheets data: " + e.getMessage());
            throw e;
        }

        return dataList;
    }
}
