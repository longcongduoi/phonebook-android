package com.nbos.phonebook;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.CursorJoiner;
import android.database.MatrixCursor;
import android.net.Uri;
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
import android.widget.Toast;

import com.nbos.phonebook.contentprovider.Provider;
import com.nbos.phonebook.database.tables.BookTable;

public class GroupActivity extends ListActivity {

	String id, name;
	static String tag = "GroupActivity";
	MatrixCursor m_cursor;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.group);
	    // this.registerForContextMenu(getListView());
	    Bundle extras = getIntent().getExtras();
	    if(extras !=null)
	    {
	    	id = extras.getString("id");
	    	name = extras.getString("name");
	    }
        setTitle("Group: "+name+" ("+numContacts()+" contacts sharing with)");
		System.out.println("group id: "+id);
		queryGroup();
		registerForContextMenu(getListView());  
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
	    // Get the info on which item was selected
	    AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
	    m_cursor.moveToPosition(info.position);
	    String contactName = m_cursor.getString(m_cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
		menu.setHeaderTitle("Menu: "+contactName);
		menu.add(0, v.getId(), 0, "Call");
		menu.add(0, v.getId(), 1, "Remove from group");
		//  menu.add(0, v.getId(), 0, "Action 2");
	}
	
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
	    // Get the info on which item was selected
	    AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();

	    // Get the Adapter behind your ListView (this assumes you're using
	    // a ListActivity; if you're not, you'll have to store the Adapter yourself
	    // in some way that can be accessed here.)
	    m_cursor.moveToPosition(info.position);
	    String contactId = m_cursor.getString(m_cursor.getColumnIndex(ContactsContract.Contacts._ID));

	    Log.i(tag, "position is: "+info.position+", contactId: "+contactId);
	    // Retrieve the item that was clicked on
	    // Object o = adapter.getItem(info.position);
		
		if (item.getTitle() == "Remove from group") {
			Log.i(tag, "Remove: " + item.getItemId());
			removeFromGroup(contactId);
		} else if (item.getTitle() == "Call") {
			//call the guy
			callFromGroup(contactId);
			
			
		} else {
			return false;
		}
		return true;
	}   	
	
	
    private void callFromGroup(String contactId) {
    	
        Uri myPhoneUri = Uri.withAppendedPath(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI, contactId);
        Log.i(tag, "Phone uri is: "+myPhoneUri);

        Cursor phones = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, 
    		null, 		
    		ContactsContract.CommonDataKinds.Phone.RAW_CONTACT_ID +" = "+ contactId,
    		null, null);	
        
        Log.i(tag, "Calling phonenumber contactID is: "+contactId);
       
        Log.i(tag, "There are "+phones.getCount()+" phone numbers");
        
        String phoneNumber=null; 
        phones.moveToFirst();
        
        Log.i(tag, "Calling phonenumber contactID is  :::::::::::"+contactId); 
        
        phoneNumber = phones.getString(phones.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));
        Log.i(tag, "Phone number is: "+phoneNumber);
        
	 	Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(Uri.parse("tel:"+phoneNumber));
        startActivity(callIntent);
	}

	private void removeFromGroup(String contactId) {
    	String [] args = { id, contactId  };
        int b = getContentResolver().delete(ContactsContract.Data.CONTENT_URI, 
        		ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID+ " = ? "
        		+ " and "+ContactsContract.CommonDataKinds.GroupMembership.RAW_CONTACT_ID +" = ? ", 
        		args);
        
        Toast.makeText(this, "Removed", Toast.LENGTH_SHORT).show();
        Data.setGroupDirty(id, getContentResolver());
        //notify registered observers that a row was updated
        getContentResolver().notifyChange(ContactsContract.Data.CONTENT_URI, null);
        queryGroup();
    }
	
	private int numContacts() {	
        Cursor contactsCursor = Data.getContacts(this);//getContacts();
        Log.i(tag, "There are "+contactsCursor.getCount()+" contacts");
        int numContacts = 0;
        Cursor dataCursor = Data.getBook(this, id);
        Log.i(tag, "There are "+dataCursor.getCount()+" contacts in the group");
	    CursorJoiner joiner = new CursorJoiner(
	    		contactsCursor, new String[] {ContactsContract.Contacts._ID},
	    		dataCursor,	new String[] {BookTable.CONTACTID}
	    );

        for (CursorJoiner.Result joinerResult : joiner) 
        {
        	switch (joinerResult) {
        		case LEFT:
        			//Log.i(tag, "LEFT");
        		break;
        		case RIGHT:
        			//Log.i(tag, "RIGHT");
        		break;
        		case BOTH: // handle case where a row with the same key is in both cursors
        			Log.i(tag, "BOTH");
        			numContacts++;        			
        		break;
        	}
        }	 
        Log.i(tag, "Sharing with "+numContacts+" contacts");
        return numContacts;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.group_menu, menu);
	    return true;
		
	}

    private void queryGroup() {
	    ContentResolver cr = getContentResolver();
	    Cursor dataCursor = cr.query(ContactsContract.Data.CONTENT_URI,
	    		// null,
	    	    new String[] {
	    			ContactsContract.Contacts._ID, 
	    			ContactsContract.Data.RAW_CONTACT_ID, 
	    			ContactsContract.RawContacts._ID,
	    			ContactsContract.Contacts.DISPLAY_NAME
	    		},
	    	    ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID+"="+id,
	    	    null, null);
	    Log.i(tag, "There are "+dataCursor.getCount()+" contacts in data");
        while(dataCursor.moveToNext())
        {
        	String name = dataCursor.getString(dataCursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
	        String contactId =
	        	dataCursor.getString(dataCursor.getColumnIndex(ContactsContract.Contacts._ID));
	        String cId =
	        	dataCursor.getString(dataCursor.getColumnIndex(ContactsContract.Data.RAW_CONTACT_ID));

	        String rawId =
	        	dataCursor.getString(dataCursor.getColumnIndex(ContactsContract.RawContacts._ID));

	        Log.i(tag, "Contact name: "+name+", id: "+contactId+", cid: "+cId+", rawId: "+rawId);

        }
	    
	    Cursor contactsCursor = cr.query(ContactsContract.Contacts.CONTENT_URI,
	    		// null,
	    	    new String[] {
	    			ContactsContract.Contacts._ID,
	    			ContactsContract.Contacts.DISPLAY_NAME
	    		},
	    		null,
	    	    // ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID+"="+id,
	    	    null, null);
	    
	    Log.i(tag, "There are "+contactsCursor.getCount()+" contacts");
        while(contactsCursor.moveToNext())
        {
        	String name = contactsCursor.getString(contactsCursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
	        String contactId =
	        	contactsCursor.getString(contactsCursor.getColumnIndex(ContactsContract.Contacts._ID));

	        Log.i(tag, "Contact name: "+name+", id: "+contactId);

        }
	    
	    CursorJoiner joiner = new CursorJoiner(
	    		contactsCursor,
	    		new String[]
	    		{ContactsContract.Contacts._ID},
	    		dataCursor,
	    		new String[] {ContactsContract.Data.RAW_CONTACT_ID}
	    );
        m_cursor = new MatrixCursor( 
        	new String[] {ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME},10);
        for (CursorJoiner.Result joinerResult : joiner) 
        {
        	switch (joinerResult) {
        		case LEFT:
        			// Log.i(tag, "LEFT");
        		break;
        		case RIGHT:
        			// Log.i(tag, "RIGHT");
        		break;
        		case BOTH: // handle case where a row with the same key is in both cursors
        			// Log.i(tag, "BOTH");
        			String id = contactsCursor.getString(contactsCursor.getColumnIndex(ContactsContract.Contacts._ID));
        			String name = contactsCursor.getString(contactsCursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
        			m_cursor.addRow(new String[] {id, name});
        		break;
        	}
        }	    
        
        String[] fields = new String[] {
                ContactsContract.Contacts.DISPLAY_NAME
        };
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, android.R.layout.simple_list_item_1, m_cursor,
                fields, new int[] {android.R.id.text1});
        getListView().setAdapter(adapter);
	    
	    Log.i(tag, "There are "+m_cursor.getCount()+" contacts in this group");
	    

	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	    	case R.id.delete_group:
	    		showDeleteGroupDialog();
	    		break;
	    	case R.id.add_contacts:
	    		showAddContacts();
	    		break;
	    	case R.id.share_group:
	    		showShareGroup();
	    		break;
	    		
	    }
	     return true;
	    /*case R.id.help:
	        showHelp();
	        return true;
	    default:
	        return super.onOptionsItemSelected(item);*/
    }

	private void showShareGroup() {
		Intent i = new Intent(GroupActivity.this, SharingWithActivity.class);
		i.putExtra("id", id);
		i.putExtra("name", name);
        startActivityForResult(i, SHARE_GROUP);	
	}

	static int ADD_CONTACTS = 1, SHARE_GROUP = 2;
	private void showAddContacts() {
		Intent i = new Intent(GroupActivity.this, AddContactsActivity.class);
		i.putExtra("id", id);
		i.putExtra("name", name);
        startActivityForResult(i, ADD_CONTACTS);	
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode == ADD_CONTACTS)
			queryGroup();
	}
	
	private void showDeleteGroupDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Are you sure you want to delete this group?")
		       .setCancelable(false)
		       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		                deleteGroup();
		           }
		       })
		       .setNegativeButton("No", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		                dialog.cancel();
		           }
		       });
		AlertDialog alert = builder.create();	
		alert.show();
	}

	private void deleteGroup() {
		System.out.println("delete group: "+id+", "+name);
		String [] args = { id };
	       try {
	            int b = getContentResolver().delete(ContactsContract.Groups.CONTENT_URI, "_ID=?", args);

	            Toast.makeText(this, "Deleted",Toast.LENGTH_SHORT).show();
	            //notify registered observers that a row was updated
	            getContentResolver().notifyChange(ContactsContract.Groups.CONTENT_URI, null);

	        } catch (Exception e) {
	            Log.v(tag, e.getMessage(), e);
	            Toast.makeText(this, tag + " Delete Failed", Toast.LENGTH_LONG).show();
	        }
	        setResult(RESULT_OK, null);
	        finish();
		/*ContentResolver cr = getContentResolver();
	    Cursor cur = cr.query(ContactsContract.Groups.CONTENT_URI, null,
		        "_ID = '" + id + "'",
		    	null, null);
	    while (cur.moveToNext()) {
	        try{
	            String lookupKey = cur.getString(cur.getColumnIndex(ContactsContract.Groups._ID));
	            Uri uri = Uri.withAppendedPath(ContactsContract.Groups.CONTENT_SUMMARY_URI, lookupKey);
	            System.out.println("The uri is " + uri.toString());
	            cr.delete(uri, null, null);
	        }
	        catch(Exception e)
	        {
	            System.out.println(e.getStackTrace());
	        }
	    }*/
		
	}
    
}
