package org.spoofer.signalseeker.celldb;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

// CellDatabase pre calculates the sine and cosign of the cell geo-coords and stores them in a seperate table.
// sqlite doesn't support trig functions so values are pre-calculated and looked up to calculate distance.
public class CellDatabase {

    private static final String TABLE_CELLS = "cell_zone";
    private static final String TABLE_CALC = "calculated";

    // Columns in the calculated table to hold results
    private static final String COL_ID = "_id";
    private static final String COL_CELL_ID = "cell_id";
    private static final String COL_LATITUDE_SIN = "latitude_rad_sin";
    private static final String COL_LATITUDE_COS = "latitude_rad_cos";
    private static final String COL_LONGITUDE_SIN = "longitude_rad_sin";
    private static final String COL_LONGITUDE_COS = "longitude_rad_cos";
    private static final String COL_DISTANCE = "distance";

    // Columns in the cell_zone table to read from
    private static final String[] CELL_COLS = new String[]{
            "_id", "latitude", "longitude"
    };

    private static final String DROP_CALC_TABLE = "DROP TABLE IF EXISTS " + TABLE_CALC;
    private static final String CREATE_CALC_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_CALC +
            "(" +
            COL_ID + " INTEGER PRIMARY KEY," + // Define a primary key
            COL_CELL_ID + " INTEGER UNIQUE," +
            COL_LATITUDE_SIN + " NUMERIC," +
            COL_LATITUDE_COS + " NUMERIC," +
            COL_LONGITUDE_SIN + " NUMERIC," +
            COL_LONGITUDE_COS + " NUMERIC" +
            ")";
    private static final String CREATE_INDEX = "CREATE UNIQUE INDEX IF NOT EXISTS i1 ON " + TABLE_CALC + "(" + COL_ID + ", " + COL_CELL_ID + ");";

    // params sin_lat_rad, cos_lat_rad, sin_lon_rad, cos_lon_rad
    private static String SELECT_CELLS = "SELECT " + COL_ID + "," + COL_CELL_ID + "," +
            "(" + COL_LATITUDE_SIN + " * %f + " + COL_LATITUDE_COS + " * %f * " +
            "(" + COL_LONGITUDE_SIN + " * %f + " + COL_LONGITUDE_COS + " * %f)) AS " + COL_DISTANCE +
            " FROM " + TABLE_CALC +
            " ORDER BY " + COL_DISTANCE + " DESC" +
            " LIMIT 25";


    private final String dbpath;

    private static final Object lock = new Object();
    private SQLiteDatabase db;

    public CellDatabase(String celldbpath) {
        super();
        this.dbpath = celldbpath;

    }

    public List<Cell> findLocalCells(double latitude, double longitude) {
        /*
        https://github.com/sozialhelden/wheelmap-android/wiki/Sqlite,-Distance-calculations
        SELECT "location",
        (sin_lat_rad * "sin_lat_rad" + cos_lat_rad * "cos_lat_rad" *
         (sin_lon_rad * "sin_lon_rad" + cos_lon_rad * "cos_lon_rad")) AS "distance_acos"
        FROM "locations"
        ORDER BY "distance_acos" DESC
        LIMIT 10;
        */
        // params sin_lat_rad, cos_lat_rad, sin_lon_rad, cos_lon_rad
        double sinLatRad = Math.toRadians(Math.sin(latitude));
        double cosLatRad = Math.toRadians(Math.cos(latitude));
        double sinLonRad = Math.toRadians(Math.sin(longitude));
        double cosLonRad = Math.toRadians(Math.cos(longitude));

        String sql = String.format(SELECT_CELLS, sinLatRad, cosLatRad, sinLonRad, cosLonRad);
        Cursor cur = getReadableDatabase().rawQuery(sql, null);
        if (cur == null)
            return null;

        List<Cell> cells = new ArrayList<>();
        while (cur.moveToNext()) {
            cells.add(readCursorCell(cur));
        }
        return cells;
    }

    public void close() {
        synchronized (lock) {
            if (db != null) {
                db.close();
                db = null;
            }
        }
    }

    private SQLiteDatabase getReadableDatabase() {
        synchronized (lock) {
            if (db != null)
                return db;
        }

        SQLiteDatabase db = SQLiteDatabase.openDatabase(dbpath, null, SQLiteDatabase.OPEN_READWRITE);
        db.execSQL(CREATE_CALC_TABLE);
        if (DatabaseUtils.queryNumEntries(db, TABLE_CALC) == 0) {
            try {
                populateCalcTable(db);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(CellDatabase.class.getSimpleName(), "Failed to populate calc table", e);
            }
        }
        return db;
    }


    private Cell readCursorCell(Cursor cur) {
        String cellId = cur.getString(cur.getColumnIndex(COL_CELL_ID));
        long distance = cur.getLong(cur.getColumnIndex(COL_DISTANCE));
        return new Cell(cellId, "", "", "",
                0, 0, distance, null);
    }

    public void populateCalcTable(SQLiteDatabase db) throws IllegalStateException {
        db.execSQL(CREATE_INDEX);

        Cursor cur = db.query(TABLE_CELLS, CELL_COLS,
                null, null,
                null, null,
                "_id", null);
        if (cur == null)
            throw new IllegalStateException("Failed to query cells database");

        db.beginTransaction();
        try {
            while (cur.moveToNext()) {
                long id = cur.getLong(0);
                double latitude = cur.getDouble(1);
                double longitude = cur.getDouble(2);

                ContentValues values = new ContentValues();
                values.put(COL_CELL_ID, id);
                values.put(COL_LATITUDE_SIN, Math.toRadians(Math.sin(latitude)));
                values.put(COL_LATITUDE_COS, Math.toRadians(Math.cos(latitude)));
                values.put(COL_LONGITUDE_SIN, Math.toRadians(Math.sin(longitude)));
                values.put(COL_LONGITUDE_COS, Math.toRadians(Math.cos(longitude)));
                db.insert(TABLE_CALC, null, values);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            cur.close();
        }
    }
}
