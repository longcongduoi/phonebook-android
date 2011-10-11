package com.nbos.phonebook;

import java.util.ArrayList;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

public class AddGroupActivity extends Activity {

	static String tag = "AddGroupActivity";

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_LEFT_ICON);
		setContentView(R.layout.add_group);
		setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.group);
		setTitle("Phonebook: Add a group");
	}

	public void addGroupButtonClicked(View v) {
		EditText groupNameEditText = (EditText) findViewById(R.id.groupNameEditText);
		String groupName = groupNameEditText.getEditableText().toString();

		ContentResolver cr = getContentResolver();
		Cursor m_cursor = cr.query(ContactsContract.Groups.CONTENT_SUMMARY_URI,
				null, ContactsContract.Groups.DELETED + "=0", null,
				ContactsContract.Groups.TITLE);
		Log.i(tag, "add groupcount" + m_cursor.getCount());

		// EditText password = (EditText) findViewById(R.id.password);
		System.out.println("group name is: " + groupName);// +", password: "+password.getEditableText());
		if (groupName.trim().length() == 0) {
			Toast toast = Toast
					.makeText(getApplicationContext(),
							"Please enter a name for the new group",
							Toast.LENGTH_SHORT);
			toast.show();
			return;
		}
		String accountName = Db.getAccountName(getApplicationContext());
		if (accountName == null)
			accountName = "default";
		if(groupNameChecking(m_cursor, groupName)){
			Db.createAGroup(getApplicationContext(), groupName, null,
					accountName, 0);
			Toast toast = Toast.makeText(getApplicationContext(), "The group "
					+ groupName + " was created", Toast.LENGTH_SHORT);
			toast.show();
			setResult(RESULT_OK, null);
			finish();
		}
		else{
			 Toast.makeText(getApplicationContext(),
					 "enter another name to create group", Toast.LENGTH_LONG) .show();
		}
		/*InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		mgr.hideSoftInputFromWindow(findViewById(R.layout.add_group).getWindowToken(), 0);*/

		// doLogin(userName.getEditableText().toString(),
		// password.getEditableText().toString());
	}

	public boolean groupNameChecking(Cursor group_cursor, String groupName) {
         
		boolean newGroupName=true;
		ArrayList<String> groups = new ArrayList<String>();
		for (int i = 0; i < group_cursor.getCount(); i++) {
			group_cursor.moveToPosition(i);
			groups.add(group_cursor.getString(
					group_cursor.getColumnIndex(ContactsContract.Groups.TITLE)));
		  }
		for (int i = 0; i < groups.size(); i++) {
			    if (groups.get(i).equals(groupName)) {
   			          newGroupName=false;
			  }
		  }
	    return newGroupName;
	 }
}
