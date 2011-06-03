package com.nbos.phonebook.sync.client;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class SharedBook {
	int id;
	String name;
	List<Contact> contacts;
	public SharedBook(int id, String name, List<Contact> contacts) {
		super();
		this.id = id;
		this.name = name;
		this.contacts = contacts;
	}
	
	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public List<Contact> getContacts() {
		return contacts;
	}
	static String tag = "SharedBook";
	public static SharedBook valueOf(JSONObject book) throws JSONException {
		int id = book.getInt("id");
		String name = book.getString("name");
		Log.i(tag, "Id: "+id+", name: "+name);
		List<Contact> contacts = new ArrayList<Contact>();
		JSONArray contactsArray = book.getJSONArray("contacts");
		for(int i=0; i< contactsArray.length(); i++)
		{
			JSONObject c = contactsArray.getJSONObject(i);
			int cid = c.getInt("id");
			String cName = c.getString("name");
			long number = c.getLong("number");
			contacts.add(new Contact(cid, number, cName));
		}
		Log.i(tag, "There are "+contacts.size()+" contacts");
		return new SharedBook(id, name, contacts);
	}
}
