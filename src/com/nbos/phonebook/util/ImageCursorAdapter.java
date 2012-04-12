package com.nbos.phonebook.util;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup;
import android.widget.AlphabetIndexer;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.nbos.phonebook.Db;
import com.nbos.phonebook.R;

public class ImageCursorAdapter extends SimpleCursorAdapter implements
		SectionIndexer {

	Cursor c;
	Context context;
	List<String> ids;
	int layout,count=0;
	static String tag = "ImageCursorAdapter";
	String addText;
	AlphabetIndexer alphaIndexer;
	ArrayList<Boolean> checkedItems = new ArrayList<Boolean>();
	Button addButton;

	public ImageCursorAdapter(Context context, int layout, Cursor c,
			List<String> ids, String[] from, int[] to) {
		super(context, layout, c, from, to);
		this.c = c;
		this.context = context;
		this.ids = ids;
		this.layout = layout;
		alphaIndexer = new AlphabetIndexer(c,
				c.getColumnIndex(ContactsContract.Data.DISPLAY_NAME),
				"ABCDEFGHIJKLMNOPQRSTUVWXYZ");
		for (int i = 0; i < this.getCount(); i++)
			checkedItems.add(i, false); // initializes all items value with
										// false
	}

	public void setAddButton(Button addButton, String addText) {
		Log.i(tag, "Setting add button: " + addButton);
		this.addButton = addButton;
		this.addText = addText;
	}

	public List<Boolean> getCheckedItems() {
		return checkedItems;
	}

	public void setCursor(Cursor c) {
		this.c = c;
	}

	public void setIds(List<String> ids) {
		this.ids = ids;
	}
	
	public void toggleSelect(int childCount, ListView list, boolean check){

		for(int i=0; i<childCount; i++)
		{
			list.setItemChecked(i, check);
			checkedItems.set(i, check);
			count = childCount;
		}
		addButton.setEnabled(check);
		if(check)
			addButton.setText(addText.replace("num",
					Integer.toString(list.getCount())));
		else
		{
			count = 0;
			addButton.setText("No contacts selected");
		}
	}

	@Override
	public View getView(final int position, View inView, ViewGroup parent) {
		View v = inView;

		if (v == null) {
			LayoutInflater inflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			v = inflater.inflate(layout, null);
		}
		this.c.moveToPosition(position);

		String contactName = this.c.getString(this.c
				.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
		final String contactId = this.c.getString(this.c.getColumnIndex(ContactsContract.Contacts._ID));
		final String contactWithOnePhoneNumber = ContactWithSinglePhoneNumber(contactId);
		// byte[] pic = images.get(pos);//
		// this.c.getBlob(this.c.getColumnIndex(ContactsContract.CommonDataKinds.Photo.PHOTO));
		ImageView iv = (ImageView) v.findViewById(R.id.contact_pic);
		ImageView ci = (ImageView) v.findViewById(R.id.call_icon);
		View vs = (View) v.findViewById(R.id.view_separator);
		if(ci != null)
		{
			if(contactWithOnePhoneNumber != null)
			{
				ci.setFocusable(true);
				ci.setVisibility(View.VISIBLE);
				vs.setVisibility(View.VISIBLE);
				ci.setOnClickListener(new OnClickListener() {
					
					public void onClick(View v1) {
						Intent callIntent = new Intent(Intent.ACTION_CALL);
						callIntent.setData(Uri.parse("tel:" + contactWithOnePhoneNumber));
						v1.getContext().startActivity(callIntent);
					}
				});
			}
			
			else 
			{
				vs.setVisibility(View.GONE);
				ci.setVisibility(View.GONE);
			}
		}
		iv.setImageBitmap(Db.getPhoto(context.getContentResolver(),
				ids.get(position)));
		iv.setScaleType(ScaleType.FIT_XY);
		TextView cName = (TextView) v.findViewById(R.id.contact_name);
		cName.setText(contactName);

		final CheckBox checkBox = (CheckBox) v.findViewById(R.id.check); // your
		if (checkBox != null) {
			checkBox.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					CheckBox cb = (CheckBox) v.findViewById(R.id.check);
					if (cb.isChecked()) {
						checkedItems.set(position, true);
						count++;
					} else if (!cb.isChecked()) {
						checkedItems.set(position, false);
						count--;
					}
					if (addButton != null)
					{
						if(count !=0)
						{
							addButton.setEnabled(true);
							addButton.setText(addText.replace("num",
									Integer.toString(count)));
						}
						
						else
						{
							addButton.setEnabled(false);
							addButton.setText("No contacts selected");
						}
						
					}
				}
			});
			checkBox.setChecked(checkedItems.get(position));
			
		}
		if(checkBox == null){
			v.setOnClickListener(new OnClickListener() {
				
				public void onClick(View v) {
					Uri contactUri = Uri.parse(ContactsContract.Contacts.CONTENT_URI + "/"
							+ contactId);
	
							Intent intent = new Intent(Intent.ACTION_VIEW, contactUri);
							v.getContext().startActivity(intent);
				}
			});
		
			v.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
	            public void onCreateContextMenu(ContextMenu menu, View v,
	                            ContextMenuInfo menuInfo) {
	            }});
		}
		return v;
	}

	public int getPositionForSection(int section) {
		return alphaIndexer.getPositionForSection(section);
	}

	public int getSectionForPosition(int position) {
		return alphaIndexer.getSectionForPosition(position);
	}

	public Object[] getSections() {
		return alphaIndexer.getSections();
	}
	
	private String ContactWithSinglePhoneNumber(String contactId) {
		Log.i(tag, "getPhoneNumber(" + contactId + ")");
		Cursor phones = context.getContentResolver().query(
				ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
				new String[] { Phone.NUMBER, Phone.TYPE },
				ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = "
						+ contactId, null, null);
		Log.i(tag, "There are " + phones.getCount() + " phone numbers");
		if (phones.getCount() == 1)
		{
			phones.moveToFirst();
			String phoneNumber = phones.getString(phones
				.getColumnIndexOrThrow(Phone.NUMBER));
			return phoneNumber;
		}
		return null;
	}
	
	private OnClickListener callClickListener = new OnClickListener() {
		
		public void onClick(View v) {
			// TODO Auto-generated method stub
			
		}
	};

}
