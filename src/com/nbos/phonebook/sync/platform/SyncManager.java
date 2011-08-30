package com.nbos.phonebook.sync.platform;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.http.util.ByteArrayBuffer;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.util.Log;

import com.nbos.phonebook.Db;
import com.nbos.phonebook.sync.client.Contact;
import com.nbos.phonebook.sync.client.Group;
import com.nbos.phonebook.sync.client.Net;

public class SyncManager {
	static String tag = "SyncManager";
	Context context;
    String account; 
    // List<PhoneContact> allContacts; 
    Cursor dataCursor, rawContactsCursor, dataPicsCursor;
    Set<String> syncedContacts = new HashSet<String>();
	public SyncManager(Context context, String account, Object[] update) {
		super();
		this.context = context;
		this.account = account;
		// this.allContacts = Db.getContacts(false, context);
		this.dataCursor = Db.getData(context);
		rawContactsCursor = Db.getRawContactsCursor(context.getContentResolver(), false);
		
        List<Contact> contacts =  (List<Contact>) update[0];
        List<Group> groups = (List<Group>) update[1];
        List<Group> sharedBooks = (List<Group>) update[2];
        syncContacts(contacts);
        syncGroups(groups, false);
        syncGroups(sharedBooks, true);
        syncPictures(contacts);
	}

	void syncContacts(List<Contact> contacts) {
        long rawContactId = 0;
        final BatchOperation batchOperation = new BatchOperation(context);
        Log.i(tag, "In SyncContacts: There are "+rawContactsCursor.getCount()+" raw contacts, num columns: "+rawContactsCursor.getColumnCount());
        for (final Contact contact : contacts) 
        {
        	if(syncedContacts.contains(contact.serverId)) continue;
        	syncedContacts.add(contact.serverId);
            // userId = Integer.parseInt(user.getUserId());
            // Check to see if the contact needs to be inserted or updated
            rawContactId = lookupRawContact(contact);
            boolean dirty = ContactManager.isDirtyContact(rawContactId, rawContactsCursor); 
            Log.d(tag, "Raw contact id is: "+rawContactId+", dirty: "+dirty);
            if(dirty) continue;
            if (rawContactId != 0) {
                if (!contact.deleted) {
                    // update contact
                    ContactManager.updateContact(context, account, contact,
                        rawContactId, batchOperation, dataCursor);
                } else {
                    // delete contact
                    ContactManager.deleteContact(context, rawContactId, batchOperation);
                }
            } else {
                // add new contact
                Log.d(tag, "In addContact, user: "+contact.name);
                if (!contact.deleted) {
                    ContactManager.addContact(context, account, contact, batchOperation);
                }
            }
            // A sync adapter should batch operations on multiple contacts,
            // because it will make a dramatic performance difference.
            if (batchOperation.size() >= 50) {
                batchOperation.execute();
            }
        }
        int num = batchOperation.size();
        batchOperation.execute();
        // if(num == 0) return;
        refreshCursors();
	}

	private void syncPictures(List<Contact> contacts) {
		getDataPicsCursor();
		for(Contact c : contacts) {
			if(c.picId == null) continue;
			syncPicture(c);
		}
		dataPicsCursor.close();
	}

	private void getDataPicsCursor() {
    	dataPicsCursor = context.getContentResolver().query(ContactsContract.Data.CONTENT_URI,
	    		// null,
	    	    new String[] {
	    			Data.CONTACT_ID,
	    			Data.RAW_CONTACT_ID,
	    			Data.MIMETYPE,
	    			CommonDataKinds.Photo.PHOTO,
	    		},
	    		ContactsContract.CommonDataKinds.Photo.PHOTO +" is not null ",
	    		null,
	    		//+"and "+Data.MIMETYPE+" = ? ",
	    	    //new String[] {ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE}, 
	    	    ContactsContract.Data.CONTACT_ID);
    	Log.i(tag, "Data pics cursor has "+dataPicsCursor.getCount()+" rows");
	}

	private void syncPicture(Contact c) {
		// check if the pic is the same in the server
		dataCursor.moveToFirst();
		if(dataCursor.getCount() > 0)
		do {
			String mimetype = dataCursor.getString(dataCursor.getColumnIndex(Data.MIMETYPE));
			if(!mimetype.equals(PhonebookSyncAdapterColumns.MIME_PROFILE)) continue;
			String serverId = dataCursor.getString(dataCursor.getColumnIndex(PhonebookSyncAdapterColumns.DATA_PID));
			if(serverId == null || !c.serverId.equals(serverId)) continue;
			
			String picId = dataCursor.getString(dataCursor.getColumnIndex(PhonebookSyncAdapterColumns.PIC_ID));
			if(c.picId.equals(picId)) // no change in pic 
			{
				Log.i(tag, "no change in pic: "+c.picId+", serverId: "+serverId);
				return;
			}
			updateContactPic(c);
			// pic id has changed, download the pic
			return;
		} while(dataCursor.moveToNext());
		// not in the database, insert the values
		updateContactPic(c);
	}

	private void updateContactPic(Contact c) {
		byte[] image = downloadPic(c);
		if(image == null) return;
		boolean newPic = true;
        String contactId = getContactIdFromServerId(c.serverId);
        Uri uri = addCallerIsSyncAdapterParameter(Data.CONTENT_URI);
        dataPicsCursor.moveToFirst();
        if(dataPicsCursor.getCount() > 0)
	    do {
	    	String mimetype = dataPicsCursor.getString(dataPicsCursor.getColumnIndex(ContactsContract.Data.MIMETYPE));
	    	if(!mimetype.equals(Photo.CONTENT_ITEM_TYPE)) continue;
	    	
	    	String cId = dataPicsCursor.getString(dataPicsCursor.getColumnIndex(Data.CONTACT_ID));
	    	if(!cId.equals(contactId)) continue;
	    	Log.i(tag, "Updating pic for serverId: "+c.serverId);
	    	ContentValues values = new ContentValues();
	    	values.put(Photo.PHOTO, image);
	    	context.getContentResolver().update(uri, values, 
	    			Data.CONTACT_ID + " = ? and " +
	    			Data.MIMETYPE + " = ? ", 
	    			new String[] {contactId, Photo.CONTENT_ITEM_TYPE});
	    	newPic = false;
	    	break;
	    } while(dataCursor.moveToNext());
        if(newPic)
        {
	        // insert the pic
	        Log.i(tag, "inserting pic for serverId: "+c.serverId+", image is: "+image.length);
	        ContentValues values = new ContentValues();
	        values.put(ContactsContract.Data.RAW_CONTACT_ID, Db.getRawContactId(contactId, rawContactsCursor));
	        values.put(ContactsContract.Data.IS_SUPER_PRIMARY, 1);
	        // values.put(ContactsContract.CALLER_IS_SYNCADAPTER, "true");
	        values.put(ContactsContract.CommonDataKinds.Photo.PHOTO, image);
	        values.put(ContactsContract.Data.MIMETYPE, Photo.CONTENT_ITEM_TYPE);
	        Uri result = context.getContentResolver().insert(uri, values);
	        Log.i(tag, "pic uri is: "+result);
        }
		ContentValues values = new ContentValues();
		values.put(PhonebookSyncAdapterColumns.PIC_ID, c.picId);
		values.put(PhonebookSyncAdapterColumns.PIC_SIZE, image.length);
		values.put(PhonebookSyncAdapterColumns.PIC_HASH, Cloud.hash(image));
		int num = context.getContentResolver().update(uri, values, 
				PhonebookSyncAdapterColumns.DATA_PID + " = ? and " + Data.MIMETYPE + " = ? ", 
				new String[] {c.serverId, PhonebookSyncAdapterColumns.MIME_PROFILE});
		Log.i(tag, "updated "+num+" entries pic id to: "+c.picId+" for serverId "+c.serverId);
        // Uri uri = ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, Long.parseLong(contactId));
        // cr.update(uri, values, ContactsContract.Contacts._ID + " = " + contactId, null);

	}

	private byte[] downloadPic(Contact c) {
		URL url;
		try {
			url = new URL(Net.DOWNLOAD_CONTACT_PIC_URI + c.picId);
			URLConnection ucon = url.openConnection();
	        InputStream is = ucon.getInputStream();
	        BufferedInputStream bis = new BufferedInputStream(is, 8192);
	        /*
	         * Read bytes to the Buffer until there is nothing more to read(-1).
	         */
	        ByteArrayBuffer baf = new ByteArrayBuffer(50);
	        int current = 0;
	        while ((current = bis.read()) != -1) 
	        	baf.append((byte) current);
	        byte[] image = baf.toByteArray();
	        Log.i(tag, "Contact: "+c.serverId+", image size: "+image.length);
	        return image;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private void refreshCursors() {
		Log.i(tag, "refreshCursors");
        dataCursor.requery();
        rawContactsCursor.requery();
	}

	void syncGroups(List<Group> groups, boolean isSharedBook) {
		for(Group g : groups)
			updateGroup(g, isSharedBook);
	}

	private void updateGroup(Group g, boolean isSharedBook) {
	    String id = g.groupId;
	    ContentResolver cr = context.getContentResolver();
	    Cursor cursor = cr.query(ContactsContract.Groups.CONTENT_URI, null,  
	    		ContactsContract.Groups.SOURCE_ID + " = "+id, null, null);
	    if(cursor.getCount() == 0)
	    {
	    	Log.i(tag, "New group: "+account);
	    	// create a group with the share book name
	    	//DatabaseHelper.createAGroup(ctx, sharedBook.name, sharedBook.owner, accountName, id);
	    	Db.createAGroup(context, g.name, isSharedBook ? g.owner : null , account, Integer.parseInt(id));
	    	cursor.requery();
	    	Log.i(tag, "cursor has "+cursor.getCount()+" rows");
	    }
	    cursor.moveToFirst();
	    String groupId = cursor.getString(cursor.getColumnIndex(ContactsContract.Groups._ID)),
	    	dirty = cursor.getString(cursor.getColumnIndex(ContactsContract.Groups.DIRTY));
	    
	    if(dirty.equals("1"))
	    {
	    	Log.i(tag, "Group is dirty, skipping update");
	    	return;
	    }
	    Cursor groupCursor = Db.getContactsInGroup(groupId, context.getContentResolver());	    
    	Log.i(tag, "Update group: "+g.name+", id: "+groupId+", contacts: "+g.contacts+", group cursor size: "+groupCursor.getCount());
    	syncContacts(g.contacts);
    	syncPictures(g.contacts);
    	Set<String> contactIds = new HashSet<String>();
    	for(Contact u : g.contacts)
    		contactIds.add(updateGroupContact(u, groupId));
    	groupCursor.moveToFirst();
    	if(groupCursor.getCount() > 0)
    	do {
    		String contactId = groupCursor.getString(groupCursor.getColumnIndex(ContactsContract.Data.CONTACT_ID));
    		if(!contactIds.contains(contactId))
    			deleteContactFromGroup(contactId, groupId);
    	} while(groupCursor.moveToNext());
    	
    	cursor.close();
    	groupCursor.close();
	}

	void deleteContactFromGroup(String contactId, String groupId) {
		Log.i(tag, "Deleting contact from group: "+groupId+", contactId: "+contactId);
	    ContentValues values = new ContentValues();
	    values.put(ContactsContract.CommonDataKinds.GroupMembership.CONTACT_ID,
	            contactId);
	    values.put(
	            ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID,
	            groupId);

	    context.getContentResolver().delete(
	    		addCallerIsSyncAdapterParameter(ContactsContract.Data.CONTENT_URI), 
	            ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID
	            + " = ? and "
	            + ContactsContract.CommonDataKinds.GroupMembership.CONTACT_ID
	            +" = ?",
	            new String[] {groupId, contactId});	
		
	}

	private String updateGroupContact(Contact u, String groupId) {
		String contactId = getContactIdFromServerId(u.serverId),
			rawContactId = Db.getRawContactId(contactId, rawContactsCursor);
		Log.i(tag, "updateGroupContact() groupId: "+groupId+", serverId: "+u.serverId+", contactId: "+contactId+", rawContactId: "+rawContactId);
		updateToGroup(groupId, contactId, rawContactId);
		return contactId;
	}

	void updateToGroup(String groupId, String contactId, String rawContactId) {
			if(groupId == null || contactId == null || rawContactId == null) return;
			if(isContactInGroup(groupId, contactId)) return;
		    ContentValues values = new ContentValues();
		    values.put(ContactsContract.CommonDataKinds.GroupMembership.RAW_CONTACT_ID,
		            rawContactId);
		    values.put(ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID,
		            groupId);
		    values.put(ContactsContract.CommonDataKinds.GroupMembership.MIMETYPE,
		    		ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE);

		    Uri uri = context.getContentResolver().insert(
		    	addCallerIsSyncAdapterParameter(ContactsContract.Data.CONTENT_URI), values);
		    Log.i(tag, "insert uri is: "+uri);
		    // DatabaseHelper.setGroupDirty(groupId, cr);		    
	}
	
	boolean isContactInGroup(String groupId, String contactId) {
	    Cursor c = context.getContentResolver()
	    	.query(ContactsContract.Data.CONTENT_URI,
	    		// null,
	    	    new String[] {
	    			ContactsContract.Contacts._ID, 
	    			ContactsContract.Data.CONTACT_ID, 
	    			ContactsContract.RawContacts._ID,
	    			ContactsContract.Contacts.DISPLAY_NAME
	    		},
	    	    ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID+" = "+groupId
	    	    +" and "+ContactsContract.Data.CONTACT_ID + " = "+contactId,
	    	    null, ContactsContract.Data.CONTACT_ID);
	    Log.i(tag, "isContactInGroup() groupId: "+groupId+", contactId: "+contactId+", num results: "+c.getCount());
	    return c.getCount() > 0;
	}
	
	private String getContactIdFromServerId(String serverId) {
		if(dataCursor.getCount() == 0) return null;
		dataCursor.moveToFirst();
		do {
			String mimeType = dataCursor.getString(dataCursor.getColumnIndex(Data.MIMETYPE));
			String sId = dataCursor.getString(dataCursor.getColumnIndex(PhonebookSyncAdapterColumns.DATA_PID));
			if(!mimeType.equals(PhonebookSyncAdapterColumns.MIME_PROFILE)
			|| !sId.equals(serverId))
				continue;
			String contactId = dataCursor.getString(dataCursor.getColumnIndex(Data.CONTACT_ID));
			Log.i(tag, "getContactIdFromServerId("+serverId+") = "+contactId);
			return contactId;
		} while(dataCursor.moveToNext());
		return null;
	}

	long lookupRawContact(Contact contact) {
		dataCursor.moveToFirst();
		if(dataCursor.getCount() > 0)
		do {
			String mimeType = dataCursor.getString(dataCursor.getColumnIndex(Data.MIMETYPE));
			String serverId = dataCursor.getString(dataCursor.getColumnIndex(PhonebookSyncAdapterColumns.DATA_PID));
			if(!mimeType.equals(PhonebookSyncAdapterColumns.MIME_PROFILE)
			|| !serverId.equals(contact.serverId))
				continue;
			long rawContactId = dataCursor.getLong(dataCursor.getColumnIndex(Data.RAW_CONTACT_ID));
			Log.i(tag, "lookupRawContact returning rawContactId: "+rawContactId+", for serverId: "+serverId);
			return rawContactId;
		} while(dataCursor.moveToNext()); 

		// could not find the contact, do a phone number search		
		/*for(PhoneContact u : allContacts) {
			if(u.number.equals(contact.number)) {// maybe a contact
				// check if the rest of the information is the same
				if(u.name.equals(contact.name))
				{
					Log.i(tag, "Existing contact; ph: "+u.number+", name: "+u.name+", serverId: "+contact.serverId+", contactId: "+u.contactId+", phone serverId: "+u.serverId+", rawContactId: "+u.rawContactId);
					// update the serverId of the contact
					Db.updateContactServerId(u.contactId, contact.serverId, context, rawContactsCursor);
					return Long.parseLong(u.rawContactId);
				}
			}
		}*/
		return 0;
	}

    private static Uri addCallerIsSyncAdapterParameter(Uri uri) {
        return uri.buildUpon().appendQueryParameter(
            ContactsContract.CALLER_IS_SYNCADAPTER, "true").build();
    }
	
	public void close() {
    	if(dataCursor != null)
    		dataCursor.close();
    	if(rawContactsCursor != null)
    		rawContactsCursor.close();
	}
}
