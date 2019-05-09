package com.github.warren_bank.myplaces;

import com.github.warren_bank.myplaces.models.WaypointListItem;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
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

    private String filepath;
    private String filename;
    private String format;
    private SORT_OPTION sort_order;

    // ---------------------------------------------------------------------------------------------
    // RecyclerView:
    // ---------------------------------------------------------------------------------------------

    private RecyclerView                places_recyclerView;
    private ArrayList<WaypointListItem> places_arrayList;
    private PlacesListAdapter           places_arrayAdapter;

    private class PlaceItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public final TextView textView;

        public PlaceItemViewHolder(TextView textView) {
            super(textView);
            this.textView = textView;
            textView.setOnClickListener(this);
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
            TextView textView = (TextView) LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
                // https://github.com/aosp-mirror/platform_frameworks_base/tree/master/core/res/res/layout
                // https://github.com/aosp-mirror/platform_frameworks_base/blob/master/core/res/res/layout/simple_list_item_1.xml
            return new PlaceItemViewHolder(textView);
        }

        @Override
        public void onBindViewHolder(PlaceItemViewHolder holder, int position) {
            WaypointListItem place = places_arrayList.get(position);
            TextView view = holder.textView;
            view.setText(place.name);
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
                sortRecyclerView();
                invalidateOptionsMenu();
                return true;
            }

            case R.id.action_sort_alphabetic: {
                sort_order = SORT_OPTION.ALPHABETIC;
                sortRecyclerView();
                invalidateOptionsMenu();
                return true;
            }

            case R.id.action_sort_distance: {
                sort_order = SORT_OPTION.DISTANCE;
                calculateDistance();
                sortRecyclerView();
                invalidateOptionsMenu();
                return true;
            }

            case R.id.action_sort_refresh: {
                if (sort_order == SORT_OPTION.DISTANCE) {
                    calculateDistance();
                    sortRecyclerView();
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

    // to do
    private void calculateDistance() {
    }

    private void viewPlace(WaypointListItem place) {
        String uri;
        Intent in;
        PackageManager pm = getPackageManager();

        // navigation
        uri = "google.navigation:q=" + place.lat + "," + place.lon;
        in  = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        if (in.resolveActivity(pm) != null) {
            startActivity(in);
            return;
        }

        // mapping
        uri = "geo:" + place.lat + "," + place.lon;
        in  = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        if (in.resolveActivity(pm) != null) {
            startActivity(in);
            return;
        }

        Toast.makeText(PlacesActivity.this, "No mapping app found", Toast.LENGTH_SHORT).show();
    }
}
