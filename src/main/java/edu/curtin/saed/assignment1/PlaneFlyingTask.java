/**
 * -----------------------------------------------------
 * PlaneFlyingTask.java
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

import io.reactivex.rxjava3.subjects.Subject;

public class PlaneFlyingTask implements Runnable {
    private static final long PLANE_UPDATE_INTERVAL_MS = 50;
    private Plane plane;
    private Airport destinationAirport;
    private SimulationManager simulationManager;
    private Subject<Plane> planeSubject;
    private Subject<String> logSubject;

    public PlaneFlyingTask(Plane plane, Airport destinationAirport) {
        this.plane = plane;
        this.destinationAirport = destinationAirport;
        this.simulationManager = SimulationManager.getInstance();
        this.planeSubject = simulationManager.getPlaneSubject();
        this.logSubject = simulationManager.getLogSubject();
    }

    @Override
    public void run() {
        try {
            plane.takeOff(destinationAirport);
            simulationManager.incrementPlanesInFlight();

            logSubject.onNext("Plane " + plane.getId() + " departing Airport " + plane.getCurrentAirport().getId() + ".");

            while (!Thread.currentThread().isInterrupted()) {
                long deltaTime = PLANE_UPDATE_INTERVAL_MS;
                boolean atDestination = plane.updatePosition(deltaTime);
                planeSubject.onNext(plane);

                if (atDestination || plane.getFlightStatus() != Plane.FlightStatus.IN_FLIGHT) {
                    logSubject.onNext("Plane " + plane.getId() + " arrived at Airport " + plane.getDestinationAirport().getId() + ".");
                    
                    plane.land();
                    simulationManager.decrementPlanesInFlight();
                    simulationManager.incrementCompletedTrips();
                    simulationManager.incrementPlanesUnderService();
                    simulationManager.handlePlaneLanding(plane);

                    break;
                }

                Thread.sleep(PLANE_UPDATE_INTERVAL_MS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}