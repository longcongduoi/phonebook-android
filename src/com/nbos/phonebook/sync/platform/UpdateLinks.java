package com.nbos.phonebook.sync.platform;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;

import com.nbos.phonebook.contentprovider.Provider;
import com.nbos.phonebook.database.tables.ContactTable;
import com.nbos.phonebook.sync.Constants;

public class UpdateLinks {
	static String tag = "UpdateLinks";
	Cloud cloud;
	UpdateLinks(Cloud cloud) throws ClientProtocolException, JSONException, IOException {
		this.cloud = cloud;
		sendLinkUpdates();
	}

	private void sendLinkUpdates() throws ClientProtocolException, JSONException, IOException {
		// Cursor serverDataCursor = syncManager.serverDataCursor;
		Map<String, Set<String>> linkedContacts = getLinkedContacts(),
			storedLinkedContacts = getStoredLinkedContacts(); 
		cloud.getServerDataIds();
		if(!cloud.newOnly 
		|| (storedLinkedContacts.size() == 0 && linkedContacts.size() != 0)) // first time
		{	// send all the links
			Object[] linkedContactsArray = linkedContacts.values().toArray();
			Integer numLinks = 0; // new Integer(linkedContactsArray.length);
			Log.i(tag ,"numLinks: "+numLinks);
			List<NameValuePair> params = cloud.getAuthParams();
			for(int i=0; i< linkedContacts.size(); i++)
			{
				Log.i(tag, "Obj: "+linkedContactsArray[i]+"numLinks: "+numLinks.intValue());
				
				Object[] rawContactIds = ((Set<String>) linkedContactsArray[i]).toArray();
				Log.i(tag, "num raw contacts: "+rawContactIds.length);
				Set<String> serverIds = new HashSet<String>();
				for(int j=0; j< rawContactIds.length; j++)
				{
					String serverId = cloud.getServerId(rawContactIds[j].toString()); 
					Log.i(tag, "Raw #"+j+": "+rawContactIds[j]+", serverId: "+serverId);
					if(serverId != null)
						serverIds.add(serverId);
				}
				int numContacts = 0;
				if(serverIds.size() > 1)
				{
					numLinks++;
					for(String serverId : serverIds)
						params.add(new BasicNameValuePair("link_"+i+"_"+numContacts++, serverId));
				}
					params.add(new BasicNameValuePair("link_"+i+"_count", 
						new Integer(numContacts).toString()));
				
			}
			Log.i(tag, "Uploading "+numLinks+" links");
			params.add(new BasicNameValuePair("numLinks", numLinks.toString()));
			Log.i(tag," numLinks: "+numLinks);
			if(numLinks > 0)
				new JSONArray(cloud.post(Cloud.SEND_LINK_UPDATES_URI, params));
			// delete old data and persist these links
			storeLinkedContacts(linkedContacts);
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
		sendChangedLinkUpdates(newLinks, deletedLinks);
		storeLinkedContacts(linkedContacts);
	}

	private void sendChangedLinkUpdates(Map<String, Set<String>> newLinks,
			Map<String, Set<String>> deletedLinks) throws ClientProtocolException, JSONException, IOException {
		if(newLinks.size() == 0 && deletedLinks.size() == 0)
		{
			Log.i(tag, "No changed links");
			return;
		}
		
		List<NameValuePair> params = cloud.getAuthParams();
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
					String serverId = cloud.getServerId(rawContactIds[j].toString()); 
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
				@SuppressWarnings("unchecked")
				Object[] rawContactIds = ((Set<String>) linkedContactsArray[i]).toArray();
				Log.i(tag, "num raw contacts: "+rawContactIds.length);
				int numContacts = 0;
				for(int j=0; j< rawContactIds.length; j++)
				{
					String serverId = cloud.getServerId(rawContactIds[j].toString()); 
					Log.i(tag, "Delete raw #"+j+": "+rawContactIds[j]+", serverId: "+serverId);
					if(serverId != null)
						params.add(new BasicNameValuePair("del_link_"+i+"_"+numContacts++, 
							serverId));
				}
				params.add(new BasicNameValuePair("del_link_"+i+"_count", 
						new Integer(numContacts).toString()));
			}
		}
		JSONArray response = new JSONArray(cloud.post(Cloud.SEND_CHANGED_LINK_UPDATES_URI, params));
	}

	static Map<String, Set<String>> getLinkedContacts(Cursor c, String contactIdColumn, String rawContactIdColumn) {
		Map<String, Set<String>> linkedContacts = new HashMap<String, Set<String>>();
		Log.i(tag, "There are "+c.getCount()+" rows");
		if(c.getCount() == 0)
			return linkedContacts;
		c.moveToFirst();
		String prevContactId = null, contactId;
		Set<String> rawContactIds = new HashSet<String>();
		do {
			contactId = c.getString(c.getColumnIndex(contactIdColumn));
			String rawContactId = c.getString(c.getColumnIndex(rawContactIdColumn));
			// Log.i(tag, "c: "+contactId+", raw: "+rawContactId);
			if(contactId == null) continue;
			if(prevContactId == null) prevContactId = contactId;
			if(!prevContactId.equals(contactId))
			{
				if(rawContactIds.size() > 1)
					linkedContacts.put(prevContactId, rawContactIds);
				rawContactIds = new HashSet<String>();
				prevContactId = contactId;
			}
			rawContactIds.add(rawContactId);
		} while(c.moveToNext());
		
		if(rawContactIds.size() > 1)
			linkedContacts.put(prevContactId, rawContactIds);
		
		Log.i(tag, "There are "+linkedContacts.size()+" linked contacts");
		for(String ct : linkedContacts.keySet())
		{
			Log.i(tag, "Contact: "+ct);
			for(String r : linkedContacts.get(ct))
			{
				Log.i(tag, "Raw contact: "+r);
			}
		}
		
		return linkedContacts;
		
	}

	public Map<String, Set<String>> getLinkedContacts() {
		Cursor c = cloud.cr
			.query(RawContacts.CONTENT_URI,  
				new String[]{
					RawContacts._ID,
					RawContacts.CONTACT_ID
				}, null, null, 
				RawContacts.CONTACT_ID);
		Map<String, Set<String>> linkedContacts = getLinkedContacts(c, RawContacts.CONTACT_ID, RawContacts._ID);
		c.close();
		return linkedContacts;
	}

	public Map<String, Set<String>> getStoredLinkedContacts() {
		Cursor c = cloud.cr.query(Constants.CONTACT_URI,
	    		null, null,null, 
	    		ContactTable.CONTACTID);
		Log.i(tag, "There are "+c.getCount()+" stored contact entries");
		Map<String, Set<String>> storedLinkedContacts = getLinkedContacts(c, ContactTable.CONTACTID, ContactTable.RAWCONTACTID);
		c.close();
		return storedLinkedContacts;
	}
	
	public void storeLinkedContacts(Map<String, Set<String>> linkedContacts) {
		BatchOperation batchOperation = new BatchOperation(cloud.context, Provider.AUTHORITY);
		int num = cloud.cr.delete(Constants.CONTACT_URI, null, null);
		Log.i(tag, "Deleted "+num+" links");
		for(String ct : linkedContacts.keySet())
		{
			Log.i(tag, "Contact: "+ct);
			for(String r : linkedContacts.get(ct))
			{
				Log.i(tag, "Raw contact: "+r);
				ContentProviderOperation.Builder builder = 
					ContentProviderOperation.newInsert(Constants.CONTACT_URI)
			            .withYieldAllowed(true);
				
				// insert into contact table
		        ContentValues bookValues = new ContentValues();
		        bookValues.put(ContactTable.CONTACTID, ct);
		        bookValues.put(ContactTable.RAWCONTACTID, r);
		        builder.withValues(bookValues);
		        batchOperation.add(builder.build());
		        // Uri cUri = cloud.cr.insert(Constants.CONTACT_URI, bookValues);
		        // Log.i(tag, "inserted: "+cUri);
			}
		}
		Log.i(tag, "Executing contact table batch insert: "+batchOperation.size());
		batchOperation.execute();
	}	
}
