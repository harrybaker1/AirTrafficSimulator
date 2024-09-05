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

 import java.util.Map;
 
 public class FlightRequestHandler extends Thread {
     private Airport airport;
     private Map<Integer, Airport> allAirports;
     private SimulationManager simulationManager;
 
     public FlightRequestHandler(Airport airport, Map<Integer, Airport> allAirports) {
         this.airport = airport;
         this.allAirports = allAirports;
         this.simulationManager = SimulationManager.getInstance();
     }
 
     @Override
     public void run() {
        try {
            while (true) {
                int destinationAirportId = airport.getFlightRequest();
                Airport destinationAirport = getAirportById(destinationAirportId);
                Plane plane = airport.getAvailablePlane();
    
                PlaneFlyingTask movementTask = new PlaneFlyingTask(plane, destinationAirport);
    
                if (!simulationManager.getPlaneTaskThreadPool().isShutdown()) {
                    simulationManager.getPlaneTaskThreadPool().execute(movementTask);
                }
            }
        } catch (InterruptedException e) {
            currentThread().interrupt();
        }
    }
 
     private Airport getAirportById(int airportId) {
         return allAirports.getOrDefault(airportId, null);
     }
 }
 