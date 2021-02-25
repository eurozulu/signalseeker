package org.spoofer.signalseeker.celldb;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;

public class CellResources {

    private final Context context;

    private static final String DB_FILE_EXTENSION = "sqlite";

    public CellResources(Context context) {
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
        CellDatabase db = new CellDatabase();
        db.openDatabase(dbfile.getAbsolutePath());
        return db;
    }


    public void downloadDatabase(String name) throws IOException {
        String webpath = TextUtils.join("/", new Object[]{getMapRootLocation(), getDBFilename(name)});
        File dbfile = getDBFile(name);
        URL url = new URL(webpath);
        HttpsURLConnection cnt = (HttpsURLConnection) url.openConnection();
        InputStream in = cnt.getInputStream();

        if (dbfile.exists()) {
            dbfile.delete();
        }

        FileOutputStream out = new FileOutputStream(dbfile);

        try {
            byte[] buf = new byte[2048];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }

        } finally {
            out.close();
            in.close();
        }
    }


    public String getMapRootLocation() {
        return "https://cdn.radiocells.org";
    }

    public File getRootStorage() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return context.getExternalFilesDir(null);
        }
        return context.getFilesDir();
    }

    public String lookupName(String country) {
        //https://stackoverflow.com/questions/28503225/get-country-code-from-country-name-in-android
        // Get all country codes in a string array.
        String[] isoCountryCodes = Locale.getISOCountries();
        Locale locale;
        String foundCode = "";

        // Iterate through all country codes:
        for (String code : isoCountryCodes) {
            // Create a locale using each country code
            locale = new Locale("", code);
            // Get country name for each code.
            if (country.equalsIgnoreCase(locale.getDisplayCountry())) {
                foundCode = code;
                break;
            }
        }
        return foundCode;
    }

    private String getDBFilename(String name) {
        return TextUtils.join(".", new Object[]{name.toLowerCase(), DB_FILE_EXTENSION});
    }

    private File getDBFile(String name) {
        File root = getRootStorage();
        return new File(root, getDBFilename(name));
    }
}
