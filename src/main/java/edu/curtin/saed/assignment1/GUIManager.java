package edu.curtin.saed.assignment1;

import java.util.HashMap;
import java.util.Map;
import edu.curtin.saed.assignment1.Plane.FlightStatus;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

@SuppressWarnings("PMD")
public class GUIManager {
    private Stage stage;
    private GridArea gridArea;
    private Label statusText;
    private TextArea messageArea;
    private Button startBtn;
    private Button endBtn;
    private CompositeDisposable compositeDisposable;
    private Map<Integer, GridAreaIcon> planeIcons;
    private SimulationManager simulationManager;

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


        simulationManager.loadSimulation();
    }

    private void initComponents() {
    gridArea = new GridArea(SimulationManager.MAP_WIDTH, SimulationManager.MAP_HEIGHT);
    gridArea.setStyle("-fx-background-color: #b8b8c2;");

    startBtn = new Button("Start");
    endBtn = new Button("End");
    endBtn.setDisable(true);

    startBtn.setOnAction(event -> startSimulation());
    endBtn.setOnAction(event -> endSimulation());
    stage.setOnCloseRequest(event -> endSimulation());

    statusText = new Label("Status: Ready");
    messageArea = new TextArea();
    messageArea.setEditable(false);

    ToolBar toolbar = new ToolBar();
    toolbar.getItems().addAll(startBtn, endBtn, new Separator(), statusText);

    SplitPane splitPane = new SplitPane();
    splitPane.getItems().addAll(gridArea, messageArea);
    splitPane.setDividerPositions(0.75);

    BorderPane contentPane = new BorderPane();
    contentPane.setTop(toolbar);
    contentPane.setCenter(splitPane);

    Scene scene = new Scene(contentPane, 1200, 1000);
    stage.setTitle("Air Traffic Simulator");
    stage.setScene(scene);
    stage.show();
}


    private void startSimulation() {
        startBtn.setDisable(true);
        endBtn.setDisable(false);
        statusText.setText("Status: Running");

        simulationManager.startSimulation();
    }

    private void endSimulation() {
        endBtn.setDisable(true);
        startBtn.setDisable(false);
        statusText.setText("Status: Stopped");

        simulationManager.endSimulation();
        dispose();
    }

    private void updatePlane(Plane plane) {
        Platform.runLater(() -> {
            GridAreaIcon icon = planeIcons.get(plane.getId());
            if (icon == null) {
                icon = new GridAreaIcon(
                    plane.getXCoord(), plane.getYCoord(), plane.getDirection(), 0.1,
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

    public void updateStatistics(int planesInFlight, int planesUnderService) {
        Platform.runLater(() -> statusText.setText("In Flight: " + planesInFlight + ", Under Service: " + planesUnderService));
    }

    public void dispose() {
        if (compositeDisposable != null && !compositeDisposable.isDisposed()) {
            compositeDisposable.dispose();
        }
    }
}
