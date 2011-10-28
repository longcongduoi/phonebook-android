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
import android.net.Uri;
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
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.nbos.phonebook.database.tables.BookTable;
import com.nbos.phonebook.util.ImageCursorAdapter;
import com.nbos.phonebook.value.ContactRow;

public class SharingWithActivity extends ListActivity {

	String tag = "SharingWithActivity", id, name,owner,
		RAW_CONTACT_ID_COLUMN = "rawContactId";
	List<String> ids;
	Cursor rawContactsCursor;
	ImageCursorAdapter adapter;
	MatrixCursor m_cursor;
	Db db;
	int layout;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		db = new Db(getApplicationContext());
		requestWindowFeature(Window.FEATURE_LEFT_ICON);
		setContentView(R.layout.sharing_with);
		setFeatureDrawableResource(Window.FEATURE_LEFT_ICON,
				R.drawable.share_64x64);

		registerForContextMenu(getListView());

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			id = extras.getString("id");
			name = extras.getString("name");
			layout = extras.getInt("layout");
		}

		setTitle("Group: " + name + " sharing with");
		populateContacts(layout);
		listview = getListView();
		listview.setFastScrollEnabled(true);
		
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		// Get the info on which item was selected
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
		m_cursor.moveToPosition(info.position);

		String contactName = m_cursor.getString(m_cursor
				.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));

		menu.setHeaderTitle("Menu: " + contactName);

		menu.add(0, v.getId(), 0, "Stop share");
		menu.add(1, v.getId(), 0, "Permissions");
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		// Get the info on which item was selected
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();

		m_cursor.moveToPosition(info.position);
		String contactId = m_cursor.getString(m_cursor
				.getColumnIndex(ContactsContract.Contacts._ID)), name = m_cursor
				.getString(m_cursor
						.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));

		Log.i(tag, "position is: " + info.position + ", contactId: "
				+ contactId + ", name: " + name);

		if (item.getTitle() == "Stop share") {
			Log.i(tag, "Remove: " + item.getItemId());
			// removeSharing(contactId);
		} else if (item.getTitle() == "Permissions") {
			sharingPermissions(contactId);
		} else {
			return false;
		}
		return true;
	}

	private void sharingPermissions(String contactId) {
		String contactName = m_cursor.getString(m_cursor
				.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
		Log.i(tag, "Permissions for " + contactName);
		Dialog dialog = new Dialog(SharingWithActivity.this);
		dialog.setContentView(R.layout.permissions);
		dialog.setTitle("Permissions for " + contactName);
		dialog.show();
	}

	public int getCheckedCount(ListView listview, int checkboxId) {

		int checkedCount = 0;
		for (int i = 0; i < listview.getChildCount(); i++) 
		{
			View v = (View) listview.getChildAt(i);
			CheckBox checked = (CheckBox) v.findViewById(checkboxId);
			if (checked.isChecked()) {
				checkedCount++;
			}
		}
		return checkedCount;
	}
    
	ListView listview;
	
	private void removeSharing() {
		populateContacts(R.layout.sharing_contact_entry);
		showButton();
	}
     
	
	
	private void showButton() {
		LinearLayout mainLayout = (LinearLayout) findViewById(R.id.sharing_with_layout);
		LinearLayout childLayout=(LinearLayout)mainLayout.findViewById(R.id.extraLayout);
		childLayout.setVisibility(1);
	    Button stopSharing = (Button)childLayout.findViewById(R.id.remove_contacts_button);
		stopSharing.setOnClickListener(removeSharingContacts);

	}
	
	
	private Button.OnClickListener removeSharingContacts = new Button.OnClickListener() {
		
			public void onClick(View v) {
				int numRemoved = 0;
				LinearLayout mainLayout = (LinearLayout) findViewById(R.id.sharing_with_layout);
				LinearLayout childLayout=(LinearLayout)mainLayout.findViewById(R.id.extraLayout);
				childLayout.setVisibility(1);
				List<Boolean> checkedItems = adapter.getCheckedItems();
				for (int i = 0; i < listview.getCount(); i++) 
				{
					if (!checkedItems.get(i)) continue;
					m_cursor.moveToPosition(i);
					String rawContactId = m_cursor.getString(m_cursor.getColumnIndex(RAW_CONTACT_ID_COLUMN));
					String contactId = db.getContactId(rawContactId, rawContactsCursor);
					// remove all raw contactIds which have the same contactId as this rawContactId
					List<String> rawContactIds = db.getRawContactIds(contactId, rawContactsCursor);
					for(String r : rawContactIds)
						db.setDeleteSharingWith(id, r);
					numRemoved++;
				}
				Toast.makeText(getApplicationContext(), 
					"Removed "+numRemoved+" contact(s) from sharing",
					Toast.LENGTH_LONG).show();
				populateContacts(layout);
				childLayout.setVisibility(-1);
				onAttachedToWindow();
			}
		};
	
	
	private void populateContacts(int layout) {
		rawContactsCursor = db.getRawContactsCursor(false);
		Cursor contactsCursor = Db.getContacts(this);
		Log.i(tag, "There are " + contactsCursor.getCount() + " contacts");
		Cursor bookCursor = db.getBook(id);
		Log.i(tag, "Book[" + id + "] is being shared with " + bookCursor.getCount()
				+ " raw contacts");
		ids = new ArrayList<String>();
		while (bookCursor.moveToNext())
			Log.i(tag,
			"bookId: " + bookCursor.getString(bookCursor.getColumnIndex(BookTable.BOOKID))
			+", contactid: " + bookCursor.getString(bookCursor.getColumnIndex(BookTable.CONTACTID))
			+ ", dirty: " + bookCursor.getString(bookCursor.getColumnIndex(BookTable.DIRTY))
			+ ", deleted: " + bookCursor.getString(bookCursor.getColumnIndex(BookTable.DELETED)));

		Log.i(tag, "Sharing with " + bookCursor.getCount() + " raw contacts");
		List<ContactRow> rows = new ArrayList<ContactRow>();
		Set<String> contactIds = new HashSet<String>();
		bookCursor.moveToFirst();
		if (bookCursor.getCount() > 0)
			do {
				String rawContactId = bookCursor.getString(bookCursor.getColumnIndex(BookTable.CONTACTID));
				String deleted = bookCursor.getString(bookCursor.getColumnIndex(BookTable.DELETED));
				String dirty = bookCursor.getString(bookCursor.getColumnIndex(BookTable.DIRTY));
				Log.i(tag, "deleted is: "+deleted+", dirty: "+dirty);
				if(deleted.equals("1")) continue;
				ContactRow row = getContactRow(rawContactId, contactsCursor,
						contactIds);
				if (row != null)
					rows.add(row);
			} while (bookCursor.moveToNext());

		m_cursor = new MatrixCursor(new String[] {
				Contacts._ID,
				RAW_CONTACT_ID_COLUMN,
				Contacts.DISPLAY_NAME }, 10);

		Collections.sort(rows);
		for (ContactRow row : rows) {
			m_cursor.addRow(new String[] { row.id, row.rawContactId, row.name });
			ids.add(row.id);
		}
		setTitle("Group: " + name + " sharing with ("+rows.size()+")");

		String[] fields = new String[] { ContactsContract.Data.DISPLAY_NAME };

		adapter = new ImageCursorAdapter(this, layout, m_cursor, ids, fields,
				new int[] { R.id.contact_name });
		getListView().setAdapter(adapter);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		if (layout == (int) R.layout.contact_entry) {
			inflater.inflate(R.menu.sharing_with_group_menu, menu);
		} else {

		}
		return true;

	}

	@Override
	public void onAttachedToWindow() {
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
		case R.id.add_new_contact:
			showCreateNewContact();
			break;
		case R.id.stop_sharing:
			removeSharing();
			break;
		}
		return true;
	}
	Cursor main_cursor;
	ArrayList<String> list1=new ArrayList<String>();
	private void showCreateNewContact() {
		/*
		 * Intent intent = new
		 * Intent(ContactsContract.Intents.SHOW_OR_CREATE_CONTACT);
		 * intent.setData(ContactsContract.Contacts.CONTENT_URI);
		 * intent.putExtra(ContactsContract.Intents.EXTRA_FORCE_CREATE, true);
		 */
		main_cursor=Db.getContacts(this);
		main_cursor.moveToFirst();
		if (main_cursor.getCount() > 0){
			do {
			      list1.add(main_cursor.getString(main_cursor.getColumnIndex(Contacts._ID)));
		       }while(main_cursor.moveToNext());
		}
		
		Log.i(tag,"count"+list1.size());
		Intent addContactIntent = new Intent(
				ContactsContract.Intents.Insert.ACTION);
		addContactIntent.setType(ContactsContract.Contacts.CONTENT_TYPE);
		startActivityForResult(addContactIntent,INSERT_CONTACT_REQUEST);
		/*
		 * Intent i = new Intent(SharingWithActivity.this,
		 * EditContactActivity.class); //
		 * i.setData(ContactsContract.Contacts.CONTENT_URI);
		 * i.setData(Uri.parse(ContactsContract.Contacts.CONTENT_URI + "/0" ));
		 * startActivityForResult(i, INSERT_CONTACT_REQUEST);
		 */
		// this.getApplicationContext(), SharingWithActivity.class);
		// Intent intent = new Intent(Contacts.Intents.SHOW_OR_CREATE_CONTACT);
		// startActivity(intent);
		// startActivityForResult(intent, INSERT_CONTACT_REQUEST);

	}

	static int SHARE_WITH = 1, INSERT_CONTACT_REQUEST = 2;

	private void showAddContactsToShareWith() {
		Intent i = new Intent(SharingWithActivity.this,
				SelectContactsToShareWithActivity.class);
		i.putExtra("id", id);
		i.putExtra("name", name);
		startActivityForResult(i, SHARE_WITH);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == SHARE_WITH) { }
		if (requestCode == INSERT_CONTACT_REQUEST && resultCode == RESULT_OK)
		{
			Uri contactUri = data.getData();
			String contactId = contactUri.getLastPathSegment();
			Log.i(tag, "Contact uri: "+contactUri+", contactId: "+contactId);
			rawContactsCursor.requery();
			shareGroupWithContact(contactId);
		}
		onAttachedToWindow();
	    populateContacts(layout);
	}

	private ContactRow getContactRow(String rawContactId,
			Cursor contactsCursor, Set<String> contactIds) {
		rawContactsCursor.moveToFirst();
		String contactId = null;
		if (rawContactsCursor.getCount() > 0)
			do {
				String rawId = rawContactsCursor.getString(rawContactsCursor.getColumnIndex(RawContacts._ID)), 
					cId = rawContactsCursor.getString(rawContactsCursor.getColumnIndex(RawContacts.CONTACT_ID));
				if (!rawContactId.equals(rawId))
					continue;
				contactId = cId;
				break;
			} while (rawContactsCursor.moveToNext());

		contactsCursor.moveToFirst();
		if (contactId != null && contactsCursor.getCount() > 0)
			do {
				String cId = contactsCursor.getString(contactsCursor
						.getColumnIndex(Contacts._ID));
				if (!cId.equals(contactId) || contactIds.contains(contactId))
					continue;
				contactIds.add(contactId);
				String name = contactsCursor
						.getString(contactsCursor
								.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
				if (name != null)
					return new ContactRow(contactId, rawContactId, name);
			} while (contactsCursor.moveToNext());
		return null;
	}
	
	
	
	private void shareGroupWithContact(String contactId) {
		List<String> rawContactIds = Db.getRawContactIds(contactId, rawContactsCursor);
		for (String rawContactId : rawContactIds) 
		{
			db.addSharingWith(id, rawContactId);
			Log.i(tag, "Sharing " + name + " with rawContact: " + rawContactId);
		}
	}

}
