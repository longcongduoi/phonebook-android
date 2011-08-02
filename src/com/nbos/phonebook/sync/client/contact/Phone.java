package com.nbos.phonebook.sync.client.contact;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.res.Resources;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;

import com.nbos.phonebook.sync.client.Contact;

public class Phone {
	public String number, type;
	public static void add(Contact contact, Cursor c) {
		Phone p = new Phone();
		p.number = c.getString(c.getColumnIndex(CommonDataKinds.Phone.NUMBER));
		int type = c.getInt(c.getColumnIndex(CommonDataKinds.Phone.TYPE));
		if(type == CommonDataKinds.Phone.TYPE_CUSTOM)
			p.type = c.getString(c.getColumnIndex(CommonDataKinds.Phone.DATA3));
		else
			p.type = getType(type);
		contact.phones.add(p);
		
	}
	private static String getType(int type) {
		int id = ContactsContract.CommonDataKinds.Phone.getTypeLabelResource(type);
		return Resources.getSystem().getString(id);
	}
	
	public String toString() {
		return "Phone ("+type+"): "+number;
	}
	
	public void addParams(List<NameValuePair> params, String index, int i) {
		if(number != null)
			params.add(new BasicNameValuePair("ph_"+index+"_"+i, number));
		if(type != null)
			params.add(new BasicNameValuePair("phType_"+index+"_"+i, type));
	}
	public static List<Phone> valueOf(JSONArray jsonArray) throws JSONException {
		List<Phone> phones = new ArrayList<Phone>();
		for(int i=0; i< jsonArray.length(); i++)
		{
			JSONObject e = jsonArray.getJSONObject(i);
			Phone ph = new Phone();
			ph.number = e.getString("num");
			ph.type = e.getString("t");
			phones.add(ph);
		}
		return phones;
	}
	
}
