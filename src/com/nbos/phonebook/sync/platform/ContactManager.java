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

package com.nbos.phonebook.sync.platform;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Im;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.StatusUpdates;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.collect.Lists;
import com.nbos.phonebook.R;
import com.nbos.phonebook.database.tables.BookTable;
import com.nbos.phonebook.sync.Constants;
import com.nbos.phonebook.sync.client.Contact;
import com.nbos.phonebook.sync.client.User;
import com.nbos.phonebook.sync.client.contact.Name;

/**
 * Class for managing contacts sync related mOperations
 */
public class ContactManager {
    /**
     * Custom IM protocol used when storing status messages.
     */
    public static final String CUSTOM_IM_PROTOCOL = "SampleSyncAdapter";
    private static final String tag = "ContactManager";

	/**
     * Add a list of status messages to the contacts provider.
     * 
     * @param context the context to use
     * @param accountName the username of the logged in user
     * @param statuses the list of statuses to store
     */
    public static void insertStatuses(Context context, String username,
        List<User.Status> list) {
        final ContentValues values = new ContentValues();
        final ContentResolver resolver = context.getContentResolver();
        final BatchOperation batchOperation =
            new BatchOperation(context);
        for (final User.Status status : list) {
            // Look up the user's sample SyncAdapter data row
            final long userId = status.getUserId();
            final long profileId = lookupProfile(resolver, userId);

            // Insert the activity into the stream
            if (profileId > 0) {
                values.put(StatusUpdates.DATA_ID, profileId);
                values.put(StatusUpdates.STATUS, status.getStatus());
                values.put(StatusUpdates.PROTOCOL, Im.PROTOCOL_CUSTOM);
                values.put(StatusUpdates.CUSTOM_PROTOCOL, CUSTOM_IM_PROTOCOL);
                values.put(StatusUpdates.IM_ACCOUNT, username);
                values.put(StatusUpdates.IM_HANDLE, status.getUserId());
                values.put(StatusUpdates.STATUS_RES_PACKAGE, context
                    .getPackageName());
                values.put(StatusUpdates.STATUS_ICON, R.drawable.icon);
                values.put(StatusUpdates.STATUS_LABEL, R.string.label);

                batchOperation
                    .add(ContactOperations.newInsertCpo(
                        StatusUpdates.CONTENT_URI, true).withValues(values)
                        .build());
                // A sync adapter should batch operations on multiple contacts,
                // because it will make a dramatic performance difference.
                if (batchOperation.size() >= 50) {
                    batchOperation.execute();
                }
            }
        }
        batchOperation.execute();
    }

    /**
     * Adds a single contact to the platform contacts provider.
     * 
     * @param context the Authenticator Activity context
     * @param accountName the account the contact belongs to
     * @param contact the sample SyncAdapter User object
     */
    public static void addContact(Context context, String accountName,
        Contact contact, BatchOperation batchOperation) {
        // Put the data in the contacts provider
        final ContactOperations contactOp =
            ContactOperations.createNewContact(context, Integer.parseInt(contact.serverId),
                accountName, batchOperation);
        contactOp.addName(contact.name);
        for(com.nbos.phonebook.sync.client.contact.Phone p : contact.phones)
        	contactOp.addPhone(p);
        for(com.nbos.phonebook.sync.client.contact.Email e : contact.emails)
        	contactOp.addEmail(e);
        contactOp.addProfileAction(Integer.parseInt(contact.serverId), accountName);
        //user.getLastName())
        /*.addEmail(
            contact.email).addPhone(contact.number, Phone.TYPE_MOBILE)
            .addPhone(contact.number, Phone.TYPE_OTHER).addProfileAction(
                Integer.parseInt(contact.serverId));*/
    }

    /**
     * Updates a single contact to the platform contacts provider.
     * 
     * @param context the Authenticator Activity context
     * @param resolver the ContentResolver to use
     * @param accountName the account the contact belongs to
     * @param contact the sample SyncAdapter contact object.
     * @param rawContactId the unique Id for this rawContact in contacts
     *        provider
     * @param dataCursor 
     * @param integer 
     */
    public static void updateContact(Context context, String accountName, Contact contact,
        long rawContactId){ 
    	Log.i(tag, "Update contact: "+contact.name+", rawContactId: "+rawContactId);
    	
    	Log.i(tag,"deleting name of contact");
    	context.getContentResolver().delete(SyncManager.addCallerIsSyncAdapterParameter(Data.CONTENT_URI),
    			Data.RAW_CONTACT_ID+ "=? and "+ Data.MIMETYPE + "=?", 
    			new String[]{String.valueOf(rawContactId), StructuredName.CONTENT_ITEM_TYPE });
    	Log.i(tag, "New Name: "+contact.name);
    	updateNameOfExistingContact(context, contact.name, rawContactId);
    	
        Log.i(tag, "deleting phone numbers");
    	context.getContentResolver().delete(
    		SyncManager.addCallerIsSyncAdapterParameter(Data.CONTENT_URI), 
    		Data.RAW_CONTACT_ID+ "=? and "+ Data.MIMETYPE + "=?", 
			new String[]{String.valueOf(rawContactId), Phone.CONTENT_ITEM_TYPE});
        
        Log.i(tag, "Contact has "+contact.phones.size()+" phones");
    	for(com.nbos.phonebook.sync.client.contact.Phone p : contact.phones)
        	addPhoneNumberToExistingContact(context, p, rawContactId);
    	
    	Log.i(tag, "deleting emails ");
    	context.getContentResolver().delete(
    			SyncManager.addCallerIsSyncAdapterParameter(Data.CONTENT_URI), 
    			Data.RAW_CONTACT_ID+ "=? and "+ Data.MIMETYPE +"=?", 
    			new String[]{String.valueOf(rawContactId), Email.CONTENT_ITEM_TYPE});
    	
    	for(com.nbos.phonebook.sync.client.contact.Email email : contact.emails)
    		addEmailToExistingContact(context, email, rawContactId);
    	
    }

    public static void updateNameOfExistingContact(Context context, Name ContactName, long rawContactId){
    	 Log.i(tag, ContactName+" added");
         ContentValues values = new ContentValues();
         
         values.put(StructuredName.RAW_CONTACT_ID, rawContactId);
         values.put(StructuredName.PREFIX, ContactName.prefix);
         values.put(StructuredName.GIVEN_NAME, ContactName.given);
         values.put(StructuredName.MIDDLE_NAME, ContactName.middle);
         values.put(StructuredName.FAMILY_NAME, ContactName.family);
         values.put(StructuredName.SUFFIX, ContactName.suffix);
         values.put(StructuredName.MIMETYPE,
                 StructuredName.CONTENT_ITEM_TYPE);
         
         context.getContentResolver().insert(
        		 SyncManager.addCallerIsSyncAdapterParameter(Data.CONTENT_URI),
        		 values);
    }
    public static void addPhoneNumberToExistingContact(Context context, 
    com.nbos.phonebook.sync.client.contact.Phone phone, long rawContactId) {
    	Log.i(tag, "Adding phone number "+phone.number);
    	if (TextUtils.isEmpty(phone.number)) return;
    	ContentValues values = new ContentValues();
    	values.put(Phone.RAW_CONTACT_ID, rawContactId);
        values.put(Phone.NUMBER, phone.number);
        int type = phone.getIntType();
        values.put(Phone.TYPE, type);
        if(type == Phone.TYPE_CUSTOM)
        	values.put(Phone.DATA3, phone.type);
        values.put(Phone.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
    	context.getContentResolver().insert(
    		SyncManager.addCallerIsSyncAdapterParameter(Data.CONTENT_URI), 
    		values);
    }
    
    public static void addEmailToExistingContact(Context context, com.nbos.phonebook.sync.client.contact.Email email, long rawContactId){
    	Log.i(tag, "adding email: "+email);
    	if(TextUtils.isEmpty(email.address)) return;
    	ContentValues values = new ContentValues();
    	values.put(Email.RAW_CONTACT_ID, rawContactId);
    	values.put(Email.DATA, email.address);
    	int type = email.getIntType();
    	values.put(Email.TYPE, type);
    	if(type == Email.TYPE_CUSTOM)
    		values.put(Email.DATA3, email.type);
    	values.put(Email.MIMETYPE, Email.CONTENT_ITEM_TYPE);
    	context.getContentResolver().insert(SyncManager.addCallerIsSyncAdapterParameter(Data.CONTENT_URI), values);
    	
    }
    /**
     * Deletes a contact from the platform contacts provider.
     * 
     * @param context the Authenticator Activity context
     * @param rawContactId the unique Id for this rawContact in contacts
     *        provider
     */
    public static void deleteContact(Context context, long rawContactId,
        BatchOperation batchOperation) {
        batchOperation.add(ContactOperations.newDeleteCpo(
            ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId),
            true).build());
    }

    /**
     * Returns the RawContact id for a sample SyncAdapter contact, or 0 if the
     * sample SyncAdapter user isn't found.
     * 
     * @param context the Authenticator Activity context
     * @param userId the sample SyncAdapter user ID to lookup
     * @return the RawContact id, or 0 if not found
     */
    /*private static long lookupRawContact(ContentResolver resolver, long userId) {
        long authorId = 0;
        final Cursor c =
            resolver.query(RawContacts.CONTENT_URI, UserIdQuery.PROJECTION,
                UserIdQuery.SELECTION, new String[] {String.valueOf(userId)},
                null);
        try {
            if (c.moveToFirst()) {
                authorId = c.getLong(UserIdQuery.COLUMN_ID);
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return authorId;
    }*/

    /**
     * Returns the Data id for a sample SyncAdapter contact's profile row, or 0
     * if the sample SyncAdapter user isn't found.
     * 
     * @param resolver a content resolver
     * @param userId the sample SyncAdapter user ID to lookup
     * @return the profile Data row id, or 0 if not found
     */
    private static long lookupProfile(ContentResolver resolver, long userId) {
        long profileId = 0;
        final Cursor c =
            resolver.query(Data.CONTENT_URI, ProfileQuery.PROJECTION,
                ProfileQuery.SELECTION, new String[] {String.valueOf(userId)},
                null);
        try {
            if (c != null && c.moveToFirst()) {
                profileId = c.getLong(ProfileQuery.COLUMN_ID);
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return profileId;
    }

    /**
     * Constants for a query to find a contact given a sample SyncAdapter user
     * ID.
     */
    private interface ProfileQuery {
        public final static String[] PROJECTION = new String[] {Data._ID};

        public final static int COLUMN_ID = 0;

        public static final String SELECTION =
            Data.MIMETYPE + "='" + PhonebookSyncAdapterColumns.MIME_PROFILE
                + "' AND " + PhonebookSyncAdapterColumns.DATA_PID + "=?";
    }
    /**
     * Constants for a query to find a contact given a sample SyncAdapter user
     * ID.
     */
    public interface UserIdQuery {
        // public final static String[] PROJECTION =
            // new String[] {RawContacts._ID, RawContacts.SOURCE_ID};
        	// new String[] {RawContacts._ID, Constants.CONTACT_SERVER_ID, RawContacts.DIRTY};
        public final static int COLUMN_ID = 0;

        // public static final String SELECTION =
            // RawContacts.ACCOUNT_TYPE + "='" + Constants.ACCOUNT_TYPE + "' AND " +
            //      RawContacts.SOURCE_ID + "=?";
        	// Constants.CONTACT_SERVER_ID + "=?";
    }

    /**
     * Constants for a query to get contact data for a given rawContactId
     */
    private interface DataQuery {
        public static final String[] PROJECTION =
            new String[] {Data._ID, Data.MIMETYPE, Data.DATA1, Data.DATA2,
                Data.DATA3,};

        public static final int COLUMN_ID = 0;
        public static final int COLUMN_MIMETYPE = 1;
        public static final int COLUMN_DATA1 = 2;
        public static final int COLUMN_DATA2 = 3;
        public static final int COLUMN_DATA3 = 4;
        public static final int COLUMN_PHONE_NUMBER = COLUMN_DATA1;
        public static final int COLUMN_PHONE_TYPE = COLUMN_DATA2;
        public static final int COLUMN_EMAIL_ADDRESS = COLUMN_DATA1;
        public static final int COLUMN_EMAIL_TYPE = COLUMN_DATA2;
        public static final int COLUMN_GIVEN_NAME = COLUMN_DATA2;
        public static final int COLUMN_FAMILY_NAME = COLUMN_DATA3;

        public static final String SELECTION = Data.RAW_CONTACT_ID + "=?";
    }

	public static void resetDirtySharedBooks(Context ctx) {
		ContentResolver cr = ctx.getContentResolver();
		int num = cr.delete(Constants.SHARE_BOOK_URI, BookTable.DELETED + " = 1", null);
		Log.i(tag, "Deleted "+num+" contacts sharing with");
	    ContentValues values = new ContentValues();
	    values = new ContentValues();
	    values.put(BookTable.DIRTY, "0");
	    num = cr.update(
	    		Constants.SHARE_BOOK_URI, values,
	    		null, null);
	    Log.i(tag, "Updated "+num+" sharebooks to dirty = 0");
	}

	public static void resetDirtyGroups(Context ctx) {
		ContentResolver cr = ctx.getContentResolver();
	    ContentValues values = new ContentValues();
	    values.put(ContactsContract.Groups.DIRTY, "0");
	    int num = cr.update(ContactsContract.Groups.CONTENT_URI, values, null, null);
	    Log.i(tag, "Updated "+num+" groups to dirty = 0");
	}
	

	public static void setDirtyContacts(Context mContext) { // for testing
	    ContentResolver cr = mContext.getContentResolver();
	    Uri uri = ContactsContract.RawContacts.CONTENT_URI;
	    ContentValues values = new ContentValues();
	    values.put(ContactsContract.RawContacts.DIRTY, 1);
	    int num = cr.update(uri, values, null, null);
	    Log.i("ContactManager", "Resetting "+num+" dirty on contacts");
	}

	public static void updateBook(JSONObject book, Context context) throws JSONException {
        int sourceId = book.getInt("sourceId"),
    		groupId = book.getInt("groupId");
        Log.i(tag, "updateBook, sourceId: "+sourceId+", contactId: "+groupId);
        ContentResolver cr = context.getContentResolver();
        Uri uri = ContactsContract.Groups.CONTENT_URI;
        ContentValues values = new ContentValues();
        values.put(ContactsContract.Groups.SOURCE_ID, sourceId);
        int rows = cr.update(uri, values, ContactsContract.Groups._ID + " = " + groupId, null);
        Log.i(tag, rows + " rows updated");
	}

	public static void updateGroup(JSONObject group, Context context) throws JSONException {
        int sourceId = group.getInt("sourceId"),
			groupId = group.getInt("groupId");
        Log.i(tag, "updateGroup, sourceId: "+sourceId+", groupId: "+groupId);
	    ContentResolver cr = context.getContentResolver();
	    Uri uri = ContactsContract.Groups.CONTENT_URI;
	    ContentValues values = new ContentValues();
	    values.put(ContactsContract.Groups.SOURCE_ID, sourceId);
	    int rows = cr.update(uri, values, ContactsContract.Groups._ID + " = " + groupId, null);
	    Log.i(tag, "updated "+ rows + " rows to sourceId: "+sourceId);
	}
}
