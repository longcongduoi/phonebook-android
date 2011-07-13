package com.nbos.phonebook.sync.platform;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;

import com.nbos.phonebook.DatabaseHelper;
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
    Cursor dataCursor, rawContactsCursor;
    
	public SyncManager(Context context, String account, List<PhoneContact> allContacts,
			Cursor dataCursor) {
		super();
		this.context = context;
		this.account = account;
		this.allContacts = allContacts;
		this.dataCursor = dataCursor;
	}

	public void setAllContacts(List<PhoneContact> allContacts) {
		this.allContacts = allContacts;
	}
	public void setDataCursor(Cursor dataCursor) {
		this.dataCursor = dataCursor;
	}
	public void setRawContactsCursor(Cursor rawContactsCursor) {
		this.rawContactsCursor = rawContactsCursor;
	}

	public void syncContacts(List<Contact> contacts) {
        long rawContactId = 0;
        final ContentResolver resolver = context.getContentResolver();
        final BatchOperation batchOperation =
            new BatchOperation(context, resolver);
        
        final Cursor rawContactsCursor =
            resolver.query(RawContacts.CONTENT_URI, UserIdQuery.PROJECTION,
                null, null, null);
        Log.i(TAG, "There are "+rawContactsCursor.getCount()+" raw contacts, num columns: "+rawContactsCursor.getColumnCount());
        Log.i(TAG, "There are "+allContacts.size()+" contacts");
        // syncSharedBooks(context);
        Log.d(TAG, "In SyncContacts");
        for (final Contact contact : contacts) {
            // userId = Integer.parseInt(user.getUserId());
            // Check to see if the contact needs to be inserted or updated
            rawContactId = ContactManager.lookupRawContact(resolver, contact, rawContactsCursor, allContacts);
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
	}

	public void refreshCursors() {
        setAllContacts(DatabaseHelper.getContacts(false, context));
        // dataCursor = DatabaseHelper.getData(mContext);
        setDataCursor(DatabaseHelper.getData(context));
        // rawContactsCursor = DatabaseHelper.getRawContactsCursor(mContext.getContentResolver(), false);
        setRawContactsCursor(DatabaseHelper.getRawContactsCursor(context.getContentResolver(), false));
		
	}

	public void syncGroups(List<Group> groups) {
		for(Group g : groups)
			updateGroup(g);
	}

	private void updateGroup(Group g) {
	    String id = g.groupId;
	    ContentResolver cr = context.getContentResolver();
	    Cursor cursor = cr.query(ContactsContract.Groups.CONTENT_URI, null,  
	    		ContactsContract.Groups.SOURCE_ID + " = "+id, null, null);
	    if(cursor.getCount() == 0)
	    {
	    	Log.i(TAG, "New group: "+account);
	    	// create a group with the share book name
	    	DatabaseHelper.createAGroup(context, g.name, null, account, Integer.parseInt(id));
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
		String contactId = DatabaseHelper.getContactIdFromServerId(context.getContentResolver(), u.serverId, rawContactsCursor),
			rawContactId = DatabaseHelper.getRawContactId(contactId, rawContactsCursor);
		Log.i(TAG, "ServerId: "+u.serverId+", contactId: "+contactId+", rawContactId: "+rawContactId);
		// if(contactId != null)
			DatabaseHelper.updateToGroup(groupId, contactId, rawContactId, context.getContentResolver());
		return contactId;
	}

}
