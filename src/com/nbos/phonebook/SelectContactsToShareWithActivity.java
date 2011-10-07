package com.nbos.phonebook;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.ListActivity;
import android.database.Cursor;
import android.database.CursorJoiner;
import android.database.MatrixCursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FilterQueryProvider;
import android.widget.ListView;
import android.widget.Toast;

import com.nbos.phonebook.database.IntCursorJoiner;
import com.nbos.phonebook.database.tables.BookTable;
import com.nbos.phonebook.util.ImageCursorAdapter;
import com.nbos.phonebook.value.ContactRow;

public class SelectContactsToShareWithActivity extends ListActivity {

	MatrixCursor m_cursor;
	Cursor rawContactsCursor;
	List<String> ids;
	String tag = "SelectContactsToShareWith", id, name;
	ImageCursorAdapter adapter;
	Db db;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.contacts_to_share_with);
		Bundle extras = getIntent().getExtras();
		db = new Db(getApplicationContext());
		if (extras != null) {
			id = extras.getString("id");
			name = extras.getString("name");
		}

		setTitle("Select contacts to share " + name + "with");
		populateContacts();
		getListView().setTextFilterEnabled(true);
		Button addContactsButton = (Button) findViewById(R.id.add_contact_button);
		addContactsButton.setOnClickListener(selectedContacts);
		listView = this.getListView();

	}

	ListView listView;

	private Button.OnClickListener selectedContacts = new Button.OnClickListener() {
		public void onClick(View v) {
			int numContacts = 0;
			Log.i(tag, "Number of items: " + listView.getChildCount());
			for (int i = 0; i < listView.getChildCount(); i++) {
				View childView = listView.getChildAt(i);
				CheckBox check = (CheckBox) childView.findViewById(R.id.check);
				if (check.isChecked()) {
					Log.i(tag, i + " is checked");
					numContacts++;
					m_cursor.moveToPosition(i);
					String contactId = m_cursor.getString(m_cursor
							.getColumnIndex(ContactsContract.Contacts._ID));
					String name = m_cursor
							.getString(m_cursor
									.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
					Log.i(tag, "Contact id is: " + contactId + ", name: "
							+ name);// +", raw contact id: "+contactId+", lookup key: "+lookupKey);
					shareGroupWithContact(contactId);
				}
			}
			Toast.makeText(getApplicationContext(),
					"Sharing with " + numContacts + " new contacts",
					Toast.LENGTH_LONG).show();
			finish();
		}

	};

	private void populateContacts() {
		rawContactsCursor = db.getRawContactsCursor(false);
		getContactsCursor("");

		String[] fields = new String[] { ContactsContract.Data.DISPLAY_NAME };

		adapter = new ImageCursorAdapter(this, R.layout.select_contact_entry,
				m_cursor, ids, fields, new int[] { R.id.contact_name });

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
		Log.i(tag, "There are " + m_cursor.getCount() + " contacts sharing");
		getListView().setAdapter(adapter);
	}

	private void getContactsCursor(String search) {
		Cursor contactsCursor = Db.getContacts(this, search);
		Cursor bookCursor = db.getBook(id);
		IntCursorJoiner joiner = new IntCursorJoiner(contactsCursor,
				new String[] { ContactsContract.Contacts._ID }, bookCursor,
				new String[] { BookTable.CONTACTID });
		ids = new ArrayList<String>();
		m_cursor = new MatrixCursor(new String[] {
				ContactsContract.Contacts._ID,
				ContactsContract.Contacts.DISPLAY_NAME }, 10);
		List<ContactRow> rows = new ArrayList<ContactRow>();
		Set<String> contactIds = getContactIds(bookCursor);
		contactsCursor.moveToFirst();
		if (contactsCursor.getCount() > 0)
			do {
				String contactId = contactsCursor.getString(contactsCursor
						.getColumnIndex(Contacts._ID)), 
						name = contactsCursor
						.getString(contactsCursor
								.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
				if (!contactIds.contains(contactId))
					rows.add(new ContactRow(contactId, name));
			} while (contactsCursor.moveToNext());
		for (CursorJoiner.Result joinerResult : joiner) {
			switch (joinerResult) {
			case LEFT:
				String id = contactsCursor.getString(contactsCursor
						.getColumnIndex(ContactsContract.Contacts._ID));
				String name = contactsCursor
						.getString(contactsCursor
								.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
				if (name != null)
					rows.add(new ContactRow(id, name));
				break;
			}
		}

		Collections.sort(rows);
		for (ContactRow row : rows) {
			m_cursor.addRow(new String[] { row.id, row.name });
			ids.add(row.id);
		}
	}

	private Set<String> getContactIds(Cursor bookCursor) {
		Set<String> contactIds = new HashSet<String>();
		bookCursor.moveToFirst();
		if (bookCursor.getCount() > 0)
			do {
				String rawContactId = bookCursor.getString(bookCursor
						.getColumnIndex(BookTable.CONTACTID));
				rawContactsCursor.moveToFirst();
				if (rawContactsCursor.getCount() > 0)
					do {
						String rawId = rawContactsCursor
								.getString(rawContactsCursor
										.getColumnIndex(RawContacts._ID)), cId = rawContactsCursor
								.getString(rawContactsCursor
										.getColumnIndex(RawContacts.CONTACT_ID));
						if (rawId.equals(rawContactId))
							contactIds.add(cId);
					} while (rawContactsCursor.moveToNext());
			} while (bookCursor.moveToNext());
		return contactIds;
	}

	private boolean isInBook(String contactId, Cursor bookCursor) {
		bookCursor.moveToFirst();
		if (bookCursor.getCount() > 0)
			do {
				String rawContactId = bookCursor.getString(bookCursor
						.getColumnIndex(BookTable.CONTACTID));
				rawContactsCursor.moveToFirst();
				if (rawContactsCursor.getCount() > 0)
				do {
					String rawId = rawContactsCursor
							.getString(rawContactsCursor
									.getColumnIndex(RawContacts._ID)), cId = rawContactsCursor
							.getString(rawContactsCursor
									.getColumnIndex(RawContacts.CONTACT_ID));
					if (rawId.equals(rawContactId) && cId.equals(contactId))
						return true;
				} while (rawContactsCursor.moveToNext());
			} while (bookCursor.moveToNext());
		return false;
	}

	protected void onListItemClick(ListView l, View v, int position, long id) {
		int currentPosition = this.getListView().getFirstVisiblePosition();
		m_cursor.moveToPosition(position);
		String contactId = m_cursor.getString(m_cursor
				.getColumnIndex(ContactsContract.Contacts._ID));
		Log.i(tag, "Contact id is: " + contactId);// +", raw contact id: "+contactId+", lookup key: "+lookupKey);
		shareGroupWithContact(contactId);
		this.setSelection(currentPosition);

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
