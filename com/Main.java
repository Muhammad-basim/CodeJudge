import analysis.IssueTracker;
import analysis.ReviewHistory;
import data.ReviewRepository;
import javafx.animation.*;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser;
import javafx.stage.StageStyle;
import model.Review;
import model.ReviewResult;
import service.AIProvider;
import service.AIService;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import service.FileService;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public class Main extends Application {

    private final AIProvider aiService = new AIService();
    private String labelForReview = "";

    @Override
    public void start(Stage primaryStage){
        showSplash(primaryStage);
    }
    public void buildMainWindow(Stage primaryStage) {
        // --- Left pane: input ---
        ToggleGroup modeGroup = new ToggleGroup();
        Label modeLabel = new Label("Mode:");
        RadioButton fileMode = new RadioButton("File");
        RadioButton snippetMode = new RadioButton("Snippet");
        HBox modeBox = new HBox(10,modeLabel, fileMode, snippetMode);
        Label focusModeLabel = new Label("Review Focus:");
        ComboBox<String> focusMode = new ComboBox<>();
        HBox focusBox = new HBox(10, focusModeLabel, focusMode);
        focusMode.getItems().addAll("General", "Security", "Performance", "Readability");
        focusMode.setValue("General"); // default
        Button browseButton = new Button("Browse...");
        Label selectedFileLabel = new Label();

        TextField labelInput = new TextField();
        labelInput.setPromptText("Enter label for the review of the code snippet.");
        TextField typeInput = new TextField();
        typeInput.setPromptText("Enter language/type of the code snippet.");

        TextArea codeInput = new TextArea();
        codeInput.getStyleClass().add("code-font");
        TextFlow resultFlow = new TextFlow();
        resultFlow.setStyle("-fx-font-family: 'Cascadia Code'; -fx-text-fill: #C5C6C7 !important; -fx-background-color: #1F2833; -fx-background-radius: 6px; -fx-border-color: #45A29E; -fx-border-radius: 6px;");

        fileMode.setToggleGroup(modeGroup);
        snippetMode.setToggleGroup(modeGroup);
        snippetMode.setSelected(true);
        browseButton.setVisible(false);
        fileMode.setOnAction(e -> {
            browseButton.setVisible(true);
            labelInput.setVisible(false);
            typeInput.setVisible(false);
        });
        snippetMode.setOnAction(e -> {
            browseButton.setVisible(false);
            labelInput.setVisible(true);
            typeInput.setVisible(true);
        });
        browseButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select a code file");
            File selectedFile = fileChooser.showOpenDialog(primaryStage);
            if (selectedFile != null) {
                try {
                    FileService fileService = new FileService();
                    String content = fileService.readFile(selectedFile.getPath());
                    codeInput.setText(content);
                    labelForReview = selectedFile.getName();
                    selectedFileLabel.setText(labelForReview);
                } catch (IOException ex) {
                    resultFlow.getChildren().clear();
                    resultFlow.getChildren().add(new Text("Could not read file: " + ex.getMessage()));
                }
            }
        });
        codeInput.setPromptText("Paste code here...");
        Button analyzeButton = new Button("Analyze");
        VBox leftPane = new VBox(10, modeBox, focusBox, browseButton, selectedFileLabel, codeInput, labelInput, typeInput, analyzeButton);
        leftPane.setPadding(new Insets(10));
        // --- Right pane: output ---

        resultFlow.setPadding(new Insets(10));
        ScrollPane resultScroll = new ScrollPane(resultFlow);
        resultScroll.setStyle("-fx-min-height: 150px;");
        resultScroll.setFitToWidth(true);
        Label resultScrollLabel = new Label("AI Code Analysis:");

        VBox rightPane = new VBox(10, resultScrollLabel, resultScroll);
        rightPane.setPadding(new Insets(10));

        leftPane.getStyleClass().add("card");
        rightPane.getStyleClass().add("card");
        // --- Split pane ---
        SplitPane splitPane = new SplitPane(leftPane, rightPane);
        splitPane.setDividerPositions(0.5);
        // --- Analyze button logic (same Task pattern as before) ---
        analyzeButton.setOnAction(e -> {
            resultFlow.getChildren().clear();
            Text analyzingText = new Text("Analyzing with AI...");
            analyzingText.setStyle("-fx-fill: #C5C6C7;");
            resultFlow.getChildren().add(analyzingText);
            analyzeButton.setDisable(true);
            if(snippetMode.isSelected()){
                if(labelInput.getText().isBlank() || typeInput.getText().isBlank()){
                    Text errorText = new Text("Please enter both a label and a type for the snippet.");
                    errorText.setStyle("-fx-fill: #45A29E;");
                    resultFlow.getChildren().clear();
                    resultFlow.getChildren().add(errorText);
                    analyzeButton.setDisable(false);
                    return;
                }
                labelForReview = labelInput.getText() + "." + typeInput.getText();
            }
            Task<ReviewResult> task = new Task<>() {
                @Override
                protected ReviewResult call() throws Exception {
                    String priorFeedback = null;
                    try{
                        Review prior = new ReviewRepository().findMostRecentByName(labelForReview);
                        if (prior != null) {
                            priorFeedback = prior.getFeedback();
                        }
                    } catch(SQLException ignored){

                    }
                    return new AIService().reviewCode(codeInput.getText(), focusMode.getValue(), priorFeedback);
                }
            };
            task.setOnSucceeded(event -> {
                ReviewResult result = task.getValue();
                if (result == null) {
                    resultFlow.getChildren().clear();
                    resultFlow.getChildren().add(new Text("AI did not return a valid response. Check your API key in Settings."));
                    analyzeButton.setDisable(false);
                    return;
                }
                resultFlow.getChildren().clear();
                Text codeQualityScoreText = new Text("Code Quality Score: " + result.getScore() + "/10\n\n");
                codeQualityScoreText.setStyle("-fx-fill: #66FCF1; -fx-font-weight: bold;");
                resultFlow.getChildren().add(codeQualityScoreText);
                renderColoredFeedback(resultFlow, "Bugs", result.getBugs());
                renderColoredFeedback(resultFlow, "Performance", result.getPerformance());
                renderColoredFeedback(resultFlow, "Readability", result.getReadability());
                String combinedFeedback = "Bugs: " + (result.getBugs() == null ? "Not provided" : result.getBugs()) +
                        "\nPerformance: " + (result.getPerformance() == null ? "Not provided" : result.getPerformance()) +
                        "\nReadability: " + (result.getReadability() == null ? "Not provided" : result.getReadability());
                Review review = new Review(combinedFeedback, LocalDateTime.now(), labelForReview, result.getScore());
                try {
                    new ReviewRepository().save(review);
                } catch (SQLException ex) {
                    resultFlow.getChildren().add(new Text("\n\n[Could not save to database: " + ex.getMessage() + "]"));
                }
                analyzeButton.setDisable(false);
            });
            task.setOnFailed(event -> {
                resultFlow.getChildren().clear();
                resultFlow.getChildren().add(new Text("Error: " + task.getException().getMessage()));
                analyzeButton.setDisable(false);
            });
            new Thread(task).start();
        });
        // --- History Tab ---
        TableView<Review> historyTable = new TableView<>();
        historyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<Review, String> nameCol = new TableColumn<>("File Name");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFileName()));

        TableColumn<Review, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFileType()));

        TableColumn<Review, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getTimeStamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        ));

        TableColumn<Review, Integer> scoreCol = new TableColumn<>("Score");
        scoreCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getScore()).asObject());

        historyTable.getColumns().addAll(nameCol, typeCol, dateCol, scoreCol);

        Button refreshButton = new Button("Refresh History");
        refreshButton.setOnAction(e -> {
            try {
                List<Review> reviews = new ReviewRepository().findAll();
                historyTable.setItems(FXCollections.observableArrayList(reviews));
            } catch (SQLException ex) {
                // show error somewhere, e.g. a temporary Label
            }
        });

        TextArea feedbackDetail = new TextArea();
        feedbackDetail.setEditable(false);
        feedbackDetail.setPromptText("Select a review above to see full feedback...");

        historyTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                feedbackDetail.setText("Feedback Details: \n"+ newSelection.getFeedback());
            }
        });

        Button exportButton = new Button("Export Selected");
        exportButton.setOnAction(e -> {
            Review selected = historyTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                feedbackDetail.setText("Please select a row to export.");
                return;
            }

            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Export Review");
            fileChooser.setInitialFileName(selected.getFileName() + "_review.md");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Markdown files", "*.md")
            );
            File saveFile = fileChooser.showSaveDialog(primaryStage);

            if (saveFile != null) {
                String content = "# CodeJudge Review: " + selected.getFileName() + "\n\n" +
                        "**Date:** " + selected.getTimeStamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n\n" +
                        "**Score:** " + selected.getScore() + "/10\n\n" +
                        "## Feedback\n\n" + selected.getFeedback();

                try {
                    Files.writeString(saveFile.toPath(), content);
                } catch (IOException ex) {
                    feedbackDetail.setText("Could not export: " + ex.getMessage());
                }
            }
        });

        Button deleteButton = new Button("Delete Selected");
        deleteButton.setOnAction(e -> {
            Review selected = historyTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                feedbackDetail.setText("Please select a row to delete.");
                return;
            }
            try {
                new ReviewRepository().deleteById(selected.getId());
                List<Review> reviews = new ReviewRepository().findAll();
                historyTable.setItems(FXCollections.observableArrayList(reviews));
                feedbackDetail.clear();
            } catch (SQLException ex) {
                feedbackDetail.setText("Could not delete: " + ex.getMessage());
            }
        });

        HBox historyButtons = new HBox(10, refreshButton, deleteButton, exportButton);
        VBox historyPane = new VBox(10, historyButtons, historyTable, feedbackDetail);
        historyPane.setPadding(new Insets(10));

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Review Date");

        NumberAxis yAxis = new NumberAxis(0, 10, 1);
        yAxis.setLabel("Score");

        LineChart<String, Number> trendChart = new LineChart<>(xAxis, yAxis);
        trendChart.setTitle("Quality Trend");

        Button loadTrendButton = new Button("Load Trend");
        loadTrendButton.setOnAction(e -> {
            try {
                List<Review> reviews = new ReviewRepository().findAll();
                ReviewHistory tempHistory = new ReviewHistory();
                tempHistory.loadFrom(reviews);
                List<Review> chronological = tempHistory.sortByDate(false); // oldest first

                XYChart.Series<String, Number> series = new XYChart.Series<>();
                series.setName("Score over time");

                for (Review review : chronological) {
                    String label = review.getTimeStamp().format(DateTimeFormatter.ofPattern("MM-dd HH:mm"));
                    series.getData().add(new XYChart.Data<>(label, review.getScore()));
                }

                trendChart.getData().clear();
                trendChart.getData().add(series);
            } catch (SQLException ex) {
                // optionally show error somewhere
            }
        });
        VBox trendPane = new VBox(10, loadTrendButton, trendChart);
        trendPane.setPadding(new Insets(10));

        Tab trendTab = new Tab("Trend", trendPane);
        trendTab.setClosable(false);

        // Setting tab UI
        PasswordField apiKeyField = new PasswordField();
        apiKeyField.setPromptText("Enter your Gemini API Key");
        Label settingsStatus = new Label();
        Button saveKeyButton = new Button("Save API Key");
        saveKeyButton.setOnAction(e -> {
            try{
                Path dir = Path.of(System.getProperty("user.home"), ".codejudge");
                Files.createDirectories(dir);
                Path path = dir.resolve("config.properties");
                Properties props = new Properties();
                props.setProperty("gemini.api.key", apiKeyField.getText());
                try(OutputStream out = Files.newOutputStream(path)){
                    props.store(out, "CodeJudge Config");
                }
                settingsStatus.setText("API key saved.");
            }catch(IOException ex){
                settingsStatus.setText("Could not save: "+ex.getMessage());
            }
        });
        VBox settingsPane = new VBox(10, new Label("Gemini API Key:"),apiKeyField,saveKeyButton, settingsStatus);
        settingsPane.setPadding(new Insets(20));
        Tab settingsTab = new Tab("Settings", settingsPane);
        settingsTab.setClosable(false);

        Label totalReviewsLabel = new Label();
        Label avgScoreLabel = new Label();
        Label topIssueLabel = new Label();
        Label lastFileLabel = new Label();

        Button refreshDashboardButton = new Button("Refresh Dashboard");
        refreshDashboardButton.setOnAction(e -> refreshDashboard(totalReviewsLabel, avgScoreLabel, topIssueLabel, lastFileLabel));
        refreshDashboard(totalReviewsLabel, avgScoreLabel, topIssueLabel, lastFileLabel);
        VBox totalCard = buildStatCard("Total Reviews: ", totalReviewsLabel);
        VBox avgCard = buildStatCard("Average Score: ", avgScoreLabel);
        VBox issueCard = buildStatCard("Most Common Issue: ", topIssueLabel);
        VBox lastCard = buildStatCard("Last Analyzed: ", lastFileLabel);
        HBox statsRow = new HBox(15, totalCard, avgCard, issueCard, lastCard);
        VBox dashboardPane = new VBox(20, refreshDashboardButton, statsRow);
        dashboardPane.setPadding(new Insets(20));
        Tab dashboardTab = new Tab("Dashboard", dashboardPane);
        dashboardTab.setClosable(false);


// --- Wrap everything in tabs ---
        Tab analyzeTab = new Tab("Analyze", splitPane);
        analyzeTab.setClosable(false);

        Tab historyTab = new Tab("History", historyPane);
        historyTab.setClosable(false);

        TabPane tabPane = new TabPane(dashboardTab, analyzeTab, historyTab, trendTab, settingsTab);

        Label appTitle = new Label("CodeJudge");
        appTitle.setId("appTitle");

        Label appTagline = new Label("AI-Powered Code Quality Tracker");
        appTagline.setStyle("-fx-text-fill: #a6adc8; -fx-font-size: 12px; -fx-padding: 8px 8px 0 30px;");

        VBox header = new VBox(2, appTitle, appTagline);
        header.setPadding(new Insets(10, 10, 0, 10));

        BorderPane mainLayout = new BorderPane();
        mainLayout.setTop(header);
        mainLayout.setCenter(tabPane);

        Scene scene = new Scene(mainLayout, 950, 650);

        primaryStage.setTitle("CodeJudge — AI Code Quality Tracker");
        primaryStage.setScene(scene);
        scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
        primaryStage.show();
    }

    private void showSplash(Stage primaryStage) {
        Label splashTitle = new Label("CodeJudge");
        splashTitle.setStyle(
                "-fx-font-size: 36px; -fx-font-weight: 900; -fx-text-fill: #66FCF1;" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(102, 252, 241, 0.3), 10, 0, 0, 0);"
        );

        Label splashTagline = new Label("Starting application...");
        splashTagline.setStyle("-fx-font-size: 13px; -fx-text-fill: #C5C6C7;");

        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(280);
        progressBar.setPrefHeight(6);
        progressBar.setStyle("-fx-background-color: transparent;");

        VBox splashLayout = new VBox(12, splashTitle, splashTagline, progressBar);
        splashLayout.setAlignment(Pos.CENTER);
        splashLayout.setStyle(
                "-fx-background-color: #0B0C10; -fx-border-color: #45A29E;" +
                        "-fx-border-width: 1px; -fx-border-radius: 8px; -fx-background-radius: 8px;"
        );
        splashLayout.setPrefSize(450, 280);

        Stage splashStage = new Stage();
        splashStage.initStyle(StageStyle.TRANSPARENT);
        splashLayout.setStyle(splashLayout.getStyle());
        Scene splashScene = new Scene(splashLayout);
        splashScene.setFill(Color.TRANSPARENT);
        splashScene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
        splashStage.setScene(splashScene);
        splashStage.centerOnScreen();
        splashStage.show();

        Task<Void> loadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                String[] messages = {
                        "Initializing core system modules...",
                        "Connecting to code analysis engine...",
                        "Checking project workspaces...",
                        "Finalizing workspace layout..."
                };
                int total = messages.length;
                for (int i = 0; i < total; i++) {
                    updateMessage(messages[i]);
                    updateProgress(i + 1, total);
                    Thread.sleep(1000);
                }
                return null;
            }
        };

        progressBar.progressProperty().bind(loadTask.progressProperty());
        splashTagline.textProperty().bind(loadTask.messageProperty());

        loadTask.setOnSucceeded(e -> {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(300), splashLayout);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(finishEvent -> {
                splashStage.close();
                buildMainWindow(primaryStage);
            });
            fadeOut.play();
        });

        loadTask.setOnFailed(e -> {
            loadTask.getException().printStackTrace();
            splashStage.close();
            buildMainWindow(primaryStage);
        });

        Thread thread = new Thread(loadTask);
        thread.setDaemon(true);
        thread.start();
    }

    private void refreshDashboard(Label totalReviewsLabel, Label avgScoreLabel, Label topIssueLabel, Label lastFileLabel) {
        try{
            List<Review> reviews = new ReviewRepository().findAll();
            totalReviewsLabel.setText(String.valueOf(reviews.size()));
            int total = 0;
            for(Review r : reviews){
                total += r.getScore();
            }
            double avg = reviews.isEmpty() ? 0 : (double) total / reviews.size();
            avgScoreLabel.setText(String.format("%.1f/10",avg));
            Map<String, Integer> counts = new IssueTracker().countIssues(reviews);
            String topIssue = "None yet";
            int maxCount = 0;
            for(Map.Entry<String, Integer> entry : counts.entrySet()){
                if(entry.getValue() > maxCount){
                    maxCount = entry.getValue();
                    topIssue = entry.getKey() + " (" + entry.getValue() + " occurences)";
                }
            }
            topIssueLabel.setText(topIssue);
            ReviewHistory tempHistory = new ReviewHistory();
            tempHistory.loadFrom(reviews);
            List<Review> newestFirst = tempHistory.sortByDate(true);
            String lastFile = newestFirst.isEmpty() ? "None yet" : newestFirst.get(0).getFileName();
            lastFileLabel.setText(lastFile);
        }catch(SQLException ex){
            totalReviewsLabel.setText("Could bot load dashboard: "+ex.getMessage());
        }
    }
    private void renderColoredFeedback(TextFlow flow, String label, String content) {
        Text headerText = new Text(label + ":\n");
        headerText.setFill(Color.web("#66FCF1"));
        headerText.setStyle("-fx-font-weight: bold;");
        flow.getChildren().add(headerText);

        for (String line : content.split("\n")) {
            Color color = Color.web("#C5C6C7");
            if (line.contains("[CRITICAL]")) color = Color.web("#FF6B6B");
            else if (line.contains("[WARNING]")) color = Color.web("#FFD93D");
            else if (line.contains("[SUGGESTION]")) color = Color.web("#66FCF1");
            else if (line.contains("[IMPROVED]")) color = Color.web("#6BCB77");
            Text text = new Text(line + "\n");
            text.setFill(color);
            flow.getChildren().add(text);
        }
        flow.getChildren().add(new Text("\n"));
    }
    private VBox buildStatCard(String title, Label valueLabel) {
        valueLabel.getStyleClass().add("stat-value");
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("stat-label");
        VBox card = new VBox(8, titleLabel, valueLabel);
        card.getStyleClass().add("card");
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }
    public static void main(String[] args) {
        launch(args);
    }
}
