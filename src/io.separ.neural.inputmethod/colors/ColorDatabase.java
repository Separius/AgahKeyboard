package io.separ.neural.inputmethod.colors;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static io.separ.neural.inputmethod.colors.ColorProfile.getIcon;

/**
 * Created by sepehr on 3/8/17.
 */
public class ColorDatabase extends SQLiteOpenHelper {
    private static final String COLOR_COLUMN = "color";
    private static final String DB_NAME = "Colors";
    private static final int DB_VERSION = 2;
    public static final String EH_TABLE_NAME = "CustomColor";
    private static final String PACKAGE_COLUMN = "package";
    private static final String TABLE_NAME = "Color";
    private static final String TITLE_COLUMN = "title";
    private static ColorDatabase database;

    private static ColorDatabase getDatabase(Context context) {
        if (database == null) {
            database = new ColorDatabase(context);
        }
        return database;
    }

    private static SQLiteDatabase getReadableDatabase(Context context) {
        return getDatabase(context).getReadableDatabase();
    }

    private static SQLiteDatabase getWritableDatabase(Context context) {
        return getDatabase(context).getWritableDatabase();
    }

    public static synchronized void addColors(Context context, String packageName, String[] colors) {
        synchronized (ColorDatabase.class) {
            if (colors.length < 1) {
                Object[] objArr = new Object[DB_VERSION];
                objArr[0] = Integer.valueOf(1);
                objArr[1] = Integer.valueOf(colors.length);
                throw new InvalidParameterException(String.format(Locale.ENGLISH, "Need %d colors, found %d", objArr));
            }
            if (existPackage(context, packageName)) {
                deletePackage(context, packageName);
            }
            ContentValues values = new ContentValues();
            values.put(PACKAGE_COLUMN, packageName);
            for (int i = 0; i < 1; i++) {
                Object[] objArr2 = new Object[DB_VERSION];
                objArr2[0] = COLOR_COLUMN;
                objArr2[1] = Integer.valueOf(i);
                values.put(String.format(Locale.ENGLISH, "%s%d", objArr2), colors[i]);
            }
            SQLiteDatabase db = getWritableDatabase(context);
            db.insert(TABLE_NAME, null, values);
            db.close();
        }
    }

    public static synchronized void addColor(Context context, String packageName, String title, String color) {
        synchronized (ColorDatabase.class) {
            if (existPackage(context, packageName)) {
                deletePackage(context, packageName);
            }
            ContentValues values = new ContentValues();
            values.put(PACKAGE_COLUMN, packageName);
            values.put(TITLE_COLUMN, title);
            values.put(COLOR_COLUMN, color);
            SQLiteDatabase db = getWritableDatabase(context);
            db.insert(EH_TABLE_NAME, null, values);
            db.close();
        }
    }

    public static synchronized boolean existPackage(Context context, String packageName) {
        boolean exist = false;
        synchronized (ColorDatabase.class) {
            SQLiteDatabase db = getReadableDatabase(context);
            Object[] objArr = new Object[DB_VERSION];
            objArr[0] = TABLE_NAME;
            objArr[1] = PACKAGE_COLUMN;
            Cursor c = db.rawQuery(String.format("SELECT * FROM %s WHERE %s = ?", objArr), new String[]{packageName});
            try {
                exist = c.moveToFirst();
                c.close();
                db.close();
            } catch (Throwable th) {
                c.close();
                db.close();
            }
        }
        return exist;
    }

    public static synchronized boolean existPackageTitle(Context context, String packageName, String title) {
        boolean exist = false;
        synchronized (ColorDatabase.class) {
            SQLiteDatabase db = getReadableDatabase(context);
            String format = String.format("SELECT * FROM %s WHERE %s = ? AND %s = ?", new Object[]{EH_TABLE_NAME, PACKAGE_COLUMN, TITLE_COLUMN});
            String[] strArr = new String[DB_VERSION];
            strArr[0] = packageName;
            strArr[1] = title;
            Cursor c = db.rawQuery(format, strArr);
            try {
                exist = c.moveToFirst();
                c.close();
                db.close();
            } catch (Throwable th) {
                c.close();
                db.close();
            }
        }
        return exist;
    }

    public static synchronized void deletePackage(Context context, String packageName) {
        synchronized (ColorDatabase.class) {
            SQLiteDatabase db = getWritableDatabase(context);
            db.delete(TABLE_NAME, String.format("%s = ?", new Object[]{PACKAGE_COLUMN}), new String[]{packageName});
            db.close();
        }
    }

    public static synchronized void deletePackageTitle(Context context, String packageName, String title) {
        synchronized (ColorDatabase.class) {
            SQLiteDatabase db = getWritableDatabase(context);
            String str = EH_TABLE_NAME;
            Object[] objArr = new Object[DB_VERSION];
            objArr[0] = PACKAGE_COLUMN;
            objArr[1] = TITLE_COLUMN;
            String format = String.format("%s = ? AND %s = ?", objArr);
            String[] strArr = new String[DB_VERSION];
            strArr[0] = packageName;
            strArr[1] = title;
            db.delete(str, format, strArr);
            db.close();
        }
    }

    public static synchronized String[] getColors(Context context, String packageName) {
        String[] colors;
        synchronized (ColorDatabase.class) {
            SQLiteDatabase db = getReadableDatabase(context);
            colors = new String[1];
            Object[] objArr = new Object[DB_VERSION];
            objArr[0] = TABLE_NAME;
            objArr[1] = PACKAGE_COLUMN;
            Cursor c = db.rawQuery(String.format("SELECT * FROM %s WHERE %s = ?", objArr), new String[]{packageName});
            try {
                if (c.moveToFirst()) {
                    for (int i = 0; i < 1; i++) {
                        Object[] objArr2 = new Object[DB_VERSION];
                        objArr2[0] = COLOR_COLUMN;
                        objArr2[1] = Integer.valueOf(i);
                        colors[i] = c.getString(c.getColumnIndex(String.format(Locale.ENGLISH, "%s%d", objArr2)));
                    }
                }
                c.close();
                db.close();
            } catch (Throwable th) {
                c.close();
                db.close();
            }
        }
        return colors;
    }

    public static synchronized String getColor(Context context, String packageName, String title) {
        String color;
        synchronized (ColorDatabase.class) {
            SQLiteDatabase db = getReadableDatabase(context);
            color = null;
            String format = String.format("SELECT * FROM %s WHERE %s = ? AND %s = ?", new Object[]{EH_TABLE_NAME, PACKAGE_COLUMN, TITLE_COLUMN});
            String[] strArr = new String[DB_VERSION];
            strArr[0] = packageName;
            strArr[1] = title;
            Cursor c = db.rawQuery(format, strArr);
            try {
                if (c.moveToFirst()) {
                    for (int i = 0; i < 1; i++) {
                        color = c.getString(c.getColumnIndex(COLOR_COLUMN));
                    }
                }
                c.close();
                db.close();
            } catch (Throwable th) {
                c.close();
                db.close();
            }
        }
        return color;
    }

    public ColorDatabase(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    public static synchronized void addTheme(Context context, int color) {
        synchronized (ColorDatabase.class) {
            SQLiteDatabase db = getWritableDatabase(context);
            db.delete(TABLE_NAME, String.format("%s = ?", new Object[]{PACKAGE_COLUMN}), new String[]{"my_theme_primary"});
            db.delete(TABLE_NAME, String.format("%s = ?", new Object[]{PACKAGE_COLUMN}), new String[]{"my_theme_secondary"});
            db.insert(TABLE_NAME, null, PrepopulateThemes("my_theme_primary", color));
            db.insert(TABLE_NAME, null, PrepopulateThemes("my_theme_secondary", getIcon(color)));
            db.close();
        }
    }

    public void onCreate(SQLiteDatabase db) {
        if (!getTablesName(db).contains(TABLE_NAME)) {
            Object[] objArr = new Object[DB_VERSION];
            objArr[0] = TABLE_NAME;
            objArr[1] = PACKAGE_COLUMN;
            String createSQL = String.format("CREATE TABLE %s (_ID INTEGER PRIMARY KEY, %s TEXT", objArr);
            for (int i = 0; i < 1; i++) {
                StringBuilder append = new StringBuilder().append(createSQL);
                Object[] objArr2 = new Object[DB_VERSION];
                objArr2[0] = COLOR_COLUMN;
                objArr2[1] = Integer.valueOf(i);
                createSQL = append.append(String.format(Locale.ENGLISH, ", %s%d", objArr2)).toString();
            }
            db.execSQL(createSQL + ")");
            db.insert(TABLE_NAME, null, PrepopulateThemes("black_theme_primary", -15592684));
            db.insert(TABLE_NAME, null, PrepopulateThemes("black_theme_secondary", -14756000));
            db.insert(TABLE_NAME, null, PrepopulateThemes("my_theme_primary", -15592684));
            db.insert(TABLE_NAME, null, PrepopulateThemes("my_theme_secondary", -14756000));
        }
        if (!getTablesName(db).contains(EH_TABLE_NAME)) {
            db.execSQL(String.format("CREATE TABLE %s (_ID INTEGER PRIMARY KEY, %s TEXT, %s TEXT, %s TEXT)", new Object[]{EH_TABLE_NAME, PACKAGE_COLUMN, TITLE_COLUMN, COLOR_COLUMN}));
        }
    }

    private static ContentValues PrepopulateThemes(String name, int color){
        ContentValues values = new ContentValues();
        values.put(PACKAGE_COLUMN, name);
        for (int i = 0; i < 1; i++) {
            Object[] objArr2 = new Object[DB_VERSION];
            objArr2[0] = COLOR_COLUMN;
            objArr2[1] = Integer.valueOf(i);
            values.put(String.format(Locale.ENGLISH, "%s%d", objArr2), String.format("#%06X", color));
        }
        return values;
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (!getTablesName(db).contains(EH_TABLE_NAME)) {
            db.execSQL(String.format("CREATE TABLE %s (_ID INTEGER PRIMARY KEY, %s TEXT, %s TEXT, %s TEXT)", new Object[]{EH_TABLE_NAME, PACKAGE_COLUMN, TITLE_COLUMN, COLOR_COLUMN}));
        }
    }

    private static List<String> getTablesName(SQLiteDatabase db) {
        ArrayList<String> tablesName = new ArrayList();
        Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);
        if (c.moveToFirst()) {
            while (!c.isAfterLast()) {
                tablesName.add(c.getString(c.getColumnIndex("name")));
                c.moveToNext();
            }
        }
        c.close();
        return tablesName;
    }
}