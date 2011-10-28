package com.nbos.phonebook.sync.client;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import com.nbos.phonebook.sync.client.contact.Address;
import com.nbos.phonebook.sync.client.contact.Email;
import com.nbos.phonebook.sync.client.contact.Im;
import com.nbos.phonebook.sync.client.contact.Name;
import com.nbos.phonebook.sync.client.contact.Organization;
import com.nbos.phonebook.sync.client.contact.Phone;
import com.nbos.phonebook.sync.platform.PhonebookSyncAdapterColumns;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;


public class PhoneContact extends Contact {
	static String tag = "PhoneContact";
	public PhoneContact(String serverId) {
		super(serverId);
	}

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
	}

	public static List<PhoneContact> getContacts(boolean newOnly, ContentResolver cr) {
		List<PhoneContact> contacts = new ArrayList<PhoneContact>();
    	Cursor cursor = getRawContactsEntityCursor(cr, newOnly);
    	
    	Log.i(tag, "There are "+cursor.getCount()+" entries, "+cursor.getColumnCount()+" columns.");
    	// for(String col : rawContactsCursor.getColumnNames())
    		// Log.i(tag, "col: "+col);
    	String prevId = "";
    	PhoneContact contact = null;
    	if(cursor.getCount() == 0) return contacts;
    	cursor.moveToFirst();
    	do {
    		String // contactId = cursor.getString(cursor.getColumnIndex(RawContacts.CONTACT_ID)),
    			rawContactId = cursor.getString(cursor.getColumnIndex(RawContacts._ID)),
    			// dirty = cursor.getString(cursor.getColumnIndex(RawContacts.DIRTY)),
    			mimeType = cursor.getString(cursor.getColumnIndex(Data.MIMETYPE)),
    			accountType = cursor.getString(cursor.getColumnIndex(RawContacts.ACCOUNT_TYPE));
    		String str = "";
            for (String key : DATA_KEYS) 
            {
                final int columnIndex = cursor.getColumnIndexOrThrow(key);
                if (cursor.isNull(columnIndex)) {
                    // don't put anything
                } else {
                    try {
                    	str += key+": "+cursor.getString(columnIndex)+", ";
                    	// Log.i(tag, key+": "+cursor.getString(columnIndex));
                        // cv.put(key, cursor.getString(columnIndex));
                    } catch (SQLiteException e) {
                    	str += key+": isBlob, ";
                    	// Log.i(tag, key+": isBlob");
                        // cv.put(key, cursor.getBlob(columnIndex));
                    }
                }
            }
            // Log.i(tag, "contactId: "+contactId+", dirty: "+dirty+", mimetype: "+mimeType+", data:: "+str);
    		// if(contactId == null) continue; // TODO: SIM contacts have null contactId
            if(!prevId.equals(rawContactId))
            {
            	if(contact != null)
            	{            	
            		Log.i(tag, "rawId: "+contact.rawContactId+", name: "+contact.name+", accountType: "+contact.accountType);
            		contacts.add(contact);
            	}
            	contact = new PhoneContact();
            	contact.rawContactId = rawContactId;
            	contact.accountType = accountType;
            	prevId = rawContactId;
            }
            addContactField(contact, cursor, mimeType);
    	} while(cursor.moveToNext());
    	if(contact != null)
    	{
    		Log.i(tag, "rawId: "+contact.rawContactId+", name: "+contact.name+", accountType: "+contact.accountType);
    		contacts.add(contact);
    	}
    	cursor.close();
    	Log.i(tag, "returning "+contacts.size()+" contacts");
    	return contacts;
	}

	static Cursor getRawContactsEntityCursor(ContentResolver cr, boolean newOnly) {
	    String where = newOnly ? ContactsContract.RawContacts.DIRTY + " = 1" : null;
		return cr.query(ContactsContract.RawContactsEntity.CONTENT_URI, null, where, null, ContactsContract.RawContacts._ID);	
	}

    static String[] DATA_KEYS = new String[]{
        Data.DATA1,
        Data.DATA2,
        Data.DATA3,
        Data.DATA4,
        Data.DATA5,
        Data.DATA6,
        Data.DATA7,
        Data.DATA8,
        Data.DATA9,
        Data.DATA10,
        Data.DATA11,
        Data.DATA12,
        Data.DATA13,
        Data.DATA14,
        Data.DATA15,
        Data.SYNC1,
        Data.SYNC2,
        Data.SYNC3,
        Data.SYNC4};
    
    public String toString() {
    	return "rawId: "+rawContactId+", name: "+name+", accountType: "+accountType+"\n"
    		+ super.toString();    	
    }
	/*
	public PhoneContact(String name, String number, String serverId, String contactId, String rawContactId) {
		super(name, number, serverId);
		this.contactId = contactId;
		this.rawContactId = rawContactId;
	}

	public PhoneContact(String name, String number, String serverId) {
		super(name, number, serverId);
	}*/
}
