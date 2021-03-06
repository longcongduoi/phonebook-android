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
import com.nbos.phonebook.util.Text;

public class Email {
	public String address, type;

	public static void add(Contact contact, Cursor c) {
		Email e = new Email();
		e.address = c.getString(c.getColumnIndex(CommonDataKinds.Email.DATA));
		int type = c.getInt(c.getColumnIndex(CommonDataKinds.Email.TYPE));
		if(type == CommonDataKinds.Email.TYPE_CUSTOM)
			e.type = c.getString(c.getColumnIndex(CommonDataKinds.Email.DATA3));
		else
			e.type = getType(type);
		if(!Text.isEmpty(e.address))
			contact.emails.add(e);
	}
	private static String getType(int type) {
		int id = ContactsContract.CommonDataKinds.Email.getTypeLabelResource(type);
		return Resources.getSystem().getString(id);
	}

	public String toString() {
		return "Email ("+type+"): "+address;
	}

	public void addParams(List<NameValuePair> params, String index, int i) {
		if(address != null)
			params.add(new BasicNameValuePair("em_"+index+"_"+i, address));
		if(type != null)
			params.add(new BasicNameValuePair("emType_"+index+"_"+i, type));
	}
	public static List<Email> valueOf(JSONArray jsonArray) throws JSONException {
		List<Email> emails = new ArrayList<Email>();
		for(int i=0; i< jsonArray.length(); i++)
		{
			JSONObject e = jsonArray.getJSONObject(i);
			Email email = new Email();
			email.address = e.getString("add");
			email.type = e.getString("t");
			emails.add(email);
		}
		return emails;
	}
	
	int [] types = {
			CommonDataKinds.Email.TYPE_HOME,
			CommonDataKinds.Email.TYPE_WORK,
			CommonDataKinds.Email.TYPE_OTHER,
			CommonDataKinds.Email.TYPE_MOBILE,
	};
	
	public int getIntType() {
		for(int intType : types)
			if(type.equals(getType(intType)))
				return intType;
		return CommonDataKinds.Phone.TYPE_CUSTOM;
	}
    

}
