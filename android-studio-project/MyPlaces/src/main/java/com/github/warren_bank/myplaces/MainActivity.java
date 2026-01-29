package com.github.warren_bank.myplaces;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import net.rdrei.android.dirchooser.DirectoryChooserActivity;
import net.rdrei.android.dirchooser.DirectoryChooserConfig;

public class MainActivity extends AppCompatActivity {

    // ---------------------------------------------------------------------------------------------
    // Data Structures:
    // ---------------------------------------------------------------------------------------------

    // Stored Preferences --------------------------------------------------------------------------

    private static final String PREFS_FILENAME = "MainActivity";
    private static final String PREF_DIRPATH = "DIRPATH";

    // Intent Request IDs --------------------------------------------------------------------------

    private static final int INTENT_REQUEST_DIRECTORY = 0;

    // Intent Extras -------------------------------------------------------------------------------

    public static final String INTENT_EXTRA_FILEPATH = "FILEPATH";
    public static final String INTENT_EXTRA_FILENAME = "FILENAME";
    public static final String INTENT_EXTRA_FORMAT   = "FORMAT";

    // Nested Class Definition ---------------------------------------------------------------------

    private static final class FileListItem implements Comparable<FileListItem> {
        public final String filepath;
        public final String filename;
        public final String format;

        public FileListItem(String filepath, String filename, String format) {
            this.filepath = filepath;
            this.filename = filename;
            this.format   = format;
        }

        @Override
        public String toString() {
            return filename;
        }

        @Override
        public int compareTo(FileListItem that) {
            if (that == null) throw new NullPointerException();

            return this.filename.compareTo(that.filename);
        }

        public boolean equals(FileListItem that) {
            if (that == null) return false;

            return this.filepath.equals(that.filepath);
        }

        public boolean equals(String that_filepath) {
            return this.filepath.equals(that_filepath);
        }

        public static ArrayList<FileListItem> fromDirectory(File directory) {
            ArrayList<FileListItem> arrayList = new ArrayList<FileListItem>();

            // sanity check
            if (!directory.isDirectory()) {
                return arrayList;
            }

            File[] allFiles = directory.listFiles();
            File   thisFile;
            String filepath;
            String filename;
            String extension;

            for (int i=0; i < allFiles.length; i++) {
                thisFile = allFiles[i];
                filename  = thisFile.getName();
                extension = null;

                if (filename.contains(".")) {
                    extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
                }

                if (extension != null) {
                    switch (extension) {
                        case "gpx":
                        case "kml":
                            filepath = thisFile.getPath();
                            arrayList.add(
                                new FileListItem(filepath, filename, extension)
                            );
                            break;
                        default:
                            break;
                    }
                }
            }

            Collections.sort(arrayList);
            return arrayList;
        }

        public static ArrayList<FileListItem> fromDirectory(String strDirpath) {
            File directory = new File(strDirpath);
            return fromDirectory(directory);
        }
    }

    // ListView ------------------------------------------------------------------------------------

    private ListView                   files_listView;
    private ArrayList<FileListItem>    files_arrayList;
    private ArrayAdapter<FileListItem> files_arrayAdapter;

    // ---------------------------------------------------------------------------------------------
    // Lifecycle Events:
    // ---------------------------------------------------------------------------------------------

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Toolbar
        initToolbar();

        // ListView
        initListView();
    }

    // ---------------------------------------------------------------------------------------------
    // ActionBar:
    // ---------------------------------------------------------------------------------------------

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {

        switch(menuItem.getItemId()) {

            case R.id.action_change_directory: {
                openDirectoryChooser();
                return true;
            }

            case R.id.action_exit: {
                ExitActivity.open(MainActivity.this);
                return true;
            }

            default: {
                return super.onOptionsItemSelected(menuItem);
            }
        }
    }

    private void openDirectoryChooser() {
        String initialDirectory = getDirpath();
        if (initialDirectory == null) initialDirectory = Environment.getExternalStorageDirectory().getAbsolutePath();

        final Intent in = new Intent(
                  MainActivity.this,
                  DirectoryChooserActivity.class);

        final DirectoryChooserConfig config = DirectoryChooserConfig.builder()
                  .initialDirectory(initialDirectory)
                  .build();

        in.putExtra(
                  DirectoryChooserActivity.EXTRA_CONFIG,
                  config);

        startActivityForResult(in, INTENT_REQUEST_DIRECTORY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == INTENT_REQUEST_DIRECTORY) {
            if (resultCode == DirectoryChooserActivity.RESULT_CODE_DIR_SELECTED) {
                String strDirpath = data.getStringExtra(DirectoryChooserActivity.RESULT_SELECTED_DIR);

                // update ListView
                populateListView(strDirpath);

                // remember directory path
                saveDirpath(strDirpath);
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Persistent Data:
    // ---------------------------------------------------------------------------------------------

    private String cacheDirpath = null;

    private void saveDirpath(String strDirpath) {
        SharedPreferences prefs = getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor prefs_editor = prefs.edit();
        prefs_editor.putString(PREF_DIRPATH, strDirpath);
        prefs_editor.apply();

        cacheDirpath = strDirpath;
    }

    private String getDirpath() {
        if (cacheDirpath == null) {
          SharedPreferences prefs = getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE);
          cacheDirpath = prefs.getString(PREF_DIRPATH, null);
        }
        return cacheDirpath;
    }

    // ---------------------------------------------------------------------------------------------
    // internal:
    // ---------------------------------------------------------------------------------------------

    private void initToolbar() {
        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");
        toolbar.setNavigationIcon(null);
    }

    private void initListView() {
        files_listView     = (ListView)findViewById(R.id.lv_files);
        files_arrayList    = new ArrayList<FileListItem>();
        files_arrayAdapter = new ArrayAdapter<FileListItem>(MainActivity.this, android.R.layout.simple_list_item_1, files_arrayList);
            // https://github.com/aosp-mirror/platform_frameworks_base/tree/master/core/res/res/layout
            // https://github.com/aosp-mirror/platform_frameworks_base/blob/master/core/res/res/layout/simple_list_item_1.xml

        files_listView.setAdapter(files_arrayAdapter);

        files_listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                FileListItem item = (FileListItem) parent.getItemAtPosition(position);
                viewPlaces(item);
            }
        });

        String strDirpath = getDirpath();
        if (strDirpath == null) {
            openDirectoryChooser();
        }
        else {
            populateListView(strDirpath);
        }
    }

    private void populateListView(String strDirpath) {
        if (strDirpath == null) return;

        // update the ArrayList
        files_arrayList.clear();
        files_arrayList.addAll(
            FileListItem.fromDirectory(strDirpath)
        );

        // notify the ListView adapter
        files_arrayAdapter.notifyDataSetChanged();
    }

    private void viewPlaces(FileListItem item) {
        Intent in = new Intent(MainActivity.this, PlacesActivity.class);

        in.putExtra(INTENT_EXTRA_FILEPATH, item.filepath);
        in.putExtra(INTENT_EXTRA_FILENAME, item.filename);
        in.putExtra(INTENT_EXTRA_FORMAT,   item.format);

        startActivity(in);
    }

}
