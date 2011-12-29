package com.nbos.phonebook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.IntentSender.SendIntentException;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.provider.ContactsContract.AggregationExceptions;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Groups;
import android.provider.ContactsContract.RawContacts;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.nbos.phonebook.database.tables.BookTable;
import com.nbos.phonebook.database.tables.ContactTable;
import com.nbos.phonebook.sync.Constants;
import com.nbos.phonebook.sync.client.PhoneContact;
import com.nbos.phonebook.sync.platform.BatchOperation;
import com.nbos.phonebook.sync.platform.Cloud;
import com.nbos.phonebook.sync.platform.PhonebookSyncAdapterColumns;
import com.nbos.phonebook.sync.platform.SyncManager;
import com.nbos.phonebook.sync.platform.UpdateContacts;

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

    public static void resetDirtyContacts(Context ctx) {
    	// UpdateContacts.resetDirtyContacts(ctx);
    }
    public static void getRawContacts(Context ctx) {
    	Cursor c = new Db(ctx).getRawContactsCursor(false);
    	Log.i(tag, "There are "+c.getCount()+" raw contacts");
    	if(c.getCount() == 0) return;
    	c.moveToFirst();
    	do {
			String cId = c.getString(c.getColumnIndex(RawContacts.CONTACT_ID)),
				rawContactId = c.getString(c.getColumnIndex(RawContacts._ID)),
				dirty = c.getString(c.getColumnIndex(RawContacts.DIRTY)),
				deleted = c.getString(c.getColumnIndex(RawContacts.DELETED)),
				accountName = c.getString(c.getColumnIndex(RawContacts.ACCOUNT_NAME)),
				accountType = c.getString(c.getColumnIndex(RawContacts.ACCOUNT_TYPE));
			//String serverId = c.getString(c.getColumnIndex(Constants.CONTACT_SERVER_ID));
			Log.i(tag, "contactId: "+cId+", raw: "+rawContactId+", deleted: "+deleted+", accountName: "+accountName+", accountType: "+accountType+", dirty: "+dirty);//+", serverId: "+serverId);
    	} while(c.moveToNext());
    	// DatabaseHelper.getSourceIdFromContactId(c, "793");
	}

    
    /*public static void getGroups(Context ctx) {
    	new Db(ctx).getGroups(false, new HashSet<String>());
    }
    

    public static void getDirtyGroups(Context ctx) {
    	new Db(ctx).getGroups(true, new HashSet<String>());
    }*/
    
    public static void getGroupList(Context ctx) {
    	Cursor c = ctx.getContentResolver().query(ContactsContract.Groups.CONTENT_SUMMARY_URI, null,
    	    	null,	    		
    	    	null, null);
    	c.moveToFirst();
    	do {
    		String name = c.getString(c.getColumnIndex(ContactsContract.Groups.TITLE)),
    			id = c.getString(c.getColumnIndex(ContactsContract.Groups._ID)),
    			serverId = c.getString(c.getColumnIndex(ContactsContract.Groups.SOURCE_ID)),
    			dirty = c.getString(c.getColumnIndex(Groups.DIRTY)),
    		    deleted = c.getString(c.getColumnIndex(Groups.DELETED));
    		Log.i(tag, "Group; name: "+name+", id: "+id+", serverId: "+serverId+ " , deleted: "+deleted+" ,dirty: "+dirty);
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
    		String contactId = c.getString(c.getColumnIndex(RawContacts.CONTACT_ID)),
    		        rawId = c.getString(c.getColumnIndex(RawContacts._ID)),
    		        mimeType = c.getString(c.getColumnIndex(Data.MIMETYPE)),
    		        name = c.getString(c.getColumnIndex(Data.DISPLAY_NAME)),
    		        deleted = c.getString(c.getColumnIndex(RawContacts.DELETED)),
    		        dirty = c.getString(c.getColumnIndex(RawContacts.DIRTY));
    		Log.i(tag, "contactId: "+contactId+",rawId: "+rawId+" ,name: "+name+" ,deleted: "
    				+deleted+ " ,dirty: "+dirty+" ,mimetype: "+mimeType);
		} while(c.moveToNext());
	}

	/*public static void getDataTable(Context applicationContext) {
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
	}*/

	/*public static void getServerDataTable(Context applicationContext) {
		Cursor c = new Db(applicationContext).getData();
		if(c.getCount() == 0)
		{
			Log.i(tag, "There is no server data");
			return;
		}
		int count = 0;
		c.moveToFirst();
		do {
    		String rawContactId = c.getString(c.getColumnIndex(Data.RAW_CONTACT_ID));
    		String contactId = c.getString(c.getColumnIndex(Data.CONTACT_ID));
    		String serverId = c.getString(c.getColumnIndex(Data.DATA1));
    		String data2 = c.getString(c.getColumnIndex(Data.DATA2));
    		String picId = c.getString(c.getColumnIndex(Data.DATA4));
    		String picSize = c.getString(c.getColumnIndex(Data.DATA5));
    		String picHash = c.getString(c.getColumnIndex(Data.DATA6));
    		
    		
    		String mimeType = c.getString(c.getColumnIndex(Data.MIMETYPE));
    		if(!mimeType.equals(PhonebookSyncAdapterColumns.MIME_PROFILE)) continue;
    		count++;
    		Log.i(tag, "raw contactId: "+rawContactId+", contactId: "+contactId+", data1: "+serverId+", picId: "+picId+", picSize: "+picSize+", picHash: "+picHash);
		} while(c.moveToNext());
		Log.i(tag, "There are "+count+" server data rows");
	}*/
	
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

	/* public static void getShareBooks(Context ctx) {
		new Db(ctx).getSharingBooks(false);
	}*/

	/*public static void getContactServerData(Context applicationContext) {
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
	}*/

	public static void deleteContactsServerData(Context applicationContext) {
		int num = applicationContext.getContentResolver().delete(Data.CONTENT_URI, Data.MIMETYPE + "='" + PhonebookSyncAdapterColumns.MIME_PROFILE + "'", null);
		Log.i(tag, "deleted "+num+" rows");
	}

	static Map<String, Set<String>> getLinkedContacts(Cursor c, String contactIdColumn, String rawContactIdColumn) {
		Map<String, Set<String>> linkedContacts = new HashMap<String, Set<String>>();
		Log.i(tag, "There are "+c.getCount()+" rows");
		if(c.getCount() == 0)
			return linkedContacts;
		c.moveToFirst();
		String prevContactId = null, contactId;
		Set<String> rawContactIds = new HashSet<String>();
		do {
			contactId = c.getString(c.getColumnIndex(contactIdColumn));
			String rawContactId = c.getString(c.getColumnIndex(rawContactIdColumn));
			Log.i(tag, "c: "+contactId+", raw: "+rawContactId);
			if(prevContactId == null) prevContactId = contactId;
			if(!prevContactId.equals(contactId))
			{
				if(rawContactIds.size() > 1)
					linkedContacts.put(prevContactId, rawContactIds);
				rawContactIds = new HashSet<String>();
				prevContactId = contactId;
			}
			rawContactIds.add(rawContactId);
		} while(c.moveToNext());
		
		if(rawContactIds.size() > 1)
			linkedContacts.put(prevContactId, rawContactIds);
		
		Log.i(tag, "There are "+linkedContacts.size()+" linked contacts");
		for(String ct : linkedContacts.keySet())
		{
			Log.i(tag, "Contact: "+ct);
			for(String r : linkedContacts.get(ct))
			{
				Log.i(tag, "Raw contact: "+r);
				// insert into contact table
		        //ContentValues bookValues = new ContentValues();
		        //bookValues.put(ContactTable.CONTACTID, ct);
		        // bookValues.put(ContactTable.RAWCONTACTID, r);
		        //Uri cUri = applicationContext.getContentResolver()
		        	//.insert(Constants.CONTACT_URI, bookValues);
		        // Log.i(tag, "inserted: "+cUri);
			}
		}
		
		return linkedContacts;
		
	}
	
	public static void getContactLinks(Context applicationContext) {
		Map<String, Set<String>> linkedContacts,
			storedLinkedContacts, 
			deletedLinks = new HashMap<String, Set<String>>(), 
			newLinks = new HashMap<String, Set<String>>(); 
		Cursor c = applicationContext.getContentResolver()
			.query(RawContacts.CONTENT_URI,  
					new String[]{
						RawContacts._ID,
						RawContacts.CONTACT_ID
					}, null, null, 
					RawContacts.CONTACT_ID);
		linkedContacts = getLinkedContacts(c, RawContacts.CONTACT_ID, RawContacts._ID);
		
		c = applicationContext.getContentResolver()
		.query(Constants.CONTACT_URI,
	    		null, null,null, 
	    		ContactTable.CONTACTID);
		Log.i(tag, "There are "+c.getCount()+" stored contact entries");
		storedLinkedContacts = getLinkedContacts(c, ContactTable.CONTACTID, ContactTable.RAWCONTACTID);
		Set<String> linkedKeys = linkedContacts.keySet(),
			storedKeys = storedLinkedContacts.keySet();
		// compare the two sets of contacts
		if(linkedKeys.equals(storedKeys))
		{
			Log.i(tag, "No links were added or deleted");
			for(String k : linkedKeys)
			{
				if(!linkedContacts.get(k).equals(storedLinkedContacts.get(k)))
				{
					Log.i(tag, "Contacts changed for: "+k);
					deletedLinks.put(k, storedLinkedContacts.get(k));
					newLinks.put(k, linkedContacts.get(k));
				}
			}
		}
		else 
		{
			for(String k : linkedKeys)
			{
				if(!storedKeys.contains(k))
				{
					Log.i(tag, "Added link: "+k);
					newLinks.put(k, linkedContacts.get(k));
				}
			}

			for(String k : storedKeys)
			{
				if(!linkedKeys.contains(k))
				{
					Log.i(tag, "Deleted link: "+k);
					deletedLinks.put(k, storedLinkedContacts.get(k));
				}
			}
		}
		sendLinkUpdates(linkedContacts);
	}
	
	private static void sendLinkUpdates(Map<String, Set<String>> linkedContacts) {
		// List<NameValuePair> params = getAuthParams();
		// if(!newOnly) {
			// send all the links
			Object[] linkedContactsArray = linkedContacts.values().toArray();
			Integer numLinks = new Integer(linkedContactsArray.length);
			// params.add(new BasicNameValuePair("numLinks", numLinks.toString()));
			for(int i=0; i< numLinks; i++)
			{
				Log.i(tag, "Obj: "+linkedContactsArray[i]);
				Object[] rawContactIds = ((Set<String>) linkedContactsArray[i]).toArray();
				// String [] rawContactIds = (String[]) ((Set<String>) linkedContactsArray[i]).toArray();
				Log.i(tag, "num raw contacts: "+rawContactIds.length);
				for(int j=0; j< rawContactIds.length; j++)
				{
					Log.i(tag, "Raw #"+j+": "+rawContactIds[j]);
				}
				// params.add(new BasicNameValuePair("link_"+i+"_count", rawContactId));
				// params.add(new BasicNameValuePair("contactId_"+index, rawContactId));
			}
		// }
	}

	public static void getStoredContactLinks(Context applicationContext) {

	}

	public static void deleteContactLinkTable(Context applicationContext) {
		int num = applicationContext.getContentResolver()
			.delete(Constants.CONTACT_URI, null, null);
		Log.i(tag, "Deleted "+num+" links");
	}
	
	public static void joinContacts(Context applicationContext) {
		// setAggregationException(rawContactId, AggregationExceptions.TYPE_KEEP_TOGETHER);
        ContentValues values = new ContentValues(3);
        Log.i(tag, "joining 4534 and 4535");
        // for (long aRawContactId : mRawContactIds) {
            //if (aRawContactId != rawContactId) {
                values.put(AggregationExceptions.RAW_CONTACT_ID1, 4534);
                values.put(AggregationExceptions.RAW_CONTACT_ID2, 4535);
                values.put(AggregationExceptions.TYPE, AggregationExceptions.TYPE_KEEP_TOGETHER);
                applicationContext.getContentResolver()
                	.update(AggregationExceptions.CONTENT_URI, values, null, null);
            //}
        //}
	}
	
	public void batch(String serverId, BatchOperation batchOperation) {
		ContentProviderOperation.Builder builder = 
			ContentProviderOperation.newInsert(
	            SyncManager.addCallerIsSyncAdapterParameter(Data.CONTENT_URI))
	            .withYieldAllowed(true);
		ContentValues values = new ContentValues();
        values.put(PhonebookSyncAdapterColumns.DATA_PID, serverId);
        values.put(Data.MIMETYPE, PhonebookSyncAdapterColumns.MIME_PROFILE);
        builder.withValues(values);
        batchOperation.add(builder.build());
	}
	
	public static void getDataPicsCursor(Context context) {
    	Cursor dataPicsCursor = context.getContentResolver().query(
    			Data.CONTENT_URI,
	    		// null,
	    	    new String[] {
	    			Data.CONTACT_ID,
	    			Contacts.DISPLAY_NAME,
	    			Data.RAW_CONTACT_ID,
	    			Data.MIMETYPE,
	    			Photo.PHOTO,
	    		},
	    		Photo.PHOTO +" is not null "
	    		+" and "+Data.MIMETYPE+" = ? ",
	    		// null,
	    		//+"and "+Data.MIMETYPE+" = ? ",
	    	    new String[] {Photo.CONTENT_ITEM_TYPE}, 
	    	    ContactsContract.Data.CONTACT_ID);
    	Log.i(tag, "Data pics cursor has "+dataPicsCursor.getCount()+" rows");
		Map<String, Integer>contactPicturesIndex = new HashMap<String, Integer>();
		dataPicsCursor.requery();
		dataPicsCursor.moveToFirst();
		if(dataPicsCursor.getCount() == 0) 
			return; // contactPicturesIndex;
		do {
	    	String name = dataPicsCursor.getString(dataPicsCursor.getColumnIndex(Data.DISPLAY_NAME));
	    	String rawId = dataPicsCursor.getString(dataPicsCursor.getColumnIndex(Data.RAW_CONTACT_ID));
	    	byte [] photo = dataPicsCursor.getBlob(dataPicsCursor.getColumnIndex(Photo.PHOTO));
	    	
	    	Log.i(tag, name+", raw: "+rawId+", index: "+dataPicsCursor.getPosition()+", photo: "+ (photo == null ? " null " : photo.length));
	    	contactPicturesIndex.put(rawId, new Integer(dataPicsCursor.getPosition()));//new ContactPicture(pic, contentType));	    	
		} while(dataPicsCursor.moveToNext());
    	
	}
	
	public static void getContacts(Context context) 
	{
		Map<String, String> contactDisplayNameMap = new HashMap<String, String>();
    	Cursor contactsCursor = context.getContentResolver().query(Contacts.CONTENT_URI, null, null, null,
				Contacts._ID);

    	Log.i(tag, "There are "+contactsCursor.getCount()+" contacts");
    	contactsCursor.moveToFirst();
    	do {
    		String contactId = contactsCursor.getString(contactsCursor.getColumnIndex(Contacts._ID)),
    			name = contactsCursor.getString(contactsCursor.getColumnIndex(Contacts.DISPLAY_NAME));
    		
    		Log.i(tag, "id: "+contactId+", name: "+name);
    	} while(contactsCursor.moveToNext());
		
		Cursor cursor = UpdateContacts.getRawContactsEntityCursor(context.getContentResolver(), false);
    	PhoneContact contact = null;
    	if(cursor.getCount() == 0) return;
    	cursor.moveToFirst();
    	do {
    		String  contactId = cursor.getString(cursor.getColumnIndex(RawContacts.CONTACT_ID)),
    			rawContactId = cursor.getString(cursor.getColumnIndex(RawContacts._ID)),
    			deleted = cursor.getString(cursor.getColumnIndex(RawContacts.DELETED)),
    			mimeType = cursor.getString(cursor.getColumnIndex(Data.MIMETYPE)),
    			accountType = cursor.getString(cursor.getColumnIndex(RawContacts.ACCOUNT_TYPE)),
    			dirty = cursor.getString(cursor.getColumnIndex(RawContacts.DIRTY));
    			if(contactId == null)
    				Log.i(tag,"contactId: "+contactId+" ,rawId: "+rawContactId+" ,deleted: "+deleted
    					+" ,accountType: "+accountType+", dirty: "+dirty+", mimeType: "+mimeType);
    	} while(cursor.moveToNext());
	}
	
	static Cursor getRawContactsEntityCursor(ContentResolver cr, boolean newOnly) {
	    String where = newOnly ? ContactsContract.RawContacts.DIRTY + " = 1" : null;
	    Log.i(tag,"newonly: "+newOnly);
		return cr.query(ContactsContract.RawContactsEntity.CONTENT_URI, null, where, null, ContactsContract.RawContacts._ID);	
	}
}
