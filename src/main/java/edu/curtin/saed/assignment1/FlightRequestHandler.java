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

    public FlightRequestHandler(Airport airport, Map<Integer, Airport> allAirports, ThreadPoolExecutor planeTaskThreadPool, PublishSubject<Plane> planeSubject) {
        this.airport = airport;
        this.allAirports = allAirports;
        this.planeTaskThreadPool = planeTaskThreadPool;
        this.planeSubject = planeSubject;
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
        private static final long PLANE_UPDATE_TIME_MS = 100;
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

            // After the flight ends, start the plane servicing task
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
                // Get the airport ID and plane ID
                int airportId = plane.getCurrentAirport().getId();
                int planeId = plane.getId();

                // Start the saed_plane_service process with the airport ID and plane ID as arguments
                Process proc = Runtime.getRuntime().exec(
                    new String[]{"comms/bin/saed_plane_service", String.valueOf(airportId), String.valueOf(planeId)}
                );

                // Capture the output of the process
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