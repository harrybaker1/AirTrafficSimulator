/**
 * -----------------------------------------------------
 * FlightRequestProducer.java
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
import java.util.logging.Level;
import java.util.logging.Logger;

public class FlightRequestProducer extends Thread {
    private static final Logger LOGGER = Logger.getLogger(FlightRequestHandler.class.getName());
    private final Airport airport;
    private final int numAirports;
    private Process process;

    public FlightRequestProducer(Airport airport, int numAirports) {
        this.airport = airport;
        this.numAirports = numAirports;
    }

    @Override
    public void run() {
        process = null;
        try {
            process = Runtime.getRuntime().exec(
                new String[]{"saed_flight_requests", String.valueOf(numAirports), String.valueOf(airport.getId() - 1)});
            
            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    try {
                        int destinationAirportId = Integer.parseInt(line);
                        airport.addFlightRequest(destinationAirportId + 1);
                    } catch (NumberFormatException e) {
                        LOGGER.log(Level.WARNING, () -> "Failed to parse destination airport ID: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, () -> "IOException occurred: " + e.getMessage());
        } finally {
            if (process != null) {
                process.destroy();
                try {
                    process.waitFor(); // Ensure the process terminates
                } catch (InterruptedException e) {
                    LOGGER.log(Level.WARNING, () -> "Process was interrupted: " + e.getMessage());
                    currentThread().interrupt(); // Handle the interruption
                }
            }
        }
    }

    public void endProcess() {
        if (process != null) {
            process.destroy();
            try {
                process.waitFor(); // Ensure the process terminates
            } catch (InterruptedException e) {
                LOGGER.log(Level.WARNING, () -> "Process was interrupted during endProcess: " + e.getMessage());
                currentThread().interrupt(); // Handle the interruption
            }
        }
    }
}
