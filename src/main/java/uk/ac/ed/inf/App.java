package uk.ac.ed.inf;

import java.io.FileNotFoundException;
import java.sql.SQLException;

/**
 * this is the main class (entry) of the program
 * parameters are received here and the program is initiated by them
 */
public class App 
{
    /** the day of the specific date where the drone program runs */
    private static String day;
    /** the month of the specific date where the drone program runs */
    private static String month;
    /** the year of the specific date where the drone program runs */
    private static String year;
    /** the web port which makes up the HTTP string to access the website content */
    private static String webPort;
    /** the database port which makes up the JDBC string to access the database content */
    private static String databasePort;

    /**
     * the main method uses the input as parameters to call functions in other classes.
     * these functions complete the job of the drone delivery planning for the day.
     * @param args an array which contains the five numbers in the input that signifies date,
     *             web port and database port
     * @throws SQLException the operations to the database can potentially throw this exception,
     * so we should catch these
     * @throws FileNotFoundException the exception is thrown when a file with the specified
     * pathname does not exist
     */
    public static void main( String[] args ) throws SQLException, FileNotFoundException {
        day=args[0];
        month=args[1];
        year=args[2];
        webPort=args[3];
        databasePort=args[4];

        Database database = new Database("localhost", databasePort);
        database.readOrderNo();
        database.readDeliverTo();
        database.costForEveryOrder();
        database.createDeliveriesTable();

        HTTPConnection HTTPConn = new HTTPConnection("localhost", webPort);
        HTTPConn.getNoFlyZone();
        HTTPConn.getShopLocation(database);

        AllOrders allOrders = new AllOrders();
        allOrders.getAllOrders(database, HTTPConn);

        NoFlyZones noFlyZones = new NoFlyZones(HTTPConn);

        Drone drone = new Drone();
        drone.findAllStops(allOrders, noFlyZones);
        drone.makeAllMoves();
        drone.createVisualisation();
        drone.createFlightPathTable(database);
    }

    // Getters
    public static String getDay() {return day;}
    public static String getMonth() {return month;}
    public static String getYear() {return year;}
    public static String getWebPort() {return webPort;}
    public static String getDatabasePort() {return databasePort;}
}
