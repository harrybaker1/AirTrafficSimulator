package edu.curtin.saed.assignment1;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import edu.curtin.saed.assignment1.Plane.FlightStatus;
import io.reactivex.rxjava3.subjects.PublishSubject;

@SuppressWarnings("PMD")
public class SimulationManager {
    public static final int MAP_WIDTH = 10;
    public static final int MAP_HEIGHT = 10;
    public static final int NUM_AIRPORTS = 10;
    public static final int NUM_PLANES_PER_AIRPORT = 2;
    private static final double MIN_DIST_BETWEEN_AIPORTS = 1.5;
    public static final int FLIGHT_REQUEST_QUEUE_LIMIT = 50;
    private ConcurrentHashMap<Integer, Airport> airports;
    private ConcurrentHashMap<Integer, Plane> planes;
    private ThreadPoolExecutor planeTaskThreadPool;
    private List<FlightRequestProducer> flightRequestProducerThreads;
    private List<FlightRequestHandler> flightRequestHandlerThreads;
    private PublishSubject<Plane> planeSubject;
    private PublishSubject<Map<Integer, Airport>> airportListSubject;
    private PublishSubject<String> logSubject;
    private int planesInFlight;
    private int planesUnderService;
    private int completedTrips;
    private boolean isRunning;


    public SimulationManager() {
        this.isRunning = false;
        this.planeTaskThreadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool((NUM_PLANES_PER_AIRPORT * NUM_AIRPORTS) / 2);
        flightRequestProducerThreads = new ArrayList<>();
        flightRequestHandlerThreads = new ArrayList<>();
        this.planeSubject = PublishSubject.create();
        this.airportListSubject = PublishSubject.create();
        this.logSubject = PublishSubject.create();
        planesInFlight = 0;
        planesUnderService = 0;
        completedTrips = 0;
        initAirports();
        initPlanes();
        initFlightRequestThreads();
    }

    public void loadSimulation() {
        airportListSubject.onNext(airports);
        for(Plane plane : planes.values()) {
            planeSubject.onNext(plane);   
        }
    }

    private void initAirports() {
        airports = new ConcurrentHashMap<>();
        int gridSize = (int) Math.ceil(Math.sqrt(NUM_AIRPORTS));
        double cellWidth = MAP_WIDTH / gridSize;
        double cellHeight = MAP_HEIGHT / gridSize;
    
        for (int i = 1; i <= NUM_AIRPORTS; i++) {
            double xCoord, yCoord;
            boolean validPosition;
    
            do {
                // Choose a random cell
                int cellX = (int) (Math.random() * gridSize);
                int cellY = (int) (Math.random() * gridSize);
    
                // Generate random position within the chosen cell
                xCoord = cellX * cellWidth + Math.random() * cellWidth;
                yCoord = cellY * cellHeight + Math.random() * cellHeight;
    
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
    }

    private void initPlanes() {
        planes = new ConcurrentHashMap<>();
        int planeId = 1;
    
        for (Airport airport : airports.values()) {
            for (int i = 0; i < NUM_PLANES_PER_AIRPORT; i++) {
                Plane plane = new Plane(planeId++, airport.getXCoord(), airport.getYCoord(), airport, FlightStatus.READY);
                planes.put(plane.getId(), plane);
                airport.addAvailablePlane(plane);
            }
        }
    }

    private void initFlightRequestThreads() {
        for (Airport airport : airports.values()) {
            FlightRequestProducer flightRequestProducer = new FlightRequestProducer(airport);
            FlightRequestHandler flightRequestHandler = new FlightRequestHandler(airport, airports, planeTaskThreadPool, planeSubject, logSubject);
    
            flightRequestProducerThreads.add(flightRequestProducer);
            flightRequestHandlerThreads.add(flightRequestHandler);
        }
    }

    public void startSimulation() {
        if (!isRunning) {
            isRunning = true;

            for (Thread thread : flightRequestProducerThreads) {
                thread.start();
            }
            for (Thread thread : flightRequestHandlerThreads) {
                thread.start();
            }
        }
    }
    
    public void endSimulation() {
        if (isRunning) {
            isRunning = false;

            for (FlightRequestProducer producerThread : flightRequestProducerThreads) {
                producerThread.endProcess();
                producerThread.interrupt();
            }
            for (FlightRequestHandler handlerThread : flightRequestHandlerThreads) {
                handlerThread.interrupt();
            }
            
            planeTaskThreadPool.shutdownNow();
        }
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
}