package com.nbos.phonebook.sync.platform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Groups;
import android.util.Log;

import com.nbos.phonebook.Db;
import com.nbos.phonebook.database.tables.BookTable;
import com.nbos.phonebook.sync.Constants;
import com.nbos.phonebook.sync.client.Contact;
import com.nbos.phonebook.sync.client.Group;
import com.nbos.phonebook.sync.client.PhoneContact;
import com.nbos.phonebook.sync.client.contact.Email;
import com.nbos.phonebook.sync.client.contact.Phone;

public class SyncManager {
	static String tag = "Sync";
	Context context;
	Db db;
	String account; 
    List<PhoneContact> allContacts; 
    Cursor rawContactsCursor;//, dataPicsCursor, dataCursor;
    Set<String> syncedContactServerIds, syncedGroupServerIds; 
    SyncPics syncPics;
    Set<String> groupRawContactIds;
	Map<String, Integer> dataRawContactIdIndex;
    BatchOperation updateGroupBatchOperation;
    Cloud cloud;
    int batchSize = 50;
	

	public SyncManager(Context context, String account, 
		List<Contact> contacts, List<Group> groups, 
		List<Group> sharedBooks, 
		// List<PicData> serverPicData, Set<String> unchangedPicsRawContactIds, 
		Set<String> syncedContactServerIds, Set<String> syncedGroupServerIds,
		SyncPics syncPics, Cloud cloud)
		// Set<String> syncedPictureIds, Map<String, String> serverPicIds) 
	{
		super();
		this.context = context;
		this.account = account;
		this.syncedContactServerIds = syncedContactServerIds;
		this.syncedGroupServerIds = syncedGroupServerIds;
		this.syncPics = syncPics;
		this.cloud = cloud;
		getDataCursors();
        syncContacts(contacts);
        syncGroups(groups, false);
        syncGroups(sharedBooks, true);
        syncPics.sync(contacts, this);
        
        closeCursors();
	}
	
	private void getDataCursors() {
		db = new Db(context);
		allContacts = UpdateContacts.getContacts(cloud);
		rawContactsCursor = db.getRawContactsCursor(false);
	}

    /*public Cursor getData() {
        final String[] PROJECTION =
            new String[] {Data._ID, Data.MIMETYPE, Data.DATA1, Data.DATA2,
                Data.DATA3, Data.DATA4, Data.DATA5, Data.DATA6, Data.DATA7, 
                Data.DATA8, Data.DATA9, Data.DATA10, 
                Data.RAW_CONTACT_ID, Data.CONTACT_ID};

    	return cloud.cr.query(Data.CONTENT_URI, PROJECTION, null, null, Data.RAW_CONTACT_ID);
    }*/

	private void closeCursors() {
		// leave serverDataCursor open for cloud queries
		// serverDataCursor.close();
		rawContactsCursor.close();
		// dataPicsCursor.close();
	}

	void syncContacts(List<Contact> contacts) {
        long rawContactId = 0;
        final BatchOperation batchOperation = new BatchOperation(context);
        Log.i(tag, "In SyncContacts: There are "+rawContactsCursor.getCount()+" raw contacts, num columns: "+rawContactsCursor.getColumnCount());
		cloud.getServerDataIds();
		Set<Long> dirtyContacts = getDirtyContacts();
        for (final Contact contact : contacts) 
        {
        	if(syncedContactServerIds.contains(contact.serverId)) continue;
            // userId = Integer.parseInt(user.getUserId());
            // Check to see if the contact needs to be inserted or updated
            rawContactId = lookupRawContact(contact);
            // TODO: insert the server ids
            boolean dirty = dirtyContacts.contains(rawContactId); // ContactManager.isDirtyContact(rawContactId, rawContactsCursor); 
            Log.d(tag, "Raw contact id is: "+rawContactId+", name: "+contact.name+", dirty: "+dirty);
            if(dirty) 
            {
            	// update the server id
            	if(!cloud.serverIds.contains(contact.serverId))
            		UpdateContacts.insertServerId(rawContactId, Long.parseLong(contact.serverId), cloud.account, batchOperation);
            	continue;
            }
                        
            if (rawContactId != 0) {
                if (!contact.deleted) // update contact
                    ContactManager.updateContact(context, account, contact, rawContactId);
                else // delete contact
                    ContactManager.deleteContact(context, rawContactId, batchOperation);
            } else {
                // add new contact
                if (!contact.deleted) {
                	Log.d(tag, "In addContact, user: "+contact.name);
                    ContactManager.addContact(context, account, contact, batchOperation);
                }
            }
            // A sync adapter should batch operations on multiple contacts,
            // because it will make a dramatic performance difference.
            if (batchOperation.size() >= 50) 
                batchOperation.execute();
            syncedContactServerIds.add(contact.serverId);            
        }
        int num = batchOperation.size();
        batchOperation.execute();
        // if(num == 0) return;
        refreshCursors();
	}
	
	private Set<Long> getDirtyContacts() {
		Set<Long> dirtyContacts = new HashSet<Long>();
		if(rawContactsCursor.getCount() == 0)
			return dirtyContacts;
		rawContactsCursor.moveToFirst();
		do {
			Long rId = rawContactsCursor.getLong(rawContactsCursor.getColumnIndex(ContactsContract.RawContacts._ID));
			String dirty = rawContactsCursor.getString(rawContactsCursor.getColumnIndex(ContactsContract.RawContacts.DIRTY));
			if(dirty.equals("1"))
				dirtyContacts.add(rId);
		} while(rawContactsCursor.moveToNext());
		return dirtyContacts;
	}

	private void refreshCursors() {
		Log.i(tag, "refreshCursors");
        cloud.serverDataCursor.requery();
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
	    if(cursor.getCount() == 0 && !g.deleted)
	    {
	    	Log.i(tag, "New group: "+account+", "+g.name+", owner: "+g.owner+", sourceId: "+g.groupId+" ,deleted:"+g.deleted);
	    	// create a group with the share book name
	    	//DatabaseHelper.createAGroup(ctx, sharedBook.name, sharedBook.owner, accountName, id);
	    	Db.createAGroup(context, g.name, isSharedBook ? g.owner : null, isSharedBook ? g.permission : null, account, Integer.parseInt(id));
	    	cursor.requery();
	    	Log.i(tag, "cursor has "+cursor.getCount()+" rows");
	    }
	    
	    if(cursor.getCount() == 0) return;
	    
	    cursor.moveToFirst();
	    String groupId = cursor.getString(cursor.getColumnIndex(ContactsContract.Groups._ID)),
	    	dirty = cursor.getString(cursor.getColumnIndex(ContactsContract.Groups.DIRTY)),
	    	deleted = cursor.getString(cursor.getColumnIndex(ContactsContract.Groups.DELETED)),
	    	oldName = cursor.getString(cursor.getColumnIndex(ContactsContract.Groups.TITLE));
	    
	    if(dirty.equals("1") || deleted.equals("1"))
	    {
	    	Log.i(tag, "Group is dirty, skipping update");
	    	return;
	    }
	    if(g.deleted)
	    {
	    	context.getContentResolver().delete(
	    			SyncManager.addCallerIsSyncAdapterParameter(Groups.CONTENT_URI),
	    			Groups.ACCOUNT_TYPE + " = ? "+" and "+Groups.SOURCE_ID + " =? ", 
		    		new String[] {Constants.ACCOUNT_TYPE, g.groupId});
	    	Log.i(tag,"Group "+ g.name+" was deleted");
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
    	//syncPics.sync(contactsToSync, this);
    	Set<String> rawContactIds = new HashSet<String>();

    	updateGroupBatchOperation = new BatchOperation(context);
  
    	cloud.getServerDataIds();
    	syncPics.sync(contactsToSync, this);
    	for(Contact u : g.contacts)
    	{
    		rawContactIds.add(updateGroupContact(u, groupId));
    		if(updateGroupBatchOperation.size() > batchSize)
    		{
    			Log.i(tag, "Executing update group batch: "+updateGroupBatchOperation.size());
    			updateGroupBatchOperation.execute();
    		}
    	}
		Log.i(tag, "Executing last update group batch: "+updateGroupBatchOperation.size());
		updateGroupBatchOperation.execute();
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
    	if(!oldName.equals(g.name))
    		Db.setNewName(groupId, g.name, context.getContentResolver());
    	
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
			String serverId = cloud.contactServerIdsMap.get(rawContactId.toString());
			Log.i(tag, "server id: "+serverId+", for raw contactId: "+rawContactId);
			if(serverId == null) continue;
			if(!g.sharingWithContactIds.contains(Long.parseLong(serverId)))
				contactsToDelete.add(rawContactId.toString());
			else
				existingContacts.add(rawContactId.toString());
		} while(bookCursor.moveToNext());
    	
		for(Long serverId : g.sharingWithContactIds)
		{
			String rawContactId = cloud.serverContactIdsMap.get(serverId.toString()); // Db.getRawContactIdFromServerId(serverId.toString(), dataCursor);
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
	
	long lookupRawContactFromServerId(String serverId) {
		
		String rawContactId = cloud.serverContactIdsMap.get(serverId.toString()); 
		if(rawContactId != null)
		{
			Log.i(tag, "rawContactId: "+rawContactId+" from serverId: "+serverId);
			return Long.parseLong(rawContactId); 
		}
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
			if(u.name.toString().trim().length() > 0 
			&& u.name.toString().equals(contact.name.toString()))
				return Long.parseLong(u.rawContactId);
		}
		return 0;
	}

    public static Uri addCallerIsSyncAdapterParameter(Uri uri) {
        return uri.buildUpon().appendQueryParameter(
            ContactsContract.CALLER_IS_SYNCADAPTER, "true").build();
    }
}
