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
        createAGroup(getApplicationContext(), groupName);
        Toast toast = Toast.makeText(getApplicationContext(), "The group "+groupName+" was created", Toast.LENGTH_SHORT);
        toast.show();
        setResult(RESULT_OK, null);
        finish();
        // doLogin(userName.getEditableText().toString(), password.getEditableText().toString());
      }
	
    private static void createAGroup(Context context, String groupName) {
        final ContentResolver resolver = context.getContentResolver();
        final BatchOperation batchOperation =
            new BatchOperation(context, resolver);
    	
		String mAccountType = "com.example.android.samplesync",
			mAccountName = DatabaseHelper.getAccountName(context, mAccountType);
		Log.i(tag, "Creating group: "+groupName);
		Uri mEntityUri = ContactsContract.Groups.CONTENT_URI.buildUpon()
			.appendQueryParameter(ContactsContract.Groups.ACCOUNT_NAME, mAccountName)
			.appendQueryParameter(ContactsContract.Groups.ACCOUNT_TYPE, mAccountType)
			.appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
			.build();
	
		ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(mEntityUri);
		Log.v("Group", "create accountgroup: "+mAccountType+", "+mAccountName);
		builder.withValue(ContactsContract.Groups.ACCOUNT_TYPE, mAccountType);
		builder.withValue(ContactsContract.Groups.ACCOUNT_NAME, mAccountName);
		builder.withValue(ContactsContract.Groups.SYSTEM_ID, mAccountName);
		builder.withValue(ContactsContract.Groups.TITLE, groupName);
		builder.withValue(ContactsContract.Groups.GROUP_VISIBLE, 1);
		batchOperation.add(builder.build());
		batchOperation.execute();
	}    
}
