package uk.ac.ed.inf;

import java.util.ArrayList;

/**
 * This class represent the orders that the drone has to deliver.
 * Each object of this class represent one order
 */
public class Order {
    /** the information about the specific order */
    private final String orderNo;
    private final String deliverToW3W;
    private final LongLat deliverTo;
    private final ArrayList<String> shopLocationW3W;
    private final ArrayList<LongLat> shopLocation;
    private final int cost;

    // Getters
    public String getOrderNo() {return orderNo;}
    public String getDeliverToW3W() {return deliverToW3W;}
    public LongLat getDeliverTo() {return deliverTo;}
    public ArrayList<String> getShopLocationW3W() {return shopLocationW3W;}
    public ArrayList<LongLat> getShopLocation() {return shopLocation;}
    public int getCost() {return cost;}

    /**
     * constructor method for an Order object
     * @param orderNo the order number
     * @param deliverToW3W the delivery location in What3Words
     * @param deliverTo the delivery location in longitude and latitude
     * @param shopLocationW3W the shop location(s) in What3Words
     * @param cost the total cost including the delivery fee
     * @param shopLocation the shop location(s) in longitude and latitude
     */
    public Order(String orderNo, String deliverToW3W, LongLat deliverTo,
                 ArrayList<String> shopLocationW3W, int cost, ArrayList<LongLat> shopLocation) {
        this.orderNo = orderNo;
        this.deliverToW3W = deliverToW3W;
        this.deliverTo = deliverTo;
        this.shopLocationW3W = shopLocationW3W;
        this.cost = cost;
        this.shopLocation = shopLocation;
    }
}
