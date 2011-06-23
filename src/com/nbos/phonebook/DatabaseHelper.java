package com.nbos.phonebook;

import java.util.ArrayList;
import java.util.List;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.CursorJoiner;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Data;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.nbos.phonebook.contentprovider.Provider;
import com.nbos.phonebook.database.IntCursorJoiner;
import com.nbos.phonebook.database.tables.BookTable;
import com.nbos.phonebook.sync.Constants;
import com.nbos.phonebook.sync.client.Contact;
import com.nbos.phonebook.sync.client.Group;
import com.nbos.phonebook.sync.client.SharingBook;
import com.nbos.phonebook.sync.client.User;
import com.nbos.phonebook.sync.platform.BatchOperation;

public class DatabaseHelper {
	static String TAG = "DATA";
	public static Cursor getContacts(Activity activity) {
		return activity.managedQuery(ContactsContract.Contacts.CONTENT_URI, null, null, null,
				ContactsContract.Contacts._ID);
				// ContactsContract.Contacts.DISPLAY_NAME);
	}

	public static Cursor getGroups(ContentResolver cr) {
	    return cr.query(ContactsContract.Groups.CONTENT_SUMMARY_URI, null,
	    		ContactsContract.Groups.DELETED + " = 0 ", null, null);
	}

	public static Cursor getContacts(Activity activity, String searchString) {
		return activity.managedQuery(ContactsContract.Contacts.CONTENT_URI, null, 
				ContactsContract.Data.DISPLAY_NAME+" like '" + searchString + "%'", null,
				ContactsContract.Contacts._ID);
				// ContactsContract.Contacts.DISPLAY_NAME);
	}

	public static String getContactIdFromSourceId(ContentResolver cr, String id) {
		Cursor cursor = cr.query(ContactsContract.RawContacts.CONTENT_URI, null, 
				Constants.CONTACT_SERVER_ID + " = " + id, 
				null, null);
		String rawContactId = "0";
		Log.i(TAG, "getContact: "+cursor.getCount()+" contacts for sourceId: "+id);
		while(cursor.moveToNext())
		{
			rawContactId = cursor.getString(cursor.getColumnIndex(ContactsContract.RawContacts.CONTACT_ID));
			Log.i(TAG, "rawContactId: "+rawContactId);
			if(rawContactId != null)
				return rawContactId;
		}
		return rawContactId;
				// ContactsContract.Contacts.DISPLAY_NAME);
	}

	public static Cursor getBook(Activity activity, String id) {
    	return activity.getContentResolver().query(
    			Uri.parse("content://"+Provider.AUTHORITY+"/"+Provider.BookContent.CONTENT_PATH),
	    		null,
	    		BookTable.BOOKID + "=" +id,
	    	    null, BookTable.CONTACTID);
	}
	
	public static void setGroupDirty(String groupId, ContentResolver cr) {
	    ContentValues values = new ContentValues();
	    values.put(ContactsContract.Groups.DIRTY, "1");

	    int num = cr.update(
	    		ContactsContract.Groups.CONTENT_URI, values,
	    		ContactsContract.Groups._ID + " = " + groupId, null);
	    Log.i(TAG, "Updated "+num+" groups to dirty");

	}

	public static void addToGroup(String groupId, String contactId, ContentResolver cr) {
		   // this.removeFromGroup(personId, groupId);
			Log.i(TAG, "Added contact to group: "+groupId);
		    ContentValues values = new ContentValues();
		    values.put(ContactsContract.CommonDataKinds.GroupMembership.RAW_CONTACT_ID,
		            contactId);
		    values.put(
		            ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID,
		            groupId);
		    values
		            .put(
		                    ContactsContract.CommonDataKinds.GroupMembership.MIMETYPE,
		                    ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE);

		    cr.insert(
		            ContactsContract.Data.CONTENT_URI, values);
		    DatabaseHelper.setGroupDirty(groupId, cr);		    
	}

	public static void updateToGroup(String groupId, String contactId, ContentResolver cr) {
		   // this.removeFromGroup(personId, groupId);
			Log.i(TAG, "updating contact to group: "+groupId);
			if(DatabaseHelper.isContactInGroup(groupId, contactId, cr)) return;
		    ContentValues values = new ContentValues();
		    values.put(ContactsContract.CommonDataKinds.GroupMembership.RAW_CONTACT_ID,
		            contactId);
		    values.put(
		            ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID,
		            groupId);
		    values
		            .put(
		                    ContactsContract.CommonDataKinds.GroupMembership.MIMETYPE,
		                    ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE);

		    cr.insert(
		            ContactsContract.Data.CONTENT_URI, values);
		    // DatabaseHelper.setGroupDirty(groupId, cr);		    
	}

	public static Cursor getContactsInGroup(String groupId,
			ContentResolver cr) {
	    return cr.query(ContactsContract.Data.CONTENT_URI,
	    		// null,
	    	    new String[] {
	    			ContactsContract.Contacts._ID, 
	    			ContactsContract.Data.RAW_CONTACT_ID, 
	    			ContactsContract.RawContacts._ID,
	    			ContactsContract.Contacts.DISPLAY_NAME,
	    			ContactsContract.CommonDataKinds.Photo.PHOTO
	    		},
	    	    ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID+" = "+groupId,
	    	    null, ContactsContract.Data.RAW_CONTACT_ID);
	}
	
	public static boolean isContactInGroup(String groupId, String contactId,
			ContentResolver cr) {
		
	    Cursor c = cr.query(ContactsContract.Data.CONTENT_URI,
	    		// null,
	    	    new String[] {
	    			ContactsContract.Contacts._ID, 
	    			ContactsContract.Data.RAW_CONTACT_ID, 
	    			ContactsContract.RawContacts._ID,
	    			ContactsContract.Contacts.DISPLAY_NAME
	    		},
	    	    ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID+" = "+groupId
	    	    +" and "+ContactsContract.Data.RAW_CONTACT_ID + " = "+contactId,
	    	    null, ContactsContract.Data.RAW_CONTACT_ID);
	    Log.i(TAG, "isContactInGroup() groupId: "+groupId+", contactId: "+contactId+", num results: "+c.getCount());
	    return c.getCount() > 0;
	}

	public static String getAccountName(Context ctx) {
        Account[] accounts = AccountManager.get(ctx).getAccounts();
        Log.i(TAG, "There are "+accounts.length+" accounts");
        for (Account account : accounts) 
        {
        	Log.i(TAG, "account name: "+account.name+", type: "+account.type);
        	if(account.type.equals(Constants.ACCOUNT_TYPE))
        		return account.name;
        }
        return null;
	}

	public static Account getAccount(Context ctx, String name) {
        Account[] accounts = AccountManager.get(ctx).getAccounts();
        Log.i(TAG, "There are "+accounts.length+" accounts");
        for (Account account : accounts) 
        {
        	Log.i(TAG, "account name: "+account.name+", type: "+account.type);
        	if(account.type.equals(Constants.ACCOUNT_TYPE) && account.name.equals(name))
        		return account;
        }
        return null;
	}

    public static void createAGroup(Context context, String groupName, String owner, String accountName, int id) {
    	// if(owner == null) owner = accountName;
        final ContentResolver resolver = context.getContentResolver();
        final BatchOperation batchOperation =
            new BatchOperation(context, resolver);
    	
		Log.i(TAG, "Creating group: "+groupName);
		Uri mEntityUri = ContactsContract.Groups.CONTENT_URI.buildUpon()
			.appendQueryParameter(ContactsContract.Groups.ACCOUNT_NAME, accountName)
			.appendQueryParameter(ContactsContract.Groups.ACCOUNT_TYPE, Constants.ACCOUNT_TYPE)
			.appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
			.build();
		
	
		ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(mEntityUri);
		Log.v("Group", "create accountgroup: "+Constants.ACCOUNT_TYPE+", "+accountName);
		builder.withValue(ContactsContract.Groups.ACCOUNT_TYPE, Constants.ACCOUNT_TYPE);
		builder.withValue(ContactsContract.Groups.ACCOUNT_NAME, accountName);
		builder.withValue(ContactsContract.Groups.SYSTEM_ID, accountName);
		builder.withValue(ContactsContract.Groups.TITLE, groupName);
		builder.withValue(ContactsContract.Groups.SOURCE_ID, id);
		builder.withValue(ContactsContract.Groups.SYNC1, owner); // using sync1 for the owner of the shared book
		builder.withValue(ContactsContract.Groups.GROUP_VISIBLE, 1);
		batchOperation.add(builder.build());
		batchOperation.execute();
	}
    
	public static List<User> getContacts(boolean newOnly, Context ctx) {
	    ContentResolver cr = ctx.getContentResolver();
	    
        final String[] CONTACTS_PROJECTION =
            new String[] {
        		ContactsContract.Contacts._ID, 
        		ContactsContract.Contacts.DISPLAY_NAME
        	};
        final String[] PHONES_PROJECTION =
                new String[] {
            		ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            		ContactsContract.CommonDataKinds.Phone.NUMBER
            	};


	    Cursor rawContactsCursor = getRawContactsCursor(cr, newOnly),
	    	contactsCursor = cr.query(ContactsContract.Contacts.CONTENT_URI, 
	    			CONTACTS_PROJECTION, null, null, null),
	    	phonesCursor = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, 
	    			PHONES_PROJECTION, null, null, null);
	    
	    Log.i(TAG, "There are "+rawContactsCursor.getCount()+" contacts ");
	    List<User> users = new ArrayList<User>();
	    if(rawContactsCursor.getCount() == 0) return users;
	    rawContactsCursor.moveToFirst();
	    do {
	        String contactId =
	            	rawContactsCursor.getString(rawContactsCursor.getColumnIndex(ContactsContract.RawContacts.CONTACT_ID));
	        String sourceId = rawContactsCursor.getString(rawContactsCursor.getColumnIndex(Constants.CONTACT_SERVER_ID));
	        String sync1 = rawContactsCursor.getString(rawContactsCursor.getColumnIndex(ContactsContract.RawContacts.SYNC1));
	        String dirty = rawContactsCursor.getString(rawContactsCursor.getColumnIndex(ContactsContract.RawContacts.DIRTY));
	        // String version = rawContactsCursor.getString(rawContactsCursor.getColumnIndex(ContactsContract.RawContacts.VERSION));
	        String name = getContactName(contactsCursor, contactId); 
	        String phoneNumber = getContactNumber(phonesCursor, contactId); 
	        Log.i(TAG, "id: "+contactId+", name is: "+name+", number is: "+phoneNumber+", dirty: "+dirty+", sync1: "+sync1);
	        if(name == null || phoneNumber == null) continue;
	        users.add(new User(name, phoneNumber, sourceId, contactId));
	    } while(rawContactsCursor.moveToNext());
	    contactsCursor.close();
	    phonesCursor.close();
	    return users;
	}
	
	public static Cursor getRawContactsCursor(ContentResolver cr, boolean newOnly) {
	    String where = newOnly ? ContactsContract.RawContacts.DIRTY + " = 1" : null;
        final String[] PROJECTION =
            new String[] {
        		ContactsContract.RawContacts.CONTACT_ID, 
        		Constants.CONTACT_SERVER_ID, 
        		ContactsContract.RawContacts.DIRTY
        	};
	    
		return cr.query(ContactsContract.RawContacts.CONTENT_URI, PROJECTION, where, null, ContactsContract.RawContacts._ID);	
	}

	private static String getContactNumber(Cursor phonesCursor, String contactId) {
		phonesCursor.moveToFirst();
		do {
			String cId = phonesCursor.getString(phonesCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID));
			if(cId.equals(contactId))
				return phonesCursor.getString(phonesCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
		} while(phonesCursor.moveToNext());
		return null;
	}

	private static String getContactName(Cursor contactsCursor, String contactId) {
		contactsCursor.moveToFirst();

		do {
			String cId = contactsCursor.getString(contactsCursor.getColumnIndex(ContactsContract.Contacts._ID));
			if(cId.equals(contactId))
				return contactsCursor.getString(contactsCursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
		} while(contactsCursor.moveToNext());
		return null;
	}

	public static List<Group> getGroups(boolean newOnly, Context ctx) {
		List<Group> groups = new ArrayList<Group>();
	    ContentResolver cr = ctx.getContentResolver();
	    String where = ContactsContract.Groups.DELETED + " = 0 ";
	    if(newOnly)
	    	where += " and " + ContactsContract.Groups.DIRTY + " = 1 ";
	    Cursor groupsCursor = cr.query(ContactsContract.Groups.CONTENT_SUMMARY_URI, 
	    		new String [] {
	    			ContactsContract.Groups.TITLE,
	    			ContactsContract.Groups._ID,
	    			ContactsContract.Groups.SOURCE_ID,
	    			ContactsContract.Groups.DIRTY
	    		},
	    		where, null, null);
	    Log.i(TAG, "There are "+groupsCursor.getCount()+" groups");
	    Cursor contactsCursor = cr.query(ContactsContract.Contacts.CONTENT_URI,
	    		// null,
	    	    new String[] {
	    			ContactsContract.Contacts._ID,
	    			ContactsContract.Contacts.DISPLAY_NAME
	    		},
	    		null, null, ContactsContract.Contacts._ID),
    	rawContactsCursor = getRawContactsCursor(cr, false);

	    Cursor phonesCursor = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, 
        		new String[] {
	    			ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
	    			ContactsContract.CommonDataKinds.Phone.NUMBER
	    		}, 		
        		null,
        		null, null);

	    
	    Log.i(TAG, "There are "+contactsCursor.getCount()+" contacts");
	    
	    while(groupsCursor.moveToNext())
	    {
	    	List<Contact> contacts = new ArrayList<Contact>();
	    	String name = groupsCursor.getString(groupsCursor.getColumnIndex(ContactsContract.Groups.TITLE));
	    	String groupId = groupsCursor.getString(groupsCursor.getColumnIndex(ContactsContract.Groups._ID));
	    	String groupSourceId = groupsCursor.getString(groupsCursor.getColumnIndex(ContactsContract.Groups.SOURCE_ID));
	    	String dirty = groupsCursor.getString(groupsCursor.getColumnIndex(ContactsContract.Groups.DIRTY));
		    Cursor dataCursor = getContactsInGroup(new Integer(groupId).toString(), cr);
		    Log.i(TAG, "There are "+dataCursor.getCount()+" contacts in group: "+groupId);
	    	
		    IntCursorJoiner joiner = new IntCursorJoiner(
		    		contactsCursor,
		    		new String[]
		    		{ContactsContract.Contacts._ID},
		    		dataCursor,
		    		new String[] {ContactsContract.Data.RAW_CONTACT_ID}
		    );
	        for (CursorJoiner.Result joinerResult : joiner) 
	        {
	        	switch (joinerResult) {
	        		case BOTH: // handle case where a row with the same key is in both cursors
	        			String contactId = contactsCursor.getString(contactsCursor.getColumnIndex(ContactsContract.Contacts._ID)),
	        				serverId = getContactServerId(contactId, rawContactsCursor), 
	        				contactName = contactsCursor.getString(contactsCursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)),
	        				contactNumber = getContactPhoneNumber(contactId, phonesCursor);
	        			if(contactNumber == null) break;
	        			contacts.add(new Contact(contactId, serverId, contactNumber, contactName));
	        			Log.i(TAG, "added contact: "+contactId+", serverId: "+serverId+", "+contactNumber+", "+contactName);
	        		break;
	        	}
	        }	    
	        groups.add(new Group(groupId, groupSourceId, name, null, contacts));
	        Log.i(TAG, "dirty is "+dirty);
	        Log.i(TAG, "Added group["+groupId+"] "+name+" with "+contacts.size()+" contacts");
	        dataCursor.close();
	    	// books
	    }
	    groupsCursor.close();
	    contactsCursor.close();
	    phonesCursor.close();
	    return groups;
	}
	
	private static String getContactPhoneNumber(String contactId, Cursor phonesCursor) {
		phonesCursor.moveToFirst();
		do {
			String cId = phonesCursor.getString(phonesCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID));
			if(!cId.equals(contactId)) continue;
			return phonesCursor.getString(phonesCursor
                    .getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));
		} while(phonesCursor.moveToNext());
		return null;
	}

	private static String getContactServerId(String contactId, Cursor rawContactsCursor) {
		rawContactsCursor.moveToFirst();
		do {
			String cId = rawContactsCursor.getString(rawContactsCursor.getColumnIndex(ContactsContract.RawContacts.CONTACT_ID));
			if(cId.equals(contactId))
				return rawContactsCursor.getString(rawContactsCursor.getColumnIndex(Constants.CONTACT_SERVER_ID));
			
		} while(rawContactsCursor.moveToNext());
		return null;
	}

	public static List<SharingBook> getSharingBooks(boolean newOnly, Context ctx) {
    	List<SharingBook> books = new ArrayList<SharingBook>();
    	String where = newOnly ? BookTable.DIRTY + " is null" : null;
    	ContentResolver cr = ctx.getContentResolver();
    	Cursor cursor = cr.query(
    			Uri.parse(Constants.SHARE_BOOK_PROVIDER),
    			null, where, null, null);
    	Cursor contactsCursor = getRawContactsCursor(cr, false);
    	Cursor groupsCursor = getGroups(cr);
    	if(cursor != null)
    		Log.i(TAG, "There are "+cursor.getCount()+" contacts sharing books");
    	while(cursor.moveToNext())
    	{
    		String groupSourceId = getSourceIdFromGroupId(groupsCursor, 
    				cursor.getString(cursor.getColumnIndex(BookTable.BOOKID))),
    			contactSourceId = getSourceIdFromContactId(contactsCursor, 
    				cursor.getString(cursor.getColumnIndex(BookTable.CONTACTID)));
    		Log.i(TAG, "groupSourceId: "+groupSourceId+", contactSourceId: "+contactSourceId);
    		if(groupSourceId != null && contactSourceId != null)
    			books.add(new SharingBook(groupSourceId, contactSourceId));
    	}
    	contactsCursor.close();
    	groupsCursor.close();
    	return books;
    }
	
	private static String getSourceIdFromGroupId(Cursor groupsCursor, String groupId) {
		if(groupsCursor.getCount() == 0) return null;
		groupsCursor.moveToFirst();
		do {
			String gId = groupsCursor.getString(groupsCursor.getColumnIndex(ContactsContract.Groups._ID)),
				sourceId = groupsCursor.getString(groupsCursor.getColumnIndex(ContactsContract.Groups.SOURCE_ID));
			if(gId.equals(groupId))
				return sourceId;
		}
		while(groupsCursor.moveToNext());
		return null;
	}

	public static String getSourceIdFromContactId(Cursor contactsCursor, String contactId) {
		contactsCursor.moveToFirst();
		do {
			String cId = contactsCursor.getString(contactsCursor.getColumnIndex(ContactsContract.RawContacts.CONTACT_ID)),
				sourceId = contactsCursor.getString(contactsCursor.getColumnIndex(Constants.CONTACT_SERVER_ID));
			if(cId.equals(contactId))
				return sourceId;
		}
		while(contactsCursor.moveToNext());
		return null;
	}

	public static String getPhoneNumber(Context ctx) {
		String ph = ((TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE)).getLine1Number();
		Log.i(TAG, "Phone number is: "+ph);
		return ph;
    }

	public static void updateContactServerId(String contactId, String serverId, ContentResolver cr) {
	    ContentValues values = new ContentValues();
	    values.put(Constants.CONTACT_SERVER_ID, serverId);

	    int num = cr.update(
	    		ContactsContract.RawContacts.CONTENT_URI, values,
	    		ContactsContract.RawContacts.CONTACT_ID + " = " + contactId, null);
	    Log.i(TAG, "Updated "+num+" contacts to serverId: "+serverId);
	}

    public static Cursor getData(Context ctx) {
        final String[] PROJECTION =
            new String[] {Data._ID, Data.MIMETYPE, Data.DATA1, Data.DATA2,
                Data.DATA3, Data.RAW_CONTACT_ID};

    	return ctx.getContentResolver().query(Data.CONTENT_URI, PROJECTION, null, null, null);
    }

	
}
