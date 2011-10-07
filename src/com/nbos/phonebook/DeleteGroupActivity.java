package com.nbos.phonebook;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.FilterQueryProvider;
import android.widget.ListView;
import android.widget.Toast;

import com.nbos.phonebook.util.WelcomeActivityCursorAdapter;

public class DeleteGroupActivity extends ListActivity {
	
	
	String tag="DeleteGroupActivity";
	
	ListView listview;
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    requestWindowFeature(Window.FEATURE_LEFT_ICON);
		setContentView(R.layout.delete_group);
		setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.icon);
		populateGroups();
		getListView().setTextFilterEnabled(true);
		String phoneNumber = getPhoneNumber();
		Log.i(tag, "phone number: " + phoneNumber);
		listview=this.getListView();
	}
	

	
	
	/*@Override 
	public boolean onCreateOptionsMenu(Menu menu) { 
		MenuInflater inflater = getMenuInflater(); 
		inflater.inflate(R.menu.delete_group_menu, menu); 
		return true;
	}
	*/
	
	/*@Override
	public void onAttachedToWindow() {
		super.onAttachedToWindow();
		openOptionsMenu();
	}*/
	
	
	public boolean onClick(View v) {
		switch (v.getId()) 
		{ 
			case R.id.delete_group: 
			showDeleteGroupDialog();
			break;
		} 
		return true;
	}
	
	/*public boolean onOptionsItemSelected(MenuItem item) { 
		
		switch (item.getItemId()) { 
			case R.id.delete_selected_groups: 
				showDeleteGroupDialog();
		 	break;
			case R.id.delete_group_cancle:
				Intent i=new Intent(DeleteGroupActivity.this,WelcomeActivity.class);
				startActivity(i);
            break;
		} 
		return true;
	}*/
	
	
	Cursor m_cursor;
	private void populateGroups() {
		ContentResolver cr = getContentResolver();
		m_cursor = cr.query(ContactsContract.Groups.CONTENT_SUMMARY_URI, null,
				ContactsContract.Groups.DELETED + "=0", null,
				ContactsContract.Groups.TITLE);

		String[] fields = new String[] { ContactsContract.Groups.TITLE,
				ContactsContract.Groups.SUMMARY_COUNT,
				ContactsContract.Groups.SYNC1 };
		Cursor sharedBooksCursor = Db.getBooks(cr);
		WelcomeActivityCursorAdapter adapter = new WelcomeActivityCursorAdapter(
				this, R.layout.delete_group_entry, m_cursor, sharedBooksCursor,
				fields, new int[] { R.id.groupName, R.id.groupCount,
						R.id.groupOwner });

		adapter.setStringConversionColumn(m_cursor
				.getColumnIndexOrThrow(ContactsContract.Groups.TITLE));

		adapter.setFilterQueryProvider(new FilterQueryProvider() {

			public Cursor runQuery(CharSequence constraint) {
				String partialItemName = null;
				if (constraint != null) {
					partialItemName = constraint.toString();
				}
				m_cursor = getContentResolver().query(
						ContactsContract.Groups.CONTENT_SUMMARY_URI,
						null,
						// "DISPLAY_NAME = '" + NAME + "'",
						ContactsContract.Groups.DELETED + "=0" + " and "
								+ ContactsContract.Groups.TITLE + " like '%"
								+ partialItemName + "%'", null, null);

				
				Log.i(tag, "partial name is" + partialItemName);

				return m_cursor;
			}
		});

		getListView().setAdapter(adapter);
	}

	
	private String getPhoneNumber() {
		return ((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE))
				.getLine1Number();
	}
	
	
	
	
	
	private void showDeleteGroupDialog() {
		 
		int delete_group_count=0;
		
		for(int i=0;i<listview.getChildCount();i++){
			View childView = listview.getChildAt(i);
    		CheckBox check =(CheckBox)childView.findViewById(R.id.check);
    		
    		if(check.isChecked()){
    			delete_group_count++;
    		}
		}
		Log.i(tag,"selected group count"+delete_group_count);
		if(delete_group_count>0){
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Are you sure you want to delete this group?")
				.setCancelable(false)
				.setPositiveButton("Yes",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								deleteGroups();
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
		
		else{
			Toast.makeText(getApplicationContext(), "select atleast one group", Toast.LENGTH_LONG).show();
		}
	}

	private void deleteGroups() {
		int numDeleted = 0;
		for(int i=0;i<listview.getChildCount();i++) 
		{
			View childView = listview.getChildAt(i);
    		CheckBox check =(CheckBox)childView.findViewById(R.id.check);
    		
    		if(!check.isChecked()) continue; 
			Log.i(tag, i+" is checked");
			m_cursor.moveToPosition(i);
			String groupId = m_cursor.getString(m_cursor
					.getColumnIndex(ContactsContract.Groups._ID)), groupName = m_cursor
					.getString(m_cursor
							.getColumnIndex(ContactsContract.Groups.TITLE));
			
			System.out.println("delete group: " + groupId + ", " + groupName);
			String[] args = { groupId };
			int b = getContentResolver().delete(
					ContactsContract.Groups.CONTENT_URI, "_ID=?", args);
			numDeleted++;
			// notify registered observers that a row was updated
			
    	}
		Toast.makeText(this, "Deleted "+numDeleted+" groups", Toast.LENGTH_LONG).show();
		getContentResolver().notifyChange(
				ContactsContract.Groups.CONTENT_URI, null);
		setResult(RESULT_OK, null);
		finish();
    }
		//
		


}
