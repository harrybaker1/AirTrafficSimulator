package edu.curtin.saed.assignment1;

public interface SimulationStatistics {
    void incrementPlanesInFlight();
    void decrementPlanesInFlight();
    void incrementPlanesUnderService();
    void decrementPlanesUnderService();
    void incrementCompletedTrips();
}
