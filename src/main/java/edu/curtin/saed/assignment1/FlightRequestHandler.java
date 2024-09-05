/**
 * -----------------------------------------------------
 * FlightRequestHandler.java
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

import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import io.reactivex.rxjava3.subjects.PublishSubject;

public class FlightRequestHandler extends Thread {
    private Airport airport;
    private Map<Integer, Airport> allAirports;
    private ThreadPoolExecutor planeTaskThreadPool;
    private PublishSubject<Plane> planeSubject;
    private PublishSubject<String> logSubject;
    private SimulationStatistics stats;

    public FlightRequestHandler(Airport airport, Map<Integer, Airport> allAirports, ThreadPoolExecutor planeTaskThreadPool, PublishSubject<Plane> planeSubject, PublishSubject<String> logSubject, SimulationStatistics stats) {
        this.airport = airport;
        this.allAirports = allAirports;
        this.planeTaskThreadPool = planeTaskThreadPool;
        this.planeSubject = planeSubject;
        this.logSubject = logSubject;
        this.stats = stats;
    }

    @Override
    public void run() {
        try {
            while (!currentThread().isInterrupted()) {
                int destinationAirportId = airport.getFlightRequest();
                Airport destinationAirport = getAirportById(destinationAirportId);
                Plane plane = airport.getAvailablePlane();
                PlaneFlyingTask movementTask = new PlaneFlyingTask(plane, destinationAirport, stats, planeSubject, logSubject, planeTaskThreadPool);
                if (!planeTaskThreadPool.isShutdown()) {
                    planeTaskThreadPool.execute(movementTask);
                }
            }
        } catch (InterruptedException e) {
            currentThread().interrupt();
        }
    }

    private Airport getAirportById(int airportId) {
        return allAirports.getOrDefault(airportId, null);
    }
}