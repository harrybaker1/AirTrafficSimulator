package edu.curtin.saed.assignment1;


@SuppressWarnings("PMD")
public class Plane {
    private final double SPEED = 1.0; //Units per second
    private int id;
    private double xCoord;
    private double yCoord;
    private double direction;
    private Airport currentAirport;
    private Airport destinationAirport;
    private FlightStatus flightStatus;
    
    public Plane(int id, double xCoord, double yCoord, Airport currentAirport, FlightStatus flightStatus) {
        this.id = id;
        this.xCoord = xCoord;
        this.yCoord = yCoord;
        this.direction = 0.0;
        this.currentAirport = currentAirport;
        this.destinationAirport = null;
        this.flightStatus = FlightStatus.READY;
    }

    public void depart(Airport destinationAirport) {
        this.destinationAirport = destinationAirport;
        this.flightStatus = FlightStatus.IN_FLIGHT;
    }

    public void updatePosition(double deltaTime) {
        if (flightStatus != FlightStatus.IN_FLIGHT || destinationAirport == null) {
            return;
        }
    
        // Convert deltaTime from milliseconds to seconds
        double deltaTimeInSeconds = deltaTime / 1000.0;
    
        // Calculate the direction vector (dx, dy)
        double dx = destinationAirport.getXCoord() - xCoord;
        double dy = destinationAirport.getYCoord() - yCoord;
    
        // Calculate the distance to the destination
        double distance = Math.hypot(dx, dy);
    
        // If the plane is close enough to the destination, snap to the destination and land
        if (distance <= SPEED * deltaTimeInSeconds) {
            xCoord = destinationAirport.getXCoord();
            yCoord = destinationAirport.getYCoord();
            land();
            return;
        }
    
        // Normalize the direction vector to get the unit direction vector
        double directionX = dx / distance;
        double directionY = dy / distance;
    
        // Update the position by moving in the direction of the destination
        xCoord += directionX * SPEED * deltaTimeInSeconds;
        yCoord += directionY * SPEED * deltaTimeInSeconds;
    

        double angleRadians = Math.atan2(dy, dx); // returns the angle in radians
        double angleDegrees = Math.toDegrees(angleRadians); // converts radians to degrees

        if (angleDegrees < 0) {
            angleDegrees += 360;
        }

        direction = angleDegrees + 90;
    }
    
    
    
    
    
    

    private void land() {
        currentAirport = destinationAirport;
        destinationAirport = null;
        flightStatus = FlightStatus.UNDER_SERVICE;
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

    public FlightStatus getFlightStatus() {
        return flightStatus;
    }

    public void setXCoord(Double xCoord) {
        this.xCoord = xCoord;
    }

    public void setYCoord(Double yCoord) {
        this.yCoord = yCoord;
    }

    public void setDirection(Double direction) {
        this.direction = direction;
    }

    public void setFlightStatus(FlightStatus flightStatus) {
        this.flightStatus = flightStatus;
    }

    enum FlightStatus {
        IN_FLIGHT,
        READY,
        UNDER_SERVICE
    }
}