/**
 * -----------------------------------------------------
 * Airport.java
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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Airport {
    private int id;
    private double xCoord;
    private double yCoord;
    private BlockingQueue<Plane> availablePlanes;
    private BlockingQueue<Integer> flightRequests;

    public Airport(int id, double xCoord, double yCoord) {
        this.id = id;
        this.xCoord = xCoord;
        this.yCoord = yCoord;
        this.availablePlanes = new LinkedBlockingQueue<>();
        this.flightRequests = new ArrayBlockingQueue<>(SimulationManager.FLIGHT_REQUEST_QUEUE_LIMIT);
    }

    public int getId() {
        return id;
    }

    public double getXCoord() {
        return xCoord;
    }

    public double getYCoord() {
        return yCoord;
    }

    public BlockingQueue<Integer> getFlightRequestQueue() {
        return flightRequests;
    }

    public void addAvailablePlane(Plane plane) {
        availablePlanes.offer(plane);
    }

    public Plane getAvailablePlane() throws InterruptedException {
        return availablePlanes.take();
    }

    public void addFlightRequest(int destinationAirportId) {
        flightRequests.offer(destinationAirportId);
    }

    public int getFlightRequest() throws InterruptedException {
        return flightRequests.take();
    }

}
