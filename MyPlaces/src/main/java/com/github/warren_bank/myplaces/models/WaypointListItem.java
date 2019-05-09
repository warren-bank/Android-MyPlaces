package com.github.warren_bank.myplaces.models;

import com.github.warren_bank.myplaces.parsers.AbstractParser;
import com.github.warren_bank.myplaces.parsers.GpxParser;
import com.github.warren_bank.myplaces.parsers.KmlParser;

import java.util.ArrayList;
import java.util.Comparator;

public class WaypointListItem {
    public final String lat;
    public final String lon;
    public final String name;
    public final int nonce;

    public float distance;

    public WaypointListItem(String lat, String lon, String name, int nonce) {
        if (name == null) name = "[undefined]";

        this.lat   = lat;
        this.lon   = lon;
        this.name  = name;
        this.nonce = nonce;
    }

    @Override
    public String toString() {
        return name;
    }

    public boolean equals(WaypointListItem that) {
        if (that == null) return false;

        return (this.lat.equals(that.lat) && this.lon.equals(that.lon));
    }

    public static ArrayList<WaypointListItem> fromFile(String filepath, String format) {
        AbstractParser parser = null;

        switch (format) {
            case "gpx":
              parser = new GpxParser(filepath);
              break;
            case "kml":
              parser = new KmlParser(filepath);
              break;
            default:
              break;
        }

        if (parser == null) {
            return new ArrayList<WaypointListItem>();
        }
        else {
            return parser.parse();
        }
    }

    // Comparator private classes

    private static class SequentialOrderComparator implements Comparator<WaypointListItem> {
        @Override
        public int compare(WaypointListItem a, WaypointListItem b) {
            if ((a == null) || (b == null)) throw new NullPointerException();

            if (a.nonce < b.nonce) return -1;
            if (a.nonce > b.nonce) return 1;
            return 0;
        }
    }

    private static class DistanceOrderComparator implements Comparator<WaypointListItem> {
        @Override
        public int compare(WaypointListItem a, WaypointListItem b) {
            if ((a == null) || (b == null)) throw new NullPointerException();

            if (a.distance < b.distance) return -1;
            if (a.distance > b.distance) return 1;
            return 0;
        }
    }

    private static class AlphabeticOrderComparator implements Comparator<WaypointListItem> {
        @Override
        public int compare(WaypointListItem a, WaypointListItem b) {
            if ((a == null) || (b == null)) throw new NullPointerException();

            return a.name.compareTo(b.name);
        }
    }

    // Comparator static instances

    public static final SequentialOrderComparator sequentialOrderComparator = new SequentialOrderComparator();
    public static final DistanceOrderComparator   distanceOrderComparator   = new DistanceOrderComparator();
    public static final AlphabeticOrderComparator alphabeticOrderComparator = new AlphabeticOrderComparator();
}
