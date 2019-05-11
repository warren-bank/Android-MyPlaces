package com.github.warren_bank.myplaces.models;

import com.github.warren_bank.myplaces.parsers.AbstractParser;
import com.github.warren_bank.myplaces.parsers.GpxParser;
import com.github.warren_bank.myplaces.parsers.KmlParser;

import com.github.warren_bank.filterablerecyclerview.FilterableListItem;

import android.location.Location;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class WaypointListItem implements FilterableListItem {
    public final String lat;
    public final String lon;
    public final String name;

    protected final int nonce;
    protected final Location location;

    public float distance;  // meters

    public WaypointListItem(String lat, String lon, String name, int nonce) {
        if (name == null) name = "[undefined]";

        this.lat      = lat;
        this.lon      = lon;
        this.name     = name;
        this.nonce    = nonce;
        this.location = new Location("MyPlaces");

        this.location.setLatitude(
            Location.convert(lat)
        );
        this.location.setLongitude(
            Location.convert(lon)
        );

        this.distance = 0;
    }

    public void updateDistance(Location currentPosition) {
        distance = currentPosition.distanceTo(location);
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public String getFilterableValue() {
        return name;
    }

    public boolean equals(WaypointListItem that) {
        if (that == null) return false;

        return (this.lat.equals(that.lat) && this.lon.equals(that.lon));
    }

    // Helper

    public static List<FilterableListItem> fromFile(String filepath, String format) {
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
            return new ArrayList<FilterableListItem>();
        }
        else {
            ArrayList<WaypointListItem> waypoints = parser.parse();
            return (List<FilterableListItem>)(List<?>) waypoints;
        }
    }

    // Comparator private classes

    private static class SequentialOrderComparator implements Comparator<FilterableListItem> {
        @Override
        public int compare(FilterableListItem x, FilterableListItem y) {
            if ((x == null) || (y == null)) throw new NullPointerException();

            WaypointListItem a = (WaypointListItem) x;
            WaypointListItem b = (WaypointListItem) y;

            if (a.nonce < b.nonce) return -1;
            if (a.nonce > b.nonce) return 1;
            return 0;
        }
    }

    private static class DistanceOrderComparator implements Comparator<FilterableListItem> {
        @Override
        public int compare(FilterableListItem x, FilterableListItem y) {
            if ((x == null) || (y == null)) throw new NullPointerException();

            WaypointListItem a = (WaypointListItem) x;
            WaypointListItem b = (WaypointListItem) y;

            if (a.distance < b.distance) return -1;
            if (a.distance > b.distance) return 1;
            return 0;
        }
    }

    private static class AlphabeticOrderComparator implements Comparator<FilterableListItem> {
        @Override
        public int compare(FilterableListItem x, FilterableListItem y) {
            if ((x == null) || (y == null)) throw new NullPointerException();

            WaypointListItem a = (WaypointListItem) x;
            WaypointListItem b = (WaypointListItem) y;

            return a.name.compareTo(b.name);
        }
    }

    // Comparator static instances

    public static final SequentialOrderComparator sequentialOrderComparator = new SequentialOrderComparator();
    public static final DistanceOrderComparator   distanceOrderComparator   = new DistanceOrderComparator();
    public static final AlphabeticOrderComparator alphabeticOrderComparator = new AlphabeticOrderComparator();
}
