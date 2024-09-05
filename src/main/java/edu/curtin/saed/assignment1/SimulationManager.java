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
 * Core simulation manager.
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
import io.reactivex.rxjava3.subjects.Subject;

public class SimulationManager {
    private static SimulationManager instance;

    public static final int MAP_WIDTH = 10;
    public static final int MAP_HEIGHT = 10;
    public static final int FLIGHT_REQUEST_QUEUE_LIMIT = 50;
    private static final double MIN_DIST_BETWEEN_AIPORTS = 1.5;
    private Map<Integer, Airport> airports;
    private Map<Integer, Plane> planes;
    private int numAirports;
    private int numPlanesPerAirport;
    private ThreadPoolExecutor planeTaskThreadPool;
    private List<FlightRequestProducer> flightRequestProducerThreads;
    private List<FlightRequestHandler> flightRequestHandlerThreads;
    private Subject<Plane> planeSubject;
    private Subject<Map<Integer, Airport>> airportListSubject; 
    private Subject<String> logSubject;
    private Subject<Map<String, Integer>> statsSubject;
    private boolean isRunning;
    //AtomicIntegers for thread safety.
    private final AtomicInteger planesInFlight = new AtomicInteger(0);
    private final AtomicInteger planesUnderService = new AtomicInteger(0);
    private final AtomicInteger completedTrips = new AtomicInteger(0);

    private SimulationManager() {
        this.isRunning = false;
        this.flightRequestProducerThreads = new ArrayList<>();
        this.flightRequestHandlerThreads = new ArrayList<>();
        //Create serialized subjects for thread safety.
        this.planeSubject = PublishSubject.<Plane>create().toSerialized();
        this.airportListSubject = PublishSubject.<Map<Integer, Airport>>create().toSerialized();
        this.logSubject = PublishSubject.<String>create().toSerialized();
        this.statsSubject = PublishSubject.<Map<String, Integer>>create().toSerialized();
    }

    //Double-Checked Locking
    public static SimulationManager getInstance() {
        if (instance == null) {
            synchronized (SimulationManager.class) {
                if (instance == null) {
                    instance = new SimulationManager();
                }
            }
        }
        return instance;
    }

    //Reset and load a new simulation.
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
                //Choose a random cell.
                int cellX = (int) (Math.random() * gridSize);
                int cellY = (int) (Math.random() * gridSize);
    
                //Generate random position in the cell.
                xCoord = cellX * cellWidth + Math.random() * (Math.min(cellWidth, MAP_WIDTH - cellX * cellWidth));
                yCoord = cellY * cellHeight + Math.random() * (Math.min(cellHeight, MAP_HEIGHT - cellY * cellHeight));

    
                validPosition = true;
    
                //Check far enough from existing airports
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
        int planeId = 1; //Starting id.
    
        for (Airport airport : airports.values()) {
            for (int i = 0; i < numPlanesPerAirport; i++) {
                Plane plane = new Plane(planeId++, airport.getXCoord(), airport.getYCoord(), planeSpeed, airport, FlightStatus.READY);
                planes.put(plane.getId(), plane);
                airport.addAvailablePlane(plane);
                planeSubject.onNext(plane);
            }
        }
    }

    //Create FlightRequestProducer & FlightRequestHandler threads for each airport.
    private void initFlightRequestThreads() {
        for (Airport airport : airports.values()) {
            FlightRequestProducer flightRequestProducer = new FlightRequestProducer(airport, numAirports);
            FlightRequestHandler flightRequestHandler = new FlightRequestHandler(airport, airports);
    
            flightRequestProducerThreads.add(flightRequestProducer);
            flightRequestHandlerThreads.add(flightRequestHandler);
        }
    }

    //Start FlightRequestProducer & FlightRequestHandler threads for each airport.
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
    
    //End FlightRequestProducer & FlightRequestHandler threads for each airport and shutdown thread pool.
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

    //Reset all simulation values, interrupt all threads and shutdown thread pool.
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
    
    public ThreadPoolExecutor getPlaneTaskThreadPool() {
        return planeTaskThreadPool;
    }

    public Subject<Plane> getPlaneSubject() {
        return planeSubject;
    }
    
    public Subject<Map<Integer, Airport>> getAirportListSubject() {
        return airportListSubject;
    }

    public Subject<String> getLogSubject() {
        return logSubject;
    }

    public Subject<Map<String, Integer>> getStatsSubject() {
        return statsSubject;
    }

    //Start a service task for a plane.
    public void handleServicing(Plane plane) {
        PlaneServicingTask servicingTask = new PlaneServicingTask(plane);
        if (!planeTaskThreadPool.isShutdown()) {
            planeTaskThreadPool.execute(servicingTask);
        }
    }

    private void emitStats() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("planesInFlight", planesInFlight.get());
        stats.put("planesUnderService", planesUnderService.get());
        stats.put("completedTrips", completedTrips.get());
        statsSubject.onNext(stats);
    }

    public void incrementPlanesInFlight() {
        planesInFlight.incrementAndGet();
        emitStats();
    }

    public void decrementPlanesInFlight() {
        planesInFlight.decrementAndGet();
        emitStats();
    }

    public void incrementPlanesUnderService() {
        planesUnderService.incrementAndGet();
        emitStats();
    }

    public void decrementPlanesUnderService() {
        planesUnderService.decrementAndGet();
        emitStats();
    }

    public void incrementCompletedTrips() {
        completedTrips.incrementAndGet();
        emitStats();
    }
}