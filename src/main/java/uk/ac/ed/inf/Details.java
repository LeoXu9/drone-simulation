package uk.ac.ed.inf;

/**
 * this class matches the details in the "words" folder from the website content
 * this class serves the purpose of deserialization of the JSON records
 * we are interested in the coordinate that corresponds to the 3 words
 */
public class Details {
    public String country;

    public static class square{
        southwest southwest;
        public static class southwest{
            public double lng;
            public double lat;
        }

        northeast northeast;
        public static class northeast{
            public double lng;
            public double lat;
        }
    }

    public String nearestPlace;

    coordinates coordinates;

    public static class coordinates{
        public double lng;
        public double lat;
    }

    public String words;
    public String language;
    public String map;
}
