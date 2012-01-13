package com.nbos.phonebook;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Groups;
import android.provider.ContactsContract.RawContacts;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.nbos.phonebook.database.tables.BookTable;
import com.nbos.phonebook.database.tables.ContactTable;
import com.nbos.phonebook.sync.Constants;
import com.nbos.phonebook.sync.client.Contact;
import com.nbos.phonebook.sync.client.ContactPicture;
import com.nbos.phonebook.sync.client.Group;
import com.nbos.phonebook.sync.client.PhoneContact;
import com.nbos.phonebook.sync.client.ServerData;
import com.nbos.phonebook.sync.client.SharingBook;
import com.nbos.phonebook.sync.platform.BatchOperation;
import com.nbos.phonebook.sync.platform.ContactOperations;
import com.nbos.phonebook.sync.platform.PhonebookSyncAdapterColumns;
import com.nbos.phonebook.sync.platform.SyncManager;
import com.nbos.phonebook.util.ImageInfo;

public class Db {
	ContentResolver cr;
	Context context;

	public Db(Context context) {
		this.context = context;
		this.cr = context.getContentResolver();
	}

	static String tag = "DATA";
	public static Cursor getContacts(Activity activity) {
		return activity.managedQuery(Contacts.CONTENT_URI, null, null, null,
				Contacts._ID);
	}
	
	public Cursor getGroups() {
	    return cr.query(Groups.CONTENT_SUMMARY_URI, null,
	    		Groups.DELETED + " = 0 ", null, null);
	}

	public static Cursor getContacts(Activity activity, String searchString) {
		return activity.managedQuery(Contacts.CONTENT_URI, null, 
				Data.DISPLAY_NAME+" like '" + searchString + "%'", null,
				Contacts._ID);
	}

	public Cursor getFullBook(String id) {
    	return cr.query(
    			Constants.SHARE_BOOK_URI,
	    		null,
	    		BookTable.BOOKID + "=" +id,
	    	    null, BookTable.CONTACTID);
	}

	public Cursor getBook(String id) {
    	return cr.query(
    			Constants.SHARE_BOOK_URI,
	    		null,
	    		BookTable.BOOKID + "=" +id
	    		+" and "+BookTable.DELETED + " = 0 ",
	    	    null, BookTable.CONTACTID);
	}

	public static Cursor getBooks(ContentResolver cr) {
    	return cr.query(
    			Constants.SHARE_BOOK_URI,
	    		null, BookTable.DELETED + " = 0 ", null, BookTable.BOOKID);
	}

	public static void setGroupDirty(String groupId, ContentResolver cr) {
	    ContentValues values = new ContentValues();
	    values.put(Groups.DIRTY, "1");

	    int num = cr.update(
	    		Groups.CONTENT_URI, values,
	    		Groups._ID + " = " + groupId, null);
	    Log.i(tag, "Updated "+num+" groups to dirty");

	}

	public static void addToGroup(String groupId, String rawContactId, ContentResolver cr) {
		   // this.removeFromGroup(personId, groupId);
			Log.i(tag, "Added contact to group: "+groupId+", contactId: "+rawContactId);
		    ContentValues values = new ContentValues();
		    values.put(GroupMembership.RAW_CONTACT_ID, rawContactId);
		    values.put(GroupMembership.GROUP_ROW_ID, groupId);
		    values.put(GroupMembership.MIMETYPE, GroupMembership.CONTENT_ITEM_TYPE);
		    cr.insert(Data.CONTENT_URI, values);
		    Db.setGroupDirty(groupId, cr);		    
	}

	public static Cursor getContactsInGroup(String groupId, ContentResolver cr) {
	    return cr.query(Data.CONTENT_URI,	
	    		// null,
	    	    new String[] {
	    			Data.CONTACT_ID,
	    			Data.RAW_CONTACT_ID
	    		},
	    	    GroupMembership.GROUP_ROW_ID+" =  ? "
	    	    +"and "+Data.MIMETYPE+" = ? ",
	    	    new String[]{groupId, GroupMembership.CONTENT_ITEM_TYPE}, Data.CONTACT_ID);
	}
	
	public static String getAccountName(Context ctx) {
        Account[] accounts = AccountManager.get(ctx).getAccounts();
        Log.i(tag, "There are "+accounts.length+" accounts");
        for (Account account : accounts) 
        {
        	Log.i(tag, "account name: "+account.name+", type: "+account.type);
        	if(account.type.equals(Constants.ACCOUNT_TYPE))
        		return account.name;
        }
        return null;
	}

	public static Account getAccount(Context ctx, String name) {
        Account[] accounts = AccountManager.get(ctx).getAccounts();
        Log.i(tag, "There are "+accounts.length+" accounts");
        for (Account account : accounts) 
        {
        	Log.i(tag, "account name: "+account.name+", type: "+account.type);
        	if(account.type.equals(Constants.ACCOUNT_TYPE) && account.name.equals(name))
        		return account;
        }
        return null;
	}

    public static void createAGroup(Context context, String groupName, String owner, Integer permission, String accountName, int id) {
    	// if(owner == null) owner = accountName;
        final BatchOperation batchOperation = new BatchOperation(context);
    	
		Log.i(tag, "Creating group: "+groupName+", account: "+accountName+", owner: "+owner);
		Uri mEntityUri = Groups.CONTENT_URI.buildUpon()
			.appendQueryParameter(Groups.ACCOUNT_NAME, accountName)
			.appendQueryParameter(Groups.ACCOUNT_TYPE, Constants.ACCOUNT_TYPE)
			.appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
			.build();
		
	
		ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(mEntityUri);
		Log.v("Group", "create accountgroup: "+Constants.ACCOUNT_TYPE+", "+accountName);
		builder.withValue(Groups.ACCOUNT_TYPE, Constants.ACCOUNT_TYPE);
		builder.withValue(Groups.ACCOUNT_NAME, accountName);
		builder.withValue(Groups.SYSTEM_ID, accountName);
		builder.withValue(Groups.TITLE, groupName);
		builder.withValue(Groups.SOURCE_ID, id);
		builder.withValue(Groups.SYNC1, owner); // using sync1 for the owner of the shared book
		builder.withValue(Groups.SYNC2, permission); // using sync2 for the owner of the shared book
		builder.withValue(Groups.GROUP_VISIBLE, 1);
		batchOperation.add(builder.build());
		batchOperation.execute();
	}
    
	public Cursor getRawContactsCursor(boolean newOnly) {
	    String where = newOnly ? RawContacts.DIRTY + " = 1" : null;
        final String[] PROJECTION =
            new String[] {
        		RawContacts._ID,
        		RawContacts.CONTACT_ID,
        		RawContacts.DIRTY,
        		RawContacts.ACCOUNT_NAME,
        		RawContacts.ACCOUNT_TYPE,
        		RawContacts.DELETED
        };
		return cr.query(RawContacts.CONTENT_URI, PROJECTION, where, null, RawContacts.CONTACT_ID);	
	}

	public List<Group> getGroups(boolean newOnly, Set<String> syncedGroupServerIds, Map<String, String> contactServerIdsMap) {
		List<Group> groups = new ArrayList<Group>();
	    String where = Groups.DELETED + " = 0 ";
	    if(newOnly)
	    	where = Groups.DIRTY + " = 1 ";
	    Cursor groupsCursor = cr.query(Groups.CONTENT_SUMMARY_URI, 
	    		new String [] {
	    			Groups.TITLE,
	    			Groups._ID,
	    			Groups.SOURCE_ID,
	    			Groups.ACCOUNT_NAME,
	    			Groups.ACCOUNT_TYPE,
	    			Groups.DIRTY,
	    			Groups.DELETED
	    		},
	    		where, null, null);
	    Log.i(tag, "There are "+groupsCursor.getCount()+" groups");

	    while(groupsCursor.moveToNext())
	    {
	    	List<Contact> contacts = new ArrayList<Contact>();
	    	String name = groupsCursor.getString(groupsCursor.getColumnIndex(Groups.TITLE));
	    	String groupId = groupsCursor.getString(groupsCursor.getColumnIndex(Groups._ID));
	    	String groupSourceId = groupsCursor.getString(groupsCursor.getColumnIndex(Groups.SOURCE_ID));
	    	if(groupSourceId != null && syncedGroupServerIds.contains(groupSourceId))
	    	{
	    		Log.i(tag, "Already synced group: "+name);
	    		continue;
	    	}
	    	String dirty = groupsCursor.getString(groupsCursor.getColumnIndex(Groups.DIRTY));
	    	String accName = groupsCursor.getString(groupsCursor.getColumnIndex(Groups.ACCOUNT_NAME));
	    	String accType = groupsCursor.getString(groupsCursor.getColumnIndex(Groups.ACCOUNT_TYPE));
	    	// String owner = groupsCursor.getString(groupsCursor.getColumnIndex(Groups.SYNC1));
	    	Log.i(tag, "Group: "+name+", account: "+accName+", account type: "+accType+", sourceId: "+groupSourceId);//+", owner: "+owner);
		    Cursor groupCursor = getContactsInGroup(new Long(groupId).toString(), cr);
		    Log.i(tag, "There are "+groupCursor.getCount()+" contacts in group: "+groupId);
	    	groupCursor.moveToFirst();
	    	if(groupCursor.getCount() > 0)
	    	do {
	    		String rawContactId =  groupCursor.getString(groupCursor.getColumnIndex(Data.RAW_CONTACT_ID)),
	    			serverId = contactServerIdsMap.get(rawContactId);
    			if(serverId != null)
    			{
    				contacts.add(new Contact(serverId));
    				Log.i(tag, "added contact: "+rawContactId+", serverId: "+serverId);//+", "+contactNumber+", "+contactName);
    			}
	    	} while(groupCursor.moveToNext());
	        groups.add(new Group(groupId, groupSourceId, name, null, contacts, null));
	        Log.i(tag, "dirty is "+dirty);
	        Log.i(tag, "Added group["+groupId+"] "+name+" with "+contacts.size()+" contacts");
	        groupCursor.close();
	    }
	    groupsCursor.close();
	    return groups;
	}
	
	public List<SharingBook> getSharingBooks(boolean newOnly, Map<String, String> contactServerIdsMap) {
    	List<SharingBook> books = new ArrayList<SharingBook>();
    	String where = newOnly ? BookTable.DIRTY + " is null" : null;
    	Cursor cursor = cr.query(
    			Constants.SHARE_BOOK_URI, null, where, null, null),
    		groupsCursor = getGroups();
    	if(cursor != null)
    		Log.i(tag, "There are "+cursor.getCount()+" contacts sharing books");
    	while(cursor.moveToNext())
    	{
    		String groupSourceId = getSourceIdFromGroupId(groupsCursor, 
    				cursor.getString(cursor.getColumnIndex(BookTable.BOOKID))),
    			contactServerId = contactServerIdsMap.get(cursor.getString(cursor.getColumnIndex(BookTable.CONTACTID))), //ServerIdFromRawContactId(dataCursor, 
    				// cursor.getString(cursor.getColumnIndex(BookTable.CONTACTID))),
    			deleted = cursor.getString(cursor.getColumnIndex(BookTable.DELETED));
    		Log.i(tag, "groupSourceId: "+groupSourceId+", contactSourceId: "+contactServerId);
    		if(groupSourceId != null && contactServerId != null)
    			books.add(new SharingBook(groupSourceId, contactServerId, deleted.equals("1")));
    	}
    	cursor.close();
    	groupsCursor.close();
    	return books;
    }
	
	private static String getSourceIdFromGroupId(Cursor groupsCursor, String groupId) {
		if(groupsCursor.getCount() == 0) return null;
		groupsCursor.moveToFirst();
		do {
			String gId = groupsCursor.getString(groupsCursor.getColumnIndex(Groups._ID)),
				sourceId = groupsCursor.getString(groupsCursor.getColumnIndex(Groups.SOURCE_ID));
			if(gId.equals(groupId))
				return sourceId;
		}
		while(groupsCursor.moveToNext());
		return null;
	}


	public static String getPhoneNumber(Context ctx) {
		String ph = ((TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE)).getLine1Number();
		Log.i(tag, "Phone number is: "+ph);
		return ph;
    }

	public static List<String> getRawContactIds(String contactId, Cursor rawContactsCursor) {
		Log.i(tag, "getRawContactId("+contactId+"), rawContactsCursor size: "+rawContactsCursor.getCount());
		if(contactId == null || rawContactsCursor.getCount() == 0) return null;
		List<String> rawContactIds = new ArrayList<String>();
		rawContactsCursor.moveToFirst();
		do {
			String cId = rawContactsCursor.getString(rawContactsCursor.getColumnIndex(RawContacts.CONTACT_ID));
			String rawContactId = rawContactsCursor.getString(rawContactsCursor.getColumnIndex(RawContacts._ID));
			// Log.i(TAG, "checking: contactID: "+cId+", rawContactId: "+rawContactId);
			if(cId != null && cId.equals(contactId))
			{
				Log.i(tag, "getRawContactId("+contactId+") = "+rawContactId);
				rawContactIds.add(rawContactId);
			}
		} while(rawContactsCursor.moveToNext());
		Log.i(tag, "returning null");
		return rawContactIds;
	}

	public static String getGroupNamesFromPhoneNumber(String phoneNumber, Context context) {
		Log.i(tag, "Getting groups for phone number: "+phoneNumber);
		if(phoneNumber == null) return null;
    	Cursor phonesCursor = context.getContentResolver().query(Phone.CONTENT_URI, 
                new String[] {
	        		Phone.CONTACT_ID,
	        		Phone.NUMBER }, 
	        		Phone.NUMBER +" = ? ", 
                new String[] { phoneNumber  }, null);
		Log.i(tag, "There are "+phonesCursor.getCount()+" contact entries");
		if(phonesCursor.getCount() == 0) return null;
		phonesCursor.moveToFirst();
		String contactId = phonesCursor.getString(phonesCursor.getColumnIndex(Phone.CONTACT_ID));

		Cursor contactGroupsCursor = context.getContentResolver()
    		.query(Data.CONTENT_URI, 
	    	    new String[] {
    				GroupMembership.GROUP_ROW_ID, 
    				Data.CONTACT_ID },
    				Data.CONTACT_ID + " = ? "
    				+ " and "+GroupMembership.MIMETYPE
    				+ " = ? ",
                new String[] { contactId, GroupMembership.CONTENT_ITEM_TYPE }, 
                	GroupMembership.GROUP_ROW_ID);
    	Log.i(tag, "contactId: "+contactId+", is in "+contactGroupsCursor.getCount()+" groups");
    	if(contactGroupsCursor.getCount() == 0) return null;
    	contactGroupsCursor.moveToFirst();
    	String groupIdsIn = "(";
    	int count = 0, num = contactGroupsCursor.getCount();
    	do {
    		
    		String groupId = contactGroupsCursor.getString(contactGroupsCursor.getColumnIndex(
    				GroupMembership.GROUP_ROW_ID));
    		Log.i(tag, "Group id: "+groupId);
    		groupIdsIn += groupId;
    		if(count < num -1) 
    			groupIdsIn += ", ";
    		count++;
    			
    	} while(contactGroupsCursor.moveToNext());
    	groupIdsIn += ")";
    	Log.i(tag, "groups in = "+groupIdsIn);
    	Cursor groupsCursor = context.getContentResolver().query(
    			Groups.CONTENT_URI, 
    			new String[] {
    				Groups.TITLE
    			},
	    		Groups.DELETED + " = 0 and " 
    			+ Groups._ID + " in "+groupIdsIn, null, null);
    	Log.i(tag, "There are "+groupsCursor.getCount()+" groups");
    	if(groupsCursor.getCount() == 0) return null;
    	Set<String> groups = new HashSet<String>();
    	
    	 
    	groupsCursor.moveToFirst();
    	do {
    		String groupName = groupsCursor.getString(groupsCursor.getColumnIndex(Groups.TITLE));
    		groups.add(groupName);
    	} while(groupsCursor.moveToNext());
    	
    	count = 0; num = groups.size();
    	String groupsString = "";
    	for(String g : groups) {
    		groupsString += g;
    		if(count < num - 1) groupsString += ", ";
    		count ++;
    	}
    	Log.i(tag, "return groups: "+groupsString);
		return groupsString;
	}

	public static void refreshAccount(Context ctx, String accountName) {
    	// int num = ctx.getContentResolver().delete(Constants.PIC_URI, PicTable.ACCOUNT + " = ? ", new String[]{accountName});
    	// Log.i(tag, "Deleted "+num+" pic entries");
	}

	public static Bitmap getPhoto(ContentResolver contentResolver, String contactId) {
		
		Uri photoUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, Long.parseLong(contactId));
		try {
			InputStream is = Contacts.openContactPhotoInputStream(contentResolver, photoUri);
			return BitmapFactory.decodeStream(is);
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static Cursor getContactServerData(Context context) {
		Cursor c = context.getContentResolver().query(Data.CONTENT_URI, 
				new String[] {Data.RAW_CONTACT_ID, PhonebookSyncAdapterColumns.DATA_PID},
				Data.MIMETYPE + " = '" + PhonebookSyncAdapterColumns.MIME_PROFILE + "'", null, Data.RAW_CONTACT_ID);
		return c;
	}

	public void addSharingWith(String groupId, String rawContactId) {
        ContentValues bookValues = new ContentValues();
        bookValues.put(BookTable.BOOKID, groupId);
        bookValues.put(BookTable.CONTACTID, rawContactId);
        bookValues.put(BookTable.DELETED, "0");
        Uri cUri = cr.insert(Constants.SHARE_BOOK_URI, bookValues);
	}

	public void setDeleteSharingWith(String bookId, String rawContactId) {
		Log.i(tag, "Setting delete flag for bookId: "+bookId+", rawContactId: "+rawContactId);
        ContentValues bookValues = new ContentValues();
        bookValues.put(BookTable.DELETED, "1");
        bookValues.put(BookTable.DIRTY, (String) null);

		int num = cr.update(Constants.SHARE_BOOK_URI, bookValues, 
				BookTable.BOOKID + " = ? and "+BookTable.CONTACTID+" = ? ", 
				new String[] {bookId, rawContactId});
		Log.i(tag, "Updated "+num+" shared books to deleted");
		Db.setGroupDirty(bookId, cr);
	}
	
	public void deleteSharingWith(String bookId, String rawContactId) {
		Log.i(tag, "Removing sharing for bookId: "+bookId+", rawContactId: "+rawContactId);
		int num = cr.delete(Constants.SHARE_BOOK_URI, 
				BookTable.BOOKID + " = ? and "+BookTable.CONTACTID+" = ? ", 
				new String[] {bookId, rawContactId});
		Log.i(tag, "Deleted "+num+" shared books");
		Db.setGroupDirty(bookId, cr);
	}

	public void updateGroupPermission(String groupId, Integer permission) {
        ContentValues bookValues = new ContentValues();
        bookValues.put(Groups.SYNC2, permission.toString());

		int num = cr.update(Groups.CONTENT_URI, bookValues, 
				Groups._ID + " = ? ", 
				new String[] {groupId});
		Log.i(tag, "Updated "+num+" shared book permission");
	}

	public String getContactId(String rawContactId, Cursor rawContactsCursor) {
		Log.i(tag, "getContactId("+rawContactId+"), rawContactsCursor size: "+rawContactsCursor.getCount());
		if(rawContactId == null || rawContactsCursor.getCount() == 0) return null;
		rawContactsCursor.moveToFirst();
		do {
			String cId = rawContactsCursor.getString(rawContactsCursor.getColumnIndex(RawContacts.CONTACT_ID));
			String rawId = rawContactsCursor.getString(rawContactsCursor.getColumnIndex(RawContacts._ID));
			// Log.i(TAG, "checking: contactID: "+cId+", rawContactId: "+rawContactId);
			if(rawId.equals(rawContactId))
			{
				Log.i(tag, "getContactId("+rawContactId+") = "+cId);
				return cId;
			}
		} while(rawContactsCursor.moveToNext());
		Log.i(tag, "returning null");
		return null;
	}

	public static void deleteServerData(Context applicationContext) {
		// int num = applicationContext.getContentResolver()
			// .delete(Data.CONTENT_URI, Data.MIMETYPE + " = '" + PhonebookSyncAdapterColumns.MIME_PROFILE + "'", null);
		// Log.i(tag, "deleted "+num+" rows of server data");
		int num = applicationContext.getContentResolver()
			.delete(Constants.CONTACT_URI, null, null);
		Log.i(tag, "deleted "+num+" rows of contact link data");
		num = applicationContext.getContentResolver()
			.delete(Constants.SHARE_BOOK_URI, null, null);
		Log.i(tag, "deleted "+num+" rows of share book data");
		
	}

	public static Account getAccount(Context context) {
		Account[] accounts = AccountManager.get(context)
				.getAccounts();
		Log.i(tag, "There are " + accounts.length + " accounts");
		for (Account account : accounts) 
			if(Constants.ACCOUNT_TYPE.equals(account.type))
				return account;
		return null;
	}
	
}
