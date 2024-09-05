/**
 * -----------------------------------------------------
 * PlaneServicingTask.java
 * -----------------------------------------------------
 * Assignment 1
 * Software Architecture and Extensible Design - COMP3003
 * Curtin University
 * 25/08/2024
 * -----------------------------------------------------
 * Harrison Baker
 * 19514341
 * -----------------------------------------------------
 * Runnable to simulate the servicing of a plane after
 * landing. Runs a provided external process which returns
 * after a random amount of time indicating completion.
 * */

package edu.curtin.saed.assignment1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import io.reactivex.rxjava3.subjects.Subject;

public class PlaneServicingTask implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(PlaneServicingTask.class.getName());
    private final Plane plane;
    private SimulationManager simulationManager;
    private Subject<String> logSubject;

    public PlaneServicingTask(Plane plane) {
        this.plane = plane;
        this.simulationManager = SimulationManager.getInstance();
        this.logSubject = simulationManager.getLogSubject();
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

                proc.waitFor(); //Wait for servicing to finish.

                logSubject.onNext(output.toString().trim());

                plane.serviced();
                simulationManager.decrementPlanesUnderService();
                plane.getCurrentAirport().addAvailablePlane(plane);
            }

        } catch (IOException e) {
            LOGGER.log(Level.WARNING, () -> "PlaneServicingTask IOExecption");
            Thread.currentThread().interrupt();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (proc != null) {
                proc.destroy();
            }
        }
    }
}
