package com.nbos.phonebook.sync.platform;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;

import com.nbos.phonebook.DatabaseHelper;
import com.nbos.phonebook.sync.Constants;
import com.nbos.phonebook.sync.client.Contact;
import com.nbos.phonebook.sync.client.Group;
import com.nbos.phonebook.sync.client.PhoneContact;
import com.nbos.phonebook.sync.client.User;
import com.nbos.phonebook.sync.platform.ContactManager.UserIdQuery;

public class SyncManager {
	static String TAG = "SyncManager";
	Context context;
    String account; 
    List<PhoneContact> allContacts; 
    Cursor dataCursor, rawContactsCursor; //, contactsCursor;
    
	public SyncManager(Context context, String account) {
		super();
		this.context = context;
		this.account = account;
		
		this.allContacts = DatabaseHelper.getContacts(false, context);
		this.dataCursor = DatabaseHelper.getData(context);
		
		rawContactsCursor = DatabaseHelper.getRawContactsCursor(context.getContentResolver(), false);
		// getContactsCursor();
	}

	/*private void getContactsCursor() {
		if(contactsCursor != null)
			contactsCursor.close();
		// contactsCursor = context.getContentResolver().query(Constants.CONTACT_URI, null, null, null, null);
		// Log.i(TAG, "There are "+contactsCursor.getCount()+" phonebook contacts");
	}*/
	
	public void syncContacts(List<Contact> contacts) {
        long rawContactId = 0;
        final ContentResolver resolver = context.getContentResolver();
        final BatchOperation batchOperation = new BatchOperation(context, resolver);
        
        Log.i(TAG, "There are "+rawContactsCursor.getCount()+" raw contacts, num columns: "+rawContactsCursor.getColumnCount());
        Log.i(TAG, "There are "+allContacts.size()+" phone contacts");
        // Log.i(TAG, "There are "+contactsCursor.getCount()+" phonebook contacts");
        // syncSharedBooks(context);
        Log.d(TAG, "In SyncContacts");
        for (final Contact contact : contacts) {
            // userId = Integer.parseInt(user.getUserId());
            // Check to see if the contact needs to be inserted or updated
            rawContactId = lookupRawContact(contact);
            boolean dirty = ContactManager.isDirtyContact(rawContactId, rawContactsCursor); 
            Log.d(TAG, "Raw contact id is: "+rawContactId+", dirty: "+dirty);
            if(dirty) continue;
            if (rawContactId != 0) {
                if (!contact.deleted) {
                    // update contact
                    ContactManager.updateContact(context, resolver, account, contact,
                        rawContactId, batchOperation, dataCursor);
                } else {
                    // delete contact
                    ContactManager.deleteContact(context, rawContactId, batchOperation);
                }
            } else {
                // add new contact
                Log.d(TAG, "In addContact, user: "+contact.name);
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
        batchOperation.execute();
        refreshCursors();
        // updateServerIds(contacts);
	}

	private void refreshCursors() {
		Log.i(TAG, "refreshCursors");
        allContacts = DatabaseHelper.getContacts(false, context);
        dataCursor.requery();
        rawContactsCursor.requery();
	}

	public void syncGroups(List<Group> groups, boolean isSharedBook) {
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
	    	Log.i(TAG, "New group: "+account);
	    	// create a group with the share book name
	    	//DatabaseHelper.createAGroup(ctx, sharedBook.name, sharedBook.owner, accountName, id);
	    	DatabaseHelper.createAGroup(context, g.name, isSharedBook ? g.owner : null , account, Integer.parseInt(id));
	    	cursor.requery();
	    	Log.i(TAG, "cursor has "+cursor.getCount()+" rows");
	    }
	    cursor.moveToFirst();
	    String groupId = cursor.getString(cursor.getColumnIndex(ContactsContract.Groups._ID)),
	    	dirty = cursor.getString(cursor.getColumnIndex(ContactsContract.Groups.DIRTY));
	    
	    if(dirty.equals("1"))
	    {
	    	Log.i(TAG, "Group is dirty, skipping update");
	    	return;
	    }
	    Cursor groupCursor = DatabaseHelper.getContactsInGroup(groupId, context.getContentResolver());	    
    	Log.i(TAG, "Update group, id: "+groupId);
    	syncContacts(g.contacts);
    	rawContactsCursor.requery();
    	Set<String> contactIds = new HashSet<String>();
    	for(Contact u : g.contacts)
    		contactIds.add(updateGroupContact(u, groupId));
    	groupCursor.moveToFirst();
    	if(groupCursor.getCount() > 0)
    	do {
    		String contactId = groupCursor.getString(groupCursor.getColumnIndex(ContactsContract.Data.CONTACT_ID));
    		if(!contactIds.contains(contactId))
    			DatabaseHelper.deleteContactFromGroup(contactId, groupId, context);
    	} while(groupCursor.moveToNext());
    	
    	cursor.close();
	}

	private String updateGroupContact(Contact u, String groupId) {
		String contactId = getContactIdFromServerId(u.serverId),
			rawContactId = DatabaseHelper.getRawContactId(contactId, rawContactsCursor);
		Log.i(TAG, "ServerId: "+u.serverId+", contactId: "+contactId+", rawContactId: "+rawContactId);
		// if(contactId != null)
			DatabaseHelper.updateToGroup(groupId, contactId, rawContactId, context.getContentResolver());
		return contactId;
	}

	private String getContactIdFromServerId(String serverId) {
		if(dataCursor.getCount() == 0) return null;
		dataCursor.moveToFirst();
		do {
			String mimeType = dataCursor.getString(dataCursor.getColumnIndex(Data.MIMETYPE));
			String sId = dataCursor.getString(dataCursor.getColumnIndex(SampleSyncAdapterColumns.DATA_PID));
			if(!mimeType.equals(SampleSyncAdapterColumns.MIME_PROFILE)
			|| !sId.equals(serverId))
				continue;
			String contactId = dataCursor.getString(dataCursor.getColumnIndex(Data.CONTACT_ID));
			Log.i(TAG, "getContactIdFromServerId("+serverId+") = "+contactId);
			return contactId;
		} while(dataCursor.moveToNext());
		return null;
	}

	long lookupRawContact(Contact contact) {
		dataCursor.moveToFirst();
		if(dataCursor.getCount() > 0)
		do {
			String mimeType = dataCursor.getString(dataCursor.getColumnIndex(Data.MIMETYPE));
			String serverId = dataCursor.getString(dataCursor.getColumnIndex(SampleSyncAdapterColumns.DATA_PID));
			if(!mimeType.equals(SampleSyncAdapterColumns.MIME_PROFILE)
			|| !serverId.equals(contact.serverId))
				continue;
			long rawContactId = dataCursor.getLong(dataCursor.getColumnIndex(Data.RAW_CONTACT_ID));
			Log.i(TAG, "lookupRawContact returning rawContactId: "+rawContactId+", for serverId: "+serverId);
		} while(dataCursor.moveToNext()); 

		// could not find the contact, do a phone number search		
		for(PhoneContact u : allContacts) {
			if(u.number.equals(contact.number)) {// maybe a contact
				// check if the rest of the information is the same
				if(u.name.equals(contact.name))
				{
					Log.i(TAG, "Existing contact; ph: "+u.number+", name: "+u.name+", serverId: "+contact.serverId+", contactId: "+u.contactId+", phone serverId: "+u.serverId+", rawContactId: "+u.rawContactId);
					// update the serverId of the contact
					DatabaseHelper.updateContactServerId(u.contactId, contact.serverId, context, rawContactsCursor);
					return Long.parseLong(u.rawContactId);
				}
			}
		}
		return 0;
	}

	/*void updateServerIds(List<Contact> contacts) {
		for(Contact contact : contacts)
		{
			for(PhoneContact u : allContacts) {
				if(u.number.equals(contact.number)) {// maybe a contact
					// check if the rest of the information is the same
					if(u.name.equals(contact.name))
					{
						Log.i(TAG, "Existing contact; ph: "+u.number+", name: "+u.name+", serverId: "+contact.serverId+", contactId: "+u.contactId+", phone serverId: "+u.serverId+", rawContactId: "+u.rawContactId);
						// update the serverId of the contact
						DatabaseHelper.updateContactServerId(u.contactId, contact.serverId, context, rawContactsCursor);
					}
				}
			}
		}
	}*/
	
	public void close() {
    	if(dataCursor != null)
    		dataCursor.close();
    	if(rawContactsCursor != null)
    		rawContactsCursor.close();
	}
}
