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

public class Plane {
    private int id;
    private double xCoord;
    private double yCoord;
    private double speed;
    private double direction;
    private Airport currentAirport;
    private Airport destinationAirport;
    private FlightStatus flightStatus;
    private final Object statusLock = new Object();
    private final Object positionLock = new Object();

    public Plane(int id, double xCoord, double yCoord, double speed, Airport currentAirport) {
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
        synchronized (statusLock) {
            this.destinationAirport = destinationAirport;
            this.flightStatus = FlightStatus.IN_FLIGHT;
            setDirection();
        }
    }

    public void land() {
        synchronized (statusLock) {
            this.currentAirport = destinationAirport;
            this.destinationAirport = null;
            this.flightStatus = FlightStatus.UNDER_SERVICE;
            this.direction = 0.0;
        }
    }

    public void serviced() {
        synchronized (statusLock) {
            flightStatus = FlightStatus.READY;
        }
    }

    private void setDirection() {
        if (destinationAirport != null) {
            double dx = destinationAirport.getXCoord() - xCoord;
            double dy = destinationAirport.getYCoord() - yCoord;
            double angleRadians = Math.atan2(dy, dx);
            double angleDegrees = Math.toDegrees(angleRadians);
            
            if (angleDegrees < 0) {
                angleDegrees += 360;
            }

            direction = angleDegrees + 45;  //Offset 45 degrees
        }
    }

    //Reference: https://www.khanacademy.org/math/algebra-home/alg-system-of-equations/alg-equivalent-systems-of-equations/v/solving-systems-of-equations-by-elimination
    public boolean updatePosition(double deltaTime) {
        if (flightStatus != FlightStatus.IN_FLIGHT || destinationAirport == null) {
            return false;
        }

        double deltaTimeInSeconds = deltaTime / 1000.0;

        double dx = destinationAirport.getXCoord() - xCoord;
        double dy = destinationAirport.getYCoord() - yCoord;

        double distance = Math.hypot(dx, dy);

        //Close enough to location therefore return true for at location.
        if (distance <= speed * deltaTimeInSeconds) {
            xCoord = destinationAirport.getXCoord();
            yCoord = destinationAirport.getYCoord();
            return true;
        }

        double directionX = dx / distance;
        double directionY = dy / distance;

        synchronized (positionLock) {
            xCoord += directionX * speed * deltaTimeInSeconds;
            yCoord += directionY * speed * deltaTimeInSeconds;
        }

        return false;
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

    public enum FlightStatus {
        IN_FLIGHT,
        READY,
        UNDER_SERVICE
    }
}
