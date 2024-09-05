package edu.curtin.saed.assignment1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import io.reactivex.rxjava3.subjects.PublishSubject;

public class PlaneServicingTask implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(PlaneServicingTask.class.getName());
    private final Plane plane;
    private SimulationStatistics stats;
    private PublishSubject<String> logSubject;

    public PlaneServicingTask(Plane plane, SimulationStatistics stats, PublishSubject<String> logSubject) {
        this.plane = plane;
        this.stats = stats;
        this.logSubject = logSubject;
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
            Thread.currentThread().interrupt();
        } finally {
            if (proc != null) {
                proc.destroy();
            }
        }
    }
}
