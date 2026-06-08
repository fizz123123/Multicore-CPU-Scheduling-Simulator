package ui;

import engine.SimulationEngine;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import model.EventRecord;
import model.Process;
import model.SimulationConfig;
import model.SimulationResult;
import scheduler.FCFSScheduler;
import scheduler.RRScheduler;
import scheduler.SJFScheduler;
import scheduler.Scheduler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MainController {

    @FXML
    private ComboBox<String> algorithmComboBox;

    @FXML
    private Spinner<Integer> timeSliceSpinner;

    @FXML
    private Spinner<Integer> coreCountSpinner;

    @FXML
    private Spinner<Integer> timeMultiplierSpinner;

    @FXML
    private RadioButton randomModeRadio;

    @FXML
    private RadioButton customModeRadio;

    @FXML
    private Slider randomProcessCountSlider;

    @FXML
    private Label randomProcessCountLabel;

    @FXML
    private TextField randomSeedTextField;

    @FXML
    private Button startButton;

    @FXML
    private Button clearButton;

    @FXML
    private Label averageWaitingTimeLabel;

    @FXML
    private Label averageTurnaroundTimeLabel;

    @FXML
    private Label totalSimulationTimeLabel;

    @FXML
    private Pane ganttChartPane;

    @FXML
    private TableView<Process> customProcessTable;

    @FXML
    private TableColumn<Process, String> processIdColumn;

    @FXML
    private TableColumn<Process, Integer> arrivalTimeColumn;

    @FXML
    private TableColumn<Process, Integer> burstTimeColumn;

    @FXML
    private TextField arrivalTimeTextField;

    @FXML
    private TextField burstTimeTextField;

    @FXML
    private Button addProcessButton;

    @FXML
    private Button removeProcessButton;

    @FXML
    private Label statusLabel;

    private final ObservableList<Process> customProcesses = FXCollections.observableArrayList();
    private final ObservableList<Process> simulationProcesses = FXCollections.observableArrayList();

    private static final int MIN_ARRIVAL_TIME = 0;
    private static final int MAX_ARRIVAL_TIME = 20;
    private static final int MIN_BURST_TIME = 1;
    private static final int MAX_BURST_TIME = 10;
    private int nextProcessNumber = 1;
    private int currentCoreCount = 0;

    @FXML
    public void initialize() {
        initializeAlgorithmComboBox();
        initializeSpinners();
        initializeSlider();
        initializeCustomProcessTable();
        initializeModeControls();

        showStatus("Ready");
    }

    private void initializeAlgorithmComboBox() {
        algorithmComboBox.setItems(
                FXCollections.observableArrayList(
                        "FCFS",
                        "SJF",
                        "RR"
                )
        );

        algorithmComboBox.getSelectionModel().select("FCFS");

        algorithmComboBox.valueProperty().addListener(
                (observable, oldValue, newValue) -> {
                    boolean isRR = "RR".equals(newValue);
                    timeSliceSpinner.setDisable(!isRR);
                }
        );

        timeSliceSpinner.setDisable(true);
    }

    private void initializeSpinners() {
        coreCountSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(
                        1,
                        4,
                        4
                )
        );

        timeMultiplierSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(
                        10,
                        500,
                        100,
                        10
                )
        );

        timeSliceSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(
                        1,
                        20,
                        2
                )
        );
    }

    private void initializeSlider() {
        randomProcessCountSlider.valueProperty().addListener(
                (observable, oldValue, newValue) -> {
                    int count = newValue.intValue();
                    randomProcessCountLabel.setText("目前數量：" + count);
                }
        );
    }

    private void initializeCustomProcessTable() {
        processIdColumn.setCellValueFactory(
                new PropertyValueFactory<>("processId")
        );

        arrivalTimeColumn.setCellValueFactory(
                new PropertyValueFactory<>("arrivalTime")
        );

        burstTimeColumn.setCellValueFactory(
                new PropertyValueFactory<>("burstTime")
        );

        customProcessTable.setItems(customProcesses);
    }

    private void initializeModeControls() {
        randomModeRadio.selectedProperty().addListener(
                (observable, oldValue, isSelected) -> updateModeControls()
        );

        customModeRadio.selectedProperty().addListener(
                (observable, oldValue, isSelected) -> updateModeControls()
        );

        updateModeControls();
    }

    private void updateModeControls() {
        boolean randomMode = randomModeRadio.isSelected();

        randomProcessCountSlider.setDisable(!randomMode);
        randomSeedTextField.setDisable(!randomMode);

        // 程序清單在兩種模式下都保留可讀狀態：
        // 自訂模式顯示使用者手動加入的程序；隨機模式顯示模擬後產生的程序。
        customProcessTable.setDisable(false);
        customProcessTable.setItems(randomMode ? simulationProcesses : customProcesses);

        arrivalTimeTextField.setDisable(randomMode);
        burstTimeTextField.setDisable(randomMode);
        addProcessButton.setDisable(randomMode);
        removeProcessButton.setDisable(randomMode);
    }

    @FXML
    private void handleStartSimulation() {
        try {
            SimulationConfig config = buildSimulationConfig();
            currentCoreCount = config.coreCount();
            Scheduler scheduler = buildScheduler(config);

            clearResultOnly();

            startButton.setDisable(true);
            showStatus("模擬執行中...");

            Task<SimulationResult> task = new Task<>() {
                @Override
                protected SimulationResult call() {
                    SimulationEngine engine =
                            new SimulationEngine(config, scheduler);

                    return engine.run();
                }
            };

            task.setOnSucceeded(event -> {
                SimulationResult result = task.getValue();

                if (result == null) {
                    showValidationError("模擬被中斷，未產生結果");
                    startButton.setDisable(false);
                    return;
                }

                showResult(result);
                showStatus("模擬完成");
                startButton.setDisable(false);
            });

            task.setOnFailed(event -> {
                Throwable exception = task.getException();

                showError(
                        "模擬執行失敗",
                        exception == null
                                ? "未知錯誤"
                                : exception.getMessage()
                );

                showValidationError("模擬失敗");
                startButton.setDisable(false);
            });

            Thread simulationThread =
                    new Thread(task, "SimulationTask");

            simulationThread.setDaemon(true);
            simulationThread.start();

        } catch (IllegalArgumentException e) {
            showValidationError(e.getMessage());
        }
    }

    @FXML
    private void handleClearResult() {
        averageWaitingTimeLabel.setText("-");
        averageTurnaroundTimeLabel.setText("-");
        totalSimulationTimeLabel.setText("-");
        ganttChartPane.getChildren().clear();

        if (randomModeRadio.isSelected()) {
            simulationProcesses.clear();
            customProcessTable.setItems(simulationProcesses);
        }

        showStatus("Ready");
    }

    @FXML
    private void handleAddCustomProcess() {
        try {
            int arrivalTime = parseAndValidateInteger(
                    "Arrival Time",
                    arrivalTimeTextField.getText(),
                    MIN_ARRIVAL_TIME,
                    MAX_ARRIVAL_TIME
            );

            int burstTime = parseAndValidateInteger(
                    "Burst Time",
                    burstTimeTextField.getText(),
                    MIN_BURST_TIME,
                    MAX_BURST_TIME
            );

            String processId = generateNextProcessId();

            customProcesses.add(
                    new Process(
                            processId,
                            arrivalTime,
                            burstTime
                    )
            );

            arrivalTimeTextField.clear();
            burstTimeTextField.clear();

            showStatus("已新增程序：" + processId);

        } catch (IllegalArgumentException e) {
            showValidationError(e.getMessage());
        }
    }

    private int parseAndValidateInteger(
            String fieldName,
            String input,
            int min,
            int max
    ) {
        String text = input.trim();

        if (text.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " 不可為空，合法範圍：" + min + " ~ " + max
            );
        }

        int value;

        try {
            value = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    fieldName + " 必須是整數，合法範圍：" + min + " ~ " + max
            );
        }

        if (value < min || value > max) {
            throw new IllegalArgumentException(
                    fieldName + " 合法範圍為：" + min + " ~ " + max
            );
        }

        return value;
    }

    private String generateNextProcessId() {
        return "P" + nextProcessNumber++;
    }

    @FXML
    private void handleRemoveCustomProcess() {
        Process selected =
                customProcessTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showValidationError("請先選取要刪除的程序");
            return;
        }

        customProcesses.remove(selected);
        showStatus("已刪除程序：" + selected.getProcessId());
    }

    private SimulationConfig buildSimulationConfig() {
        int coreCount = coreCountSpinner.getValue();
        int timeMultiplier = timeMultiplierSpinner.getValue();
        int timeSlice = timeSliceSpinner.getValue();

        boolean isRandomMode = randomModeRadio.isSelected();

        int randomProcessCount =
                (int) randomProcessCountSlider.getValue();

        Long randomSeed = parseRandomSeed();

        List<Process> copiedCustomProcesses =
                copyCustomProcesses();

        if (!isRandomMode && copiedCustomProcesses.isEmpty()) {
            throw new IllegalArgumentException(
                    "自訂模式至少需要新增一個程序"
            );
        }

        return new SimulationConfig(
                coreCount,
                timeMultiplier,
                timeSlice,
                isRandomMode,
                randomProcessCount,
                randomSeed,
                copiedCustomProcesses
        );
    }

    private Long parseRandomSeed() {
        String text = randomSeedTextField.getText().trim();

        if (text.isBlank()) {
            return null;
        }

        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Random Seed 必須是整數，或保持空白"
            );
        }
    }

    private List<Process> copyCustomProcesses() {
        List<Process> copied = new ArrayList<>();

        for (Process process : customProcesses) {
            copied.add(
                    new Process(
                            process.getProcessId(),
                            process.getArrivalTime(),
                            process.getBurstTime()
                    )
            );
        }

        return copied;
    }

    private Scheduler buildScheduler(SimulationConfig config) {
        String selectedAlgorithm =
                algorithmComboBox.getSelectionModel().getSelectedItem();

        if ("FCFS".equals(selectedAlgorithm)) {
            return new FCFSScheduler();
        }

        if ("SJF".equals(selectedAlgorithm)) {
            return new SJFScheduler();
        }

        if ("RR".equals(selectedAlgorithm)) {
            return new RRScheduler(config.timeSlice());
        }

        throw new IllegalArgumentException("尚未選擇排程演算法");
    }

    private void showResult(SimulationResult result) {
        averageWaitingTimeLabel.setText(
                String.format("%.2f", result.averageWaitingTime())
        );

        averageTurnaroundTimeLabel.setText(
                String.format("%.2f", result.averageTurnaroundTime())
        );

        totalSimulationTimeLabel.setText(
                result.totalSimulationTime() + " TU"
        );

        updateProcessTable(result);
        drawGanttChart(result);
    }

    private void updateProcessTable(SimulationResult result) {
        if (randomModeRadio.isSelected()) {
            simulationProcesses.setAll(result.processes());
            customProcessTable.setItems(simulationProcesses);
        } else {
            customProcesses.setAll(result.processes());
            customProcessTable.setItems(customProcesses);
        }
    }

    private void drawGanttChart(SimulationResult result) {
        ganttChartPane.getChildren().clear();

        List<EventRecord> records =
                new ArrayList<>(result.eventLog());

        records.sort(
                Comparator.comparingInt(EventRecord::startTime)
                        .thenComparing(EventRecord::coreName)
        );

        if (records.isEmpty()) {
            ganttChartPane.getChildren().add(
                    new Text(20, 30, "尚無事件紀錄")
            );
            return;
        }

        double leftMargin = 70;
        double topMargin = 42;
        double rowHeight = 56;
        double barHeight = 28;
        double unitWidth = 35;

        int maxEndTime =
                records.stream()
                        .mapToInt(EventRecord::endTime)
                        .max()
                        .orElse(0);

        int timelineEnd = Math.max(
                maxEndTime,
                result.totalSimulationTime()
        );

        List<String> coreNames = buildCoreNames(records);

        drawTimeGrid(
                leftMargin,
                topMargin,
                rowHeight,
                barHeight,
                unitWidth,
                timelineEnd,
                coreNames.size()
        );

        for (int i = 0; i < coreNames.size(); i++) {
            String coreName = coreNames.get(i);

            double y = topMargin + i * rowHeight;

            drawCoreRow(
                    coreName,
                    leftMargin,
                    y,
                    barHeight,
                    unitWidth,
                    timelineEnd
            );

            List<EventRecord> coreRecords =
                    records.stream()
                            .filter(record -> record.coreName().equals(coreName))
                            .sorted(Comparator.comparingInt(EventRecord::startTime))
                            .toList();

            int previousEndTime = 0;

            for (EventRecord record : coreRecords) {
                if (record.startTime() > previousEndTime) {
                    drawIdleBlock(
                            previousEndTime,
                            record.startTime(),
                            leftMargin,
                            y,
                            unitWidth,
                            barHeight
                    );
                }

                drawProcessBlock(
                        record,
                        leftMargin,
                        y,
                        unitWidth,
                        barHeight
                );

                previousEndTime = record.endTime();
            }

            if (previousEndTime < timelineEnd) {
                drawIdleBlock(
                        previousEndTime,
                        timelineEnd,
                        leftMargin,
                        y,
                        unitWidth,
                        barHeight
                );
            }

            drawCoreTimeLabels(
                    coreRecords,
                    timelineEnd,
                    leftMargin,
                    y,
                    unitWidth,
                    barHeight
            );
        }

        ganttChartPane.setPrefWidth(
                leftMargin + timelineEnd * unitWidth + 120
        );

        ganttChartPane.setPrefHeight(
                topMargin + coreNames.size() * rowHeight + 50
        );
    }

    private List<String> buildCoreNames(List<EventRecord> records) {
        if (currentCoreCount > 0) {
            List<String> coreNames = new ArrayList<>();

            for (int i = 1; i <= currentCoreCount; i++) {
                coreNames.add("Core-" + i);
            }

            return coreNames;
        }

        return records.stream()
                .map(EventRecord::coreName)
                .distinct()
                .sorted()
                .toList();
    }

    private void drawTimeGrid(
            double leftMargin,
            double topMargin,
            double rowHeight,
            double barHeight,
            double unitWidth,
            int timelineEnd,
            int rowCount
    ) {
        int tickInterval = 5;
        double gridHeight = rowCount * rowHeight - rowHeight + barHeight;

        for (int time = 0; time <= timelineEnd; time += tickInterval) {
            double x = leftMargin + time * unitWidth;

            Text tickLabel = new Text(
                    x - 4,
                    topMargin - 14,
                    String.valueOf(time)
            );

            tickLabel.setFill(Color.GRAY);

            Rectangle gridLine = new Rectangle(
                    x,
                    topMargin - 8,
                    1,
                    gridHeight + 12
            );

            gridLine.setFill(Color.rgb(235, 235, 235));

            ganttChartPane.getChildren().addAll(
                    gridLine,
                    tickLabel
            );
        }
    }

    private void drawCoreRow(
            String coreName,
            double leftMargin,
            double y,
            double barHeight,
            double unitWidth,
            int timelineEnd
    ) {
        Text coreLabel = new Text(
                10,
                y + 20,
                coreName
        );

        Rectangle rowTrack = new Rectangle(
                leftMargin,
                y,
                Math.max(unitWidth, timelineEnd * unitWidth),
                barHeight
        );

        rowTrack.setArcWidth(6);
        rowTrack.setArcHeight(6);
        rowTrack.setFill(Color.rgb(248, 248, 248));
        rowTrack.setStroke(Color.rgb(220, 220, 220));

        ganttChartPane.getChildren().addAll(
                rowTrack,
                coreLabel
        );
    }

    private void drawIdleBlock(
            int startTime,
            int endTime,
            double leftMargin,
            double y,
            double unitWidth,
            double barHeight
    ) {
        if (endTime <= startTime) {
            return;
        }

        double x = leftMargin + startTime * unitWidth;
        double width = (endTime - startTime) * unitWidth;

        Rectangle block = new Rectangle(
                x,
                y,
                width,
                barHeight
        );

        block.setArcWidth(6);
        block.setArcHeight(6);
        block.setFill(Color.rgb(235, 235, 235));
        block.setStroke(Color.rgb(160, 160, 160));
        block.getStrokeDashArray().addAll(6.0, 4.0);

        Tooltip tooltip = new Tooltip(
                "State: IDLE"
                        + "\nTime: " + startTime + " → " + endTime
        );

        Tooltip.install(block, tooltip);

        ganttChartPane.getChildren().add(block);

        if (width >= 55) {
            Text idleLabel = new Text(
                    x + 6,
                    y + 19,
                    "IDLE"
            );

            idleLabel.setFill(Color.rgb(100, 100, 100));
            Tooltip.install(idleLabel, tooltip);
            ganttChartPane.getChildren().add(idleLabel);
        }
    }

    private void drawProcessBlock(
            EventRecord record,
            double leftMargin,
            double y,
            double unitWidth,
            double barHeight
    ) {
        double x = leftMargin + record.startTime() * unitWidth;

        double width = Math.max(
                8,
                (record.endTime() - record.startTime()) * unitWidth
        );

        Rectangle block = new Rectangle(
                x,
                y,
                width,
                barHeight
        );

        block.setArcWidth(6);
        block.setArcHeight(6);
        block.setFill(getProcessColor(record.processId()));
        block.setStroke(Color.BLACK);

        Tooltip tooltip = new Tooltip(
                "Process: " + record.processId()
                        + "\nCore: " + record.coreName()
                        + "\nExecution: #" + record.executionCount()
                        + "\nTime: " + record.startTime() + " → " + record.endTime()
        );

        Tooltip.install(block, tooltip);

        ganttChartPane.getChildren().add(block);

        String labelText;

        if (width >= 70) {
            labelText =
                    record.processId()
                            + " (#"
                            + record.executionCount()
                            + ")";
        } else {
            labelText = record.processId();
        }

        Text label = new Text(
                x + 6,
                y + 19,
                labelText
        );

        Tooltip.install(label, tooltip);

        ganttChartPane.getChildren().add(label);
    }

    private void drawCoreTimeLabels(
            List<EventRecord> coreRecords,
            int timelineEnd,
            double leftMargin,
            double y,
            double unitWidth,
            double barHeight
    ) {
        java.util.TreeSet<Integer> timeBoundaries = new java.util.TreeSet<>();
        timeBoundaries.add(0);
        timeBoundaries.add(timelineEnd);

        for (EventRecord record : coreRecords) {
            timeBoundaries.add(record.startTime());
            timeBoundaries.add(record.endTime());
        }

        for (Integer time : timeBoundaries) {
            drawTimeBoundaryLabel(
                    time,
                    timelineEnd,
                    leftMargin,
                    y,
                    unitWidth,
                    barHeight
            );
        }
    }

    private void drawTimeBoundaryLabel(
            int time,
            int timelineEnd,
            double leftMargin,
            double y,
            double unitWidth,
            double barHeight
    ) {
        Text timeLabel = new Text(String.valueOf(time));
        timeLabel.setFill(Color.rgb(70, 70, 70));

        double labelWidth = timeLabel.getLayoutBounds().getWidth();
        double x = leftMargin + time * unitWidth - labelWidth / 2;

        if (time == 0) {
            x = leftMargin;
        } else if (time == timelineEnd) {
            x = leftMargin + time * unitWidth - labelWidth;
        }

        timeLabel.setX(x);
        timeLabel.setY(y + barHeight + 16);

        ganttChartPane.getChildren().add(timeLabel);
    }

    private Color getProcessColor(String processId) {
        int hash = Math.abs(processId.hashCode());

        Color[] colors = {
                Color.LIGHTBLUE,
                Color.LIGHTGREEN,
                Color.LIGHTPINK,
                Color.LIGHTYELLOW,
                Color.LIGHTSALMON,
                Color.LIGHTCYAN,
                Color.PLUM,
                Color.KHAKI
        };

        return colors[hash % colors.length];
    }

    private void clearResultOnly() {
        averageWaitingTimeLabel.setText("-");
        averageTurnaroundTimeLabel.setText("-");
        totalSimulationTimeLabel.setText("-");
        ganttChartPane.getChildren().clear();
    }

    private void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);

            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private void showValidationError(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: #c0392b; -fx-font-weight: bold;");
    }

    private void showStatus(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: #2c3e50; -fx-font-weight: normal;");
    }
}