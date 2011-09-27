package com.nbos.phonebook;


import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.Toast;

import com.nbos.phonebook.sync.platform.BatchOperation;

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
        // EditText password = (EditText) findViewById(R.id.password);
        System.out.println("group name is: "+groupName);//+", password: "+password.getEditableText());
        if(groupName.trim().length() == 0)
        {
        	Toast toast = Toast.makeText(getApplicationContext(), "Please enter a name for the new group", Toast.LENGTH_SHORT);
        	toast.show();
        	return;
        }
        String accountName = Db.getAccountName(getApplicationContext());
        if(accountName == null) accountName = "default";
        Db.createAGroup(getApplicationContext(), groupName, null, accountName, 0);
        Toast toast = Toast.makeText(getApplicationContext(), "The group "+groupName+" was created", Toast.LENGTH_SHORT);
        toast.show();
        setResult(RESULT_OK, null);
        finish();
        // doLogin(userName.getEditableText().toString(), password.getEditableText().toString());
      }
}
