package com.nbos.phonebook.sync.client.contact;

import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import android.content.res.Resources;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;

import com.nbos.phonebook.sync.client.Contact;

public class Organization {
	public String company, title, type;
	public static void add(Contact contact, Cursor c) {
		Organization o = new Organization();
		o.company = c.getString(c.getColumnIndex(CommonDataKinds.Organization.COMPANY));
		o.title = c.getString(c.getColumnIndex(CommonDataKinds.Organization.TITLE));
		int type = c.getInt(c.getColumnIndex(CommonDataKinds.Organization.TYPE));
		if(type == CommonDataKinds.Organization.TYPE_CUSTOM)
			o.type = c.getString(c.getColumnIndex(CommonDataKinds.Organization.DATA3));
		else
			o.type = getType(type);
		contact.orgs.add(o);
	}
	private static String getType(int type) {
		int id = ContactsContract.CommonDataKinds.Organization.getTypeLabelResource(type);
		return Resources.getSystem().getString(id);
	}
	
	public String toString() {
		return "Org ("+type+"): "+company+", "+title;
	}
	
	public void addParams(List<NameValuePair> params, String index, int i) {
		if(company != null)
			params.add(new BasicNameValuePair("org_company_"+index+"_"+i, company));
		if(title != null)
			params.add(new BasicNameValuePair("org_title_"+index+"_"+i, title));
		if(type != null)
			params.add(new BasicNameValuePair("org_type_"+index+"_"+i, type));
	}
}
