package com.nbos.phonebook.sync.platform;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;

import com.nbos.phonebook.sync.Constants;
import com.nbos.phonebook.sync.client.Contact;
import com.nbos.phonebook.sync.client.PhoneContact;
import com.nbos.phonebook.sync.client.contact.Address;
import com.nbos.phonebook.sync.client.contact.Email;
import com.nbos.phonebook.sync.client.contact.Im;
import com.nbos.phonebook.sync.client.contact.Name;
import com.nbos.phonebook.sync.client.contact.Organization;
import com.nbos.phonebook.sync.client.contact.Phone;

public class UpdateContacts {
	static String tag = "UpdateContacts";
	Cloud cloud;
	int numContacts = 0;
    public static String[] DATA_KEYS = new String[]{
        Data.DATA1,
        Data.DATA2,
        Data.DATA3,
        Data.DATA4,
        Data.DATA5,
        Data.DATA6,
        Data.DATA7,
        Data.DATA8,
        Data.DATA9,
        Data.DATA10,
        Data.DATA11,
        Data.DATA12,
        Data.DATA13,
        Data.DATA14,
        Data.DATA15,
        Data.SYNC1,
        Data.SYNC2,
        Data.SYNC3,
        Data.SYNC4};
	
	UpdateContacts(Cloud cloud) throws ClientProtocolException, IOException, JSONException
	{
		this.cloud = cloud;
		List<PhoneContact> contacts = getContacts(cloud);
		numContacts = contacts.size();
		sendContactUpdates(contacts);
	}

	public static List<PhoneContact> getContacts(Cloud cloud) 
	{
		List<PhoneContact> contacts = new ArrayList<PhoneContact>();
    	Cursor cursor = getRawContactsEntityCursor(cloud.cr, cloud.newOnly);
    	
    	Log.i(tag, "There are "+cursor.getCount()+" entries, "+cursor.getColumnCount()+" columns.");
    	// for(String col : rawContactsCursor.getColumnNames())
    		// Log.i(tag, "col: "+col);
    	String prevId = "", dirty;
    	PhoneContact contact = null;
    	if(cursor.getCount() == 0) return contacts;
    	cursor.moveToFirst();
    	do {
    		String  contactId = cursor.getString(cursor.getColumnIndex(RawContacts.CONTACT_ID)),
    			rawContactId = cursor.getString(cursor.getColumnIndex(RawContacts._ID)),
    			deleted = cursor.getString(cursor.getColumnIndex(RawContacts.DELETED)),
    			mimeType = cursor.getString(cursor.getColumnIndex(Data.MIMETYPE)),
    			accountType = cursor.getString(cursor.getColumnIndex(RawContacts.ACCOUNT_TYPE));
    			dirty = cursor.getString(cursor.getColumnIndex(RawContacts.DIRTY));
    		if(cloud.contactServerIdsMap != null)
    		{
    			String contactServerId = cloud.contactServerIdsMap.get(rawContactId);
    			if(cloud.syncedContactServerIds.contains(contactServerId)) // This contact has already been synced
    				continue;
    		}
    		String str = "";
            for (String key : DATA_KEYS) 
            {
                final int columnIndex = cursor.getColumnIndexOrThrow(key);
                if (cursor.isNull(columnIndex)) {
                    // don't put anything
                } else {
                    try {
                    	str += key+": "+cursor.getString(columnIndex)+", ";
                    	// Log.i(tag, key+": "+cursor.getString(columnIndex));
                        // cv.put(key, cursor.getString(columnIndex));
                    } catch (SQLiteException e) {
                    	str += key+": isBlob, ";
                    	// Log.i(tag, key+": isBlob");
                        // cv.put(key, cursor.getBlob(columnIndex));
                    }
                }
            }
            // Log.i(tag, "contactId: "+contactId+", dirty: "+dirty+", mimetype: "+mimeType+", data:: "+str);
    		// if(contactId == null) continue; // TODO: SIM contacts have null contactId
            if(!prevId.equals(rawContactId))
            {
            	if(contact != null)
            	{            	
            		Log.i(tag, "rawId: "+contact.rawContactId+", name: "+contact.name+", accountType: "
            				+contact.accountType+", deleted: "+contact.deleted+", dirty: "+dirty+" ,contactId: "+contactId);
            		contacts.add(contact);
            	}
            	contact = new PhoneContact();
            	contact.rawContactId = rawContactId;
            	contact.accountType = accountType;
            	contact.deleted = deleted.equals("1");
            	prevId = rawContactId;
            }
            addContactField(contact, cursor, mimeType);
    	} while(cursor.moveToNext());
    	if(contact != null)
    	{
    		Log.i(tag, "rawId: "+contact.rawContactId+", name: "+contact.name+", accountType: "+contact.accountType+", deleted: "+contact.deleted+", dirty: "+dirty);
    		contacts.add(contact);
    	}
    	cursor.close();
    	Log.i(tag, "returning "+contacts.size()+" contacts");
    	return contacts;
	}
	
	public static Cursor getRawContactsEntityCursor(ContentResolver cr, boolean newOnly) {
	    String where = newOnly ? ContactsContract.RawContacts.DIRTY + " = 1" : null;
		return cr.query(ContactsContract.RawContactsEntity.CONTENT_URI, null, where, null, ContactsContract.RawContacts._ID);	
	}

	static void addContactField(Contact contact, Cursor cursor, String mimeType) {
		if(mimeType == null) return;
        if(mimeType.equals(CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE))
        	Name.add(contact, cursor);
        if(mimeType.equals(CommonDataKinds.Phone.CONTENT_ITEM_TYPE))
        	Phone.add(contact, cursor);
        if(mimeType.equals(CommonDataKinds.Email.CONTENT_ITEM_TYPE))
        	Email.add(contact, cursor);
        if(mimeType.equals(CommonDataKinds.Im.CONTENT_ITEM_TYPE))
        	Im.add(contact, cursor);
        if(mimeType.equals(CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE))
        	Address.add(contact, cursor);
        if(mimeType.equals(CommonDataKinds.Organization.CONTENT_ITEM_TYPE))
        	Organization.add(contact, cursor);
        if(mimeType.equals(CommonDataKinds.Note.CONTENT_ITEM_TYPE))
        	Contact.addNote(contact, cursor);
        if(mimeType.equals(CommonDataKinds.Nickname.CONTENT_ITEM_TYPE))
        	Contact.addNickname(contact, cursor);
        if(mimeType.equals(CommonDataKinds.Website.CONTENT_ITEM_TYPE))
        	Contact.addWebsite(contact, cursor);
        if(mimeType.equals(PhonebookSyncAdapterColumns.MIME_PROFILE)) // the server id
        	addServerId(contact, cursor);
	}
	
	static void addServerId(Contact contact, Cursor c) {
		String serverId = c.getString(c.getColumnIndex(PhonebookSyncAdapterColumns.DATA_PID));
		contact.serverId = serverId;
	}

	private void sendContactUpdates(List<PhoneContact> contacts) throws ClientProtocolException, IOException, JSONException {
        int batchSize = 50;
        
		for(int b = 0; b < contacts.size();  b = b + batchSize)
		{
			Log.i(Cloud.tag, "Sending batch #"+b);
			int numContacts = 0;
	        List<NameValuePair> params = cloud.getAuthParams();
			for(int i = b; i < b + batchSize && i < contacts.size(); i++)
			{
				//System.out.print(i+", ");
	        	PhoneContact contact =  contacts.get(i);
	        	if(contact.serverId != null // this server id has already been synced in the pull 
	        	&& cloud.syncedContactServerIds.contains(contact.serverId))
	        		continue; 
	        	contact.addParams(params, new Integer(numContacts).toString());
	        	numContacts++;
			}
			Log.i(tag, "num contacts: "+numContacts);
	        params.add(new BasicNameValuePair("numContacts", new Integer(numContacts).toString()));
	        if(cloud.lastUpdated != null)
	        	params.add(new BasicNameValuePair(Constants.ACCOUNT_LAST_UPDATED, cloud.lastUpdated));
			if(numContacts > 0)
			{
				JSONArray contactUpdates = new JSONArray(cloud.post(Cloud.SEND_CONTACT_UPDATES_URI, params));
				Log.i(tag, "There are "+contactUpdates.length()+" contact updates");
				updateServerData(contactUpdates);
			}
		}
	}
	
	private void updateServerData(JSONArray contactUpdates) throws JSONException {
        final BatchOperation batchOperation = new BatchOperation(cloud.context);
        Map<String, String []> contactServerIds = cloud.getContactServerIds();
        Log.i(tag, "Contact serverIds size is: "+contactServerIds.size()+", num updates: "+contactUpdates.length());
        Log.i(tag, "Contact serverIds: "+contactServerIds);
        for (int i = 0; i < contactUpdates.length(); i++)
        {
        	JSONObject contact = contactUpdates.getJSONObject(i);
            long serverId = contact.getLong("sourceId"),
        	contactId = contact.getLong("contactId");
            String cId = new Long(contactId).toString();
            String [] data = contactServerIds.get(cId);
            String sourceId = new Long(serverId).toString();
            Log.i(tag, "updateContact, sourceId: "+serverId+", contactId: "+contactId+", data: "+data);
            if(data == null)
            {
            	insertServerId(contactId, serverId, cloud.account, batchOperation);
            	cloud.contactServerIdsMap.put(cId, sourceId);
            }
            else
            {
            	Log.i(tag, "checking update");
            	String dataId = data [0],
            		sId = data[1];
            	if(Long.parseLong(sId) != serverId)
            		updateServerId(dataId, contactId, serverId, batchOperation);
            }
        	
        	if(batchOperation.size() > 50)
        	{
        		Log.i(tag, "Executing batch server data: "+batchOperation.size());
        		batchOperation.execute();
        	}
        }
        Log.i(tag, "Executing last batch server data: "+batchOperation.size());
        batchOperation.execute();
	}

	public void resetDirtyContacts() {
		// TODO: reset individual contact and group from update contact or group
	    
	    Uri uri = RawContacts.CONTENT_URI;
	    ContentValues values = new ContentValues();
	    values.put(RawContacts.DIRTY, 0);
	    int num = cloud.cr.update(uri, values, null, null);
	    Log.i(tag, "Resetting "+num+" dirty on contacts");
	    
	    // delete phonebook contacts that are marked deleted
	    num = cloud.cr.delete(SyncManager.addCallerIsSyncAdapterParameter(uri), RawContacts.DELETED + " = 1 "
	    		+" and " + RawContacts.ACCOUNT_TYPE + " = ? ", 
	    		new String[] {Constants.ACCOUNT_TYPE});
	    Log.i(tag, "Deleted "+num+" phonebook contacts");
	}

	public static void insertServerId(Long rawContactId, Long serverId, String account, BatchOperation batchOperation) {
		Log.i(tag, "insertServerId("+serverId+") for rawContactId: "+rawContactId);
		ContentProviderOperation.Builder builder = 
			ContentProviderOperation.newInsert(
	            SyncManager.addCallerIsSyncAdapterParameter(Data.CONTENT_URI))
	            .withYieldAllowed(true);
		ContentValues values = new ContentValues();
        values.put(PhonebookSyncAdapterColumns.DATA_PID, serverId);
        values.put(PhonebookSyncAdapterColumns.ACCOUNT, account);
        values.put(Data.RAW_CONTACT_ID, rawContactId);        
        values.put(Data.MIMETYPE, PhonebookSyncAdapterColumns.MIME_PROFILE);
        builder.withValues(values);
        batchOperation.add(builder.build());
	}

	void updateServerId(String dataId, long rawContactId,
			long serverId, BatchOperation batchOperation) {
		Log.i(tag, "updateServerData - raw: "+rawContactId+", serverId: "+serverId+", data: "+dataId);
		Uri uri = SyncManager.addCallerIsSyncAdapterParameter(Data.CONTENT_URI);
			uri = ContentUris.withAppendedId(uri, Long.parseLong(dataId));
		ContentProviderOperation.Builder builder = 
			ContentProviderOperation.newUpdate(uri)
	            .withYieldAllowed(true);
			
    	ContentValues values = new ContentValues();
        values.put(PhonebookSyncAdapterColumns.DATA_PID, serverId);
        values.put(PhonebookSyncAdapterColumns.ACCOUNT, cloud.account);
        builder.withValues(values);
        batchOperation.add(builder.build());
	}
	
}
