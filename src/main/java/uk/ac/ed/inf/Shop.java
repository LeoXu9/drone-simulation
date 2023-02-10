package uk.ac.ed.inf;

import java.util.ArrayList;

/**
 * this class matches the details in the "menus" folder from the website content
 * this class serves the purpose of deserialization of the JSON records
 * we are interested in the location, item and price
 */
public class Shop {
    public String name;
    public String location;
    public ArrayList<Menu> menu;
    public static class Menu {
        public String item;
        public int pence;
    }
}
