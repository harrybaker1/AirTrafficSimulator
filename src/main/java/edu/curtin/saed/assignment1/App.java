package edu.curtin.saed.assignment1;

import javafx.application.Application;
import javafx.stage.Stage;


@SuppressWarnings("PMD")
public class App extends Application
{
    public static void main(String[] args)
    {
        launch();
    }

    @Override
    public void start(Stage stage)
    {
        SimulationManager simulationManager = new SimulationManager();
        GUIManager guiManager = new GUIManager(simulationManager, stage);
    }
}