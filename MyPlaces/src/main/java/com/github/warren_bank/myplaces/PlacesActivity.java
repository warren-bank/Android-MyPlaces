package com.github.warren_bank.myplaces;

import com.github.warren_bank.myplaces.helpers.DistanceFormatter;
import com.github.warren_bank.myplaces.models.WaypointListItem;
import com.github.warren_bank.myplaces.services.PlacesLocationManager;

import com.github.warren_bank.filterablerecyclerview.FilterableListItem;
import com.github.warren_bank.filterablerecyclerview.FilterableListItemOnClickListener;
import com.github.warren_bank.filterablerecyclerview.FilterableViewHolder;
import com.github.warren_bank.filterablerecyclerview.FilterableAdapter;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Filter;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PlacesActivity extends AppCompatActivity implements FilterableListItemOnClickListener {

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

    private List<FilterableListItem>    unfilteredList;
    private FilterableAdapter           recyclerFilterableAdapter;
    private RecyclerView                recyclerView;

    private Filter                      searchFilter;
    private SearchView                  searchView;

    public class PlacesFilterableViewHolder extends FilterableViewHolder {
        private TextView text1;
        private TextView text2;

        public PlacesFilterableViewHolder(
            View view,
            List<FilterableListItem> filteredList,
            FilterableListItemOnClickListener listener
        ) {
            super(view, filteredList, listener);
        }

        @Override
        public void onCreate(View view) {
            text1 = view.findViewById(android.R.id.text1);
            text2 = view.findViewById(android.R.id.text2);
        }

        @Override
        public void onUpdate(FilterableListItem filterableListItem) {
            WaypointListItem place = (WaypointListItem) filterableListItem;

            text1.setText(place.name);

            if (sort_order == SORT_OPTION.DISTANCE) {
                if (place.distance == 0) {
                    text2.setText("finding current location..");
                }
                else {
                    text2.setText(
                        DistanceFormatter.format((int)place.distance)  // cast float to int
                    );
                }
                text2.setVisibility(View.VISIBLE);
            }
            else {
                text2.setVisibility(View.GONE);
            }
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

        searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        initSearch();

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {

        switch(menuItem.getItemId()) {

            case R.id.action_search: {
                return true;
            }

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

    @Override
    public void onBackPressed() {
        if (!searchView.isIconified()) {
            searchView.setIconified(true);
            return;
        }
        super.onBackPressed();
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
        unfilteredList = WaypointListItem.fromFile(filepath, format);

        recyclerFilterableAdapter  = new FilterableAdapter(
            R.layout.two_line_list_item,
            unfilteredList,
            PlacesActivity.this,
            PlacesFilterableViewHolder.class,
            PlacesActivity.class,
            PlacesActivity.this
        );

        recyclerView = findViewById(R.id.rv_places);
        recyclerView.setLayoutManager(new LinearLayoutManager(PlacesActivity.this));
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(recyclerFilterableAdapter);

        // add divider between list items
        recyclerView.addItemDecoration(
            new DividerItemDecoration(PlacesActivity.this, DividerItemDecoration.VERTICAL)
        );

        searchFilter = recyclerFilterableAdapter.getFilter();
    }

    private void initSort() {
        // order immediately after data is extracted from file
        sort_order = SORT_OPTION.SEQUENTIAL;

        places_locationManager = new PlacesLocationManager(
            PlacesActivity.this,
            unfilteredList,
            recyclerFilterableAdapter
        );
    }

    private void initSearch() {
        searchView.setMaxWidth(Integer.MAX_VALUE);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchFilter.filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                searchFilter.filter(query);
                return false;
            }
        });
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
                Collections.sort(unfilteredList, comparator);
                return null;
            }
 
            @Override
            protected void onPostExecute(final Void result) {
                recyclerFilterableAdapter.notifyDataSetChanged();
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

    @Override
    public void onFilterableListItemClick(FilterableListItem item) {
        WaypointListItem place = (WaypointListItem) item;

        viewPlace(place);
    }
}
