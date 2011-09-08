package com.nbos.phonebook;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.nbos.phonebook.database.tables.BookTable;
import com.nbos.phonebook.sync.Constants;
import com.nbos.phonebook.sync.platform.PhonebookSyncAdapterColumns;

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

    public static void getContacts(Activity ctx) {
    	Cursor contactsCursor = Db.getContacts(ctx);
    	Log.i(tag, "There are "+contactsCursor.getCount()+" contacts");
    	contactsCursor.moveToFirst();
    	do {
    		String contactId = contactsCursor.getString(contactsCursor.getColumnIndex(Contacts._ID)),
    			name = contactsCursor.getString(contactsCursor.getColumnIndex(Contacts.DISPLAY_NAME));
    		Log.i(tag, "id: "+contactId+", name: "+name);
    	} while(contactsCursor.moveToNext());
    	
	}

    public static void getStringId() {
    	// int id = ContactsContract.CommonDataKinds.Email.getTypeLabelResource(3);
    	
    	int id = ContactsContract.CommonDataKinds.Im.getProtocolLabelResource(0);
    	String str = Resources.getSystem().getString(id);
    	Log.i(tag, "String is: "+str);
    }
    
    public static void telephony(Activity a) {
    	String s1 = "+91-984-435-2341", 
    		s2 = "9844352341";
    	Log.i(tag, "Compare ("+s1+", "+s2+") = "+PhoneNumberUtils.compare(s1, s2));
        TelephonyManager tel = (TelephonyManager) a.getSystemService(Context.TELEPHONY_SERVICE);
        String networkOperator = tel.getNetworkOperator();
        int mcc, mnc;
        if (networkOperator != null) {
        	
            mcc = Integer.parseInt(networkOperator.substring(0, 3));
            mnc = Integer.parseInt(networkOperator.substring(3));
            Log.i(tag, "MCC: "+mcc+", mnc: "+mnc);
        }
        else
        	Log.i(tag, "networkOperator is null");
    	TelephonyManager t;
    }
    /*public static void getRawContactsEntity(Context ctx) {
        String[] DATA_KEYS = new String[]{
            Data.DATA1,
            Data.DATA2,
            Data.DATA3,
            Data.DATA4,
            Data.DATA5,
            Data.DATA6,
            Data.DATA7,
            Data.DATA8,
            Data.DATA9,
            Data.DATA10,
            Data.DATA11,
            Data.DATA12,
            Data.DATA13,
            Data.DATA14,
            Data.DATA15,
            Data.SYNC1,
            Data.SYNC2,
            Data.SYNC3,
            Data.SYNC4};
    	
    	Cursor cursor = Db.getRawContactsEntityCursor(ctx.getContentResolver(), true);
    	Log.i(tag, "There are "+cursor.getCount()+" entries, "+cursor.getColumnCount()+" columns.");
    	for(String col : cursor.getColumnNames())
    		Log.i(tag, "col: "+col);
    	cursor.moveToFirst();
    	do {
    		String contactId = cursor.getString(cursor.getColumnIndex(RawContacts.CONTACT_ID)),
    			dirty = cursor.getString(cursor.getColumnIndex(RawContacts.DIRTY)),
    			mimeType = cursor.getString(cursor.getColumnIndex(Data.MIMETYPE));
    		Log.i(tag, "contactId: "+contactId+", dirty: "+dirty+", mimetype: "+mimeType);
            for (String key : DATA_KEYS) {
                final int columnIndex = cursor.getColumnIndexOrThrow(key);
                if (cursor.isNull(columnIndex)) {
                    // don't put anything
                } else {
                    try {
                    	Log.i(tag, key+": "+cursor.getString(columnIndex));
                        // cv.put(key, cursor.getString(columnIndex));
                    } catch (SQLiteException e) {
                    	Log.i(tag, key+": isBlob");
                        // cv.put(key, cursor.getBlob(columnIndex));
                    }
                }
            	
            }
    	} while(cursor.moveToNext());
	}*/

    public static void getRawContacts(Context ctx) {
    	Cursor c = new Db(ctx).getRawContactsCursor(false);
    	Log.i(tag, "There are "+c.getCount()+" raw contacts");
    	c.moveToFirst();
    	do {
			String cId = c.getString(c.getColumnIndex(RawContacts.CONTACT_ID)),
				accountName = c.getString(c.getColumnIndex(RawContacts.ACCOUNT_NAME)),
				accountType = c.getString(c.getColumnIndex(RawContacts.ACCOUNT_TYPE));
			//String serverId = c.getString(c.getColumnIndex(Constants.CONTACT_SERVER_ID));
			Log.i(tag, "contactId: "+cId+", accountName: "+accountName+", accountType: "+accountType);//+", serverId: "+serverId);
    	} while(c.moveToNext());
    	// DatabaseHelper.getSourceIdFromContactId(c, "793");
	}

    
    public static void getGroups(Context ctx) {
    	new Db(ctx).getGroups(false);
    }
    

    public static void getDirtyGroups(Context ctx) {
    	new Db(ctx).getGroups(true);
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
    
	public static void getRawContactsTable(Context ctx) {
		Cursor c = new Db(ctx).getRawContactsCursor(false);
		c.moveToFirst();
		do {
    		String contactId = c.getString(c.getColumnIndex(RawContacts.CONTACT_ID));
    		Log.i(tag, "contactId: "+contactId);
		} while(c.moveToNext());
	}

	public static void getDataTable(Context applicationContext) {
		Cursor c = new Db(applicationContext).getData();
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

	public static void getPics(Context ctx) {
    	/*Cursor dataPicsCursor = ctx.getContentResolver().query(ContactsContract.Data.CONTENT_URI,
	    		// null,
	    	    new String[] {
	    			ContactsContract.Data.CONTACT_ID,
	    			ContactsContract.Data.RAW_CONTACT_ID,
	    			ContactsContract.CommonDataKinds.Photo.PHOTO,
	    		},
	    		ContactsContract.CommonDataKinds.Photo.PHOTO +" is not null "
	    		+"and "+Data.MIMETYPE+" = ? ",
	    	    new String[] {ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE}, 
	    	    ContactsContract.Data.CONTACT_ID);
    	Log.i(tag, "There are "+dataPicsCursor.getCount()+" pics");
		*/
    	Cursor dataPicsCursor = ctx.getContentResolver().query(ContactsContract.Data.CONTENT_URI,
	    		null,
	    	    /*new String[] {
	    			ContactsContract.Contacts._ID, 
	    			ContactsContract.Data.CONTACT_ID,
	    			ContactsContract.Data.RAW_CONTACT_ID,
	    			ContactsContract.RawContacts._ID,
	    			ContactsContract.Contacts.DISPLAY_NAME,
	    			ContactsContract.CommonDataKinds.Photo.PHOTO,
	    			Data.MIMETYPE, Data.DATA1,
	    		},*/
	    		// ContactsContract.CommonDataKinds.Photo.PHOTO +" is not null "
	    		// +"and "+
	    		Data.MIMETYPE+"=='"+ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE+"'",
	    	    null, // new String[] {ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE}, 
	    	    ContactsContract.Data.CONTACT_ID);
    	Log.i(tag, "There are "+dataPicsCursor.getCount()+" notes");
    	
		
	}

	public static void getShareBooks(Context ctx) {
		new Db(ctx).getSharingBooks(false);
	}

	public static void getContactServerData(Context applicationContext) {
		Cursor c = new Db(applicationContext).getData();
		c.moveToFirst();
		do {
			String mimetype = c.getString(c.getColumnIndex(Data.MIMETYPE));
			if(!mimetype.equals(PhonebookSyncAdapterColumns.MIME_PROFILE)) continue;
			String serverId = c.getString(c.getColumnIndex(PhonebookSyncAdapterColumns.DATA_PID)),
				contactId = c.getString(c.getColumnIndex(Data.CONTACT_ID)),
				rawContactId = c.getString(c.getColumnIndex(Data.RAW_CONTACT_ID)),
				picId = c.getString(c.getColumnIndex(PhonebookSyncAdapterColumns.PIC_ID)),
				picHash = c.getString(c.getColumnIndex(PhonebookSyncAdapterColumns.PIC_HASH));
			Log.i(tag, "contactId: "+contactId+", rawId: "+rawContactId+", serverId: "+serverId+", picId: "+picId);
		} while(c.moveToNext());
		Log.i(tag, "Got contact server data");
	}

	public static void deleteContactsServerData(Context applicationContext) {
		int num = applicationContext.getContentResolver().delete(Data.CONTENT_URI, Data.MIMETYPE + "='" + PhonebookSyncAdapterColumns.MIME_PROFILE + "'", null);
		Log.i(tag, "deleted "+num+" rows");
	}
	
}
