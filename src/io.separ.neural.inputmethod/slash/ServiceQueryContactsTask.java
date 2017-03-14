package io.separ.neural.inputmethod.slash;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;

import java.util.ArrayList;

/**
 * Created by sepehr on 3/2/17.
 */
public class ServiceQueryContactsTask extends BaseQuerySearchTask {
    private final SearchType searchType;

    public enum SearchType {
        Phone,
        Email
    }

    public ServiceQueryContactsTask(String name, SearchType searchType) {
        super("contacts", name);
        this.searchType = searchType;
    }

    protected void run(Context context) throws Exception {
        String[] PROJECTION = new String[3];
        PROJECTION[0] = "display_name";
        PROJECTION[1] = "photo_thumb_uri";
        PROJECTION[2] = this.searchType == SearchType.Phone ? "data1" : "data1";
        Uri uri = ContactsContract.Data.CONTENT_URI;
        String selection = "display_name LIKE ? AND mimetype=?";
        String[] selectionArgs = new String[2];
        selectionArgs[0] = "%" + getQuery() + "%";
        selectionArgs[1] = this.searchType == SearchType.Phone ? "vnd.android.cursor.item/phone_v2" : "vnd.android.cursor.item/email_v2";
        Cursor phoneCursor = context.getContentResolver().query(uri, PROJECTION, selection, selectionArgs, "times_contacted desc LIMIT 20");
        if (phoneCursor != null) {
            int idDisplayName = phoneCursor.getColumnIndex("display_name");
            int thumbnail = phoneCursor.getColumnIndex("photo_thumb_uri");
            int data = phoneCursor.getColumnIndex(this.searchType == SearchType.Phone ? "data1" : "data1");
            ArrayList<RSearchItem> items = new ArrayList();
            phoneCursor.moveToFirst();
            while (!phoneCursor.isAfterLast()) {
                RSearchItem item = new RSearchItem();
                item.setTitle(phoneCursor.getString(idDisplayName));
                item.setSubtitle(phoneCursor.getString(data));
                item.setOutput(this.searchType == SearchType.Phone ? item.getTitle() + "\n" + PhoneNumberUtils.formatNumber(phoneCursor.getString(data)) : phoneCursor.getString(data));
                item.setService("contacts");
                item.setDisplayType(RSearchItem.DEFAULT_TYPE);
                item.setUid(item.getOutput());
                items.add(item);
                phoneCursor.moveToNext();
            }
            phoneCursor.close();
            setResults(items);
        }
    }

    protected boolean handleError(Context context, Throwable e) {
        return false;
    }
}
