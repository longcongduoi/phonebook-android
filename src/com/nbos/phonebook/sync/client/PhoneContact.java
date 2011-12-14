package com.nbos.phonebook.sync.client;

import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import android.provider.ContactsContract.Data;


public class PhoneContact extends Contact {
	static String tag = "PhoneContact";
	public PhoneContact() {
		super();
	}

	public String rawContactId, accountType;

	/*public PhoneContact(String contactId) {
		this.contactId = contactId;
	}*/
	public void addParams(List<NameValuePair> params, String index) {
		super.addParams(params, index);
    	params.add(new BasicNameValuePair("contactId_"+index, rawContactId));
    	params.add(new BasicNameValuePair("accountType_"+index, accountType));
    	if(deleted)
    		params.add(new BasicNameValuePair("deleted_"+index, "t"));
	}

    public String toString() {
    	return "raw: "+rawContactId+", name: "+name+", accountType: "+accountType+"\n"
    		+ super.toString();    	
    }
}
