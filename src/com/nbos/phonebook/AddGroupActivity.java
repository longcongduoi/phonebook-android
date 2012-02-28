package com.nbos.phonebook;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Groups;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class AddGroupActivity extends Activity {

	static String tag = "AddGroupActivity";
	String name,groupId;
	int position;
	EditText groupName;
	Cursor m_cursor;
	Button addGroup;
	TextView header;
	ContentResolver cr;
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_LEFT_ICON);
		setContentView(R.layout.add_group);
		setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.group);
		addGroup = (Button)findViewById(R.id.addGroupButton);
	    header = (TextView)findViewById(R.id.textView1);
	    groupName = (EditText)findViewById(R.id.groupNameEditText);
	    
		setTitle("Phonebook: Add a group");
		addGroup.setOnClickListener(addGroupButton);
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
		    name = extras.getString("name");
		    groupId = extras.getString("id");
		    position = extras.getInt("position");
		}
	    
	    cr = getContentResolver();
	    
		if(name!=null)
		{
			addGroup.setOnClickListener(renameGroupButton);
			addGroup.setText("OK");
			groupName.setText(name);
			header.setText("Rename group");
			setTitle("Phonebook: Rename group");
		}
		Log.i(tag,"name: "+name);
	}

	public void addGroupButtonClicked(View v) {
		EditText groupNameEditText = (EditText) findViewById(R.id.groupNameEditText);
		String groupName = groupNameEditText.getEditableText().toString();
		m_cursor = cr.query(ContactsContract.Groups.CONTENT_SUMMARY_URI,
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
			Db.createAGroup(getApplicationContext(), groupName, null, null,
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
	
	private Button.OnClickListener addGroupButton = new Button.OnClickListener(){

		public void onClick(View v) {
			EditText groupNameEditText = (EditText) findViewById(R.id.groupNameEditText);
			String groupName = groupNameEditText.getEditableText().toString();
			m_cursor = cr.query(ContactsContract.Groups.CONTENT_SUMMARY_URI,
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
				Db.createAGroup(getApplicationContext(), groupName, null, null,
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
		}
		
	};
	
	private Button.OnClickListener renameGroupButton = new Button.OnClickListener() {
		public void onClick(View v) 
		{
			String newName = groupName.getEditableText().toString();
			Db.setNewName(groupId, newName, cr);
			finish();
		}

	};
}
