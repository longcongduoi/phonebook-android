package com.nbos.phonebook.util;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.util.Log;
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
	private Cursor c, sharedBooksCursor;
	private Context context;

	public WelcomeActivityCursorAdapter(Context context, int layout, 
			Cursor c, Cursor sharedBooksCursor,
			String[] from, int[] to) {
		super(context, layout, c, from, to);
		this.context = context;
		this.c = c;
		this.sharedBooksCursor = sharedBooksCursor;
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
			image.setImageResource(android.R.drawable.ic_menu_share);
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
		int numSharingWith = 0;
		sharedBooksCursor.moveToFirst();
		if(sharedBooksCursor.getCount() > 0)
		do {
			int bookId = sharedBooksCursor.getInt(sharedBooksCursor.getColumnIndex(BookTable.BOOKID));
			if(bookId > groupId) break;
			if(bookId == groupId) 
				numSharingWith++;
			
		} while(sharedBooksCursor.moveToNext());
		return numSharingWith;
	}

   
}
