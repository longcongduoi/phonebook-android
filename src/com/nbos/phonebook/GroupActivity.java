package com.nbos.phonebook;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.CursorJoiner;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.FilterQueryProvider;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.nbos.phonebook.database.IntCursorJoiner;
import com.nbos.phonebook.database.tables.BookTable;
import com.nbos.phonebook.sync.Constants;
import com.nbos.phonebook.sync.client.BookPermission;
import com.nbos.phonebook.util.ImageCursorAdapter;
import com.nbos.phonebook.value.ContactRow;

public class GroupActivity extends ListActivity {

	String id, name, owner, permission, accountType;
	static String tag = "GroupActivity";
	MatrixCursor m_cursor;
	List<String> ids;
	Cursor groupCursor;
	ImageCursorAdapter adapter;
	Cursor rawContactsCursor;
	Db db;
	int layout,keyValue;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_LEFT_ICON);
		setContentView(R.layout.group);
		setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.group);

		// this.registerForContextMenu(getListView());
		db = new Db(getApplicationContext());
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			id = extras.getString("id");
			name = extras.getString("name");
			owner = extras.getString("owner");
			permission = extras.getString("permission");
			accountType = extras.getString("accountType");
			layout = extras.getInt("layout");
			Log.i(tag, "Owner is: " + owner + ", permission is: "+permission);
		}
		queryGroup(layout);
		registerForContextMenu(getListView());
		listView = this.getListView();
		getListView().setTextFilterEnabled(true);

	}

	@Override
	public void onAttachedToWindow() {
		super.onAttachedToWindow();
		openOptionsMenu();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		Log.i(tag, "Create context menu");
		boolean hasNumber = false, hasEdit = false;
		// Get the info on which item was selected
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
		m_cursor.moveToPosition(info.position);
		String contactName = m_cursor.getString(m_cursor.getColumnIndex(Contacts.DISPLAY_NAME)), 
			contactId = m_cursor.getString(m_cursor.getColumnIndex(Contacts._ID)),
			phoneNumber = getPhoneNumber(contactId);
		if(phoneNumber != null)
			hasNumber = true;
		if (owner == null 
		||(owner != null && accountType.equals(Constants.ACCOUNT_TYPE) && Integer.parseInt(permission) >= BookPermission.EDIT_CONTACTS.ordinal())
		||(owner != null && !accountType.equals(Constants.ACCOUNT_TYPE)))
			hasEdit = true;
		if(!hasNumber && !hasEdit)
			return;

		super.onCreateContextMenu(menu, v, menuInfo);
		menu.setHeaderTitle("Menu: " + contactName);
		int order = 0;
		if (hasNumber)
			menu.add(0, v.getId(), order++, "Call");
		if(hasEdit)
			menu.add(0, v.getId(), order++, "Edit");
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
		// Retrieve the item that was clicked on
		// Object o = adapter.getItem(info.position);

		if (item.getTitle() == "Remove from group") {
			Log.i(tag, "Remove: " + item.getItemId());
			removeFromGroup(contactId);
		} else if (item.getTitle() == "Call") {
			// call the guy
			callFromGroup(contactId);
		} else if (item.getTitle() == "Edit") {
			showEdit(contactId);
		} else {
			return false;
		}
		return true;
	}

	int EDIT_CONTACT = 1;

	private void showEdit(String contactId) {
		// Intent i = new Intent(Intent.ACTION_EDIT);
		Intent i = new Intent(GroupActivity.this, EditContactActivity.class);
		i.setData(Uri.parse(ContactsContract.Contacts.CONTENT_URI + "/"
				+ contactId));
		startActivityForResult(i, EDIT_CONTACT);
	}

	private String getPhoneNumber(String contactId) {
		Log.i(tag, "getPhoneNumber(" + contactId + ")");
		Cursor phones = getContentResolver().query(
				ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
				new String[] { Phone.NUMBER, Phone.TYPE },
				ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = "
						+ contactId, null, null);
		Log.i(tag, "There are " + phones.getCount() + " phone numbers");
		if (phones.getCount() == 0)
			return null;
		phones.moveToFirst();
		String phoneNumber = phones.getString(phones
				.getColumnIndexOrThrow(Phone.NUMBER));
		Log.i(tag, "contactId:" + contactId + ", phone number is: "
				+ phoneNumber);
		phones.close();
		return phoneNumber;
	}

	private void callFromGroup(String contactId) {
		String phoneNumber = getPhoneNumber(contactId);
		if (phoneNumber == null)
			return;
		Log.i(tag, "Calling contactId:" + contactId + ", phone number is: "
				+ phoneNumber);
		Intent callIntent = new Intent(Intent.ACTION_CALL);
		callIntent.setData(Uri.parse("tel:" + phoneNumber));
		startActivity(callIntent);
	}

	private void removeFromGroup(String contactId) {
		String[] args = { id, contactId };
		int b = getContentResolver()
				.delete(ContactsContract.Data.CONTENT_URI,
						ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID
								+ " = ? "
								+ " and "
								+ ContactsContract.CommonDataKinds.GroupMembership.CONTACT_ID
								+ " = ? ", args);
		Db.setGroupDirty(id, getContentResolver());
	}

	private int numContacts() {
		Cursor contactsCursor = Db.getContacts(this);// getContacts();
		Log.i(tag, "There are " + contactsCursor.getCount() + " contacts");
		int numContacts = 0;
		Cursor bookCursor = db.getBook(id);
		Log.i(tag, "There are " + bookCursor.getCount()
				+ " contacts sharing this group");
		IntCursorJoiner joiner = new IntCursorJoiner(contactsCursor,
				new String[] { ContactsContract.Contacts._ID }, bookCursor,
				new String[] { BookTable.CONTACTID });

		for (CursorJoiner.Result joinerResult : joiner) {
			switch (joinerResult) {
			case BOTH: // handle case where a row with the same key is in both
						// cursors
				numContacts++;
				break;
			}
		}
		Log.i(tag, "Sharing with " + numContacts + " contacts");
		return numContacts;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		if(owner == null)
		{
			inflater.inflate(R.menu.group_menu, menu);
			return true;
		}
		// this is a shared group
		int perm = Integer.parseInt(permission);
		if (perm < BookPermission.ADD_CONTACTS.ordinal()) 
			return false;
		
		inflater.inflate(R.menu.group_menu, menu);
		menu.findItem(R.id.share_group).setVisible(false); // cannot share shared group
		if(perm < BookPermission.ADD_REMOVE_CONTACTS.ordinal()) // no permission to remove
			menu.findItem(R.id.remove_contacts).setVisible(false);
		return true;
	}

	/*
	 @Override protected void onListItemClick(ListView l, View v, int
	 position, long id) { m_cursor.moveToPosition(position); String contactId
	 = m_cursor.getString(m_cursor
	 .getColumnIndex(ContactsContract.Contacts._ID)); Log.i(tag,
	 "position is: " + position + ", contactId: " + contactId + ", name: " +
	 name); callFromGroup(contactId); }
	 */

	private void queryGroup(int layout) {
		if (owner == null || !accountType.equals(Constants.ACCOUNT_TYPE)) //
			setTitle("Group: " + name + " (" + numContacts()
					+ " contacts sharing with)");
		else
			setTitle("Group: " + name + " (" + owner + " is sharing)");
		groupCursor = Db.getContactsInGroup(id, this.getContentResolver());
		getContactsFromGroupCursor("");
		String[] fields = new String[] { ContactsContract.Contacts.DISPLAY_NAME };
		adapter = new ImageCursorAdapter(this, layout, m_cursor, ids, fields,
				new int[] { R.id.contact_name });

		adapter.setStringConversionColumn(m_cursor
				.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME));

		adapter.setFilterQueryProvider(new FilterQueryProvider() {

			public Cursor runQuery(CharSequence constraint) {
				String partialItemName = null;
				if (constraint != null) {
					partialItemName = constraint.toString();
				}
				getContactsFromGroupCursor(partialItemName);
				adapter.setCursor(m_cursor);
				adapter.setIds(ids);
				return m_cursor;
			}
		});

		getListView().setAdapter(adapter);

		Log.i(tag, "There are " + m_cursor.getCount()
				+ " contacts in this group");

	}

	private void getContactsFromGroupCursor(String search) {
		ids = new ArrayList<String>();
		Log.i(tag, "There are " + groupCursor.getCount()
				+ " contacts in data for groupId: " + id);
		Cursor contactsCursor = Db.getContacts(this, search);
		Log.i(tag, "There are " + contactsCursor.getCount()
				+ " contacts matching " + search);

		IntCursorJoiner joiner = new IntCursorJoiner(contactsCursor,
				new String[] { ContactsContract.Contacts._ID }, groupCursor,
				new String[] { ContactsContract.Data.CONTACT_ID });
		m_cursor = new MatrixCursor(new String[] {
				ContactsContract.Contacts._ID,
				ContactsContract.Contacts.DISPLAY_NAME }, 10);

		List<ContactRow> rows = new ArrayList<ContactRow>();
		for (CursorJoiner.Result joinerResult : joiner) {
			switch (joinerResult) {
			case BOTH: // handle case where a row with the same key is in both
						// cursors
				String contactId = contactsCursor.getString(contactsCursor
						.getColumnIndex(ContactsContract.Contacts._ID));
				String rawContactId = groupCursor.getString(groupCursor
						.getColumnIndex(Data.RAW_CONTACT_ID));

				String name = contactsCursor
						.getString(contactsCursor
								.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
				Log.i(tag, "Contact id: " + contactId + ", rawContactId: "
						+ rawContactId + ", name: " + name);
				if (name != null)
					rows.add(new ContactRow(contactId, name));
				break;
			}
		}
		Collections.sort(rows);
		Log.i(tag, "There are " + rows.size() + " contacts matching " + search);
		for (ContactRow row : rows) {
			m_cursor.addRow(new String[] { row.id, row.name });
			Log.i(tag, "Adding row[" + row.id + "] = " + row.name);
			ids.add(row.id);
		}

	}

	ListView mGroupList;
	boolean mShowInvisible = false;

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.delete_group:
			showDeleteGroupDialog();
			break;
		case R.id.add_contacts:
			Log.i(tag, "Option menu");
			showAddContacts();
			break;
		case R.id.share_group:
			showShareGroup();
			break;
		case R.id.stop_sharing:
			showSharingContacts();
			break;
		case R.id.remove_contacts:
			if(m_cursor.getCount()>0)
			removeContacts();
			break;
		//case R.id.bump_contacts:
			//break;
		}
		return true;
		/*
		  case R.id.help: showHelp(); return true; default: return
		  super.onOptionsItemSelected(item);
		 */
	}

	ListView listView;

	private void removeContacts() {
		queryGroup(R.layout.remove_contacts_entry);
		keyValue=1;
		showButton();
	}

	private void showButton() {
		LinearLayout mainLayout = (LinearLayout) findViewById(R.id.linearLayout1);
		LinearLayout childLayout = (LinearLayout) mainLayout
				.findViewById(R.id.extraLayout);
		childLayout.setVisibility(1);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.FILL_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		childLayout.setLayoutParams(params);
		Button extraButton = (Button) childLayout
				.findViewById(R.id.remove_contacts_button);
		extraButton.setOnClickListener(removeContacts);
	}

	private Button.OnClickListener removeContacts = new OnClickListener() {

		public void onClick(View v) {
			int numRemoved = 0;
			List<Boolean> checkedItems = adapter.getCheckedItems();
			LinearLayout mainLayout = (LinearLayout) findViewById(R.id.linearLayout1);
			LinearLayout childLayout = (LinearLayout) mainLayout
					.findViewById(R.id.extraLayout);
			Log.i(tag, "Number of items: " + listView.getChildCount());
			for (int i = 0; i < listView.getCount(); i++) {
				if (checkedItems.get(i)) {
					numRemoved++;
					m_cursor.moveToPosition(i);
					String contactId = m_cursor.getString(m_cursor
							.getColumnIndex(ContactsContract.Contacts._ID));
					Log.i(tag, "Contact id is: " + contactId);
					removeFromGroup(contactId);
				}
			}
			childLayout.setVisibility(-1);
			getContentResolver().notifyChange(
					ContactsContract.Data.CONTENT_URI, null);
			queryGroup(layout);
			Toast.makeText(getApplicationContext(),
					"Deleted " + numRemoved + " contact(s)", Toast.LENGTH_LONG)
					.show();
			onAttachedToWindow();
		}
	};
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		
	    if (keyCode == KeyEvent.KEYCODE_BACK && keyValue==1){
	    	
	    	LinearLayout mainLayout = (LinearLayout) findViewById(R.id.linearLayout1);
			LinearLayout childLayout = (LinearLayout) mainLayout
					.findViewById(R.id.extraLayout);
			childLayout.setVisibility(-1);
	    	queryGroup(layout);
	    	keyValue=0;
	    	onAttachedToWindow();
	    }
	    else{
	    	return super.onKeyDown(keyCode, event);
	    }
	    return true;
	}

	private void showSharingContacts() {

		Intent i = new Intent(GroupActivity.this, SharingWithActivity.class);
		i.putExtra("id", id);
		i.putExtra("name", name);
		i.putExtra("layout", R.layout.sharing_contact_entry);
		startActivityForResult(i, STOP_SHARING);
	}

	private void showShareGroup() {
		Intent i = new Intent(GroupActivity.this, SharingWithActivity.class);
		i.putExtra("id", id);
		i.putExtra("name", name);
		i.putExtra("layout", R.layout.contact_entry);
		startActivityForResult(i, SHARE_GROUP);
	}

	static int ADD_CONTACTS = 1, SHARE_GROUP = 2, STOP_SHARING = 3;

	private void showAddContacts() {
		Log.i(tag, "Add Contact activity");
		Intent i = new Intent(GroupActivity.this, AddContactsActivity.class);
		i.putExtra("id", id);
		i.putExtra("name", name);
		startActivityForResult(i, ADD_CONTACTS);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		onAttachedToWindow();
		queryGroup(layout);
	}

	private void showDeleteGroupDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Are you sure you want to delete this group?")
				.setCancelable(false)
				.setPositiveButton("Yes",
						new DialogInterface.OnClickListener() {
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
		System.out.println("delete group: " + id + ", " + name);
		String[] args = { id };
		try {
			int b = getContentResolver().delete(
					ContactsContract.Groups.CONTENT_URI, "_ID=?", args);

			Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
			// notify registered observers that a row was updated
			getContentResolver().notifyChange(
					ContactsContract.Groups.CONTENT_URI, null);

		} catch (Exception e) {
			Log.v(tag, e.getMessage(), e);
			Toast.makeText(this, tag + " Delete Failed", Toast.LENGTH_LONG)
					.show();
		}
		setResult(RESULT_OK, null);
		finish();
	}
}