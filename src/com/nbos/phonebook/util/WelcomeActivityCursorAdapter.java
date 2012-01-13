package com.nbos.phonebook.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.nbos.phonebook.R;
import com.nbos.phonebook.database.tables.BookTable;
import com.nbos.phonebook.sync.Constants;

public class WelcomeActivityCursorAdapter extends SimpleCursorAdapter {
	static String tag = "WelcomeActivityCursorAdapter";
	private Cursor c, sharedBooksCursor, rawContactsCursor;
	private Context context; // this is required
	ArrayList<Boolean> checkedItems = new ArrayList<Boolean>();
	Button addButton;
	String addText;
	int count = 0;

	public WelcomeActivityCursorAdapter(Context context, int layout, Cursor c,
			Cursor sharedBooksCursor, Cursor rawContactsCursor, String[] from,
			int[] to) {
		super(context, layout, c, from, to);
		this.context = context;
		this.c = c;
		this.sharedBooksCursor = sharedBooksCursor;
		this.rawContactsCursor = rawContactsCursor;
		for (int i = 0; i < this.getCount(); i++)
			checkedItems.add(i, false);
	}

	public List<Boolean> getCheckedItems() {
		return checkedItems;
	}

	public void setAddButton(Button addButton, String addText) {
		Log.i(tag, "Setting add button: " + addButton);
		this.addButton = addButton;
		this.addText = addText;
	}

	@Override
	public View getView(final int position, View inView, ViewGroup parent) {
		View v = super.getView(position, inView, parent);
		TextView sharedText = (TextView) v.findViewById(R.id.sharing_with);
		ImageView image = (ImageView) v.findViewById(R.id.sharing_with_icon);
		c.moveToPosition(position);
		int groupId = c.getInt(c.getColumnIndex(ContactsContract.Groups._ID));
		String groupName = c.getString(c
				.getColumnIndex(ContactsContract.Groups.TITLE));
		Log.i(tag, "groupId: " + groupId + " groupName: " + groupName);
		String accountType = c.getString(c
				.getColumnIndex(ContactsContract.Groups.ACCOUNT_TYPE));
		TextView ownerTextView = (TextView) v.findViewById(R.id.groupOwner);
		if (accountType.equals(Constants.ACCOUNT_TYPE)) {
			String owner = c.getString(c
					.getColumnIndex(ContactsContract.Groups.SYNC1));
			ownerTextView.setText(owner);
		} else
			ownerTextView.setText(R.string.empty);

		int numSharingWith = getNumSharingWith(groupId);
		if (numSharingWith > 0) {
			image.setImageResource(R.drawable.share);
			sharedText.setText(new Integer(numSharingWith).toString());
		} else {
			image.setImageDrawable(null);
			sharedText.setText(null);
		}

		CheckBox checkBox = (CheckBox) v.findViewById(R.id.check);
		if (checkBox != null) {
			checkBox.setOnClickListener(new OnClickListener() {
				public void onClick(View v) 
				{
					CheckBox cb = (CheckBox) v.findViewById(R.id.check);
					if (cb.isChecked()) 
					{
						count++;
						checkedItems.set(position, true);
					} else if (!cb.isChecked()) 
					{
						count--;
						checkedItems.set(position, false);
					}
					if (addButton != null) 
					{
						addButton.setVisibility(1);
						if (count != 0) 
						{
							addButton.setEnabled(true);
							addButton.setText(addText.replace("num",
									Integer.toString(count)));
						}

						else 
						{
							addButton.setEnabled(false);
							addButton.setText("No groups selected");
						}

					}
				}
			});
			checkBox.setChecked(checkedItems.get(position));
		}
		return v;
	}

	private int getNumSharingWith(int groupId) {
		sharedBooksCursor.moveToFirst();
		Set<String> contactIds = new HashSet<String>();
		if (sharedBooksCursor.getCount() > 0)
			do {
				int bookId = sharedBooksCursor.getInt(sharedBooksCursor
						.getColumnIndex(BookTable.BOOKID));
				if (bookId > groupId)
					break;
				if (bookId == groupId) {
					String rawId = sharedBooksCursor
							.getString(sharedBooksCursor
									.getColumnIndex(BookTable.CONTACTID)), contactId = getContactId(rawId);
					if (contactId != null)
						contactIds.add(contactId);
				}

			} while (sharedBooksCursor.moveToNext());
		return contactIds.size();
	}

	private String getContactId(String rawContactId) {
		rawContactsCursor.moveToFirst();
		if (rawContactsCursor.getCount() > 0)
			do {
				String rawId = rawContactsCursor.getString(rawContactsCursor
						.getColumnIndex(RawContacts._ID)), cId = rawContactsCursor
						.getString(rawContactsCursor
								.getColumnIndex(RawContacts.CONTACT_ID));
				if (!rawContactId.equals(rawId))
					continue;
				return cId;
			} while (rawContactsCursor.moveToNext());
		return null;
	}

}
