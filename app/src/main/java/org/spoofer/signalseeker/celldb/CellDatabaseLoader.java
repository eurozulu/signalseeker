package org.spoofer.signalseeker.celldb;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;

public class CellDatabaseLoader {

    private final Context context;

    private static final String DB_FILE_EXTENSION = "sqlite";
    private static final String WEB_ROOT_URL = "https://cdn.radiocells.org";

    public CellDatabaseLoader(Context context) {
        this.context = context;
    }


    public boolean hasDatabase(String name) {
        File f = getDBFile(name);
        return (f.exists() && f.canRead() && !f.isDirectory());
    }

    public CellDatabase getDatabase(String name) throws IOException {
        if (!hasDatabase(name))
            throw new IOException("database not found");

        File dbfile = getDBFile(name);
        return new CellDatabase(dbfile.getAbsolutePath());
    }

    public long downloadDatabase(String name) {
        String target = getDBFilename(name);
        Uri src = Uri.parse(TextUtils.join("/", new String[]{WEB_ROOT_URL, target}));
        DownloadManager.Request req = new DownloadManager.Request(src);
        req.setTitle("Cell database for " + name);
        req.setDescription("Full country database for " + name);
        req.setDestinationInExternalPublicDir("", "celldatabases/" + target);

        DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        return dm.enqueue(req);
    }

    private File getRootStorage() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return context.getExternalFilesDir(null);
            //return context.getExternalFilesDir(null);
        }
        return context.getFilesDir();
    }

    private String getDBFilename(String name) {
        return TextUtils.join(".", new Object[]{name.toLowerCase(), DB_FILE_EXTENSION});
    }

    private File getDBFile(String name) {
        File root = getRootStorage();
        return new File(root, getDBFilename(name));
    }


}
