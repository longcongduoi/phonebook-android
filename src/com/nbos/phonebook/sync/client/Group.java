package com.nbos.phonebook.sync.client;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class Group {
	public String groupId, serverId, name, owner;
	public List<Contact> contacts;
	public Set<Long> sharingWithContactIds;
	public Group(String groupId, String serverId, String name, String owner, 
			List<Contact> contacts, Set<Long> sharingWithContactIds) {
		super();
		this.groupId = groupId;
		this.serverId = serverId;
		this.name = name;
		this.owner = owner;
		this.contacts = contacts;
		this.sharingWithContactIds = sharingWithContactIds;
	}
	
	static String tag = "Group";
	public static Group valueOf(JSONObject group) throws JSONException {
		int id = group.getInt("id");
		String name = group.getString("name"),
			owner = group.getString("owner");;
		Log.i(tag, "Id: "+id+", name: "+name+", owner: "+owner);
		
		List<Contact> contacts = new ArrayList<Contact>();
		JSONArray contactsArray = group.getJSONArray("contacts");
		for(int i=0; i< contactsArray.length(); i++)
			contacts.add(Contact.valueOf(contactsArray.getJSONObject(i)));
		Log.i(tag, "There are "+contacts.size()+" contacts in group "+name);
		
		JSONArray sharingWithContactIdsArray = group.getJSONArray("sharingWith");
		Set<Long> sharingWithContactIds = new HashSet<Long>();
		for(int i=0; i< sharingWithContactIdsArray.length(); i++)
			sharingWithContactIds.add(sharingWithContactIdsArray.getLong(i));
		return new Group(new Integer(id).toString(), null, name, owner, contacts, sharingWithContactIds);
	}
	
}
