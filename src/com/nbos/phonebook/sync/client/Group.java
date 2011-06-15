package com.nbos.phonebook.sync.client;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class Group {
	String name;
	int groupId;
	List<Contact> contacts;
	public Group(int groupId, String name, List<Contact> contacts) {
		super();
		this.groupId = groupId;
		this.name = name;
		this.contacts = contacts;
	}
	public int getGroupId() {
		return groupId;
	}
	public void setGroupId(int groupId) {
		this.groupId = groupId;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public List<Contact> getContacts() {
		return contacts;
	}
	public void setContacts(List<Contact> contacts) {
		this.contacts = contacts;
	}
	
	static String tag = "Group";
	public static Group valueOf(JSONObject group) throws JSONException {
		int id = group.getInt("id");
		String name = group.getString("name");
		Log.i(tag, "Id: "+id+", name: "+name);
		List<Contact> contacts = new ArrayList<Contact>();
		JSONArray contactsArray = group.getJSONArray("contacts");
		for(int i=0; i< contactsArray.length(); i++)
		{
			JSONObject c = contactsArray.getJSONObject(i);
			int cid = c.getInt("id");
			String cName = c.getString("name"),
				number = c.getString("number");
			contacts.add(new Contact(cid, number, cName));
		}
		Log.i(tag, "There are "+contacts.size()+" contacts in group "+name);
		return new Group(id, name, contacts);
	}
	
}
