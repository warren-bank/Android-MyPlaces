package com.github.warren_bank.myplaces.services;

import com.github.warren_bank.myplaces.models.WaypointListItem;

import com.github.warren_bank.filterablerecyclerview.FilterableListItem;
import com.github.warren_bank.filterablerecyclerview.FilterableAdapter;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;

import java.util.Collections;
import java.util.List;

public class PlacesLocationManager {
    private Activity                    activity;
    private LocationManager             locationManager;
    private List<FilterableListItem>    unfilteredList;
    private FilterableAdapter           recyclerFilterableAdapter;
    private PlacesLocationListener      places_locationListener;
    private int                         interval;

    private static final int            PERMISSIONS_REQUEST_CODE = 0;

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
        Activity                    activity,
        List<FilterableListItem>    unfilteredList,
        FilterableAdapter           recyclerFilterableAdapter
    ) {
        this.activity                  = activity;
        this.locationManager           = (LocationManager)activity.getSystemService(Context.LOCATION_SERVICE);
        this.unfilteredList            = unfilteredList;
        this.recyclerFilterableAdapter = recyclerFilterableAdapter;
        this.places_locationListener   = new PlacesLocationListener();
        this.interval                  = 0;
    }

    public void setInterval(int seconds) {
        if (!has_permission()) {
            locationManager = null;
            interval        = seconds;

            request_permission();
            return;
        }

        if (interval != seconds) {
            clearInterval();

            // sanity checks
            if (Build.VERSION.SDK_INT >= 28) {
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
        // sanity checks
        if (locationManager == null) return;

        if (interval != 0) {
            locationManager.removeUpdates(places_locationListener);
            interval = 0;
        }
    }

    public void refresh() {
        // sanity checks
        if (locationManager == null) return;
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) return;
        if (Build.VERSION.SDK_INT >= 28) {
            if (!locationManager.isLocationEnabled()) return;
        }

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

                for (int i=0; i < unfilteredList.size(); i++) {
                    WaypointListItem point = (WaypointListItem) unfilteredList.get(i);
                    point.updateDistance(location);
                }

                Collections.sort(unfilteredList, WaypointListItem.distanceOrderComparator);
                return null;
            }
 
            @Override
            protected void onPostExecute(final Void result) {
                recyclerFilterableAdapter.refresh();
            }
        }.execute();
    }

    private boolean has_permission() {
        if (Build.VERSION.SDK_INT < 23) {
            return true;
        } else {
            String permission = Manifest.permission.ACCESS_FINE_LOCATION;

            return (activity.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED);
        }
    }

    private void request_permission() {
        String permission = Manifest.permission.ACCESS_FINE_LOCATION;

        // Make request on behalf of Activity.
        // The Activity will need to override "onRequestPermissionsResult",
        // and proxy the call to the same-name handler in this class.
        activity.requestPermissions(new String[]{permission}, PERMISSIONS_REQUEST_CODE);
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CODE: {
                if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    locationManager = (LocationManager)activity.getSystemService(Context.LOCATION_SERVICE);
                    int seconds     = interval;
                    interval        = 0;

                    setInterval(seconds);
                } else {
                    // permission denied
                    interval        = 0;
                }
            }
        }
    }
}
