package com.nbos.phonebook;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;

import com.nbos.phonebook.database.tables.BookTable;
import com.nbos.phonebook.database.tables.PicTable;
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
	    		Constants.SHARE_BOOK_URI, values,
	    		null, null);
	    Log.i(tag, "Updated "+num+" sharebooks to dirty = 0");
	}

    public static void getContacts(Context ctx) {
    	Db.getContacts(false, ctx);
	}

    public static void getRawContacts(Context ctx) {
    	Cursor c = Db.getRawContactsCursor(ctx.getContentResolver(), false);
    	Log.i(tag, "There are "+c.getCount()+" raw contacts");
    	c.moveToFirst();
    	do {
			String cId = c.getString(c.getColumnIndex(ContactsContract.RawContacts.CONTACT_ID));
			//String serverId = c.getString(c.getColumnIndex(Constants.CONTACT_SERVER_ID));
			Log.i(tag, "contactId: "+cId);//+", serverId: "+serverId);
    	} while(c.moveToNext());
    	// DatabaseHelper.getSourceIdFromContactId(c, "793");
	}

    
    public static void getGroups(Context ctx) {
    	Db.getGroups(false, ctx);
    }
    

    public static void getDirtyGroups(Context ctx) {
    	Db.getGroups(true, ctx);
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
    
    /*public static void setContactsServerIdToNull(Context ctx) {
	    ContentValues values = new ContentValues();
	    values.put(Constants.CONTACT_SERVER_ID, (String) null);
	    ContentResolver cr = ctx.getContentResolver();
	    int num = cr.update(
	    		ContactsContract.RawContacts.CONTENT_URI, values,
	    		null, null);
	    Log.i(tag, "Updated "+num+" contacts serverId to null");

    }*/
    
    public static void getData(Context ctx) {
    	final Cursor c =
    		ctx.getContentResolver().query(Data.CONTENT_URI, null, null, null, null);
    	Log.i(tag, "There are "+c.getCount()+" data items");
    
    }
    
    public static void getContactPictures(Context ctx) {
    	List<ContactPicture> pics = Db.getContactPictures(ctx, true);
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
    

	public static void getRawContactsTable(Context ctx) {
		Cursor c = Db.getRawContactsCursor(ctx.getContentResolver(), false);
		c.moveToFirst();
		do {
    		String contactId = c.getString(c.getColumnIndex(RawContacts.CONTACT_ID));
    		Log.i(tag, "contactId: "+contactId);
		} while(c.moveToNext());
	}

	public static void getDataTable(Context applicationContext) {
		Cursor c = Db.getData(applicationContext);
		c.moveToFirst();
		do {
    		String rawContactId = c.getString(c.getColumnIndex(Data.RAW_CONTACT_ID));
    		String contactId = c.getString(c.getColumnIndex(Data.CONTACT_ID));
    		String serverId = c.getString(c.getColumnIndex(Data.DATA1));
    		String data2 = c.getString(c.getColumnIndex(Data.DATA2));
    		String mimeType = c.getString(c.getColumnIndex(Data.MIMETYPE));
    		Log.i(tag, "raw contactId: "+rawContactId+", contactId: "+contactId+", data1: "+serverId+", data2: "+data2+", mimeType: "+mimeType);
		} while(c.moveToNext());
	}
	
	public static void getDataPicsTable(Context applicationContext) {
		Cursor c = applicationContext.getContentResolver().query(ContactsContract.Data.CONTENT_URI,
	    		// null,
	    	    new String[] {
					Data._ID,
	    			Data.CONTACT_ID,
	    			Data.RAW_CONTACT_ID,
	    			CommonDataKinds.Photo.PHOTO,
	    			Data.MIMETYPE
	    		},
	    		null,
	    		// ContactsContract.CommonDataKinds.Photo.PHOTO +" is not null",
	    	    null, ContactsContract.Data.CONTACT_ID);
    	Log.i(tag, "Data pics cursor has "+c.getCount()+" rows");
		
		c.moveToFirst();
		do {
			String dataId = c.getString(c.getColumnIndex(Data._ID));
    		String rawContactId = c.getString(c.getColumnIndex(Data.RAW_CONTACT_ID));
    		String contactId = c.getString(c.getColumnIndex(Data.CONTACT_ID));
    		String mimeType = c.getString(c.getColumnIndex(Data.MIMETYPE));
    		if(!mimeType.equals(ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE))
    			continue;
    		byte [] photo = c.getBlob(c.getColumnIndex(CommonDataKinds.Photo.PHOTO));
    		Log.i(tag, "id: "+dataId+", raw contactId: "+rawContactId+", contactId: "+contactId+", mimeType: "+mimeType+", photo is: "+((photo == null) ? "null" : photo.length) );
		} while(c.moveToNext());
		c.close();
	}

	public static void updateServerId(Context applicationContext) {
		Db.updateContactServerId("997", "1", applicationContext, Db.getRawContactsCursor(applicationContext.getContentResolver(), false));
		getDataTable(applicationContext);
	}

	public static void getPicTable(Context ctx) {
    	final Cursor c = ctx.getContentResolver().query(Constants.PIC_URI, null, null, null, null);
    	Log.i(tag, "There are "+c.getCount()+" pics");
    	c.moveToFirst();
    	do {
    		String serverId = c.getString(c.getColumnIndex(PicTable.SERVERID));
    		String picId = c.getString(c.getColumnIndex(PicTable.PICID));
    		Log.i(tag, "serverId: "+serverId+", picId: "+picId);
    	} while(c.moveToNext());
    }
	
	public static void deletePicTable(Context ctx) {
    	int num = ctx.getContentResolver().delete(Constants.PIC_URI, null, null);
    	Log.i(tag, "Deleted "+num+" pic entries");
	}
	
	public static void getContactPics(Context ctx) {
		Db.getContactPictures(ctx, false);
	}
}
