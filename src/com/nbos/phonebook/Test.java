package com.nbos.phonebook;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Data;
import android.util.Log;

import com.nbos.phonebook.database.tables.BookTable;
import com.nbos.phonebook.sync.Constants;
import com.nbos.phonebook.sync.client.ContactPicture;
import com.nbos.phonebook.sync.client.NetworkUtilities;

public class Test {
	static String tag = "Test";
    public static void resetBookTableDirty(Context ctx) {
		ContentResolver cr = ctx.getContentResolver();
	    ContentValues values = new ContentValues();
	    values = new ContentValues();
	    values.put(BookTable.DIRTY, (Integer) null);
	    int num = cr.update(
	    		Uri.parse(Constants.SHARE_BOOK_PROVIDER), values,
	    		null, null);
	    Log.i(tag, "Updated "+num+" sharebooks to dirty = 0");
	}

    public static void getContacts(Context ctx) {
    	DatabaseHelper.getContacts(false, ctx);
	}

    public static void getRawContacts(Context ctx) {
    	Cursor c = DatabaseHelper.getRawContactsCursor(ctx.getContentResolver(), false);
    	Log.i(tag, "There are "+c.getCount()+" raw contacts");
    	c.moveToFirst();
    	do {
			String cId = c.getString(c.getColumnIndex(ContactsContract.RawContacts.CONTACT_ID));
			String serverId = c.getString(c.getColumnIndex(Constants.CONTACT_SERVER_ID));
			Log.i(tag, "contactId: "+cId+", serverId: "+serverId);
    	} while(c.moveToNext());
    	DatabaseHelper.getSourceIdFromContactId(c, "793");
	}

    
    public static void getGroups(Context ctx) {
    	DatabaseHelper.getGroups(false, ctx);
    }
    

    public static void getDirtyGroups(Context ctx) {
    	DatabaseHelper.getGroups(true, ctx);
    }
    
    public static void getGroupList(Context ctx) {
    	Cursor c = ctx.getContentResolver().query(ContactsContract.Groups.CONTENT_SUMMARY_URI, null,
    	    	ContactsContract.Groups.DELETED + "=0",	    		
    	    	null, null);
    	c.moveToFirst();
    	do {
    		String name = c.getString(c.getColumnIndex(ContactsContract.Groups.TITLE)),
    			id = c.getString(c.getColumnIndex(ContactsContract.Groups._ID)),
    			serverId = c.getString(c.getColumnIndex(ContactsContract.Groups.SOURCE_ID));
    		Log.i(tag, "Group; name: "+name+", id: "+id+", serverId: "+serverId);
    	} while(c.moveToNext());
    }
    
    public static void setContactsServerIdToNull(Context ctx) {
	    ContentValues values = new ContentValues();
	    values.put(Constants.CONTACT_SERVER_ID, (String) null);
	    ContentResolver cr = ctx.getContentResolver();
	    int num = cr.update(
	    		ContactsContract.RawContacts.CONTENT_URI, values,
	    		null, null);
	    Log.i(tag, "Updated "+num+" contacts serverId to null");

    }
    
    public static void getData(Context ctx) {
    	final Cursor c =
    		ctx.getContentResolver().query(Data.CONTENT_URI, null, null, null, null);
    	Log.i(tag, "There are "+c.getCount()+" data items");
    
    }
    
    public static void getContactPictures(Context ctx) {
    	List<ContactPicture> pics = DatabaseHelper.getContactPictures(ctx.getContentResolver(), true);
		Map<String, String> params = new HashMap<String, String>();
		params.put("upload", "avatar");
		params.put("errorAction", "error");
		params.put("errorController", "file");
		params.put("successAction", "success");
		params.put("successController", "file");

    	for(ContactPicture pic : pics)
    	{
    		String contentType = pic.mimeType.split("/")[1];
    		Log.i(tag, "uploading "+contentType);
    		params.remove("id");
    		params.put("id", pic.serverId);

    		NetworkUtilities.upload(NetworkUtilities.UPLOAD_CONTACT_PIC_URI, pic.pic, contentType, params);
    	}
    }
}
