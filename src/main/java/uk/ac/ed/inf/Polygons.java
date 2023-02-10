package uk.ac.ed.inf;

import java.util.ArrayList;

/**
 * this class matches the details in the "buildings" folder from the website content
 * this class serves the purpose of deserialization of the JSON records
 * we are interested in the coordinate that make up the polygons (no-fly-zones)
 */
public class Polygons {

    public String type;

    public ArrayList<ArrayList<Points>> coordinates;

    public static class Points{
        public String type;
        public ArrayList<Double> coordinates;
    }
}
