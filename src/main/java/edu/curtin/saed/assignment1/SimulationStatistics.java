/**
 * -----------------------------------------------------
 * SimulationStatistics.java
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

public interface SimulationStatistics {
    void incrementPlanesInFlight();
    void decrementPlanesInFlight();
    void incrementPlanesUnderService();
    void decrementPlanesUnderService();
    void incrementCompletedTrips();
}
