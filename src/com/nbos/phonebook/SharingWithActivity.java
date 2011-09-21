package com.nbos.phonebook;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.Dialog;
import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.nbos.phonebook.database.tables.BookTable;
import com.nbos.phonebook.util.ImageCursorAdapter;
import com.nbos.phonebook.value.ContactRow;

public class SharingWithActivity extends ListActivity {

	String tag = "SharingWithActivity",
		id, name;
	List<String> ids;
	Cursor rawContactsCursor;
	ImageCursorAdapter adapter;
	MatrixCursor m_cursor;
	Db db;
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    db = new Db(getApplicationContext());
        requestWindowFeature(Window.FEATURE_LEFT_ICON);
	    setContentView(R.layout.sharing_with);
        setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, android.R.drawable.ic_menu_share);
	    registerForContextMenu(getListView());  
		
	    Bundle extras = getIntent().getExtras();
	    if(extras !=null)
	    {
	    	id = extras.getString("id");
	    	name = extras.getString("name");
	    }
	    populateContacts();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
	    // Get the info on which item was selected
	    AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
	    m_cursor.moveToPosition(info.position);
	   
	    String contactName = m_cursor.getString(m_cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
		
	    menu.setHeaderTitle("Menu: "+contactName);
	    
		menu.add(0, v.getId(), 0, "Stop share");
		menu.add(1,v.getId(),0,   "Permissions");
	}
	
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
	    // Get the info on which item was selected
	    AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();

	    m_cursor.moveToPosition(info.position);
	    String contactId = m_cursor.getString(m_cursor.getColumnIndex(ContactsContract.Contacts._ID)),
	    	name = m_cursor.getString(m_cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));

	    Log.i(tag, "position is: "+info.position+", contactId: "+contactId+", name: "+name);
	    
		if (item.getTitle() == "Stop share") {
			Log.i(tag, "Remove: " + item.getItemId());
			removeSharing(contactId);
		} 
		else if (item.getTitle() == "Permissions"){
			sharingPermissions(contactId);
		}
		else{
			return false;
		}
		return true;   
	}		
	
	private  void sharingPermissions(String contactId) {
		String contactName = m_cursor.getString(m_cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
		Log.i(tag, "Permissions for "+contactName);
		Dialog dialog = new Dialog(SharingWithActivity.this);
	    dialog.setContentView(R.layout.permissions);
	    dialog.setTitle("Permissions for "+contactName);
	    dialog.show();
	}
	
	private void removeSharing(String contactId) {
		
		
	}

	private void populateContacts() {	
		rawContactsCursor = db.getRawContactsCursor(false);
        Cursor contactsCursor = Db.getContacts(this);
        Log.i(tag, "There are "+contactsCursor.getCount()+" contacts");
        Cursor bookCursor = Db.getBook(this, id);
        Log.i(tag, "Book["+id+"] has "+bookCursor.getCount()+" contacts");
        ids = new ArrayList<String>();
        while(bookCursor.moveToNext())
        	Log.i(tag, "contactid: "+bookCursor.getString(bookCursor.getColumnIndex(BookTable.CONTACTID))
        			+" dirty: "+bookCursor.getString(bookCursor.getColumnIndex(BookTable.DIRTY))
        			+" serverId: "+bookCursor.getString(bookCursor.getColumnIndex(BookTable.SERVERID)));

        Log.i(tag, "Sharing with "+bookCursor.getCount()+" contacts");
        List<ContactRow> rows= new ArrayList<ContactRow>();
        Set<String> contactIds = new HashSet<String>();
        bookCursor.moveToFirst();
        if(bookCursor.getCount() > 0)
        do {
        	String rawContactId = bookCursor.getString(bookCursor.getColumnIndex(BookTable.CONTACTID));
        	ContactRow row = getContactRow(rawContactId, contactsCursor, contactIds);
        	if(row != null)
        		rows.add(row);
        } while(bookCursor.moveToNext());

	    /*IntCursorJoiner joiner = new IntCursorJoiner(
	    		contactsCursor, new String[] {ContactsContract.Contacts._ID},
	    		bookCursor,	new String[] {BookTable.CONTACTID}
	    );*/
	    	
        m_cursor = new MatrixCursor( 
            	new String[] {ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME}, 10);
        
        
        /*for (CursorJoiner.Result joinerResult : joiner) 
        {
        	String id;
        	switch (joinerResult) {
        		case BOTH: // handle case where a row with the same key is in both cursors
        			id = contactsCursor.getString(contactsCursor.getColumnIndex(ContactsContract.Contacts._ID));
        			String name = contactsCursor.getString(contactsCursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
        			Log.i(tag, "name: "+name+", id: "+id);
        			//m_cursor.addRow(new String[] {id, name}); 
        			if(name != null)
         				rows.add(new ContactRow(id, name));
        		break;
        	}
        }*/	    
    	Collections.sort(rows);
    	for(ContactRow row : rows)
    	{
    		m_cursor.addRow(new String[] {row.id, row.name});
    		ids.add(row.id);
    	}
        
        String[] fields = new String[] {
                ContactsContract.Data.DISPLAY_NAME
        };
        
		adapter = new ImageCursorAdapter(this, R.layout.contact_entry,
				m_cursor, ids, fields, new int[] { R.id.contact_name });
        getListView().setAdapter(adapter);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.sharing_with_group_menu, menu);
	    return true;
		
	}
	
	
	@Override
	public void onAttachedToWindow() {
		// TODO Auto-generated method stub
		super.onAttachedToWindow();
		openOptionsMenu();
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	    	case R.id.add_contact_share_with:
	    		showAddContactsToShareWith();
	    		break;
	    }
	     return true;
    }

	static int SHARE_WITH = 1;
	private void showAddContactsToShareWith() {
		Intent i = new Intent(SharingWithActivity.this, SelectContactsToShareWithActivity.class);
		i.putExtra("id", id);
		i.putExtra("name", name);
        startActivityForResult(i, SHARE_WITH);	
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		//if(requestCode == SHARE_WITH)
			populateContacts();
	}

	private ContactRow getContactRow(String rawContactId,
			Cursor contactsCursor, Set<String> contactIds) {
		rawContactsCursor.moveToFirst();
		String contactId = null;
		if(rawContactsCursor.getCount() > 0)
		do {
			String rawId = rawContactsCursor.getString(rawContactsCursor.getColumnIndex(RawContacts._ID)),
				cId = rawContactsCursor.getString(rawContactsCursor.getColumnIndex(RawContacts.CONTACT_ID));
			if(!rawContactId.equals(rawId)) continue;
			contactId = cId;
			break;
		} while(rawContactsCursor.moveToNext());
		
		contactsCursor.moveToFirst();
		if(contactId != null && contactsCursor.getCount() > 0)
		do {
			String cId = contactsCursor.getString(contactsCursor.getColumnIndex(Contacts._ID));
			if(!cId.equals(contactId)
			|| contactIds.contains(contactId)) 
				continue;
			contactIds.add(contactId);
			String name = contactsCursor.getString(contactsCursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
			if(name != null)
				return new ContactRow(contactId, name);
		} while(contactsCursor.moveToNext());
		return null;
	}
	
}
