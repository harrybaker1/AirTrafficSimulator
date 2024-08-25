/**
 * -----------------------------------------------------
 * GUIManager.java
 * -----------------------------------------------------
 * Assignment 1
 * Software Architecture and Extensible Design - COMP3003
 * Curtin University
 * 25/08/2024
 * -----------------------------------------------------
 * Harrison Baker
 * 19514341
 * -----------------------------------------------------
 * */

package edu.curtin.saed.assignment1;

import java.util.HashMap;
import java.util.Map;
import edu.curtin.saed.assignment1.Plane.FlightStatus;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.util.Pair;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;

public class GUIManager {
    private Stage stage;
    private GridArea gridArea;
    private Label statusText;
    private TextArea messageArea;
    private Button startBtn;
    private Button endBtn;
    private CheckBox gridLinesCheckBox;
    private CompositeDisposable compositeDisposable;
    private Map<Integer, GridAreaIcon> planeIcons;
    private SimulationManager simulationManager;
    private boolean isSimulationConfigured = false;

    public GUIManager(SimulationManager simulationManager, Stage stage) {
        this.simulationManager = simulationManager;
        this.stage = stage;
        planeIcons = new HashMap<>();
        compositeDisposable = new CompositeDisposable();
        initComponents();

        compositeDisposable.add(simulationManager.getPlaneSubject()
            .subscribe(plane -> Platform.runLater(() -> updatePlane(plane)), Throwable::printStackTrace));

        compositeDisposable.add(simulationManager.getAirportListSubject()
            .subscribe(airports -> Platform.runLater(() -> updateAirports(airports)), Throwable::printStackTrace));
        
        compositeDisposable.add(simulationManager.getLogSubject()
            .subscribe(message -> Platform.runLater(() -> logMessage(message)), Throwable::printStackTrace));

        compositeDisposable.add(simulationManager.getStatsSubject()
            .subscribe(stats -> Platform.runLater(() -> updateStatistics(stats)), Throwable::printStackTrace));

        showInputDialog();
    }

    private void initComponents() {
        gridArea = new GridArea(SimulationManager.MAP_WIDTH, SimulationManager.MAP_HEIGHT);
        gridArea.setStyle("-fx-background-color: #4CAF50;");

        startBtn = new Button("Start");
        endBtn = new Button("End");
        endBtn.setDisable(true);

        gridLinesCheckBox = new CheckBox("Show Grid Lines");
        gridLinesCheckBox.setSelected(true);
        gridLinesCheckBox.setOnAction(event -> {
            gridArea.setGridLines(gridLinesCheckBox.isSelected());
            gridArea.requestLayout();
        });

        startBtn.setOnAction(event -> {
            if (!isSimulationConfigured) {
                showInputDialog();
                return;
            }
            startSimulation();
        });

        endBtn.setOnAction(event -> endSimulation());
        stage.setOnCloseRequest(event -> endSimulation());

        statusText = new Label("In Flight: 0" + 
                           "\tUnder Service: 0" +
                           "\tCompleted Trips: 0");
        messageArea = new TextArea();
        messageArea.setEditable(false);
        messageArea.setWrapText(true);

        ToolBar toolbar = new ToolBar();
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        toolbar.getItems().addAll(startBtn, endBtn, new Separator(), gridLinesCheckBox, new Separator(), spacer, new Separator(), statusText);

        SplitPane splitPane = new SplitPane();
        splitPane.getItems().addAll(gridArea, messageArea);
        splitPane.setDividerPositions(0.71);


        BorderPane contentPane = new BorderPane();
        contentPane.setTop(toolbar);
        contentPane.setCenter(splitPane);

        Scene scene = new Scene(contentPane, 1000, 1000);
        stage.setTitle("Air Traffic Simulator");
        Image icon = new Image(App.class.getClassLoader().getResourceAsStream("airport.png"));
        stage.getIcons().add(icon);
        stage.setScene(scene);
        stage.show();
    }

    private void startSimulation() {
        startBtn.setDisable(true);
        endBtn.setDisable(false);

        simulationManager.startSimulation();
    }

    private void endSimulation() {
        endBtn.setDisable(true);
        startBtn.setDisable(false);

        simulationManager.endSimulation();
        isSimulationConfigured = false;
        dispose();
    }

    private void updatePlane(Plane plane) {
        Platform.runLater(() -> {
            GridAreaIcon icon = planeIcons.get(plane.getId());
            if (icon == null) {
                icon = new GridAreaIcon(
                    plane.getXCoord(), plane.getYCoord(), plane.getDirection(), 0.7,
                    App.class.getClassLoader().getResourceAsStream("plane.png"),
                    String.valueOf(plane.getId()));
                planeIcons.put(plane.getId(), icon);
                gridArea.getIcons().add(icon);
            } else {
                icon.setPosition(plane.getXCoord(), plane.getYCoord());
                icon.setRotation(plane.getDirection());
            }

            icon.setShown(plane.getFlightStatus() == FlightStatus.IN_FLIGHT);

            gridArea.requestLayout();
        });
    }

    private void updateAirports(Map<Integer, Airport> airports) {
        Platform.runLater(() -> {
            for (Airport airport : airports.values()) {
                GridAreaIcon icon = new GridAreaIcon(
                    airport.getXCoord(), airport.getYCoord(), 0.0, 1.0,
                    App.class.getClassLoader().getResourceAsStream("airport.png"),
                    String.valueOf(airport.getId()));
                gridArea.getIcons().add(icon);
            }
            gridArea.requestLayout();
        });
    }

    public void logMessage(String message) {
        messageArea.appendText(message + "\n");
    }

    public void updateStatistics(Map<String, Integer> stats) {
        int planesInFlight = stats.getOrDefault("planesInFlight", 0);
        int planesUnderService = stats.getOrDefault("planesUnderService", 0);
        int completedTrips = stats.getOrDefault("completedTrips", 0);
        
        statusText.setText("In Flight: " + planesInFlight + 
                           "\tUnder Service: " + planesUnderService + 
                           "\tCompleted Trips: " + completedTrips);
    }

    private void showInputDialog() {
        Dialog<Pair<Integer, Pair<Integer, Double>>> dialog = new Dialog<>();
        
        // Remove the title and header
        dialog.setTitle(null);
        dialog.setHeaderText(null);
    
        // Set the OK button type
        ButtonType loadButton = new ButtonType("Load", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().add(loadButton);
        Button loadButtonNode = (Button) dialog.getDialogPane().lookupButton(loadButton);
        loadButtonNode.setDisable(true); // Disable by default
    
        // Create the fields and labels
        GridPane grid = new GridPane();
        grid.setHgap(40);
    
        TextField airportsField = new TextField();
        airportsField.setPrefWidth(40);
        TextField planesPerAirportField = new TextField();
        planesPerAirportField.setPrefWidth(40);
        TextField planeSpeedField = new TextField();
        planeSpeedField.setPrefWidth(40);
    
        grid.add(new Label("Airports (2 - 10)"), 0, 0);
        grid.add(airportsField, 1, 0);
        grid.add(new Label("Planes Per Airport (1 - 10)"), 0, 1);
        grid.add(planesPerAirportField, 1, 1);
        grid.add(new Label("Plane Speed (0.1 - 2.0)"), 0, 2);
        grid.add(planeSpeedField, 1, 2);
    
        dialog.getDialogPane().setContent(grid);
    
        // Validation logic
        ChangeListener<String> validationListener = (observable, oldValue, newValue) -> {
            try {
                int airports = Integer.parseInt(airportsField.getText());
                int planesPerAirport = Integer.parseInt(planesPerAirportField.getText());
                double speed = Double.parseDouble(planeSpeedField.getText());
    
                boolean isValid = airports >= 2 && airports <= 10 &&
                                  planesPerAirport >= 1 && planesPerAirport <= 10 &&
                                  speed >= 0.1 && speed <= 2.0;
    
                loadButtonNode.setDisable(!isValid);
            } catch (NumberFormatException e) {
                loadButtonNode.setDisable(true); // Disable the button if input is invalid
            }
        };
    
        // Attach validation listener to all input fields
        airportsField.textProperty().addListener(validationListener);
        planesPerAirportField.textProperty().addListener(validationListener);
        planeSpeedField.textProperty().addListener(validationListener);
    
        // Convert the result to a pair of inputs when the OK button is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loadButton) {
                return new Pair<>(
                    Integer.parseInt(airportsField.getText()),
                    new Pair<>(
                        Integer.parseInt(planesPerAirportField.getText()),
                        Double.parseDouble(planeSpeedField.getText())
                    )
                );
            }
            return null;
        });
    
        // Show the dialog and process the result or use default values if canceled
        dialog.showAndWait().ifPresentOrElse(result -> {
            int numAirports = result.getKey();
            int numPlanesPerAirport = result.getValue().getKey();
            double planeSpeed = result.getValue().getValue();
            reset();
            isSimulationConfigured = true;
            simulationManager.loadSimulation(numAirports, numPlanesPerAirport, planeSpeed);
        }, () -> {
            // Use default values if the dialog is closed with the "X" button or "Cancel"
            reset();
            isSimulationConfigured = true;
            simulationManager.loadSimulation(10, 10, 1.0);
        });
    }

    private void reset() {
        gridArea.getIcons().clear(); 
        planeIcons.clear();

        messageArea.clear();
        statusText.setText("In Flight: 0\tUnder Service: 0\tCompleted Trips: 0");

        gridArea.requestLayout();
    }

    public void dispose() {
        if (compositeDisposable != null && !compositeDisposable.isDisposed()) {
            compositeDisposable.dispose();
        }
    }
}
