/**
 * -----------------------------------------------------
 * Plane.java
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

@SuppressWarnings("PMD")
public class Plane {
    private int id;
    private double xCoord;
    private double yCoord;
    private double speed;
    private double direction;
    private Airport currentAirport;
    private Airport destinationAirport;
    private FlightStatus flightStatus;
    private final Object lock = new Object();

    public Plane(int id, double xCoord, double yCoord, double speed, Airport currentAirport, FlightStatus flightStatus) {
        this.id = id;
        this.xCoord = xCoord;
        this.yCoord = yCoord;
        this.speed = speed;
        this.direction = 0.0;
        this.currentAirport = currentAirport;
        this.destinationAirport = null;
        this.flightStatus = FlightStatus.READY;
    }

    public void takeOff(Airport destinationAirport) {
        synchronized (lock) {
            this.destinationAirport = destinationAirport;
            this.flightStatus = FlightStatus.IN_FLIGHT;
            calculateDirection();
        }
    }

    public void land() {
        synchronized (lock) {
            this.currentAirport = destinationAirport;
            this.destinationAirport = null;
            this.flightStatus = FlightStatus.UNDER_SERVICE;
            this.direction = 0.0;
        }
    }

    public void serviced() {
        synchronized (lock) {
            flightStatus = FlightStatus.READY;
        }
    }

    private void calculateDirection() {
        if (destinationAirport != null) {
            double dx = destinationAirport.getXCoord() - xCoord;
            double dy = destinationAirport.getYCoord() - yCoord;
            double angleRadians = Math.atan2(dy, dx);
            double angleDegrees = Math.toDegrees(angleRadians);
            
            if (angleDegrees < 0) {
                angleDegrees += 360;
            }

            direction = angleDegrees + 45;  // Adjusting with an offset of 45 degrees if needed
        }
    }

    public boolean updatePosition(double deltaTime) {
        synchronized (lock) {
            if (flightStatus != FlightStatus.IN_FLIGHT || destinationAirport == null) {
                return false;
            }

            double deltaTimeInSeconds = deltaTime / 1000.0;

            double dx = destinationAirport.getXCoord() - xCoord;
            double dy = destinationAirport.getYCoord() - yCoord;

            double distance = Math.hypot(dx, dy);

            if (distance <= speed * deltaTimeInSeconds) {
                xCoord = destinationAirport.getXCoord();
                yCoord = destinationAirport.getYCoord();
                return true;
            }

            double directionX = dx / distance;
            double directionY = dy / distance;

            xCoord += directionX * speed * deltaTimeInSeconds;
            yCoord += directionY * speed * deltaTimeInSeconds;

            return false;
        }
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

    public double getDirection() {
        return direction;
    }

    public Airport getCurrentAirport() {
        return currentAirport;
    }

    public Airport getDestinationAirport() {
        return destinationAirport;
    }

    public FlightStatus getFlightStatus() {
        return flightStatus;
    }

    enum FlightStatus {
        IN_FLIGHT,
        READY,
        UNDER_SERVICE
    }
}
