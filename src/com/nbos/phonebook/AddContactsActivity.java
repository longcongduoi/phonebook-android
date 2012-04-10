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
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter.CursorToStringConverter;
import android.widget.Toast;

import com.nbos.phonebook.database.IntCursorJoiner;
import com.nbos.phonebook.util.ImageCursorAdapter;
import com.nbos.phonebook.value.ContactRow;

public class AddContactsActivity extends ListActivity {

	MatrixCursor m_cursor;
	Cursor rawContactsCursor;
	String tag = "AddContactsActivity", id, name,owner;
	List<String> ids;
	ImageCursorAdapter adapter;
	Button addContactsButton;
	Db db;
	LinearLayout childLayout;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		db = new Db(getApplicationContext());
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_LEFT_ICON);
		setContentView(R.layout.add_contacts);
		setFeatureDrawableResource(Window.FEATURE_LEFT_ICON,
				R.drawable.add_group);
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			id = extras.getString("id");
			name = extras.getString("name");
			owner=extras.getString("owner");
		}
		setTitle(" Add contacts to " + "'" +name+ "'");
		rawContactsCursor = db.getRawContactsCursor(false);
		childLayout = (LinearLayout) findViewById(R.id.linearlayout1);
		addContactsButton = (Button) findViewById(R.id.add_contact_button);
		addContactsButton.setOnClickListener(selectedContacts);
		listView = this.getListView();
		populateContacts();
		getListView().setTextFilterEnabled(true);
	}

	ListView listView;

	private Button.OnClickListener selectedContacts = new Button.OnClickListener() {
		public void onClick(View v) {
			int numContacts = 0;
			List<Boolean> checkedItems = adapter.getCheckedItems();
			Log.i(tag, "Number of items: " + listView.getChildCount());
			for (int i = 0; i < listView.getCount(); i++) 
			{
				if (checkedItems.get(i)) 
				{
					Log.i(tag, i + " is checked");
					numContacts++;
					m_cursor.moveToPosition(i);
					String contactId = m_cursor.getString(m_cursor
							.getColumnIndex(ContactsContract.Contacts._ID));
					List<String> rawContactIds = Db.getRawContactIds(contactId,
							rawContactsCursor);

					Log.i(tag, "Contact id is: " + contactId
							+ ", raw contactIds is: " + rawContactIds);// +", raw contact id: "+contactId+", lookup key: "+lookupKey);
					for (String rawContactId : rawContactIds)
						Db.addToGroup(id, rawContactId, getContentResolver());

				}
			}
			Toast.makeText(getApplicationContext(),
					"Added " + numContacts + " new contact(s)", Toast.LENGTH_LONG)
					.show();
			finish();
		}

	};

	private void populateContacts() {
		getContactsCursor("");

		String[] fields = new String[] { ContactsContract.Data.DISPLAY_NAME, };
		adapter = new ImageCursorAdapter(this, R.layout.select_contact_entry,
				m_cursor, ids, fields, new int[] { R.id.contact_name });

		adapter.setAddButton(addContactsButton, " Add num contacts to group");
		adapter.setStringConversionColumn(m_cursor
				.getColumnIndexOrThrow(ContactsContract.Data.DISPLAY_NAME));


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
		getListView().setAdapter(adapter);
	}

	void getContactsCursor(String search) {
		Cursor contactsCursor = Db.getContacts(this, search);
		Cursor groupCursor = Db.getContactsInGroup(id, getContentResolver());
		IntCursorJoiner joiner = new IntCursorJoiner(contactsCursor,
				new String[] { ContactsContract.Contacts._ID }, groupCursor,
				new String[] { ContactsContract.Data.CONTACT_ID });

		ids = new ArrayList<String>();
		m_cursor = new MatrixCursor(new String[] {
				ContactsContract.Contacts._ID,
				ContactsContract.Contacts.DISPLAY_NAME }, 10);

		List<ContactRow> rows = new ArrayList<ContactRow>();
		for (CursorJoiner.Result joinerResult : joiner) {
			switch (joinerResult) {
			case LEFT:
				String contactId = contactsCursor.getString(contactsCursor
						.getColumnIndex(ContactsContract.Contacts._ID));
				String name = contactsCursor
						.getString(contactsCursor
								.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
				if (name != null)
					rows.add(new ContactRow(contactId, name));
				break;

			}
		}
		// Collections.sort(rows);
		Collections.sort(rows);
		for (ContactRow row : rows) {
			m_cursor.addRow(new String[] { row.id, row.name });
			ids.add(row.id);
		}

		contactsCursor.close();
		groupCursor.close();
	}

	ListView mGroupList;
	boolean mShowInvisible = false;

	/*@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		int currentPosition = this.getListView().getFirstVisiblePosition();
		m_cursor.moveToPosition(position);
		String contactId = m_cursor.getString(m_cursor
				.getColumnIndex(ContactsContract.Contacts._ID));
		List<String> rawContactIds = Db.getRawContactIds(contactId,
				rawContactsCursor);

		Log.i(tag, "Contact id is: " + contactId + ", raw contactIds is: "
				+ rawContactIds);// +", raw contact id: "+contactId+", lookup key: "+lookupKey);
		for (String rawContactId : rawContactIds)
			Db.addToGroup(this.id, rawContactId, getContentResolver());
		populateContacts();
		this.setSelection(currentPosition);
	}*/

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		this.populateContacts();
	}

}
