package com.nbos.phonebook.sync.platform;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.util.ByteArrayBuffer;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.Data;
import android.util.Log;

import com.nbos.phonebook.Db;
import com.nbos.phonebook.database.tables.BookTable;
import com.nbos.phonebook.sync.client.Contact;
import com.nbos.phonebook.sync.client.ContactPicture;
import com.nbos.phonebook.sync.client.Group;
import com.nbos.phonebook.sync.client.Net;
import com.nbos.phonebook.sync.client.PhoneContact;
import com.nbos.phonebook.sync.client.contact.Email;
import com.nbos.phonebook.sync.client.contact.Phone;
import com.nbos.phonebook.util.ImageInfo;
import com.nbos.phonebook.value.PicData;

public class SyncManager {
	static String tag = "Sync";
	Context context;
	Db db;
    
	String account; 
    
    List<PhoneContact> allContacts; 
    
    Cursor dataCursor, serverDataCursor, 
    	rawContactsCursor;//, dataPicsCursor;
    
    Set<String> syncedContactServerIds, syncedGroupServerIds, syncedPictureIds,
    	unchangedPicsRawContactIds, serverDataIds;
    Map<String, String> serverPicIds;
    List<PicData> serverPicData;
    
    Set<String> groupRawContactIds;
    
    BatchOperation updateGroupBatchOperation,
    	updatePicBatchOperation,
    	updatePicIdBatchOperation;
    
    int batchSize = 50;
	
    Map<String, Integer> contactPicturesIndex;
	
	public SyncManager(Context context, String account, 
		List<Contact> contacts, List<Group> groups, 
		List<Group> sharedBooks, List<PicData> serverPicData, 
		Set<String> unchangedPicsRawContactIds, Set<String> syncedContactServerIds, 
		Set<String> syncedGroupServerIds, Set<String> syncedPictureIds, Map<String, String> serverPicIds) 
	{
		super();
		this.context = context;
		this.serverPicData = serverPicData;
		this.account = account;
		this.unchangedPicsRawContactIds = unchangedPicsRawContactIds;
		this.syncedContactServerIds = syncedContactServerIds;
		this.syncedGroupServerIds = syncedGroupServerIds;
		this.syncedPictureIds = syncedPictureIds;
		this.serverPicIds = serverPicIds;
		
		getDataCursors();
        syncContacts(contacts);
        syncGroups(groups, false);
        syncGroups(sharedBooks, true);
        syncPictures(contacts);
        closeCursors();
	}
	
	private void getDataCursors() {
		db = new Db(context);
		allContacts = db.getContacts(false);
		dataCursor = db.getData();
		serverDataCursor = db.getProfileData();
		rawContactsCursor = db.getRawContactsCursor(false);
	}

	private void closeCursors() {
		dataCursor.close();
		serverDataCursor.close();
		rawContactsCursor.close();
		// dataPicsCursor.close();
	}

	void syncContacts(List<Contact> contacts) {
        long rawContactId = 0;
        final BatchOperation batchOperation = new BatchOperation(context);
        Log.i(tag, "In SyncContacts: There are "+rawContactsCursor.getCount()+" raw contacts, num columns: "+rawContactsCursor.getColumnCount());
        serverDataCursor.requery();
        Log.i(tag, "Server data cursor has "+serverDataCursor.getCount()+" rows");
		getServerDataIds();

        for (final Contact contact : contacts) 
        {
        	if(syncedContactServerIds.contains(contact.serverId)) continue;
        	syncedContactServerIds.add(contact.serverId);
            // userId = Integer.parseInt(user.getUserId());
            // Check to see if the contact needs to be inserted or updated
            rawContactId = lookupRawContact(contact);
            boolean dirty = ContactManager.isDirtyContact(rawContactId, rawContactsCursor); 
            Log.d(tag, "Raw contact id is: "+rawContactId+", name: "+contact.name+", dirty: "+dirty);
            if(dirty) continue;
            if (rawContactId != 0) {
                if (!contact.deleted) {
                    // update contact
                    ContactManager.updateContact(context, account, contact,
                        rawContactId, batchOperation, dataCursor, serverDataIds.contains(contact.serverId));
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
		serverDataCursor.requery();
		contactPicturesIndex = db.getContactPicturesIndex();
		updatePicBatchOperation = new BatchOperation(context);
    	updatePicIdBatchOperation = new BatchOperation(context);
		for(Contact c : contacts) {
			if(c.picId == null) continue;
			syncPicture(c);
			if(updatePicBatchOperation.size() > batchSize)
			{
				Log.i(tag, "Executing update pic batch operation: "+updatePicBatchOperation.size());
				updatePicBatchOperation.execute();
			}
			
			if(updatePicIdBatchOperation.size() > batchSize)
			{
				Log.i(tag, "Executing update pic id batch operation: "+updatePicIdBatchOperation.size());
				updatePicIdBatchOperation.execute();
			}
		}
		
		Log.i(tag, "Executing last update pic batch operation: "+updatePicBatchOperation.size());
		updatePicBatchOperation.execute();
		Log.i(tag, "Executing last update pic id batch operation: "+updatePicIdBatchOperation.size());
		updatePicIdBatchOperation.execute();
		db.closeDataPicsCursor();
	}

	private void getServerDataIds() {
		serverDataIds = new HashSet<String>();
		if(serverDataCursor.getCount() == 0)
			return;
		serverDataCursor.moveToFirst();
		do {
			String serverId = serverDataCursor.getString(serverDataCursor.getColumnIndex(PhonebookSyncAdapterColumns.DATA_PID));
			if(serverId != null)
				serverDataIds.add(serverId);
		} while(serverDataCursor.moveToNext());
	}

	private void syncPicture(Contact c) {
		// check if the pic is the same in the server
		serverDataCursor.moveToFirst();
		if(serverDataCursor.getCount() > 0)
		do {
			String serverId = serverDataCursor.getString(serverDataCursor.getColumnIndex(PhonebookSyncAdapterColumns.DATA_PID));
			if(serverId == null || !c.serverId.equals(serverId)) continue;
			
			String rawContactId = serverDataCursor.getString(serverDataCursor.getColumnIndex(Data.RAW_CONTACT_ID)),
				picId = serverDataCursor.getString(serverDataCursor.getColumnIndex(PhonebookSyncAdapterColumns.PIC_ID));
			
			if(c.picId.equals(picId)) // no change in pic 
			{
				unchangedPicsRawContactIds.add(rawContactId);
				Log.i(tag, "no change in pic: "+c.picId+", serverId: "+serverId);
				return;
			}
			break;
			//updateContactPic(c);
			// pic id has changed, download the pic
			//return;
		} while(serverDataCursor.moveToNext());
		// not in the database, insert the values
		updateContactPic(c);
	}

	
	private void updateContactPic(Contact c) {
		if(syncedPictureIds.contains(c.picId))
		{
			Log.d(tag, "Already synced picture id: "+c.picId);
			return;
		}
		byte[] image = null;
		boolean sameImage = false;
        Long rawContactId = lookupRawContact(c);
        Uri uri = addCallerIsSyncAdapterParameter(Data.CONTENT_URI);
        ContactPicture contactPicture = db.getContactPicture(rawContactId.toString(), false);
        if(contactPicture == null) return;
    	byte[] photo = contactPicture.pic;
    	PicData picData = getPicData(c.serverId);
    	Boolean isServerPic = ImageInfo.isServerPic(c.serverId, photo, serverPicData);
    	// is server pic null mean its not in the server table
    	if(photo != null && isServerPic != null && isServerPic) // see if pic is already in the server data
    	// && ImageInfo.isServerPic(c.serverId, photo, serverPicData))
    	{
    		Log.i(tag, "Photo is same on server");
    		sameImage = true;
    		// updateContactPicData(rawContactId, c.serverId, c., new Long(pic.pic.length).toString(), hash);
    		// return;
    	}
    	
   		image = downloadPic(c);
   		if(image == null) return;
    	
    	if(photo != null && !sameImage) // already has a photo, update
    	{
	    	Log.i(tag, "Updating pic for serverId: "+c.serverId);
	    	ContentValues values = new ContentValues();
	    	values.put(Photo.PHOTO, image);
	    	context.getContentResolver().update(uri, values, 
	    			Data.RAW_CONTACT_ID + " = ? and " +
	    			Data.MIMETYPE + " = ? ", 
	    			new String[] {rawContactId.toString(), Photo.CONTENT_ITEM_TYPE});
    	}
        
        /*dataPicsCursor.moveToFirst();
        if(dataPicsCursor.getCount() > 0)
	    do {
	    	String mimetype = dataPicsCursor.getString(dataPicsCursor.getColumnIndex(Data.MIMETYPE));
	    	if(!mimetype.equals(Photo.CONTENT_ITEM_TYPE)) continue;
	    	
	    	String rawId = dataPicsCursor.getString(dataPicsCursor.getColumnIndex(Data.RAW_CONTACT_ID));
	    	if(!rawId.equals(rawContactId)) continue;
	    	byte[] photo = dataPicsCursor.getBlob(dataPicsCursor.getColumnIndex(Photo.PHOTO));
	    	if(photo != null 
	    	&& ImageInfo.isServerPic(c.serverId, photo, serverPicData))
	    		return;
	    	image = downloadPic(c);
	    	if(image == null) return;
	    	Log.i(tag, "Updating pic for serverId: "+c.serverId);
	    	ContentValues values = new ContentValues();
	    	values.put(Photo.PHOTO, image);
	    	context.getContentResolver().update(uri, values, 
	    			Data.RAW_CONTACT_ID + " = ? and " +
	    			Data.MIMETYPE + " = ? ", 
	    			new String[] {rawContactId.toString(), Photo.CONTENT_ITEM_TYPE});
	    	newPic = false;
	    	break;
	    } while(dataPicsCursor.moveToNext());*/
        // TODO: Batch insert the pics?
    	else if(!sameImage)// if(newPic) // insert the pic
        {
	        Log.i(tag, "inserting pic for serverId: "+c.serverId+", image is: "+image.length+", rawContactId: "+rawContactId);
	        ContentValues values = new ContentValues();
	        values.put(Data.RAW_CONTACT_ID, rawContactId);
	        values.put(Data.IS_SUPER_PRIMARY, 1);
	        // values.put(ContactsContract.CALLER_IS_SYNCADAPTER, "true");
	        values.put(Photo.PHOTO, image);
	        values.put(Data.MIMETYPE, Photo.CONTENT_ITEM_TYPE);
	        Uri result = context.getContentResolver().insert(uri, values);
	        Log.i(tag, "pic uri is: "+result);
        }
        
    	
    	if(serverDataIds.contains(c.serverId))
    	{
	    	ContentValues values = new ContentValues();
			values.put(PhonebookSyncAdapterColumns.PIC_ID, c.picId);
			values.put(PhonebookSyncAdapterColumns.PIC_SIZE, image.length);
			values.put(PhonebookSyncAdapterColumns.PIC_HASH, ImageInfo.hash(image));
			int num = context.getContentResolver().update(uri, values, 
					PhonebookSyncAdapterColumns.DATA_PID + " = ? and " + Data.MIMETYPE + " = ? ", 
					new String[] {c.serverId, PhonebookSyncAdapterColumns.MIME_PROFILE});
			Log.i(tag, "updated "+num+" entries pic id to: "+c.picId+" for serverId "+c.serverId);
    	}
    	else
    		serverPicIds.put(c.serverId, c.picId);
		syncedPictureIds.add(c.picId);
        // Uri uri = ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, Long.parseLong(contactId));
        // cr.update(uri, values, ContactsContract.Contacts._ID + " = " + contactId, null);

	}

	private PicData getPicData(String serverId) {
		for(PicData p : serverPicData)
			if(p.serverId.equals(serverId))
				return p;
		return null;
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
        serverDataCursor.requery();
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
	    	Log.i(tag, "New group: "+account+", "+g.name+", owner: "+g.owner+", sourceId: "+g.groupId);
	    	// create a group with the share book name
	    	//DatabaseHelper.createAGroup(ctx, sharedBook.name, sharedBook.owner, accountName, id);
	    	Db.createAGroup(context, g.name, isSharedBook ? g.owner : null, isSharedBook ? g.permission : null, account, Integer.parseInt(id));
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
	    syncedGroupServerIds.add(id);
	    if(isSharedBook)
	    	db.updateGroupPermission(groupId, g.permission);
	    
	    getGroupRawContactIds(groupId);
	    // Cursor groupCursor = Db.getContactsInGroup(groupId, context.getContentResolver());	    
    	Log.i(tag, "Update group: "+g.name+", id: "+groupId+", contacts: "+g.contacts);
    	List<Contact> contactsToSync = new ArrayList<Contact>();
    	for(Contact c : g.contacts)
    	{
    		// if contact has a server id it would have been part of the contacts sync if it has been modified
    		if(lookupRawContactFromServerId(c.serverId) == 0) 
    			contactsToSync.add(c);
    	}
    	Log.i(tag, "Contacts to sync in this group: "+contactsToSync.size());
    	syncContacts(contactsToSync);
    	syncPictures(contactsToSync);
    	Set<String> rawContactIds = new HashSet<String>();

    	updateGroupBatchOperation = new BatchOperation(context);
    	for(Contact u : g.contacts)
    	{
    		rawContactIds.add(updateGroupContact(u, groupId));
    		if(updateGroupBatchOperation.size() > batchSize)
    		{
    			updateGroupBatchOperation.execute();
    			Log.i(tag, "Executing update group batch: "+updateGroupBatchOperation.size());
    		}
    	}
		updateGroupBatchOperation.execute();
		Log.i(tag, "Executing last update group batch: "+updateGroupBatchOperation.size());

		for(String rawContactId : groupRawContactIds)
		{
    		if(!rawContactIds.contains(rawContactId))
    			deleteContactFromGroup(rawContactId, groupId);
		}
    	/*groupCursor.moveToFirst();
    	if(groupCursor.getCount() > 0)
    	do {
    		String rawContactId = groupCursor.getString(groupCursor.getColumnIndex(ContactsContract.Data.RAW_CONTACT_ID));
    		if(!rawContactIds.contains(rawContactId))
    			deleteContactFromGroup(rawContactId, groupId);
    	} while(groupCursor.moveToNext());
    	
    	cursor.close();
    	groupCursor.close();
    	*/
    	// update sharing with book info
    	if(isSharedBook) return;
    	
    	updateSharingWithContacts(g, groupId);
	}

	private void getGroupRawContactIds(String groupId) {
		groupRawContactIds = new HashSet<String>();
	    Cursor groupCursor = Db.getContactsInGroup(groupId, context.getContentResolver());
	    Log.i(tag, "group["+groupId+"] has "+groupCursor.getCount()+" entries");
	    if(groupCursor.getCount() == 0)
	    {
	    	groupCursor.close();
	    	return;
	    }
	    groupCursor.moveToFirst();
	    do {
	    	String rawContactId = groupCursor.getString(groupCursor.getColumnIndex(ContactsContract.Data.RAW_CONTACT_ID));
	    	groupRawContactIds.add(rawContactId);
	    } while(groupCursor.moveToNext());
	    groupCursor.close();
	}

	private void updateSharingWithContacts(Group g, String groupId) {
    	Cursor bookCursor = db.getFullBook(groupId);
    	Set<String> contactsToDelete = new HashSet<String>(),
    		existingContacts = new HashSet<String>(),
    		contactsToAdd = new HashSet<String>();
    	Log.i(tag, "Group sharing with contacts: "+g.sharingWithContactIds);
		bookCursor.moveToFirst();
		if(bookCursor.getCount() > 0)
		do {
			Long rawContactId = bookCursor.getLong(bookCursor.getColumnIndex(BookTable.CONTACTID));
			String serverId = Db.getServerIdFromRawContactId(dataCursor, rawContactId.toString());
			Log.i(tag, "server id: "+serverId+", for raw contactId: "+rawContactId);
			if(serverId == null) continue;
			if(!g.sharingWithContactIds.contains(Long.parseLong(serverId)))
				contactsToDelete.add(rawContactId.toString());
			else
				existingContacts.add(rawContactId.toString());
		} while(bookCursor.moveToNext());
    	
		for(Long serverId : g.sharingWithContactIds)
		{
			String rawContactId = Db.getRawContactIdFromServerId(serverId.toString(), dataCursor);
			if(rawContactId == null) continue;
			if(!existingContacts.contains(rawContactId))
				contactsToAdd.add(rawContactId);
		}
		Log.i(tag, "sharing contacts to add: "+contactsToAdd+", delete: "+contactsToDelete+", existing: "+existingContacts);
    	
		for(String contact : contactsToAdd)
			db.addSharingWith(groupId, contact);
		for(String contact : contactsToDelete)
			db.deleteSharingWith(groupId, contact);
	}

	void deleteContactFromGroup(String rawContactId, String groupId) {
		Log.i(tag, "Deleting contact from group: "+groupId+", contactId: "+rawContactId);
	    ContentValues values = new ContentValues();
	    values.put(ContactsContract.CommonDataKinds.GroupMembership.RAW_CONTACT_ID,
	            rawContactId);
	    values.put(
	            ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID,
	            groupId);

	    context.getContentResolver().delete(
	    		addCallerIsSyncAdapterParameter(ContactsContract.Data.CONTENT_URI), 
	            ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID
	            + " = ? and "
	            + ContactsContract.CommonDataKinds.GroupMembership.RAW_CONTACT_ID
	            +" = ?",
	            new String[] {groupId, rawContactId});	
		
	}

	private String updateGroupContact(Contact u, String groupId) {
		Long rawContactId = lookupRawContact(u);
		Log.i(tag, "updateGroupContact() groupId: "+groupId+", serverId: "+u.serverId+", rawContactId: "+rawContactId);
		updateToGroup(groupId, rawContactId.toString());
		return rawContactId.toString();
	}

	/*void updateToGroup(String groupId, String rawContactId, Cursor groupCursor) {
			if(groupId == null || rawContactId == null) return;
			if(isContactInGroup(groupId, rawContactId, groupCursor)) return;
		    ContentValues values = new ContentValues();
		    values.put(GroupMembership.RAW_CONTACT_ID, rawContactId);
		    values.put(GroupMembership.GROUP_ROW_ID, groupId);
		    values.put(GroupMembership.MIMETYPE, GroupMembership.CONTENT_ITEM_TYPE);

		    Uri uri = context.getContentResolver().insert(
		    	addCallerIsSyncAdapterParameter(ContactsContract.Data.CONTENT_URI), values);
		    Log.i(tag, "update to group uri is: "+uri);
		    // DatabaseHelper.setGroupDirty(groupId, cr);		    
	}*/

	void updateToGroup(String groupId, String rawContactId) {
		if(groupId == null || rawContactId == null
		|| groupRawContactIds.contains(rawContactId)) return;
		
		ContentProviderOperation.Builder builder = 
			ContentProviderOperation.newInsert(
	            SyncManager.addCallerIsSyncAdapterParameter(Data.CONTENT_URI))
	            .withYieldAllowed(true);

	    ContentValues values = new ContentValues();
	    values.put(GroupMembership.RAW_CONTACT_ID, rawContactId);
	    values.put(GroupMembership.GROUP_ROW_ID, groupId);
	    values.put(GroupMembership.MIMETYPE, GroupMembership.CONTENT_ITEM_TYPE);
        builder.withValues(values);
        updateGroupBatchOperation.add(builder.build());
	    // DatabaseHelper.setGroupDirty(groupId, cr);		    
	}
	
	long lookupRawContactFromServerId(String contactServerId) {
		serverDataCursor.moveToFirst();
		if(serverDataCursor.getCount() > 0)
		do {
			String mimeType = serverDataCursor.getString(serverDataCursor.getColumnIndex(Data.MIMETYPE));
			String serverId = serverDataCursor.getString(serverDataCursor.getColumnIndex(PhonebookSyncAdapterColumns.DATA_PID));
			if(!mimeType.equals(PhonebookSyncAdapterColumns.MIME_PROFILE)
			|| !serverId.equals(contactServerId))
				continue;
			long rawContactId = serverDataCursor.getLong(serverDataCursor.getColumnIndex(Data.RAW_CONTACT_ID));
			Log.i(tag, "rawContactId: "+rawContactId+", serverId: "+serverId);
			return rawContactId;
		} while(serverDataCursor.moveToNext()); 
		return 0;
	}
	
	long lookupRawContact(Contact contact) {
		long sId = lookupRawContactFromServerId(contact.serverId); 
		if(sId != 0)
			return sId;

		// could not find the contact, do a phone number and email search		
		for(PhoneContact u : allContacts) 
		{
			for(Phone p : u.phones)
			{
				for(Phone cp : contact.phones)
				{
					if(p.number.equals(cp.number)
					&& u.rawContactId != null)
						return Long.parseLong(u.rawContactId);
				}
			}
			
			for(Email e : u.emails)
			{
				for(Email ce : contact.emails)
				{
					if(ce.address.equals(e.address)
					&& u.rawContactId != null)
						return Long.parseLong(u.rawContactId);
				}
			}
			// Log.d(tag, "Compare "+u.name+" = "+contact.name+", "+u.name.toString().equals(contact.name.toString()));
			if(u.name.toString().equals(contact.name.toString()))
				return Long.parseLong(u.rawContactId);
		}
		return 0;
	}

    public static Uri addCallerIsSyncAdapterParameter(Uri uri) {
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
