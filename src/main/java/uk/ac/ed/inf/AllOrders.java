package uk.ac.ed.inf;

import java.util.ArrayList;

/**
 * the AllOrders class contains a collection of all the orders in the day which was given in the parameters
 * and a way to assemble the orders into the collection
 */
public class AllOrders {
    /**
     * a collection of all the orders for the day
     */
    public ArrayList<Order> allOrders;

    /**
     * constructor method which creates an empty arraylist for all the orders
     */
    public AllOrders() {
        this.allOrders = new ArrayList<>();
    }

    /**
     * obtain the information for all the orders from the Order class with usage of database and HTTP connection
     * information includes: OrderNo, Customer, DeliverTo(W3W), DeliverTo(coordinate), ShopLocation(W3W),
     * ShopLocation(coordinate), cost
     * some information is from the database and some is from the website
     * @param database entry for database content is given in the parameter and is used here to access the
     *                 information of the orders
     * @param HTTPConn entry for website content is given in the parameter and is used here to access the
     *                 information of the orders
     */
    public void getAllOrders(Database database, HTTPConnection HTTPConn){
        for(int i = 0; i < HTTPConn.getShopLocation().size(); i++){
            this.allOrders.add(new Order(database.getOrderNoList().get(i),
                    database.getDeliverToList().get(i),
                    HTTPConn.getDeliverToCoordinate(database.getDeliverToList().get(i)),
                    HTTPConn.getShopLocation().get(i), database.getCostList().get(i),
                    HTTPConn.getCoordinate(HTTPConn.getShopLocation().get(i))));
        }
    }
}
