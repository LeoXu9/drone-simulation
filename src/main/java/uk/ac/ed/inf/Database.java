package uk.ac.ed.inf;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * the class interacts with the database
 * its job is to collect information from the database and store it into the attributes
 */
public class Database {
    /** the connection to the database that stores the order information that we need*/
    private final Connection conn;
    /** these are the arraylists that separately store the properties of the orders
     * the arraylist are all the same length and given an index the elements should exactly make up an order
     */
    private final ArrayList<String> orderNoList;
    private final ArrayList<String> deliverToList;
    private final ArrayList<Integer> costList;
    private final ArrayList<ArrayList<String>> itemList;

    // Getters
    public ArrayList<String> getOrderNoList() {return orderNoList;}
    public ArrayList<String> getDeliverToList() {return deliverToList;}
    public ArrayList<Integer> getCostList() {return costList;}
    public ArrayList<ArrayList<String>> getItemList() {return itemList;}
    public Connection getConn() {return conn;}

    /**
     * the connection is initialised by the jdbcString with the given database port
     * the arraylists are initialised as empty arraylists
     * @param machine_name the name of the machine which we assume being named 'localhost'
     * @param port endpoint of the database service for communication purposes
     * @throws SQLException the operations to the database can potentially throw this exception,
     * so we should catch these
     */
    public Database(String machine_name, String port) throws SQLException {
        String jdbcString = "jdbc:derby://" + machine_name + ":" + port + "/derbyDB";
        //We use the java.sql.DriverManager to get a java.sql.Connection to
        //the database that we specify with jdbcString
        this.conn = DriverManager.getConnection(jdbcString);
        this.orderNoList = new ArrayList<>();
        this.deliverToList = new ArrayList<>();
        this.costList = new ArrayList<>();
        this.itemList = new ArrayList<>();
    }

    /**
     * obtains the order number of the orders for the day from the table "orders" readily prepared
     * in the database
     * assigns these order numbers to the arraylist "orderNoList"
     * @throws SQLException the operations to the database can potentially throw this exception,
     * so we should catch these
     */
    public void readOrderNo() throws SQLException {
        final String orderQuery =
                "select * from orders where deliveryDate=(?)";
        PreparedStatement psDateQuery =
                conn.prepareStatement(orderQuery);
        String x = App.getYear() + "-" + App.getMonth() + "-" + App.getDay();
        psDateQuery.setString(1, x);
        ResultSet rs = psDateQuery.executeQuery();
        while (rs.next()) {
            String order = rs.getString("orderNo");
            this.orderNoList.add(order);
        }
    }

    /**
     * obtains the delivery location of the orders for the day from the table "orders" readily
     * prepared in the database
     * assigns these locations to the arraylist "deliverToList"
     * @throws SQLException the operations to the database can potentially throw this exception,
     * so we should catch these
     */
    public void readDeliverTo() throws SQLException {
        final String deliveryToQuery =
                "select * from orders where deliveryDate=(?)";
        PreparedStatement psDeliveryToQuery =
                conn.prepareStatement(deliveryToQuery);
        String x = App.getYear() + "-" + App.getMonth() + "-" + App.getDay();
        psDeliveryToQuery.setString(1, x);
        ResultSet rs2 = psDeliveryToQuery.executeQuery();
        while (rs2.next()) {
            String deliverTo = rs2.getString("deliverTo");
            this.deliverToList.add(deliverTo);
        }
    }

    /**
     * calculates the cost for each order and places the value into the arraylist "costList" in sequence
     * @throws SQLException the operations to the database can potentially throw this exception,
     * so we should catch these
     */
    public void costForEveryOrder() throws SQLException {
        ArrayList<String> items = new ArrayList<>();
        for (String s : orderNoList) {
            final String itemQuery =
                    "select * from orderDetails where orderNo=(?)";
            PreparedStatement psItemQuery =
                    conn.prepareStatement(itemQuery);
            psItemQuery.setString(1, s);
            ResultSet rs = psItemQuery.executeQuery();
            while (rs.next()) {
                String item = rs.getString("item");
                items.add(item);
            }
            String[] strings = new String[items.size()];
            strings = items.toArray(strings);
            HTTPConnection HTTPConn = new HTTPConnection("localhost", App.getWebPort());
            int cost = HTTPConn.getDeliveryCost(strings);
            costList.add(cost);
            ArrayList<String> item_for_shop = new ArrayList<>(Arrays.asList(strings));
            itemList.add(item_for_shop);
            items.clear();
        }
    }

    /**
     * outputs a table called "deliveries" that contains the order number, delivery location
     * and cost for every order
     * @throws SQLException the operations to the database can potentially throw this exception,
     * so we should catch these
     */
    public void createDeliveriesTable() throws SQLException {
        Statement statement = conn.createStatement();
        DatabaseMetaData databaseMetadata = conn.getMetaData();
        ResultSet resultSet =
                databaseMetadata.getTables
                        (null, null, "DELIVERIES", null);
        // If the resultSet is not empty then the table exists, so we can drop it
        if (resultSet.next()) {
            statement.execute("drop table deliveries");
        }
        statement.execute(
                "create table deliveries(" +
                        "orderNo char(8), " +
                        "deliveredTo varchar(19), " +
                        "costInPence int)");
        PreparedStatement psDelivery = conn.prepareStatement(
                "insert into deliveries values (?, ?, ?)");
        for (int i=0; i<costList.size(); i++) {
            psDelivery.setString(1, orderNoList.get(i));
            psDelivery.setString(2, deliverToList.get(i));
            psDelivery.setInt(3, costList.get(i));
            psDelivery.execute();
        }
    }


}
