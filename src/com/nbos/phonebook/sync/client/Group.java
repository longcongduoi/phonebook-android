package com.nbos.phonebook.sync.client;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class Group {
	public String groupId, serverId, name, owner;
	public List<Contact> contacts;
	public Group(String groupId, String serverId, String name, String owner, List<Contact> contacts) {
		super();
		this.groupId = groupId;
		this.serverId = serverId;
		this.name = name;
		this.owner = owner;
		this.contacts = contacts;
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
		{
			JSONObject c = contactsArray.getJSONObject(i);
			int serverId = c.getInt("id");
			String cName = c.getString("name"),
				number = c.getString("number");
			contacts.add(new Contact(cName, number, new Integer(serverId).toString()));
		}
		Log.i(tag, "There are "+contacts.size()+" contacts in group "+name);
		return new Group(new Integer(id).toString(), null, name, owner, contacts);
	}
	
}
