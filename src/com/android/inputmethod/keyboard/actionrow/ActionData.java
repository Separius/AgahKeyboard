package com.android.inputmethod.keyboard.actionrow;

/**
 * Created by sepehr on 2/24/17.
 */
public class ActionData {
    private int ID;
    private boolean actualValue;
    private String key;
    private final String summary;
    private final String title;

    public ActionData(String title, String summary, String key, boolean actualValue, int ID) {
        this.title = title;
        this.summary = summary;
        this.key = key;
        this.actualValue = actualValue;
        this.ID = ID;
    }

    public boolean isChecked() {
        return this.actualValue;
    }

    public String getTitle() {
        return this.title;
    }

    public String getSummary() {
        return this.summary;
    }

    public int getID() {
        return this.ID;
    }

    public String getKey() {
        return this.key;
    }

    public void setChecked(boolean checked) {
        this.actualValue = checked;
    }
}