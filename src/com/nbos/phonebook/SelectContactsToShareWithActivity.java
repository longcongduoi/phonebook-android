package com.nbos.phonebook;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.ListActivity;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.CursorJoiner;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.FilterQueryProvider;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.nbos.phonebook.contentprovider.Provider;
import com.nbos.phonebook.database.IntCursorJoiner;
import com.nbos.phonebook.database.tables.BookTable;
import com.nbos.phonebook.value.ContactRow;

public class SelectContactsToShareWithActivity extends ListActivity {

	MatrixCursor m_cursor;
	String tag = "SelectContactsToShareWith",
		id, name;
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.contacts_to_share_with);
	    Bundle extras = getIntent().getExtras();
	    if(extras !=null)
	    {
	    	id = extras.getString("id");
	    	name = extras.getString("name");
	    }
	    
	    setTitle("Select contacts to share "+name+" with");
	    populateContacts();
	    getListView().setTextFilterEnabled(true);
	}
	
	private void populateContacts() {
        getContactsCursor("");
        
        String[] fields = new String[] {
                ContactsContract.Data.DISPLAY_NAME
        };
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.contact_entry, m_cursor,
                fields, new int[] {R.id.contact_name});
        
        adapter.setStringConversionColumn(
                m_cursor.getColumnIndexOrThrow(ContactsContract.Data.DISPLAY_NAME));
      
        adapter.setFilterQueryProvider(new FilterQueryProvider() {

            public Cursor runQuery(CharSequence constraint) {
                String partialItemName = null;
                if (constraint != null) {
                    partialItemName = constraint.toString();
                }
                getContactsCursor(partialItemName);
                return m_cursor;
            }
        });
        Log.i(tag, "There are "+m_cursor.getCount()+" contacts sharing");
        getListView().setAdapter(adapter);
	}
    
	private void getContactsCursor(String search) {
		Cursor contactsCursor = DatabaseHelper.getContacts(this, search);
       
        
        Cursor dataCursor = DatabaseHelper.getBook(this, id); 
       
        IntCursorJoiner joiner = new IntCursorJoiner(
	    		contactsCursor, new String[] {ContactsContract.Contacts._ID},
	    		dataCursor,	new String[] {BookTable.CONTACTID}
	    );

        m_cursor = new MatrixCursor( 
            	new String[] {ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME},10);

        List<ContactRow> rows = new ArrayList<ContactRow>();
        for (CursorJoiner.Result joinerResult : joiner) 
        {
        	switch (joinerResult) {
        		case LEFT:
        			String id = contactsCursor.getString(contactsCursor.getColumnIndex(ContactsContract.Contacts._ID));
        			String name = contactsCursor.getString(contactsCursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
        			if(name != null)
        				rows.add(new ContactRow(id, name, null));
        		break;
        	}
        }	    
        
        Collections.sort(rows);
        for(ContactRow row : rows)
        	m_cursor.addRow(new String[] {row.id, row.name});
		
	}

	protected void onListItemClick(ListView l, View v, int position, long id) {
		int currentPosition= this.getListView().getFirstVisiblePosition();
		m_cursor.moveToPosition(position);
		String contactId = m_cursor.getString(m_cursor.getColumnIndex(ContactsContract.Contacts._ID));
		Log.i(tag, "Contact id is: "+contactId);//+", raw contact id: "+contactId+", lookup key: "+lookupKey);
		shareGroupWithContact(contactId);
		this.setSelection(currentPosition);
		
	}

	private void shareGroupWithContact(String contactId) {
        Uri URI = Uri.parse("content://"+Provider.AUTHORITY+"/"+Provider.BookContent.CONTENT_PATH);
        
        ContentValues bookValues = new ContentValues();
        bookValues.put(BookTable.BOOKID, id);
        bookValues.put(BookTable.CONTACTID, contactId);
        bookValues.put(BookTable.SERVERID, "0");
        // bookValues.put(BookTable.SERVERID, "0");
        
        Uri cUri = this.getContentResolver().insert(URI, bookValues);
        Log.i(tag, "Sharing "+name+" with "+contactId+", uri: "+cUri);
        populateContacts();	
	}
}
