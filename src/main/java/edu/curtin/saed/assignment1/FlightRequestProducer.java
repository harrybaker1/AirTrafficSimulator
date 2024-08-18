package edu.curtin.saed.assignment1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@SuppressWarnings("PMD")
public class FlightRequestProducer extends Thread {
    private Airport airport;
    private Process process;
    
    public FlightRequestProducer(Airport airport) {
        this.airport = airport;
    }

    @Override
    public void run() {
        process = null;
        try {
            process = Runtime.getRuntime().exec(
                new String[]{"saed_flight_requests", String.valueOf(SimulationManager.NUM_AIRPORTS), String.valueOf(airport.getId())});
            
            BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                try {
                    int destinationAirportId = Integer.parseInt(line);
                    airport.addFlightRequest(destinationAirportId);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    public void endProcess() {
        process.destroy();
    }
}
