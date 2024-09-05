package edu.curtin.saed.assignment1;

import java.util.concurrent.ThreadPoolExecutor;

import io.reactivex.rxjava3.subjects.PublishSubject;

public class PlaneFlyingTask implements Runnable {
    private static final long PLANE_UPDATE_INTERVAL_MS = 50;
    private Plane plane;
    private Airport destinationAirport;
    private SimulationStatistics stats;
    private PublishSubject<Plane> planeSubject;
    private PublishSubject<String> logSubject;
    private ThreadPoolExecutor planeTaskThreadPool;

    public PlaneFlyingTask(Plane plane, Airport destinationAirport, SimulationStatistics stats, PublishSubject<Plane> planeSubject, PublishSubject<String> logSubject, ThreadPoolExecutor planeTaskThreadPool) {
        this.plane = plane;
        this.destinationAirport = destinationAirport;
        this.stats = stats;
        this.planeSubject = planeSubject;
        this.logSubject = logSubject;
        this.planeTaskThreadPool = planeTaskThreadPool;
    }

    @Override
    public void run() {
        try {
            plane.takeOff(destinationAirport);
            stats.incrementPlanesInFlight();
            logSubject.onNext("Plane " + plane.getId() + " departing Airport " + plane.getCurrentAirport().getId() + ".");

            while (!Thread.currentThread().isInterrupted()) {
                long deltaTime = PLANE_UPDATE_INTERVAL_MS;
                boolean atDestination = plane.updatePosition(deltaTime);
                planeSubject.onNext(plane);

                if (atDestination || plane.getFlightStatus() != Plane.FlightStatus.IN_FLIGHT) {
                    logSubject.onNext("Plane " + plane.getId() + " arrived at Airport " + plane.getDestinationAirport().getId() + ".");
                    plane.land();
                    stats.decrementPlanesInFlight();
                    stats.incrementCompletedTrips();
                    stats.incrementPlanesUnderService();

                    PlaneServicingTask serviceTask = new PlaneServicingTask(plane, stats, logSubject);
                    if (!planeTaskThreadPool.isShutdown()) {
                        planeTaskThreadPool.execute(serviceTask);
                    }
                    break;
                }

                Thread.sleep(PLANE_UPDATE_INTERVAL_MS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}