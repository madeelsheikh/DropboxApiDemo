package com.example.dropboxapidemo;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.core.v2.files.FileMetadata;
import com.example.dropboxapidemo.Dropbox.DownloadFileTask;
import com.example.dropboxapidemo.Dropbox.DropboxManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getName();

    private FileMetadata mFile;

    private DownloadFileTask download;

    // Replace APP_KEY from your APP_KEY
    final static private String APP_KEY = "YOUR APP_KEY";
    // Replace APP_SECRET from your APP_SECRET
    final static private String APP_SECRET = "YOUR APP_SECRET";

    private final static String DROP_BOX_KEY = "YOUR_DROPBOX_KEY";

    private DropboxAPI<AndroidAuthSession> mDBApi;

    private enum BackupAction {
            NONE,
            BACKUP_DROPBOX,
            RESTORE_DROPBOX,
    }

    private BackupAction mCurrentAction;
    private SharedPreferences mPref;
    private DropboxManager mDropboxManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPref = PreferenceManager.getDefaultSharedPreferences(this);
        mCurrentAction = BackupAction.NONE;

        (findViewById(R.id.btn_Download)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // perform the download if authenticated, in the case of unauthorized performs an authentication process

                if (mDropboxManager == null) {
                    mDropboxManager = new DropboxManager(getApplicationContext());
                }

                try {
                    // perform the download
                    new AsyncHttpRequest(MainActivity.this, BackupAction.RESTORE_DROPBOX).execute();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        // callback method
        initialize_session();


        // downloadFiles();
    }

    @Override
    protected void onResume() {
        super.onResume();
        switch (mCurrentAction) {
            case NONE:
                break;
            case BACKUP_DROPBOX:
                mCurrentAction = BackupAction.NONE;

                try {
                    String token = mDropboxManager.getAccessToken();
                    if (token == null) {
                        break;
                    }
                    mDropboxManager = new DropboxManager(this);

                    // order to save the authentication after the next time, to save the token
                    SharedPreferences.Editor Edit = mPref.edit();
                    Edit.putString("access_token", token);
                    Edit.commit();

                    // run the upload
                    new AsyncHttpRequest(this, BackupAction.BACKUP_DROPBOX).execute();

                } catch (IllegalStateException e) {
                    // IllegalStateException
                }
                break;
            case RESTORE_DROPBOX:
                mCurrentAction = BackupAction.NONE;

                try {
                    String token = mDropboxManager.getAccessToken();
                    if (token == null) {
                        break;
                    }

                    mDropboxManager = new DropboxManager(this);

                    // order to save the authentication after the next time, to save the token
                    SharedPreferences.Editor Edit = mPref.edit();
                    Edit.putString("access_token", token);
                    Edit.commit();

                    new AsyncHttpRequest(this, BackupAction.RESTORE_DROPBOX).execute();
                } catch (IllegalStateException e) {
                    // IllegalStateException
                }
                break;
            default:
                break;
        }
    }

    /**
     * Initialize the Session of the Key pair to authenticate with dropbox
     */
    protected void initialize_session() {
        try {
            // store app key and secret key
            AppKeyPair appKeys = new AppKeyPair(APP_KEY, APP_SECRET);
            AndroidAuthSession session = new AndroidAuthSession(appKeys);
            //Pass app key pair to the new DropboxAPI object.
            mDBApi = new DropboxAPI<AndroidAuthSession>(session);
            // MyActivity below should be your activity class name
            //start session
            mDBApi.getSession().setOAuth2AccessToken(DROP_BOX_KEY);
            Toast.makeText(getApplicationContext(), "Connected successfully..", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void createPath() {
        File database = getApplicationContext().getDatabasePath("voters.db");
        if (false == database.exists()) {
            //Copy db
            if (CopyDatabase()) {

            }
        }
    }

    private boolean CopyDatabase() {
        try {
            InputStream inputStream = getApplicationContext().getAssets().open("voters.db");
            String outFileName = "/data/data/com.example.dropboxapidemo/databases/voters.db";
            OutputStream outputStream = new FileOutputStream(outFileName);
            byte[] buff = new byte[1024];
            int length = 0;
            while ((length = inputStream.read(buff)) > 0) {
                outputStream.write(buff, 0, length);
            }
            outputStream.flush();
            outputStream.close();
            Log.w("MainActivity", "DB copied");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Called after asynchronous processing is over.
     * Display a message.
     */
    public void callback(BackupAction action, boolean result) {
        switch (action) {
            case BACKUP_DROPBOX:
                if (result) {
                    // success
                } else {
                    // failed
                }
                break;
            case RESTORE_DROPBOX:
                if (result) {
                    // success
                } else {
                    // failed
                }
                break;
            default:

                break;
        }
    }

    /**
     * An asynchronous processing task that makes an HTTP request for upload / download.
     */
    public class AsyncHttpRequest extends AsyncTask<Void, Void, Boolean> {

        private MainActivity activity;
        private BackupAction action;
        private ProgressDialog progressDialog;

        public AsyncHttpRequest(MainActivity activity, BackupAction action) {
            this.activity = activity;
            this.action = action;
        }

        @Override
        protected void onPreExecute() {
            // Produce the progress dialog

            this.progressDialog = new ProgressDialog(activity);
            switch (action) {
                case BACKUP_DROPBOX:
                    this.progressDialog.setTitle("Uploading ...");
                    this.progressDialog.setMessage("is being uploaded in this state, please wait.");
                    break;
                case RESTORE_DROPBOX:
                    this.progressDialog.setTitle("during the download ...");
                    this.progressDialog.setMessage("is downloading this remains Please wait for a while.");
                    break;
                default:
                    break;
            }
            this.progressDialog.setCancelable(false);  // Cancel Disable
            this.progressDialog.show();
            return;
        }

        @Override
        protected Boolean doInBackground(Void... builder) {
            boolean isSuccess = false;

            // Source File Name Example
            // /data/data/" + getPackageName() + "/YOUR FOLDER NAME/YOUR FILE NAME
            String srcFilePath = "SOURCE FILE NAME";  // SQLite in Android terminal Data
            String dstFilePath = "DESTINATION FILE NAME";  // Filename to be saved in Dropbox

            switch (action) {
                case BACKUP_DROPBOX: {
                    isSuccess = mDropboxManager.backup(srcFilePath, dstFilePath);
                    break;
                }
                case RESTORE_DROPBOX: {
                    isSuccess = mDropboxManager.restore(dstFilePath, srcFilePath);
                    break;
                }
                default:
                    break;
            }
            return isSuccess;
        }

        @Override
        protected void onPostExecute(Boolean Result) {
            // close the progress dialog
            if (this.progressDialog != null && this.progressDialog.isShowing()) {
                this.progressDialog.dismiss();
            }
            activity.callback(action, Result);
        }
    }
}
