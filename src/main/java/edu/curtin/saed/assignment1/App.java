package edu.curtin.saed.assignment1;

import javafx.application.Application;
import javafx.stage.Stage;


@SuppressWarnings("PMD")
public class App extends Application
{
    private GUIManager guiManager;
    private SimulationManager simulationManager;
    public static void main(String[] args)
    {
        launch();
    }

    @Override
    public void start(Stage stage)
    {
        simulationManager = new SimulationManager();
        guiManager = new GUIManager(simulationManager, stage);
    }
}