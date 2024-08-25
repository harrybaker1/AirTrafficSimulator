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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.logging.Level;

import edu.curtin.saed.assignment1.Plane.FlightStatus;
import io.reactivex.rxjava3.subjects.PublishSubject;
public class FlightRequestHandler extends Thread {
    private static final Logger LOGGER = Logger.getLogger(FlightRequestHandler.class.getName());
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
                PlaneFlyingTask movementTask = new PlaneFlyingTask(plane, destinationAirport);
                if (!planeTaskThreadPool.isShutdown()) {
                    planeTaskThreadPool.execute(movementTask);
                }
            }
        } catch (InterruptedException e) {
            LOGGER.log(Level.WARNING, () -> e.getMessage());
            currentThread().interrupt();
        }
    }

    private Airport getAirportById(int airportId) {
        return allAirports.getOrDefault(airportId, null);
    }

    private class PlaneFlyingTask implements Runnable {
        private static final long PLANE_UPDATE_INTERVAL_MS = 50;
        private Plane plane;
        private Airport destinationAirport;
        private ScheduledExecutorService scheduler;

        public PlaneFlyingTask(Plane plane, Airport destinationAirport) {
            this.plane = plane;
            this.destinationAirport = destinationAirport;
            this.scheduler = Executors.newSingleThreadScheduledExecutor();
        }

        @Override
        public void run() {
            plane.takeOff(destinationAirport);
            stats.incrementPlanesInFlight();
            logSubject.onNext("Plane " + plane.getId() + " departing Airport " + plane.getCurrentAirport().getId() + ".");

            scheduler.scheduleAtFixedRate(() -> {
                long deltaTime = PLANE_UPDATE_INTERVAL_MS;
                boolean atDestination = plane.updatePosition(deltaTime);
                planeSubject.onNext(plane);

                if (atDestination || plane.getFlightStatus() != FlightStatus.IN_FLIGHT) {
                    logSubject.onNext("Plane " + plane.getId() + " arrived at Airport " + plane.getDestinationAirport().getId() + ".");
                    plane.land();
                    stats.decrementPlanesInFlight();
                    stats.incrementCompletedTrips();
                    stats.incrementPlanesUnderService();

                    PlaneServicingTask serviceTask = new PlaneServicingTask(plane);
                    if (!planeTaskThreadPool.isShutdown()) {
                        planeTaskThreadPool.execute(serviceTask);
                    }

                    scheduler.shutdown();
                }
            }, 0, PLANE_UPDATE_INTERVAL_MS, TimeUnit.MILLISECONDS);
        }
    }

    private class PlaneServicingTask implements Runnable {
        private final Plane plane;

        public PlaneServicingTask(Plane plane) {
            this.plane = plane;
        }

        @Override
        public void run() {
            Process proc = null;

            try {
                proc = Runtime.getRuntime().exec(
                    new String[]{"comms/bin/saed_plane_service", String.valueOf(plane.getCurrentAirport().getId()), String.valueOf(plane.getId())}
                );

                try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                    StringBuilder output = new StringBuilder();
                    String line;

                    while ((line = bufferedReader.readLine()) != null) {
                        output.append(line).append("\n");
                    }

                    proc.waitFor();

                    logSubject.onNext(output.toString().trim());

                    plane.serviced();
                    stats.decrementPlanesUnderService();
                    plane.getCurrentAirport().addAvailablePlane(plane);
                }

            } catch (IOException e) {
                LOGGER.log(Level.WARNING, () -> e.getMessage());
            } catch (InterruptedException e) {
                LOGGER.log(Level.WARNING, () -> e.getMessage());
                currentThread().interrupt();
            } finally {
                if (proc != null) {
                    proc.destroy();
                }
            }
        }
    }
}
