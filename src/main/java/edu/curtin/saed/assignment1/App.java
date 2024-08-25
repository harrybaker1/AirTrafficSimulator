/**
 * -----------------------------------------------------
 * App.java
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

import javafx.application.Application;
import javafx.stage.Stage;

public class App extends Application
{
    public static void main(String[] args)
    {
        launch();
    }

    @Override
    public void start(Stage stage)
    {
        new GUIManager(new SimulationManager(), stage);
    }
}