package com.example.notebud;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "NotesDB";
    private static final int DATABASE_VERSION = 2;

    private static final String TABLE_NOTES = "notes";
    private static final String KEY_ID = "id";
    private static final String KEY_TITLE = "title";
    private static final String KEY_CONTENT = "content";
    private static final String KEY_TIMESTAMP = "timestamp";
    private static final String KEY_ATTACHMENT = "attachment";
    private static final String KEY_IMAGE_PATH = "image_path";
    private static final String KEY_LOCKED = "locked";
    private static final String KEY_PASSWORD = "password";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_NOTES + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_TITLE + " TEXT,"
                + KEY_CONTENT + " TEXT,"
                + KEY_TIMESTAMP + " TEXT,"
                + KEY_ATTACHMENT + " TEXT,"
                + KEY_IMAGE_PATH + " TEXT,"
                + KEY_LOCKED + " INTEGER DEFAULT 0,"
                + KEY_PASSWORD + " TEXT"
                + ")";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            Cursor cursor = db.rawQuery("PRAGMA table_info(" + TABLE_NOTES + ")", null);
            List<String> columns = new ArrayList<>();
            if (cursor != null) {
                int nameIndex = cursor.getColumnIndex("name");
                while (cursor.moveToNext() && nameIndex != -1) {
                    String columnName = cursor.getString(nameIndex);
                    columns.add(columnName.toLowerCase());
                }
                cursor.close();
            }

            if (!columns.contains(KEY_IMAGE_PATH.toLowerCase())) {
                db.execSQL("ALTER TABLE " + TABLE_NOTES + " ADD COLUMN " + KEY_IMAGE_PATH + " TEXT");
            }
            if (!columns.contains(KEY_LOCKED.toLowerCase())) {
                db.execSQL("ALTER TABLE " + TABLE_NOTES + " ADD COLUMN " + KEY_LOCKED + " INTEGER DEFAULT 0");
            }
            if (!columns.contains(KEY_PASSWORD.toLowerCase())) {
                db.execSQL("ALTER TABLE " + TABLE_NOTES + " ADD COLUMN " + KEY_PASSWORD + " TEXT");
            }
        }
    }

    public long addNote(String title, String content, String attachmentPath, String imagePath) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_TITLE, title);
        values.put(KEY_CONTENT, content);
        values.put(KEY_TIMESTAMP, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
        values.put(KEY_ATTACHMENT, attachmentPath);
        values.put(KEY_IMAGE_PATH, imagePath);
        values.put(KEY_LOCKED, 0); // Default to unlocked
        values.put(KEY_PASSWORD, (String)null); // Cast null to String to resolve ambiguity
        long id = db.insert(TABLE_NOTES, null, values);
        db.close();
        return id;
    }

    public List<Note> getAllNotes() {
        List<Note> notesList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_NOTES + " ORDER BY " + KEY_TIMESTAMP + " DESC";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            int idIndex = cursor.getColumnIndex(KEY_ID);
            int titleIndex = cursor.getColumnIndex(KEY_TITLE);
            int contentIndex = cursor.getColumnIndex(KEY_CONTENT);
            int timestampIndex = cursor.getColumnIndex(KEY_TIMESTAMP);
            int attachmentIndex = cursor.getColumnIndex(KEY_ATTACHMENT);
            int imagePathIndex = cursor.getColumnIndex(KEY_IMAGE_PATH);
            int lockedIndex = cursor.getColumnIndex(KEY_LOCKED);
            int passwordIndex = cursor.getColumnIndex(KEY_PASSWORD);

            if (idIndex != -1 && titleIndex != -1 && contentIndex != -1 &&
                    timestampIndex != -1 && attachmentIndex != -1 &&
                    imagePathIndex != -1 && lockedIndex != -1 && passwordIndex != -1) {

                do {
                    Note note = new Note(
                            cursor.getLong(idIndex),
                            cursor.getString(titleIndex),
                            cursor.getString(contentIndex),
                            cursor.getString(timestampIndex),
                            cursor.getString(attachmentIndex)
                    );
                    note.setImagePath(cursor.getString(imagePathIndex));
                    note.setLocked(cursor.getInt(lockedIndex) == 1);
                    note.setNotePassword(cursor.getString(passwordIndex));
                    notesList.add(note);
                } while (cursor.moveToNext());
            }
        }

        cursor.close();
        db.close();
        return notesList;
    }

    public int updateNote(Note note) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(KEY_TITLE, note.getTitle());
        values.put(KEY_CONTENT, note.getContent());
        values.put(KEY_ATTACHMENT, note.getAttachmentPath());
        values.put(KEY_IMAGE_PATH, note.getImagePath());
        values.put(KEY_LOCKED, note.isLocked() ? 1 : 0);
        if (note.isLocked() && note.getNotePassword() != null) {
            values.put(KEY_PASSWORD, note.getNotePassword());
        }

        int rowsAffected = db.update(TABLE_NOTES, values, KEY_ID + " = ?",
                new String[]{String.valueOf(note.getId())});
        db.close();
        return rowsAffected;
    }

    public void deleteNote(long noteId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NOTES, KEY_ID + " = ?", new String[]{String.valueOf(noteId)});
        db.close();
    }

    public Note getNote(long noteId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NOTES, null, KEY_ID + " = ?",
                new String[]{String.valueOf(noteId)}, null, null, null);

        Note note = null;
        if (cursor != null && cursor.moveToFirst()) {
            int idIndex = cursor.getColumnIndex(KEY_ID);
            int titleIndex = cursor.getColumnIndex(KEY_TITLE);
            int contentIndex = cursor.getColumnIndex(KEY_CONTENT);
            int timestampIndex = cursor.getColumnIndex(KEY_TIMESTAMP);
            int attachmentIndex = cursor.getColumnIndex(KEY_ATTACHMENT);
            int imagePathIndex = cursor.getColumnIndex(KEY_IMAGE_PATH);
            int lockedIndex = cursor.getColumnIndex(KEY_LOCKED);
            int passwordIndex = cursor.getColumnIndex(KEY_PASSWORD);

            if (idIndex != -1 && titleIndex != -1 && contentIndex != -1 &&
                    timestampIndex != -1 && attachmentIndex != -1 &&
                    imagePathIndex != -1 && lockedIndex != -1 && passwordIndex != -1) {

                note = new Note(
                        cursor.getLong(idIndex),
                        cursor.getString(titleIndex),
                        cursor.getString(contentIndex),
                        cursor.getString(timestampIndex),
                        cursor.getString(attachmentIndex)
                );
                note.setImagePath(cursor.getString(imagePathIndex));
                note.setLocked(cursor.getInt(lockedIndex) == 1);
                note.setNotePassword(cursor.getString(passwordIndex));
            }
            cursor.close();
        }
        db.close();
        return note;
    }

    public boolean checkNotePassword(long noteId, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NOTES, new String[]{KEY_PASSWORD},
                KEY_ID + " = ?", new String[]{String.valueOf(noteId)},
                null, null, null);

        boolean isCorrect = false;
        if (cursor != null && cursor.moveToFirst()) {
            int passwordIndex = cursor.getColumnIndex(KEY_PASSWORD);
            if (passwordIndex != -1) {
                String storedPassword = cursor.getString(passwordIndex);
                isCorrect = storedPassword != null && storedPassword.equals(password);
            }
            cursor.close();
        }
        db.close();
        return isCorrect;
    }
}