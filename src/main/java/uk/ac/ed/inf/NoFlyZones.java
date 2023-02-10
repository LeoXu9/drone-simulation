package uk.ac.ed.inf;

import com.google.gson.Gson;
import java.awt.geom.*;

import java.util.ArrayList;

/**
 * this class contains a way to extract the information about no-fly-zones and store it into the attributes
 */
public class NoFlyZones {
    /** the collection of polygons of the no-fly-zones */
    private final ArrayList<Path2D.Double> noFlyZonePath;
    /** the collection of vertices of the no-fly-zones */
    private final ArrayList<LongLat> noFlyZoneVertices;

    // Getter
    public ArrayList<LongLat> getNoFlyZoneVertices() {
        return noFlyZoneVertices;
    }

    /**
     * constructor method that extracts information from HTTPConnection's attribute "lstP"
     * and assigns value to the two arraylist attributes of this class
     * @param HTTPConn an instance of the HTTPConnection class so its attribute "lstP" is provided
     */
    public NoFlyZones(HTTPConnection HTTPConn) {
        ArrayList<ArrayList<LongLat>> noFlyZoneCoordinates = new ArrayList<>();
        // initialises the two attributes as empty arraylists
        this.noFlyZonePath = new ArrayList<>();
        this.noFlyZoneVertices = new ArrayList<>();
        for(int i = 0; i < HTTPConn.getLstP().size(); i++){
            ArrayList<LongLat> arrayLists = new ArrayList<>();
            String string = new Gson().toJson(HTTPConn.getLstP().get(i));
            Polygons polygons = new Gson().fromJson(string, Polygons.class);
            // minus 1 since the last element is a duplicate of the first one in the polygon
            for(int j = 0; j < polygons.coordinates.get(0).size()-1; j++){
                arrayLists.add(new LongLat(polygons.coordinates.get(0).get(j).coordinates.get(0),
                        polygons.coordinates.get(0).get(j).coordinates.get(1)));
            }
            noFlyZoneCoordinates.add(arrayLists);
        }
        for (ArrayList<LongLat> noFlyZoneCoordinate : noFlyZoneCoordinates) {
            noFlyZoneVertices.addAll(noFlyZoneCoordinate);
        }
        for (ArrayList<LongLat> noFlyZoneCoordinate : noFlyZoneCoordinates) {
            Path2D.Double path2D = new Path2D.Double();
            path2D.moveTo(noFlyZoneCoordinates.get(0).get(0).getLongitude(),
                    noFlyZoneCoordinates.get(0).get(1).getLatitude());
            for (int j = 1; j < noFlyZoneCoordinate.size(); j++) {
                path2D.lineTo(noFlyZoneCoordinate.get(j).getLongitude(),
                        noFlyZoneCoordinate.get(j).getLatitude());
            }
            path2D.closePath();
            this.noFlyZonePath.add(path2D);
        }
    }


    /**
     * the method is inspired by a stackoverflow post
     * reference is included in the report
     * it detects if the line intersects with the no-fly-zone polygons
     * @param l1 one of the points that constructs the line
     * @param l2 one of the points that constructs the line
     * @return true if the line does intersect with the no-fly-zone polygons and false otherwise
     */
    //https://stackoverflow.com/questions/24645064/how-to-check-if-path2d-intersect-with-line
    public boolean obstacleFound(LongLat l1, LongLat l2) {
        Line2D line = new Line2D.Double
                (l1.getLongitude(), l1.getLatitude(), l2.getLongitude(), l2.getLatitude());
        for (Path2D.Double path2D : this.noFlyZonePath) {
            Point2D.Double start = null;
            Point2D.Double point1 = null;
            Point2D.Double point2 = null;
            for (PathIterator pi = path2D.getPathIterator(null); !pi.isDone(); pi.next()) {
                double[] coordinates = new double[6];
                switch (pi.currentSegment(coordinates)) {
                    case PathIterator.SEG_MOVETO -> {
                        point2 = new Point2D.Double(coordinates[0], coordinates[1]);
                        point1 = null;
                        start = (Point2D.Double) point2.clone();
                    }
                    case PathIterator.SEG_LINETO -> {
                        point1 = point2;
                        point2 = new Point2D.Double(coordinates[0], coordinates[1]);
                    }
                    case PathIterator.SEG_CLOSE -> {
                        point1 = point2;
                        point2 = start;
                    }
                }
                if (point1 != null) {
                    Line2D segment = new Line2D.Double(point1, point2);
                    if (segment.intersectsLine(line))
                        return true;
                }
            }
        }
        return false;
    }

    /**
     * the method is implemented with inspiration from a stackoverflow post
     * reference is included in the report
     * calculates the distance of a point to a line
     * @param l the line
     * @param p the point
     * @return the distance
     */
    //http://www.java2s.com/example/java-utility-method/
    // distance-between-point-and-line/distancetoline3-line2d-l-point2d-p-6a970.html
    public static double distanceToLine(Line2D l, Point2D p){
    // Calculate the distance between the point (nX, nY) and the line through the
    // points (nP1X, nP1Y), (nP2X, nP2Y).

        double dDist;

        if (l.getX1() == l.getX2())
            // Vertical line
            dDist = p.getX() - l.getX1();
        else if (l.getY1() == l.getY2())
            // Horizontal line
            dDist = p.getY() - l.getY1();
        else {
            // Figure out the slope and Y intercept of the line
            double dM1 = (l.getY2() - l.getY1()) / (l.getX2() - l.getX1());
            double dB1 = l.getY1() - (dM1 * l.getX1());
            // Figure out the slope and Y intercept of the perpendicular line
            // through the third point
            double dM2 = -(1 / dM1);
            double dB2 = p.getY() - (dM2 * p.getX());

            // Find the intersection of the two lines
            double dXInt, dYInt;
            dXInt = (dB2 - dB1) / (dM1 - dM2);
            dYInt = (dM2 * dXInt) + dB2;

            // Now calculate the distance between the point and the intersection of
            // the two lines.
            dDist = Math.sqrt(Math.pow(dXInt - p.getX(), 2) + Math.pow(dYInt - p.getY(), 2));
        }
        return Math.abs(dDist);
    }
}
