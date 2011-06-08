package com.nbos.phonebook;


import android.app.Activity;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.nbos.phonebook.sync.platform.BatchOperation;

public class AddGroupActivity extends Activity {

	static String tag = "AddGroupActivity";
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.add_group);
	    getWindow().setTitle("Add a group");
	}
	
    public void addGroupButtonClicked(View v) {
        EditText groupNameEditText = (EditText) findViewById(R.id.groupNameEditText);
        String groupName = groupNameEditText.getEditableText().toString();
        // EditText password = (EditText) findViewById(R.id.password);
        System.out.println("group name is: "+groupName);//+", password: "+password.getEditableText());
        if(groupName.trim().length() == 0)
        {
        	Toast toast = Toast.makeText(getApplicationContext(), "Please enter a name for the new group", Toast.LENGTH_SHORT);
        	toast.show();
        	return;
        }
        String accountName = DatabaseHelper.getAccountName(getApplicationContext());
        if(accountName == null) accountName = "default";
        DatabaseHelper.createAGroup(getApplicationContext(), groupName, accountName, 0);
        Toast toast = Toast.makeText(getApplicationContext(), "The group "+groupName+" was created", Toast.LENGTH_SHORT);
        toast.show();
        setResult(RESULT_OK, null);
        finish();
        // doLogin(userName.getEditableText().toString(), password.getEditableText().toString());
      }
}
