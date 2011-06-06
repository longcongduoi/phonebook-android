package com.nbos.phonebook;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.database.CursorJoiner;
import android.database.MatrixCursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.nbos.phonebook.database.IntCursorJoiner;
import com.nbos.phonebook.value.ContactRow;

public class AddContactsActivity extends ListActivity {

	MatrixCursor m_cursor;
	String tag = "AddContactsActivity",
		id, name;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.add_contacts);
	    Bundle extras = getIntent().getExtras();
	    if(extras !=null)
	    {
	    	id = extras.getString("id");
	    	name = extras.getString("name");
	    }
	    setTitle("Phonebook: Add contacts to "+name);
	    Log.i(tag, "Intent is: "+getIntent().getClass().getName());
	    populateContacts();
	}

	private void populateContacts() {
        Cursor contactsCursor = Data.getContacts(this);
        Cursor dataCursor = Data.getContactsInGroup(id, getContentResolver());
	    IntCursorJoiner joiner = new IntCursorJoiner(
	    		contactsCursor, new String[] {ContactsContract.Contacts._ID},
	    		dataCursor,	new String[] {ContactsContract.Data.RAW_CONTACT_ID}
	    );

        m_cursor = new MatrixCursor( 
            	new String[] {ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME},10);

        List<ContactRow> rows =new ArrayList<ContactRow>();
        for (CursorJoiner.Result joinerResult : joiner) 
        {
				switch (joinerResult) {
        		case LEFT:
        			String id = contactsCursor.getString(contactsCursor.getColumnIndex(ContactsContract.Contacts._ID));
        			String name = contactsCursor.getString(contactsCursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
        			if(name != null)
        				rows.add(new ContactRow(id, name));	        			
        		break;
        		
        	}
        }	     
        //Collections.sort(rows);
        Collections.sort(rows);
        for(ContactRow row : rows)
        	m_cursor.addRow(new String[] {row.id, row.name});
        
        
        String[] fields = new String[] {
                ContactsContract.Data.DISPLAY_NAME
        };
        
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.contact_entry, m_cursor,
                fields, new int[] {R.id.contact_name});
        getListView().setAdapter(adapter);
	}

	boolean mShowInvisible = false;

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		int currentPosition = this.getListView().getFirstVisiblePosition();
		m_cursor.moveToPosition(position);
		String contactId = m_cursor.getString(m_cursor.getColumnIndex(ContactsContract.Contacts._ID));
		Log.i(tag, "Contact id is: "+contactId);//+", raw contact id: "+contactId+", lookup key: "+lookupKey);
		Data.addToGroup(this.id, contactId, getContentResolver());
		populateContacts();
		this.setSelection(currentPosition);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		this.populateContacts();
	}
	
	
}
