package com.poipoipo.timeline.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.poipoipo.timeline.data.Event;
import com.poipoipo.timeline.data.Label;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseHelper {

    public static final String DATABASE_NAME = "Event.db";
    public static final String TABLE_EVENT = "Event";
    public static final String TABLE_SUBTITLE = "Subtitle";
    public static final String TABLE_TITLE = "Title";
    public static final String TABLE_LOCATION = "Location";
    public static final int VERSION = 2;

    SQLiteDatabase database;
    ContentValues values = new ContentValues();
    Cursor cursor;
    List<Event> events = new ArrayList<>();
    Map<Integer, String> subtitles;
    Map<Integer, String> titles;
    Map<Integer, String> locations;

    public DatabaseHelper(Context context) {
        database = new DatabaseOpenHelper(context, DATABASE_NAME, null, VERSION).getWritableDatabase();
        subtitles = query(TABLE_SUBTITLE);
        titles = query(TABLE_TITLE);
        locations = query(TABLE_LOCATION);
    }

    public void insert(Event event) {
        values.clear();
        values.put("category", event.getTitle());
        values.put("title", event.getSubtitle());
        values.put("start", event.getStart());
        values.put("location", event.getLocation());
        values.put("note", event.getNote());
        if (event.hasEndTime) {
            values.put("end", event.getEnd());
        }
        database.insert(TABLE_EVENT, null, values);
    }

    public void insert(List<Event> list) {
        for (Event event : list) {
            insert(event);
        }
    }

    public void insert(String tag, String value) {
        values.clear();
        values.put(tag, value);
        database.insert(tag, null, values);
    }

    public void update(int start, String[] labels, String[] labelsValues) {
        if (labels.length == labelsValues.length) {
            values.clear();
            for (int i = 0; i <= labels.length; i++) {
                values.put(labels[i], labelsValues[i]);
            }
            database.update(TABLE_EVENT, values, "start = ?", new String[]{Integer.toString(start)});
        }
    }

    public void update(String label, String before, String after) {
        values.clear();
        values.put(label, after);
        database.update(label, values, "value = ?", new String[]{before});
    }

    public void delete(String table) {
        database.delete(table, null, null);
    }

    public void delete(int start) {
        database.delete(TABLE_EVENT, "start = ?", new String[]{Integer.toString(start)});
    }

    public void delete(String label, String which) {
        database.delete(label, "value  = ?", new String[]{which});
    }

    public Event query(int start) {
        cursor = database.query(TABLE_EVENT, null, "start = ?", new String[]{Integer.toString(start)}, null, null, null);
        Event event = new Event(cursor.getInt(cursor.getColumnIndex("start")));
        event.setTitle(subtitles.get(cursor.getInt(cursor.getColumnIndex("category"))));
        event.setSubtitle(titles.get(cursor.getInt(cursor.getColumnIndex("title"))));
        if (event.hasEndTime) {
            event.setEnd(cursor.getInt(cursor.getColumnIndex("end")));
        }
        event.setLocation(locations.get(cursor.getInt(cursor.getColumnIndex("location"))));
        return event;
    }

//    public List<Event> query() {
//        cursor = database.query(TABLE_EVENT, null, null, null, null, null, null);
//        queryTraverse();
//        return events;
//    }

    public List<Event> query(int dataMin, int dataMax) {
        String where = "start < ? and start > ?";
        String[] whereValues = {Integer.toString(dataMax), Integer.toString(dataMin)};
        cursor = database.query(TABLE_EVENT, null, where, whereValues, null, null, null);
        queryTraverse();
        return events;
    }

    public String getLabelById(int type, int id) {
        String s = null;
        switch (type) {
            case Label.TITLE:
                s = titles.get(id);
                break;
            case Label.SUBTITLE:
                s = subtitles.get(id);
                break;
            case Label.LOCATION:
                s = locations.get(id);
                break;
        }
        return s;
    }

    private void queryTraverse() {
        events.clear();
        if (cursor.moveToFirst()) {
            do {
                Event event = new Event(cursor.getInt(cursor.getColumnIndex("start")));
                event.setTitle(subtitles.get(cursor.getInt(cursor.getColumnIndex("category"))));
                event.setSubtitle(titles.get(cursor.getInt(cursor.getColumnIndex("title"))));
                if (event.hasEndTime) {
                    event.setEnd(cursor.getInt(cursor.getColumnIndex("end")));
                }
                event.setLocation(locations.get(cursor.getInt(cursor.getColumnIndex("location"))));
                events.add(event);
            } while (cursor.moveToNext());
        }
        cursor.close();
    }

    private Map<Integer, String> query(String label) {
        Map<Integer, String> map = new HashMap<>();
        cursor = database.query(label, null, null, null, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                map.put(cursor.getInt(cursor.getColumnIndex("id")), cursor.getString(cursor.getColumnIndex("value")));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return map;
    }

}