package com.nbos.phonebook;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;

import com.nbos.phonebook.contentprovider.Provider;
import com.nbos.phonebook.database.tables.BookTable;
import com.nbos.phonebook.sync.platform.BatchOperation;

public class DatabaseHelper {
	static String tag = "DATA";
	public static Cursor getContacts(Activity activity) {
		return activity.managedQuery(ContactsContract.Contacts.CONTENT_URI, null, null, null,
				ContactsContract.Contacts._ID);
				// ContactsContract.Contacts.DISPLAY_NAME);
	}

	public static Cursor getContacts(Activity activity, String searchString) {
		return activity.managedQuery(ContactsContract.Contacts.CONTENT_URI, null, 
				ContactsContract.Data.DISPLAY_NAME+" like '" + searchString + "%'", null,
				ContactsContract.Contacts._ID);
				// ContactsContract.Contacts.DISPLAY_NAME);
	}

	public static String getContactIdFromSourceId(ContentResolver cr, int id) {
		Cursor cursor = cr.query(ContactsContract.RawContacts.CONTENT_URI, null, 
				ContactsContract.RawContacts.SOURCE_ID + " = " + id, null,
				null);
		String rawContactId = "0";
		Log.i(tag, "getContact: "+cursor.getCount()+" contacts for sourceId: "+id);
		while(cursor.moveToNext())
		{
			rawContactId = cursor.getString(cursor.getColumnIndex(ContactsContract.RawContacts.CONTACT_ID));
			Log.i(tag, "rawContactId: "+rawContactId);
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
	    Log.i(tag, "Updated "+num+" groups to dirty");

	}

	public static void addToGroup(String groupId, String contactId, ContentResolver cr) {
		   // this.removeFromGroup(personId, groupId);
			Log.i(tag, "Added contact to group: "+groupId);
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
			Log.i(tag, "updating contact to group: "+groupId);
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
	    			ContactsContract.Contacts.DISPLAY_NAME
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
	    Log.i(tag, "isContactInGroup() groupId: "+groupId+", contactId: "+contactId+", num results: "+c.getCount());
	    return c.getCount() > 0;
	}

	public static final String ACCOUNT_TYPE = "com.nbos.phonebook"; 
	public static String getAccountName(Context ctx) {
        Account[] accounts = AccountManager.get(ctx).getAccounts();
        Log.i(tag, "There are "+accounts.length+" accounts");
        for (Account account : accounts) 
        {
        	Log.i(tag, "account name: "+account.name+", type: "+account.type);
        	if(account.type.equals(ACCOUNT_TYPE))
        		return account.name;
        }
        return null;
	}

    public static void createAGroup(Context context, String groupName, String accountName, int id) {
        final ContentResolver resolver = context.getContentResolver();
        final BatchOperation batchOperation =
            new BatchOperation(context, resolver);
    	
		Log.i(tag, "Creating group: "+groupName);
		Uri mEntityUri = ContactsContract.Groups.CONTENT_URI.buildUpon()
			.appendQueryParameter(ContactsContract.Groups.ACCOUNT_NAME, accountName)
			.appendQueryParameter(ContactsContract.Groups.ACCOUNT_TYPE, ACCOUNT_TYPE)
			.appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
			.build();
		
	
		ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(mEntityUri);
		Log.v("Group", "create accountgroup: "+ACCOUNT_TYPE+", "+accountName);
		builder.withValue(ContactsContract.Groups.ACCOUNT_TYPE, ACCOUNT_TYPE);
		builder.withValue(ContactsContract.Groups.ACCOUNT_NAME, accountName);
		builder.withValue(ContactsContract.Groups.SYSTEM_ID, accountName);
		builder.withValue(ContactsContract.Groups.TITLE, groupName);
		builder.withValue(ContactsContract.Groups.SOURCE_ID, id);
		builder.withValue(ContactsContract.Groups.GROUP_VISIBLE, 1);
		batchOperation.add(builder.build());
		batchOperation.execute();
	}
	
}
