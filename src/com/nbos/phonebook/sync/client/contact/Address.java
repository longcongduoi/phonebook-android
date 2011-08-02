package com.nbos.phonebook.sync.client.contact;

import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import android.content.res.Resources;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;

import com.nbos.phonebook.sync.client.Contact;

public class Address {
	public String street, poBox, neighborhood, city, state, postalCode, country, type;
	public static void add(Contact contact, Cursor c) {
		Address a = new Address();
		a.street = c.getString(c.getColumnIndex(CommonDataKinds.StructuredPostal.STREET));
		a.poBox = c.getString(c.getColumnIndex(CommonDataKinds.StructuredPostal.POBOX));
		a.neighborhood = c.getString(c.getColumnIndex(CommonDataKinds.StructuredPostal.NEIGHBORHOOD));
		a.city = c.getString(c.getColumnIndex(CommonDataKinds.StructuredPostal.CITY));
		a.state = c.getString(c.getColumnIndex(CommonDataKinds.StructuredPostal.REGION));
		a.postalCode = c.getString(c.getColumnIndex(CommonDataKinds.StructuredPostal.POSTCODE));
		a.country = c.getString(c.getColumnIndex(CommonDataKinds.StructuredPostal.COUNTRY));
		int type = c.getInt(c.getColumnIndex(CommonDataKinds.StructuredPostal.TYPE));
		if(type == CommonDataKinds.StructuredPostal.TYPE_CUSTOM)
			a.type = c.getString(c.getColumnIndex(CommonDataKinds.StructuredPostal.DATA3));
		else
			a.type = getType(type);
		contact.addresses.add(a);
		
	}
	private static String getType(int type) {
		int id = ContactsContract.CommonDataKinds.StructuredPostal.getTypeLabelResource(type);
		return Resources.getSystem().getString(id);
	}
	
	public String toString() {
		String address = "Address ("+type+"): ";
		address += (street == null ? "" : (street + ", "))
				+  (poBox == null ? "" : (poBox + ", "))
				+  (neighborhood == null ? "" : (neighborhood + ", "))
				+  (city == null ? "" : (city + ", "))
				+  (state == null ? "" : (state + ", "))
				+  (postalCode == null ? "" : (postalCode + ", "))
				+  (country == null ? "" : (country + ", "));
		address = address.trim();
		if(address.endsWith(",")) address = address.substring(0, address.length()-1);
		return address;
	}
	
	public void addParams(List<NameValuePair> params, String index, int i) {
		if(street != null)
			params.add(new BasicNameValuePair("ad_street_"+index+"_"+i, street));
		if(poBox != null)
			params.add(new BasicNameValuePair("ad_pb_"+index+"_"+i, poBox));
		if(neighborhood != null)
			params.add(new BasicNameValuePair("ad_nh_"+index+"_"+i, neighborhood));
		if(city != null)
			params.add(new BasicNameValuePair("ad_city_"+index+"_"+i, city));
		if(state != null)
			params.add(new BasicNameValuePair("ad_st_"+index+"_"+i, state));
		if(postalCode != null)
			params.add(new BasicNameValuePair("ad_pc_"+index+"_"+i, postalCode));
		if(country != null)
			params.add(new BasicNameValuePair("ad_country_"+index+"_"+i, country));
		if(type != null)
			params.add(new BasicNameValuePair("ad_type_"+index+"_"+i, type));
	}
}
