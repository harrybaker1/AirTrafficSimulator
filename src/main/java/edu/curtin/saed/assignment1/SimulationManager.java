/**
 * -----------------------------------------------------
 * SimulationManager.java
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

import edu.curtin.saed.assignment1.Plane.FlightStatus;
import io.reactivex.rxjava3.subjects.PublishSubject;

public class SimulationManager implements SimulationStatistics{
    public static final int MAP_WIDTH = 10;
    public static final int MAP_HEIGHT = 10;
    private static final double MIN_DIST_BETWEEN_AIPORTS = 1.5;
    public static final int FLIGHT_REQUEST_QUEUE_LIMIT = 50;
    private Map<Integer, Airport> airports;
    private Map<Integer, Plane> planes;
    private int numAirports;
    private int numPlanesPerAirport;
    private ThreadPoolExecutor planeTaskThreadPool;
    private List<FlightRequestProducer> flightRequestProducerThreads;
    private List<FlightRequestHandler> flightRequestHandlerThreads;
    private PublishSubject<Plane> planeSubject;
    private PublishSubject<Map<Integer, Airport>> airportListSubject;
    private PublishSubject<String> logSubject;
    private PublishSubject<Map<String, Integer>> statsSubject;
    private boolean isRunning;
    private final AtomicInteger planesInFlight = new AtomicInteger(0);
    private final AtomicInteger planesUnderService = new AtomicInteger(0);
    private final AtomicInteger completedTrips = new AtomicInteger(0);

    public SimulationManager() {
        this.isRunning = false;
        this.flightRequestProducerThreads = new ArrayList<>();
        this.flightRequestHandlerThreads = new ArrayList<>();
        this.planeSubject = PublishSubject.create();
        this.airportListSubject = PublishSubject.create();
        this.logSubject = PublishSubject.create();
        this.statsSubject = PublishSubject.create();
    }

    public void loadSimulation(int numAirports, int numPlanesPerAirport, double planeSpeed) {
        reset();
        this.numAirports = numAirports;
        this.numPlanesPerAirport = numPlanesPerAirport;
        this.planeTaskThreadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(numAirports * numPlanesPerAirport);
        initAirports();
        initPlanes(planeSpeed);
        initFlightRequestThreads();
    }

    private void initAirports() {
        airports = new ConcurrentHashMap<>();
        int gridSize = (int) Math.ceil(Math.sqrt(numAirports));
        double cellWidth = MAP_WIDTH / gridSize;
        double cellHeight = MAP_HEIGHT / gridSize;
    
        for (int i = 1; i <= numAirports; i++) {
            double xCoord, yCoord;
            boolean validPosition;
    
            do {
                // Choose a random cell
                int cellX = (int) (Math.random() * gridSize);
                int cellY = (int) (Math.random() * gridSize);
    
                // Generate random position within the chosen cell
                xCoord = Math.min(cellX * cellWidth + Math.random() * cellWidth, 9.0);
                yCoord = Math.min(cellY * cellHeight + Math.random() * cellHeight, 9.0);
    
                validPosition = true;
    
                // Check if this position is far enough from existing airports
                for (Airport existingAirport : airports.values()) {
                    double distance = Math.hypot(xCoord - existingAirport.getXCoord(), yCoord - existingAirport.getYCoord());
                    if (distance < MIN_DIST_BETWEEN_AIPORTS) {
                        validPosition = false;
                        break;
                    }
                }
            } while (!validPosition);
    
            airports.put(i, new Airport(i, xCoord, yCoord));
        }
        airportListSubject.onNext(airports);
    }

    private void initPlanes(double planeSpeed) {
        planes = new ConcurrentHashMap<>();
        int planeId = 1;
    
        for (Airport airport : airports.values()) {
            for (int i = 0; i < numPlanesPerAirport; i++) {
                Plane plane = new Plane(planeId++, airport.getXCoord(), airport.getYCoord(), planeSpeed, airport, FlightStatus.READY);
                planes.put(plane.getId(), plane);
                airport.addAvailablePlane(plane);
                planeSubject.onNext(plane);
            }
        }
    }

    private void initFlightRequestThreads() {
        for (Airport airport : airports.values()) {
            FlightRequestProducer flightRequestProducer = new FlightRequestProducer(airport, numAirports);
            FlightRequestHandler flightRequestHandler = new FlightRequestHandler(airport, airports, planeTaskThreadPool, planeSubject, logSubject, this);
    
            flightRequestProducerThreads.add(flightRequestProducer);
            flightRequestHandlerThreads.add(flightRequestHandler);
        }
    }

    public void startSimulation() {
        if (!isRunning) {
            isRunning = true;

            for (Thread thread : flightRequestHandlerThreads) {
                thread.start();
            }
            for (Thread thread : flightRequestProducerThreads) {
                thread.start();
            }
        }
    }
    
    public void endSimulation() {
        if (isRunning) {
            isRunning = false;

            for (FlightRequestProducer producerThread : flightRequestProducerThreads) {
                producerThread.interrupt();
            }
            for (FlightRequestHandler handlerThread : flightRequestHandlerThreads) {
                handlerThread.interrupt();
            }
            
            planeTaskThreadPool.shutdownNow();
        }
    }

    public void reset() {
        if (planeTaskThreadPool != null && !planeTaskThreadPool.isShutdown()) {
            planeTaskThreadPool.shutdownNow();
        }
        
        if (flightRequestProducerThreads != null) {
            flightRequestProducerThreads.forEach(thread -> {
                if (thread.isAlive()) {
                    thread.interrupt();
                }
            });
        }
        
        if (flightRequestHandlerThreads != null) {
            flightRequestHandlerThreads.forEach(thread -> {
                if (thread.isAlive()) {
                    thread.interrupt();
                }
            });
        }
    
        if (airports != null) {
            airports.clear();
        }
        
        if (planes != null) {
            planes.clear();
        }
        
        if (flightRequestProducerThreads != null) {
            flightRequestProducerThreads.clear();
        }
        
        if (flightRequestHandlerThreads != null) {
            flightRequestHandlerThreads.clear();
        }
    
        planesInFlight.set(0);
        planesUnderService.set(0);
        completedTrips.set(0);
    
        isRunning = false;
    }
    
    
    public PublishSubject<Plane> getPlaneSubject() {
        return planeSubject;
    }
    
    public PublishSubject<Map<Integer, Airport>> getAirportListSubject() {
        return airportListSubject;
    }

    public PublishSubject<String> getLogSubject() {
        return logSubject;
    }

    public PublishSubject<Map<String, Integer>> getStatsSubject() {
        return statsSubject;
    }

    private void emitStats() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("planesInFlight", planesInFlight.get());
        stats.put("planesUnderService", planesUnderService.get());
        stats.put("completedTrips", completedTrips.get());
        statsSubject.onNext(stats);
    }

    @Override
    public void incrementPlanesInFlight() {
        planesInFlight.incrementAndGet();
        emitStats();
    }

    @Override
    public void decrementPlanesInFlight() {
        planesInFlight.decrementAndGet();
        emitStats();
    }

    @Override
    public void incrementPlanesUnderService() {
        planesUnderService.incrementAndGet();
        emitStats();
    }

    @Override
    public void decrementPlanesUnderService() {
        planesUnderService.decrementAndGet();
        emitStats();
    }

    @Override
    public void incrementCompletedTrips() {
        completedTrips.incrementAndGet();
        emitStats();
    }
}