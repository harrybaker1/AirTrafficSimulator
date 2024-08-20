package edu.curtin.saed.assignment1;


@SuppressWarnings("PMD")
public class Plane {
    private static double SPEED = 0.5;
    private int id;
    private double xCoord;
    private double yCoord;
    private double direction;
    private Airport currentAirport;
    private Airport destinationAirport;
    private FlightStatus flightStatus;
    private final Object lock = new Object();
    
    public Plane(int id, double xCoord, double yCoord, Airport currentAirport, FlightStatus flightStatus) {
        this.id = id;
        this.xCoord = xCoord;
        this.yCoord = yCoord;
        this.direction = 0.0;
        this.currentAirport = currentAirport;
        this.destinationAirport = null;
        this.flightStatus = FlightStatus.READY;
    }

    public void takeOff(Airport destinationAirport) {
        synchronized (lock) {
            this.destinationAirport = destinationAirport;
            this.flightStatus = FlightStatus.IN_FLIGHT;
        }
    }

    public void land() {
        synchronized (lock) {
            this.currentAirport = destinationAirport;
            this.destinationAirport = null;
            this.flightStatus = FlightStatus.UNDER_SERVICE;
        }
    }

    public void serviced() {
        synchronized (lock) {
            flightStatus = FlightStatus.READY;  
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
    
            if (distance <= SPEED * deltaTimeInSeconds) {
                xCoord = destinationAirport.getXCoord();
                yCoord = destinationAirport.getYCoord();
                return true;
            }
    
            double directionX = dx / distance;
            double directionY = dy / distance;
    
            xCoord += directionX * SPEED * deltaTimeInSeconds;
            yCoord += directionY * SPEED * deltaTimeInSeconds;
    
            double angleRadians = Math.atan2(dy, dx);
            double angleDegrees = Math.toDegrees(angleRadians);
    
            //DONT NEED TO RECALC ANGLE EVERY TIME
            if (angleDegrees < 0) {
                angleDegrees += 360;
            }
            
            direction = angleDegrees + 45;
    
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