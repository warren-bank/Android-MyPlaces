package com.github.warren_bank.myplaces;

import com.github.warren_bank.myplaces.models.WaypointListItem;
import com.github.warren_bank.myplaces.services.PlacesLocationManager;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class PlacesActivity extends AppCompatActivity {

    // ---------------------------------------------------------------------------------------------
    // Data Structures:
    // ---------------------------------------------------------------------------------------------

    private static enum SORT_OPTION { SEQUENTIAL, ALPHABETIC, DISTANCE }

    private String                      filepath;
    private String                      filename;
    private String                      format;
    private SORT_OPTION                 sort_order;
    private PlacesLocationManager       places_locationManager;

    private static final int GPS_INTERVAL = (60 * 5);  // 5 minutes, this is the period of time between GPS updates when sort_order is based on distance from the current location

    // ---------------------------------------------------------------------------------------------
    // RecyclerView:
    // ---------------------------------------------------------------------------------------------

    private RecyclerView                places_recyclerView;
    private ArrayList<WaypointListItem> places_arrayList;
    private PlacesListAdapter           places_arrayAdapter;

    private class PlaceItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public final TextView textView_1;
        public final TextView textView_2;

        public PlaceItemViewHolder(View v) {
            super(v);
            this.textView_1 = v.findViewById(android.R.id.text1);
            this.textView_2 = v.findViewById(android.R.id.text2);
            v.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int position = getAdapterPosition();
            WaypointListItem place = places_arrayList.get(position);
            viewPlace(place);
        }
    }

    private class PlacesListAdapter extends RecyclerView.Adapter<PlaceItemViewHolder> {
        @Override
        public PlaceItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = (View) LayoutInflater.from(parent.getContext()).inflate(R.layout.two_line_list_item, parent, false);
                // https://github.com/aosp-mirror/platform_frameworks_base/tree/master/core/res/res/layout
                // https://github.com/aosp-mirror/platform_frameworks_base/blob/master/core/res/res/layout/two_line_list_item.xml
            return new PlaceItemViewHolder(v);
        }

        @Override
        public void onBindViewHolder(PlaceItemViewHolder holder, int position) {
            WaypointListItem place = places_arrayList.get(position);
            holder.textView_1.setText(place.name);

            if (sort_order == SORT_OPTION.DISTANCE) {
                holder.textView_2.setText(place.distance + " meters");
                holder.textView_2.setVisibility(View.VISIBLE);
            }
            else {
                holder.textView_2.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return places_arrayList.size();
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Lifecycle Events:
    // ---------------------------------------------------------------------------------------------

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_places);

        // read extras:
        initExtras();

        // Toolbar
        initToolbar();

        // RecyclerView
        initRecyclerView();

        // sort
        initSort();
    }

    @Override
    public void onPause() {
        super.onPause();

        places_locationManager.clearInterval();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (sort_order == SORT_OPTION.DISTANCE) {
            places_locationManager.setInterval(GPS_INTERVAL);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // ActionBar:
    // ---------------------------------------------------------------------------------------------

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_places, menu);

        menu.findItem(R.id.action_sort_refresh).setVisible(sort_order == SORT_OPTION.DISTANCE);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {

        switch(menuItem.getItemId()) {

            case R.id.action_sort_sequential: {
                sort_order = SORT_OPTION.SEQUENTIAL;  // the sequential order in which places naturally occur in the XML file
                places_locationManager.clearInterval();
                sortRecyclerView();
                invalidateOptionsMenu();
                return true;
            }

            case R.id.action_sort_alphabetic: {
                sort_order = SORT_OPTION.ALPHABETIC;
                places_locationManager.clearInterval();
                sortRecyclerView();
                invalidateOptionsMenu();
                return true;
            }

            case R.id.action_sort_distance: {
                sort_order = SORT_OPTION.DISTANCE;
                sortRecyclerView();                 // (a) perform an immediate sort based on previously calculated distances,
                                                    // (b) when an updated position is received, then recalculate all distances and sort again
                places_locationManager.setInterval(GPS_INTERVAL);
                invalidateOptionsMenu();
                return true;
            }

            case R.id.action_sort_refresh: {
                if (sort_order == SORT_OPTION.DISTANCE) {
                    places_locationManager.refresh();
                }
                return true;
            }

            case R.id.action_exit: {
                finish();
                return true;
            }

            default: {
                return super.onOptionsItemSelected(menuItem);
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // internal:
    // ---------------------------------------------------------------------------------------------

    private void initExtras() {
        Intent in = getIntent();
        if (in == null) return;

        filepath = in.getStringExtra(MainActivity.INTENT_EXTRA_FILEPATH);
        filename = in.getStringExtra(MainActivity.INTENT_EXTRA_FILENAME);
        format   = in.getStringExtra(MainActivity.INTENT_EXTRA_FORMAT);
    }

    private void initToolbar() {
        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        String title = (filename == null) ? "" : filename;

        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(title);

        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

    private void initRecyclerView() {
        places_arrayList    = WaypointListItem.fromFile(filepath, format);
        places_arrayAdapter = new PlacesListAdapter();
        places_recyclerView = (RecyclerView)findViewById(R.id.rv_places);

        places_recyclerView.setLayoutManager(new LinearLayoutManager(PlacesActivity.this));
        places_recyclerView.setHasFixedSize(true);
        places_recyclerView.setAdapter(places_arrayAdapter);

        // add divider between list items
        places_recyclerView.addItemDecoration(
            new DividerItemDecoration(
                    PlacesActivity.this,
                    DividerItemDecoration.VERTICAL
                )
        );
    }

    private void initSort() {
        // order immediately after data is extracted from file
        sort_order = SORT_OPTION.SEQUENTIAL;

        places_locationManager = new PlacesLocationManager(
            PlacesActivity.this,
            places_arrayList,
            places_arrayAdapter
        );
    }

    private void sortRecyclerView() {
        final Comparator comparator;

        switch(sort_order) {
            case SEQUENTIAL:
                comparator = WaypointListItem.sequentialOrderComparator;
                break;
            case ALPHABETIC:
                comparator = WaypointListItem.alphabeticOrderComparator;
                break;
            case DISTANCE:
                comparator = WaypointListItem.distanceOrderComparator;
                break;
            default:
                comparator = null;
                break;
        }

        if (comparator == null) return;

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(final Void ... params) {
                Collections.sort(places_arrayList, comparator);
                return null;
            }
 
            @Override
            protected void onPostExecute(final Void result) {
                places_arrayAdapter.notifyDataSetChanged();
            }
        }.execute();
    }

    private void viewPlace(WaypointListItem place) {
        String uri;
        Intent in;
        PackageManager pm = getPackageManager();

        // search:
        //     format: "geo:0,0?q=latitude,longitude(label)"
        //     G-Maps: https://developers.google.com/maps/documentation/urls/android-intents#search_for_a_location
        //     OsmAnd: https://github.com/osmandapp/Osmand/blob/f9cd73d4f02b45c429a94e5444124cc6f3463111/OsmAnd/AndroidManifest.xml#L357
        uri = "geo:0,0?q=" + place.lat + "," + place.lon + "(" + Uri.encode(place.name).replace("(","%28").replace(")","%29") + ")";
        in  = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        if (in.resolveActivity(pm) != null) {
            startActivity(in);
            return;
        }

        // navigation:
        //     format: "google.navigation:q=latitude,longitude"
        //     G-Maps: https://developers.google.com/maps/documentation/urls/android-intents#launch_turn-by-turn_navigation
        //     OsmAnd: https://github.com/osmandapp/Osmand/blob/f9cd73d4f02b45c429a94e5444124cc6f3463111/OsmAnd/AndroidManifest.xml#L289
        uri = "google.navigation:q=" + place.lat + "," + place.lon;
        in  = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        if (in.resolveActivity(pm) != null) {
            startActivity(in);
            return;
        }

        // mapping:
        //     format: "geo:latitude,longitude?z=zoom"
        //     G-Maps: https://developers.google.com/maps/documentation/urls/android-intents#display_a_map
        //     OsmAnd: https://github.com/osmandapp/Osmand/blob/f9cd73d4f02b45c429a94e5444124cc6f3463111/OsmAnd/AndroidManifest.xml#L357
        uri = "geo:" + place.lat + "," + place.lon;
        in  = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        if (in.resolveActivity(pm) != null) {
            startActivity(in);
            return;
        }

        Toast.makeText(PlacesActivity.this, "No mapping app found", Toast.LENGTH_SHORT).show();
    }
}
