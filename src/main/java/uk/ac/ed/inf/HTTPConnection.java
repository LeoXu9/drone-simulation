package uk.ac.ed.inf;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mapbox.geojson.*;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

/**
 * the class with http connection and json parser functionality to obtain information from the website
 */
public class HTTPConnection {

    /** the name of the machine */
    private final String machine_name;
    /** the port where the web server is running */
    private final String port;
    /** to make request and get response */
    private static final HttpClient client = HttpClient.newHttpClient();
    /** the query string where data is passed to the web server */
    private String http_string;
    /** the shop locations obtained from the website content */
    private final ArrayList<ArrayList<String>> shopLocation;
    /** list of polygons that depict the boundary of the no-fly-zones */
    private ArrayList<Polygon> lstP;

    /** constructor method that records the machine name as well as the port number
     * also initialises the shopLocation and lstP arraylists
     */
    public HTTPConnection(String machine_name, String port) {
        this.machine_name = machine_name;
        this.port = port;
        this.shopLocation = new ArrayList<>();
        this.lstP = new ArrayList<>();
    }

    // Getters
    public ArrayList<Polygon> getLstP() {
        return lstP;
    }
    public ArrayList<ArrayList<String>> getShopLocation() {
        return shopLocation;
    }

    /** Builds an HTTP GET request */
    public HttpRequest makeRequest(){
        HttpRequest request = null;
        try{
            request = HttpRequest.newBuilder()
                    .uri(URI.create(this.http_string))
                    .build();
        }catch (IllegalArgumentException e) {
            System.err.println("Exception occurred: " + e.getMessage());
            System.exit(1);
        }
        return request;
    }

    /** Sends the request to the HTTP client */
    public HttpResponse<String> receiveResponse(){
        HttpResponse<String> response = null;
        try{
            response = client.send(this.makeRequest(), HttpResponse.BodyHandlers.ofString());
        }catch(IOException | InterruptedException e) {
            System.err.println("Exception occurred: " + e.getMessage());
            System.exit(1);
        }
        return response;
    }

    /**
     * Calculates the total cost of a delivery given the item names
     * This includes both the delivery charge (50p) and the cost of the food
     * @param strings a variable number of names of food in the order
     * @return the total cost of food plus standard delivery cost of 50p
     */
    public int getDeliveryCost(String... strings){
        this.http_string = "http://" + machine_name + ":" + port + "/menus/menus.json";
        // parsing json into a list of java object
        Type type = new TypeToken<List<Shop>>() {}.getType();
        List<Shop> shopList = new Gson().fromJson(this.receiveResponse().body(), type);

        // find out the price of each item and add them to total cost
        int netPayment = 50;
        for (Shop shop : shopList) {
            for (int j = 0; j < shop.menu.size(); j++) {
                if (Arrays.asList(strings).contains(shop.menu.get(j).item)) {
                    netPayment += shop.menu.get(j).pence;
                }
            }
        }
        return netPayment;
    }

    /**
     * obtains the shop locations of the orders for the day from the website content
     * the shop locations are in strings (What3Words)
     * @param database the database contains the information of the shop names providing the item names
     */
    public void getShopLocation(Database database){
        this.http_string = "http://" + machine_name + ":" + port + "/menus/menus.json";
        Type type = new TypeToken<List<Shop>>() {}.getType();
        List<Shop> shopList = new Gson().fromJson(this.receiveResponse().body(), type);
        for(int i = 0; i < database.getItemList().size(); i++){
            ArrayList<String> list = new ArrayList<>();
            for (int j = 0; j < database.getItemList().get(i).size(); j++){
                for(Shop shop : shopList){
                    for (int k = 0; k < shop.menu.size(); k++) {
                        if (database.getItemList().get(i).get(j).equals(shop.menu.get(k).item)) {
                            list.add(shop.location);
                        }
                    }
                }
            }
            Set<String> set = new HashSet<>(list);
            list.clear();
            list.addAll(set);
            this.shopLocation.add(list);
        }
    }

    /**
     * converts the shop location in W3W to in longitude and latitude
     * @param shopLocationW3W the list of string that contains the shop locations in terms of W3W
     * @return the converted list of LongLat that corresponds to the input one
     */
    public ArrayList<LongLat> getCoordinate(ArrayList<String> shopLocationW3W){
        ArrayList<LongLat> coords = new ArrayList<>();
        for (String w3w : shopLocationW3W){
            Details details = extractDetails(w3w);
            coords.add(new LongLat(details.coordinates.lng, details.coordinates.lat));
        }
        return coords;
    }

    /**
     * maps the detail files from the website content to the prepared java class
     * @param w3w the 3 words which are also required to reach the bottom level details files
     * @return an object of the Details class
     */
    private Details extractDetails(String w3w) {
        String[] words = w3w.split("[.]");
        this.http_string =  "http://" + machine_name + ":" + port + "/words/"
                + words[0] + "/" + words[1] + "/" + words[2] + "/details.json";
        Type type = new TypeToken<Details>() {}.getType();
        return new Gson().fromJson(this.receiveResponse().body(), type);
    }

    /**
     * extracts the longitude and latitude from the Details object and forms a LongLat object using those
     * @param w3w the What3Words which gives to the path to the bottom level details file
     * @return the LongLat object constructed by the coordinate of the location w3w signifies
     */
    public LongLat getDeliverToCoordinate(String w3w){
        Details details = extractDetails(w3w);
        return new LongLat(details.coordinates.lng, details.coordinates.lat);
    }

    /**
     * assigns the polygons extracted from the "buildings" folder in the website content
     * to the attribute "lstP" of this class
     */
    public void getNoFlyZone(){
        this.http_string = "http://" + machine_name + ":" + port + "/buildings/no-fly-zones.geojson";
        String source = this.receiveResponse().body();
        FeatureCollection fc = FeatureCollection.fromJson(source);
        List<Feature> lstF = fc.features();
        List<Geometry> lstG = new ArrayList<>();
        ArrayList<Polygon> lstP = new ArrayList<>();
        assert lstF != null;
        for(Feature f : lstF){
            lstG.add(f.geometry());
        }
        for(Geometry g : lstG){
            if(g instanceof Polygon){
                lstP.add((Polygon)g);
            }
        }
        this.lstP = lstP;
    }
}
