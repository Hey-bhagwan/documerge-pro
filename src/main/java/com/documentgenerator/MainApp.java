package com.documentgenerator;

import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.image.Image;

import com.documentgenerator.WordGenerator;
import com.documentgenerator.DocumentProcessor;
import com.documentgenerator.ExcelReader;
import com.documentgenerator.GoogleSheetsReader;
import javafx.application.Platform;

import java.io.File;
import java.util.List;
import java.util.Map;

public class MainApp extends Application {

    private TextField excelFileField;
    private TextField googleSheetsUrlField;
    private TextField templateFileField;
    private TextField outputDirField;
    private Button processButton;
    private TextArea logArea;
    private ProgressBar progressBar;
    private RadioButton localFileRadio;
    private RadioButton googleSheetsRadio;
    private ToggleGroup dataSourceGroup;

    @Override
public void start(Stage primaryStage) {
    // Initialize trial on first run
    TrialManager.initializeTrial();
    
    // Check trial validity before showing main window
    if (!TrialManager.isTrialValid()) {
        showTechnicalErrorAndExit();
        return;
    }
    
    primaryStage.setTitle("DocuMerge Pro");
    
    // Add application icon with multiple sizes
try {
    primaryStage.getIcons().addAll(
        new Image(getClass().getResourceAsStream("/icon-16.png")),
        new Image(getClass().getResourceAsStream("/icon-32.png")),
        new Image(getClass().getResourceAsStream("/icon-48.png")),
        new Image(getClass().getResourceAsStream("/icon-64.png"))
    );
    System.out.println("✓ Multiple icon sizes loaded successfully");
    System.out.println("Total icons set: " + primaryStage.getIcons().size());
} catch (Exception e) {
    System.out.println("⚠ Some icon files not found, trying fallback...");
    
    // Fallback to original single icon
    try {
        Image icon = new Image(getClass().getResourceAsStream("/icon.png"));
        if (!icon.isError()) {
            primaryStage.getIcons().add(icon);
            System.out.println("✓ Fallback icon loaded");
        }
    } catch (Exception fallbackException) {
        System.out.println("✗ No icons could be loaded");
    }
}

    
    
    // Your existing code continues...
    VBox root = createMainLayout();
    Scene scene = new Scene(root, 900, 700);
    primaryStage.setScene(scene);
    primaryStage.show();
}

    
    private void showTechnicalErrorAndExit() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Application Error");
        alert.setHeaderText("System Error");
        alert.setContentText(
            "DocuMerge Pro has encountered a critical system error and cannot continue.\n\n" +
            "Error Code: SYS_CORE_0x8847\n" +
            "Please contact technical support for assistance.\n\n" +
            "The application will now close."
        );
        
        alert.showAndWait();
        Platform.exit();
    }
    
    private void showProcessingError() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Processing Error");
        alert.setHeaderText("System Processing Error");
        alert.setContentText(
            "A critical error occurred while initializing the document processing engine.\n\n" +
            "Error Code: PROC_INIT_0x4429\n" +
            "This may be due to a corrupted system configuration.\n\n" +
            "Please restart the application and try again."
        );
        alert.showAndWait();
    }
    
    private void showTechnicalProcessingError() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Critical Error");
        alert.setHeaderText("System Core Failure");
        alert.setContentText(
            "The application has encountered a fatal system error.\n\n" +
            "Error Code: CORE_PROC_0x7748\n" +
            "The application will now terminate to prevent data corruption."
        );
        
        alert.setOnHidden(e -> Platform.exit());
        alert.showAndWait();
    }
    

    private VBox createMainLayout() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(20));

        // Title
        Label titleLabel = new Label("DocuMerge Pro");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        // Data source selection
        VBox dataSourceSection = createDataSourceSection();

        // File selection section
        VBox fileSection = createFileSelectionSection();

        // Process button
        processButton = new Button("Generate Documents");
        processButton.setStyle("-fx-font-size: 14px; -fx-padding: 10px 20px;");
        processButton.setOnAction(e -> processDocuments());

        // Progress bar
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(Double.MAX_VALUE);
        progressBar.setVisible(false);

        // Log area
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefRowCount(10);
        logArea.setWrapText(true);

        root.getChildren().addAll(
                titleLabel,
                dataSourceSection,
                fileSection,
                processButton,
                progressBar,
                new Label("Processing Log:"),
                logArea);

        return root;
    }

    private VBox createDataSourceSection() {
        VBox section = new VBox(10);

        Label sectionTitle = new Label("Data Source:");
        sectionTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        // Radio buttons for data source selection
        dataSourceGroup = new ToggleGroup();

        localFileRadio = new RadioButton("Local Excel File");
        localFileRadio.setToggleGroup(dataSourceGroup);
        localFileRadio.setSelected(true);
        localFileRadio.setOnAction(e -> updateDataSourceVisibility());

        googleSheetsRadio = new RadioButton("Google Sheets URL");
        googleSheetsRadio.setToggleGroup(dataSourceGroup);
        googleSheetsRadio.setOnAction(e -> updateDataSourceVisibility());

        section.getChildren().addAll(sectionTitle, localFileRadio, googleSheetsRadio);
        return section;
    }

    private VBox createFileSelectionSection() {
        VBox section = new VBox(15);

        // Excel file selection (initially visible)
        HBox excelBox = createFileSelectionRow(
                "Excel Data File:",
                excelFileField = new TextField(),
                "Select Excel File",
                this::selectExcelFile);
        excelBox.setId("excelFileBox");

        // Google Sheets URL input (initially hidden)
        HBox googleSheetsBox = createUrlInputRow(
                "Google Sheets URL:",
                googleSheetsUrlField = new TextField());
        googleSheetsBox.setId("googleSheetsBox");
        googleSheetsBox.setVisible(false);
        googleSheetsBox.setManaged(false);

        // Template file selection
        HBox templateBox = createFileSelectionRow(
                "Word Template:",
                templateFileField = new TextField(),
                "Select Template",
                this::selectTemplateFile);

        // Output directory selection
        HBox outputBox = createFileSelectionRow(
                "Output Directory:",
                outputDirField = new TextField(),
                "Select Directory",
                this::selectOutputDirectory);

        section.getChildren().addAll(excelBox, googleSheetsBox, templateBox, outputBox);
        return section;
    }

    private HBox createUrlInputRow(String labelText, TextField textField) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        Label label = new Label(labelText);
        label.setPrefWidth(150);

        textField.setPrefWidth(400);
        textField.setPromptText("https://docs.google.com/spreadsheets/d/SPREADSHEET_ID/edit");

        // Add validation button for Google Sheets URL
        Button validateButton = new Button("Validate");
        validateButton.setOnAction(e -> validateGoogleSheetsUrl());

        row.getChildren().addAll(label, textField, validateButton);
        return row;
    }

    private void updateDataSourceVisibility() {
        boolean isLocalFile = localFileRadio.isSelected();

        // Find and toggle visibility
        VBox parent = (VBox) excelFileField.getParent().getParent();
        for (javafx.scene.Node node : parent.getChildren()) {
            if ("excelFileBox".equals(node.getId())) {
                node.setVisible(isLocalFile);
                node.setManaged(isLocalFile);
            } else if ("googleSheetsBox".equals(node.getId())) {
                node.setVisible(!isLocalFile);
                node.setManaged(!isLocalFile);
            }
        }
    }

    private void validateGoogleSheetsUrl() {
        String url = googleSheetsUrlField.getText().trim();
        if (url.isEmpty()) {
            showAlert("Error", "Please enter a Google Sheets URL");
            return;
        }

        try {
            String spreadsheetId = GoogleSheetsReader.extractSpreadsheetId(url);
            showAlert("Success", "Valid Google Sheets URL detected!\nSpreadsheet ID: " + spreadsheetId);
        } catch (Exception e) {
            showAlert("Error", "Invalid Google Sheets URL format. Please use a valid Google Sheets URL.");
        }
    }

    private HBox createFileSelectionRow(String labelText, TextField textField,
            String buttonText, Runnable buttonAction) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        Label label = new Label(labelText);
        label.setPrefWidth(150);

        textField.setPrefWidth(400);

        Button button = new Button(buttonText);
        button.setOnAction(e -> buttonAction.run());

        row.getChildren().addAll(label, textField, button);
        return row;
    }

    private void selectExcelFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Excel File");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel Files", "*.xlsx", "*.xls"));

        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            // Store full path but display only filename
            excelFileField.setText(file.getName());
            excelFileField.setUserData(file.getAbsolutePath());
        }
    }

    private void selectTemplateFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Word Template");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Word Documents", "*.docx"));

        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            templateFileField.setText(file.getName());
            templateFileField.setUserData(file.getAbsolutePath());
        }
    }

    private void selectOutputDirectory() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Output Directory");

        File directory = directoryChooser.showDialog(null);
        if (directory != null) {
            outputDirField.setText(directory.getName());
            outputDirField.setUserData(directory.getAbsolutePath());
        }
    }

    private void processDocuments() {
    // Perform trial check before processing
    if (!TrialManager.isTrialValid()) {
        showProcessingError();
        return;
    }
    
    String templatePath = (String) templateFileField.getUserData();
    String outputDir = (String) outputDirField.getUserData();

    // Validate common inputs
    if (templateFileField.getText().isEmpty() || outputDirField.getText().isEmpty() ||
            templatePath == null || outputDir == null) {
        showAlert("Error", "Please select Word template and output directory.");
        return;
    }

    processButton.setDisable(true);
    progressBar.setVisible(true);
    progressBar.setProgress(-1);
    logArea.clear();
    logArea.appendText("Starting document generation...\n");

    Task<Void> task = new Task<Void>() {
        @Override
        protected Void call() throws Exception {
            // Additional trial check during processing
            if (!TrialManager.isTrialValid()) {
                throw new Exception("System processing error occurred");
            }
            
            try {
                List<Map<String, String>> data;

                if (localFileRadio.isSelected()) {
                    String excelPath = (String) excelFileField.getUserData();
                    if (excelPath == null || excelFileField.getText().isEmpty()) {
                        throw new Exception("Please select an Excel file.");
                    }
                    data = ExcelReader.readExcelData(excelPath);
                    javafx.application.Platform.runLater(() -> 
                        logArea.appendText("Reading data from local Excel file...\n"));
                } else {
                    String googleSheetsUrl = googleSheetsUrlField.getText().trim();
                    if (googleSheetsUrl.isEmpty()) {
                        throw new Exception("Please enter a Google Sheets URL.");
                    }
                    data = GoogleSheetsReader.readGoogleSheetsData(googleSheetsUrl, "Sheet1");
                    javafx.application.Platform.runLater(() -> 
                        logArea.appendText("Reading data from Google Sheets...\n"));
                }

                // Generate documents for each row
                for (int i = 0; i < data.size(); i++) {
                    Map<String, String> rowData = data.get(i);
                    String outputFileName = outputDir + "/document_" + (i + 1) + "_" +
                            rowData.get("Name").replaceAll("\\s+", "_") + ".docx";

                    WordGenerator.generateDocument(templatePath, outputFileName, rowData);

                    final int currentDoc = i + 1;
                    final int totalDocs = data.size();
                    javafx.application.Platform.runLater(() ->
                            logArea.appendText("Generated document " + currentDoc + "/" + totalDocs + "\n"));
                }

                return null;
            } catch (Exception e) {
                throw e;
            }
        }

        @Override
        protected void succeeded() {
            logArea.appendText("Document generation completed successfully!\n");
            showAlert("Success", "All documents have been generated successfully!");
            resetUI();
        }

        @Override
        protected void failed() {
            String errorMsg = getException().getMessage();
            if (errorMsg.contains("System processing error")) {
                // Show technical error instead of trial expiry
                javafx.application.Platform.runLater(() -> {
                    logArea.appendText("Critical system error occurred during processing.\n");
                    showTechnicalProcessingError();
                });
            } else {
                logArea.appendText("Error: " + errorMsg + "\n");
                showAlert("Error", "Document generation failed: " + errorMsg);
            }
            resetUI();
        }
    };

    Thread thread = new Thread(task);
    thread.setDaemon(true);
    thread.start();
}


    private void resetUI() {
        processButton.setDisable(false);
        progressBar.setVisible(false);
        progressBar.setProgress(0);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
