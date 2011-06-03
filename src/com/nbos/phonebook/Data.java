package com.nbos.phonebook;

import com.nbos.phonebook.contentprovider.Provider;
import com.nbos.phonebook.database.tables.BookTable;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;

public class Data {
	static String tag = "DATA";
	public static Cursor getContacts(Activity activity) {
		return activity.managedQuery(ContactsContract.Contacts.CONTENT_URI, null, null, null,
				ContactsContract.Contacts._ID);
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
		    Data.setGroupDirty(groupId, cr);		    
	}
	
}
