package com.example.dropboxapidemo.Dropbox;

import android.content.Context;
import android.util.Log;

import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.android.Auth;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.files.SearchMatch;
import com.dropbox.core.v2.files.SearchResult;
import com.dropbox.core.v2.files.WriteMode;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.List;

public class DropboxManager {

    private Context mContext;
    private DbxClientV2 mClient;

    private final static String DROP_BOX_KEY = "YOUR_DROPBOX_KEY";

    /**
     * Constructor.
     *
     * @ param context Context
     */
    public DropboxManager(Context context) {
        mContext = context;
        DbxRequestConfig config = new DbxRequestConfig("Name/Version");
        mClient = new DbxClientV2(config, DROP_BOX_KEY);
    }

    /**
     * Start the authentication process.
     * Authentication page opens.
     */
    public void startAuthenticate() {
        Auth.startOAuth2Authentication(mContext, DROP_BOX_KEY);
    }

    /**
     * Obtain an authentication token.
     *
     * @return authentication token.
     */
    public String getAccessToken() {
        return Auth.getOAuth2Token();
    }

     /**
     * Restore files.
     * @ param srcFilePath Restore source file path (Dropbox)
     * @ param dstFilePath Restore destination file path (Android)
     * @ return Returns true if it succeeds, false if it fails
     */
    public boolean restore(String srcFilePath, String dstFilePath) {
        try {

            SearchResult result = mClient.files().search("", srcFilePath);
            List<SearchMatch> matches = result.getMatches();
            //Log.d("matches.size(): " + matches.size());

            Metadata metadata = null;
            for (SearchMatch match : matches) {
                metadata = match.getMetadata();
                break;
            }

            if (metadata == null) {
                // If the file is not found
                Log.d("tag", "metadata not found The");
                return false;
            } else {
                Log.d("tag", "Metadata.GetPathLower () : " + metadata.getPathLower());
            }

            // download, replace the file
            File File = new File(dstFilePath);
            OutputStream Os = new FileOutputStream(File);
            mClient.files().download(metadata.getPathLower()).download(Os);

        } catch (Exception e) {
            Log.d("tag", "Download Error:" + e);
            return false;
        }
        return true;
    }

    /**
     * Back up files.
     * @ param srcFilePath Save file path (Android)
     * @ param dstFilePath Save file path (Dropbox)
     * @ return Returns true if it succeeds, false if it fails
     */

    public boolean backup(String srcFilePath, String dstFilePath) {
        try {
            File file = new File(srcFilePath);
            InputStream input = new FileInputStream(file);
            byte[] bytes = convertFileToBytes(file);
            // Dropbox file path since it is necessary to specify "/" at the beginning, put a "/" at the beginning of the file path
            mClient.files().uploadBuilder("/" + dstFilePath)
                    .withMode(WriteMode.OVERWRITE)
                    .uploadAndFinish(input);

        } catch (Exception e) {
            Log.e("tag", "Upload Error: " + e);
            return false;
        }
        return true;
    }

    /**
     * Convert File to bytes [].
     *
     * @return bytes
     * @throws IOException
     * @ param file file
     */
    private byte[] convertFileToBytes(File file) throws IOException {
        final long fileSize = file.length();
        final int byteSize = (int) fileSize;
        byte[] bytes = new byte[byteSize];
        try {
            RandomAccessFile raf = new RandomAccessFile(file, "r");
            try {
                raf.readFully(bytes);
            } finally {
                raf.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bytes;
    }

}
