package com.nbos.phonebook.sync.platform;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Groups;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;

import com.nbos.phonebook.Db;
import com.nbos.phonebook.database.tables.ContactTable;
import com.nbos.phonebook.sync.Constants;
import com.nbos.phonebook.sync.client.Contact;
import com.nbos.phonebook.sync.client.ContactPicture;
import com.nbos.phonebook.sync.client.Group;
import com.nbos.phonebook.sync.client.PhoneContact;
import com.nbos.phonebook.sync.client.ServerData;
import com.nbos.phonebook.sync.client.SharingBook;
import com.nbos.phonebook.util.ImageInfo;
import com.nbos.phonebook.util.Notify;
import com.nbos.phonebook.value.PicData;

public class Cloud {
	static String tag = "Cloud";
	Db db;
    Context context;
    String accountName, authToken, lastUpdated;
    HttpClient httpClient;
    List<PicData> serverPicData;
    Set<String> unchangedPicsRawContactIds, syncedContactServerIds, syncedGroupServerIds;
    boolean newOnly;
    public static final int REGISTRATION_TIMEOUT = 20 * 60 * 1000; // ms

    public static final String 
    	PARAM_USERNAME = "username",
    	PARAM_PASSWORD = "password",
    	PARAM_PHONE_NUMBER = "ph",
    	PARAM_VALIDATION_CODE = "valid",
    	PARAM_UPDATED = "timestamp",
    	USER_AGENT = "AuthenticationService/1.0",
    	BASE_URL = "http://phonebook.nbostech.com/phonebook",
    	// BASE_URL = "http://10.9.8.29:8080/phonebook",
    	AUTH_URI = BASE_URL + "/mobile/index",
    	REG_URL = BASE_URL + "/mobile/register",
    	VALIDATION_URI = BASE_URL + "/mobile/validate",
    	NEW_VALIDATION_CODE_URI = BASE_URL + "/mobile/newValidationCode",
    	CHECK_VALID_ACCOUNT_URI = BASE_URL + "/mobile/valid",
    	GET_CONTACT_UPDATES_URI = BASE_URL + "/mobile/contacts",
    	GET_SHARED_BOOK_UPDATES_URI = BASE_URL + "/mobile/sharedBooks",
    	GET_SHARED_BOOK_ID_UPDATES_URI = BASE_URL + "/mobile/sharedBookIds",
        SEND_CONTACT_UPDATES_URI = BASE_URL + "/mobile/updateContacts",
        SEND_GROUP_UPDATES_URI = BASE_URL + "/mobile/updateGroups",
        TIMESTAMP_URI = BASE_URL + "/mobile/timestamp",
    	SEND_SHARED_BOOK_UPDATES_URI = BASE_URL + "/mobile/updateSharedBooks",
    	SEND_LINK_UPDATES_URI = BASE_URL + "/mobile/updateLinks",
    	SEND_CHANGED_LINK_UPDATES_URI = BASE_URL + "/mobile/updateChangedLinks",
    	UPLOAD_CONTACT_PIC_URI = BASE_URL + "/fileUploader/process",
    	DOWNLOAD_CONTACT_PIC_URI = BASE_URL + "/download/index/",
    	GET_PIC_DATA_URI = BASE_URL + "/mobile/picData";

	public Cloud(Context context, String name, String authtoken) {
		this.context = context;
		this.db = new Db(context);
		accountName = name;
		authToken = authtoken;
	}
	
	public String sync(String lastUpdated) throws AuthenticationException, ParseException, JSONException, IOException {
		// sendAllContacts();
		this.lastUpdated = lastUpdated;		
		newOnly = lastUpdated != null;
        Object[] update = getContactUpdates();
		unchangedPicsRawContactIds = new HashSet<String>();
		syncedContactServerIds = new HashSet<String>();
		syncedGroupServerIds = new HashSet<String>();
        List<Contact> contacts =  (List<Contact>) update[0];
        List<Group> groups = (List<Group>) update[1];
        List<Group> sharedBooks = getSharedBooks();
        notify(contacts, groups, sharedBooks);
        if(contacts.size() > 0 || groups.size() > 0 || sharedBooks.size() > 0)
        {
        	serverPicData = getServerPicData();
        	new SyncManager(context, accountName, 
        			contacts, groups, sharedBooks, 
        			serverPicData, unchangedPicsRawContactIds, 
        			syncedContactServerIds, syncedGroupServerIds);
        }

        getSharedBookIds();
        
        sendUpdates();
        return getTimestamp();
	}

	/*public void sendAllContacts() throws ClientProtocolException, IOException, JSONException {
		sendFriendUpdates(false);
	}*/
	
	private void getSharedBookIds() throws ClientProtocolException, JSONException, IOException {
        List<NameValuePair> params = getAuthParams();
        if(lastUpdated != null)
        	params.add(new BasicNameValuePair(Constants.ACCOUNT_LAST_UPDATED, lastUpdated));
        final JSONArray sharedBookUpdateIds = new JSONArray(post(GET_SHARED_BOOK_ID_UPDATES_URI, params));
        Set<String> sharedBookIds = new HashSet<String>();
        for (int i = 0; i < sharedBookUpdateIds.length(); i++)  
        	sharedBookIds.add(new Long(sharedBookUpdateIds.getLong(i)).toString());
        deleteSharedBooksNotIn(sharedBookIds);
	}

	private void deleteSharedBooksNotIn(Set<String> sharedBookSourceIds) {
	    Cursor sharedGroupsCursor = context.getContentResolver()
	    	.query(Groups.CONTENT_URI, null,
	    		Groups.SYNC1+" is not null"
	    		+" and "+Groups.ACCOUNT_NAME+" = ? "
	    		+" and "+Groups.ACCOUNT_TYPE+" = ? ", 
	    		new String[]{accountName, Constants.ACCOUNT_TYPE}, null),
	    		
	    	groupItemsCursor = context.getContentResolver()
	    		.query(Data.CONTENT_URI, 
	    				new String[] {
	    					GroupMembership.GROUP_ROW_ID,
	    					GroupMembership.RAW_CONTACT_ID
	    				}, 
	    				GroupMembership.MIMETYPE+" = ? ", 
	    				new String[] {GroupMembership.CONTENT_ITEM_TYPE}, 
	    				GroupMembership.RAW_CONTACT_ID);
	    
	    Log.i(tag, "There are "+sharedGroupsCursor.getCount()+" shared groups");
	    Log.i(tag, "There are "+groupItemsCursor.getCount()+" grouped contacts");
	    
	    Set<String> sharedBookIds = new HashSet<String>();
	    sharedGroupsCursor.moveToFirst();
	    if(sharedGroupsCursor.getCount() > 0)
	    do {
	    	String groupId = sharedGroupsCursor.getString(sharedGroupsCursor.getColumnIndex(Groups._ID)),
	    		sourceId = sharedGroupsCursor.getString(sharedGroupsCursor.getColumnIndex(Groups.SOURCE_ID));
	    	if(sharedBookSourceIds.contains(sourceId))
	    		sharedBookIds.add(groupId);
	    } while(sharedGroupsCursor.moveToNext());

	    sharedGroupsCursor.moveToFirst();
	    if(sharedGroupsCursor.getCount() > 0)
	    do {
	    	String groupId = sharedGroupsCursor.getString(sharedGroupsCursor.getColumnIndex(Groups._ID)),
	    		sourceId = sharedGroupsCursor.getString(sharedGroupsCursor.getColumnIndex(Groups.SOURCE_ID));
	    	if(!sharedBookSourceIds.contains(sourceId))
	    		deleteGroup(groupId, sharedBookIds, groupItemsCursor);
	    } while(sharedGroupsCursor.moveToNext());
	    sharedGroupsCursor.close();
	    groupItemsCursor.close();
	}

	private void deleteGroup(String groupId, Set<String> sharedBookIds, Cursor groupItemsCursor) {
		// delete contacts in group
		Cursor groupCursor = Db.getContactsInGroup(groupId, context.getContentResolver());
		groupCursor.moveToFirst();
	    if(groupCursor.getCount() > 0)
	    do {
	    	String rawContactId = groupCursor.getString(groupCursor.getColumnIndex(Data.RAW_CONTACT_ID));
	    	Set<String> groupIds = getGroupIds(rawContactId, groupItemsCursor);
	    	Log.i(tag, "Raw contact: "+rawContactId+" is in "+groupIds.size()+" groups");
	    	boolean isInOtherSharedBook = false;
	    	for(String gId : groupIds)
	    	{
	    		if(!gId.equals(groupId)
	    		&& sharedBookIds.contains(gId))
	    		{
	    			Log.i(tag, "Contact is another shared book");
	    			isInOtherSharedBook = true;
	    			break;
	    		}
	    	}
	    	if(isInOtherSharedBook) continue;
	    	Log.i(tag, "Delete the contact");
	    	int numDelete = context.getContentResolver()
	    		.delete(SyncManager.addCallerIsSyncAdapterParameter(RawContacts.CONTENT_URI), 
	    				RawContacts._ID + " = ? ", 
	    				new String[] { rawContactId });
	    	Log.i(tag, "deleted "+numDelete+" contact");
	    			

    		// check if contact is in other shared books	
		    /*values.put(GroupMembership.RAW_CONTACT_ID, rawContactId);
		    values.put(GroupMembership.GROUP_ROW_ID, groupId);
		    values.put(GroupMembership.MIMETYPE, GroupMembership.CONTENT_ITEM_TYPE);
		    */
	    } while(groupCursor.moveToNext());
	    groupCursor.close();
	    
	    int numDelete = context.getContentResolver().delete(
	    	SyncManager.addCallerIsSyncAdapterParameter(ContactsContract.Groups.CONTENT_URI), 
	    	Groups._ID + " = ? ", 
			new String[] { groupId} );
	    Log.i(tag, "Deleted "+numDelete+" group for id: "+groupId);

	}

	private Set<String> getGroupIds(String rawContactId, Cursor groupItemsCursor) {
		Set<String> groupIds = new HashSet<String>();
		
		groupItemsCursor.moveToFirst();
	    if(groupItemsCursor.getCount() > 0)
	    do {
	    	String rawCId = groupItemsCursor.getString(groupItemsCursor.getColumnIndex(GroupMembership.RAW_CONTACT_ID));
	    	if(Long.parseLong(rawCId) < Long.parseLong(rawContactId))
	    		continue;
	    	if(Long.parseLong(rawCId) > Long.parseLong(rawContactId))
	    		break;
	    	String gId = groupItemsCursor.getString(groupItemsCursor.getColumnIndex(GroupMembership.GROUP_ROW_ID));	    	
	    	groupIds.add(gId);
    		// check if contact is in other shared books	
		    /*values.put(GroupMembership.RAW_CONTACT_ID, rawContactId);
		    values.put(GroupMembership.GROUP_ROW_ID, groupId);
		    values.put(GroupMembership.MIMETYPE, GroupMembership.CONTENT_ITEM_TYPE);
		    */
	    } while(groupItemsCursor.moveToNext());
		
		return groupIds;
	}

	private void notify(List<Contact> contacts, List<Group> groups,
			List<Group> sharedBooks) {
		if(contacts.size() == 0 
		&& groups.size() == 0 
		&& sharedBooks.size() == 0)
			return;
		
		StringBuffer note = new StringBuffer("");
		if(newOnly)
			note.append("Updated ");
		else
			note.append("Synced ");
		if(contacts.size() > 0)
			note.append(contacts.size()+" contacts");
		if(groups.size() > 0)
		{
			if(contacts.size() > 0)
				note.append(", ");
			note.append(groups.size()+" groups");
		}
		if(sharedBooks.size() > 0)
		{
			if(contacts.size() > 0 || groups.size() > 0)
				note.append(", ");
			note.append(sharedBooks.size()+" shared books");
		}
		Notify.show("Phonebook: "+accountName, note.toString(), "Phonebook update", context);
	}

	private List<Group> getSharedBooks() throws ClientProtocolException, JSONException, IOException {
        List<NameValuePair> params = getAuthParams();
        if(lastUpdated != null)
        	params.add(new BasicNameValuePair(Constants.ACCOUNT_LAST_UPDATED, lastUpdated));
        final JSONArray sharedBooks = new JSONArray(post(GET_SHARED_BOOK_UPDATES_URI, params));
        final List<Group> books = new ArrayList<Group>();
        for (int i = 0; i < sharedBooks.length(); i++)  
            books.add(Group.valueOf(sharedBooks.getJSONObject(i)));
        return books;
	}

	Object[] getContactUpdates() throws JSONException, ParseException, IOException, AuthenticationException 
    {
    	final List<Contact> contactsList = new ArrayList<Contact>();
        final List<Group> groupsList = new ArrayList<Group>();
        List<NameValuePair> params = getAuthParams();
        if(lastUpdated != null)
        	params.add(new BasicNameValuePair(Constants.ACCOUNT_LAST_UPDATED, lastUpdated));
        final JSONArray update = new JSONArray(post(GET_CONTACT_UPDATES_URI, params)),
        	contacts = update.getJSONArray(0),
        	groups = update.getJSONArray(1);
        	
        for (int i = 0; i < contacts.length(); i++) 
            contactsList.add(Contact.valueOf(contacts.getJSONObject(i)));
        
        Log.i(tag, "Contacts: "+contactsList);
        for (int i = 0; i < groups.length(); i++) 
            groupsList.add(Group.valueOf(groups.getJSONObject(i)));
        
        return new Object[] {contactsList, groupsList}; 
    }

	public void sendUpdates() throws ClientProtocolException, IOException, JSONException {
		List<PhoneContact> contacts = db.getContacts(newOnly);
		sendContactUpdates(contacts);
        sendGroupUpdates(db.getGroups(newOnly));
        sendSharedBookUpdates(db.getSharingBooks(newOnly));
        sendLinkUpdates();
        uploadContactPictures();
        if(contacts.size() > 0)
        	ContactManager.resetDirtyContacts(context);
	}

	private void sendLinkUpdates() throws ClientProtocolException, JSONException, IOException {
		Cursor serverDataCursor = db.getProfileData();
		Map<String, Set<String>> linkedContacts = db.getLinkedContacts(),
			storedLinkedContacts = db.getStoredLinkedContacts(); 
		
		if(!newOnly 
		|| (storedLinkedContacts.size() == 0 && linkedContacts.size() != 0)) // first time
		{	// send all the links
			Object[] linkedContactsArray = db.getLinkedContacts().values().toArray();
			Integer numLinks = new Integer(linkedContactsArray.length);
			List<NameValuePair> params = getAuthParams();
			params.add(new BasicNameValuePair("numLinks", numLinks.toString()));
			for(int i=0; i< numLinks.intValue(); i++)
			{
				Log.i(tag, "Obj: "+linkedContactsArray[i]);
				Object[] rawContactIds = ((Set<String>) linkedContactsArray[i]).toArray();
				Log.i(tag, "num raw contacts: "+rawContactIds.length);
				int numContacts = 0;
				for(int j=0; j< rawContactIds.length; j++)
				{
					String serverId = getServerId(rawContactIds[j].toString(), serverDataCursor); 
					Log.i(tag, "Raw #"+j+": "+rawContactIds[j]+", serverId: "+serverId);
					if(serverId != null)
						params.add(new BasicNameValuePair("link_"+i+"_"+numContacts++, 
							serverId));
				}
				params.add(new BasicNameValuePair("link_"+i+"_count", 
						new Integer(numContacts).toString()));
				
				
			}
			serverDataCursor.close();
			JSONArray response = new JSONArray(post(SEND_LINK_UPDATES_URI, params));
			
			// delete old data and persist these links
			db.storeLinkedContacts(linkedContacts);
			return;
		}
		// send only the changed links
		Map<String, Set<String>> deletedLinks = new HashMap<String, Set<String>>(), 
			newLinks = new HashMap<String, Set<String>>(); 
		Set<String> linkedKeys = linkedContacts.keySet(),
		storedKeys = storedLinkedContacts.keySet();
		// compare the two sets of contacts
		if(linkedKeys.equals(storedKeys))
		{
			Log.i(tag, "No links were added or deleted");
			for(String k : linkedKeys)
			{
				if(!linkedContacts.get(k).equals(storedLinkedContacts.get(k)))
				{
					Log.i(tag, "Contacts changed for: "+k);
					deletedLinks.put(k, storedLinkedContacts.get(k));
					newLinks.put(k, linkedContacts.get(k));
				}
			}
		}
		else 
		{
			for(String k : linkedKeys)
			{
				if(!storedKeys.contains(k))
				{
					Log.i(tag, "Added link: "+k);
					newLinks.put(k, linkedContacts.get(k));
				}
			}
	
			for(String k : storedKeys)
			{
				if(!linkedKeys.contains(k))
				{
					Log.i(tag, "Deleted link: "+k);
					deletedLinks.put(k, storedLinkedContacts.get(k));
				}
			}
		}
		sendChangedLinkUpdates(newLinks, deletedLinks, serverDataCursor);
		db.storeLinkedContacts(linkedContacts);
	}

	private void sendChangedLinkUpdates(Map<String, Set<String>> newLinks,
			Map<String, Set<String>> deletedLinks, Cursor serverDataCursor) throws ClientProtocolException, JSONException, IOException {
		if(newLinks.size() == 0 && deletedLinks.size() == 0)
		{
			Log.i(tag, "No changed links");
			return;
		}
		
		List<NameValuePair> params = getAuthParams();
		if(newLinks.size() > 0)
		{
			Object[] linkedContactsArray = newLinks.values().toArray();
			params.add(new BasicNameValuePair("numAddLinks", new Integer(newLinks.size()).toString()));
			for(int i=0; i< newLinks.size(); i++)
			{
				Log.i(tag, "Obj: "+linkedContactsArray[i]);
				Object[] rawContactIds = ((Set<String>) linkedContactsArray[i]).toArray();
				Log.i(tag, "num raw contacts: "+rawContactIds.length);
				int numContacts = 0;
				for(int j=0; j< rawContactIds.length; j++)
				{
					String serverId = getServerId(rawContactIds[j].toString(), serverDataCursor); 
					Log.i(tag, "Add raw #"+j+": "+rawContactIds[j]+", serverId: "+serverId);
					if(serverId != null)
						params.add(new BasicNameValuePair("add_link_"+i+"_"+numContacts++, 
							serverId));
				}
				params.add(new BasicNameValuePair("add_link_"+i+"_count", 
						new Integer(numContacts).toString()));
			}
		}
		
		if(deletedLinks.size() > 0)
		{
			Object[] linkedContactsArray = deletedLinks.values().toArray();
			params.add(new BasicNameValuePair("numDeletedLinks", new Integer(deletedLinks.size()).toString()));
			for(int i=0; i< deletedLinks.size(); i++)
			{
				Log.i(tag, "Obj: "+linkedContactsArray[i]);
				Object[] rawContactIds = ((Set<String>) linkedContactsArray[i]).toArray();
				Log.i(tag, "num raw contacts: "+rawContactIds.length);
				int numContacts = 0;
				for(int j=0; j< rawContactIds.length; j++)
				{
					String serverId = getServerId(rawContactIds[j].toString(), serverDataCursor); 
					Log.i(tag, "Delete raw #"+j+": "+rawContactIds[j]+", serverId: "+serverId);
					if(serverId != null)
						params.add(new BasicNameValuePair("del_link_"+i+"_"+numContacts++, 
							serverId));
				}
				params.add(new BasicNameValuePair("del_link_"+i+"_count", 
						new Integer(numContacts).toString()));
			}
		}
		JSONArray response = new JSONArray(post(SEND_CHANGED_LINK_UPDATES_URI, params));
	}
	
	private String getServerId(String rawContactId, Cursor serverDataCursor) {
		serverDataCursor.moveToFirst();
		if(serverDataCursor.getCount() > 0)
		do {
			String mimeType = serverDataCursor.getString(serverDataCursor.getColumnIndex(Data.MIMETYPE)),
				serverId = serverDataCursor.getString(serverDataCursor.getColumnIndex(PhonebookSyncAdapterColumns.DATA_PID)),
				rawId = serverDataCursor.getString(serverDataCursor.getColumnIndex(Data.RAW_CONTACT_ID));
			if(!mimeType.equals(PhonebookSyncAdapterColumns.MIME_PROFILE)
			|| !rawId.equals(rawContactId))
				continue;
			Log.i(tag, "getServerId("+rawContactId+") = "+serverId);
			return serverId;
		} while(serverDataCursor.moveToNext()); 
		return null;
	}

	public String getTimestamp() throws ClientProtocolException, JSONException, IOException {
        JSONArray response = new JSONArray(post(TIMESTAMP_URI, getAuthParams()));
        Long timestamp = response.getLong(0);
        return timestamp.toString();
	}

	private void sendContactUpdates(List<PhoneContact> contacts) throws ClientProtocolException, IOException, JSONException {
        int batchSize = 30;
        Cursor serverDataCursor = Db.getContactServerData(context);
		for(int b = 0; b < contacts.size();  b = b + batchSize)
		{
			Log.i(tag, "Sending batch #"+b);
			int numContacts = 0;
	        List<NameValuePair> params = getAuthParams();
			for(int i = b; i < b + batchSize && i < contacts.size(); i++)
			{
				//System.out.print(i+", ");
	        	PhoneContact contact =  contacts.get(i);
	        	if(contact.serverId != null // this server id has already been synced in the pull 
	        	&& syncedContactServerIds.contains(contact.serverId))
	        		continue; 
	        	contact.addParams(params, new Integer(numContacts).toString());
	        	numContacts++;
			}
			Log.i(tag, "num contacts: "+numContacts);
	        params.add(new BasicNameValuePair("numContacts", new Integer(numContacts).toString()));
	        if(lastUpdated != null)
	        	params.add(new BasicNameValuePair(Constants.ACCOUNT_LAST_UPDATED, lastUpdated));
			
	        JSONArray contactUpdates = new JSONArray(post(SEND_CONTACT_UPDATES_URI, params));
	        updateServerData(contactUpdates, serverDataCursor);
		}

        
        /*for(int i=0; i< contacts.size(); i++)
        {
        	PhoneContact contact =  contacts.get(i);
        	if(contact.serverId != null // this server id has already been synced in the pull 
        	&& syncedContactServerIds.contains(contact.serverId))
        		continue; 
        	contact.addParams(params, new Integer(numContacts).toString());
        	numContacts++;
        }
        
        params.add(new BasicNameValuePair("numContacts", new Integer(numContacts).toString()));
        if(lastUpdated != null)
        	params.add(new BasicNameValuePair(Constants.ACCOUNT_LAST_UPDATED, lastUpdated));
		
        JSONArray contactUpdates = new JSONArray(post(SEND_CONTACT_UPDATES_URI, params));
        updateServerData(contactUpdates);*/
	}
	
	private void updateServerData(JSONArray contactUpdates, Cursor serverDataCursor) throws JSONException {
        final BatchOperation batchOperation = new BatchOperation(context);
        for (int i = 0; i < contactUpdates.length(); i++)
        {
        	ContactManager.updateContact(contactUpdates.getJSONObject(i), context, serverDataCursor, batchOperation);
        	if(batchOperation.size() > 50)
        		batchOperation.execute();
        }
        batchOperation.execute();
	}

	private List<PicData> getServerPicData() throws ClientProtocolException, JSONException, IOException {
		List<PicData> picData = new ArrayList<PicData>();
		JSONArray picsJson = new JSONArray(post(GET_PIC_DATA_URI, getAuthParams()));
		for(int i=0; i< picsJson.length(); i++)
		{
			JSONArray picJson = picsJson.getJSONArray(i);
			picData.add(new PicData(picJson.getLong(0), picJson.getLong(1), picJson.getLong(2)));
		}
		return picData;
	}

	void uploadContactPictures() throws ClientProtocolException, JSONException, IOException {
		Map<String, String> params = new HashMap<String, String>();
		params.put("upload", "avatar");
		params.put("errorAction", "error");
		params.put("errorController", "file");
		params.put("successAction", "success");
		params.put("successController", "file");
		Cursor rawContactsCursor = db.getRawContactsCursor(newOnly);
		Log.i(tag, "There are "+rawContactsCursor.getCount()+" raw contacts entries");
		if(rawContactsCursor.getCount() == 0) return;
		serverPicData = getServerPicData();
	    Cursor dataCursor = db.getProfileData(),
	    	photosDataCursor = context.getContentResolver().query(ContactsContract.Data.CONTENT_URI,
	    		// null,
	    	    new String[] {
	    			Contacts._ID, 
	    			Data.CONTACT_ID,
	    			Data.RAW_CONTACT_ID,
	    			RawContacts._ID,
	    			Contacts.DISPLAY_NAME,
	    			Photo.PHOTO,
	    			Data.MIMETYPE, Data.DATA1,
	    		},
	    		ContactsContract.CommonDataKinds.Photo.PHOTO +" is not null ",
	    		null,
	    		// +"and "+Data.MIMETYPE+" = ? ",
	    	    // new String[] {ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE}, 
	    	    ContactsContract.Data.CONTACT_ID);
	    
	    Log.i(tag, "There are "+photosDataCursor.getCount()+" photo data entries");
	    
	    photosDataCursor.moveToFirst();
	    rawContactsCursor.moveToFirst();
	    do {
	    	
	    	String rawContactId = rawContactsCursor.getString(rawContactsCursor.getColumnIndex(ContactsContract.RawContacts._ID));
	    	if(unchangedPicsRawContactIds.contains(rawContactId))
	    	{
	    		Log.i(tag, "Unchanged pic");
	    		continue;
	    	}
	    	ServerData data = Db.getServerDataFromContactId(dataCursor, rawContactId);
	    	if(data == null) {
	    		Log.i(tag, "Server data is null for contactId: "+rawContactId);
	    		continue;
	    	}
	    	String serverId = data.serverId,
	    		picId = data.picId, 
	    		picSize = data.picSize,
	    		picHash = data.picHash;
	    	
	    	ContactPicture pic = null;
			try {
				pic = getContactPicture(photosDataCursor, rawContactId);
				if(pic == null) continue;
				String hash = ImageInfo.hash(pic.pic);
		        Log.i(tag, "picId: "+picId);//+", hash: " + hash);

				if(picId != null && picSize != null) {
					int pSize = Integer.parseInt(picSize);
					if(pSize == pic.pic.length 
					&& picHash != null && hash.equals(picHash)
					&& isServerPicData(serverId, picId, serverPicData))
					{
						Log.i(tag, "Same image not uploading");
						continue;
					}
				}
	    		String contentType = pic.mimeType.split("/")[1];
	    		Log.i(tag, "uploading "+contentType);
	    		params.remove("id");
	    		params.put("id", serverId);
	    		JSONObject response = upload(UPLOAD_CONTACT_PIC_URI, pic.pic, contentType, params);
	    		if(response != null)
	    		{
	    			try {
						int status = response.getInt("ok");
						if(status == 1)
						{
							String pId = response.getString("id");
								// pSize = response.getString("size"),
								// hash = response.getString("hash");
							updateContactPicData(rawContactId, serverId, pId, new Long(pic.pic.length).toString(), hash);
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}
	    		}
			} catch (IOException e) {
				e.printStackTrace();
			}
	    } while(rawContactsCursor.moveToNext());
	    rawContactsCursor.close();
	    dataCursor.close();
	    photosDataCursor.close();
	}

	private boolean isServerPicData(String serverId, String picId,
			List<PicData> serverPicData) {
		for(PicData p : serverPicData)
		{
			if(p.serverId.equals(serverId))
			{
				if(p.picId.equals(picId))
				{
					Log.i(tag, "Pic is same on server");
					return true;
				}
				else return false;
			}
		}
		Log.i(tag, "No pic on server");
		return false;
	}

	private void updateContactPicData(String rawContactId, String serverId, String picId, String picSize, String hash) {
		Uri uri = Data.CONTENT_URI;
		ContentValues values = new ContentValues();
		values.put(PhonebookSyncAdapterColumns.PIC_ID, picId);
		values.put(PhonebookSyncAdapterColumns.PIC_SIZE, picSize);
		values.put(PhonebookSyncAdapterColumns.PIC_HASH, hash);
		context.getContentResolver().update(uri, values, 
				Data.RAW_CONTACT_ID + " = " + rawContactId + " and " +
				PhonebookSyncAdapterColumns.DATA_PID + " = " + serverId + " and " +
				Data.MIMETYPE + " = '" + PhonebookSyncAdapterColumns.MIME_PROFILE + "'", null);
	}

	private ContactPicture getContactPicture(Cursor dataCursor, String rawContactId) throws IOException {
		if(dataCursor.getCount() == 0) return null;
		dataCursor.moveToFirst();
	    do {
	    	String mimetype = dataCursor.getString(dataCursor.getColumnIndex(Data.MIMETYPE));
	    	if(!mimetype.equals(Photo.CONTENT_ITEM_TYPE)) continue;
	    	String rawId = dataCursor.getString(dataCursor.getColumnIndex(Data.RAW_CONTACT_ID));
	    	if(!rawId.equals(rawContactId)) continue;
	    	String name = dataCursor.getString(dataCursor.getColumnIndex(Contacts.DISPLAY_NAME));
	    	byte[] pic = dataCursor.getBlob(dataCursor.getColumnIndex(Photo.PHOTO));
	    	String contentType = new ImageInfo(pic).getMimeType(); 
	    		// dataCursor.getString(dataCursor.getColumnIndex(ContactsContract.CommonDataKinds.Photo.MIMETYPE));
	    	// String serverId
	    	Log.i(tag, "Contact["+rawContactId+"] "+name+", pic: "+(pic == null ? "null" : pic.length+", content type: "+contentType));
	    	return new ContactPicture(pic, contentType);
	    } while(dataCursor.moveToNext());
	    return null;
	}
	
	private void sendGroupUpdates(List<Group> groups) throws ClientProtocolException, IOException, JSONException {
        List<NameValuePair> params = getAuthParams();
        int numGroups = 0;
        
        for(int i=0; i< groups.size(); i++)
        {
        	Group group =  groups.get(i);
        	if(group.serverId != null 
        	&& syncedGroupServerIds.contains(group.serverId))
        	{
        		Log.i(tag, "Already synced group "+group.name+"["+group.serverId+"]");
        		continue;
        	}
        	String index = new Integer(numGroups).toString();
        	
        	params.add(new BasicNameValuePair("groupId_"+index, group.groupId));
        	params.add(new BasicNameValuePair("serverId_"+index, group.serverId));
        	params.add(new BasicNameValuePair("bookName_"+index, group.name));
        	List<Contact> bookContacts = group.contacts;
        	params.add(new BasicNameValuePair("numContacts_"+index, new Integer(bookContacts.size()).toString()));
        	for(int j=0; j< bookContacts.size(); j++)
        	{
        		Contact bContact = bookContacts.get(j);
        		String cIndex = new Integer(j).toString();
        		params.add(new BasicNameValuePair("serverId_"+index+"_"+cIndex, bContact.serverId));
        	}
        	numGroups++;
        }
        params.add(new BasicNameValuePair("numBooks", new Integer(numGroups).toString()));
        JSONArray groupUpdates = new JSONArray(post(SEND_GROUP_UPDATES_URI, params));
        for (int i = 0; i < groupUpdates.length(); i++)
        	ContactManager.updateGroup(groupUpdates.getJSONObject(i), context);
        if(groups.size() > 0)
        	ContactManager.resetDirtyGroups(context);

	}
	
	private void sendSharedBookUpdates(List<SharingBook> books) throws ClientProtocolException, IOException, JSONException {
        List<NameValuePair> params = getAuthParams();
        params.add(new BasicNameValuePair("numShareBooks", new Integer(books.size()).toString()));
        for(int i=0; i< books.size(); i++)
        {
        	String index = new Integer(i).toString();
        	SharingBook book =  books.get(i);
        	params.add(new BasicNameValuePair("shareBookId_"+index, book.groupId));
        	params.add(new BasicNameValuePair("shareContactId_"+index, book.contactId));
        	if(book.deleted)
        		params.add(new BasicNameValuePair("shareContactDeleted_"+index, "true"));
        	Log.i(tag, "Shared book["+book.groupId+"] deleted: "+book.deleted);
        }
        JSONArray bookUpdates = new JSONArray(post(SEND_SHARED_BOOK_UPDATES_URI, params));
        for (int i = 0; i < bookUpdates.length(); i++)
        {
        	//Long deletedContactId = bookUpdates.getLong(i);
        	// Log.i(tag, "Deleted contactId: ")
        	// ContactManager.updateBook(bookUpdates.getJSONObject(i), context);
        }
        if(books.size() > 0)
        	ContactManager.resetDirtySharedBooks(context);
	}
	
	public String post(String url, List<NameValuePair> params) throws ClientProtocolException, IOException, JSONException {
        HttpEntity entity = new UrlEncodedFormEntity(params);
        final HttpPost post = new HttpPost(url);
        Log.i(tag, "Sending to: "+url);
        post.addHeader(entity.getContentType());
        post.setEntity(entity);
        maybeCreateHttpClient();
        final HttpResponse resp = httpClient.execute(post);
        final String response = EntityUtils.toString(resp.getEntity());
        Log.i(tag, "Response is: "+response);
        return response;
	}
	
    /**
     * Configures the httpClient to connect to the URL provided.
     */
    void maybeCreateHttpClient() {
        if (httpClient == null) {
            httpClient = new DefaultHttpClient();
            final HttpParams params = httpClient.getParams();
            HttpConnectionParams.setConnectionTimeout(params, REGISTRATION_TIMEOUT);
            HttpConnectionParams.setSoTimeout(params, REGISTRATION_TIMEOUT);
            ConnManagerParams.setTimeout(params, REGISTRATION_TIMEOUT);
        }
    }
	
	private List<NameValuePair> getAuthParams() {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(PARAM_USERNAME, accountName));
        params.add(new BasicNameValuePair(PARAM_PASSWORD, authToken));
        /*if (lastUpdated != null) {
        final SimpleDateFormat formatter =
            new SimpleDateFormat("yyyy/MM/dd HH:mm");
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        params.add(new BasicNameValuePair(PARAM_UPDATED, formatter
            .format(lastUpdated)));
    }
    Log.i(TAG, params.toString());*/
        
        return params;
	}
	
	JSONObject upload(String uploadUrl, byte[] data, String contentType, Map<String, String> params) {
		Log.i(tag, "Uploading to "+uploadUrl);
		HttpURLConnection connection = null;
		DataOutputStream outputStream = null;

		// String pathToOurFile = "/tmp/avatar/1310118336631/card_errors.gif";
		// String uploadUrl = "http://10.9.8.29:8080/phonebook/fileUploader/process";
		String lineEnd = "\r\n";
		String twoHyphens = "--";
		String boundary =  "*****";

		try
		{
		URL url = new URL(uploadUrl);
		connection = (HttpURLConnection) url.openConnection();

		// Allow Inputs & Outputs
		// connection.setDoInput(true);
		connection.setDoOutput(true);
		// connection.setUseCaches(false);

		// Enable POST method
		connection.setRequestMethod("POST");

		connection.setRequestProperty("Connection", "Keep-Alive");
		connection.setRequestProperty("Content-Type", "multipart/form-data;boundary="+boundary);
		
		//connection.setInstanceFollowRedirects(true);
		// HttpURLConnection.setFollowRedirects(true);
		Log.i(tag, "Follow redirects: "+connection.getInstanceFollowRedirects());

		outputStream = new DataOutputStream( connection.getOutputStream() );
		for (Map.Entry<String, String> entry : params.entrySet()) {
		    String name = entry.getKey();
		    String value = entry.getValue();
		    Log.i(tag, "param: "+name+", value: "+value);
		    if(value == null) continue;
		    outputStream.writeBytes(twoHyphens + boundary + lineEnd);
		    outputStream.writeBytes("Content-Disposition: form-data; name=\""+name+"\"" + lineEnd);
		    outputStream.writeBytes(lineEnd);
		    outputStream.writeBytes(value);
		    outputStream.writeBytes(lineEnd);
		}

		outputStream.writeBytes(twoHyphens + boundary + lineEnd);
		// outputStream.writeBytes("Content-Disposition: form-data; name=\"file\";filename=\"" + pathToOurFile +"\"" + lineEnd);
		outputStream.writeBytes("Content-Disposition: form-data; name=\"file\";filename=\"" + "image."+contentType +"\"" + lineEnd);
		outputStream.writeBytes(lineEnd);

		outputStream.write(data, 0, data.length);

		outputStream.writeBytes(lineEnd);
		outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

		// Responses from the server (code and message)
		int serverResponseCode = connection.getResponseCode();
		String serverResponseMessage = connection.getResponseMessage();
		String location = connection.getHeaderField("Location");
		Log.i(tag, "response code: "+serverResponseCode+", message: "+serverResponseMessage+", location: "+location);
		if(serverResponseCode == 302)
		{
			url = new URL(location);
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.connect();
		}
		
		InputStream in = new BufferedInputStream(connection.getInputStream(), 8*1024);
		String response = convertStreamToString(in);
		Log.i(tag, "Response: "+response);
		outputStream.flush();
		outputStream.close();
		return new JSONObject(response);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}		
		return null;
	}
	
    String convertStreamToString(InputStream is) throws IOException {
	/*
	 * To convert the InputStream to String we use the
	 * Reader.read(char[] buffer) method. We iterate until the
	 * Reader return -1 which means there's no more data to
	 * read. We use the StringWriter class to produce the string.
	 */
		if (is != null) {
		    Writer writer = new StringWriter();
		
		    char[] buffer = new char[1024];
		    try {
		        Reader reader = new BufferedReader(
		                new InputStreamReader(is, "UTF-8"));
		        int n;
		        while ((n = reader.read(buffer)) != -1) {
		            writer.write(buffer, 0, n);
		        }
		    } finally {
		        is.close();
		    }
		    return writer.toString();
		} else {        
		    return "";
		}
    }
}