package com.nbos.phonebook.sync.client;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.database.Cursor;
import android.provider.ContactsContract.CommonDataKinds;
import android.util.Log;

import com.nbos.phonebook.sync.client.contact.Address;
import com.nbos.phonebook.sync.client.contact.Email;
import com.nbos.phonebook.sync.client.contact.Im;
import com.nbos.phonebook.sync.client.contact.Name;
import com.nbos.phonebook.sync.client.contact.Organization;
import com.nbos.phonebook.sync.client.contact.Phone;
import com.nbos.phonebook.sync.platform.PhonebookSyncAdapterColumns;

public class Contact {
	static String tag = "Contact";
	public String serverId, picId;
		// name, number, email;
	public boolean deleted;
	public Name name = new Name();
	public List<Phone> phones = new ArrayList<Phone>();
	public List<Email> emails = new ArrayList<Email>();
	public List<Im> ims = new ArrayList<Im>();
	public List<Address> addresses = new ArrayList<Address>();
	public List<Organization> orgs = new ArrayList<Organization>();
	public List<String> 
		notes = new ArrayList<String>(),
		nicknames = new ArrayList<String>(),
		websites = new ArrayList<String>();
	
	public Contact() {}
	
	public Contact(String serverId) {
		this.serverId = serverId;
	}

	public void addParams(List<NameValuePair> params, String index) {
		name.addParams(params, index);
		
		if(phones.size() > 0)
			params.add(new BasicNameValuePair("numPhones_"+index, new Integer(phones.size()).toString()));
		int i = 0;
		for(Phone p : phones)
			p.addParams(params, index, i++);
		
		if(emails.size() > 0)
			params.add(new BasicNameValuePair("numEmails_"+index, new Integer(emails.size()).toString()));
		i = 0;
		for(Email e : emails)
			e.addParams(params, index, i++);
		
		if(ims.size() > 0)
			params.add(new BasicNameValuePair("numIms_"+index, new Integer(ims.size()).toString()));
		i = 0;
		for(Im im : ims)
			im.addParams(params, index, i++);
		
		if(addresses.size() > 0)
			params.add(new BasicNameValuePair("numAddresses_"+index, new Integer(addresses.size()).toString()));
		i = 0;
		for(Address a : addresses)
			a.addParams(params, index, i++);

		if(orgs.size() > 0)
			params.add(new BasicNameValuePair("numOrgs_"+index, new Integer(orgs.size()).toString()));
		i = 0;
		for(Organization o : orgs)
			o.addParams(params, index, i++);

		if(notes.size() > 0)
			params.add(new BasicNameValuePair("numNotes_"+index, new Integer(notes.size()).toString()));
		i = 0;
		for(String n : notes)
			params.add(new BasicNameValuePair("note_"+index+"_"+i++, n));

		if(nicknames.size() > 0)
			params.add(new BasicNameValuePair("numNicks_"+index, new Integer(nicknames.size()).toString()));
		i = 0;
		for(String n : nicknames)
			params.add(new BasicNameValuePair("nick_"+index+"_"+i++, n));
		
		if(websites.size() > 0)
			params.add(new BasicNameValuePair("numWebsites_"+index, new Integer(websites.size()).toString()));
		i = 0;
		for(String w : websites)
			params.add(new BasicNameValuePair("website_"+index+"_"+i++, w));
		if(serverId != null && serverId.trim().length() > 0)
			params.add(new BasicNameValuePair("id_"+index, serverId));
	}
	
	public String toString() {
		String str = "\n" + name;
		for(Phone p : phones)
			str += "\n" + p;
		for(Email e : emails)
			str += "\n" + e;
		for(Im i : ims)
			str += "\n" + i;
		for(Address a : addresses)
			str += "\n" + a;
		for(Organization o : orgs)
			str += "\n" + o;
		for(String n : notes)
			str += "\nNote: " + n;
		for(String n : nicknames)
			str += "\nNickname: " + n;
		for(String w : websites)
			str += "\nWebsite: " + w;
		return str;
	}
	
	public static void addNote(Contact contact, Cursor c) {
		String note = c.getString(c.getColumnIndex(CommonDataKinds.Note.NOTE));
		contact.notes.add(note);
	}

	public static void addNickname(Contact contact, Cursor c) {
		String nickname = c.getString(c.getColumnIndex(CommonDataKinds.Nickname.NAME));
		contact.nicknames.add(nickname);
	}

	public static void addWebsite(Contact contact, Cursor c) {
		String website = c.getString(c.getColumnIndex(CommonDataKinds.Website.URL));
		contact.websites.add(website);
	}
	
	static void addContactField(Contact contact, Cursor cursor, String mimeType) {
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

	public static Contact valueOf(JSONObject user) {
        try {
        	Contact c = new Contact();
        	c.serverId = new Integer(user.getInt("id")).toString();
        	c.picId = user.has("pic") ? user.getString("pic") : null;
        	if(user.has("name"))
        	c.name = Name.valueOf(user.getJSONObject("name"));
        	if(user.has("phones") )
        		c.phones = Phone.valueOf(user.getJSONArray("phones"));
        	if(user.has("emails"))
        		c.emails = Email.valueOf(user.getJSONArray("emails"));
        	if(user.has("ims"))
        		c.ims = Im.valueOf(user.getJSONArray("ims"));
        	if(user.has("addresses"))
        		c.addresses = Address.valueOf(user.getJSONArray("addresses"));
        	if(user.has("orgs"))
        		c.orgs = Organization.valueOf(user.getJSONArray("orgs"));
        	if(user.has("notes"))
        		c.notes = valueOfStrings(user.getJSONArray("notes"));
        	if(user.has("nicks"))
        		c.nicknames = valueOfStrings(user.getJSONArray("nicks"));
        	if(user.has("ws") )
        		c.websites = valueOfStrings(user.getJSONArray("ws"));
        	
            /*String name = user.has("name") ? user.getString("name") : null,
            	number = user.has("number") ? user.getString("number") : null,
            	serverId = new Integer(user.getInt("id")).toString(),
            	picId = user.getString("pic");
            Log.i(tag, "name: "+name+", picId is: "+picId);*/
            return c; // new Contact(name, number, serverId, picId); 
        } catch (final Exception ex) {
            Log.i(tag, "Error parsing JSON user object" + ex.toString());
        }
        return null;
    }	
	/*public Contact(String name, String number, String serverId, String picId) {
		this(name, number, serverId);
		this.picId = picId;
	}

	public Contact(String name, String number, String serverId) {
		this.serverId = serverId;
		this.number = number;
		this.name = name;
	}

*/

	private static List<String> valueOfStrings(JSONArray jsonArray) throws JSONException {
		List<String> strings = new ArrayList<String>();
		for(int i=0; i< jsonArray.length(); i++)
			strings.add(jsonArray.getString(i));
		return strings;
		
	}
	
	/*@Override
	public String toString() {
		return name;
	}*/
}
