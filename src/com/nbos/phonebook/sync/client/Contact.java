package com.nbos.phonebook.sync.client;

import org.json.JSONObject;

import android.util.Log;

public class Contact {
	static String tag = "Contact";
	public String serverId, picId,
		name, number, email;
	public boolean deleted;
	
	public Contact(String name, String number, String serverId, String picId) {
		this(name, number, serverId);
		this.picId = picId;
	}

	public Contact(String name, String number, String serverId) {
		this.serverId = serverId;
		this.number = number;
		this.name = name;
	}

	public static Contact valueOf(JSONObject user) {
        try {
            String name = user.has("name") ? user.getString("name") : null,
            	number = user.has("number") ? user.getString("number") : null,
            	serverId = new Integer(user.getInt("id")).toString(),
            	picId = user.getString("pic");
            Log.i(tag, "name: "+name+", picId is: "+picId);
            return new Contact(name, number, serverId, picId); 
        } catch (final Exception ex) {
            Log.i(tag, "Error parsing JSON user object" + ex.toString());

        }
        return null;
    }
	
	@Override
	public String toString() {
		return name;
	}
	
}
