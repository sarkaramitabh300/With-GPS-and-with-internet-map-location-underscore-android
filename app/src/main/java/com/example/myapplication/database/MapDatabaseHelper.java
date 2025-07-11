package com.example.myapplication.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.myapplication.model.DownloadedMap;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * SQLite database helper for managing downloaded maps metadata
 */
public class MapDatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "MapDatabaseHelper";
    
    // Database Info
    private static final String DATABASE_NAME = "OfflineMaps.db";
    private static final int DATABASE_VERSION = 1;
    
    // Table Names
    private static final String TABLE_DOWNLOADED_MAPS = "downloaded_maps";
    
    // Column Names
    private static final String KEY_ID = "id";
    private static final String KEY_LABEL = "label";
    private static final String KEY_DESCRIPTION = "description";
    private static final String KEY_CENTER_LAT = "center_latitude";
    private static final String KEY_CENTER_LNG = "center_longitude";
    private static final String KEY_NE_LAT = "northeast_latitude";
    private static final String KEY_NE_LNG = "northeast_longitude";
    private static final String KEY_SW_LAT = "southwest_latitude";
    private static final String KEY_SW_LNG = "southwest_longitude";
    private static final String KEY_ZOOM_LEVEL = "zoom_level";
    private static final String KEY_DOWNLOAD_DATE = "download_date";
    private static final String KEY_FILE_SIZE = "file_size_bytes";
    private static final String KEY_MAP_TYPE = "map_type";
    private static final String KEY_IS_AVAILABLE_OFFLINE = "is_available_offline";
    
    // Date format for database storage
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    
    private static MapDatabaseHelper instance;
    
    public static synchronized MapDatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new MapDatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }
    
    private MapDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_DOWNLOADED_MAPS_TABLE = "CREATE TABLE " + TABLE_DOWNLOADED_MAPS +
                "(" +
                KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                KEY_LABEL + " TEXT NOT NULL," +
                KEY_DESCRIPTION + " TEXT," +
                KEY_CENTER_LAT + " REAL NOT NULL," +
                KEY_CENTER_LNG + " REAL NOT NULL," +
                KEY_NE_LAT + " REAL NOT NULL," +
                KEY_NE_LNG + " REAL NOT NULL," +
                KEY_SW_LAT + " REAL NOT NULL," +
                KEY_SW_LNG + " REAL NOT NULL," +
                KEY_ZOOM_LEVEL + " INTEGER NOT NULL," +
                KEY_DOWNLOAD_DATE + " TEXT NOT NULL," +
                KEY_FILE_SIZE + " INTEGER DEFAULT 0," +
                KEY_MAP_TYPE + " TEXT NOT NULL," +
                KEY_IS_AVAILABLE_OFFLINE + " INTEGER DEFAULT 1" +
                ")";
        
        db.execSQL(CREATE_DOWNLOADED_MAPS_TABLE);
        Log.d(TAG, "Database table created successfully");
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_DOWNLOADED_MAPS);
        onCreate(db);
    }
    
    // Add a new downloaded map
    public long addDownloadedMap(DownloadedMap map) {
        SQLiteDatabase db = this.getWritableDatabase();
        
        ContentValues values = new ContentValues();
        values.put(KEY_LABEL, map.getLabel());
        values.put(KEY_DESCRIPTION, map.getDescription());
        values.put(KEY_CENTER_LAT, map.getCenterLatitude());
        values.put(KEY_CENTER_LNG, map.getCenterLongitude());
        values.put(KEY_NE_LAT, map.getNorthEastLat());
        values.put(KEY_NE_LNG, map.getNorthEastLng());
        values.put(KEY_SW_LAT, map.getSouthWestLat());
        values.put(KEY_SW_LNG, map.getSouthWestLng());
        values.put(KEY_ZOOM_LEVEL, map.getZoomLevel());
        values.put(KEY_DOWNLOAD_DATE, DATE_FORMAT.format(map.getDownloadDate()));
        values.put(KEY_FILE_SIZE, map.getFileSizeBytes());
        values.put(KEY_MAP_TYPE, map.getMapType());
        values.put(KEY_IS_AVAILABLE_OFFLINE, map.isAvailableOffline() ? 1 : 0);
        
        long id = db.insert(TABLE_DOWNLOADED_MAPS, null, values);
        db.close();
        
        Log.d(TAG, "Added downloaded map with ID: " + id);
        return id;
    }
    
    // Get all downloaded maps
    public List<DownloadedMap> getAllDownloadedMaps() {
        List<DownloadedMap> mapList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_DOWNLOADED_MAPS + " ORDER BY " + KEY_DOWNLOAD_DATE + " DESC";
        
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        
        if (cursor.moveToFirst()) {
            do {
                DownloadedMap map = cursorToDownloadedMap(cursor);
                mapList.add(map);
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        db.close();
        
        Log.d(TAG, "Retrieved " + mapList.size() + " downloaded maps");
        return mapList;
    }
    
    // Delete a downloaded map
    public void deleteDownloadedMap(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_DOWNLOADED_MAPS, KEY_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
        
        Log.d(TAG, "Deleted downloaded map ID: " + id);
    }
    
    // Check if a map with the same label already exists
    public boolean isMapLabelExists(String label) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_DOWNLOADED_MAPS,
                new String[]{KEY_ID},
                KEY_LABEL + "=?",
                new String[]{label},
                null, null, null);
        
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        db.close();
        
        return exists;
    }
    
    // Get count of downloaded maps
    public int getDownloadedMapsCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_DOWNLOADED_MAPS, null);
        
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        
        cursor.close();
        db.close();
        
        return count;
    }
    
    // Helper method to convert cursor to DownloadedMap object
    private DownloadedMap cursorToDownloadedMap(Cursor cursor) {
        DownloadedMap map = new DownloadedMap();
        
        map.setId(cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ID)));
        map.setLabel(cursor.getString(cursor.getColumnIndexOrThrow(KEY_LABEL)));
        map.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(KEY_DESCRIPTION)));
        map.setCenterLatitude(cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_CENTER_LAT)));
        map.setCenterLongitude(cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_CENTER_LNG)));
        map.setNorthEastLat(cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_NE_LAT)));
        map.setNorthEastLng(cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_NE_LNG)));
        map.setSouthWestLat(cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_SW_LAT)));
        map.setSouthWestLng(cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_SW_LNG)));
        map.setZoomLevel(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ZOOM_LEVEL)));
        map.setFileSizeBytes(cursor.getLong(cursor.getColumnIndexOrThrow(KEY_FILE_SIZE)));
        map.setMapType(cursor.getString(cursor.getColumnIndexOrThrow(KEY_MAP_TYPE)));
        map.setAvailableOffline(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_IS_AVAILABLE_OFFLINE)) == 1);
        
        // Parse date
        String dateString = cursor.getString(cursor.getColumnIndexOrThrow(KEY_DOWNLOAD_DATE));
        try {
            Date date = DATE_FORMAT.parse(dateString);
            map.setDownloadDate(date);
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing date: " + dateString, e);
            map.setDownloadDate(new Date()); // Default to current date
        }
        
        return map;
    }
}
