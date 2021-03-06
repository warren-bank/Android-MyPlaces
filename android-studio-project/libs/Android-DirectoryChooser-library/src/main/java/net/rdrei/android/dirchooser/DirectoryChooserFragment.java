package net.rdrei.android.dirchooser;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileObserver;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.gu.option.Option;
import com.gu.option.UnitFunction;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Activities that contain this fragment must implement the
 * {@link OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link DirectoryChooserFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DirectoryChooserFragment extends DialogFragment {
    public  static final String KEY_CURRENT_DIRECTORY = "CURRENT_DIRECTORY";
    private static final String ARG_CONFIG = "CONFIG";
    private static final String TAG = DirectoryChooserFragment.class.getSimpleName();
    private static final int PERMISSIONS_REQUEST_CODE = 0;
    private String mInitialDirectory;

    private Option<OnFragmentInteractionListener> mListener = Option.none();

    private Button mBtnConfirm;
    private Button mBtnCancel;
    private ImageButton mBtnNavUp;
    private TextView mTxtvSelectedFolder;
    private ListView mListDirectories;

    private ArrayAdapter<String> mListDirectoriesAdapter;
    private List<String> mFilenames;
    /**
     * The directory that is currently being shown.
     */
    private File mSelectedDir;
    private File[] mFilesInDir;
    private FileObserver mFileObserver;
    private DirectoryChooserConfig mConfig;

    public DirectoryChooserFragment() {
        // Required empty public constructor
    }

    /**
     * To create the config, make use of the provided
     * {@link DirectoryChooserConfig#builder()}.
     *
     * @return A new instance of DirectoryChooserFragment.
     */
    public static DirectoryChooserFragment newInstance(@NonNull final DirectoryChooserConfig config) {
        final DirectoryChooserFragment fragment = new DirectoryChooserFragment();
        final Bundle args = new Bundle();
        args.putParcelable(ARG_CONFIG, config);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mSelectedDir != null) {
            outState.putString(KEY_CURRENT_DIRECTORY, mSelectedDir.getAbsolutePath());
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() == null) {
            throw new IllegalArgumentException(
                    "You must create DirectoryChooserFragment via newInstance().");
        }
        mConfig = getArguments().getParcelable(ARG_CONFIG);

        if (mConfig == null) {
            throw new NullPointerException("No ARG_CONFIG provided for DirectoryChooserFragment " +
                    "creation.");
        }

        mInitialDirectory  = mConfig.initialDirectory();

        if (savedInstanceState != null) {
            mInitialDirectory = savedInstanceState.getString(KEY_CURRENT_DIRECTORY);
        }

        if (getShowsDialog()) {
            setStyle(DialogFragment.STYLE_NO_TITLE, 0);
        } else {
            setHasOptionsMenu(false);
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {

        assert getActivity() != null;
        final View view = inflater.inflate(R.layout.directory_chooser, container, false);

        mBtnConfirm = (Button) view.findViewById(R.id.btnConfirm);
        mBtnCancel = (Button) view.findViewById(R.id.btnCancel);
        mBtnNavUp = (ImageButton) view.findViewById(R.id.btnNavUp);
        mTxtvSelectedFolder = (TextView) view.findViewById(R.id.txtvSelectedFolder);
        mListDirectories = (ListView) view.findViewById(R.id.directoryList);

        mBtnConfirm.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(final View v) {
                if (isValidFile(mSelectedDir)) {
                    returnSelectedFolder();
                }
            }
        });

        mBtnCancel.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(final View v) {
                mListener.foreach(new UnitFunction<OnFragmentInteractionListener>() {
                    @Override
                    public void apply(final OnFragmentInteractionListener listener) {
                        listener.onCancelChooser();
                    }
                });
            }
        });

        mListDirectories.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(final AdapterView<?> parent, final View view,
                    final int position, final long id) {
                debug("Selected index: %d", position);
                if (mFilesInDir != null && position >= 0
                        && position < mFilesInDir.length) {
                    changeDirectory(mFilesInDir[position]);
                }
            }
        });

        mBtnNavUp.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(final View v) {
                final File parent;
                if (mSelectedDir != null
                        && (parent = mSelectedDir.getParentFile()) != null) {
                    changeDirectory(parent);
                }
            }
        });

        adjustResourceLightness();

        mFilenames = new ArrayList<>();
        mListDirectoriesAdapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_list_item_1, mFilenames);
        mListDirectories.setAdapter(mListDirectoriesAdapter);

        checkPermissionsAndOpenInitialDirectory();

        return view;
    }

    private void openInitialDirectory() {
        final File initialDir;
        if ((mInitialDirectory != null) && (!mInitialDirectory.isEmpty()) && isValidFile(new File(mInitialDirectory))) {
            initialDir = new File(mInitialDirectory);
        } else {
            initialDir = Environment.getExternalStorageDirectory();
        }

        changeDirectory(initialDir);
    }

    private void checkPermissionsAndOpenInitialDirectory() {
        if (Build.VERSION.SDK_INT < 23) {
            openInitialDirectory();
        } else {
            String permission = Manifest.permission.READ_EXTERNAL_STORAGE;

            if (ContextCompat.checkSelfPermission(getContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{permission}, PERMISSIONS_REQUEST_CODE);
            } else {
                openInitialDirectory();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CODE: {
                if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    openInitialDirectory();
                } else {
                    // permission denied: cancel
                    mSelectedDir = null;
                    returnSelectedFolder();
                }
            }
        }
    }

    private void adjustResourceLightness() {
        // change up button to light version if using dark theme
        int color = 0xFFFFFF;
        final Resources.Theme theme = getActivity().getTheme();

        if (theme != null) {
            final TypedArray backgroundAttributes = theme.obtainStyledAttributes(
                    new int[]{android.R.attr.colorBackground});

            if (backgroundAttributes != null) {
                color = backgroundAttributes.getColor(0, 0xFFFFFF);
                backgroundAttributes.recycle();
            }
        }

        // convert to greyscale and check if < 128
        if (color != 0xFFFFFF && 0.21 * Color.red(color) +
                0.72 * Color.green(color) +
                0.07 * Color.blue(color) < 128) {
            mBtnNavUp.setImageResource(R.drawable.navigation_up_light);
        }
    }

    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);

        if (activity instanceof OnFragmentInteractionListener) {
            mListener = Option.some((OnFragmentInteractionListener) activity);
        } else {
            Fragment owner = getTargetFragment();
            if (owner instanceof OnFragmentInteractionListener) {
                mListener = Option.some((OnFragmentInteractionListener) owner);
            }
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mFileObserver != null) {
            mFileObserver.stopWatching();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mFileObserver != null) {
            mFileObserver.startWatching();
        }
    }

    private static void debug(final String message, final Object... args) {
        Log.d(TAG, String.format(message, args));
    }

    /**
     * Change the directory that is currently being displayed.
     *
     * @param dir The file the activity should switch to. This File must be
     *            non-null and a directory, otherwise the displayed directory
     *            will not be changed
     */
    private void changeDirectory(final File dir) {
        if (dir == null) {
            debug("Could not change folder: dir was null");
        } else if (!dir.isDirectory()) {
            debug("Could not change folder: dir is no directory");
        } else {
            final File[] contents = dir.listFiles();
            if (contents != null) {
                int numDirectories = 0;
                for (final File f : contents) {
                    if (f.isDirectory()) {
                        numDirectories++;
                    }
                }
                mFilesInDir = new File[numDirectories];
                mFilenames.clear();
                for (int i = 0, counter = 0; i < numDirectories; counter++) {
                    if (contents[counter].isDirectory()) {
                        mFilesInDir[i] = contents[counter];
                        mFilenames.add(contents[counter].getName());
                        i++;
                    }
                }
                Arrays.sort(mFilesInDir);
                Collections.sort(mFilenames);
                mSelectedDir = dir;
                mTxtvSelectedFolder.setText(dir.getAbsolutePath());
                mListDirectoriesAdapter.notifyDataSetChanged();
                mFileObserver = createFileObserver(dir.getAbsolutePath());
                mFileObserver.startWatching();
                debug("Changed directory to %s", dir.getAbsolutePath());
            } else {
                debug("Could not change folder: contents of dir were null");
            }
        }
        refreshButtonState();
    }

    /**
     * Changes the state of the buttons depending on the currently selected file
     * or folder.
     */
    private void refreshButtonState() {
        final Activity activity = getActivity();
        if (activity != null && mSelectedDir != null) {
            mBtnConfirm.setEnabled(isValidFile(mSelectedDir));
            activity.invalidateOptionsMenu();
        }
    }

    /**
     * Refresh the contents of the directory that is currently shown.
     */
    private void refreshDirectory() {
        if (mSelectedDir != null) {
            changeDirectory(mSelectedDir);
        }
    }

    /**
     * Sets up a FileObserver to watch the current directory.
     */
    private FileObserver createFileObserver(final String path) {
        return new FileObserver(path, FileObserver.CREATE | FileObserver.DELETE
                | FileObserver.MOVED_FROM | FileObserver.MOVED_TO) {

            @Override
            public void onEvent(final int event, final String path) {
                debug("FileObserver received event %d", event);
                final Activity activity = getActivity();

                if (activity != null) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            refreshDirectory();
                        }
                    });
                }
            }
        };
    }

    /**
     * Returns the selected folder as a result to the activity the fragment's attached to. The
     * selected folder can also be null.
     */
    private void returnSelectedFolder() {
        if (mSelectedDir != null) {
            debug("Returning %s as result", mSelectedDir.getAbsolutePath());
            mListener.foreach(new UnitFunction<OnFragmentInteractionListener>() {
                @Override
                public void apply(final OnFragmentInteractionListener f) {
                    f.onSelectDirectory(mSelectedDir.getAbsolutePath());
                }
            });
        } else {
            mListener.foreach(new UnitFunction<OnFragmentInteractionListener>() {
                @Override
                public void apply(final OnFragmentInteractionListener f) {
                    f.onCancelChooser();
                }
            });
        }

    }

    /**
     * Returns true if the selected file or directory would be valid selection.
     */
    private boolean isValidFile(final File file) {
        return (file != null && file.isDirectory() && file.canRead());
    }

    @Nullable
    public OnFragmentInteractionListener getDirectoryChooserListener() {
        return mListener.get();
    }

    public void setDirectoryChooserListener(@Nullable final OnFragmentInteractionListener listener) {
        mListener = Option.option(listener);
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        /**
         * Triggered when the user successfully selected their destination directory.
         */
        void onSelectDirectory(@NonNull String path);

        /**
         * Advices the activity to remove the current fragment.
         */
        void onCancelChooser();
    }

}
