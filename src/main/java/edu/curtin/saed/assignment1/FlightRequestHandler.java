package edu.curtin.saed.assignment1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

import edu.curtin.saed.assignment1.Plane.FlightStatus;
import io.reactivex.rxjava3.subjects.PublishSubject;

@SuppressWarnings("PMD")
public class FlightRequestHandler extends Thread {
    private Airport airport;
    private Map<Integer, Airport> allAirports;
    private ThreadPoolExecutor planeTaskThreadPool;
    private PublishSubject<Plane> planeSubject;
    private PublishSubject<String> logSubject;

    public FlightRequestHandler(Airport airport, Map<Integer, Airport> allAirports, ThreadPoolExecutor planeTaskThreadPool, PublishSubject<Plane> planeSubject, PublishSubject<String> logSubject) {
        this.airport = airport;
        this.allAirports = allAirports;
        this.planeTaskThreadPool = planeTaskThreadPool;
        this.planeSubject = planeSubject;
        this.logSubject = logSubject;
    }

    @Override
    public void run() {
        try {
            while (true) {
                int destinationAirportId = airport.getFlightRequest();
                Airport destinationAirport = getAirportById(destinationAirportId);
                Plane plane = airport.getAvailablePlane();
                PlaneFlyingTask movementTask = new PlaneFlyingTask(plane, destinationAirport, planeTaskThreadPool);
                planeTaskThreadPool.execute(movementTask);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Airport getAirportById(int airportId) {
        for (Airport a : allAirports.values()) {
            if (a.getId() == airportId) {
                return a;
            }
        }
        return null;
    }

    private class PlaneFlyingTask implements Runnable {
        private static final long PLANE_UPDATE_TIME_MS = 25; //Increase if performance issues
        private Plane plane;
        private Airport destinationAirport;
        private ThreadPoolExecutor planeTaskThreadPool;

        public PlaneFlyingTask(Plane plane, Airport destinationAirport, ThreadPoolExecutor planeTaskThreadPool) {
            this.plane = plane;
            this.destinationAirport = destinationAirport;
            this.planeTaskThreadPool = planeTaskThreadPool;
        }

        @Override
        public void run() {
            plane.depart(destinationAirport);
            logSubject.onNext("Plane " + plane.getId() + " departing Airport " + plane.getCurrentAirport().getId() + ".");

            long previousUpdateTime = System.currentTimeMillis();

            while (plane.getFlightStatus() == FlightStatus.IN_FLIGHT) {
                long currentTime = System.currentTimeMillis();
                long deltaTime = currentTime - previousUpdateTime;
                previousUpdateTime = currentTime;

                plane.updatePosition(deltaTime);

                planeSubject.onNext(plane);
                
                try {
                    Thread.sleep(PLANE_UPDATE_TIME_MS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }

            logSubject.onNext("Plane " + plane.getId() + " arrived at Airport " + plane.getCurrentAirport().getId() + ".");

            PlaneServicingTask serviceTask = new PlaneServicingTask(plane);
            planeTaskThreadPool.execute(serviceTask);
        }
    }


    private class PlaneServicingTask implements Runnable {
        private Plane plane;

        public PlaneServicingTask(Plane plane) {
            this.plane = plane;
        }

        @Override
        public void run() {
            try {
                Process proc = Runtime.getRuntime().exec(
                    new String[]{"comms/bin/saed_plane_service", String.valueOf(plane.getCurrentAirport().getId()), String.valueOf(plane.getId())}
                );

                BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
                String line;
                StringBuilder output = new StringBuilder();

                while ((line = br.readLine()) != null) {
                    output.append(line).append("\n");
                }

                proc.waitFor();

                plane.setFlightStatus(FlightStatus.READY);
                plane.getCurrentAirport().addAvailablePlane(plane);

            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}