package com.nbos.phonebook.util;

import java.util.HashSet;
import java.util.Set;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.provider.ContactsContract.RawContacts;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.nbos.phonebook.R;
import com.nbos.phonebook.database.tables.BookTable;
import com.nbos.phonebook.sync.Constants;

public class WelcomeActivityCursorAdapter extends SimpleCursorAdapter {
	static String tag = "WelcomeActivityCursorAdapter";
	private Cursor c, sharedBooksCursor, rawContactsCursor;
	private Context context; // this is required

	public WelcomeActivityCursorAdapter(Context context, int layout, 
			Cursor c, Cursor sharedBooksCursor,
			Cursor rawContactsCursor, String[] from, int[] to) {
		super(context, layout, c, from, to);
		this.context = context;
		this.c = c;
		this.sharedBooksCursor = sharedBooksCursor;
		this.rawContactsCursor = rawContactsCursor;
	}
	

	@Override
	public View getView(int position, View inView, ViewGroup parent) {
		View v = super.getView(position, inView, parent);
		TextView sharedText = (TextView) v.findViewById(R.id.sharing_with);
		ImageView image = (ImageView) v.findViewById(R.id.sharing_with_icon);
		c.moveToPosition(position);
		int groupId = c.getInt(c.getColumnIndex(ContactsContract.Groups._ID));
		String accountType = c.getString(c.getColumnIndex(ContactsContract.Groups.ACCOUNT_TYPE));
		if(accountType.equals(Constants.ACCOUNT_TYPE))
		{
			String owner = c.getString(c.getColumnIndex(ContactsContract.Groups.SYNC1));
			TextView ownerTextView = (TextView) v.findViewById(R.id.groupOwner);
			ownerTextView.setText(owner);
		}
		int numSharingWith = getNumSharingWith(groupId);
		if(numSharingWith > 0)
		{
			image.setImageResource(R.drawable.share);
			sharedText.setText(new Integer(numSharingWith).toString());
		}
		else
		{
			image.setImageDrawable(null);
			sharedText.setText(null);
		}
		return v;
	}


	private int getNumSharingWith(int groupId) {
		sharedBooksCursor.moveToFirst();
		Set<String> contactIds = new HashSet<String>();
		if(sharedBooksCursor.getCount() > 0)
		do {
			int bookId = sharedBooksCursor.getInt(sharedBooksCursor.getColumnIndex(BookTable.BOOKID));
			if(bookId > groupId) break;
			if(bookId == groupId) 
			{
				String rawId = sharedBooksCursor.getString(sharedBooksCursor.getColumnIndex(BookTable.CONTACTID)),
					contactId = getContactId(rawId); 
				if(contactId != null)
					contactIds.add(contactId);
			}
				
			
		} while(sharedBooksCursor.moveToNext());
		return contactIds.size();
	}


	private String getContactId(String rawContactId) {
		rawContactsCursor.moveToFirst();
		if (rawContactsCursor.getCount() > 0)
			do {
				String rawId = rawContactsCursor.getString(rawContactsCursor.getColumnIndex(RawContacts._ID)), 
					cId = rawContactsCursor.getString(rawContactsCursor.getColumnIndex(RawContacts.CONTACT_ID));
				if (!rawContactId.equals(rawId)) continue;
				return cId;
			} while (rawContactsCursor.moveToNext());
		return null;
	}

   
}
