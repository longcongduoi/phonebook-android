/*
 * Copyright (C) 2010 The Android Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.nbos.phonebook.sync.syncadapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.ParseException;
import org.apache.http.auth.AuthenticationException;
import org.json.JSONException;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.database.Cursor;
import android.database.CursorJoiner;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.nbos.phonebook.database.tables.BookTable;
import com.nbos.phonebook.sync.Constants;
import com.nbos.phonebook.sync.client.Contact;
import com.nbos.phonebook.sync.client.Group;
import com.nbos.phonebook.sync.client.NetworkUtilities;
import com.nbos.phonebook.sync.client.SharedBook;
import com.nbos.phonebook.sync.client.SharingBook;
import com.nbos.phonebook.sync.client.User;
import com.nbos.phonebook.sync.client.User.Status;
import com.nbos.phonebook.sync.platform.ContactManager;

/**
 * SyncAdapter implementation for syncing sample SyncAdapter contacts to the
 * platform ContactOperations provider.
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {
    private static final String TAG = "SyncAdapter";

    private final AccountManager mAccountManager;
    private final Context mContext;

    private Date mLastUpdated;

    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        mContext = context;
        mAccountManager = AccountManager.get(context);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
        ContentProviderClient provider, SyncResult syncResult) {
        List<User> users;
        List<SharedBook> sharedBooks;
        List<Status> statuses;
        String authtoken = null;
        
        // ContactManager.setDirtyContacts(mContext); // for testing
        
        try {
             // use the account manager to request the credentials
             authtoken =
                mAccountManager.blockingGetAuthToken(account,
                    Constants.AUTHTOKEN_TYPE, true /* notifyAuthFailure */);
             // fetch updates from the sample service over the cloud
             
             Object[] update =   NetworkUtilities.fetchFriendUpdates(account, authtoken,
                    mLastUpdated, getPhoneNumber());
             users =  (List<User>) update[0];
             sharedBooks = (List<SharedBook>) update[1];
             NetworkUtilities.sendFriendUpdates(account, authtoken,
                     mLastUpdated, getFewContacts(5), getNewContacts(), getGroups(), getSharingBooks(), mContext);
            // update the last synced date.
            mLastUpdated = new Date();
            // update platform contacts.
            ContactManager.syncContacts(mContext, account.name, users);
            ContactManager.syncSharedBooks(mContext, account.name, sharedBooks);
            // fetch and update status messages for all the synced users.
            // statuses = NetworkUtilities.fetchFriendStatuses(account, authtoken);
            // ContactManager.insertStatuses(mContext, account.name, statuses);
            ContactManager.resetDirtyContacts(mContext);
        } catch (final AuthenticatorException e) {
            syncResult.stats.numParseExceptions++;
            Log.e(TAG, "AuthenticatorException", e);
        } catch (final OperationCanceledException e) {
            Log.e(TAG, "OperationCanceledExcetpion", e);
        } catch (final IOException e) {
            Log.e(TAG, "IOException", e);
            syncResult.stats.numIoExceptions++;
        } catch (final AuthenticationException e) {
            mAccountManager.invalidateAuthToken(Constants.ACCOUNT_TYPE,
                authtoken);
            syncResult.stats.numAuthExceptions++;
            Log.e(TAG, "AuthenticationException", e);
        } catch (final ParseException e) {
            syncResult.stats.numParseExceptions++;
            Log.e(TAG, "ParseException", e);
        } catch (final JSONException e) {
            syncResult.stats.numParseExceptions++;
            Log.e(TAG, "JSONException", e);
        }
    }
    
    
	private Long getPhoneNumber() {
    	long phoneNumber = Long.parseLong(((TelephonyManager) 
    			mContext.getSystemService(Context.TELEPHONY_SERVICE))
    			.getLine1Number().replace("-", ""));	
    	return phoneNumber;
    }

	private List<SharingBook> getSharingBooks() {
    	List<SharingBook> books = new ArrayList<SharingBook>();
    	Cursor cursor = mContext.getContentResolver().query(
    			Uri.parse(Constants.SHARE_BOOK_PROVIDER),
    			null,
    			BookTable.DIRTY + " is null",
    			null, null);
    	if(cursor != null)
    		Log.i(TAG, "There are "+cursor.getCount()+" contacts sharing books");
    	while(cursor.moveToNext())
    		books.add(
    			new SharingBook(
    				cursor.getInt(cursor.getColumnIndex(BookTable.BOOKID)),
    				cursor.getInt(cursor.getColumnIndex(BookTable.CONTACTID))));
    	
    	return books;
    }
    
	private List<Group> getGroups() {
		List<Group> groups = new ArrayList<Group>();
	    ContentResolver cr = mContext.getContentResolver();
	    Cursor cursor = cr.query(ContactsContract.Groups.CONTENT_SUMMARY_URI, null,
	        // "DISPLAY_NAME = '" + NAME + "'",
	    	ContactsContract.Groups.DELETED + " = 0 "
	    	+ "and " + ContactsContract.Groups.DIRTY + " = 1 ",	    		
	    	null, null);
	    Log.i(TAG, "There are "+cursor.getCount()+" groups");
	    
	    
	    Cursor contactsCursor = cr.query(ContactsContract.Contacts.CONTENT_URI,
	    		// null,
	    	    new String[] {
	    			ContactsContract.Contacts._ID,
	    			ContactsContract.Contacts.DISPLAY_NAME
	    		},
	    		null, null, null);
	    Log.i(TAG, "There are "+contactsCursor.getCount()+" contacts");
	    
	    while(cursor.moveToNext())
	    {
	    	List<Contact> contacts = new ArrayList<Contact>();
	    	String name = cursor.getString(cursor.getColumnIndex(ContactsContract.Groups.TITLE));
	    	int groupId = cursor.getInt(cursor.getColumnIndex(ContactsContract.Groups._ID));
	    	String dirty = cursor.getString(cursor.getColumnIndex(ContactsContract.Groups.DIRTY));
		    Cursor dataCursor = cr.query(ContactsContract.Data.CONTENT_URI,
		    		// null,
		    	    new String[] {
		    			ContactsContract.Contacts._ID, 
		    			ContactsContract.Data.RAW_CONTACT_ID, 
		    			ContactsContract.RawContacts._ID,
		    			ContactsContract.Contacts.DISPLAY_NAME
		    		},
		    	    ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID
		    	    + " = " 
		    	    + cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID)),
		    	    null, null);
		    Log.i(TAG, "There are "+dataCursor.getCount()+" contacts in data");
	    	
		    CursorJoiner joiner = new CursorJoiner(
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
	        			int contactId = contactsCursor.getInt(contactsCursor.getColumnIndex(
	        					ContactsContract.Contacts._ID));
	        			String contactName = contactsCursor.getString(contactsCursor.getColumnIndex(
	        					ContactsContract.Contacts.DISPLAY_NAME));
	        	        Cursor phones = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, 
	        	        		null, 		
	        	        		ContactsContract.CommonDataKinds.Phone.RAW_CONTACT_ID +" = "+ contactId,
	        	        		null, null);
	        	            
	        	            // Log.i(TAG, "There are "+phones.getCount()+" phone numbers");
	        	            if(phones.getCount() == 0) break;
	        	            phones.moveToFirst();
	        	            Long contactNumber = phones.getLong(phones
	        	                    .getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));

	        			contacts.add(new Contact(contactId, contactNumber, contactName));
	        			Log.i(TAG, "added contact: "+contactId+", "+contactNumber+", "+contactName);
	        		break;
	        	}
	        }	    
	    	
	        groups.add(new Group(groupId, name, contacts));
	        Log.i(TAG, "dirty is "+dirty);
	        Log.i(TAG, "Added book["+groupId+"] "+name+" with "+contacts.size()+" contacts");
	    	// books
	    }
	    return groups;
	}

    private List<User> getFewContacts(int numContacts) {
	    ContentResolver cr = mContext.getContentResolver();
	    Uri uri = ContactsContract.RawContacts.CONTENT_URI;
	    Cursor cursor = cr.query(uri, 
	    		null, null, // get all contacts
	    	//	ContactsContract.RawContacts.DIRTY + " = 1",
	        // "DISPLAY_NAME = '" + NAME + "'",
	    	// null, 
	    	null, null);
	    Log.i(TAG, "There are "+cursor.getCount()+" contacts");
	    List<User> users = new ArrayList<User>();
	    int num = 0;
	    while(cursor.moveToNext()) {
	    	if(num > numContacts) break;
	        String contactId =
	            	cursor.getString(cursor.getColumnIndex(ContactsContract.RawContacts._ID));
	        String sourceId = cursor.getString(cursor.getColumnIndex(ContactsContract.RawContacts.SOURCE_ID));
	        String dirty = cursor.getString(cursor.getColumnIndex(ContactsContract.RawContacts.DIRTY));
	        String version = cursor.getString(cursor.getColumnIndex(ContactsContract.RawContacts.VERSION));
	        Cursor contact = cr.query(ContactsContract.Contacts.CONTENT_URI, null,
	        		ContactsContract.Contacts._ID+" = '" + contactId + "' ",
	    	null, null);
	        if(contact.getCount()==0) continue;
	        contact.moveToFirst();
	        //Log.i(TAG, "There are "+contact.getCount()+" contacts for "+contactId);
	        // contact.moveToFirst();
	        String name = contact.getString(contact.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
	        
	            //sourceId = 
	            	//cursor.getString(cursor.getColumnIndex(ContactsContract.RawContacts.SOURCE_ID));
	        Cursor phones = mContext.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, 
	    		null, 		
	    		ContactsContract.CommonDataKinds.Phone.RAW_CONTACT_ID +" = "+ contactId,
	    		null, null);
	        if(phones.getCount() == 0) continue;
	        phones.moveToFirst();
	        String phoneNumber = phones.getString(phones
	                .getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));
	        Log.i(TAG, "id: "+contactId+", name is: "+name+", number is: "+phoneNumber+", sourceId: "+sourceId+", dirty: "+dirty+", version is: "+version);
	        users.add(new User(name, phoneNumber, sourceId != null ? Integer.parseInt(sourceId) : 0, Integer.parseInt(contactId)));
	        num++;
	        phones.close();
	    }
	    return users;
	}

	private List<User> getNewContacts() {
	    ContentResolver cr = mContext.getContentResolver();
	    Uri uri = ContactsContract.RawContacts.CONTENT_URI;
	    Cursor cursor = cr.query(uri, 
	    		null,
	    		ContactsContract.RawContacts.DIRTY + " = 1",
	        // "DISPLAY_NAME = '" + NAME + "'",
	    	// null, 
	    	null, null);
	    Log.i(TAG, "There are "+cursor.getCount()+" contacts");
	    List<User> users = new ArrayList<User>();
	    while(cursor.moveToNext()) {
	        String contactId =
	            	cursor.getString(cursor.getColumnIndex(ContactsContract.RawContacts._ID));
	        String sourceId = cursor.getString(cursor.getColumnIndex(ContactsContract.RawContacts.SOURCE_ID));
	        String dirty = cursor.getString(cursor.getColumnIndex(ContactsContract.RawContacts.DIRTY));
	        String version = cursor.getString(cursor.getColumnIndex(ContactsContract.RawContacts.VERSION));
	        Cursor contact = cr.query(ContactsContract.Contacts.CONTENT_URI, null,
	        		ContactsContract.Contacts._ID+" = '" + contactId + "' ",
	    	null, null);
	        if(contact.getCount()==0) continue;
	        contact.moveToFirst();
	        //Log.i(TAG, "There are "+contact.getCount()+" contacts for "+contactId);
	        // contact.moveToFirst();
	        String name = contact.getString(contact.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
	        
	            //sourceId = 
	            	//cursor.getString(cursor.getColumnIndex(ContactsContract.RawContacts.SOURCE_ID));
	        Cursor phones = mContext.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, 
	    		null, 		
	    		ContactsContract.CommonDataKinds.Phone.RAW_CONTACT_ID +" = "+ contactId,
	    		null, null);
	        if(phones.getCount() == 0) continue;
	        phones.moveToFirst();
	        String phoneNumber = phones.getString(phones
	                .getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));
	        Log.i(TAG, "id: "+contactId+", name is: "+name+", number is: "+phoneNumber+", sourceId: "+sourceId+", dirty: "+dirty+", version is: "+version);
	        users.add(new User(name, phoneNumber, sourceId != null ? Integer.parseInt(sourceId) : 0, Integer.parseInt(contactId)));
	        phones.close();
	    }
	    return users;
	}
}
