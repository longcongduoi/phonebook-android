package com.nbos.phonebook;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.Dialog;
import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.database.CursorJoiner;
import android.database.MatrixCursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.SimpleCursorAdapter;

import com.nbos.phonebook.database.IntCursorJoiner;
import com.nbos.phonebook.database.tables.BookTable;
import com.nbos.phonebook.util.ImageCursorAdapter;
import com.nbos.phonebook.value.ContactRow;

public class SharingWithActivity extends ListActivity {

	String tag = "SharingWithActivity",
		id, name;
	List<String> ids;
	ImageCursorAdapter adapter;	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    
	    setContentView(R.layout.sharing_with);
	    registerForContextMenu(getListView());  
		
	    Bundle extras = getIntent().getExtras();
	    if(extras !=null)
	    {
	    	id = extras.getString("id");
	    	name = extras.getString("name");
	    }
	    
	    setTitle("Sharing "+name+" with");
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

	MatrixCursor m_cursor;
	private void populateContacts() {	
		
        Cursor contactsCursor = Db.getContacts(this);
        Log.i(tag, "There are "+contactsCursor.getCount()+" contacts");
        Cursor dataCursor = Db.getBook(this, id);
        Log.i(tag, "all columns: "+BookTable.ALL_COLUMNS.length+", data columns: "+dataCursor.getColumnCount());
        ids = new ArrayList<String>();
        while(dataCursor.moveToNext())
        	Log.i(tag, "contactid: "+dataCursor.getString(dataCursor.getColumnIndex(BookTable.CONTACTID))
        			+" dirty: "+dataCursor.getString(dataCursor.getColumnIndex(BookTable.DIRTY))
        			+" serverId: "+dataCursor.getString(dataCursor.getColumnIndex(BookTable.SERVERID)));

        Log.i(tag, "Sharing with "+dataCursor.getCount()+" contacts");
	    IntCursorJoiner joiner = new IntCursorJoiner(
	    		contactsCursor, new String[] {ContactsContract.Contacts._ID},
	    		dataCursor,	new String[] {BookTable.CONTACTID}
	    );
	    	
        m_cursor = new MatrixCursor( 
            	new String[] {ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME},10);
        
        List<ContactRow> rows= new ArrayList<ContactRow>();
        for (CursorJoiner.Result joinerResult : joiner) 
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
        }	    
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

}
