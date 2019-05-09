package com.github.warren_bank.myplaces.services;

import com.github.warren_bank.myplaces.models.WaypointListItem;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;

public class PlacesLocationManager {
    private Context                     context;
    private LocationManager             locationManager;
    private ArrayList<WaypointListItem> places_arrayList;
    private RecyclerView.Adapter        places_arrayAdapter;
    private PlacesLocationListener      places_locationListener;
    private int                         interval;

    private class PlacesLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(Location location) {
            PlacesLocationManager.this.calculateDistance(location);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }           
    }

    public PlacesLocationManager(
        Context                     context,
        ArrayList<WaypointListItem> places_arrayList,
        RecyclerView.Adapter        places_arrayAdapter
    ) {
        this.context                  = context;
        this.locationManager          = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
        this.places_arrayList         = places_arrayList;
        this.places_arrayAdapter      = places_arrayAdapter;
        this.places_locationListener  = new PlacesLocationListener();
        this.interval                 = 0;
    }

    public void setInterval(int seconds) {
        if (interval != seconds) {
            clearInterval();

            // sanity checks
            if (android.os.Build.VERSION.SDK_INT >= 28) {
                if (!locationManager.isLocationEnabled()) return;
            }
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) return;

            interval = seconds;

            long minTime      = seconds * 1000;  // milliseconds
            float minDistance = 1.5f;            // 1.5 meters is about 5 ft
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                minTime,
                minDistance,
                places_locationListener
            );
        }
    }

    public void clearInterval() {
        if (interval != 0) {
            locationManager.removeUpdates(places_locationListener);
            interval = 0;
        }
    }

    public void refresh() {
        // sanity checks
        if (android.os.Build.VERSION.SDK_INT >= 28) {
            if (!locationManager.isLocationEnabled()) return;
        }
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) return;

        locationManager.requestSingleUpdate(
            LocationManager.GPS_PROVIDER,
            places_locationListener,
            null
        );
    }

    protected void calculateDistance(Location location) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(final Void ... params) {

                for (int i=0; i < places_arrayList.size(); i++) {
                    WaypointListItem point = places_arrayList.get(i);
                    point.updateDistance(location);
                }

                Collections.sort(places_arrayList, WaypointListItem.distanceOrderComparator);
                return null;
            }
 
            @Override
            protected void onPostExecute(final Void result) {
                places_arrayAdapter.notifyDataSetChanged();
            }
        }.execute();
    }
}
