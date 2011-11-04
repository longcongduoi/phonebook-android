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

public class Im {
	public String name, protocol;
	public static void add(Contact contact, Cursor c) {
		Im im = new Im();
		im.name = c.getString(c.getColumnIndex(CommonDataKinds.Im.DATA));
		int protocol = c.getInt(c.getColumnIndex(CommonDataKinds.Im.PROTOCOL));
		if(protocol == CommonDataKinds.Im.PROTOCOL_CUSTOM)
			im.protocol = c.getString(c.getColumnIndex(CommonDataKinds.Im.CUSTOM_PROTOCOL));
		else
			im.protocol = getType(protocol);
		if(!Text.isEmpty(im.name))
		contact.ims.add(im);
	}
	private static String getType(int type) {
		int id = ContactsContract.CommonDataKinds.Im.getProtocolLabelResource(type);
		return Resources.getSystem().getString(id);
	}
	
	public String toString() {
		return "IM ("+protocol+"): "+name;
	}
	public void addParams(List<NameValuePair> params, String index, int i) {
		if(name != null)
			params.add(new BasicNameValuePair("im_"+index+"_"+i, name));
		if(protocol != null)
			params.add(new BasicNameValuePair("imType_"+index+"_"+i, protocol));
	}
	public static List<Im> valueOf(JSONArray jsonArray) throws JSONException {
		List<Im> ims = new ArrayList<Im>();
		for(int i=0; i< jsonArray.length(); i++)
		{
			JSONObject e = jsonArray.getJSONObject(i);
			Im im = new Im();
			im.name = e.getString("im");
			im.protocol = e.getString("t");
		}
		return ims;
	}
}
