package uk.ac.ed.inf;

/**
 * An coordinate constructed by a longitude and a latitude
 */

public class LongLat {

    /** The longitude which measures east or west of the point */
    private final double longitude;
    /** The latitude which measures north or south of the point */
    private final double latitude;
    /** Every move when flying is a straight line of length 0.00015 degrees */
    public static final double DISTANCE_TOLERANCE = 0.00015;

    /**
     * Constructor method that gives the point a longitude and a latitude when generated
     * @param longitude longitude of the point
     * @param latitude latitude of the point
     */
    public LongLat(double longitude, double latitude) {
        this.longitude = longitude;
        this.latitude = latitude;
    }

    // Getters
    public double getLongitude() {
        return longitude;
    }
    public double getLatitude() {
        return latitude;
    }

    /**
     * Calculates the distance between the point and another point using the Pythagorean formula
     * @param longLat the point which we are calculating the distance from this one
     * @return the Pythagorean distance between the two points
     */
    public double distanceTo(LongLat longLat) {
        return Math.sqrt(Math.pow(this.latitude - longLat.latitude, 2) +
                Math.pow(this.longitude - longLat.longitude, 2));
    }

    /**
     * Checks if the point is close to the other one by comparing to the distance tolerance
     * @param longLat the point which we are checking if is close to this one
     * @return True if they are close and false if not
     */
    public boolean closeTo(LongLat longLat) {
        double distance = this.distanceTo(longLat);
        return distance < DISTANCE_TOLERANCE;
    }

    /**
     * Gives the next position of the point after a movement is made
     * it moves with a conventional distance of 0.00015.
     * @param angle the direction in degrees that the point is moving
     * @return the point at the next position
     */
    public LongLat nextPosition(int angle) {
        if (angle >= 0 && angle <= 350 && angle % 10 == 0) {
            // checks if angle is valid
            double longitude_change = DISTANCE_TOLERANCE * Math.cos(Math.toRadians(angle));
            double latitude_change = DISTANCE_TOLERANCE * Math.sin(Math.toRadians(angle));
            return new LongLat
                    (this.longitude + longitude_change, this.latitude + latitude_change);
        } else {
            // an invalid angle produces an exception
            throw new IllegalArgumentException("Please give a valid angle.");
        }
    }
}
