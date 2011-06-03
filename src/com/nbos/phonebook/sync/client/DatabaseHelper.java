package com.nbos.phonebook.sync.client;

import android.app.Activity;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;

import com.nbos.phonebook.sync.Constants;
import com.nbos.phonebook.sync.platform.BatchOperation;
import com.nbos.phonebook.database.tables.BookTable;

public class DatabaseHelper {
	static String tag = "DATA";
	public static Cursor getContacts(Activity activity) {
		return activity.managedQuery(ContactsContract.Contacts.CONTENT_URI, null, null, null,
				ContactsContract.Contacts._ID);
				// ContactsContract.Contacts.DISPLAY_NAME);
	}

	public static Cursor getBook(Activity activity, String id) {
    	return activity.getContentResolver().query(
    			Uri.parse(Constants.SHARE_BOOK_PROVIDER),
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

    public static void createAGroup(Context context, String groupName, String accountName) {
        final ContentResolver resolver = context.getContentResolver();
        final BatchOperation batchOperation =
            new BatchOperation(context, resolver);
    	
		String mAccountName = accountName,
		mAccountType = "com.example.android.samplesync";
		Log.i(tag, "Creating group: "+groupName);
		Uri mEntityUri = ContactsContract.Groups.CONTENT_URI.buildUpon()
			.appendQueryParameter(ContactsContract.Groups.ACCOUNT_NAME, mAccountName)
			.appendQueryParameter(ContactsContract.Groups.ACCOUNT_TYPE, mAccountType)
			.appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
			.build();
	
		ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(mEntityUri);
		Log.v("Group", "create accountgroup: "+mAccountType+", "+mAccountName);
		builder.withValue(ContactsContract.Groups.ACCOUNT_TYPE, mAccountType);
		builder.withValue(ContactsContract.Groups.ACCOUNT_NAME, mAccountName);
		builder.withValue(ContactsContract.Groups.SYSTEM_ID, mAccountName);
		builder.withValue(ContactsContract.Groups.TITLE, groupName);
		builder.withValue(ContactsContract.Groups.GROUP_VISIBLE, 1);
		batchOperation.add(builder.build());
		batchOperation.execute();
	}    

}
