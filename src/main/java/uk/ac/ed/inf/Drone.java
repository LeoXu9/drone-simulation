package uk.ac.ed.inf;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;

/**
 * this class contains the algorithm for the flight path of the drone
 */
public class Drone {
    /** whether the flight is finished (returned to Appleton Tower) */
    private boolean finished;
    /** the battery power left in the drone */
    private int batteryPower;
    /** location tracker when planning the path (not for when the real moves happen) */
    private LongLat testLocation;
    /** location tracker for when the drone moves */
    private LongLat currentLocation;
    /** every LongLat location after a move */
    private final ArrayList<LongLat> flightPath;
    /** the landmarks automatically created given the confined area */
    private final ArrayList<LongLat> landmarks;
    /** the midway points, delivery locations, shop locations that the drone needs to reach
     * sequentially through the flight */
    private final ArrayList<LongLat> allStops;
    private final ArrayList<Order> orderInLine;
    private final ArrayList<String> orderNoFlightPath;
    private final ArrayList<Integer> angles;

    /** these four coordinates define the boundary of the confinement area */
    public static final double MAX_LONGITUDE = -3.184319;
    public static final double MAX_LATITUDE = 55.946233;
    public static final double MIN_LONGITUDE = -3.192473;
    public static final double MIN_LATITUDE = 55.942617;
    /** location of Appleton Tower which is the start and destination of the flight */
    public static final double APPLETON_TOWER_LONGITUDE = -3.186874;
    public static final double APPLETON_TOWER_LATITUDE = 55.944494;
    public static final int ANGLE_HOVERING = -999;

    /**
     * constructor method for the Drone class
     * the drone initialises itself with 1500 battery power (1500 moves), location at the Appleton Tower,
     * landmarks are found and stored in an arraylist
     */
    public Drone() {
        this.angles = new ArrayList<>();
        this.finished = false;
        this.batteryPower = 1500;
        this.allStops = new ArrayList<>();
        this.currentLocation = new LongLat(APPLETON_TOWER_LONGITUDE, APPLETON_TOWER_LATITUDE);
        this.testLocation = new LongLat(APPLETON_TOWER_LONGITUDE, APPLETON_TOWER_LATITUDE);
        this.allStops.add(this.currentLocation);
        this.flightPath = new ArrayList<>();
        this.landmarks = new ArrayList<>();
        this.findLandmarks();
        this.orderInLine = new ArrayList<>();
        this.orderNoFlightPath = new ArrayList<>();
    }

    /**
     * the landmarks are found by equally dividing the confinement area into little squares
     * we combine the equally split longitude and latitude, and output a number of landmarks
     */
    public void findLandmarks(){
        ArrayList<Double> longitudeList = new ArrayList<>();
        ArrayList<Double> latitudeList = new ArrayList<>();
        double longitudeDiff = (MAX_LONGITUDE - MIN_LONGITUDE)/40;
        double latitudeDiff = (MAX_LATITUDE - MIN_LATITUDE)/40;
        for(int i = 1; i < 40; i++){
            longitudeList.add(MIN_LONGITUDE+ i*longitudeDiff);
            latitudeList.add(MIN_LATITUDE + i*latitudeDiff);
        }
        for (Double value : longitudeList) {
            for (Double aDouble : latitudeList) {
                this.landmarks.add(new LongLat(value, aDouble));
            }
        }
    }

    /**
     * finds the potential midway point if there is no straight line that passes through start and destination
     * without touching the no-fly-zones
     * @param start where the path starts
     * @param dest where the path ends
     * @param noFlyZones the no-fly-zone class
     *                   we need to use one of its method to see if there is no-fly-zones in the way
     * @return an arraylist of points that contains the destination (but not start)
     * and a potential midway point
     */
    public ArrayList<LongLat> findPath(LongLat start, LongLat dest, NoFlyZones noFlyZones){
        ArrayList<LongLat> path = new ArrayList<>();
        if(noFlyZones.obstacleFound(start, dest)) {
            path.add(bestLandmark(start, dest, noFlyZones));
        }
        path.add(dest);
        return path;
    }

    /**
     * finds the best landmark out of all the ones in the "landmark" arraylist
     * double filter then finds the one with the shortest distance
     * @param start where the path starts
     * @param dest where the path ends
     * @param noFlyZones the no-fly-zone class
     *                   we need to use one of its method to see if there is no-fly-zones in the way
     * @return the best landmark which takes the least distance for the drone go from start to destination
     */
    public LongLat bestLandmark(LongLat start, LongLat dest, NoFlyZones noFlyZones){
        // find the ones that connect start and destination and do not pass through no-fly-zones
        ArrayList<LongLat> okLandmarks = new ArrayList<>();
        for(LongLat longLat : this.landmarks){
            if(!noFlyZones.obstacleFound(start, longLat)&&!noFlyZones.obstacleFound(longLat, dest)){
                okLandmarks.add(longLat);
            }
        }
        // find the ones that are not too close the no-fly-zone vertices because the
        // imprecision of doubles in GeoJson causes the path that are too close to cross the no-fly-zones
        ArrayList<LongLat> filteredLandmarks = new ArrayList<>();
        for (LongLat okLandmark : okLandmarks) {
            boolean filter = true;
            for (LongLat longLat1 : noFlyZones.getNoFlyZoneVertices()) {
                Point2D p = new Point2D.Double(longLat1.getLongitude(), longLat1.getLatitude());
                Line2D l1 = new Line2D.Double(okLandmark.getLongitude(), okLandmark.getLatitude(),
                        start.getLongitude(), start.getLatitude());
                Line2D l2 = new Line2D.Double(okLandmark.getLongitude(), okLandmark.getLatitude(),
                        dest.getLongitude(), dest.getLatitude());
                if (NoFlyZones.distanceToLine(l1, p) < 0.00015
                        || NoFlyZones.distanceToLine(l2, p) < 0.00015 ) {
                    filter = false;
                    break;
                }
            }
            if(filter){
                filteredLandmarks.add(okLandmark);
            }
        }
        // find the one with the shortest distance
        ArrayList<Double> lengths = new ArrayList<>();
        for(LongLat longLat : filteredLandmarks){
            lengths.add(start.distanceTo(longLat) + dest.distanceTo(longLat));
        }
        Double minVal = Collections.min(lengths);
        int minIdx = lengths.indexOf(minVal);
        return filteredLandmarks.get(minIdx);
    }

    /**
     * finds the order that takes the shortest distance out of all the orders left
     * @param allOrders the orders left to deliver for the day
     * @param noFlyZones the no-fly-zone class, we need to use one of its method to see
     *                   if there is no-fly-zones in the way
     */
    public void findShortestOrder(AllOrders allOrders, NoFlyZones noFlyZones){
        // calculate all the path lengths and store them in allPathLengths arraylist
        ArrayList<Double> allPathLengths = new ArrayList<>();
        for(Order order : allOrders.allOrders){
            double pathLength = 0;
            if(order.getShopLocation().size()==1){
                pathLength += this.pathLength
                        (this.findPath(this.testLocation, order.getShopLocation().get(0), noFlyZones));
                pathLength += this.pathLength
                        (this.findPath(order.getShopLocation().get(0), order.getDeliverTo(), noFlyZones));
            }
            else{
                if(this.optimisedShopSequence(order, noFlyZones)){
                    pathLength = getPath2(noFlyZones, order);
                }else{
                    pathLength = getPath1(noFlyZones, order);
                }
            }
            allPathLengths.add(pathLength);
        }
        Double minVal = Collections.min(allPathLengths);
        int minIdx = allPathLengths.indexOf(minVal);
        // the order with the shortest distance is stored as result
        Order result = allOrders.allOrders.get(minIdx);
        this.orderInLine.add(result);
        if(result.getShopLocation().size()==1){
            // when the order only contains food from one shop
            this.allStops.add(this.testLocation);
            ArrayList<LongLat> ll = this.findPath
                    (this.testLocation, result.getShopLocation().get(0), noFlyZones);
            this.allStops.addAll(ll);
            ArrayList<LongLat> ll2 = this.findPath
                    (result.getShopLocation().get(0), result.getDeliverTo(), noFlyZones);
            this.allStops.addAll(ll2);
        }else{
            //when the order contains food from two shops
            if(this.optimisedShopSequence(result, noFlyZones)){
                // when it is more efficient to go the second food shop
                this.allStops.add(this.testLocation);
                ArrayList<LongLat> ll = this.findPath
                        (this.testLocation, result.getShopLocation().get(1), noFlyZones);
                this.allStops.addAll(ll);
                ArrayList<LongLat> ll2 = this.findPath
                        (result.getShopLocation().get(1), result.getShopLocation().get(0), noFlyZones);
                this.allStops.addAll(ll2);
                ArrayList<LongLat> ll3 = this.findPath
                        (result.getShopLocation().get(0), result.getDeliverTo(), noFlyZones);
                this.allStops.addAll(ll3);
            }else{
                // when it is more efficient to go the second food shop
                this.allStops.add(this.testLocation);
                ArrayList<LongLat> ll = this.findPath
                        (this.testLocation, result.getShopLocation().get(0), noFlyZones);
                this.allStops.addAll(ll);
                ArrayList<LongLat> ll2 = this.findPath
                        (result.getShopLocation().get(0), result.getShopLocation().get(1), noFlyZones);
                this.allStops.addAll(ll2);
                ArrayList<LongLat> ll3 = this.findPath
                        (result.getShopLocation().get(1), result.getDeliverTo(), noFlyZones);
                this.allStops.addAll(ll3);
            }
        }
        // change the testLocation so for the next loop we are at a different location
        this.testLocation = result.getDeliverTo();
        // remove the order that has been delivered
        allOrders.allOrders.remove(minIdx);
    }

    /**
     * calculates the path length given a group of LongLat
     * @param longLats an arraylist of LongLat that signify the sequential stops of the path
     * @return the length of the path
     */
    public double pathLength(ArrayList<LongLat> longLats){
        double length = 0;
        for(int i = 0; i < longLats.size()-1; i++){
            length += longLats.get(i).distanceTo(longLats.get(i+1));
        }
        return length;
    }

    /**
     * find the length of the path of an order when the drone reaches the first food shop, then the second one
     * given that the order contains food from two shops
     * @param noFlyZones the no-fly-zone class, we need to use one of its method to see
     *                   if there is no-fly-zones in the way
     * @param order the order that is to be calculated the length of path,
     *              its delivery location and shop locations are key to the function
     * @return the length of the order path
     */
    private double getPath1(NoFlyZones noFlyZones, Order order) {
        double path1 = 0;
        path1 += this.pathLength
                (this.findPath(this.testLocation, order.getShopLocation().get(0), noFlyZones));
        path1 += this.pathLength
                (this.findPath(order.getShopLocation().get(0), order.getShopLocation().get(1), noFlyZones));
        path1 += this.pathLength
                (this.findPath(order.getShopLocation().get(1), order.getDeliverTo(), noFlyZones));
        return path1;
    }

    /**
     * find the length of the path of an order when the drone reaches the second food shop,
     *  then the first one given that the order contains food from two shops
     * @param noFlyZones the no-fly-zone class, we need to use one of its method to see
     *                   if there is no-fly-zones in the way
     * @param order the order that is to be calculated the length of path,
     *              its delivery location and shop locations are key to the function
     * @return the length of the order path
     */
    private double getPath2(NoFlyZones noFlyZones, Order order) {
        double path2 = 0;
        path2 += this.pathLength(this.findPath
                (this.testLocation, order.getShopLocation().get(1), noFlyZones));
        path2 += this.pathLength(this.findPath
                (order.getShopLocation().get(1), order.getShopLocation().get(0), noFlyZones));
        path2 += this.pathLength(this.findPath
                (order.getShopLocation().get(0), order.getDeliverTo(), noFlyZones));
        return path2;
    }

    /**
     * find whether the distance is shorter going to first shop or the second shop
     * @param order the order which gives us information about the shop locations and delivery location
     * @param noFlyZones the no-fly-zone class, we need to use one of its method to see
     *                   if there is no-fly-zones in the way
     * @return true if it is more efficient to go to the second food shop and false if otherwise
     */
    public boolean optimisedShopSequence(Order order, NoFlyZones noFlyZones){
        double path1;
        double path2;
        path1 = getPath1(noFlyZones, order);
        path2 = getPath2(noFlyZones, order);
        return path1 > path2;
    }

    /**
     * finds all the midway points, food shops and delivery locations that drone has to go through
     * to complete the orders for the day
     * @param allOrders the orders left for the day
     * @param noFlyZones the no-fly-zone class, we need to use one of its method to see
     *                   if there is no-fly-zones in the way
     */
    public void findAllStops(AllOrders allOrders, NoFlyZones noFlyZones){
        while(!allOrders.allOrders.isEmpty()){
            this.findShortestOrder(allOrders, noFlyZones);
        }
        this.allStops.add(new LongLat(APPLETON_TOWER_LONGITUDE, APPLETON_TOWER_LATITUDE));
    }

    /**
     * makes the drone to move once based on its current battery power and location
     * when the drone has more than 40 battery power, it keeps doing delivery and will return
     * to the Appleton Tower once delivery is done.
     * when the drone has less than 40 battery power, even if it has not done delivering every order,
     * it goes back to Appleton Tower.
     */
    public void makeAMove(){
        if(this.batteryPower>=40){
            // when the drone is able to make more than 40 moves
            LongLat nextStop = this.allStops.get(1);
            if(this.currentLocation.closeTo(nextStop)){
                // when the drone is close to the stop
                this.flightPath.add(this.currentLocation);
                this.allStops.remove(1);
                this.batteryPower -= 1;
                this.orderNoFlightPath.add(orderInLine.get(0).getOrderNo());
                this.angles.add(ANGLE_HOVERING);
                if(nextStop.equals(orderInLine.get(0).getDeliverTo())&&orderInLine.size()!=1){
                    orderInLine.remove(0);
                }
            }else if (this.currentLocation.closeTo(new LongLat
                    (APPLETON_TOWER_LONGITUDE, APPLETON_TOWER_LATITUDE))&&this.allStops.size()==1){
                // when the drone is close to the destination (Appleton Tower)
                this.flightPath.add(this.currentLocation);
                this.finished = true;
                this.batteryPower -= 1;
                this.angles.add(ANGLE_HOVERING);
                this.orderNoFlightPath.add(orderInLine.get(0).getOrderNo());
            }else{
                // when the drone needs to move for 0.00015 degrees so that it gets closer the next stop
                this.flightPath.add(this.currentLocation);
                int angle = this.findAngle(nextStop);
                this.currentLocation = this.currentLocation.nextPosition(angle);
                this.batteryPower -= 1;
                this.orderNoFlightPath.add(orderInLine.get(0).getOrderNo());
                this.angles.add(angle);
            }
        }else {
            // when the battery power is not enough to make 40 moves
            if(!this.currentLocation.closeTo
                    (new LongLat(APPLETON_TOWER_LONGITUDE, APPLETON_TOWER_LATITUDE))){
                // goes to the destination (Appleton Tower)
                LongLat nextStop = this.allStops.get(0);
                this.flightPath.add(this.currentLocation);
                int angle = this.findAngle(nextStop);
                this.currentLocation = this.currentLocation.nextPosition(angle);
                this.angles.add(angle);
            }
            else{
                // stops at Appleton Tower
                this.flightPath.add(this.currentLocation);
                this.finished=true;
                this.angles.add(ANGLE_HOVERING);
            }
            this.orderNoFlightPath.add(orderInLine.get(0).getOrderNo());
            this.batteryPower -= 1;
        }
    }

    /**
     * the drone keeps making moves till it reaches the destination
     */
    public void makeAllMoves(){
        while(this.allStops.size()>1&&!this.finished){
            this.makeAMove();
        }
        // the counter means the total number of moves of the drone made for the delivery for the day
    }

    /**
     * tests every possible angle and finds the one that makes the most progress to the next stop
     * @param nextStop the next stop where the drone is ready to make a turn
     * @return the best angle that will give the move the most progress
     */
    public int findAngle(LongLat nextStop){
        ArrayList<Double> distances = new ArrayList<>();
        for (int testAngle = 0; testAngle < 360; testAngle += 10){
            LongLat nextPos = this.currentLocation.nextPosition(testAngle);
            distances.add(nextPos.distanceTo(nextStop));
        }
        Double minVal = Collections.min(distances);
        int minIdx = distances.indexOf(minVal);
        return minIdx*10;
    }

    /**
     * creates a GeoJson in the current directory that contains a feature which shows the flight path
     * of the drone
     * @throws FileNotFoundException the exception is thrown when a file with the specified pathname
     * does not exist
     */
    public void createVisualisation() throws FileNotFoundException {
        ArrayList<Point> points = new ArrayList<>();
        for(LongLat longLat : this.flightPath){
            points.add(Point.fromLngLat(longLat.getLongitude(), longLat.getLatitude()));
        }
        LineString lineString = LineString.fromLngLats(points);
        Feature feature = Feature.fromGeometry(lineString);
        FeatureCollection featureCollection = FeatureCollection.fromFeature(feature);
        String fileName = "drone-" + App.getDay() + "-" + App.getMonth() + "-" + App.getYear() + ".geojson";
        PrintWriter out = new PrintWriter(fileName + ".txt");
        out.println(featureCollection.toJson());
        out.close();
    }

    public ArrayList<LongLat> getFlightPath() {
        return flightPath;
    }

    public ArrayList<String> getOrderNoFlightPath() {
        return orderNoFlightPath;
    }

    public ArrayList<Integer> getAngles() {
        return angles;
    }

    public void createFlightPathTable(Database database) throws SQLException {
        Statement statement = database.getConn().createStatement();
        DatabaseMetaData databaseMetadata = database.getConn().getMetaData();
        ResultSet resultSet =
                databaseMetadata.getTables
                        (null, null, "FLIGHTPATH", null);
        // If the resultSet is not empty then the table exists, so we can drop it
        if (resultSet.next()) {
            statement.execute("drop table flightpath");
        }
        statement.execute(
                "create table flightpath(" +
                        "orderNo char(8), " +
                        "fromLongitude double, " +
                        "fromLatitude double, "+
                        "angle integer, " +
                        "toLongitude double," +
                        "toLatitude double)");
        PreparedStatement psFlightPath = database.getConn().prepareStatement(
                "insert into flightpath values (?, ?, ?, ?, ?, ?)");
        psFlightPath.setString(1, this.orderNoFlightPath.get(0));
        psFlightPath.setDouble(2, this.flightPath.get(0).getLongitude());
        psFlightPath.setDouble(3, this.flightPath.get(0).getLatitude());
        psFlightPath.setInt(4, this.angles.get(0));
        psFlightPath.setDouble(5, this.flightPath.get(0).getLongitude());
        psFlightPath.setDouble(6, this.flightPath.get(0).getLatitude());
        psFlightPath.execute();
        for (int i=1; i<orderNoFlightPath.size()-1; i++) {
            psFlightPath.setString(1, this.orderNoFlightPath.get(i));
            psFlightPath.setDouble(2, this.flightPath.get(i).getLongitude());
            psFlightPath.setDouble(3, this.flightPath.get(i).getLatitude());
            psFlightPath.setInt(4, this.angles.get(i));
            psFlightPath.setDouble(5, this.flightPath.get(i+1).getLongitude());
            psFlightPath.setDouble(6, this.flightPath.get(i+1).getLatitude());
            psFlightPath.execute();
        }
    }
}
