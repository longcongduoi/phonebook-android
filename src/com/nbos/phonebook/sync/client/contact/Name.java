package com.nbos.phonebook.sync.client.contact;

import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import android.database.Cursor;
import android.provider.ContactsContract.CommonDataKinds;

import com.nbos.phonebook.sync.client.Contact;

public class Name {
	public String prefix, given, middle, family, suffix;
	
	public String toString() {
		String name = "";
		if(prefix != null)
			name += prefix + " ";
		if(given != null)
			name += given + " ";
		if(middle != null)
			name += middle + " ";
		if(family != null)
			name += family + " ";
		if(suffix != null)
			name += suffix;
		return name.trim();
	}
	
		
	public static Contact add(Contact contact, Cursor c) {
		contact.name.given = c.getString(c.getColumnIndex(CommonDataKinds.StructuredName.GIVEN_NAME));
		contact.name.family = c.getString(c.getColumnIndex(CommonDataKinds.StructuredName.FAMILY_NAME));
		contact.name.prefix = c.getString(c.getColumnIndex(CommonDataKinds.StructuredName.PREFIX));
		contact.name.middle = c.getString(c.getColumnIndex(CommonDataKinds.StructuredName.MIDDLE_NAME));
		contact.name.suffix = c.getString(c.getColumnIndex(CommonDataKinds.StructuredName.SUFFIX));
		return contact;
	}


	public void addParams(List<NameValuePair> params, String index) {
		if(prefix != null && prefix.trim().length() > 0)
			params.add(new BasicNameValuePair("name_p_"+index, prefix));
		if(given != null && given.trim().length() > 0)
			params.add(new BasicNameValuePair("name_g_"+index, given));
		if(middle != null && middle.trim().length() > 0)
			params.add(new BasicNameValuePair("name_m_"+index, middle));
		if(family != null && family.trim().length() > 0)
			params.add(new BasicNameValuePair("name_f_"+index, family));
		if(suffix != null && suffix.trim().length() > 0)
			params.add(new BasicNameValuePair("name_s_"+index, suffix));
	}


	public static Name valueOf(JSONObject json) throws JSONException {
		Name n = new Name();
		n.prefix = json.has("p") ? json.getString("p") : null;
		n.given = json.has("g") ? json.getString("g") : null;
		n.middle = json.has("m") ? json.getString("m") : null;
		n.family = json.has("f") ? json.getString("f") : null;
		n.suffix = json.has("s") ? json.getString("s") : null;
		return n;
	}
}
